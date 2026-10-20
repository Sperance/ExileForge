package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.MapLaunch
import com.sperance.exileforge.core.campaign.MapLineKind
import com.sperance.exileforge.core.display.ItemLine
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.MapCapture
import com.sperance.exileforge.core.display.influenceTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.lineNote
import com.sperance.exileforge.core.display.modifierLine
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.AtlasBonuses
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/** Карты зоны [all] из тайника и выбранная из них [picked]: лента, плита и строка уровня читают одно. */
internal data class ZoneMaps(val all: List<StashMap>, val picked: StashMap?)

/** Карты тайника для зоны [zone] и выбранная в окне входа [launch]. */
internal fun zoneMaps(game: GameUi, zone: Zone, launch: MapLaunch): ZoneMaps {
    val all = stashMaps(game).filter { it.item.mapZone == zone.code }
    return ZoneMaps(all, all.firstOrNull { it.item.id == launch.picked })
}

/**
 * Лента снаряжения входа одной строкой: квадрат «без карты» и карты зоны рамкой редкости (тир - в углу), за чертой - зелья и
 * скарабеи сумки. Карта и стопка открывают свою карточку, взять или убрать - её кнопкой; «без карты» выбирается сразу. Нет карт зоны -
 * пунктирный «?», касание объясняет, откуда они; скарабеи без карты погашены, касание говорит, что нужна карта.
 */
