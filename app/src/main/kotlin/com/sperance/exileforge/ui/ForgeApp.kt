package com.sperance.exileforge.ui

import android.view.HapticFeedbackConstants
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
import com.sperance.exileforge.core.display.workTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.data.settings.DraftStore
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.features.UpdateViewModel
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

@Composable fun ForgeApp(vm: ForgeViewModel, updates: UpdateViewModel) {
    // The first-visit guides (3.14.0): read once per device, one sheet at a time above whatever screen is open.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val guides = remember { GuideDesk(GuideStore(context.applicationContext), scope) }
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
@Composable private fun ForgeState.sliced(vararg reads: Any?): ForgeState = remember(*reads) { this }

/** The parts every screen may read: the account, the world, the play, the link, the language, the mode and the command in flight. */
private val ForgeState.common: Array<Any?> get() = arrayOf(busy, loading, lang, mode, account, world, play, link, stashSort, stashHideWorn)

/** A screen with its own toasts reads the notice and the refusal too. */
private val ForgeState.toasts: Array<Any?> get() = arrayOf(notice, message, error)

@Composable private fun ForgeScreens(vm: ForgeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val expedition by vm.expedition.collectAsStateWithLifecycle()
    val trial by vm.trial.collectAsStateWithLifecycle()
    // Language is part of the key: every cached label is rebuilt in the chosen tongue.
    // The dictionary arrives after the first frame, so its size joins the key: when the server's
    // names land, every screen that printed a bare code is drawn again.
    // The beetle (3.48.0): in the banner of the game; since 3.57.0 in the own header of every screen without one.
    var bugOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val draftScope = rememberCoroutineScope()
    val drafts = remember { DraftStore(context.applicationContext, draftScope) }
    // Players' suggestions and the inbox (3.73.0): sheets over everything, like the beetle's.
    var suggestionsOpen by remember { mutableStateOf(false) }
    var mailOpen by remember { mutableStateOf(false) }
    LaunchedEffect(s.phase, s.play.heroId) { if (s.phase == AppPhase.GAME && s.play.heroId.isNotBlank()) vm.warmUp() }
    // A level that opens a place says so once (3.76.0): the level is remembered per hero, the first reading only sets it.
    val level = s.heroInfo?.level
    var seen by remember(s.play.heroId) { mutableStateOf<Int?>(null) }
    LaunchedEffect(s.play.heroId, level) {
        if (level == null) return@LaunchedEffect
        seen?.let { before -> if (!s.isTester) Feature.gained(before, level).forEach { vm.announce(ui("unlock.opened", ui(it.title))) } }
        seen = level
    }
    // The world map's art is built as soon as the campaign arrives (3.75.0), away from the main thread: the tab opens on it.
    val campaign = s.index?.campaign
    LaunchedEffect(campaign) { campaign?.let { WorldArt.of(it) } }
    key(s.account.server, s.account.sessionEpoch, s.lang, s.world.localeStrings) {
        CompositionLocalProvider(LocalBugReport provides { bugOpen = true }, LocalMailOpen provides { mailOpen = true }) {
            Box(Modifier.fillMaxSize()) {
                // The two screens above the tabs carry no banner and no bottom bar: there is no character to
                // name in the one and no tab to reach from the other.
                when (s.phase) {
                    AppPhase.AUTH -> AuthScreen(s.sliced(*s.common, *s.toasts), vm)

                    AppPhase.CHARACTERS -> CharacterSelectScreen(s.sliced(*s.common, *s.toasts), vm)

                    // A campaign run takes the whole screen: no banner and no bar, the scene is the game.
                    // The zone's card (2.76.0) lies on the world map in the tab itself.
                    // The warm-up (3.54.0): entering a hero, the loading screen stands until everything is read.
                    AppPhase.GAME -> s.play.warmup?.takeIf { !it.finished }?.let { WarmupScreen(it) }
                        ?: expedition?.let { ExpeditionPlay(s.sliced(*s.common, *s.toasts, s.logFilter), vm, it) }
                        // A trial (3.49.0) is an arena of its own, over the whole screen too.
                        ?: trial?.let { TrialScreen(s.sliced(*s.common, *s.toasts, s.logFilter), vm, it) }
                        // The atlas (2.68.0) is a sky of its own, above the tabs.
                        ?: s.play.atlas?.let { AtlasScreen(s.sliced(*s.common, *s.toasts), vm) }
                        ?: GameScaffold(s, vm, logs) { bugOpen = true }
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
    if (suggestionsOpen) SuggestionsSheet(s, vm) { suggestionsOpen = false }
    if (mailOpen) MailSheet(s, vm) { mailOpen = false }
    // The inbox (3.73.0) is asked at sign-in and every few minutes after, quietly: the envelope counts the unread.
    LaunchedEffect(s.account.signedIn, s.account.sessionEpoch) {
        while (s.account.signedIn) {
            vm.loadMail()
            kotlinx.coroutines.delay(MAIL_POLL_MS)
        }
    }
    // «Пока вас не было» (3.69.0): the crafts catch-up of an absence, once, over whatever the game shows after the warm-up.
    if (s.phase == AppPhase.GAME && s.play.warmup?.finished != false) CraftsAwayHost(s, vm)
}

/** The game proper: the banner, the destinations and whichever tab is open. */
@Composable private fun GameScaffold(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>, onBug: () -> Unit) {
    Scaffold(
        containerColor = Ink,
        bottomBar = {
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
                        onClick = { if (index == TAB_CITY && s.tab == TAB_CITY) vm.building(null) else vm.tab(index) },
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
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(Modifier.fillMaxSize().voidBackdrop()) {
                // The craft under way is read with the game, so the banner's plaque knows it from the start.
                LaunchedEffect(s.play.heroId) { if (s.play.heroId.isNotBlank()) vm.loadCrafts(silent = true) }
                ForgeBanner(s, vm, onBug)
                if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
                // The Exile's Path (3.79.0): the first hour's next step, under the banner on every tab until it is walked.
                ExilePathPlate(s, onGo = vm::tab, onClaim = vm::claimPath)
                HeroTab.of(s.tab)?.let { HeroTabStrip(it, locked = { tab -> !s.unlocked(Feature.ofTab(tab)) }, onSelect = vm::tab) }
                // Each tab is handed its slice (3.56.0): a toast, a refusal or another tab's reads no longer redraw it.
                when (s.tab) {
                    TAB_ACCOUNT -> ServerScreen(s.sliced(*s.common), vm)

                    TAB_SETTINGS -> SettingsScreen(s.sliced(*s.common, s.settings), vm, logs)

                    TAB_HERO -> HeroScreen(s.sliced(*s.common), vm)

                    TAB_EXPEDITION -> ExpeditionScreen(s.sliced(*s.common, s.logFilter), vm)

                    TAB_CRAFTS -> CraftsScreen(s.sliced(*s.common), vm)

                    TAB_PROGRESS -> ProgressScreen(s.sliced(*s.common), vm)

                    TAB_TREE -> SkillTreeScreen(s.sliced(*s.common), vm)

                    TAB_SKILLS -> GrimoireScreen(s.sliced(*s.common), vm)

                    TAB_CITY -> CityScreen(s.sliced(*s.common, s.building, s.guild, s.quests, s.market), vm)

                    TAB_ADMIN -> AdminScreen(s.sliced(*s.common, s.admin), vm)

                    TAB_REDEMPTION -> RedemptionScreen(s.sliced(*s.common, s.admin), vm)

                    // The forge, the menagerie and the trials open from the hub of «Развитие», «back» leading to it.
                    else -> ProgressPlace.of(s.tab)?.let { ProgressPlaceScreen(it, s.sliced(*s.common), vm) }
                }
            }
            // The toasts float over the screen, under the banner (2.80.0).
            ToastHost(s, vm::dismissMessage, vm::dismissNotice, Modifier.align(Alignment.TopCenter).padding(top = 60.dp))
        }
    }
}

/**
 * Title banner: the sigil, the character being played, and the one switch an exile keeps at hand.
 *
 * The subtitle names the character rather than the league, because every button on every tab acts
 * on that one and nothing on screen would otherwise say which.
 *
 * The administrator's mode switch used to sit here too. It moved to the administrator's own tab in
 * 2.3.0: it is not something a player has, and the banner is the one thing on screen every player
 * sees on every tab. The language runes went the same way in 2.4.0, to the Account tab and to the
 * sign-in screen: with a third language they were a crowd, and a language is a setting, not an act.
 * Since 2.48.0 the hero's class is gone from it, a short plaque of the craft under way opens the
 * crafts, and the account sits in its corner — it left the bottom bar. Since 3.75.0 the inbox, the beetle and the
 * account share one «⋮»: with every badge up the name of the game no longer fit.
 */
@Composable private fun ForgeBanner(s: ForgeState, vm: ForgeViewModel, onBug: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Gold.copy(alpha = .10f), Color.Transparent, Gold.copy(alpha = .06f))))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The game's sigil opens the account (3.77.0), where the menu's row was.
        val accountOpen = s.tab == TAB_ACCOUNT
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).border(1.dp, (if (accountOpen) GoldBright else Gold).copy(alpha = .5f), RoundedCornerShape(9.dp))
                .clickable(onClickLabel = ui("nav.account")) { vm.tab(TAB_ACCOUNT) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(ForgeGlyphs.Sigil, ui("nav.account"), tint = if (accountOpen) GoldBright else Gold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("EXILE FORGE", style = MaterialTheme.typography.titleLarge, color = GoldBright, maxLines = 1, softWrap = false)
            // The loaded hero names themself; before the snapshot lands, the menu's row does.
            val named = s.heroInfo != null || s.heroRow != null
            Text(
                if (named) s.heroName + ui("app.hero_level", s.heroLevel) else ui("app.title"),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        WorkBadge(s) { vm.tab(TAB_CRAFTS) }
        LinkBadge(s.link, admin = s.isAdmin, onRetry = vm::retryLink)
        BannerMenu(s.feedback.unread, settingsOpen = s.tab == TAB_SETTINGS, onMail = LocalMailOpen.current, onBug = onBug, onSettings = vm::openSettings)
    }
}

/**
 * The banner's «⋮» (3.75.0, the owner's pick «B» of three mockups): the inbox, the beetle and the account behind one button,
 * so the name of the game fits beside the badges. A letter unread marks the button itself with a dot.
 */
@Composable private fun BannerMenu(unread: Int, settingsOpen: Boolean, onMail: (() -> Unit)?, onBug: () -> Unit, onSettings: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Box {
                Icon(Icons.Outlined.MoreVert, ui("common.more"), tint = if (settingsOpen) GoldBright else Gold, modifier = Modifier.size(24.dp))
                if (unread > 0) Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(LifeRed, CircleShape))
            }
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            fun pick(action: () -> Unit) {
                open = false
                action()
            }
            onMail?.let { mail ->
                DropdownMenuItem(
                    text = { Text(ui("mail.title")) },
                    onClick = { pick(mail) },
                    leadingIcon = { Icon(Icons.Outlined.Mail, null, tint = Gold) },
                    trailingIcon = {
                        if (unread > 0) {
                            Text(
                                if (unread > 9) "9+" else unread.toString(),
                                color = Ink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(LifeRed, CircleShape).padding(horizontal = 6.dp),
                            )
                        }
                    },
                )
            }
            DropdownMenuItem(text = { Text(ui("bug.open")) }, onClick = { pick(onBug) }, leadingIcon = { Icon(Icons.Outlined.BugReport, null, tint = Gold) })
            DropdownMenuItem(
                text = { Text(ui("settings.title"), color = if (settingsOpen) GoldBright else Parchment) },
                onClick = { pick(onSettings) },
                leadingIcon = { Icon(Icons.Outlined.Settings, null, tint = Gold) },
            )
        }
    }
}

