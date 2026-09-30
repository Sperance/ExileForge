package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.display.effectText
import com.sperance.exileforge.core.display.effectUnit
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.RolledMonster

/** One line of what a monster carries: a stat and an operation, summed — its two sources apart, [own] from the monster, [map] from the map's buffs. */
data class MonsterLine(val stat: String, val op: Op, val own: Double, val map: Double, val fromMap: Boolean) {
    val value: Double get() = own + map
    val split: Boolean get() = fromMap && own != 0.0
}

/** A monster's modifiers and its map's buffs as one list, summed per stat and operation — the same sum the fight folds; display only. */
fun monsterLines(monster: RolledMonster): List<MonsterLine> =
    (monster.modifiers.flatMap { it.effects }.map { it to false } + monster.mapBuffs.map { it to true })
        .groupBy { (effect, _) -> effect.stat to effect.op }
        .map { (key, parts) ->
            val (map, own) = parts.partition { it.second }
            MonsterLine(key.first, key.second, own.sumOf { it.first.value }, map.sumOf { it.first.value }, map.isNotEmpty())
        }

fun monsterLineText(line: MonsterLine, index: ContentIndex? = null): String {
    // A taunt is there or not: how many modifiers gave it says nothing.
    if (line.stat == "STOCK_TAUNT") return statTitle(line.stat)
    return effectText(line.stat, line.op, line.value, index)
}

/** «монстр 20% · карта 15%»: where a summed line comes from, when both sources give it. */
fun monsterLineSources(line: MonsterLine, index: ContentIndex? = null): String? = line.takeIf { it.split && it.stat != "STOCK_TAUNT" }?.let {
    val unit = effectUnit(it.stat, it.op, index)
    ui("fight.line_sources", modNumber(it.stat, it.own) + unit, modNumber(it.stat, it.map) + unit)
}
