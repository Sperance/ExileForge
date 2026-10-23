package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.EffectOrigins
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.core.sheet.SheetShift
import com.sperance.exileforge.core.sheet.StatExplainer
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.runSheet
import com.sperance.exileforge.core.sheet.runSources
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.LocalContentIndex
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SumShare
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaHold
import com.sperance.exileforge.ui.screens.expedition.arena.Shelved
import com.sperance.exileforge.ui.screens.expedition.arena.effectTint
import com.sperance.exileforge.ui.screens.expedition.arena.effectTitle
import com.sperance.exileforge.ui.screens.expedition.arena.row
import com.sperance.exileforge.ui.screens.hero.StatSheet
import com.sperance.exileforge.ui.screens.hero.StatTrend
import com.sperance.exileforge.ui.theme.*

/** Где герой сейчас: в бою или на карте; ни там, ни там (вне захода по карте и вне боя) - лист героя без меток. */
private enum class StatsPlace(val badge: String?, val about: String) {
    FIGHT("expedition.stats_fight", "expedition.stats_fight_about"),
    MAP("expedition.stats_map", "expedition.stats_map_about"),
    HERO(null, "expedition.stats_hero_about"),
}

/**
 * Характеристики героя в походе - один список, как сводка экрана «Герой» ([StatSheet]): итог в бою (живое тело героя [FightHud.heroBody]
 * с картой, баффами, дебаффами и глотками) или на карте (все бонусы захода [mapEffects]), против своего листа героя без похода. Шапка -
 * «Характеристики» и метка «В бою» / «На карте», под ней - сколько строк сдвинуто к лучшему и к худшему. Сдвинутая строка раскрывается
 * касанием: «Герой без похода», вклады карты ([origins]: карта, Атлас, древо гильдии, зелье) и боя (эффект, вид и срок) - каждые своей
 * меткой, итог; разбор - кнопкой внутри. Числа идут вживую со снимком боя, строки не переставляются; бой арены стоит, пока лист
 * открыт ([ArenaHold]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(game: GameUi, mapEffects: Map<String, Double>?, origins: EffectOrigins = EffectOrigins.NONE, fight: FightHud? = null, onDismiss: () -> Unit) {
    ArenaHold()
    val sheets = rememberFightSheets(game, mapEffects, origins, fight?.heroBody?.stats)
    val index = LocalContentIndex.current
    val place = when {
        sheets.inFight != null -> StatsPlace.FIGHT
        sheets.onMap != null -> StatsPlace.MAP
        else -> StatsPlace.HERO
    }
    val now = sheets.inFight ?: sheets.onMap ?: sheets.mine
    val effects = fight?.heroEffects
    val fightShares = remember(effects, index) { effects?.let { fightParts(it, index) }.orEmpty() }
    val mapShares = remember(sheets.mapSources, game, game.lang) { mapParts(sheets.mapSources, game) }
    val trend = remember(sheets.mine, now, mapShares, fightShares) {
        StatTrend(sheets.mine.values, SheetShift.of(sheets.mine.values, now.values), mergeParts(mapShares, fightShares))
    }
    val (better, worse) = trend.count(now.values, index)
    val hero = game.hero
    val conditional = remember(hero, index) { hero?.let { h -> index?.let { ConditionalStats.of(h, it) } }.orEmpty() }
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Engraved(ui("expedition.stats_title"))
                    place.badge?.let { PlaceBadge(ui(it)) }
                }
            }
            item {
                MutedText(
                    listOf(ui(place.about), if (better + worse == 0) ui("expedition.stats_unchanged") else ui("expedition.stats_changed", better, worse)).joinToString(" "),
                )
            }
            item { StatSheet(game, now, conditional = conditional, trend = trend, shifts = sheets.mapShifts) }
        }
    }
}

/** Метка места в шапке листа: «В бою» или «На карте». */
@Composable private fun PlaceBadge(text: String) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        color = Rune,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.background(Rune.copy(alpha = .12f), shape).border(1.dp, Rune.copy(alpha = .5f), shape).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Вклады бонусов захода в изменённые цифры ([FightSheets.mapSources]): источник (строка карты, Атлас, узел гильдии, зелье) и доля. */
private fun mapParts(sources: Map<String, List<Shift>>, game: GameUi): Map<String, List<SumShare>> {
    if (sources.isEmpty()) return emptyMap()
    val explainer = StatExplainer(game)
    return sources.mapValues { (key, shifts) ->
        val percent = game.index?.stats?.isPercent(key) == true
        shifts.map { SumShare(explainer.runSourceTitle(it.source), explainer.fmt(key, it.delta, Op.ADD, percent), accent = Rune) }
    }
}