/**
 * The link to the server (3.30.0): a small crossed cloud while it cannot be reached, and how many commands wait
 * to be sent. Nothing at all while the server answers and nothing waits. A tap on a cloud that only waits asks
 * the server again at once; on a crossed one (3.79.0) it says why — by cause, with the transport's words for an
 * administrator — and offers «Повторить». The server is asked again by itself meanwhile.
 */
@Composable private fun LinkBadge(link: LinkState, admin: Boolean, onRetry: () -> Unit) {
    if (!link.offline && link.waiting.isEmpty()) return
    val tint = if (link.offline) LifeRed else Gold
    val label = if (link.offline) link.outage?.title ?: ui("link.offline") else ui("link.waiting", link.waiting.size)
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clickable(onClickLabel = ui("link.retry")) { if (link.offline) open = true else onRetry() }.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(if (link.offline) Icons.Outlined.CloudOff else Icons.Outlined.CloudUpload, label, tint = tint, modifier = Modifier.size(18.dp))
            if (link.waiting.isNotEmpty()) Text(link.waiting.size.toString(), color = tint, fontSize = 11.sp)
        }
        DropdownMenu(open && link.offline, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            Column(Modifier.widthIn(max = 280.dp).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, color = LifeRed, style = MaterialTheme.typography.titleSmall)
                link.outage?.let { Text(it.hint, color = Parchment, style = MaterialTheme.typography.bodySmall) }
                Text(ui("link.auto_retry"), color = Muted, style = MaterialTheme.typography.labelSmall)
                if (link.waiting.isNotEmpty()) Text(ui("link.waiting", link.waiting.size), color = Gold, style = MaterialTheme.typography.labelSmall)
                if (admin) link.detail?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = {
                    open = false
                    onRetry()
                }) { Text(ui("link.retry_now"), color = GoldBright) }
            }
        }
    }
}

/** The craft under way, very short: its name and the cycle filling, every frame. Nothing when the hero works at nothing. */
@Composable private fun WorkBadge(s: ForgeState, onClick: () -> Unit) {
    val crafts = s.play.crafts ?: return
    val work = crafts.work ?: return
    val offset = crafts.now - s.play.craftsAt
    val now by produceState(System.currentTimeMillis()) { while (true) withFrameMillis { value = System.currentTimeMillis() } }
    Column(Modifier.widthIn(max = 120.dp).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(ForgeGlyphs.Anvil, null, tint = Gold, modifier = Modifier.size(12.dp))
            Text(workTitle(work.job, work.choice), color = Parchment, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LinearProgressIndicator(
            progress = { if (work.cycleMillis > 0) ((now + offset - work.settledAt).toFloat() / work.cycleMillis).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 1.dp),
            color = Gold,
            trackColor = PanelRaised,
        )
    }
}

/** How often the inbox is asked again while signed in (3.73.0). */
private const val MAIL_POLL_MS = 5 * 60_000L
