package com.sperance.exileforge.ui

import android.view.HapticFeedbackConstants
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
import com.sperance.exileforge.ui.components.StartStages
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.UpdateGate
import com.sperance.exileforge.ui.components.WarmupScreen
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

@Composable fun ForgeApp(updates: UpdateViewModel) {
    val shell: ShellViewModel = koinViewModel()
    val context = LocalContext.current
    val expedition by shell.run.collectAsStateWithLifecycle()
    val trial by shell.arena.collectAsStateWithLifecycle()
    val rift by shell.rift.collectAsStateWithLifecycle()
    // The settings (3.77.0) that reach every screen: the text's size, the motion, the lit screen and the phone's buzz.
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
        // Updates (3.72.0): over everything; a run or a trial under way is finished first.
        // 3.86.0: the start window holds only until the server answers.
        // 3.87.0: the window reads the model without the lifecycle's gate - a frozen collection held it forever.
        val window by shell.reach.window.collectAsState()
        val stages by remember(shell) {
            shell.game.map { g -> StartStages(g.index != null, g.world.localeStrings > 0, g.world.iconKeys > 0) }.distinctUntilChanged()
        }.collectAsState(StartStages(contentReady = false, dictionaryReady = false, iconsReady = false))
        val watchdog = koinInject<StallWatchdog>()
        UpdateGate(updates, busy = expedition != null || trial != null, stages = stages, window = window, onRetry = shell.reach::retry, diagnostics = watchdog::diagnostics)
    }
}

