package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.MapLineKind
import com.sperance.exileforge.core.campaign.MapStats
import com.sperance.exileforge.core.campaign.TokenState
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.campaign.WorldToken
import com.sperance.exileforge.core.campaign.run.AutoPlan
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.mapDescription
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.MapLaunchState
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Monster
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*

/** Снаряжение входа в зону (3.80.24): карты из сундука, зелье и скарабеи. */
/**
 * The stash's maps of this zone as a ribbon, the empty square first for going in without one; the
 * picked map's rarity, the three figures it pays and its lines marked by kind — red a harm with the
 * share of risk it pays, blue the content, gold a reward. Every number is the rules' own.
 */
@Composable internal fun Maps(game: GameUi, vm: ExpeditionViewModel, index: ContentIndex, zone: Zone, launch: MapLaunchState) {
    val maps = stashMaps(game).filter { it.item.mapZone == zone.code }
    if (maps.isEmpty()) {
        MutedText(ui("expedition.launch_no_maps", zone.level))
        return
    }
    val picked = maps.firstOrNull { it.item.id == launch.picked }
    MapRibbon(maps, picked, enabled = !game.busy, onPick = vm::pickMap)
    Text(
        picked?.view?.title ?: ui("expedition.launch_no_map"),
        color = picked?.let { rarityColor(it.view.rarity.name) } ?: Muted,
        style = MaterialTheme.typography.titleSmall,
    )
    picked ?: return
    val rarity = picked.view.rarity
    val own = index.campaign.maps.rarityBonus[rarity] ?: 0.0
    Text(
        if (own > 0) ui("expedition.launch_rarity_line", rarityTitle(rarity, game.lang), number(own)) else rarityTitle(rarity, game.lang),
        color = rarityColor(rarity.name),
        style = MaterialTheme.typography.labelMedium,
    )
    // A map taken by an influence (3.52.0, server 1.50.0): stronger monsters — the risk pays for them — its influence on the loot;
    // the Abyss (server 1.65.0) only from its orb, with its cracks besides.
    val influence = picked.item.influence?.takeIf { index.campaign.maps.influence.accepts(it) }
    val bonus = LootRoller(index).activeMap(zone.code, picked.view.effects(), rarity, influence = influence)
    influence?.let {
        val rule = index.campaign.maps.influence
        Text(
            ui("expedition.launch_influence", com.sperance.exileforge.core.i18n.loc("enum.EnumInfluence.${it.name}"), number(rule.power), number(rule.items)),
            color = stateColor(it.name.lowercase()),
            style = MaterialTheme.typography.labelMedium,
        )
    }
    Row(Modifier.fillMaxWidth().background(Abyss).border(1.dp, PanelRaised).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
        Figure(bonus.quantity, ui("expedition.launch_quantity"))
        Figure(bonus.rarity, ui("expedition.launch_rarity"))
        Figure(bonus.experience, ui("expedition.launch_experience"))
    }
    lines(picked.view, index).forEach { LineRow(it) }
    MutedText(ui("expedition.launch_spent"))
}

/** The picked map's lines in the order it rolled them; a composite line is a harm if any of its stats is one. */
internal fun lines(map: ItemView, index: ContentIndex): List<MapLine> = map.lines.map { line ->
    val effects = line.definition?.effects.orEmpty()
    val kinds = effects.map { MapStats.kindOf(index, it.stat) }
    val kind = when {
        MapLineKind.HARM in kinds -> MapLineKind.HARM
        MapLineKind.REWARD in kinds -> MapLineKind.REWARD
        else -> MapLineKind.CONTENT
    }
    val risk = effects.withIndex().sumOf { (i, effect) -> MapStats.riskOf(index, effect.stat, line.values.getOrElse(i) { 0.0 }) }
    MapLine(line.text, kind, risk)
}

