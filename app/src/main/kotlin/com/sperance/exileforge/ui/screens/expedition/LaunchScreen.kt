package com.sperance.exileforge.ui.screens.expedition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.mapDescription
import com.sperance.exileforge.core.campaign.mapTitle
import com.sperance.exileforge.core.campaign.monsterTitle
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.MapLineKind
import com.sperance.exileforge.core.model.campaign.MapRule
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.MapLaunchState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.theme.*
import com.sperance.exileforge.core.model.modifier.definition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** One stash map of the location, with the document its row and its lines are drawn from. */
private data class StashMap(val instance: EquipmentInstance, val document: JsonObject)

/** A line of the picked map: the server's sentence, what it is, and what it pays if it is a harm. */
private data class MapLine(val text: String, val kind: MapLineKind, val risk: Double)

/**
 * The launch window as «Портал» (since 2.38.0) — the owner's pick of five mockups. It takes the
 * whole screen, as the run does, with a way back in the corner. The map sits in a portal on top
 * (drawn still, no animation), the stash maps of this location are a ribbon under it with an empty
 * square for going in without one, then the three figures of what the picked map pays, its lines
 * marked by kind — red a harm with the share of risk it pays, blue the content, gold a reward —
 * and under a divider the location itself and its services. Without a map of this location in the
 * stash the portal block folds to a small emblem and the window is the location's description.
 * «Войти в портал» is pinned at the foot. Every number is the server's rule; the entry spends the map.
 */
@Composable fun LaunchScreen(s: ForgeState, vm: ForgeViewModel) {
    val launch = s.play.launch ?: return
    BackHandler(!s.busy) { vm.closeLaunch() }
    LaunchedEffect(launch.mapCode) { if (s.world.campaign == null) vm.loadCampaign(); vm.ensureHero() }
    val view = s.world.campaign
    val map = view?.chapters?.flatMap { it.maps }?.firstOrNull { it.code == launch.mapCode }
    Scaffold(containerColor = Ink,
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                RefusalLine(s.refusal, vm::dismissMessage)
                Button(enabled = map != null && s.play.hero != null && !s.busy, onClick = { vm.startRun(launch.mapCode) }, shape = CutCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)) {
                    Icon(ForgeGlyphs.Portal, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(ui("expedition.launch_go"), style = MaterialTheme.typography.titleMedium)
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::closeLaunch, enabled = !s.busy) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, ui("common.back"), tint = Gold) }
            }
            if (map == null || view == null) { InfoCard(ui("common.loading"), ui("expedition.loading_hint")); return@Column }
            LaunchBody(s, vm, map, launch, view.maps, view.services)
        }
    }
}

