package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.loc

fun monsterTitle(code: String): String = loc(LocaleKey.monsterName(code))
fun mapTitle(code: String): String = loc(LocaleKey.mapName(code))
fun mapDescription(code: String): String = loc(LocaleKey.mapDescription(code))
fun regionTitle(code: String): String = loc(LocaleKey.regionName(code))

/**
 * One line of what a monster carries (since 2.45.0): a characteristic and an operation, summed —
 * and since 2.73.0 its two sources apart, [own] from the monster, [map] from the map's buffs.
 */
data class MonsterLine(val stat: String, val operation: String, val own: Double, val map: Double, val fromMap: Boolean) {
    val value: Double get() = own + map
    /** Both the monster and the map give it: the line says how much each. */
    val split: Boolean get() = fromMap && own != 0.0
}

/**
 * A monster's modifiers and its map's buffs as one list, summed per characteristic and operation:
 * +40% life of its own and +30% from the map read as +70%. The same sum the fight folds — display only.
 */
fun monsterLines(monster: RolledMonster): List<MonsterLine> =
    (monster.modifiers.flatMap { it.effects }.map { it to false } + monster.mapBuffs.map { it to true })
        .filterNot { (effect, _) -> com.sperance.exileforge.core.display.retired(effect.stat) }
        .groupBy { (effect, _) -> effect.stat to effect.operation }
        .map { (key, parts) ->
            val (map, own) = parts.partition { it.second }
            MonsterLine(key.first, key.second, own.sumOf { it.first.value }, map.sumOf { it.first.value }, map.isNotEmpty())
        }

fun monsterLineText(line: MonsterLine): String {
    val stat = statTitle(line.stat)
    // A taunt is there or not (2.71.0): how many modifiers gave it says nothing.
    if (line.stat == "STOCK_TAUNT") return stat
    val value = statNumber(line.stat, line.value)
    return when (line.operation) {
        "ADD" -> ui("fight.line_add", value, stat)
        "INCREASED" -> ui("fight.line_increased", value, stat)
        "MORE" -> ui("fight.line_more", value, stat)
        else -> ui("fight.line_set", value, stat)
    }
}

/** «монстр 20% · карта 15%»: where a summed line comes from, when both sources give it (2.73.0). */
fun monsterLineSources(line: MonsterLine): String? = line.takeIf { it.split && it.stat != "STOCK_TAUNT" }?.let {
    val unit = if (it.operation == "INCREASED" || it.operation == "MORE") "%" else ""
    ui("fight.line_sources", statNumber(it.stat, it.own) + unit, statNumber(it.stat, it.map) + unit)
}
