package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.ADMIN_TABS
import com.sperance.exileforge.core.app.AppMode
import com.sperance.exileforge.core.app.AppModes
import com.sperance.exileforge.core.app.AppStartup
import com.sperance.exileforge.core.app.Building
import com.sperance.exileforge.core.app.Buzz
import com.sperance.exileforge.core.app.ConnectionActions
import com.sperance.exileforge.core.app.Foreground
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.ServerReach
import com.sperance.exileforge.core.app.SessionActions
import com.sperance.exileforge.core.app.TAB_REDEMPTION
import com.sperance.exileforge.core.app.TAB_TREE
import com.sperance.exileforge.core.app.Warmup
import com.sperance.exileforge.core.app.WarmupActions
import com.sperance.exileforge.core.campaign.ExpeditionActions
import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.campaign.TrialActions
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.feedback.reportSheet
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.presentation.app.BugDelivery
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Оболочка приложения: вкладки и здания по прежнему номеру, «Настройки» поверх экрана, строка в тосты,
 * прогрев, поход и испытание поверх стека и жизненный цикл активности (над сервисами, без рантайма).
 *
 * Приложение запускается один раз, кто бы из оболочки или активности ни попросил первым.
 */
class ShellViewModel(
    startup: AppStartup,
    slice: GameSlice,
    private val navigator: Navigator,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val buzzer: Buzzes,
    private val prefs: PreferencesRepository,
    private val connection: ServerConnection,
    private val bugs: BugDelivery,
    private val sessions: SessionRepository,
    private val modes: AppModes,
    private val session: SessionActions,
    private val link: ConnectionActions,
    private val warming: WarmupActions,
    private val expedition: ExpeditionActions,
    trial: TrialActions,
    rift: com.sperance.exileforge.core.campaign.RiftActions,
    /** Связь с сервером на старте: окно запуска уходит по её ответу. */
    val reach: ServerReach,
    private val foreground: Foreground,
    /** Последние падения: их стеки прикладываются к отчёту об ошибке. */
    private val crashes: com.sperance.exileforge.presentation.app.CrashKeeper,
    crafts: com.sperance.exileforge.core.crafts.CraftsRepository,
) : ViewModel() {
    init {
        reach.start()
        startup.start()
    }

    /** Верх стека навигатора: фаза, вкладка, здание. */
    val route: StateFlow<Route> = navigator.current

    /** Прогрев героя, поход и испытание - состояния игры поверх стека. */
    val warmup: StateFlow<Warmup?> = warming.state
    val run: StateFlow<ExpeditionRun?> = expedition.run
    val arena: StateFlow<TrialArena?> = trial.arena

    /** Досчёт ремёсел после отлучки ещё не показан листом: вкладка ремёсел помечена. */
    val craftsAway: StateFlow<Boolean> = crafts.state.map { it.away != null }.stateIn(viewModelScope, SharingStarted.Eagerly, crafts.state.value.away != null)

    /** Разлом недели: открыт - экран Разлома поверх стека. */
    val rift: StateFlow<com.sperance.exileforge.core.campaign.RiftState> = rift.state

    /** Что телефон отзывает вибрацией, уже по настройкам. */
    val buzzes: SharedFlow<Buzz> get() = buzzer.flow

    fun warmUp() = warming.start()

    /**
     * Отчёт жука: со входом или без. Собранный отчёт сразу ложится в очередь на диске ([BugDelivery]) и уходит
     * оттуда повторами, не занимая очередь команд; [onQueued] - отчёт сохранён, черновик можно стирать. Снимков экрана нет.
     *
     * К ошибке прикладываются последние падения клиента и снимок героя; без прочитанного контента отчёт уходит без журнала сбоев.
     */
    fun reportBug(report: BugReportRequest, onQueued: suspend () -> Unit = {}) = bugs.file({
        val rules = game.value.index?.rules?.feedback
        val evidence = if (report.kind == FeedbackKind.BUG) {
            report.copy(traces = crashes.recent(), heroSheet = game.value.hero?.reportSheet().orEmpty())
        } else {
            report
        }
        connection.current.value?.bugReport(evidence, rules?.failures ?: 0) ?: evidence
    }, onQueued)

    /** Аккорд громкости: просьба открыть лист отчёта. */
    private val bugAsks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val bugRequests: SharedFlow<Unit> = bugAsks

    /** Открыть лист отчёта: его откроет хозяин системных листов. */
    fun requestBug() {
        bugAsks.tryEmit(Unit)
    }

    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui

    /** Повторное нажатие на открытую вкладку: её экран закрывает вложенное и прокручивается наверх. */
    private val reselects = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val reselected: kotlinx.coroutines.flow.SharedFlow<Int> = reselects

    /**
     * Вкладка по прежнему номеру; закрытую уровнем героя навигатор не откроет и скажет, с какого.
     *
     * Промокоды заводит только администратор: модератору их экран не открывается.
     */
    fun tab(tab: Int) {
        if (!adminTools() && tab in ADMIN_TABS) return
        if (tab == TAB_REDEMPTION && !sessions.state.value.isAdmin) return
        commands.dismissMessage()
        val again = navigator.current.value.tab == tab
        navigator.tab(Route.ofTab(tab))
        if (again) reselects.tryEmit(tab)
    }

    /** Инструменты администратора: роль модерации аккаунта и режим вместе. */
    private fun adminTools(): Boolean = sessions.state.value.isModerator && modes.mode.value == AppMode.ADMIN

    /** Здание Города или площадь для null. */
    fun building(building: Building?) {
        commands.dismissMessage()
        navigator.tab(Route.ofBuilding(building))
    }

    /** «Настройки» поверх открытой вкладки; закрытие возвращает на неё. [page] - сразу на свою страницу. */
    fun openSettings(page: String? = null) {
        commands.dismissMessage()
        navigator.open(Route.Settings(page))
    }

    /** Узел дерева, на котором оно откроется: гнездо для самоцвета из карточки. */
    private var treeNode: String? = null

    /** Пассивное дерево с выбранным узлом [node]. */
    fun openTree(node: String) {
        treeNode = node
        tab(TAB_TREE)
    }

    fun takeTreeNode(): String? = treeNode.also { treeNode = null }

    fun closeSettings() = navigator.back()

    /** Проба связи сразу: нажата иконка «не в сети». */
    fun retryLink() = link.wake(now = true)

    /** Сеть устройства вернулась. */
    fun networkBack() {
        link.networkBack()
        bugs.kick()
    }

    /** Строка в тосты с экранов (место, открытое уровнем). */
    fun announce(text: String) = notices.toast(text)

    fun dismissMessage() = commands.dismissMessage()
    fun dismissNotice() = notices.dismiss()
    fun buzz(kind: Buzz) = buzzer.buzz(kind)

    /** Настройка устройства, в хранилище устройства: фильтр журнала боя. Фильтры списков предметов - у самих списков. */
    fun logFilter(kinds: Set<LogKind>) {
        viewModelScope.launch { prefs.saveLogFilter(kinds) }
    }

    /** Возврат на передний план: фоновые циклы идут дальше, связь восстанавливается без нажатия. */
    fun reconnect() {
        foreground.back()
        session.reconnect()
    }

    /** Касание красного облака связи: переподключение сразу. */
    fun reconnectLink() = session.reconnectLink()

    /** Уход с переднего плана: фоновые циклы встают на паузу, момент запомнен - долгое отсутствие обновит экран на возврате. */
    fun away() {
        foreground.away()
        session.away()
    }

    /** Журнал похода уходит сразу: процесс, убитый в фоне, не унесёт его с собой. */
    fun flushRun() = expedition.flushRun()

    /** Вход встретил сервер новее сборки: проверка обновлений идёт сразу. */
    val newerServer: Flow<Unit> get() = session.newerServer
    val serverFailed: Flow<Unit> get() = session.serverFailed
}
