package com.sperance.exileforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.jobTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.ui.components.GuideDesk
import com.sperance.exileforge.ui.components.GuideHost
import com.sperance.exileforge.ui.components.LocalGuideDesk
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.ToastHost
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.admin.AdminScreen
import com.sperance.exileforge.ui.screens.city.CityScreen
import com.sperance.exileforge.ui.screens.craft.CraftScreen
import com.sperance.exileforge.ui.screens.crafts.CraftsScreen
import com.sperance.exileforge.ui.screens.expedition.AtlasScreen
import com.sperance.exileforge.ui.screens.expedition.ExpeditionPlay
import com.sperance.exileforge.ui.screens.expedition.ExpeditionScreen
import com.sperance.exileforge.ui.screens.hero.HeroScreen
import com.sperance.exileforge.ui.screens.hero.HeroTab
import com.sperance.exileforge.ui.screens.hero.HeroTabStrip
import com.sperance.exileforge.ui.screens.redemption.RedemptionScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.session.AuthScreen
import com.sperance.exileforge.ui.screens.session.CharacterSelectScreen
import com.sperance.exileforge.ui.screens.skills.GrimoireScreen
import com.sperance.exileforge.ui.screens.tree.SkillTreeScreen
import com.sperance.exileforge.ui.theme.*

@Composable fun ForgeApp(vm: ForgeViewModel) {
    // The first-visit guides (3.14.0): read once per device, one sheet at a time above whatever screen is open.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val guides = remember { GuideDesk(GuideStore(context.applicationContext), scope) }
    CompositionLocalProvider(LocalGuideDesk provides guides) {
        ForgeScreens(vm)
        GuideHost(guides)
    }
}

@Composable private fun ForgeScreens(vm: ForgeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val expedition by vm.expedition.collectAsStateWithLifecycle()
    // Language is part of the key: every cached label is rebuilt in the chosen tongue.
    // The dictionary arrives after the first frame, so its size joins the key: when the server's
    // names land, every screen that printed a bare code is drawn again.
    key(s.account.server, s.account.sessionEpoch, s.lang, s.world.localeStrings) {
        // The two screens above the tabs carry no banner and no bottom bar: there is no character to
        // name in the one and no tab to reach from the other.
        when (s.phase) {
            AppPhase.AUTH -> AuthScreen(s, vm)
            AppPhase.CHARACTERS -> CharacterSelectScreen(s, vm)
            // A campaign run takes the whole screen: no banner and no bar, the scene is the game.
            // The zone's card (2.76.0) lies on the world map in the tab itself.
            AppPhase.GAME -> expedition?.let { ExpeditionPlay(s, vm, it) }
                // The atlas (2.68.0) is a sky of its own, above the tabs.
                ?: s.play.atlas?.let { AtlasScreen(s, vm) }
                ?: GameScaffold(s, vm, logs)
        }
    }
}

