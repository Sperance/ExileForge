package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.chapterTitle
import com.sperance.exileforge.core.campaign.mapDescription
import com.sperance.exileforge.core.campaign.mapTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.ServiceRule
import com.sperance.exileforge.presentation.state.MapServices
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The expedition tab: the campaign and the way into it.
 *
 * «Кампания» leads straight to the next map the character has not cleared, because that is what
 * the button is pressed for nine times out of ten; the chapter's maps are listed under it for the
 * tenth — a cleared map is played again for its loot. A locked map says so and opens nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ExpeditionScreen(s: ForgeState, vm: ForgeViewModel) {
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero(); vm.loadCampaign() }
    val view = s.world.campaign
    val progress = s.play.campaign
    val ready = view != null && progress != null && s.play.hero != null && !s.busy
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.CAMPAIGN), onRefresh = vm::loadCampaign, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader(ui("expedition.title"), ui("expedition.subtitle"), ForgeGlyphs.Portal) }
            if (view == null || progress == null) {
                item { InfoCard(ui("common.loading"), ui("expedition.loading_hint")) }
                return@LazyColumn
            }
            view.chapters.forEach { chapter ->
                val cleared = chapter.maps.count { it.code in progress.cleared }
                item(key = chapter.code) {
                    ForgePanel {
                        Engraved(chapterTitle(chapter.code))
                        Text(ui("expedition.cleared", cleared, chapter.maps.size), color = Muted, style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { if (chapter.maps.isEmpty()) 0f else cleared / chapter.maps.size.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp), color = Gold, trackColor = PanelRaised)
                        val next = vm.nextCampaignMap()
                        Button(enabled = ready && next != null, onClick = { next?.let { vm.startRun(it.code) } }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(ui("expedition.campaign"), style = MaterialTheme.typography.titleMedium)
                        }
                        next?.let { Text(ui("expedition.next", mapTitle(it.code), it.level), color = Rune, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                items(chapter.maps, key = { it.code }) { map ->
                    val open = map.code in progress.unlocked
                    MapRow(map, open = open, cleared = map.code in progress.cleared, enabled = ready && open,
                        onServices = { vm.openMapServices(map.code) }.takeIf { open && ready }) { vm.startRun(map.code) }
                }
            }
            item { Text(ui("expedition.note"), color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
    val services = s.play.mapServices
    if (services != null && view != null) view.chapters.flatMap { it.maps }.firstOrNull { it.code == services.mapCode }?.let { map ->
        MapServicesSheet(s, map, services, view.services, onTreasure = { vm.buyTreasure(map.code) }, onSummon = { vm.summonGuardian(map.code) }, onDismiss = vm::closeMapServices)
    }
}

/**
 * A map's services for gold (since 2.36.0, server 0.34.0): a treasure map — one more chest this
 * window, once — and summoning a slain guardian back to the exit. The prices are the server's per
 * map level; each is held to confirm, and a refusal is the server's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MapServicesSheet(s: ForgeState, map: CampaignMap, services: MapServices, rule: ServiceRule,
    onTreasure: () -> Unit, onSummon: () -> Unit, onDismiss: () -> Unit) {
    val money = s.play.hero?.character?.money ?: 0L
    val treasure = rule.treasurePerLevel * map.level
    val summon = rule.summonPerLevel * map.level
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(mapTitle(map.code), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            PropertyRow(ui("merchant.gold"), money.toString(), com.sperance.exileforge.core.display.Glyph.CURRENCY)
            Engraved(ui("expedition.treasure"))
            Text(ui("expedition.chests_window", services.chests.left, com.sperance.exileforge.ui.screens.auction.untilText(services.chests.refreshAt)),
                color = Muted, style = MaterialTheme.typography.bodySmall)
            if (services.chests.bought) Text(ui("expedition.treasure_done"), color = Vital, style = MaterialTheme.typography.bodySmall)
            else HoldButton(ui("expedition.treasure_buy", treasure), Gold, Modifier.fillMaxWidth(), enabled = !s.busy && money >= treasure, onHeld = onTreasure)
            Engraved(ui("expedition.guardian"))
            if (services.boss.alive) Text(ui("expedition.guardian_stands"), color = Muted, style = MaterialTheme.typography.bodySmall)
            else {
                Text(ui("expedition.guardian_back", com.sperance.exileforge.ui.screens.auction.untilText(services.boss.respawnAt)), color = Muted, style = MaterialTheme.typography.bodySmall)
                HoldButton(ui("expedition.guardian_summon", summon), Gold, Modifier.fillMaxWidth(), enabled = !s.busy && money >= summon, onHeld = onSummon)
            }
        }
    }
}

/** One map of the chapter: its number, name, level and what lives there, and whether it is open. */
@Composable private fun MapRow(map: CampaignMap, open: Boolean, cleared: Boolean, enabled: Boolean, onServices: (() -> Unit)?, onClick: () -> Unit) {
    val accent = when { cleared -> Vital; open -> Gold; else -> Muted }
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Panel, shape).border(1.dp, if (open && !cleared) Gold.copy(alpha = .5f) else PanelRaised, shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RaritySpine(accent, 4.dp)
        Box(Modifier.size(38.dp).border(1.dp, accent.copy(alpha = .7f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Text(map.order.toString(), color = accent, style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(mapTitle(map.code), color = if (open) GoldBright else Muted, style = MaterialTheme.typography.titleSmall)
            Text(ui("expedition.map_level", map.level), color = Rune, style = MaterialTheme.typography.labelSmall)
            Text(mapDescription(map.code), color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        onServices?.let { IconButton(onClick = it) { Icon(ForgeGlyphs.Coins, ui("expedition.services"), tint = Gold, modifier = Modifier.size(20.dp)) } }
        when {
            cleared -> Icon(Icons.Outlined.CheckCircle, ui("expedition.map_cleared"), tint = Vital, modifier = Modifier.size(20.dp))
            !open -> Icon(Icons.Outlined.Lock, ui("expedition.map_locked"), tint = Muted, modifier = Modifier.size(20.dp))
            else -> Unit
        }
    }
}
