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
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.MapLaunch
import com.sperance.exileforge.core.campaign.MapLineKind
import com.sperance.exileforge.core.campaign.TokenState
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.campaign.WorldToken
import com.sperance.exileforge.core.character.view
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.mapDescription
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.modifierLine
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.rules.content.AtlasBonuses
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.rules.content.MapInfluenceRule
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

/** Снаряжение входа в зону: карты из сундука, зелье и скарабеи. */
/**
 * The stash's maps of this zone as a ribbon, the empty square first for going in without one; the
 * picked map's rarity, the three figures it pays and its lines marked by kind — red a harm with the
 * share of risk it pays, blue the content, gold a reward. Every number is the rules' own.
 *
 * Тир карты поднимает уровень района: по нему монстры, добыча и мифические захода. Захваченная карта - сильнее монстры и влияние на добычу.
 * Атлас усиливает действующие числа карты, как их сложит сервер на входе.
 */
@Composable internal fun Maps(game: GameUi, vm: ExpeditionViewModel, index: ContentIndex, zone: Zone, launch: MapLaunch) {
    val maps = stashMaps(game).filter { it.item.mapZone == zone.code }
    if (maps.isEmpty()) {
        MutedText(ui("expedition.launch_no_maps", zone.level))
        return
    }
    val picked = maps.firstOrNull { it.item.id == launch.picked }
    MapRibbon(maps, picked, enabled = !game.busy, onPick = vm::pickMap)
    TierGate(index, game.hero?.level)
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
    picked.view.areaLevel?.takeIf { picked.item.mapTier > 0 }?.let { area ->
        Text(ui("expedition.launch_area", picked.item.mapTier, area, zone.level), color = GoldBright, style = MaterialTheme.typography.labelMedium)
    }
    val influence = picked.item.influence?.takeIf { index.campaign.maps.influence.accepts(it) }
    val atlas = remember(game.hero?.info?.atlas, index) { AtlasBonuses.of(index.atlasGraph, game.hero?.info?.atlas.orEmpty(), index::modifier) }
    val loot = LootRoller(index)
    val bonus = loot.activeMap(zone.code, loot.atlasPowers(loot.mapEffects(picked.item, atlas.mapEffect), atlas.effects), rarity, picked.item.mapTier, influence, atlas.effects)
    influence?.let { Captured(it, index.campaign.maps.influence) { stat -> atlas.effects[stat] ?: 0.0 } }
    Row(Modifier.fillMaxWidth().background(Abyss).border(1.dp, PanelRaised).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
        Figure(bonus.quantity, ui("expedition.launch_quantity"))
        Figure(bonus.rarity, ui("expedition.launch_rarity"))
        Figure(bonus.experience, ui("expedition.launch_experience"))
    }
    lines(picked.view, index, atlas.mapEffect).forEach { LineRow(it) }
    MutedText(ui("expedition.launch_spent"))
}

/**
 * Захваченная карта строками, как строки карты: имя влияния его цветом, затем действующие числа правила с узлами атласа [atlas]
 * ([MapInfluenceRule.powerAt], [MapInfluenceRule.itemsAt]) - сила монстров, шанс вещи нести влияние, вещь влияния со стража, и у Бездны -
 * её расщелины сверх окна ([MapInfluenceRule.cracksOf]).
 */
@Composable private fun Captured(influence: Influence, rule: MapInfluenceRule, atlas: (String) -> Double) {
    val tint = stateColor(influence.name.lowercase())
    Text(ui("expedition.capture.title", loc("enum.EnumInfluence.${influence.name}")), color = tint, style = MaterialTheme.typography.labelMedium)
    LineRow(MapLine(ui("expedition.capture.power", number(rule.powerAt(atlas))), MapLineKind.HARM, 0.0))
    LineRow(MapLine(ui("expedition.capture.items", number(rule.itemsAt(atlas))), MapLineKind.REWARD, 0.0))
    LineRow(MapLine(ui("expedition.capture.boss"), MapLineKind.REWARD, 0.0))
    rule.cracksOf(influence).takeIf { it > 0 }?.let { LineRow(MapLine(ui("expedition.capture.cracks", it), MapLineKind.CONTENT, 0.0)) }
}

/**
 * Ворота тиров: высший тир упавшей карты у героя уровня [level] - правило `MapTierRule.heroCap`; ниже предела тиров -
 * строка «до T10; T12 - с 80 ур.», на пределе - ничего.
 */
@Composable private fun TierGate(index: ContentIndex, level: Int?) {
    val tiers = index.campaign.maps.tiers ?: return
    level ?: return
    val cap = tiers.heroCap(level)
    if (cap >= tiers.max) return
    val next = tiers.heroTiers.filter { (from, tier) -> from > level && tier > cap }.minByOrNull { it.key } ?: return
    MutedText(ui("expedition.launch_tier_gate", cap, next.value, next.key), style = MaterialTheme.typography.labelSmall)
}

