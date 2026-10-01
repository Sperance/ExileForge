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

/**
 * Where the update stands (3.72.0). [verified] - a check succeeded since the start: until then the game is closed.
 * [update] - the build the player must take; [progress] - its download, 0..1; [installing] - in the system installer.
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
)

/**
 * Updates without a store (3.72.0): at start, by hand and every hour, GitHub Releases is asked for a build newer than this
 * one that speaks the live server's wire. Any such build is required: the game stays closed until it is installed. A check
 * that fails at start closes the game too, until one succeeds; a later one that fails changes nothing.
 */
class UpdateViewModel(app: Application, private val server: suspend () -> StaticManifest?) : AndroidViewModel(app) {
    private val updates = Updates()
    // A build that does not update itself (debug, the tested shrunk one) is never closed: only a check by hand runs.
    private val mutable = MutableStateFlow(if (BuildConfig.UPDATES) UpdateState() else UpdateState(checking = false, verified = true))
    val state: StateFlow<UpdateState> = mutable.asStateFlow()
    private val checks = Mutex()
    private var download: Job? = null

    init {
        if (BuildConfig.UPDATES) viewModelScope.launch { while (true) { check(); delay(Updates.PERIOD_MS) } }
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

    /** «Повторить» after a check that failed. */
    fun retry() { viewModelScope.launch { check() } }

    private suspend fun check(manual: Boolean = false) = checks.withLock {
        mutable.update { it.copy(checking = true, upToDate = false) }
        try {
            val found = updates.check(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, server())
            mutable.update { s ->
                // The update being downloaded is kept: a newer one waits for the next check after it.
                s.copy(checking = false, verified = true, failure = null, update = if (s.progress != null || s.installing) s.update else found,
                    upToDate = manual && found == null)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            mutable.update { it.copy(checking = false, failure = ui("update.check_failed", e.message ?: e::class.simpleName.orEmpty())) }
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

    class Factory(private val app: Application, private val server: suspend () -> StaticManifest?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UpdateViewModel(app, server) as T
    }

    private companion object { const val DIR = "updates" }
}
