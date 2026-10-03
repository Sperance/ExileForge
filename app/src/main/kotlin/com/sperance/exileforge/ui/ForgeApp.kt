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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.crafts.CraftsViewModel
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.ui.components.BugSheet
import com.sperance.exileforge.ui.components.ExilePathPlate
import com.sperance.exileforge.ui.components.GuideDesk
import com.sperance.exileforge.ui.components.GuideHost
import com.sperance.exileforge.ui.components.LocalBugReport
import com.sperance.exileforge.ui.components.LocalGuideDesk
import com.sperance.exileforge.ui.components.LocalMailOpen
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import com.sperance.exileforge.ui.components.LocalUpdates
import com.sperance.exileforge.ui.components.MailSheet
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.SuggestionsSheet
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.UpdateGate
import com.sperance.exileforge.ui.components.WarmupScreen
import com.sperance.exileforge.ui.components.destination
import com.sperance.exileforge.ui.components.pathStep
import com.sperance.exileforge.ui.components.pulse
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.admin.AdminScreen
import com.sperance.exileforge.ui.screens.city.CityScreen
import com.sperance.exileforge.ui.screens.crafts.CraftsAwayHost
import com.sperance.exileforge.ui.screens.crafts.CraftsScreen
import com.sperance.exileforge.ui.screens.expedition.AtlasScreen
import com.sperance.exileforge.ui.screens.expedition.ExpeditionPlay
import com.sperance.exileforge.ui.screens.expedition.ExpeditionScreen
import com.sperance.exileforge.ui.screens.expedition.TrialScreen
import com.sperance.exileforge.ui.screens.expedition.world.WorldArt
import com.sperance.exileforge.ui.screens.hero.HeroScreen
import com.sperance.exileforge.ui.screens.hero.HeroTab
import com.sperance.exileforge.ui.screens.hero.HeroTabStrip
import com.sperance.exileforge.ui.screens.progress.ProgressPlace
import com.sperance.exileforge.ui.screens.progress.ProgressPlaceScreen
import com.sperance.exileforge.ui.screens.progress.ProgressScreen
import com.sperance.exileforge.ui.screens.redemption.RedemptionScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.server.SettingsScreen
import com.sperance.exileforge.ui.screens.session.AuthScreen
import com.sperance.exileforge.ui.screens.session.CharacterSelectScreen
import com.sperance.exileforge.ui.screens.skills.GrimoireScreen
import com.sperance.exileforge.ui.screens.tree.SkillTreeScreen
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable fun ForgeApp(vm: ForgeViewModel, updates: UpdateViewModel) {
    // The first-visit guides (3.14.0): read once per device, one sheet at a time above whatever screen is open.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val guideStore = koinInject<GuideStore>()
    val guides = remember { GuideDesk(guideStore, scope) }
    val expedition by vm.expedition.collectAsStateWithLifecycle()
    val trial by vm.trial.collectAsStateWithLifecycle()
    // The settings (3.77.0) that reach every screen: the text's size, the motion, the lit screen and the phone's buzz.
    val settings by remember(vm) { vm.state.map { it.settings }.distinctUntilChanged() }.collectAsStateWithLifecycle(GameSettings())
    val base = LocalDensity.current
    val view = LocalView.current
    val lit = when (settings.keepScreen) {
        KeepScreen.ALWAYS -> true
        KeepScreen.NEVER -> false
        KeepScreen.EXPEDITION -> expedition != null || trial != null
    }
    DisposableEffect(view, lit) {
        view.keepScreenOn = lit
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(vm, view) {
        vm.buzzes.collect { kind ->
            view.performHapticFeedback(if (kind == Buzz.DANGER) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    CompositionLocalProvider(
        LocalGuideDesk provides guides,
        LocalUpdates provides updates,
        LocalMotion provides settings.animations,
        LocalSettings provides settings,
        LocalDensity provides Density(base.density, base.fontScale * settings.textSize.scale),
    ) {
        ForgeScreens(vm)
        GuideHost(guides)
        // Updates (3.72.0): over everything; a run or a trial under way is finished first.
        UpdateGate(updates, busy = expedition != null || trial != null)
    }
}

/**
 * The state as one screen reads it (3.56.0): the very same instance is handed down while the parts the screen draws are
 * unchanged, so Compose skips the screen on a tick that only moved a toast, a refusal strip, another tab's reads or the
 * phase. [reads] names those parts — everything the screen and what it opens read, derived getters included (`index`
 * is `world`, `hero` is `play`, `isAdmin` is `account`, `refreshing` is `busy` and `loading`). A part left out would
 * be shown stale, so the lists below err on the wide side: the common slice is every part but the toasts and the tabs
 * of other screens.
 */
@Composable internal fun ForgeState.sliced(vararg reads: Any?): ForgeState = remember(*reads) { this }

/** The parts every screen may read: the account, the world, the play, the link, the language, the mode and the command in flight. */
internal val ForgeState.common: Array<Any?> get() = arrayOf(busy, loading, lang, mode, account, world, play, link, stashSort, stashHideWorn)

/** A screen with its own toasts reads the notice and the refusal too. */
internal val ForgeState.toasts: Array<Any?> get() = arrayOf(notice, message, error)

@Composable internal fun ForgeScreens(vm: ForgeViewModel) {
    val shell: ShellViewModel = koinViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val expedition by vm.expedition.collectAsStateWithLifecycle()
    val trial by vm.trial.collectAsStateWithLifecycle()
    val navigator = koinInject<Navigator>()
    val route by navigator.current.collectAsStateWithLifecycle()
    // Language is part of the key: every cached label is rebuilt in the chosen tongue.
    // The dictionary arrives after the first frame, so its size joins the key: when the server's
    // names land, every screen that printed a bare code is drawn again.
    // The beetle (3.48.0): in the banner of the game; since 3.57.0 in the own header of every screen without one.
    var bugOpen by remember { mutableStateOf(false) }
    val drafts = koinInject<DraftStore>()
    // Players' suggestions and the inbox (3.73.0): sheets over everything, like the beetle's.
    var suggestionsOpen by remember { mutableStateOf(false) }
    var mailOpen by remember { mutableStateOf(false) }
    LaunchedEffect(s.phase, s.play.heroId) { if (s.phase == AppPhase.GAME && s.play.heroId.isNotBlank()) vm.warmUp() }
    // A level that opens a place says so once (3.76.0): the level is remembered per hero, the first reading only sets it.
    val level = s.heroInfo?.level
    var seen by remember(s.play.heroId) { mutableStateOf<Int?>(null) }
    LaunchedEffect(s.play.heroId, level) {
        if (level == null) return@LaunchedEffect
        seen?.let { before -> if (!s.isTester) Feature.gained(before, level).forEach { shell.announce(ui("unlock.opened", ui(it.title))) } }
        seen = level
    }
    // The world map's art is built as soon as the campaign arrives (3.75.0), away from the main thread: the tab opens on it.
    val campaign = s.index?.campaign
    LaunchedEffect(campaign) { campaign?.let { WorldArt.of(it) } }
    key(s.account.server, s.account.sessionEpoch, s.lang, s.world.localeStrings) {
        CompositionLocalProvider(LocalBugReport provides { bugOpen = true }, LocalMailOpen provides { mailOpen = true }) {
            Box(Modifier.fillMaxSize()) {
                // Прогрев (3.54.0), поход и испытание (3.49.0) - не экраны стека, а состояния игры: они накрывают всё, пока идут.
                val warmup = s.play.warmup?.takeIf { s.phase == AppPhase.GAME && !it.finished }
                val run = expedition
                val arena = trial
                when {
                    warmup != null -> WarmupScreen(warmup)
                    run != null -> ExpeditionPlay(s.sliced(*s.common, *s.toasts, s.logFilter), run)
                    arena != null -> TrialScreen(s.sliced(*s.common, *s.toasts, s.logFilter), arena)
                    else -> Shell(s, vm, logs, route, navigator) { bugOpen = true }
                }
            }
        }
    }
    if (bugOpen) {
        BugSheet(
            s,
            expedition,
            drafts,
            onDismiss = { bugOpen = false },
            onSuggestions = { suggestionsOpen = true },
            onSend = { report -> vm.reportBug(report) { drafts.clear(report.kind) } },
        )
    }
    if (suggestionsOpen) SuggestionsSheet { suggestionsOpen = false }
    if (mailOpen) MailSheet(s) { mailOpen = false }
    // The inbox (3.73.0) is asked at sign-in and every few minutes after, quietly: the envelope counts the unread.
    val mailbox = koinViewModel<FeedbackViewModel>()
    LaunchedEffect(s.account.signedIn, s.account.sessionEpoch) {
        while (s.account.signedIn) {
            mailbox.loadMail()
            kotlinx.coroutines.delay(MAIL_POLL_MS)
        }
    }
    // «Пока вас не было» (3.69.0): the crafts catch-up of an absence, once, over whatever the game shows after the warm-up.
    if (s.phase == AppPhase.GAME && s.play.warmup?.finished != false) CraftsAwayHost(s)
}

/**
 * Стек экранов (3.80.23): `NavDisplay` над стеком навигатора. Экраны игры стоят в оболочке - шапка, полоса команды,
 * Путь Изгнанника, полоска героя и нижняя панель; вход, меню героев и атлас - без неё.
 */
@Composable internal fun Shell(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>, route: Route, navigator: Navigator, onBug: () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val heroModel: HeroViewModel = koinViewModel()
    val craftsModel: CraftsViewModel = koinViewModel()
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
                entry<Route.Auth> { AuthScreen(s.sliced(*s.common, *s.toasts)) }
                entry<Route.Characters> { CharacterSelectScreen(s.sliced(*s.common, *s.toasts)) }
                entry<Route.Account> { ServerScreen(s.sliced(*s.common)) }
                entry<Route.Settings> { SettingsScreen(s.sliced(*s.common), logs) }
                entry<Route.Hero> { HeroScreen(s.sliced(*s.common)) }
                entry<Route.Tree> { SkillTreeScreen(s.sliced(*s.common)) }
                entry<Route.Grimoire> { GrimoireScreen(s.sliced(*s.common)) }
                entry<Route.Expedition> { ExpeditionScreen(s.sliced(*s.common, s.logFilter)) }
                entry<Route.Crafts> { CraftsScreen(s.sliced(*s.common)) }
                entry<Route.Progress> { ProgressScreen(s.sliced(*s.common)) }
                // The forge, the menagerie and the trials open from the hub of «Развитие», «back» leading to it.
                entry<Route.Forge> { ProgressPlaceScreen(ProgressPlace.FORGE, s.sliced(*s.common)) }
                entry<Route.Pets> { ProgressPlaceScreen(ProgressPlace.PETS, s.sliced(*s.common)) }
                entry<Route.Trials> { ProgressPlaceScreen(ProgressPlace.TRIALS, s.sliced(*s.common)) }
                entry<Route.Chronicle> { ProgressPlaceScreen(ProgressPlace.CHRONICLE, s.sliced(*s.common)) }
                // The atlas (2.68.0) is a sky of its own, above the tabs.
                entry<Route.Atlas> { AtlasScreen(s.sliced(*s.common, *s.toasts)) }
                // The City's square and its buildings are one screen that reads which building is open.
                entry<Route.City> { CityScreen(s.sliced(*s.common, s.building, s.guild, s.quests, s.market)) }
                entry<Route.Quests> { CityScreen(s.sliced(*s.common, s.building, s.guild, s.quests, s.market)) }
                entry<Route.Merchant> { CityScreen(s.sliced(*s.common, s.building, s.guild, s.quests, s.market)) }
                entry<Route.Auction> { CityScreen(s.sliced(*s.common, s.building, s.guild, s.quests, s.market)) }
                entry<Route.Guild> { CityScreen(s.sliced(*s.common, s.building, s.guild, s.quests, s.market)) }
                entry<Route.Admin> { AdminScreen(s.sliced(*s.common, s.admin), vm) }
                entry<Route.Redemption> { RedemptionScreen(s.sliced(*s.common, s.admin), vm) }
            },
        )
    }
    if (!route.bars) {
        screens(Modifier.fillMaxSize())
        return
    }
    Scaffold(containerColor = Ink, bottomBar = { GameBar(s) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(Modifier.fillMaxSize().voidBackdrop()) {
                // The craft under way is read with the game, so the banner's plaque knows it from the start.
                LaunchedEffect(s.play.heroId) { if (s.play.heroId.isNotBlank()) craftsModel.load(silent = true) }
                ForgeBanner(s, onBug)
                if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
                // The Exile's Path (3.79.0): the first hour's next step, under the banner on every tab until it is walked.
                ExilePathPlate(s, onGo = shell::tab, onClaim = heroModel::claimPath)
                HeroTab.of(s.tab)?.let { HeroTabStrip(it, locked = { tab -> !s.unlocked(Feature.ofTab(tab)) }, onSelect = shell::tab) }
                screens(Modifier.weight(1f).fillMaxWidth())
            }
            // The toasts float over the screen, under the banner (2.80.0).
            ToastHost(s, shell::dismissMessage, shell::dismissNotice, Modifier.align(Alignment.TopCenter).padding(top = 60.dp))
        }
    }
}

/** The bottom bar: five destinations are the game; an administrator gets exactly one more. A tab tapped again walks back to its root. */
@Composable internal fun GameBar(s: ForgeState) {
    val shell: ShellViewModel = koinViewModel()
    NavigationBar(
        containerColor = Abyss,
        tonalElevation = 0.dp,
        modifier = Modifier.drawBehind { drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = .4f), Color.Transparent)), Offset(0f, 0f), Offset(size.width, 0f), 1f) },
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
        val destinations = PLAYER_TABS + listOfNotNull(TAB_ADMIN.takeIf { s.adminTools })
        val icons = mapOf<Int, ImageVector>(
            TAB_ACCOUNT to ForgeGlyphs.Portal,
            TAB_HERO to ForgeGlyphs.Helm,
            TAB_EXPEDITION to ForgeGlyphs.Swords,
            TAB_CRAFTS to ForgeGlyphs.Anvil,
            TAB_PROGRESS to ForgeGlyphs.Sigil,
            TAB_CITY to ForgeGlyphs.Keep,
            TAB_ADMIN to ForgeGlyphs.Scroll,
        )
        // The tab the Exile's Path sends the player to next pulses while its step waits (3.79.0).
        val beckons = s.pathStep()?.takeIf { !it.second }?.first?.check?.destination
        val beat = pulse(1.18f)
        destinations.forEach { index ->
            val label = labels.getValue(index)
            // The tree and the grimoire are the hero's (3.24.0), the forge, the menagerie and the trials the hub's: while one is
            // open, its tab reads as the one chosen. The City's tab tapped again from inside a building (3.22.0) walks back out
            // to the square, as «Развитие» tapped again from a tile's screen walks back to its hub.
            NavigationBarItem(
                selected = s.tab == index || (index == TAB_HERO && HeroTab.of(s.tab) != null) ||
                    (index == TAB_PROGRESS && ProgressPlace.of(s.tab) != null),
                onClick = { shell.tab(index) },
                icon = {
                    // Free atlas points (3.47.0) mark the tab the atlas opens from: «Развитие».
                    val free = if (index == TAB_PROGRESS) s.atlasState?.available ?: 0 else 0
                    // A tab the hero's level has not opened wears a lock (3.76.0).
                    val locked = !s.unlocked(Feature.ofTab(index))
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
                            tint = if (index == beckons && s.tab != index) GoldBright else LocalContentColor.current,
                            modifier = Modifier.size(22.dp).scale(if (index == beckons && s.tab != index) beat else 1f),
                        )
                    }
                },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoldBright,
                    selectedTextColor = Gold,
                    indicatorColor = Gold.copy(alpha = .16f),
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}

/** How often the inbox is asked again while signed in (3.73.0). */
internal const val MAIL_POLL_MS = 5 * 60_000L