/**
 * The picked map's lines in the order it rolled them; a composite line is a harm if any of its stats is one.
 *
 * Атлас усиливает только строки `scalable` (печать, флаги и счётные - как есть); риск строки с условием платится, только если условие на
 * карте есть.
 */
internal fun lines(map: ItemView, index: ContentIndex, atlasEffect: Double = 1.0): List<MapLine> {
    val roller = LootRoller(index)
    val bound = map.lines.map { line -> line.definition?.let { def -> def to roller.mapValues(def, line.values, atlasEffect) } }
    val all = bound.filterNotNull().flatMap { (def, values) -> def.bind(values) }.groupingBy { it.first.stat }.fold(0.0) { sum, (_, value) -> sum + value }
    return map.lines.mapIndexed { i, line -> line(index, line, bound[i]?.second, all) }
}

/** Строка карты [line] со значениями [values] (null - как выпала) и меткой риска среди всех строк карты [all]. */
private fun line(index: ContentIndex, line: ItemLine, values: List<Double>?, all: Map<String, Double>): MapLine {
    val effects = line.definition?.effects.orEmpty()
    val kinds = effects.map { MapLineKind.of(index, it.stat) }
    val kind = when {
        MapLineKind.HARM in kinds -> MapLineKind.HARM
        MapLineKind.REWARD in kinds -> MapLineKind.REWARD
        else -> MapLineKind.CONTENT
    }
    val def = line.definition
    val shown = values ?: line.values
    val risk = def?.bind(shown).orEmpty().sumOf { (effect, value) -> MapLineKind.risk(index, effect.stat, value, all) }
    return MapLine(if (def != null && shown != line.values) modifierLine(index, def, shown) else line.text, kind, risk)
}

/** Бафф героя оплачивается наградой карты. */
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
        if (line.risk < 0) MutedText(ui("expedition.launch_cost", number(-line.risk)), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable internal fun Figure(value: Double, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(ui("expedition.launch_percent", signedNumber(value)), color = if (value > 0) GoldBright else Muted, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        MutedText(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * The crafts' gifts to the run: one potion of the bag drunk on entering, and with a map up to two scarabs
 * spent with it. A tap picks, a tap again puts back; what each does is the item's own line.
 * Касание открывает карточку стопки, взять или убрать - её кнопкой.
 */
@Composable internal fun Brews(game: GameUi, vm: ExpeditionViewModel, launch: MapLaunch) {
    val brews = game.index?.rules?.brews ?: return
    val potions = brews.potions.keys.filter { (game.bagAmount(it) ?: 0L) > 0 }
    val scarabs = brews.scarabs.keys.filter { (game.bagAmount(it) ?: 0L) > 0 }
    if (potions.isEmpty() && scarabs.isEmpty()) return
    val inspect = rememberInspect()
    fun brew(code: String, taken: Boolean, run: () -> Unit) = inspect(Inspect.Stack(code, InspectAction(ui(if (taken) "brew.put_back" else "brew.take"), enabled = !game.busy, run = run)))
    ForgePanel {
        if (potions.isNotEmpty()) {
            Engraved(ui("brew.potion"))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(potions, key = { it }) { code ->
                    val chosen = launch.potion == code
                    Square(if (chosen) GoldBright else PanelRaised, chosen, true, itemTitle(code), { brew(code, chosen) { vm.pickPotion(code) } }) {
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
                        val add = set == 0 || (launch.scarabs.size < brews.scarabsPerMap && set < (game.bagAmount(code) ?: 0L))
                        Square(
                            if (set > 0) GoldBright else PanelRaised,
                            set > 0,
                            true,
                            itemTitle(code),
                            { brew(code, !add) { vm.toggleScarab(code, add = add) } },
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

/** The stash maps of this zone as squares framed in their rarity, the empty one first; карта открывает карточку, выбирает её кнопка. */
@Composable internal fun MapRibbon(maps: List<StashMap>, picked: StashMap?, enabled: Boolean, onPick: (String?) -> Unit) {
    val inspect = rememberInspect()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp), modifier = Modifier.fillMaxWidth()) {
        item(key = "none") {
            Square(if (picked == null) GoldBright else PanelRaised, picked == null, enabled, ui("expedition.launch_no_map"), { onPick(null) }) {
                Icon(Icons.Outlined.Close, null, tint = Muted, modifier = Modifier.size(20.dp))
            }
        }
        items(maps, key = { it.item.id }) { map ->
            val chosen = map.item.id == picked?.item?.id
            val color = rarityColor(map.view.rarity.name)
            Square(if (chosen) GoldBright else color, chosen, true, map.view.title, { inspect(Inspect.Copy(map.item, InspectAction(ui("expedition.map_pick"), enabled = enabled && !chosen) { onPick(map.item.id) })) }) {
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