/** The game proper: the banner, the destinations and whichever tab is open. */
@Composable private fun GameScaffold(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>) {
    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Abyss, tonalElevation = 0.dp,
                modifier = Modifier.drawBehind { drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = .4f), Color.Transparent)), Offset(0f, 0f), Offset(size.width, 0f), 1f) }) {
                // Four destinations are the game; an administrator gets exactly one more, and
                // the promo codes live behind it as a button.
                val labels = mapOf(TAB_HERO to ui("nav.hero"), TAB_EXPEDITION to ui("nav.expedition"), TAB_CRAFTS to ui("nav.crafts"),
                    TAB_CITY to ui("nav.city"), TAB_ACCOUNT to ui("nav.account"),
                    TAB_ADMIN to ui("nav.admin"))
                val destinations = PLAYER_TABS + listOfNotNull(TAB_ADMIN.takeIf { s.adminTools })
                val icons = mapOf<Int, ImageVector>(TAB_ACCOUNT to ForgeGlyphs.Portal, TAB_HERO to ForgeGlyphs.Helm, TAB_EXPEDITION to ForgeGlyphs.Swords,
                    TAB_CRAFTS to ForgeGlyphs.Anvil, TAB_CITY to ForgeGlyphs.Keep, TAB_ADMIN to ForgeGlyphs.Scroll)
                destinations.forEach { index ->
                    val label = labels.getValue(index)
                    // The tree, the grimoire and the forge are the hero's (3.24.0): while one is open, the Hero tab reads as the one chosen.
                    // The City's tab tapped again from inside a building (3.22.0) walks back out to the square.
                    NavigationBarItem(selected = s.tab == index || (index == TAB_HERO && HeroTab.of(s.tab) != null),
                        onClick = { if (index == TAB_CITY && s.tab == TAB_CITY) vm.building(null) else vm.tab(index) },
                        icon = { Icon(icons.getValue(index), null, modifier = Modifier.size(22.dp)) }, label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = GoldBright, selectedTextColor = Gold,
                            indicatorColor = Gold.copy(alpha = .16f), unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
        Column(Modifier.fillMaxSize().voidBackdrop()) {
            // The craft under way is read with the game, so the banner's plaque knows it from the start.
            LaunchedEffect(s.play.heroId) { if (s.play.heroId.isNotBlank()) vm.loadCrafts(silent = true) }
            ForgeBanner(s, vm)
            if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
            HeroTab.of(s.tab)?.let { HeroTabStrip(it, vm::tab) }
            when (s.tab) {
                TAB_ACCOUNT -> ServerScreen(s, vm, logs)
                TAB_HERO -> HeroScreen(s, vm)
                TAB_EXPEDITION -> ExpeditionScreen(s, vm)
                TAB_CRAFTS -> CraftsScreen(s, vm)
                TAB_TREE -> SkillTreeScreen(s, vm)
                TAB_SKILLS -> GrimoireScreen(s, vm)
                TAB_CITY -> CityScreen(s, vm)
                TAB_ADMIN -> AdminScreen(s, vm)
                // The forge keeps no place in the bar: it opens from the Hero tab, as the promo
                // codes open from the administrator's, and the bar is the way back out of both.
                TAB_CRAFT -> CraftScreen(s, vm)
                TAB_REDEMPTION -> RedemptionScreen(s, vm)
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
 * crafts, and the account sits in its corner — it left the bottom bar.
 */
@Composable private fun ForgeBanner(s: ForgeState, vm: ForgeViewModel) {
    Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Gold.copy(alpha = .10f), Color.Transparent, Gold.copy(alpha = .06f))))
        .padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).border(1.dp, Gold.copy(alpha = .5f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("EXILE FORGE", style = MaterialTheme.typography.titleLarge, color = GoldBright)
            // The loaded hero names themself; before the snapshot lands, the menu's row does.
            val named = s.heroInfo != null || s.heroRow != null
            Text(if (named) s.heroName + ui("app.hero_level", s.heroLevel) else ui("app.title"),
                style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        WorkBadge(s) { vm.tab(TAB_CRAFTS) }
        LinkBadge(s.link, vm::retryLink)
        IconButton(onClick = { vm.tab(TAB_ACCOUNT) }) {
            Icon(ForgeGlyphs.Portal, ui("nav.account"), tint = if (s.tab == TAB_ACCOUNT) GoldBright else Gold, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * The link to the server (3.30.0): a small crossed cloud while it cannot be reached, and how many commands wait
 * to be sent. Nothing at all while the server answers and nothing waits. A tap asks the server again at once.
 */
@Composable private fun LinkBadge(link: LinkState, onRetry: () -> Unit) {
    if (!link.offline && link.waiting.isEmpty()) return
    val tint = if (link.offline) LifeRed else Gold
    val label = if (link.offline) ui("link.offline") else ui("link.waiting", link.waiting.size)
    Row(Modifier.clickable(onClickLabel = ui("link.retry"), onClick = onRetry).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(if (link.offline) Icons.Outlined.CloudOff else Icons.Outlined.CloudUpload, label, tint = tint, modifier = Modifier.size(18.dp))
        if (link.waiting.isNotEmpty()) Text(link.waiting.size.toString(), color = tint, fontSize = 11.sp)
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
            Text(jobTitle(work.job), color = Parchment, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LinearProgressIndicator(progress = { if (work.cycleMillis > 0) ((now + offset - work.settledAt).toFloat() / work.cycleMillis).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 1.dp), color = Gold, trackColor = PanelRaised)
    }
}
