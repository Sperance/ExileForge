package com.sperance.exileforge.ui

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sperance.exileforge.core.app.AppPhase
import com.sperance.exileforge.core.app.Building
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.KeepScreen
import com.sperance.exileforge.core.app.TAB_HERO
import com.sperance.exileforge.core.i18n.LaunchLines
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.core.update.UpdateModel
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.platform.Haptics
import com.sperance.exileforge.platform.StallWatchdog
import com.sperance.exileforge.ui.components.BugSheet
import com.sperance.exileforge.ui.components.HeaderCollapse
import com.sperance.exileforge.ui.components.LocalHeaderCollapse
import com.sperance.exileforge.ui.components.LocalMailOpen
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import com.sperance.exileforge.ui.components.LocalUpdateHint
import com.sperance.exileforge.ui.components.LocalUpdates
import com.sperance.exileforge.ui.components.MailSheet
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.ReportsSheet
import com.sperance.exileforge.ui.components.StartActions
import com.sperance.exileforge.ui.components.StartStages
import com.sperance.exileforge.ui.components.ToastBelow
import com.sperance.exileforge.ui.components.ToastLayer
import com.sperance.exileforge.ui.components.UpdateGate
import com.sperance.exileforge.ui.components.UpdateHintState
import com.sperance.exileforge.ui.components.UpdateHost
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
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
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.progress.ProgressPlace
import com.sperance.exileforge.ui.screens.progress.ProgressPlaceScreen
import com.sperance.exileforge.ui.screens.progress.ProgressScreen
import com.sperance.exileforge.ui.screens.redemption.RedemptionScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.server.SettingsScreen
import com.sperance.exileforge.ui.screens.server.SettingsViewModel
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
@Composable fun ForgeApp(updates: UpdateModel) {
    val shell: ShellViewModel = koinViewModel()
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
    val buzzes = koinInject<Buzzes>()
    val haptics = koinInject<Haptics>()
    LaunchedEffect(buzzes, haptics) { buzzes.flow.collect(haptics::play) }
    val settingsModel = koinViewModel<SettingsViewModel>()
    val route by koinInject<Navigator>().current.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalUpdates provides updates,
        LocalUpdateHint provides remember { UpdateHintState() },
        LocalMotion provides settings.animations,
        LocalSettings provides settings,
        com.sperance.exileforge.ui.components.LocalSettingsEdit provides settingsModel::change,
        com.sperance.exileforge.ui.components.LocalBuzzes provides buzzes,
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
            launchLine = { settingsModel.launchLine(LaunchLines.keys(uiLanguage)) },
        )
        UpdateHost(updates, busy = expedition != null || trial != null || rift.arena != null, barShown = route.bars)
    }
}

/**
 * В ключе экранов - язык и размер словаря: пришедшие с сервера имена перерисовывают экраны, что печатали код. Карта мира строится, как
 * только пришла кампания, вне главного потока; слой пергамента живёт с оболочкой. Хозяева поверх всего: системные листы (отчёт и настройки,
 * аккорд громкости), отчёты, справки, карточки предмета, умения и игрока, экран санкции, письма. Прогрев, поход и испытание - не экраны
 * стека, а состояния игры, что накрывают всё; «герой в бою» ([com.sperance.exileforge.ui.components.LocalInFight]) - заход героя по состоянию сервера ([com.sperance.exileforge.core.model.hero.HeroView.inRun]). Тосты - один слой над всеми ними ([ToastLayer]), сюжетом и прологом тоже.
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
    var mailOpen by remember { mutableStateOf(false) }
    LaunchedEffect(route.phase, game.heroId) { if (route.phase == AppPhase.GAME && game.heroId.isNotBlank()) shell.warmUp() }
    val campaign = game.index?.campaign
    LaunchedEffect(campaign) { campaign?.let { WorldArt.of(it) } }
    ToastLayer(game, shell::dismissMessage, shell::dismissNotice) {
        CompositionLocalProvider(com.sperance.exileforge.ui.components.LocalSystemSheets provides sheets) {
            key(game.session.server, game.sessionEpoch, game.lang, game.world.localeLanguage, game.world.localeStrings) {
                CompositionLocalProvider(
                    LocalMailOpen provides { mailOpen = true },
                    com.sperance.exileforge.ui.components.LocalContentIndex provides game.index,
                    com.sperance.exileforge.ui.components.LocalFlaskBonuses provides remember(game.hero, game.index) { com.sperance.exileforge.core.sheet.FlaskBonuses(game) },
                    com.sperance.exileforge.ui.screens.expedition.world.LocalWorldArtLayer provides com.sperance.exileforge.ui.screens.expedition.world.rememberWorldArtLayer(),
                    com.sperance.exileforge.ui.components.LocalInFight provides (game.hero?.inRun == true),
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
                                                else -> Shell(game, navigator)
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
                    onReports = { sheets.reports = com.sperance.exileforge.ui.components.ReportsView(it) },
                    onSend = { report -> shell.reportBug(report) { drafts.clear(report.kind) } },
                )
            }
            if (mailOpen) com.sperance.exileforge.ui.components.ItemInspectorHost(game) { MailSheet(game) { mailOpen = false } }
            sheets.reports?.let { view -> ReportsSheet(view, issueLinks = game.seesIssues) { sheets.reports = null } }
            if (route.phase == AppPhase.GAME) com.sperance.exileforge.ui.components.LetterHost(game)
            if (route.phase == AppPhase.GAME && expedition == null && trial == null && !rift.open) com.sperance.exileforge.ui.screens.quests.StoryAfterHost(game)
            if (route.phase == AppPhase.GAME && warmup?.finished != false && expedition == null && trial == null) UnfinishedRunHost()
        }
    }
}

/** Тост под шапкой игры: оболочка с баннером опускает его ниже баннера. */
private val BANNER_TOAST_TOP = 60.dp

