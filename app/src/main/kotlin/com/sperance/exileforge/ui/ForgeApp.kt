package com.sperance.exileforge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.display.documentTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.*
import com.sperance.exileforge.ui.components.LocalEntityPageLoader
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.RefusalLine
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import androidx.compose.ui.text.style.TextOverflow
import com.sperance.exileforge.ui.screens.auction.AuctionScreen
import com.sperance.exileforge.ui.screens.session.AuthScreen
import com.sperance.exileforge.ui.screens.session.CharacterSelectScreen
import com.sperance.exileforge.ui.screens.admin.AdminScreen
import com.sperance.exileforge.ui.screens.catalog.CatalogScreen
import com.sperance.exileforge.ui.screens.checks.ChecksScreen
import com.sperance.exileforge.ui.screens.craft.CraftScreen
import com.sperance.exileforge.ui.screens.editor.EditorScreen
import com.sperance.exileforge.ui.screens.expedition.ExpeditionPlay
import com.sperance.exileforge.ui.screens.expedition.ExpeditionScreen
import com.sperance.exileforge.ui.screens.expedition.AtlasScreen
import com.sperance.exileforge.ui.screens.crafts.CraftsScreen
import com.sperance.exileforge.ui.screens.hero.HeroScreen
import com.sperance.exileforge.ui.screens.redemption.RedemptionScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.tree.SkillTreeScreen
import com.sperance.exileforge.ui.theme.*

@Composable fun ForgeApp(vm: ForgeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val expedition by vm.expedition.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    BackHandler(s.admin.editorOpen && !s.busy) { confirmDiscard = true }
    CompositionLocalProvider(LocalEntityPageLoader provides vm::referencePage) {
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
            ?: GameScaffold(s, vm, logs, onDeleteRequest = { confirmDelete = true }, onDiscardRequest = { confirmDiscard = true })
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, containerColor = Panel, titleContentColor = Gold,
        title = { Text(ui("common.delete_record_q")) },
        text = { Text("${s.admin.original?.let(::documentTitle)}\n${s.admin.original?.entityId}\n" + ui("common.delete_record_text")) },
        confirmButton = { TextButton(enabled = !s.busy, onClick = { confirmDelete = false; vm.delete() }) { Text(ui("common.delete"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(ui("common.cancel")) } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, containerColor = Panel, titleContentColor = Gold,
        title = { Text(ui("editor.close_q")) },
        text = { Text(ui("editor.close_text")) },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; vm.closeEditor() }) { Text(ui("common.close")) } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(ui("editor.keep_editing")) } })
    }
    }
}

/** The game proper: the banner, the destinations and whichever tab is open. */
@Composable private fun GameScaffold(s: ForgeState, vm: ForgeViewModel, logs: List<com.sperance.exileforge.core.network.RequestLog>,
    onDeleteRequest: () -> Unit, onDiscardRequest: () -> Unit) {
    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Abyss, tonalElevation = 0.dp,
                modifier = Modifier.drawBehind { drawLine(Gold.copy(alpha = .35f), Offset(0f, 0f), Offset(size.width, 0f), 2f) }) {
                // Five destinations are the game; an administrator gets exactly one more, and
                // everything that used to crowd the bar lives behind it as a button.
                val labels = mapOf(TAB_HERO to ui("nav.hero"), TAB_EXPEDITION to ui("nav.expedition"), TAB_CRAFTS to ui("nav.crafts"),
                    TAB_AUCTION to ui("nav.auction"), TAB_ACCOUNT to ui("nav.account"),
                    TAB_ADMIN to ui("nav.admin"))
                val destinations = PLAYER_TABS + listOfNotNull(TAB_ADMIN.takeIf { s.adminTools })
                val icons = mapOf<Int, ImageVector>(TAB_ACCOUNT to ForgeGlyphs.Portal, TAB_HERO to ForgeGlyphs.Helm, TAB_EXPEDITION to ForgeGlyphs.Swords,
                    TAB_CRAFTS to ForgeGlyphs.Anvil, TAB_AUCTION to ForgeGlyphs.Orb, TAB_ADMIN to ForgeGlyphs.Scroll)
                destinations.forEach { index ->
                    val label = labels.getValue(index)
                    // The tree is the hero's (2.40.0): while it is open, the Hero tab reads as the one chosen.
                    NavigationBarItem(selected = s.tab == index || (index == TAB_HERO && s.tab == TAB_TREE), onClick = { vm.tab(index) },
                        icon = { Icon(icons.getValue(index), null, modifier = Modifier.size(22.dp)) }, label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = GoldBright, selectedTextColor = Gold,
                            indicatorColor = Gold.copy(alpha = .16f), unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().voidBackdrop()) {
            // The craft under way is read with the game, so the banner's plaque knows it from the start.
            LaunchedEffect(s.play.characterId) { if (s.play.characterId.isNotBlank()) vm.loadCrafts(silent = true) }
            ForgeBanner(s, vm)
            if (s.busy || s.reading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
            RefusalLine(s.refusal, vm::dismissMessage)
            when (s.tab) {
                TAB_CATALOG -> CatalogScreen(s, vm)
                TAB_EDITOR -> EditorScreen(s, vm, onDelete = onDeleteRequest, onClose = onDiscardRequest)
                TAB_CHECKS -> ChecksScreen(s, vm, logs)
                TAB_ACCOUNT -> ServerScreen(s, vm, logs)
                TAB_HERO -> HeroScreen(s, vm)
                TAB_EXPEDITION -> ExpeditionScreen(s, vm)
                TAB_CRAFTS -> CraftsScreen(s, vm)
                TAB_TREE -> SkillTreeScreen(s, vm)
                TAB_AUCTION -> AuctionScreen(s, vm)
                TAB_ADMIN -> AdminScreen(s, vm)
                // The forge keeps no place in the bar: it opens from the Hero tab, as the editor,
                // the checks and the catalogue open from the administrator's, and the bar is the
                // way back out of all of them.
                TAB_CRAFT -> CraftScreen(s, vm)
                TAB_REDEMPTION -> RedemptionScreen(s, vm)
            }
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
        Box(Modifier.size(40.dp).border(1.dp, Gold.copy(alpha = .5f), CutCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("EXILE FORGE", style = MaterialTheme.typography.titleLarge, color = GoldBright)
            val hero = s.character
            Text(if (hero == null) ui("app.title") else hero.name + ui("app.hero_level", hero.level),
                style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        WorkBadge(s) { vm.tab(TAB_CRAFTS) }
        IconButton(onClick = { vm.tab(TAB_ACCOUNT) }) {
            Icon(ForgeGlyphs.Portal, ui("nav.account"), tint = if (s.tab == TAB_ACCOUNT) GoldBright else Gold, modifier = Modifier.size(24.dp))
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
            Text(com.sperance.exileforge.ui.screens.crafts.jobTitle(work.job), color = Parchment, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LinearProgressIndicator(progress = { if (work.cycleMillis > 0) ((now + offset - work.settledAt).toFloat() / work.cycleMillis).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 1.dp), color = Gold, trackColor = PanelRaised)
    }
}