/** Вклады карты и боя по цифре: сперва карты, затем боя. */
private fun mergeParts(map: Map<String, List<SumShare>>, fight: Map<String, List<SumShare>>): Map<String, List<SumShare>> =
    (map.keys + fight.keys).associateWith { key -> map[key].orEmpty() + fight[key].orEmpty() }

/**
 * Листы героя для сравнения: свой ([mine]), под картой ([onMap], без карты - null) со сдвигами по источникам ([mapShifts]) и
 * источниками изменённых цифр ([mapSources]), в бою ([inFight], вне боя - null) и сдвиг боя ([fightShift]) против прежнего листа
 * ([fightBase]: «На карте», без карты - свой) - его стрелки печатает карточка героя арены.
 */
@Immutable
internal class FightSheets(
    val mine: StatFigures,
    val onMap: StatFigures?,
    val mapShifts: Map<String, List<Shift>>,
    val mapSources: Map<String, List<Shift>>,
    val inFight: StatFigures?,
) {
    val fightBase: StatFigures get() = onMap ?: mine
    val fightShift: SheetShift = inFight?.let { SheetShift.of(fightBase.values, it.values) } ?: SheetShift.NONE

    companion object {
        /** Цифры листа [onMap], что не равны своим ([mine]). */
        fun changed(mine: StatFigures, onMap: StatFigures): Set<String> = onMap.values.filter { (key, value) -> (mine.values[key] ?: 0.0) != value }.keys
    }
}

/**
 * Листы для сравнения ([FightSheets]) героя [game] под картой [mapEffects] и телом боя [body] (статы [FightHud.heroBody]); пересчёт - только
 * при смене героя, карты или тела боя, не каждый снимок.
 */
@Composable internal fun rememberFightSheets(game: GameUi, mapEffects: Map<String, Double>?, origins: EffectOrigins, body: Map<String, Double>?): FightSheets {
    val hero = game.hero
    val combat = hero?.combat
    val mine = hero?.figures ?: StatFigures(emptyMap())
    val run = remember(hero, mapEffects, origins) { mapEffects?.let { hero?.runSheet(it, origins) } }
    val onMap = remember(run, combat, mine, mapEffects) { mapEffects?.let { run?.stats()?.let { stats -> combat?.figures(stats) ?: StatFigures(stats) } ?: mine } }
    val shifts = remember(run) { run?.shifts().orEmpty() }
    val sources = remember(hero, shifts, mine, onMap) { onMap?.let { hero?.runSources(FightSheets.changed(mine, it), shifts) }.orEmpty() }
    val inFight = remember(combat, body) { body?.let { stats -> combat?.figures(stats) ?: StatFigures(stats) } }
    return remember(mine, onMap, shifts, sources, inFight) { FightSheets(mine, onMap, shifts, sources, inFight) }
}

/**
 * Из чего сложились цифры героя в бою: каждый эффект героя [effects] ([FightHud.heroEffects]: с таймером и носимый весь бой) - строкой
 * на каждую цифру, что сдвигает его строка: имя, вид, срок и вклад строки; числа строк - из контента [index].
 */
internal fun fightParts(effects: List<EffectView>, index: ContentIndex?): Map<String, List<SumShare>> {
    val parts = LinkedHashMap<String, MutableList<SumShare>>()
    effects.filter { !it.halted }.forEach { view ->
        val row = Shelved.Effect(view).row(index)
        val left = if (view.lasting) ui("fight.shelf.whole") else ui("fight.fact_seconds", fineNumber(view.seconds))
        view.lines.forEach { line ->
            parts.getOrPut(line.stat) { mutableListOf() } += SumShare("${effectTitle(view)} · ${row.source}", SkillText.statLine(line.stat, line.op, line.value, index), left, effectTint(view))
        }
    }
    return parts
}
