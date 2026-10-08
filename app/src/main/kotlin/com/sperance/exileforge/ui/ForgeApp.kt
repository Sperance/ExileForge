package com.sperance.exileforge.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sperance.exileforge.core.display.workTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.app.StallWatchdog
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.ui.components.BugSheet
import com.sperance.exileforge.ui.components.CollapsibleHeader
import com.sperance.exileforge.ui.components.HeaderCollapse
import com.sperance.exileforge.ui.components.LocalBugReport
import com.sperance.exileforge.ui.components.LocalHeaderCollapse
import com.sperance.exileforge.ui.components.LocalMailOpen
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import com.sperance.exileforge.ui.components.LocalUpdates
import com.sperance.exileforge.ui.components.MailSheet
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.StartStages
import com.sperance.exileforge.ui.components.SuggestionsSheet
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.UpdateGate
import com.sperance.exileforge.ui.components.WarmupScreen
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
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
import com.sperance.exileforge.ui.theme.depthPanel
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
        ForgeScreens()
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
    val logs by shell.logs.collectAsStateWithLifecycle()
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
    var bugOpen by remember { mutableStateOf(false) }
    val drafts = koinInject<DraftStore>()
    // Players' suggestions and the inbox (3.73.0): sheets over everything, like the beetle's.
    var suggestionsOpen by remember { mutableStateOf(false) }
    var mailOpen by remember { mutableStateOf(false) }
    LaunchedEffect(route.phase, game.heroId) { if (route.phase == AppPhase.GAME && game.heroId.isNotBlank()) shell.warmUp() }
    // The world map's art is built as soon as the campaign arrives (3.75.0), away from the main thread: the tab opens on it.
    val campaign = game.index?.campaign
    LaunchedEffect(campaign) { campaign?.let { WorldArt.of(it) } }
    // 3.81.0: the dictionary's language joins it too — the Russian and English dictionaries hold the same number of strings,
    // so a switch whose dictionary landed after the first redraw left the server's names in the old language.
    key(game.session.server, game.sessionEpoch, game.lang, game.world.localeLanguage, game.world.localeStrings) {
        CompositionLocalProvider(LocalBugReport provides { bugOpen = true }, LocalMailOpen provides { mailOpen = true }) {
            // Листы-справки (3.92.0): умение монстра, проклятие, свойство, фаза - откуда бы их ни открыли
            com.sperance.exileforge.ui.components.LoreHost(game) {
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

                        else -> Shell(game, logs, route, navigator) { bugOpen = true }
                    }
                }
            }
        }
    }
    if (bugOpen) {
        BugSheet(
            game,
            route,
            expedition,
            drafts,
            onDismiss = { bugOpen = false },
            onSuggestions = { suggestionsOpen = true },
            onSend = { report -> shell.reportBug(report) { drafts.clear(report.kind) } },
        )
    }
    if (suggestionsOpen) SuggestionsSheet { suggestionsOpen = false }
    if (mailOpen) MailSheet(game) { mailOpen = false }
    // Незаконченный заход (3.89.0): после прогрева, пока поход не на экране, - продолжить или покинуть.
    if (route.phase == AppPhase.GAME && warmup?.finished != false && expedition == null && trial == null) UnfinishedRunHost()
}

/**
 * Стек экранов (3.80.23): `NavDisplay` над стеком навигатора. Экраны игры стоят в оболочке - шапка, полоса команды,
 * Путь Изгнанника, полоска героя и нижняя панель; вход, меню героев и атлас - без неё.
 */
