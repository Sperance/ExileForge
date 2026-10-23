package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.campaign.MapLineKind
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.VaalZone

/**
 * Строка выбранной карты в окне входа: фраза [text] с действующими числами, вид [kind], доля опасности к добыче [risk] (у вреда) и
 * пояснение характеристики по касанию [note] (то же, что у строки карточки, [ItemLine.note]).
 */
data class MapLine(val text: String, val kind: MapLineKind, val risk: Double, val note: String?)

/**
 * Строки карты в окне входа - те же числа, что сложит сервер на входе: Атлас усиливает только строки `scalable` (печать, флаги и
 * счётные - как есть, `LootRoller.mapValues`), риск строки с условием платится, только если условие на карте есть.
 */
object MapLines {
    /** Строки карты [map] в порядке броска при силе модификаторов карт Атласа [atlasEffect]; составная строка - вред, если вреден любой её стат. */
    fun of(map: ItemView, index: ContentIndex, atlasEffect: Double = 1.0): List<MapLine> {
        val roller = LootRoller(index)
        val bound = map.lines.map { line -> line.definition?.let { def -> def to roller.mapValues(def, line.values, atlasEffect) } }
        val all = bound.filterNotNull().flatMap { (def, values) -> def.bind(values) }.groupingBy { it.first.stat }.fold(0.0) { sum, (_, value) -> sum + value }
        return map.lines.mapIndexed { i, line -> line(index, line, bound[i]?.second, all) }
    }

    /**
     * Строки зоны Ваал [zone] фразами в порядке броска - с теми числами, что действуют в зоне: «Сила модификаторов карт» Атласа на
     * броске ([VaalZone.atlasEffect]) ложится тем же правилом, что на строки карты; строка, которой контент не знает, - её код.
     */
    fun vaal(zone: VaalZone, index: ContentIndex): List<String> {
        val roller = LootRoller(index)
        return zone.rolls.map { roll -> index.modifier(roll.code)?.let { def -> modifierLine(index, def, roller.mapValues(def, roll.values(def), zone.atlasEffect)) } ?: displayName(roll.code.value) }
    }

    /** Сноска строк Ваал «с силой Атласа +12%» по множителю [atlasEffect]; без силы Атласа - null. */
    fun vaalAtlasNote(atlasEffect: Double): String? = if (atlasEffect == 1.0) null else ui("run.mods.vaal_atlas", signedNumber((atlasEffect - 1) * 100) + "%")

    /** Строка карты [line] со значениями [values] (null - как выпала) и риском среди всех строк карты [all]. */
    private fun line(index: ContentIndex, line: ItemLine, values: List<Double>?, all: Map<String, Double>): MapLine {
        val def = line.definition
        val kinds = def?.effects.orEmpty().map { MapLineKind.of(index, it.stat) }
        val kind = when {
            MapLineKind.HARM in kinds -> MapLineKind.HARM
            MapLineKind.REWARD in kinds -> MapLineKind.REWARD
            else -> MapLineKind.CONTENT
        }
        val shown = values ?: line.values
        val risk = def?.bind(shown).orEmpty().sumOf { (effect, value) -> MapLineKind.risk(index, effect.stat, value, all) }
        val text = if (def != null && shown != line.values) modifierLine(index, def, shown) else line.text
        return MapLine(text, kind, risk, line.note)
    }
}
