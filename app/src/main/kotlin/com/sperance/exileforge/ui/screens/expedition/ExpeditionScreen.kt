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
import com.sperance.exileforge.core.model.campaign.MapRule
import com.sperance.exileforge.core.model.campaign.ServiceRule
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.monsterTitle
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.presentation.state.MapLaunchState
import com.sperance.exileforge.ui.screens.auction.untilText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                        Button(enabled = ready && next != null, onClick = { next?.let { vm.openLaunch(it.code) } }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(ui("expedition.campaign"), style = MaterialTheme.typography.titleMedium)
                        }
                        next?.let { Text(ui("expedition.next", mapTitle(it.code), it.level), color = Rune, style = MaterialTheme.typography.labelMedium) }
                    }
                }
                items(chapter.maps, key = { it.code }) { map ->
                    val open = map.code in progress.unlocked
                    MapRow(map, open = open, cleared = map.code in progress.cleared, enabled = ready && open) { vm.openLaunch(map.code) }
                }
            }
            item { Text(ui("expedition.note"), color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
    val launch = s.play.launch
    if (launch != null && view != null) view.chapters.flatMap { it.maps }.firstOrNull { it.code == launch.mapCode }?.let { map ->
        LaunchSheet(s, map, launch, view.services, view.maps, onPick = vm::pickMap, onTreasure = { vm.buyTreasure(map.code) },
            onSummon = { vm.summonGuardian(map.code) }, onGo = { vm.startRun(map.code) }, onDismiss = vm::closeLaunch)
    }
}

/**
 * The launch window (since 2.37.0, server 0.35.0): it opens before every run. What the location is
 * — its level, who lives there, whether its guardian stands, how many chests the window holds — then
 * the map slot: every stash map of this location, each with its rolls, and what the picked one pays
 * by the server's own weights. The map services live here too. «В путь» enters on the server, which
 * spends the map; a refusal is the server's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LaunchSheet(s: ForgeState, map: CampaignMap, launch: MapLaunchState, services: ServiceRule, rule: MapRule,
    onPick: (String?) -> Unit, onTreasure: () -> Unit, onSummon: () -> Unit, onGo: () -> Unit, onDismiss: () -> Unit) {
    val money = s.play.hero?.character?.money ?: 0L
    val template = MapRule.templateCode(map.code)
    val maps = s.play.hero?.inventory.orEmpty().filter { !it.equipped && !it.socketed }
        .map { it to inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
        .filter { (_, document) -> document.text("slot") == MapRule.SLOT && document.text("code") == template }
    val picked = maps.firstOrNull { it.first.id == launch.picked }?.first
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(mapTitle(map.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                Text(ui("expedition.map_level", map.level), color = Rune, style = MaterialTheme.typography.labelMedium)
                Text(mapDescription(map.code), color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(ui("expedition.launch_dwellers", map.monsters.joinToString { monsterTitle(it.code) }), color = Parchment, style = MaterialTheme.typography.bodySmall)
                map.boss?.let { boss ->
                    val slain = launch.boss?.alive == false
                    Text(if (slain) ui("expedition.launch_boss_slain", monsterTitle(boss.code), untilText(launch.boss!!.respawnAt))
                        else ui("expedition.launch_boss_alive", monsterTitle(boss.code)), color = if (slain) Muted else LifeRed, style = MaterialTheme.typography.bodySmall)
                }
                launch.chests?.let { Text(ui("expedition.chests_window", it.left, untilText(it.refreshAt)), color = Muted, style = MaterialTheme.typography.bodySmall) }

                Engraved(ui("expedition.launch_map"))
                if (maps.isEmpty()) Text(ui("expedition.launch_no_maps", map.level), color = Muted, style = MaterialTheme.typography.bodySmall)
                else {
                    FilterChip(selected = picked == null, onClick = { onPick(null) }, label = { Text(ui("expedition.launch_no_map")) })
                    maps.forEach { (instance, document) ->
                        ItemRow(document, definitions = s.world.definitions, selected = instance.id == picked?.id, enabled = !s.busy) {
                            onPick(instance.id.takeIf { it != picked?.id })
                        }
                    }
                }
                picked?.let { item ->
                    val bonus = rule.bonus(MapEffects.of(item.params, s.world.definitions))
                    Text(ui("expedition.launch_bonus", number(bonus.quantity), number(bonus.rarity), number(bonus.experience)),
                        color = GoldBright, style = MaterialTheme.typography.labelLarge)
                    Text(ui("expedition.launch_spent"), color = Muted, style = MaterialTheme.typography.bodySmall)
                }

                Engraved(ui("expedition.services"))
                PropertyRow(ui("merchant.gold"), money.toString(), com.sperance.exileforge.core.display.Glyph.CURRENCY)
                launch.chests?.let { chests ->
                    val treasure = services.treasurePerLevel * map.level
                    if (chests.bought) Text(ui("expedition.treasure_done"), color = Vital, style = MaterialTheme.typography.bodySmall)
                    else HoldButton(ui("expedition.treasure_buy", treasure), Gold, Modifier.fillMaxWidth(), enabled = !s.busy && money >= treasure, onHeld = onTreasure)
                }
                launch.boss?.takeIf { !it.alive }?.let {
                    val summon = services.summonPerLevel * map.level
                    HoldButton(ui("expedition.guardian_summon", summon), Gold, Modifier.fillMaxWidth(), enabled = !s.busy && money >= summon, onHeld = onSummon)
                }
            }
            Button(enabled = !s.busy, onClick = onGo, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)) {
                Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(ui(if (picked != null) "expedition.launch_go_map" else "expedition.launch_go"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** One map of the chapter: its number, name, level and what lives there, and whether it is open. */
@Composable private fun MapRow(map: CampaignMap, open: Boolean, cleared: Boolean, enabled: Boolean, onClick: () -> Unit) {
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
        when {
            cleared -> Icon(Icons.Outlined.CheckCircle, ui("expedition.map_cleared"), tint = Vital, modifier = Modifier.size(20.dp))
            !open -> Icon(Icons.Outlined.Lock, ui("expedition.map_locked"), tint = Muted, modifier = Modifier.size(20.dp))
            else -> Unit
        }
    }
}
