package com.sperance.exileforge.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.app.StallWatchdog
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.components.BugSheet
import com.sperance.exileforge.ui.components.HeaderCollapse
import com.sperance.exileforge.ui.components.LocalBugReport
import com.sperance.exileforge.ui.components.LocalHeaderCollapse
import com.sperance.exileforge.ui.components.LocalMailOpen
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import com.sperance.exileforge.ui.components.LocalUpdates
import com.sperance.exileforge.ui.components.MailSheet
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.ReportsSheet
import com.sperance.exileforge.ui.components.StartActions
import com.sperance.exileforge.ui.components.StartStages
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.UpdateGate
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.screens.admin.AdminScreen
import com.sperance.exileforge.ui.screens.city.CityScreen
import com.sperance.exileforge.ui.screens.city.UniquesScreen
import com.sperance.exileforge.ui.screens.crafts.CraftsScreen
import com.sperance.exileforge.ui.screens.expedition.AtlasScreen
import com.sperance.exileforge.ui.screens.expedition.ExpeditionPlay
import com.sperance.exileforge.ui.screens.expedition.ExpeditionScreen
import com.sperance.exileforge.ui.screens.expedition.TrialScreen
import com.sperance.exileforge.ui.screens.expedition.UnfinishedRunHost
import com.sperance.exileforge.ui.screens.expedition.world.WorldArt
import com.sperance.exileforge.ui.screens.hero.HeroChrome
import com.sperance.exileforge.ui.screens.hero.HeroScreen
import com.sperance.exileforge.ui.screens.progress.ProgressPlace
import com.sperance.exileforge.ui.screens.progress.ProgressPlaceScreen
import com.sperance.exileforge.ui.screens.progress.ProgressScreen
import com.sperance.exileforge.ui.screens.redemption.RedemptionScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.server.SettingsScreen
import com.sperance.exileforge.ui.screens.session.AuthScreen
import com.sperance.exileforge.ui.screens.session.CharacterSelectScreen
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Настройки на всех экранах: размер текста, движение, экран без сна, отклик. Обновление - поверх всего, забег и испытание сперва кончаются;
 * окно запуска держится, пока сервер не ответит, и читает модель без шлюза жизненного цикла. Прогрев героя - то же окно запуска.
 */