@Composable internal fun ForgeScreens() {
    val shell: ShellViewModel = koinViewModel()
    val game by shell.game.collectAsStateWithLifecycle()
    val expedition by shell.run.collectAsStateWithLifecycle()
    val trial by shell.arena.collectAsStateWithLifecycle()
    val rift by shell.rift.collectAsStateWithLifecycle()
    val warming by shell.warmup.collectAsStateWithLifecycle()
    val navigator = koinInject<Navigator>()
    val route by navigator.current.collectAsStateWithLifecycle()
    // The warm-up of the hero on screen (3.54.0); another hero's is not this one's.
    val warmup = warming?.takeIf { it.heroId == game.heroId }
    // Language is part of the key: every cached label is rebuilt in the chosen tongue.
    // The dictionary arrives after the first frame, so its size joins the key: when the server's
    // names land, every screen that printed a bare code is drawn again.
    // The beetle (3.48.0): in the banner of the game; since 3.57.0 in the own header of every screen without one.
    // Системные листы (4.7.0): отчёт об ошибке и настройки - и из оболочки, и из меню забега
    val sheets = remember { com.sperance.exileforge.ui.components.SystemSheets() }
    val drafts = koinInject<DraftStore>()
    // Аккорд громкости в Activity открывает лист отчёта (4.8.0)
    LaunchedEffect(shell) { shell.bugRequests.collect { sheets.bug = true } }
    // Отчёты (0.14.1, макет A): «Мои» и «Известные» - лист поверх всего, как жук; null - закрыт, иначе открытая вкладка.
    var reportsTab by remember { mutableStateOf<com.sperance.exileforge.ui.components.ReportsTab?>(null) }
    var mailOpen by remember { mutableStateOf(false) }
    LaunchedEffect(route.phase, game.heroId) { if (route.phase == AppPhase.GAME && game.heroId.isNotBlank()) shell.warmUp() }
    // The world map's art is built as soon as the campaign arrives (3.75.0), away from the main thread: the tab opens on it.
    val campaign = game.index?.campaign
    LaunchedEffect(campaign) { campaign?.let { WorldArt.of(it) } }
    // 3.81.0: the dictionary's language joins it too — the Russian and English dictionaries hold the same number of strings,
    // so a switch whose dictionary landed after the first redraw left the server's names in the old language.
    key(game.session.server, game.sessionEpoch, game.lang, game.world.localeLanguage, game.world.localeStrings) {
        CompositionLocalProvider(
            LocalBugReport provides { sheets.bug = true },
            LocalMailOpen provides { mailOpen = true },
            com.sperance.exileforge.ui.components.LocalSystemSheets provides sheets,
            // Пергамент карты мира (4.10.x): слой живёт с оболочкой, вход в «Поход» его только воспроизводит
            com.sperance.exileforge.ui.screens.expedition.world.LocalWorldArtLayer provides com.sperance.exileforge.ui.screens.expedition.world.rememberWorldArtLayer(),
        ) {
            // Судьбоносные строки вещей (4.6.2) читают дар и надетое героя отсюда - и в листах карточек
            com.sperance.exileforge.ui.components.FateSightHost(game) {
                // Листы-справки (3.92.0): умение монстра, проклятие, свойство, фаза - откуда бы их ни открыли
                com.sperance.exileforge.ui.components.LoreHost(game) {
                    // Карточка предмета (4.3.2): любое касание вещи или стопки открывает её здесь, поверх всего
                    com.sperance.exileforge.ui.components.ItemInspectorHost(game) {
                        // Карточка умения (0.14.4, #25): любое умение героя где угодно открывает её здесь, поверх всего
                        com.sperance.exileforge.ui.screens.skills.SkillInspectorHost(game) {
                            // Карточка игрока (4.5.1): имя или строка игрока где угодно открывает её здесь, поверх всего
                            com.sperance.exileforge.ui.components.PlayerCardHost(game) {
                                Box(Modifier.fillMaxSize()) {
                                    // Прогрев (3.54.0), поход и испытание (3.49.0) - не экраны стека, а состояния игры: они накрывают всё, пока идут.
                                    val warming = warmup?.takeIf { route.phase == AppPhase.GAME && !it.finished }
                                    val run = expedition
                                    val arena = trial
                                    val notice = game.session.notice
                                    when {
                                        // Доступ закрыт санкцией (3.88.5): экран санкции накрывает всё, пока игрок его не закроет
                                        notice != null -> com.sperance.exileforge.ui.screens.session.SanctionScreen(notice)

                                        warming != null -> WarmupScreen(warming)

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
    // Настройки поверх забега (4.7.0): из оболочки они - экран стека, из меню забега - этот лист, забег под ним держится
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
    // Вложения письма открывают карточку предмета (4.3.2): лист почты стоит вне хозяина игры - у него свой
    if (mailOpen) com.sperance.exileforge.ui.components.ItemInspectorHost(game) { MailSheet(game) { mailOpen = false } }
    // Новое письмо (4.6.3): из карточки игрока, «Ответить» и рассылки гильдии - лист поверх всего, и поверх ящика
    if (route.phase == AppPhase.GAME) com.sperance.exileforge.ui.components.LetterHost(game)
    // Продолжение сюжета (4.7.0): шаг выполнен - сюжетный экран, когда забег уже не на экране
    if (route.phase == AppPhase.GAME && expedition == null && trial == null && !rift.open) com.sperance.exileforge.ui.screens.quests.StoryAfterHost(game)
    // Незаконченный заход (3.89.0): после прогрева, пока поход не на экране, - продолжить или покинуть.
    if (route.phase == AppPhase.GAME && warmup?.finished != false && expedition == null && trial == null) UnfinishedRunHost()
}

/**
 * Стек экранов (3.80.23): `NavDisplay` над стеком навигатора. Экраны игры стоят в оболочке - шапка, полоса команды,
 * Путь Изгнанника, полоска героя и нижняя панель; вход, меню героев и атлас - без неё.
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
            // Память вкладок (4.x): корень вкладки хранит прокрутку до возвращения; смена вкладки сдвигает экран в сторону порядка
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
                // The forge and the trials open from the hub of «Развитие», «back» leading to it.
                entry<Route.Forge> { ProgressPlaceScreen(ProgressPlace.FORGE) }
                entry<Route.Trials> { com.sperance.exileforge.ui.screens.expedition.TrialsPage() }
                // The atlas (2.68.0) is a sky of its own, above the tabs.
                entry<Route.Atlas> { AtlasScreen() }
                entry<Route.Story> { com.sperance.exileforge.ui.screens.quests.StoryScreen() }
                // The City's square and its buildings are one screen that reads which building is open.
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
    // Сворачиваемые шапки (3.88.7): одно состояние на открытую вкладку, любой список внутри сворачивает их прокруткой вниз.
    val collapse = remember(route.tab) { HeaderCollapse() }
    Scaffold(containerColor = Ink, bottomBar = { GameBar(game, route) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(Modifier.fillMaxSize().voidBackdrop().nestedScroll(collapse.connection)) {
                // Шапка игры с меню не сворачивается (3.88.8): сворачиваются только шапки экранов.
                // Смена вкладки (4.x) сдвигает шапку вместе с экраном, когда её облик меняется (строка героя на «Герое»)
                TabShifted(route, look = { it.tab == TAB_HERO }) { ForgeBanner(game, it, onBug) }
                if (game.busy || game.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
                CompositionLocalProvider(LocalHeaderCollapse provides collapse) {
                    TabShifted(route, look = { it.root?.tab }) { shown ->
                        Column {
                            // Полоса разделов героя (3.90.5) - над персонажем, надетым, тайником и сумкой; сам герой - в шапке игры.
                            HeroChrome(shown.tab)
                            // Полоса страниц «Похода» (4.0.0): карта, испытания и Атлас
                            com.sperance.exileforge.ui.screens.expedition.ExpeditionChrome(shown)
                        }
                    }
                    screens(Modifier.weight(1f).fillMaxWidth())
                }
            }
            // The toasts float over the screen, under the banner (2.80.0).
            ToastHost(game, shell::dismissMessage, shell::dismissNotice, Modifier.align(Alignment.TopCenter).padding(top = 60.dp))
        }
    }
}
