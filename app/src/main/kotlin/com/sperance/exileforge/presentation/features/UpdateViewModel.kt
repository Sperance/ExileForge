package com.sperance.exileforge.presentation.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.StaticManifest
import com.sperance.exileforge.core.update.AvailableUpdate
import com.sperance.exileforge.core.update.Updates
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.update.InstallResult
import com.sperance.exileforge.update.UpdateInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.io.File

/**
 * Where the update stands (3.72.0). [verified] - a check succeeded since the start (3.73.0: the game no longer waits for it).
 * [update] - the build the player must take; [progress] - its download, 0..1; [installing] - in the system installer.
 * [askSources] - the first start asks once for «install unknown apps» (3.73.0), so an update later installs at once.
 */
data class UpdateState(
    val checking: Boolean = true,
    val verified: Boolean = false,
    val failure: String? = null,
    val update: AvailableUpdate? = null,
    val progress: Float? = null,
    val installing: Boolean = false,
    val needsPermission: Boolean = false,
    val error: String? = null,
    /** The answer of a check asked for by hand: this build is the latest. */
    val upToDate: Boolean = false,
    val askSources: Boolean = false,
    /**
     * The start's window may let the game in (3.81.1): a check passed, or the first one has not come back within
     * [UpdateViewModel.FIRST_GATE_MS] — a GitHub out of reach no longer holds the game shut; a newer build found later still locks it.
     */
    val opened: Boolean = false,
)

/**
 * Updates without a store (3.72.0): at start, by hand and every hour, GitHub Releases is asked for a build newer than this
 * one that speaks the live server's wire. Any such build is required: the game stays closed until it is installed. Since
 * 3.73.0 the check runs unseen: the game opens at once, and a check that fails is quietly tried again a minute later.
 */
class UpdateViewModel(
    app: Application,
    private val server: suspend () -> StaticManifest?,
    newerServer: kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow(),
    private val guides: GuideStore,
    http: OkHttpClient,
) : AndroidViewModel(app) {
    private val updates = Updates(client = http)

    // A build that does not update itself (debug, the tested shrunk one) is never closed: only a check by hand runs.
    private val mutable = MutableStateFlow(if (BuildConfig.UPDATES) UpdateState() else UpdateState(checking = false, verified = true, opened = true))
    val state: StateFlow<UpdateState> = mutable.asStateFlow()
    private val checks = Mutex()
    private var download: Job? = null

    init {
        // Until one check has passed the game stays shut (3.76.0), so a failed one is tried again soon; after that, hourly.
        if (BuildConfig.UPDATES) {
            viewModelScope.launch {
                delay(FIRST_GATE_MS)
                mutable.update { it.copy(opened = true) }
            }
            viewModelScope.launch {
                while (true) {
                    delay(
                        if (check()) {
                            Updates.PERIOD_MS
                        } else if (state.value.verified) {
                            RETRY_MS
                        } else {
                            FIRST_RETRY_MS
                        },
                    )
                }
            }
        }
        // A server newer than this build refused the sign-in (3.74.0): its build is looked for now, not in an hour.
        if (BuildConfig.UPDATES) viewModelScope.launch { newerServer.collect { check() } }
        if (BuildConfig.UPDATES && !UpdateInstaller.allowed(app)) {
            viewModelScope.launch {
                if (SOURCES !in guides.read.first()) mutable.update { it.copy(askSources = true) }
            }
        }
        viewModelScope.launch {
            UpdateInstaller.results.collect { result ->
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

    /** «Повторить» on the start's gate (3.76.0): one more check at once. */
    fun retry() {
        viewModelScope.launch { check() }
    }

    /**
     * The player came back to the app (3.76.0): the releases are asked again, at most once a minute — a build that came
     * out meanwhile locks the game at once. The caller does not ask during a run.
     */
    fun resumed() {
        if (!BuildConfig.UPDATES || System.currentTimeMillis() - lastCheck < RESUME_GAP_MS) return
        viewModelScope.launch { check() }
    }

    @Volatile private var lastCheck = 0L

    /** The first-start question about unknown sources is answered, either way: it is not asked again. */
    fun sourcesAsked() {
        mutable.update { it.copy(askSources = false) }
        viewModelScope.launch { guides.markRead(SOURCES) }
    }

    /** One check; whether it reached GitHub. */
    private suspend fun check(manual: Boolean = false): Boolean = checks.withLock {
        lastCheck = System.currentTimeMillis()
        mutable.update { it.copy(checking = true, upToDate = false) }
        try {
            val found = updates.check(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, server())
            mutable.update { s ->
                // The update being downloaded is kept: a newer one waits for the next check after it.
                s.copy(
                    checking = false,
                    verified = true,
                    opened = true,
                    failure = null,
                    update = if (s.progress != null || s.installing) s.update else found,
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

    /** «Обновить»: the APK is downloaded, its SHA-256 checked, and the system installer asked. */
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
                val apk = File(File(context.cacheDir, DIR).apply { listFiles()?.forEach { it.delete() } }, "ExileForge-${update.info.versionName}.apk")
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

    companion object {
        /** How long the start's window waits for the first check before it lets the game in regardless. */
        const val FIRST_GATE_MS = 12_000L

        private const val DIR = "updates"

        /** A failed check is tried again this soon, unseen. */
        private const val RETRY_MS = 60_000L

        /** Before the first check has passed, with the game shut behind it: tried again this soon. */
        private const val FIRST_RETRY_MS = 10_000L

        /** Coming back to the app asks again no sooner than this after the last check. */
        private const val RESUME_GAP_MS = 60_000L

        /** The first-start question about unknown sources, as the device's guides remember it. */
        private const val SOURCES = "install_sources"
    }
}
