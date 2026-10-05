package com.sperance.exileforge.presentation.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.update.AvailableUpdate
import com.sperance.exileforge.core.update.Updates
import com.sperance.exileforge.core.update.Wire
import com.sperance.exileforge.presentation.app.ServerReach
import com.sperance.exileforge.presentation.app.StartStage
import com.sperance.exileforge.presentation.app.StartupTrace
import com.sperance.exileforge.update.InstallResult
import com.sperance.exileforge.update.UpdateInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.File

/**
 * Where the update stands (3.86.0). [checking]/[failure] - the check under way or why it failed. [update] - the build found:
 * every one locks the game until it is installed. [progress] - its download, 0..1; [installing] - in the system installer.
 * [askSources] - «install unknown apps» is not allowed yet: the game asks for it until it is, so an update installs at once.
 */
data class UpdateState(
    val checking: Boolean = true,
    val failure: String? = null,
    val update: AvailableUpdate? = null,
    val progress: Float? = null,
    val installing: Boolean = false,
    val needsPermission: Boolean = false,
    val error: String? = null,
    /** The answer of a check asked for by hand: this build is the latest. */
    val upToDate: Boolean = false,
    val askSources: Boolean = false,
) {
    /** The build the game must take before anything else: every update found is one. */
    val mandatory: Boolean get() = update != null
}

/**
 * Updates from GitHub Releases (3.86.0): at start, when the server answers (its wire decides whether a build is required),
 * by hand and every hour. A failed check is quietly tried again a minute later and never holds the start.
 */
class UpdateViewModel(
    app: Application,
    private val reach: ServerReach,
    newerServer: kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow(),
    http: OkHttpClient,
    private val trace: StartupTrace,
) : AndroidViewModel(app) {
    private val updates = Updates(client = http)

    // A build that does not update itself (debug, the tested shrunk one) never checks by itself: only a check by hand runs.
    private val mutable = MutableStateFlow(UpdateState(checking = BuildConfig.UPDATES))
    val state: StateFlow<UpdateState> = mutable.asStateFlow()
    private val checks = Mutex()
    private var download: Job? = null

    @Volatile private var lastCheck = 0L

    // Установщик ушёл в системное окно: ответ его ждётся, пока игрок не вернулся в игру без него.
    private var installerReturn: Job? = null

    init {
        if (BuildConfig.UPDATES) {
            viewModelScope.launch {
                while (true) delay(if (check()) Updates.PERIOD_MS else RETRY_MS)
            }
            // The server answered: its wire may make a build required, so the check runs again with it.
            viewModelScope.launch { reach.state.map { it.wire }.filterNotNull().distinctUntilChanged().collect { check() } }
            // A server newer than this build refused the sign-in (3.74.0): its build is looked for now, not in an hour.
            viewModelScope.launch { newerServer.collect { check() } }
        }
        recheckSources()
        viewModelScope.launch {
            UpdateInstaller.results.collect { result ->
                installerReturn?.cancel()
                when (result) {
                    InstallResult.Done -> mutable.update { it.copy(installing = false) }
                    is InstallResult.Failed -> mutable.update { it.copy(installing = false, error = ui("update.install_failed", result.message)) }
                }
            }
        }
    }

    /** «Проверить обновления»: asked by hand, the answer is said either way. */
    fun checkNow() {
        viewModelScope.launch { check(manual = true) }
    }

    /** The player came back to the app: the build is asked again, at most once a minute. The caller does not ask during a run. */
    fun resumed() {
        if (!BuildConfig.UPDATES || System.currentTimeMillis() - lastCheck < RESUME_GAP_MS) return
        viewModelScope.launch { check() }
    }

    /**
     * The game is in front again (back from the settings or from the system installer): «install unknown apps» is asked
     * again, and an install the system closed without an answer stops waiting - after [INSTALLER_GRACE_MS], so an
     * answer on its way still wins; the player retries with «Обновить».
     */
    fun foreground() {
        recheckSources()
        if (!state.value.installing) return
        installerReturn?.cancel()
        installerReturn = viewModelScope.launch {
            delay(INSTALLER_GRACE_MS)
            mutable.update { it.copy(installing = false) }
        }
    }

    private fun recheckSources() {
        val allowed = UpdateInstaller.allowed(getApplication())
        mutable.update { it.copy(askSources = BuildConfig.UPDATES && !allowed, needsPermission = it.needsPermission && !allowed) }
    }

    /** One check; whether GitHub answered. */
    private suspend fun check(manual: Boolean = false): Boolean = checks.withLock {
        lastCheck = System.currentTimeMillis()
        mutable.update { it.copy(checking = true, upToDate = false) }
        try {
            val wire: Wire? = reach.state.value.wire
            val found = trace.step(StartStage.VERSION, "start.step.latest") { updates.check(BuildConfig.VERSION_CODE, wire) }
            mutable.update { s ->
                // The update being downloaded is kept: a newer one waits for the next check after it.
                val next = if (s.progress != null || s.installing) s.update else found
                s.copy(
                    checking = false,
                    failure = null,
                    update = next,
                    upToDate = manual && found == null,
                )
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutable.update { it.copy(checking = false, failure = ui("update.check_failed", e.message ?: e::class.simpleName.orEmpty())) }
            false
        }
    }

    /** «Обновить»: the APK is downloaded from GitHub (resumed where it broke off), its SHA-256 checked, the installer asked. */
    fun install() {
        val update = state.value.update ?: return
        if (download?.isActive == true) return
        val context = getApplication<Application>()
        if (!UpdateInstaller.allowed(context)) {
            mutable.update { it.copy(needsPermission = true) }
            return
        }
        download = viewModelScope.launch {
            mutable.update { it.copy(needsPermission = false, error = null, progress = 0f) }
            try {
                val name = "ExileForge-${update.info.versionName}.apk"
                // Другие сборки - мусор; начатая эта остаётся и докачивается.
                val dir = File(context.cacheDir, DIR).apply { listFiles()?.filter { it.name != name }?.forEach { it.delete() } }
                val apk = File(dir, name)
                updates.download(update, apk) { read, total -> if (total > 0) mutable.update { it.copy(progress = (read.toFloat() / total).coerceIn(0f, 1f)) } }
                mutable.update { it.copy(progress = null, installing = true) }
                UpdateInstaller.install(context, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update { it.copy(progress = null, installing = false, error = ui("update.download_failed", e.message ?: e::class.simpleName.orEmpty())) }
            }
        }
    }

    /** The permission screen was left: the player tries again. */
    fun permissionAsked() = mutable.update { it.copy(needsPermission = false) }

    private companion object {
        const val DIR = "updates"

        /** A failed check is tried again this soon, unseen. */
        const val RETRY_MS = 60_000L

        /** Coming back to the app asks again no sooner than this after the last check. */
        const val RESUME_GAP_MS = 60_000L

        /** Back in the game while installing: the installer's answer is waited for this long before the retry is offered. */
        const val INSTALLER_GRACE_MS = 3_000L
    }
}
