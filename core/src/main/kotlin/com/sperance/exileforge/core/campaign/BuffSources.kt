package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.display.effectUnit
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.EffectSource
import com.sperance.exileforge.rules.roll.SourcedEffects

/** Откуда строка карты на монстре: строки карты-предмета по источникам сервера ([EffectSource]) и сделки алтаря. */
enum class BuffSource {
    MAP,
    ATLAS,
    SCARAB,
    INFLUENCE,
    PACT,
    ;

    companion object {
        /** Источник доли строк карты-предмета источника [source]. */
        fun of(source: EffectSource): BuffSource = when (source) {
            EffectSource.MAP -> MAP
            EffectSource.ATLAS -> ATLAS
            EffectSource.SCARAB -> SCARAB
            EffectSource.INFLUENCE -> INFLUENCE
        }
    }
}

/** Доля источника [source] в строке монстра [stat] с операцией [op]. */
data class BuffShare(val source: BuffSource, val stat: String, val op: Op, val value: Double)

/**
 * Источники строк карты на монстрах захода: каждый источник карты-предмета [sources], как их сложил сервер, свои строки захода [own]
 * (зона Ваал) - к карте, сделки алтаря [pacts] - отдельно. [boss] - боссу ещё и сила босса карты.
 */
object BuffSources {
    fun of(sources: List<SourcedEffects>, own: Map<String, Double>, pacts: Map<String, Double>, boss: Boolean): List<BuffShare> {
        fun shares(source: BuffSource, part: Map<String, Double>) = (MapEffects.buffs(part) + if (boss) MapEffects.bossBuffs(part) else emptyList()).map { BuffShare(source, it.stat, it.op, it.value) }
        return sources.flatMap { shares(BuffSource.of(it.source), it.effects) } + shares(BuffSource.MAP, own) + shares(BuffSource.PACT, pacts)
    }
}

/**
 * «монстр +20% · карта +15% · атлас +5% · сделка +10%»: из чего сложена строка монстра - его свои строки и каждая
 * доля [shares] той же характеристики; null - у строки один источник.
 */
fun monsterLineParts(line: MonsterLine, shares: List<BuffShare>, index: ContentIndex? = null): String? {
    if (line.stat == com.sperance.exileforge.rules.content.CoreStat.TAUNT.code) return null
    val mine = shares.filter { it.stat == line.stat && it.op == line.op }.groupBy { it.source }.mapValues { (_, list) -> list.sumOf { it.value } }.filterValues { it != 0.0 }
    val parts = (if (line.own != 0.0) 1 else 0) + mine.size
    if (parts < 2 && mine.keys.singleOrNull().let { it == null || it == BuffSource.MAP }) return monsterLineSources(line, index)
    val unit = effectUnit(line.stat, line.op, index)
    fun part(key: String, value: Double) = ui(key, signedNumber(value) { v -> modNumber(line.stat, v) } + unit)
    return buildList {
        if (line.own != 0.0) add(part("fight.part_monster", line.own))
        mine.forEach { (source, value) -> add(part("fight.part_${source.name.lowercase()}", value)) }
    }.joinToString(" · ")
}
