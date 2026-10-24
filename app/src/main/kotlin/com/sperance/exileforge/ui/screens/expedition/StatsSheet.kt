package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.EffectOrigins
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.campaign.combat.HeroLadder
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.sheet.ConditionalStats
import com.sperance.exileforge.core.sheet.FightFigures
import com.sperance.exileforge.core.sheet.RunSteps
import com.sperance.exileforge.core.sheet.SheetShift
import com.sperance.exileforge.core.sheet.StatExplainer
import com.sperance.exileforge.core.sheet.StatSheetModel
import com.sperance.exileforge.core.sheet.fightFigures
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.runSheet
import com.sperance.exileforge.core.sheet.runSources
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.LocalContentIndex
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.ToneTag
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaHold
import com.sperance.exileforge.ui.screens.hero.StatSheetWindows
import com.sperance.exileforge.ui.screens.hero.StatTrend
import com.sperance.exileforge.ui.screens.hero.rememberStatSheetState
import com.sperance.exileforge.ui.screens.hero.statSheet
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Где герой сейчас: в бою или на карте; ни там, ни там (вне захода по карте и вне боя) - лист героя без меток. */
private enum class StatsPlace(val badge: String?, val about: String) {
    FIGHT("expedition.stats_fight", "expedition.stats_fight_about"),
    MAP("expedition.stats_map", "expedition.stats_map_about"),
    HERO(null, "expedition.stats_hero_about"),
}

/**
 * Характеристики героя в походе - один список, как сводка экрана «Герой» ([statSheet]): итог в бою (живое тело героя [FightHud.heroBody]
 * с картой, баффами, дебаффами и глотками) или на карте (все бонусы захода [mapEffects]), против своего листа героя без похода. Шапка -
 * «Характеристики» и метка «В бою» / «На карте», под ней - сколько строк сдвинуто к лучшему и к худшему. Сдвинутая строка раскрывается
 * касанием ([StatShares]): «Герой без похода», вклады карты ([origins]: карта, Атлас, древо гильдии, зелье) и боя по частям тела боя
 * ([FightHud.heroLadder]: эффекты по имени, заряды, силы вещей, условия, низкое здоровье, ауры врагов), «Сверх предела» - числами в
 * единице строки, сумма - число строки; разбор - кнопкой внутри. Числа идут вживую со снимком боя, строки не переставляются; бой арены
 * стоит, пока лист открыт ([ArenaHold]).
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
    val hero = game.hero
    val fightFigures = rememberFightFigures(hero, fight?.heroLadder)
    val charges = fight?.heroCharges.orEmpty().filter { it.count > 0 }.associate { it.kind to it.count }
    val steps = remember(sheets, now, fightFigures) {
        RunSteps(sheets.mine, fightFigures?.start ?: sheets.onMap, sheets.mapSources, sheets.onMap != null, fightFigures?.steps.orEmpty(), now)
    }
    val waiting = sheets.inFight != null && fightFigures == null
    val trend = remember(sheets.mine, now, steps, waiting, charges, game, game.lang) {
        val shares = StatShares(game, steps, charges)
        StatTrend(sheets.mine.values, SheetShift.of(sheets.mine.values, now.values)) { key -> if (waiting) emptyList() else shares.of(key) }
    }
    val (better, worse) = remember(trend, now, index) { trend.count(now.values, index) }
    val lang = game.lang
    val model by produceState<StatSheetModel?>(null, now, hero, index, lang) {
        val content = index ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val conditional = hero?.let { ConditionalStats.of(it, content) }.orEmpty()
            StatSheetModel.of(now, content, hero?.level, lang, conditional, resists = null, explainer = StatExplainer(game))
        }
    }
    val sheetState = rememberStatSheetState()
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding(), contentPadding = PaddingValues(16.dp)) {
            item {
                Row(Modifier.padding(bottom = HEAD_GAP), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Engraved(ui("expedition.stats_title"))
                    place.badge?.let { ToneTag(ui(it), Rune) }
                }
            }
            item {
                MutedText(
                    listOf(ui(place.about), if (better + worse == 0) ui("expedition.stats_unchanged") else ui("expedition.stats_changed", better, worse)).joinToString(" "),
                    Modifier.padding(bottom = HEAD_GAP),
                )
            }
            model?.let { statSheet(it, sheetState, trend, index) }
        }
        model?.let { StatSheetWindows(game, it, sheetState, sheets.mapShifts) }
    }
}

/** Зазор под шапкой и пояснением листа; строки сводки держат свои зазоры сами. */
private val HEAD_GAP = 8.dp

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
 * Тело боя [ladder] героя [hero] по ступеням цифрами сводки ([fightFigures]) - вне главного потока и заново лишь при смене героя или
 * частей тела боя, не каждый снимок; пока считается - прежние ступени, вне боя - null.
 */
@Composable private fun rememberFightFigures(hero: HeroView?, ladder: HeroLadder?): FightFigures? {
    val figures by produceState<FightFigures?>(null, hero, ladder) {
        value = if (hero == null || ladder == null) null else withContext(Dispatchers.Default) { hero.fightFigures(ladder) }
    }
    return figures
}
