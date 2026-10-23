package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.EffectOrigins
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.SheetShift
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.LocalContentIndex
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaHold
import com.sperance.exileforge.ui.screens.expedition.arena.Shelved
import com.sperance.exileforge.ui.screens.expedition.arena.effectTint
import com.sperance.exileforge.ui.screens.expedition.arena.effectTitle
import com.sperance.exileforge.ui.screens.expedition.arena.row
import com.sperance.exileforge.ui.screens.hero.StatPart
import com.sperance.exileforge.ui.screens.hero.StatSheet
import com.sperance.exileforge.ui.screens.hero.StatTrend
import com.sperance.exileforge.ui.theme.*

/** Вкладка листа характеристик: свой лист героя, лист под картой и лист в бою. */
private enum class StatsTab(val key: String) {
    HERO("expedition.stats_hero"),
    MAP("expedition.stats_map"),
    FIGHT("expedition.stats_fight"),
}

/**
 * The hero's figures over a map, in tabs at the foot as the gear's window has them: the hero's own sheet, and the same sheet under
 * the map's and the atlas's effects — [mapEffects], what the run was started with — each changed figure lit beside what it was.
 *
 * Карта ложится на лист боя героя, как в заходе (`HeroBuild`). Бой без карты ([mapEffects] null: испытания, Разлом, башня) - вкладки
 * «На карте» нет. В бою ([fight]) - третья вкладка «В бою», с неё лист и открывается: живое тело героя ([FightHud.heroBody]: баффы,
 * дебаффы, глотки, карта) против «На карте» (без карты - против «Героя»), сдвиг - стрелкой цвета сдвига, строка раскрывается -
 * из каких эффектов сложилась ([fightParts]). Числа идут вживую со снимком боя, строки не переставляются; бой арены стоит, пока
 * лист открыт ([ArenaHold]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(game: GameUi, mapEffects: Map<String, Double>?, origins: EffectOrigins = EffectOrigins.NONE, fight: FightHud? = null, onDismiss: () -> Unit) {
    ArenaHold()
    val sheets = rememberFightSheets(game, mapEffects, origins, fight?.heroBody?.stats)
    val tabs = StatsTab.entries.filter { (it != StatsTab.MAP || sheets.onMap != null) && (it != StatsTab.FIGHT || sheets.inFight != null) }
    var picked by remember { mutableStateOf(if (fight != null) StatsTab.FIGHT else StatsTab.HERO) }
    val tab = picked.takeIf { it in tabs } ?: tabs.first()
    val index = LocalContentIndex.current
    val effects = fight?.heroEffects
    val parts = remember(effects, index) { effects?.let { fightParts(it, index) }.orEmpty() }
    val trend = StatTrend(sheets.fightShift, ui(if (sheets.onMap != null) StatsTab.MAP.key else StatsTab.HERO.key), parts).takeIf { tab == StatsTab.FIGHT }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Engraved(ui(tab.key)) }
                item { MutedText(hint(tab, sheets)) }
                item {
                    when (tab) {
                        StatsTab.HERO -> StatSheet(game, sheets.mine)
                        StatsTab.MAP -> sheets.onMap?.let { StatSheet(game, it, before = sheets.mine.values, shifts = sheets.mapShifts) }
                        StatsTab.FIGHT -> sheets.inFight?.let { StatSheet(game, it, before = sheets.fightBase.values, trend = trend) }
                    }
                }
            }
            TabRow(selectedTabIndex = tabs.indexOf(tab).coerceAtLeast(0), containerColor = Abyss, contentColor = Gold) {
                tabs.forEach { t -> Tab(selected = t == tab, onClick = { picked = t }, text = { Text(ui(t.key)) }) }
            }
        }
    }
}

/** Подпись вкладки: что за лист и сколько цифр сдвинуто против прежнего. */
private fun hint(tab: StatsTab, sheets: FightSheets): String = when (tab) {
    StatsTab.HERO -> ui("expedition.stats_hero_hint")
    StatsTab.MAP -> if (sheets.mapChanged == 0) ui("expedition.stats_map_none") else ui("expedition.stats_map_hint", sheets.mapChanged)
    StatsTab.FIGHT -> if (sheets.fightShift.isEmpty) ui("expedition.stats_fight_none") else ui("expedition.stats_fight_hint", sheets.fightShift.up.size, sheets.fightShift.down.size)
}

/**
 * Листы героя для сравнения: свой ([mine]), под картой ([onMap], без карты - null) со сдвигами по источникам ([mapShifts]), в бою
 * ([inFight], вне боя - null) и сдвиг боя ([fightShift]) против прежнего листа ([fightBase]: «На карте», без карты - свой).
 */
@Immutable
internal class FightSheets(
    val mine: StatFigures,
    val onMap: StatFigures?,
    val mapShifts: Map<String, List<Shift>>,
    val inFight: StatFigures?,
) {
    val fightBase: StatFigures get() = onMap ?: mine
    val fightShift: SheetShift = inFight?.let { SheetShift.of(fightBase.values, it.values) } ?: SheetShift.NONE
    val mapChanged: Int = onMap?.values?.count { (key, value) -> (mine.values[key] ?: 0.0) != value } ?: 0
}

/**
 * Листы для сравнения ([FightSheets]) героя [game] под картой [mapEffects] и телом боя [body] (статы [FightHud.heroBody]); пересчёт - только
 * при смене героя, карты или тела боя, не каждый снимок.
 */
@Composable internal fun rememberFightSheets(game: GameUi, mapEffects: Map<String, Double>?, origins: EffectOrigins, body: Map<String, Double>?): FightSheets {
    val combat = game.hero?.combat
    val mine = game.hero?.figures ?: StatFigures(emptyMap())
    val onMap = remember(combat, mine, mapEffects) { mapEffects?.let { MapEffects.hero(mine.stats, it).let { stats -> combat?.figures(stats) ?: StatFigures(stats) } } }
    val shifts = remember(mine, mapEffects, origins) { mapEffects?.let { MapEffects.heroShifts(mine.stats, it, origins) }.orEmpty() }
    val inFight = remember(combat, body) { body?.let { stats -> combat?.figures(stats) ?: StatFigures(stats) } }
    return remember(mine, onMap, shifts, inFight) { FightSheets(mine, onMap, shifts, inFight) }
}

/**
 * Из чего сложились цифры героя в бою: каждый эффект героя [effects] ([FightHud.heroEffects]: с таймером и носимый весь бой) - строкой
 * на каждую цифру, что сдвигает его строка: имя, вид, срок и вклад строки; числа строк - из контента [index].
 */
internal fun fightParts(effects: List<EffectView>, index: ContentIndex?): Map<String, List<StatPart>> {
    val parts = LinkedHashMap<String, MutableList<StatPart>>()
    effects.filter { !it.halted }.forEach { view ->
        val row = Shelved.Effect(view).row(index)
        val left = if (view.lasting) ui("fight.shelf.whole") else ui("fight.fact_seconds", fineNumber(view.seconds))
        view.lines.forEach { line ->
            parts.getOrPut(line.stat) { mutableListOf() } += StatPart(effectTitle(view), row.source, left, SkillText.statLine(line.stat, line.op, line.value, index), effectTint(view))
        }
    }
    return parts
}
