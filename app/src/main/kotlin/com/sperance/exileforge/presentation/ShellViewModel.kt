package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.platform.ScreenCapture
import com.sperance.exileforge.platform.ScreenShot
import com.sperance.exileforge.presentation.app.AppStartup
import com.sperance.exileforge.presentation.app.BugDelivery
import com.sperance.exileforge.presentation.app.ConnectionActions
import com.sperance.exileforge.presentation.app.Foreground
import com.sperance.exileforge.presentation.app.ServerReach
import com.sperance.exileforge.presentation.app.SessionActions
import com.sperance.exileforge.presentation.app.Warmup
import com.sperance.exileforge.presentation.app.WarmupActions
import com.sperance.exileforge.presentation.expedition.ExpeditionActions
import com.sperance.exileforge.presentation.expedition.TrialActions
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.ADMIN_TABS
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.AppModes
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.TAB_REDEMPTION
import com.sperance.exileforge.presentation.state.TAB_TREE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    rift: com.sperance.exileforge.presentation.expedition.RiftActions,
    /** Связь с сервером на старте (3.86.0): окно запуска уходит по её ответу. */
    val reach: ServerReach,
    private val foreground: Foreground,
) : ViewModel() {
    init {
        reach.start()
        // Приложение запускается один раз, кто бы из оболочки или активности ни попросил первым.
        startup.start()
    }

    /** Верх стека навигатора: фаза, вкладка, здание (3.80.40). */
    val route: StateFlow<Route> = navigator.current

    /** Прогрев героя, поход и испытание - состояния игры поверх стека. */
    val warmup: StateFlow<Warmup?> = warming.state
    val run: StateFlow<ExpeditionRun?> = expedition.run
    val arena: StateFlow<TrialArena?> = trial.arena

    /** Разлом недели (3.96.0): открыт - экран Разлома поверх стека. */
    val rift: StateFlow<com.sperance.exileforge.presentation.expedition.RiftState> = rift.state

    /** Что телефон отзывает вибрацией (3.77.0), уже по настройкам. */
    val buzzes: SharedFlow<Buzz> get() = buzzer.flow

    fun warmUp() = warming.start()

    /**
     * Отчёт жука (3.48.0): со входом или без. Снимок [shot] (4.8.0) сжимается в пределы правил `feedback` и едет в отчёте,
     * если вид отчёта его берёт; не вписался - отчёт уходит без него. С 4.9.0 собранный отчёт сразу ложится в очередь на диске
     * ([BugDelivery]) и уходит оттуда повторами, не занимая очередь команд; [onQueued] - отчёт сохранён, черновик можно стирать.
     */
    fun reportBug(report: BugReportRequest, shot: ScreenShot? = null, onQueued: suspend () -> Unit = {}) = bugs.file({
        // Без прочитанного контента (4.12.x: у правил нет значений по умолчанию) отчёт уходит без снимка и без журнала сбоев
        val rules = game.value.index?.rules?.feedback
        val screenshot = rules?.let { feedback -> shot?.takeIf { report.kind.shots }?.let { ScreenCapture.encode(it, feedback) } }
        val built = report.copy(screenshot = screenshot)
        connection.current.value?.bugReport(built, rules?.failures ?: 0) ?: built
    }, onQueued)

    /** Аккорд громкости (4.8.0): лист отчёта со снимком, снятым до него; null - снимок не вышел, лист всё равно открывается. */
    private val bugShots = MutableSharedFlow<ScreenShot?>(extraBufferCapacity = 1)
    val bugRequests: SharedFlow<ScreenShot?> = bugShots

    /** Открыть лист отчёта со снимком [shot] (4.8.0): его откроет хозяин системных листов. */
    fun requestBug(shot: ScreenShot?) {
        bugShots.tryEmit(shot)
    }

    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui

    /** Повторное нажатие на открытую вкладку (3.95.0): её экран закрывает вложенное и прокручивается наверх. */
    private val reselects = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val reselected: kotlinx.coroutines.flow.SharedFlow<Int> = reselects

    /** Вкладка по прежнему номеру; закрытую уровнем героя навигатор не откроет и скажет, с какого. */
    fun tab(tab: Int) {
        if (!adminTools() && tab in ADMIN_TABS) return
        // Промокоды (ADMIN-UI) заводит только администратор: модератору их экран не открывается
        if (tab == TAB_REDEMPTION && !sessions.state.value.isAdmin) return
        commands.dismissMessage()
        val again = navigator.current.value.tab == tab
        navigator.tab(Route.ofTab(tab))
        if (again) reselects.tryEmit(tab)
    }

    /** Инструменты администратора (4.12.x, ADMIN-UI): роль модерации аккаунта и режим вместе. */
    private fun adminTools(): Boolean = sessions.state.value.isModerator && modes.mode.value == AppMode.ADMIN

    /** Здание Города (3.22.0) или площадь для null. */
    fun building(building: Building?) {
        commands.dismissMessage()
        navigator.tab(Route.ofBuilding(building))
    }

    /** «Настройки» (3.77.0) поверх открытой вкладки; закрытие возвращает на неё. [page] (3.94.0) - сразу на свою страницу. */
    fun openSettings(page: String? = null) {
        commands.dismissMessage()
        navigator.open(Route.Settings(page))
    }

    /** Узел дерева, на котором оно откроется (3.94.1): гнездо для самоцвета из карточки. */
    private var treeNode: String? = null

    /** Пассивное дерево с выбранным узлом [node]. */
    fun openTree(node: String) {
        treeNode = node
        tab(TAB_TREE)
    }

    fun takeTreeNode(): String? = treeNode.also { treeNode = null }

    fun closeSettings() = navigator.back()

    /** Проба связи сразу (3.30.0): нажата иконка «не в сети». */
    fun retryLink() = link.wake(now = true)

    /** Сеть устройства вернулась (3.95.2). */
    fun networkBack() {
        link.networkBack()
        bugs.kick()
    }

    /** Строка в тосты с экранов (3.76.0: место, открытое уровнем). */
    fun announce(text: String) = notices.toast(text)

    fun dismissMessage() = commands.dismissMessage()
    fun dismissNotice() = notices.dismiss()
    fun buzz(kind: Buzz) = buzzer.buzz(kind)

    /** Настройка устройства, в хранилище устройства: фильтр журнала боя. Фильтры списков предметов - у самих списков (4.2.0). */
    fun logFilter(kinds: Set<LogKind>) {
        viewModelScope.launch { prefs.saveLogFilter(kinds) }
    }

    // ---- жизненный цикл активности (3.80.42: из удалённой общей модели) ----

    /** Возврат на передний план: фоновые циклы идут дальше, связь восстанавливается без нажатия. */
    fun reconnect() {
        foreground.back()
        session.reconnect()
    }

    /** Касание красного облака связи (4.4.x): переподключение сразу. */
    fun reconnectLink() = session.reconnectLink()

    /** Уход с переднего плана: фоновые циклы встают на паузу, момент запомнен - долгое отсутствие обновит экран на возврате. */
    fun away() {
        foreground.away()
        session.away()
    }

    /** Журнал похода уходит сразу: процесс, убитый в фоне, не унесёт его с собой. */
    fun flushRun() = expedition.flushRun()

    /** Вход встретил сервер новее сборки (3.74.0): проверка обновлений идёт сразу. */
    val newerServer: Flow<Unit> get() = session.newerServer
    val serverFailed: Flow<Unit> get() = session.serverFailed
}
