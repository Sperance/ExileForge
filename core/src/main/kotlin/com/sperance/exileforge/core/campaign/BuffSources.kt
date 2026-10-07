package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.display.effectUnit
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.signedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.Op

/** Откуда строка карты на монстре (3.93.0): строки карты-предмета, силы атласа героя, сделки алтаря. */
enum class BuffSource { MAP, ATLAS, PACT }

/** Доля источника [source] в строке монстра [stat] с операцией [op]. */
data class BuffShare(val source: BuffSource, val stat: String, val op: Op, val value: Double)

/**
 * Источники строк карты на монстрах захода (3.93.0). Сервер складывает силы атласа в строки карты (`MAP_MONSTER_*`), поэтому доля
 * атласа - его собственные числа, а доля карты - остаток; сделки алтаря лежат отдельно. [boss] - боссу ещё и сила босса карты.
 */
object BuffSources {
    private val ATLAS_POWERS = mapOf(
        MapStat.MONSTER_LIFE.code to AtlasStat.MONSTER_LIFE.code,
        MapStat.MONSTER_DAMAGE.code to AtlasStat.MONSTER_DAMAGE.code,
        MapStat.MONSTER_SPEED.code to AtlasStat.MONSTER_SPEED.code,
    )

    fun of(effects: Map<String, Double>, atlas: Map<String, Double>, pacts: Map<String, Double>, boss: Boolean): List<BuffShare> {
        val atlasPart = ATLAS_POWERS.mapNotNull { (map, own) -> atlas[own]?.takeIf { it != 0.0 && map in effects }?.let { map to it } }.toMap()
        val mapPart = effects.mapValues { (code, value) -> value - (atlasPart[code] ?: 0.0) }.filterValues { it != 0.0 }
        fun shares(source: BuffSource, part: Map<String, Double>) = (MapEffects.buffs(part) + if (boss) MapEffects.bossBuffs(part) else emptyList()).map { BuffShare(source, it.stat, it.op, it.value) }
        return shares(BuffSource.MAP, mapPart) + shares(BuffSource.ATLAS, atlasPart) + shares(BuffSource.PACT, pacts)
    }
}

/**
 * «монстр +20% · карта +15% · атлас +5% · сделка +10%» (3.93.0): из чего сложена строка монстра - его свои строки и каждая
 * доля [shares] той же характеристики; null - у строки один источник.
 */
fun monsterLineParts(line: MonsterLine, shares: List<BuffShare>, index: ContentIndex? = null): String? {
    if (line.stat == com.sperance.exileforge.rules.content.CoreStat.TAUNT.code) return null
    val mine = shares.filter { it.stat == line.stat && it.op == line.op }.groupBy { it.source }.mapValues { (_, list) -> list.sumOf { it.value } }.filterValues { it != 0.0 }
    val parts = (if (line.own != 0.0) 1 else 0) + mine.size
    if (parts < 2 && mine.keys.singleOrNull() != BuffSource.ATLAS && mine.keys.singleOrNull() != BuffSource.PACT) return monsterLineSources(line, index)
    val unit = effectUnit(line.stat, line.op, index)
    fun part(key: String, value: Double) = ui(key, signedNumber(value) { v -> modNumber(line.stat, v) } + unit)
    return buildList {
        if (line.own != 0.0) add(part("fight.part_monster", line.own))
        mine.forEach { (source, value) -> add(part("fight.part_${source.name.lowercase()}", value)) }
    }.joinToString(" · ")
}