@Composable internal fun KitRibbon(game: GameUi, vm: ExpeditionViewModel, zone: Zone, maps: ZoneMaps, launch: MapLaunch) {
    val brews = game.index?.rules?.brews
    val potions = brews?.potions?.keys.orEmpty().filter { (game.bagAmount(it) ?: 0L) > 0 }
    val scarabs = brews?.scarabs?.keys.orEmpty().filter { (game.bagAmount(it) ?: 0L) > 0 }
    val perMap = brews?.scarabsPerMap ?: 0
    val inspect = rememberInspect()
    val enabled = !game.busy
    fun brew(code: String, taken: Boolean, run: () -> Unit) = inspect(Inspect.Stack(code, InspectAction(ui(if (taken) "brew.put_back" else "brew.take"), enabled = enabled) { run.invoke() }))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (maps.all.isEmpty()) {
            item(key = "none") { NoMaps(zone.level) }
        } else {
            item(key = "none") {
                Square(if (maps.picked == null) GoldBright else PanelRaised, maps.picked == null, enabled, ui("expedition.launch_no_map"), { vm.pickMap(null) }) {
                    Icon(Icons.Outlined.Close, null, tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
            items(maps.all, key = { it.item.id }) { map ->
                val chosen = map.item.id == maps.picked?.item?.id
                val color = rarityColor(map.view.rarity.name)
                val pick = InspectAction(ui("expedition.map_pick"), enabled = enabled && !chosen) { vm.pickMap(map.item.id) }
                Square(if (chosen) GoldBright else color, chosen, true, map.view.title, { inspect(Inspect.Copy(map.item, pick)) }) {
                    ItemIcon(map.view, color, Modifier.size(28.dp))
                    map.item.mapTier.takeIf { it > 0 }?.let { Corner(ui("expedition.launch_tier", it), color) }
                }
            }
        }
        if (potions.isNotEmpty() || scarabs.isNotEmpty()) item(key = "rule") { Box(Modifier.size(1.dp, 36.dp).background(PanelRaised)) }
        items(potions, key = { "potion:$it" }) { code ->
            val chosen = launch.potion == code
            Square(if (chosen) GoldBright else PanelRaised, chosen, true, itemTitle(code), { brew(code, chosen) { vm.pickPotion(code) } }) {
                BagIcon(code, Modifier.size(28.dp))
            }
        }
        items(scarabs, key = { "scarab:$it" }) { code ->
            if (launch.picked == null) {
                Tipped({ Tip(itemTitle(code), ui("brew.scarabs_need_map"), Muted) }) {
                    Square(PanelRaised, false, false, itemTitle(code), {}) { BagIcon(code, Modifier.size(28.dp).alpha(.35f)) }
                }
                return@items
            }
            val set = launch.scarabs.count { it == code }
            val add = set == 0 || (launch.scarabs.size < perMap && set < (game.bagAmount(code) ?: 0L))
            Square(if (set > 0) GoldBright else PanelRaised, set > 0, true, itemTitle(code), { brew(code, !add) { vm.toggleScarab(code, add = add) } }) {
                BagIcon(code, Modifier.size(28.dp))
                if (set > 1) Corner("×$set", GoldBright)
            }
        }
    }
}

/** Пунктирный «?» вместо карт: в тайнике нет карты зоны уровня [level]; касание - откуда они берутся. */
@Composable private fun NoMaps(level: Int) {
    Tipped({ Tip("", ui("expedition.launch_no_maps", level), Muted) }) {
        Box(Modifier.size(52.dp).background(Abyss).dashedBorder(Muted, RectangleShape), contentAlignment = Alignment.Center) {
            Text("?", color = Muted, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Подпись в правом нижнем углу квадрата ленты: тир карты или число взятых скарабеев. */
@Composable private fun BoxScope.Corner(text: String, tint: Color) {
    Text(text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 3.dp, vertical = 1.dp))
}

/**
 * Плита выбранной карты: редкость с тиром и две цифры добычи - количество и редкость - в заголовке, затем строки карты в порядке
 * выпадения цветом вида (красная - опасность с долей к добыче, синяя - содержимое, золотая - награда) и захват одной строкой. Прибавка
 * редкости, трата карты на входе, доля строки и разбор захвата - в подсказках по касанию. Все числа - правил.
 *
 * Тир карты, а без тира - уровень карты ставит уровень района: по нему монстры, добыча и мифические захода. Захваченная карта - сильнее
 * монстры и влияние на добычу. Атлас и взятые скарабеи усиливают действующие числа карты, как их сложит сервер на входе.
 */
@Composable internal fun MapPlate(game: GameUi, index: ContentIndex, zone: Zone, picked: StashMap, launch: MapLaunch) {
    val rarity = picked.view.rarity
    val tint = rarityColor(rarity.name)
    val own = index.campaign.maps.rarityBonus[rarity] ?: 0.0
    val influence = picked.item.influence?.takeIf { index.campaign.maps.influence.accepts(it) }
    val atlas = remember(game.hero?.info?.atlas, index) { AtlasBonuses.of(index.atlasGraph, game.hero?.info?.atlas.orEmpty(), index::modifier) }
    val loot = LootRoller(index)
    val scarabs = launch.scarabs.mapNotNull(index.rules.brews::scarab)
    val bonus = loot.activeMap(zone.code, loot.mapSources(picked.item, atlas.mapEffect, atlas.effects, scarabs), rarity, picked.item.mapTier, influence, atlas.effects, picked.item.itemLevel)
    val capture = MapCapture.of(index.campaign.maps.influence, influence) { stat -> atlas.effects[stat] ?: 0.0 }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().background(Abyss, shape).border(1.dp, PanelRaised, shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val title = listOfNotNull(rarityTitle(rarity, game.lang), picked.item.mapTier.takeIf { it > 0 }?.let { ui("expedition.launch_tier", it) }).joinToString(" · ")
            val note = listOfNotNull(ui("expedition.launch_rarity_line", number(own)).takeIf { own > 0 }, ui("expedition.launch_spent")).joinToString("\n")
            TapText(title, tint, MaterialTheme.typography.titleSmall, { Tip("", note, tint) }, Modifier.weight(1f))
            Figure(bonus.quantity, ui("expedition.launch_loot"))
            Figure(bonus.rarity, ui("expedition.launch_rarity"))
        }
        lines(picked.view, index, atlas.mapEffect).forEach { LineRow(it) }
        capture?.let { CaptureLine(it) }
    }
}

/**
 * Захват карты одной строкой: буква и имя влияния его цветом, справа - сила монстров и шанс вещи нести влияние ([MapCapture] - те же
 * числа, что на плашке карточки карты). Касание имени - разбор: сила монстров, шанс влияния, вещь влияния со стража и у Бездны - её
 * расщелины сверх окна.
 */
@Composable private fun CaptureLine(capture: MapCapture) {
    val tint = influenceColor(capture.influence)
    val name = influenceTitle(capture.influence)
    val parts = listOfNotNull(
        ui("expedition.capture.power", number(capture.power)),
        ui("expedition.capture.items", number(capture.items)),
        ui("expedition.capture.boss"),
        capture.cracks.takeIf { it > 0 }?.let { ui("expedition.capture.cracks", it) },
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(16.dp).border(1.dp, tint, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            Text(name.take(1), color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        TapText(ui("expedition.capture.title", name), tint, MaterialTheme.typography.bodySmall, { Tip("", parts.joinToString("\n"), tint) })
        Text(
            ui("expedition.capture.short", number(capture.power), number(capture.items)),
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Ворота тиров: высший тир упавшей карты у героя уровня [level] - правило `MapTierRule.heroCap`; ниже предела тиров -
 * «до T10; T12 - с 80 ур.», на пределе - null.
 */
internal fun tierGate(index: ContentIndex, level: Int?): String? {
    val tiers = index.campaign.maps.tiers ?: return null
    level ?: return null
    val cap = tiers.heroCap(level)
    if (cap >= tiers.max) return null
    val next = tiers.heroTiers.filter { (from, tier) -> from > level && tier > cap }.minByOrNull { it.key } ?: return null
    return ui("expedition.launch_tier_gate", cap, next.value, next.key)
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
    return MapLine(if (def != null && shown != line.values) modifierLine(index, def, shown) else line.text, kind, risk, lineNote(index, def))
}

/**
 * Строка карты ромбом цвета вида; касание текста - пояснение её характеристики ([MapLine.note]); доля опасности к добыче - «8%» справа,
 * касание объясняет; бафф героя оплачивается наградой карты.
 */
@Composable internal fun LineRow(line: MapLine) {
    val tint = when (line.kind) {
        MapLineKind.HARM -> LifeRed
        MapLineKind.CONTENT -> Rune
        MapLineKind.REWARD -> Gold
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Rhombus(tint, 7.dp)
        Tipped(line.note?.let { note -> { Tip("", note, tint) } }, Modifier.weight(1f)) {
            Text(line.text, color = ModBlue, style = MaterialTheme.typography.bodySmall)
        }
        if (line.kind == MapLineKind.HARM && line.risk > 0) {
            val share = number(line.risk)
            TapText(ui("expedition.launch_percent", share), Muted, MaterialTheme.typography.labelSmall, { Tip(ui("expedition.launch_risk", share), ui("expedition.launch_risk_tip"), LifeRed) })
        }
        if (line.risk < 0) MutedText(ui("expedition.launch_cost", number(-line.risk)), style = MaterialTheme.typography.labelSmall)
    }
}

/** Цифра добычи в заголовке плиты: «+62%» ярко и подпись [label] рядом. */
@Composable private fun Figure(value: Double, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("expedition.launch_percent", signedNumber(value)), color = if (value > 0) GoldBright else Muted, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        MutedText(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Значение с подсказкой: [text] цвета [tint] с пунктиром снизу, касание открывает [tip]. Пустой заголовок подсказки не печатается.
 */
@Composable internal fun TapText(text: String, tint: Color, style: TextStyle, tip: () -> Tip, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Tipped(tip, modifier) {
        Text(text, color = tint, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = Modifier.dottedUnderline(tint))
    }
}

/** Пунктир под текстом цвета [color]: знак, что касание объясняет значение. */
internal fun Modifier.dottedUnderline(color: Color): Modifier = drawBehind {
    val y = size.height - 1.dp.toPx()
    val dash = 2.dp.toPx()
    drawLine(color.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)))
}

/** Квадрат ленты 52dp: рамка [frame], выбранный - толще; касание - [onClick] с подписью [label] для чтения с экрана. */
@Composable internal fun Square(frame: Color, chosen: Boolean, enabled: Boolean, label: String, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.size(52.dp).background(Abyss).border(if (chosen) 2.dp else 1.dp, frame)
            .clickable(enabled = enabled, role = Role.RadioButton, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
