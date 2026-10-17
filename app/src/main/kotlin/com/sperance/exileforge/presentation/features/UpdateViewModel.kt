package com.sperance.exileforge.presentation.features

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.HttpLink
import com.sperance.exileforge.core.session.WireFit
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
import kotlinx.coroutines.flow.collectLatest
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
import java.io.File

/**
 * Where the update stands. [checking]/[failure] - the check under way or why it failed. [update] - the build found:
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
    val askSources: Boolean = false,
) {
    /** The build the game must take before anything else: every update found is one. */
    val mandatory: Boolean get() = update != null
}

/**
 * Updates from GitHub Releases: at start, when the server answers (its wire decides whether a build is required),
 * by hand and every hour. A failed check is quietly tried again a minute later and never holds the start.
 *
 * Сами проверки идут одной очередью ([pending]): на холодном старте - только когда ресурсы игры ([resources])
 * дочитаны, а не бок о бок с ними. Возвращение в игру (не чаще [RESUME_GAP_MS]) и каждая ошибка сервера (426 - сразу, прочие ошибки и обрыв
 * без ответа - не чаще [FAILED_GAP_MS]) проверяют сборку тут же, и во время похода или испытания ([playing]): найденная
 * сборка закрывает игру по их концу (окно обновления это знает), а сверка ресурсов ([resourcesDue]) ждёт их конца - она
 * перечитывает мир под идущим боем. Провод сервера каждая проверка спрашивает заново ([ServerReach.freshWire]).
 *
 * Ответ сервера может сделать сборку обязательной - проверка идёт снова. Сервер новее сборки - окно запуска ждёт новую, она ищется каждые
 * [AWAIT_POLL_MS]; отказ входа сервером новее - сборка ищется сразу, мимо очереди. Ошибка сервера или молчание - сверка ресурсов и сборки
 * не чаще раза в [FAILED_GAP_MS].
 */
