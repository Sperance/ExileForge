package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.BrewStat
import com.sperance.exileforge.rules.sheet.Shift

/**
 * Лист героя под эффектами захода [effects]: строки зелья и древа гильдии ([BrewStat.lines]) ложатся сводом листа [fold] среди его
 * операций - до «больше»-множителей, как строки снаряжения, - а строки карты и дары Атласа - поверх готового листа ([MapEffects.hero]).
 * Одна точка тела героя в бою ([HeroBuild]), листа «На карте» и полки эффектов: их цифры и сдвиги по источникам ([origins]) одни.
 *
 * [fold] - свод листа героя со строками боя: тот же лист, что без эффектов захода, при пустых строках.
 */
class RunSheet(
    private val effects: Map<String, Double>,
    private val origins: EffectOrigins = EffectOrigins.NONE,
    private val fold: (List<StatLine>) -> Map<String, Double>,
) {
    /** Строки зелья и древа гильдии в своде листа: их же берёт рост урона умений, как строки снаряжения. */
    val boons: List<StatLine> = BrewStat.lines(effects)

    /** Лист со строками боя [lines] под всеми эффектами захода. */
    fun stats(lines: List<StatLine> = emptyList()): Map<String, Double> = MapEffects.hero(brewed(lines), effects)

    /**
     * Что эффекты захода сдвинули на листе со строками [lines], по характеристике и источнику: доли зелья и гильдии - сводом листа,
     * строки карты и дары Атласа - поверх; сумма сдвигов - разница [stats] и листа без эффектов.
     */
    fun shifts(lines: List<StatLine> = emptyList()): Map<String, List<Shift>> {
        if (effects.isEmpty()) return emptyMap()
        val brewed = brewed(lines)
        val own = if (boons.isEmpty()) emptyMap() else boonShifts(fold(lines), brewed, lines)
        val map = MapEffects.heroShifts(brewed, effects, origins)
        return (own.keys + map.keys).associateWith { own[it].orEmpty() + map[it].orEmpty() }
    }

    private fun brewed(lines: List<StatLine>): Map<String, Double> = if (boons.isEmpty()) fold(lines) else fold(boons + lines)

    /** Сдвиги листа [plain] строками зелья и гильдии по источникам: каждая доля строки - сводом одна, остаток вместе - последней. */
    private fun boonShifts(plain: Map<String, Double>, brewed: Map<String, Double>, lines: List<StatLine>): Map<String, List<Shift>> {
        val parts = BrewStat.ALL.flatMap { code ->
            val value = effects[code]?.takeIf { it != 0.0 } ?: return@flatMap emptyList()
            origins.parts(code, value).map { (source, share) -> source to fold(BrewStat.lines(mapOf(code to share)) + lines) }
        }
        return SourceShifts.split(plain, parts, brewed)
    }

    companion object {
        /**
         * Лист эффектов захода над готовыми характеристиками [stats] - без листа героя за ними (лист боя ещё не собран): строки ложатся
         * сводом правил поверх готового ([StatLines.fold]).
         */
        fun over(stats: Map<String, Double>, effects: Map<String, Double>, origins: EffectOrigins = EffectOrigins.NONE): RunSheet =
            RunSheet(effects, origins) { lines -> if (lines.isEmpty()) stats else StatLines.fold(stats, lines, emptySet()) }
    }
}

