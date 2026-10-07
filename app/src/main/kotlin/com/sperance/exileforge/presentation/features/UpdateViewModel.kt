package com.sperance.exileforge.presentation.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.update.AvailableUpdate
import com.sperance.exileforge.core.update.Updates
import com.sperance.exileforge.core.update.Wire
import com.sperance.exileforge.presentation.app.ServerReach
import com.sperance.exileforge.presentation.app.StartStage
import com.sperance.exileforge.presentation.app.StartupTrace
import com.sperance.exileforge.presentation.world.GameResources
import com.sperance.exileforge.update.InstallResult
import com.sperance.exileforge.update.UpdateInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
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
 *
 * С 3.90.3 сами проверки идут одной очередью ([pending]): на холодном старте - только когда ресурсы игры ([resources])
 * дочитаны, а не бок о бок с ними; по каждому возвращению в игру - сперва сверка ресурсов, затем проверка, без
 * поминутного порога; во время похода или испытания ([playing]) - ни одной, отложенная проверка идёт по их концу.
 */
class UpdateViewModel(
    app: Application,
    private val reach: ServerReach,
    newerServer: Flow<Unit> = emptyFlow(),
    http: OkHttpClient,
    private val trace: StartupTrace,
    private val resources: GameResources,
    private val playing: Flow<Boolean>,
) : AndroidViewModel(app) {
    private val updates = Updates(client = http)

    // A build that does not update itself (debug, the tested shrunk one) never checks by itself: only a check by hand runs.
    private val mutable = MutableStateFlow(UpdateState(checking = BuildConfig.UPDATES))
    val state: StateFlow<UpdateState> = mutable.asStateFlow()
    private val checks = Mutex()
    private var download: Job? = null

    /** Отложенная проверка: null - не нужна; первая - холодного старта. Две просьбы сливаются в более полную. */
    private val pending = MutableStateFlow<Request?>(Request.CHECK)

    /** Холодный старт позади: возвращения в игру с этих пор сверяют ресурсы. */
    @Volatile private var warm = false
    private var periodic: Job? = null

    // Установщик ушёл в системное окно: ответ его ждётся, пока игрок не вернулся в игру без него.
    private var installerReturn: Job? = null

    init {
        if (BuildConfig.UPDATES) {
            viewModelScope.launch { serve() }
            // The server answered: its wire may make a build required, so the check runs again with it.
            viewModelScope.launch { reach.state.map { it.wire }.filterNotNull().distinctUntilChanged().collect { ask(Request.CHECK) } }
            // A server newer than this build refused the sign-in (3.74.0): its build is looked for now, not in an hour -
            // мимо очереди: без новой сборки игра дальше входа не пойдёт, и ресурсов ей ждать незачем.
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

    /**
     * Очередь проверок: первая ждёт ресурсов старта (не дольше [RESOURCES_WAIT_MS] - без связи они могут и не прийти), каждая -
     * конца похода и испытания; после каждой следующая плановая встаёт через час, после отказа GitHub - через минуту.
     */
    private suspend fun serve() {
        trace.step(StartStage.VERSION, "start.step.wait_resources") { withTimeoutOrNull(RESOURCES_WAIT_MS) { resources.ready.first { it } } }
        while (true) {
            pending.filterNotNull().first()
            playing.first { !it }
            val request = pending.getAndUpdate { null } ?: continue
            if (request == Request.RECHECK) resources.recheck()
            val answered = check()
            warm = true
            periodic?.cancel()
            periodic = viewModelScope.launch {
                delay(if (answered) Updates.PERIOD_MS else RETRY_MS)
                ask(Request.CHECK)
            }
        }
    }

    private fun ask(request: Request) = pending.update { maxOf(it ?: request, request) }

    /** «Проверить обновления»: asked by hand, the answer is said either way. */
    fun checkNow() {
        viewModelScope.launch { check(manual = true) }
    }

    /**
     * Игрок вернулся в игру (3.90.3): сверка ресурсов и проверка сборки - каждый раз, без порога; во время похода или
     * испытания - по их концу. До конца холодного старта возвращение ничего не добавляет: его проверка и так впереди.
     */
    fun resumed() {
        if (BuildConfig.UPDATES && warm) ask(Request.RECHECK)
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

    /** Отложенная проверка (3.90.3): просто проверка или, по возвращении в игру, сперва сверка ресурсов; вторая полнее. */
    private enum class Request { CHECK, RECHECK }

    private companion object {
        const val DIR = "updates"

        /** A failed check is tried again this soon, unseen. */
        const val RETRY_MS = 60_000L

        /** Дольше этого первая проверка не ждёт ресурсов старта: без связи они не придут, а сборку искать надо. */
        const val RESOURCES_WAIT_MS = 30_000L

        /** Back in the game while installing: the installer's answer is waited for this long before the retry is offered. */
        const val INSTALLER_GRACE_MS = 3_000L
    }
}