@Composable internal fun Shell(game: GameUi, logs: List<RequestLog>, route: Route, navigator: Navigator, onBug: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val heroModel: HeroViewModel = koinViewModel()
    val screens: @Composable (Modifier) -> Unit = { modifier ->
        NavDisplay(
            backStack = navigator.stack,
            modifier = modifier,
            onBack = { navigator.back() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            popTransitionSpec = { fadeIn() togetherWith fadeOut() },
            predictivePopTransitionSpec = { _ -> fadeIn() togetherWith fadeOut() },
            entryProvider = entryProvider {
                entry<Route.Auth> { AuthScreen() }
                entry<Route.Characters> { CharacterSelectScreen() }
                entry<Route.Account> { ServerScreen() }
                entry<Route.Settings> { key -> SettingsScreen(key.page, logs) }
                entry<Route.Hero> { HeroScreen() }
                entry<Route.Tree> { ProgressPlaceScreen(ProgressPlace.TREE) }
                entry<Route.Grimoire> { ProgressPlaceScreen(ProgressPlace.GRIMOIRE) }
                entry<Route.Expedition> { ExpeditionScreen() }
                entry<Route.Crafts> { CraftsScreen() }
                entry<Route.Progress> { ProgressScreen() }
                // The forge, the menagerie and the trials open from the hub of «Развитие», «back» leading to it.
                entry<Route.Forge> { ProgressPlaceScreen(ProgressPlace.FORGE) }
                entry<Route.Pets> { ProgressPlaceScreen(ProgressPlace.PETS) }
                entry<Route.Trials> { com.sperance.exileforge.ui.screens.expedition.TrialsPage() }
                // The atlas (2.68.0) is a sky of its own, above the tabs.
                entry<Route.Atlas> { AtlasScreen() }
                // The City's square and its buildings are one screen that reads which building is open.
                entry<Route.City> { CityScreen(null) }
                entry<Route.Quests> { CityScreen(Building.QUESTS) }
                entry<Route.Merchant> { CityScreen(Building.MERCHANT) }
                entry<Route.Auction> { CityScreen(Building.AUCTION) }
                entry<Route.Guild> { CityScreen(Building.GUILD) }
                entry<Route.History> { CityScreen(Building.HISTORY) }
                entry<Route.Chronicle> { CityScreen(Building.CHRONICLE) }
                entry<Route.Hall> { CityScreen(Building.HALL) }
                entry<Route.Uniques> { UniquesScreen() }
                entry<Route.Admin> { AdminScreen() }
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
                ForgeBanner(game, route, onBug)
                if (game.busy || game.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
                CompositionLocalProvider(LocalHeaderCollapse provides collapse) {
                    // Полоса разделов героя (3.90.5) - над персонажем, надетым, тайником и сумкой; сам герой - в шапке игры.
                    HeroChrome(route.tab)
                    // Полоса страниц «Похода» (4.0.0): карта, испытания и Атлас
                    com.sperance.exileforge.ui.screens.expedition.ExpeditionChrome(route)
                    screens(Modifier.weight(1f).fillMaxWidth())
                }
            }
            // The toasts float over the screen, under the banner (2.80.0).
            ToastHost(game, shell::dismissMessage, shell::dismissNotice, Modifier.align(Alignment.TopCenter).padding(top = 60.dp))
        }
    }
}

/** The bottom bar: five destinations are the game; an administrator gets exactly one more. A tab tapped again walks back to its root. */
@Composable internal fun GameBar(game: GameUi, route: Route) {
    val shell: ShellViewModel = koinViewModel()
    // Парящая капсула «Глубины» (3.88.7): панель со светом по кромке и тенью, отступом от краёв и жестов системы.
    val capsule = RoundedCornerShape(24.dp)
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0),
        modifier = Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 8.dp).depthPanel(capsule, elevation = 16.dp).clip(capsule).height(68.dp),
    ) {
        // Five destinations are the game; an administrator gets exactly one more, and
        // the promo codes live behind it as a button.
        val labels = mapOf(
            TAB_HERO to ui("nav.hero"),
            TAB_EXPEDITION to ui("nav.expedition"),
            TAB_CRAFTS to ui("nav.crafts"),
            TAB_PROGRESS to ui("nav.progress"),
            TAB_CITY to ui("nav.city"),
            TAB_ACCOUNT to ui("nav.account"),
            TAB_ADMIN to ui("nav.admin"),
        )
        val destinations = PLAYER_TABS + listOfNotNull(TAB_ADMIN.takeIf { game.adminTools })
        val icons = mapOf<Int, ImageVector>(
            TAB_ACCOUNT to ForgeGlyphs.Portal,
            TAB_HERO to ForgeGlyphs.Helm,
            TAB_EXPEDITION to ForgeGlyphs.Swords,
            TAB_CRAFTS to ForgeGlyphs.Anvil,
            TAB_PROGRESS to ForgeGlyphs.Sigil,
            TAB_CITY to ForgeGlyphs.Keep,
            TAB_ADMIN to ForgeGlyphs.Scroll,
        )
        destinations.forEach { index ->
            val label = labels.getValue(index)
            // Выбрана вкладка корня экрана (4.0.1): кузня, зверинец, дерево и гримуар - «Развития», испытания и атлас - «Похода»,
            // здания - Города. Вкладка, нажатая снова изнутри (3.22.0), возвращает к своему корню.
            NavigationBarItem(
                selected = route.root?.tab == index,
                onClick = { shell.tab(index) },
                icon = {
                    // Свободные очки отмечают вкладку, откуда их тратят: атлас (3.47.0) - «Поход», дерево (3.90.5) - «Развитие».
                    val free = when (index) {
                        TAB_EXPEDITION -> game.atlasState?.available ?: 0
                        TAB_PROGRESS -> game.treeState?.available ?: 0
                        else -> 0
                    }
                    // A tab the hero's level has not opened wears a lock (3.76.0).
                    val locked = !game.unlocked(Feature.ofTab(index))
                    BadgedBox(badge = {
                        if (locked) {
                            Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(12.dp))
                        } else if (free > 0) {
                            Badge(containerColor = GoldBright, contentColor = Ink) { Text(free.toString(), fontSize = 9.sp) }
                        }
                    }) {
                        Icon(
                            icons.getValue(index),
                            null,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Ink,
                    selectedTextColor = GoldBright,
                    indicatorColor = Gold,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}
