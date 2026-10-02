package com.sperance.exileforge.presentation.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.StaticManifest
import com.sperance.exileforge.core.update.AvailableUpdate
import com.sperance.exileforge.core.update.Updates
import com.sperance.exileforge.update.InstallResult
import com.sperance.exileforge.update.UpdateInstaller
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import com.sperance.exileforge.data.settings.GuideStore

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
)

/**
 * Updates without a store (3.72.0): at start, by hand and every hour, GitHub Releases is asked for a build newer than this
 * one that speaks the live server's wire. Any such build is required: the game stays closed until it is installed. Since
 * 3.73.0 the check runs unseen: the game opens at once, and a check that fails is quietly tried again a minute later.
 */
class UpdateViewModel(app: Application, private val server: suspend () -> StaticManifest?,
                      newerServer: kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow()) : AndroidViewModel(app) {
    private val updates = Updates()
    // A build that does not update itself (debug, the tested shrunk one) is never closed: only a check by hand runs.
    private val mutable = MutableStateFlow(if (BuildConfig.UPDATES) UpdateState() else UpdateState(checking = false, verified = true))
    val state: StateFlow<UpdateState> = mutable.asStateFlow()
    private val checks = Mutex()
    private var download: Job? = null

    private val guides = GuideStore(app)

    init {
        if (BuildConfig.UPDATES) viewModelScope.launch { while (true) delay(if (check()) Updates.PERIOD_MS else RETRY_MS) }
        // A server newer than this build refused the sign-in (3.74.0): its build is looked for now, not in an hour.
        if (BuildConfig.UPDATES) viewModelScope.launch { newerServer.collect { check() } }
        if (BuildConfig.UPDATES && !UpdateInstaller.allowed(app)) viewModelScope.launch {
            if (SOURCES !in guides.read.first()) mutable.update { it.copy(askSources = true) }
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
    fun checkNow() { viewModelScope.launch { check(manual = true) } }

    /** The first-start question about unknown sources is answered, either way: it is not asked again. */
    fun sourcesAsked() {
        mutable.update { it.copy(askSources = false) }
        viewModelScope.launch { guides.markRead(SOURCES) }
    }

    /** One check; whether it reached GitHub. */
    private suspend fun check(manual: Boolean = false): Boolean = checks.withLock {
        mutable.update { it.copy(checking = true, upToDate = false) }
        try {
            val found = updates.check(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, server())
            mutable.update { s ->
                // The update being downloaded is kept: a newer one waits for the next check after it.
                s.copy(checking = false, verified = true, failure = null, update = if (s.progress != null || s.installing) s.update else found,
                    upToDate = manual && found == null)
            }
            true
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            mutable.update { it.copy(checking = false, failure = ui("update.check_failed", e.message ?: e::class.simpleName.orEmpty())) }
            false
        }
    }

    /** «Обновить»: the APK is downloaded, its SHA-256 checked, and the system installer asked. */
    fun install() {
        val update = state.value.update ?: return
        if (download?.isActive == true) return
        val context = getApplication<Application>()
        if (!UpdateInstaller.allowed(context)) { mutable.update { it.copy(needsPermission = true) }; return }
        download = viewModelScope.launch {
            mutable.update { it.copy(needsPermission = false, error = null, progress = 0f) }
            try {
                val apk = File(File(context.cacheDir, DIR).apply { listFiles()?.forEach { it.delete() } }, "ExileForge-${update.info.versionName}.apk")
                updates.download(update, apk) { read, total -> if (total > 0) mutable.update { it.copy(progress = (read.toFloat() / total).coerceIn(0f, 1f)) } }
                mutable.update { it.copy(progress = null, installing = true) }
                UpdateInstaller.install(context, apk)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(progress = null, installing = false, error = ui("update.download_failed", e.message ?: e::class.simpleName.orEmpty())) }
            }
        }
    }

    /** The permission screen was left: the player tries again. */
    fun permissionAsked() = mutable.update { it.copy(needsPermission = false) }

    class Factory(private val app: Application, private val newerServer: kotlinx.coroutines.flow.Flow<Unit>,
                  private val server: suspend () -> StaticManifest?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UpdateViewModel(app, server, newerServer) as T
    }

    private companion object {
        const val DIR = "updates"
        /** A failed check is tried again this soon, unseen. */
        const val RETRY_MS = 60_000L
        /** The first-start question about unknown sources, as the device's guides remember it. */
        const val SOURCES = "install_sources"
    }
}