@Composable private fun LaunchBody(s: ForgeState, vm: ForgeViewModel, map: CampaignMap, launch: MapLaunchState, rule: MapRule,
                                   services: com.sperance.exileforge.core.model.campaign.ServiceRule) {
    val template = MapRule.templateCode(map.code)
    val maps = s.play.hero?.inventory.orEmpty().filter { !it.equipped && !it.socketed }
        .map { StashMap(it, inventoryDocument(it, s.world.inventoryBases[it.equipmentId])) }
        .filter { it.document.text("slot") == MapRule.SLOT && it.document.text("code") == template }
    val picked = maps.firstOrNull { it.instance.id == launch.picked }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(mapTitle(map.code), color = GoldBright, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(ui("expedition.map_level", map.level), color = Rune, style = MaterialTheme.typography.labelMedium)
        }
        if (maps.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Portal(null, 72.dp) }
            Text(mapDescription(map.code), color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text(ui("expedition.launch_no_maps", map.level), color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Portal(picked, 176.dp) }
            Text(picked?.document?.text("name") ?: ui("expedition.launch_no_map"), color = picked?.let { rarityColor(it.document.text("rarity")) } ?: Muted,
                style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            // The map's rarity in words, and what it pays by itself (2.47.0, server 0.42.0).
            picked?.let { chosen ->
                val rarity = chosen.instance.rarity
                val own = rule.rarityBonus[rarity] ?: 0.0
                Text(if (own > 0) ui("expedition.launch_rarity_line", rarityTitle(rarity, s.lang), number(own)) else rarityTitle(rarity, s.lang),
                    color = rarityColor(rarity), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            MapRibbon(maps, picked, enabled = !s.busy, onPick = vm::pickMap)
            val bonus = picked?.let { rule.bonus(MapEffects.of(it.instance.params, s.world.definitions), it.instance.rarity) }
            Row(Modifier.fillMaxWidth().background(Abyss).border(1.dp, PanelRaised).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                Figure(bonus?.quantity ?: 0.0, ui("expedition.launch_quantity"))
                Figure(bonus?.rarity ?: 0.0, ui("expedition.launch_rarity"))
                Figure(bonus?.experience ?: 0.0, ui("expedition.launch_experience"))
            }
            picked?.let { lines(it, s, rule).forEach { line -> LineRow(line) } }
                ?: Text(mapDescription(map.code), color = Muted, style = MaterialTheme.typography.bodySmall)
            if (picked != null) Text(ui("expedition.launch_spent"), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        OrnateDivider()
        Text(ui("expedition.launch_dwellers", map.monsters.joinToString { monsterTitle(it.code) }), color = Parchment, style = MaterialTheme.typography.bodySmall)
        map.boss?.let { boss ->
            val slain = launch.boss?.alive == false
            Text(if (slain) ui("expedition.launch_boss_slain", monsterTitle(boss.code), untilText(launch.boss!!.respawnAt))
                else ui("expedition.launch_boss_alive", monsterTitle(boss.code)), color = if (slain) Muted else LifeRed, style = MaterialTheme.typography.bodySmall)
        }
        launch.chests?.let { Text(ui("expedition.chests_window", it.left, untilText(it.refreshAt)), color = Muted, style = MaterialTheme.typography.bodySmall) }
        val money = s.play.hero?.character?.money ?: 0L
        PropertyRow(ui("merchant.gold"), money.toString(), Glyph.CURRENCY)
        launch.chests?.let { chests ->
            val price = services.treasurePerLevel * map.level
            if (chests.bought) Text(ui("expedition.treasure_done"), color = Vital, style = MaterialTheme.typography.bodySmall)
            else HoldButton(ui("expedition.treasure_buy", price), Gold, Modifier.fillMaxWidth(), enabled = !s.busy && money >= price) { vm.buyTreasure(map.code) }
        }
        launch.boss?.takeIf { !it.alive }?.let {
            val price = services.summonPerLevel * map.level
            HoldButton(ui("expedition.guardian_summon", price), Gold, Modifier.fillMaxWidth(), enabled = !s.busy && money >= price) { vm.summonGuardian(map.code) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** The picked map's lines in the order it rolled them; a composite line is a harm if any of its stats is one. */
private fun lines(map: StashMap, s: ForgeState, rule: MapRule): List<MapLine> {
    val documents = (map.document["params"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    return map.instance.params.mapIndexed { index, modifier ->
        val effects = s.world.definitions.definition(modifier.modifierId)?.effects.orEmpty()
        val kinds = effects.map { rule.kindOf(it.stat) }
        val kind = when { MapLineKind.HARM in kinds -> MapLineKind.HARM; MapLineKind.REWARD in kinds -> MapLineKind.REWARD; else -> MapLineKind.CONTENT }
        val risk = effects.withIndex().sumOf { (i, effect) -> rule.riskOf(effect.stat, modifier.values.getOrElse(i) { 0.0 }) }
        MapLine(documents.getOrNull(index)?.let { modifierText(it, s.world.definitions) }.orEmpty(), kind, risk)
    }
}

@Composable private fun LineRow(line: MapLine) {
    val tint = when (line.kind) { MapLineKind.HARM -> LifeRed; MapLineKind.CONTENT -> Rune; MapLineKind.REWARD -> Gold }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Rhombus(tint, 7.dp)
        Text(line.text, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (line.kind == MapLineKind.HARM && line.risk > 0)
            Text(ui("expedition.launch_risk", number(line.risk)), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun Figure(value: Double, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(ui("expedition.launch_percent", number(value)), color = if (value > 0) GoldBright else Muted, style = MaterialTheme.typography.headlineSmall)
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** The stash maps of this location as squares framed in their rarity, the empty one first. */
@Composable private fun MapRibbon(maps: List<StashMap>, picked: StashMap?, enabled: Boolean, onPick: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp), modifier = Modifier.fillMaxWidth()) {
        item(key = "none") {
            Square(if (picked == null) GoldBright else PanelRaised, picked == null, enabled, ui("expedition.launch_no_map"), { onPick(null) }) {
                Icon(Icons.Outlined.Close, null, tint = Muted, modifier = Modifier.size(20.dp))
            }
        }
        items(maps, key = { it.instance.id }) { map ->
            val chosen = map.instance.id == picked?.instance?.id
            val color = rarityColor(map.document.text("rarity"))
            Square(if (chosen) GoldBright else color, chosen, enabled, map.document.text("name"), { onPick(map.instance.id) }) {
                ItemIcon(map.document, color, Modifier.size(28.dp))
            }
        }
    }
}

@Composable private fun Square(frame: Color, chosen: Boolean, enabled: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(52.dp).background(Panel).border(if (chosen) 2.dp else 1.dp, frame)
        .clickable(enabled = enabled, role = Role.RadioButton, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

/** The portal, drawn still: a glow, two rings and four studs, with the picked map in its socket or the socket empty. */
@Composable private fun Portal(picked: StashMap?, size: Dp) {
    val socket = picked?.let { rarityColor(it.document.text("rarity")) } ?: Muted
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(this.size.width / 2, this.size.height / 2)
            val r = this.size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Rune.copy(alpha = .5f), Color(0xFF2D4F6B).copy(alpha = .3f), Color.Transparent), centre, r), r, centre)
            drawCircle(Bronze, r * .86f, centre, style = Stroke(1.dp.toPx()))
            drawCircle(Gold, r * .76f, centre, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))))
            listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f).forEach { (x, y) -> drawCircle(Gold, 3.dp.toPx(), Offset(centre.x + x * r * .86f, centre.y + y * r * .86f)) }
        }
        Box(Modifier.size(size * .34f).background(Panel).border(1.5.dp, socket), contentAlignment = Alignment.Center) {
            if (picked != null) ItemIcon(picked.document, socket, Modifier.fillMaxSize(.7f))
            else Icon(ForgeGlyphs.Atlas, null, tint = Muted, modifier = Modifier.fillMaxSize(.55f))
        }
    }
}