@Composable internal fun LineRow(line: MapLine) {
    val tint = when (line.kind) {
        MapLineKind.HARM -> LifeRed
        MapLineKind.CONTENT -> Rune
        MapLineKind.REWARD -> Gold
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Rhombus(tint, 7.dp)
        Text(line.text, color = ModBlue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (line.kind == MapLineKind.HARM && line.risk > 0) {
            MutedText(ui("expedition.launch_risk", number(line.risk)), style = MaterialTheme.typography.labelSmall)
        }
        // A hero's buff is paid for from the map's reward (3.16.0, server 1.14.0).
        if (line.risk < 0) MutedText(ui("expedition.launch_cost", number(-line.risk)), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable internal fun Figure(value: Double, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(ui("expedition.launch_percent", number(value)), color = if (value > 0) GoldBright else Muted, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        MutedText(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * The crafts' gifts to the run (3.79.0): one potion of the bag drunk on entering, and with a map up to two scarabs
 * spent with it. A tap picks, a tap again puts back; what each does is the item's own line.
 */
@Composable internal fun Brews(game: GameUi, vm: ExpeditionViewModel, launch: MapLaunchState) {
    val brews = game.index?.rules?.brews ?: return
    val potions = brews.potions.keys.filter { (game.bagAmount(it) ?: 0L) > 0 }
    val scarabs = brews.scarabs.keys.filter { (game.bagAmount(it) ?: 0L) > 0 }
    if (potions.isEmpty() && scarabs.isEmpty()) return
    ForgePanel {
        if (potions.isNotEmpty()) {
            Engraved(ui("brew.potion"))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(potions, key = { it }) { code ->
                    val chosen = launch.potion == code
                    Square(if (chosen) GoldBright else PanelRaised, chosen, !game.busy, itemTitle(code), { vm.pickPotion(code) }) {
                        BagIcon(code, Modifier.size(28.dp))
                    }
                }
            }
            launch.potion?.let { MutedText(itemDescription(it), style = MaterialTheme.typography.bodySmall) }
        }
        if (scarabs.isNotEmpty()) {
            Engraved(ui("brew.scarabs", launch.scarabs.size, brews.scarabsPerMap))
            if (launch.picked == null) {
                MutedText(ui("brew.scarabs_need_map"), style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scarabs, key = { it }) { code ->
                        val set = launch.scarabs.count { it == code }
                        Square(
                            if (set > 0) GoldBright else PanelRaised,
                            set > 0,
                            !game.busy,
                            itemTitle(code),
                            { vm.toggleScarab(code, add = set == 0 || (launch.scarabs.size < brews.scarabsPerMap && set < (game.bagAmount(code) ?: 0L))) },
                        ) {
                            BagIcon(code, Modifier.size(28.dp))
                            if (set > 1) Text("×$set", color = GoldBright, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
                        }
                    }
                }
            }
            launch.scarabs.distinct().forEach { MutedText(itemDescription(it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/** The stash maps of this zone as squares framed in their rarity, the empty one first. */
@Composable internal fun MapRibbon(maps: List<StashMap>, picked: StashMap?, enabled: Boolean, onPick: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp), modifier = Modifier.fillMaxWidth()) {
        item(key = "none") {
            Square(if (picked == null) GoldBright else PanelRaised, picked == null, enabled, ui("expedition.launch_no_map"), { onPick(null) }) {
                Icon(Icons.Outlined.Close, null, tint = Muted, modifier = Modifier.size(20.dp))
            }
        }
        items(maps, key = { it.item.id }) { map ->
            val chosen = map.item.id == picked?.item?.id
            val color = rarityColor(map.view.rarity.name)
            Square(if (chosen) GoldBright else color, chosen, enabled, map.view.title, { onPick(map.item.id) }) {
                ItemIcon(map.view, color, Modifier.size(28.dp))
            }
        }
    }
}

@Composable internal fun Square(frame: Color, chosen: Boolean, enabled: Boolean, label: String, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.size(52.dp).background(Abyss).border(if (chosen) 2.dp else 1.dp, frame)
            .clickable(enabled = enabled, role = Role.RadioButton, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
