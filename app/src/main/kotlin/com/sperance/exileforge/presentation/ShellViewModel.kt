package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.presentation.app.AppStartup
import com.sperance.exileforge.presentation.app.ConnectionActions
import com.sperance.exileforge.presentation.app.SessionActions
import com.sperance.exileforge.presentation.app.StartStage
import com.sperance.exileforge.presentation.app.StartupTrace
import com.sperance.exileforge.presentation.app.Warmup
import com.sperance.exileforge.presentation.app.WarmupActions
import com.sperance.exileforge.presentation.expedition.ExpeditionActions
import com.sperance.exileforge.presentation.expedition.TrialActions
import com.sperance.exileforge.presentation.features.UpdateSource
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.ADMIN_TABS
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppModes
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.StashSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Оболочка приложения (3.80.30): вкладки и здания по прежнему номеру, «Настройки» поверх экрана, строка в тосты,
 * прогрев, поход и испытание поверх стека и жизненный цикл активности (3.80.44: над сервисами, без рантайма).
 */
class ShellViewModel(
    startup: AppStartup,
    slice: GameSlice,
    private val navigator: Navigator,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val buzzer: Buzzes,
    private val journal: RequestJournal,
    private val prefs: PreferencesRepository,
    private val connection: ServerConnection,
    private val sessions: SessionRepository,
    private val modes: AppModes,
    private val session: SessionActions,
    private val link: ConnectionActions,
    private val warming: WarmupActions,
    private val expedition: ExpeditionActions,
    trial: TrialActions,
    private val trace: StartupTrace,
) : ViewModel() {
    init {
        // Приложение запускается один раз, кто бы из оболочки или активности ни попросил первым.
        startup.start()
    }

    /** Верх стека навигатора: фаза, вкладка, здание (3.80.40). */
    val route: StateFlow<Route> = navigator.current

    /** Прогрев героя, поход и испытание - состояния игры поверх стека. */
    val warmup: StateFlow<Warmup?> = warming.state
    val run: StateFlow<ExpeditionRun?> = expedition.run
    val arena: StateFlow<TrialArena?> = trial.arena
    val logs = journal.entries

    /** Что телефон отзывает вибрацией (3.77.0), уже по настройкам. */
    val buzzes: SharedFlow<Buzz> get() = buzzer.flow

    fun warmUp() = warming.start()

    /** Отчёт жука (3.48.0): уходит сразу, со входом или без; [onSent] - когда сервер его принял. */
    fun reportBug(report: BugReportRequest, onSent: suspend () -> Unit = {}) = commands.task {
        // Шаги запуска (3.82.0) - в отчёт: где и сколько стоял старт.
        connection.api.reportBug(report.copy(requests = report.requests + trace.journal().lines().takeLast(STARTUP_LINES).chunked(STARTUP_CHUNK).map { it.joinToString("\n") }))
        onSent()
        notices.toast(ui("bug.sent"))
    }

    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui

    /** Вкладка по прежнему номеру; закрытую уровнем героя навигатор не откроет и скажет, с какого. */
    fun tab(tab: Int) {
        if (!adminTools() && tab in ADMIN_TABS) return
        commands.dismissMessage()
        navigator.tab(Route.ofTab(tab))
    }

    /** Администратор в инструментах: отладочная сборка, роль и режим вместе. */
    private fun adminTools(): Boolean = BuildConfig.DEBUG && sessions.state.value.isAdmin && modes.mode.value == AppMode.ADMIN

    /** Здание Города (3.22.0) или площадь для null. */
    fun building(building: Building?) {
        commands.dismissMessage()
        navigator.tab(Route.ofBuilding(building))
    }

    /** «Настройки» (3.77.0) поверх открытой вкладки; закрытие возвращает на неё. */
    fun openSettings() {
        commands.dismissMessage()
        navigator.open(Route.Settings)
    }

    fun closeSettings() = navigator.back()

    /** Проба связи сразу (3.30.0): нажата иконка «не в сети». */
    fun retryLink() = link.wake(now = true)

    /** Строка в тосты с экранов (3.76.0: место, открытое уровнем). */
    fun announce(text: String) = notices.toast(text)

    fun dismissMessage() = commands.dismissMessage()
    fun dismissNotice() = notices.dismiss()
    fun buzz(kind: Buzz) = buzzer.buzz(kind)
    fun clearLogs() = journal.clear()

    /** Настройки устройства, в хранилище устройства: порядок сундука и «скрыть надетое» (3.30.0, 3.69.0), фильтр журнала боя. */
    fun stashSort(sort: StashSort) {
        viewModelScope.launch { prefs.saveStashSort(sort) }
    }

    fun stashHideWorn(hide: Boolean) {
        viewModelScope.launch { prefs.saveStashHideWorn(hide) }
    }

    fun logFilter(kinds: Set<LogKind>) {
        viewModelScope.launch { prefs.saveLogFilter(kinds) }
    }

    // ---- жизненный цикл активности (3.80.42: из удалённой общей модели) ----

    /** Возврат на передний план: связь восстанавливается без нажатия. */
    fun reconnect() = session.reconnect()

    /** Уход с переднего плана: момент запомнен, чтобы долгое отсутствие обновило экран на возврате. */
    fun away() = session.away()

    /** Журнал похода уходит сразу: процесс, убитый в фоне, не унесёт его с собой. */
    fun flushRun() = expedition.flushRun()

    /** Вход встретил сервер новее сборки (3.74.0): проверка обновлений идёт сразу. */
    val newerServer: Flow<Unit> get() = session.newerServer

    /**
     * Откуда проверять обновления (3.82.0): сервер игры и провод живого сервера; провода нет, пока сервер не отвечает.
     * Ожидание сервера и его манифест - подпункты «Версии» в окне запуска.
     */
    suspend fun updateSource(): UpdateSource {
        trace.step(StartStage.VERSION, "start.step.wait_server") { withTimeoutOrNull(API_WAIT_MS) { session.apiReady.await() } }
        val live = try {
            trace.step(StartStage.VERSION, "start.step.server_manifest") { connection.api.liveManifest() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return UpdateSource(sessions.state.value.server, live?.let { it.revision to it.rules })
    }

    private companion object {
        /** Сколько проверка обновлений ждёт, пока у приложения появится сервер. */
        const val API_WAIT_MS = 10_000L

        /** Последние шаги запуска в баг-репорте и сколько строк в одной записи (сервер берёт записи до 400 знаков). */
        const val STARTUP_LINES = 40
        const val STARTUP_CHUNK = 5
    }
}