@Composable fun ForgeApp(updates: UpdateViewModel) {
    val shell: ShellViewModel = koinViewModel()
    val context = LocalContext.current
    val expedition by shell.run.collectAsStateWithLifecycle()
    val trial by shell.arena.collectAsStateWithLifecycle()
    val rift by shell.rift.collectAsStateWithLifecycle()
    val settings by remember(shell) { shell.game.map { it.settings }.distinctUntilChanged() }.collectAsStateWithLifecycle(GameSettings())
    val base = LocalDensity.current
    val view = LocalView.current
    val lit = when (settings.keepScreen) {
        KeepScreen.ALWAYS -> true
        KeepScreen.NEVER -> false
        KeepScreen.EXPEDITION -> expedition != null || trial != null || rift.arena != null
    }
    DisposableEffect(view, lit) {
        view.keepScreenOn = lit
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(shell, view) {
        shell.buzzes.collect { kind ->
            view.performHapticFeedback(if (kind == Buzz.DANGER) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    CompositionLocalProvider(
        LocalUpdates provides updates,
        LocalMotion provides settings.animations,
        LocalSettings provides settings,
        LocalDensity provides Density(base.density, base.fontScale * settings.textSize.scale),
    ) {
        com.sperance.exileforge.ui.components.DecorClockHost { ForgeScreens() }
        val window by shell.reach.window.collectAsState()
        val stages by remember(shell) {
            shell.game.map { g -> StartStages(g.index != null, g.world.localeStrings > 0, g.world.iconKeys > 0) }.distinctUntilChanged()
        }.collectAsState(StartStages(contentReady = false, dictionaryReady = false, iconsReady = false))
        val watchdog = koinInject<StallWatchdog>()
        val warmup by shell.warmup.collectAsState()
        val heroId by remember(shell) { shell.game.map { it.heroId }.distinctUntilChanged() }.collectAsState("")
        val actions = remember(shell) { StartActions(shell.reach::retry, shell.reach::release, shell.reach::checkFit) }
        UpdateGate(
            updates,
            busy = expedition != null || trial != null || rift.arena != null,
            stages = stages,
            window = window,
            warmup = warmup?.takeIf { it.heroId == heroId },
            actions = actions,
            diagnostics = watchdog::diagnostics,
        )
    }
}

/**
 * В ключе экранов - язык и размер словаря: пришедшие с сервера имена перерисовывают экраны, что печатали код. Карта мира строится, как
 * только пришла кампания, вне главного потока; слой пергамента живёт с оболочкой. Хозяева поверх всего: системные листы (отчёт и настройки,
 * аккорд громкости), отчёты, справки, карточки предмета, умения и игрока, экран санкции, письма. Прогрев, поход и испытание - не экраны
 * стека, а состояния игры, что накрывают всё.
 */
@Composable internal fun ForgeScreens() {
    val shell: ShellViewModel = koinViewModel()
    val game by shell.game.collectAsStateWithLifecycle()
    val expedition by shell.run.collectAsStateWithLifecycle()
    val trial by shell.arena.collectAsStateWithLifecycle()
    val rift by shell.rift.collectAsStateWithLifecycle()
    val warming by shell.warmup.collectAsStateWithLifecycle()
    val navigator = koinInject<Navigator>()
    val route by navigator.current.collectAsStateWithLifecycle()
    val warmup = warming?.takeIf { it.heroId == game.heroId }
    val sheets = remember { com.sperance.exileforge.ui.components.SystemSheets() }
    val drafts = koinInject<DraftStore>()
    LaunchedEffect(shell) { shell.bugRequests.collect { sheets.bug = true } }
    var reportsTab by remember { mutableStateOf<com.sperance.exileforge.ui.components.ReportsTab?>(null) }
    var mailOpen by remember { mutableStateOf(false) }
    LaunchedEffect(route.phase, game.heroId) { if (route.phase == AppPhase.GAME && game.heroId.isNotBlank()) shell.warmUp() }
    val campaign = game.index?.campaign
    LaunchedEffect(campaign) { campaign?.let { WorldArt.of(it) } }
    key(game.session.server, game.sessionEpoch, game.lang, game.world.localeLanguage, game.world.localeStrings) {
        CompositionLocalProvider(
            LocalBugReport provides { sheets.bug = true },
            LocalMailOpen provides { mailOpen = true },
            com.sperance.exileforge.ui.components.LocalSystemSheets provides sheets,
            com.sperance.exileforge.ui.screens.expedition.world.LocalWorldArtLayer provides com.sperance.exileforge.ui.screens.expedition.world.rememberWorldArtLayer(),
        ) {
            com.sperance.exileforge.ui.components.FateSightHost(game) {
                com.sperance.exileforge.ui.components.LoreHost(game) {
                    com.sperance.exileforge.ui.components.ItemInspectorHost(game) {
                        com.sperance.exileforge.ui.screens.skills.SkillInspectorHost(game) {
                            com.sperance.exileforge.ui.components.PlayerCardHost(game) {
                                Box(Modifier.fillMaxSize()) {
                                    val warming = warmup?.takeIf { route.phase == AppPhase.GAME && !it.finished }
                                    val run = expedition
                                    val arena = trial
                                    val notice = game.session.notice
                                    when {
                                        notice != null -> com.sperance.exileforge.ui.screens.session.SanctionScreen(notice)
                                        warming != null -> Box(Modifier.fillMaxSize().background(Ink))
                                        run != null -> ExpeditionPlay(run)
                                        arena != null -> TrialScreen(arena)
                                        rift.open -> com.sperance.exileforge.ui.screens.expedition.RiftScreen()
                                        else -> Shell(game, route, navigator) { sheets.bug = true }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (sheets.settings) com.sperance.exileforge.ui.screens.server.SettingsOverlay { sheets.settings = false }
    if (sheets.bug) {
        BugSheet(
            game,
            route,
            expedition,
            drafts,
            onDismiss = { sheets.bug = false },
            onReports = { reportsTab = it },
            onSend = { report -> shell.reportBug(report) { drafts.clear(report.kind) } },
        )
    }
    reportsTab?.let { tab -> ReportsSheet(tab) { reportsTab = null } }
    if (mailOpen) com.sperance.exileforge.ui.components.ItemInspectorHost(game) { MailSheet(game) { mailOpen = false } }
    if (route.phase == AppPhase.GAME) com.sperance.exileforge.ui.components.LetterHost(game)
    if (route.phase == AppPhase.GAME && expedition == null && trial == null && !rift.open) com.sperance.exileforge.ui.screens.quests.StoryAfterHost(game)
    if (route.phase == AppPhase.GAME && warmup?.finished != false && expedition == null && trial == null) UnfinishedRunHost()
}

/**
 * Стек экранов: `NavDisplay` над стеком навигатора. Экраны игры стоят в оболочке - шапка, полоса команды,
 * Путь Изгнанника, полоска героя и нижняя панель; вход, меню героев и атлас - без неё.
 *
 * Корень вкладки хранит прокрутку до возвращения; смена вкладки сдвигает экран и меняющуюся шапку в сторону порядка. Сворачиваемые шапки -
 * одно состояние на вкладку, шапка игры не сворачивается.
 */
@Composable internal fun Shell(game: GameUi, route: Route, navigator: Navigator, onBug: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val heroModel: HeroViewModel = koinViewModel()
    val tabs = rememberTabMemory(game.heroId)
    val screens: @Composable (Modifier) -> Unit = { modifier ->
        NavDisplay(
            backStack = navigator.stack,
            modifier = modifier,
            onBack = { navigator.back() },
            entryDecorators = listOf(tabs.decorator),
            transitionSpec = { tabShift(navigator.tabStep) },
            popTransitionSpec = { tabShift(navigator.tabStep) },
            predictivePopTransitionSpec = { _ -> tabShift(0) },
            entryProvider = entryProvider {
                entry<Route.Auth> { AuthScreen() }
                entry<Route.Characters> { CharacterSelectScreen() }
                entry<Route.Fate> { com.sperance.exileforge.ui.screens.session.FateScreen() }
                entry<Route.Account> { ServerScreen() }
                entry<Route.Settings> { key -> SettingsScreen(key.page) }
                entry<Route.Hero>(metadata = TabMemory.ROOT) { HeroScreen() }
                entry<Route.Tree> { ProgressPlaceScreen(ProgressPlace.TREE) }
                entry<Route.Grimoire> { ProgressPlaceScreen(ProgressPlace.GRIMOIRE) }
                entry<Route.Expedition>(metadata = TabMemory.ROOT) { ExpeditionScreen() }
                entry<Route.Crafts>(metadata = TabMemory.ROOT) { CraftsScreen() }
                entry<Route.Progress>(metadata = TabMemory.ROOT) { ProgressScreen() }
                entry<Route.Forge> { ProgressPlaceScreen(ProgressPlace.FORGE) }
                entry<Route.Trials> { com.sperance.exileforge.ui.screens.expedition.TrialsPage() }
                entry<Route.Atlas> { AtlasScreen() }
                entry<Route.Story> { com.sperance.exileforge.ui.screens.quests.StoryScreen() }
                entry<Route.City>(metadata = TabMemory.ROOT) { CityScreen(null) }
                entry<Route.Quests> { CityScreen(Building.QUESTS) }
                entry<Route.Merchant> { CityScreen(Building.MERCHANT) }
                entry<Route.Auction> { CityScreen(Building.AUCTION) }
                entry<Route.Guild> { CityScreen(Building.GUILD) }
                entry<Route.History> { CityScreen(Building.HISTORY) }
                entry<Route.Chronicle> { CityScreen(Building.CHRONICLE) }
                entry<Route.Hall> { CityScreen(Building.HALL) }
                entry<Route.Uniques> { UniquesScreen() }
                entry<Route.Admin>(metadata = TabMemory.ROOT) { AdminScreen() }
                entry<Route.Redemption> { RedemptionScreen() }
            },
        )
    }
    if (!route.bars) {
        screens(Modifier.fillMaxSize())
        return
    }
    val collapse = remember(route.tab) { HeaderCollapse() }
    Scaffold(containerColor = Ink, bottomBar = { GameBar(game, route) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(Modifier.fillMaxSize().voidBackdrop().nestedScroll(collapse.connection)) {
                TabShifted(route, look = { it.tab == TAB_HERO }) { ForgeBanner(game, it, onBug) }
                if (game.busy || game.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
                CompositionLocalProvider(LocalHeaderCollapse provides collapse) {
                    TabShifted(route, look = { it.root?.tab }) { shown ->
                        Column {
                            HeroChrome(shown.tab)
                            com.sperance.exileforge.ui.screens.expedition.ExpeditionChrome(shown)
                        }
                    }
                    screens(Modifier.weight(1f).fillMaxWidth())
                }
            }
            ToastHost(game, shell::dismissMessage, shell::dismissNotice, Modifier.align(Alignment.TopCenter).padding(top = 60.dp))
        }
    }
}