/**
 * Стек экранов: `NavDisplay` над стеком навигатора. Экраны игры стоят в оболочке - шапка, полоса команды,
 * Путь Изгнанника, полоска героя и нижняя панель; вход, меню героев и атлас - без неё.
 *
 * `NavDisplay` один и стоит на одном месте под `Scaffold`, что есть всегда: экран без оболочки лишь гасит её части. Есть ли оболочка и
 * какая - по верху стека навигатора в том же кадре, что и сам стек: память вкладок ([rememberTabMemory]) - одна на `NavDisplay`, и
 * второй `NavDisplay` на смене оболочки (вход → экран героя) взял бы ключ экрана, пока его держит первый.
 *
 * Корень вкладки хранит прокрутку до возвращения; смена вкладки сдвигает экран и меняющуюся шапку в сторону порядка. Сворачиваемые шапки -
 * одно состояние на вкладку, шапка игры не сворачивается.
 */
@Composable internal fun Shell(game: GameUi, navigator: Navigator) {
    val shell: ShellViewModel = koinViewModel()
    val heroModel: HeroViewModel = koinViewModel()
    val tabs = rememberTabMemory(game.heroId)
    val screens: @Composable (Modifier) -> Unit = { modifier ->
        NavDisplay(
            backStack = navigator.stack,
            modifier = modifier,
            onBack = { navigator.back() },
            entryDecorators = listOf(tabs.decorator, ScreenAwayDecorator),
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
                entry<Route.Chronicle> { com.sperance.exileforge.ui.screens.city.HistoryChronicleScreen() }
                entry<Route.Hall> { CityScreen(Building.HALL) }
                entry<Route.Uniques> { UniquesScreen() }
                entry<Route.Redemption> { RedemptionScreen() }
            },
        )
    }
    val route = navigator.stack.lastOrNull() ?: Route.Auth
    val bars = route.bars
    val collapse = remember(route.tab) { HeaderCollapse() }
    Scaffold(
        containerColor = Ink,
        bottomBar = { if (bars) GameBar(game, route) },
        contentWindowInsets = if (bars) ScaffoldDefaults.contentWindowInsets else WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).then(if (bars) Modifier.imePadding() else Modifier)) {
            Column(Modifier.fillMaxSize().then(if (bars) Modifier.voidBackdrop().nestedScroll(collapse.connection) else Modifier)) {
                if (bars) {
                    TabShifted(route, look = { it.tab == TAB_HERO }) { ForgeBanner(game, it) }
                    if (game.busy || game.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
                }
                CompositionLocalProvider(LocalHeaderCollapse provides collapse) {
                    if (bars) {
                        TabShifted(route, look = { it.root?.tab }) { shown ->
                            Column {
                                HeroChrome(shown.tab)
                                com.sperance.exileforge.ui.screens.expedition.ExpeditionChrome(shown)
                            }
                        }
                    }
                    screens(Modifier.weight(1f).fillMaxWidth())
                }
            }
            if (bars) ToastBelow(BANNER_TOAST_TOP)
        }
    }
}