class UpdateViewModel(
    app: Application,
    private val reach: ServerReach,
    newerServer: Flow<Unit> = emptyFlow(),
    http: HttpLink,
    private val trace: StartupTrace,
    private val resources: GameResources,
    private val playing: Flow<Boolean>,
    serverFailed: Flow<Unit> = emptyFlow(),
) : AndroidViewModel(app) {
    private val updates = Updates(http)

    // A build that does not update itself (debug, the tested shrunk one) never checks by itself: only a check by hand runs.
    private val mutable = MutableStateFlow(UpdateState(checking = BuildConfig.UPDATES))
    val state: StateFlow<UpdateState> = mutable.asStateFlow()
    private val checks = Mutex()

    /** «Что нового»: заметки последних релизов; null - ещё не загружены, пусто - GitHub не ответил. */
    private val notesFlow = MutableStateFlow<List<com.sperance.exileforge.core.update.ReleaseNotes>?>(null)
    val notes: StateFlow<List<com.sperance.exileforge.core.update.ReleaseNotes>?> = notesFlow.asStateFlow()

    /** Загружает заметки последних релизов, если их ещё нет или прошлый раз не вышло. */
    fun loadNotes() {
        if (!notesFlow.value.isNullOrEmpty()) return
        notesFlow.value = null
        viewModelScope.launch { notesFlow.value = runCatching { updates.recent() }.getOrDefault(emptyList()) }
    }
    private var download: Job? = null

    /** Отложенная проверка: null - не нужна; первая - холодного старта. Две просьбы сливаются в более полную. */
    private val pending = MutableStateFlow<Request?>(Request.CHECK)

    /** Холодный старт позади: возвращения в игру с этих пор сверяют ресурсы. */
    @Volatile private var warm = false

    /** Когда GitHub в последний раз ответил на проверку ([SystemClock.elapsedRealtime]); 0 - ещё не отвечал. */
    @Volatile private var answeredAt = 0L

    /** Сверка ресурсов просрочена: идёт, как только нет похода и испытания. */
    private val resourcesDue = MutableStateFlow(false)
    private var periodic: Job? = null

    // Установщик ушёл в системное окно: ответ его ждётся, пока игрок не вернулся в игру без него.
    private var installerReturn: Job? = null

    init {
        if (BuildConfig.UPDATES) {
            viewModelScope.launch { serve() }
            viewModelScope.launch { refreshResources() }
            viewModelScope.launch { reach.state.map { it.wire }.filterNotNull().distinctUntilChanged().collect { ask(Request.CHECK) } }
            viewModelScope.launch {
                reach.window.map { it.fit == WireFit.GAME_BEHIND }.distinctUntilChanged().collectLatest { behind ->
                    while (behind && state.value.update == null) {
                        check(outdated = true)
                        if (state.value.update == null) delay(AWAIT_POLL_MS)
                    }
                }
            }
            viewModelScope.launch { newerServer.collect { check(outdated = true) } }
            viewModelScope.launch {
                var last = 0L
                serverFailed.collect {
                    val now = System.currentTimeMillis()
                    if (now - last >= FAILED_GAP_MS) {
                        last = now
                        ask(Request.RECHECK)
                    }
                }
            }
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
     * Очередь проверок: первая ждёт провода сервера (не дольше [WIRE_WAIT_MS] - без связи он может и не прийти; не
     * ресурсов: окно запуска держит игру до конца проверки, она идёт сразу), каждая следующая идёт сразу, и в походе тоже; сверка
     * ресурсов просьбы [Request.RECHECK] - своим чередом ([refreshResources]). После каждой проверки следующая плановая встаёт
     * через час, после отказа GitHub - через минуту.
     */
    private suspend fun serve() {
        trace.step(StartStage.VERSION, "start.step.server_manifest") { reach.wire(WIRE_WAIT_MS) }
        while (true) {
            pending.filterNotNull().first()
            val request = pending.getAndUpdate { null } ?: continue
            if (request == Request.RECHECK) resourcesDue.value = true
            val answered = check()
            warm = true
            periodic?.cancel()
            periodic = viewModelScope.launch {
                delay(if (answered) Updates.PERIOD_MS else RETRY_MS)
                ask(Request.CHECK)
            }
        }
    }

    /** Просроченная сверка ресурсов: по концу похода и испытания - она перечитывает мир, на котором идёт бой. */
    private suspend fun refreshResources() {
        while (true) {
            resourcesDue.first { it }
            playing.first { !it }
            resourcesDue.value = false
            resources.recheck()
        }
    }

    private fun ask(request: Request) = pending.update { maxOf(it ?: request, request) }

    /**
     * Игрок вернулся в игру: проверка сборки - сразу, в походе тоже; сверка ресурсов - по концу похода и испытания.
     * До конца холодного старта возвращение ничего не добавляет: его проверка и так впереди. Не чаще
     * [RESUME_GAP_MS] после последнего ответа GitHub: короткие отлучки из игры запросов не добавляют.
     */
    fun resumed() {
        if (!BuildConfig.UPDATES || !warm) return
        val answered = answeredAt
        if (answered == 0L || SystemClock.elapsedRealtime() - answered >= RESUME_GAP_MS) ask(Request.RECHECK)
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

    /**
     * One check; whether GitHub answered. Провод сервера - свежий ([ServerReach.freshWire]): провод старта мог устареть, пока
     * игра лежала в фоне, и тогда сборка под новый сервер отвергалась бы как «не на его проводе». [outdated] - сервер сам
     * назвал сборку устаревшей.
     *
     * Скачиваемое обновление сохраняется: новое ждёт следующей проверки.
     */
    private suspend fun check(outdated: Boolean = false): Boolean = checks.withLock {
        mutable.update { it.copy(checking = true) }
        try {
            val wire: Wire? = reach.freshWire()
            val found = trace.step(StartStage.VERSION, "start.step.latest") { updates.check(BuildConfig.VERSION_CODE, wire, outdated) }
            mutable.update { s ->
                val next = if (s.progress != null || s.installing) s.update else found
                s.copy(
                    checking = false,
                    failure = null,
                    update = next,
                )
            }
            answeredAt = SystemClock.elapsedRealtime()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutable.update { it.copy(checking = false, failure = ui("update.check_failed", e.message ?: e::class.simpleName.orEmpty())) }
            false
        }
    }

    /** «Проверить сейчас» окна запуска: сервер новее сборки - новая ищется тут же, а не через [AWAIT_POLL_MS]. */
    fun checkNow() {
        if (BuildConfig.UPDATES) viewModelScope.launch { check(outdated = true) }
    }

    /**
     * «Обновить»: the APK is downloaded from GitHub (resumed where it broke off), its SHA-256 checked, the installer asked.
     *
     * Другие сборки - мусор; начатая эта остаётся и докачивается.
     */
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

    /** Отложенная проверка: просто проверка или, по возвращении в игру, сперва сверка ресурсов; вторая полнее. */
    private enum class Request { CHECK, RECHECK }

    private companion object {
        const val DIR = "updates"

        /** A failed check is tried again this soon, unseen. */
        const val RETRY_MS = 60_000L

        /** Не чаще одной проверки обновления на ошибки ответов сервера и обрывы без ответа. */
        const val FAILED_GAP_MS = 5 * 60_000L

        /** Возвращение в игру проверяет сборку, только если GitHub отвечал не позже этого. */
        const val RESUME_GAP_MS = 10 * 60_000L

        /** Дольше этого первая проверка не ждёт провода сервера: без связи он не придёт, а сборку искать надо. */
        const val WIRE_WAIT_MS = 20_000L

        /** Как часто ищется новая сборка, пока сервер новее этой. */
        const val AWAIT_POLL_MS = 30_000L

        /** Back in the game while installing: the installer's answer is waited for this long before the retry is offered. */
        const val INSTALLER_GRACE_MS = 3_000L
    }
}
