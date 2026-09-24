package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.model.campaign.MonsterModifier

/**
 * A monster modifier as a sentence: the server's template with each effect's value in its place.
 * A sentence is never a label with numbers appended — word order differs between languages.
 */
fun monsterModifierText(modifier: MonsterModifier): String =
    modifier.effects.foldIndexed(loc(LocaleKey.monsterModifierName(modifier.code))) { index, text, effect ->
        text.replace("{$index}", statNumber(effect.stat, effect.value))
    }

fun monsterTitle(code: String): String = loc(LocaleKey.monsterName(code))
fun mapTitle(code: String): String = loc(LocaleKey.mapName(code))
fun mapDescription(code: String): String = loc(LocaleKey.mapDescription(code))
fun chapterTitle(code: String): String = loc(LocaleKey.chapterName(code))

/** One line of what a monster carries (since 2.45.0): a characteristic and an operation, summed, and whether the map is in it. */
data class MonsterLine(val stat: String, val operation: String, val value: Double, val fromMap: Boolean)

/**
 * A monster's modifiers and its map's buffs as one list, summed per characteristic and operation:
 * +40% life of its own and +30% from the map read as +70%. The same sum the fight folds — display only.
 */
fun monsterLines(monster: RolledMonster): List<MonsterLine> =
    (monster.modifiers.flatMap { it.effects }.map { it to false } + monster.mapBuffs.map { it to true })
        .filterNot { (effect, _) -> com.sperance.exileforge.core.display.retired(effect.stat) }
        .groupBy { (effect, _) -> effect.stat to effect.operation }
        .map { (key, parts) -> MonsterLine(key.first, key.second, parts.sumOf { it.first.value }, parts.any { it.second }) }

fun monsterLineText(line: MonsterLine): String {
    val value = statNumber(line.stat, line.value)
    val stat = statTitle(line.stat)
    return when (line.operation) {
        "ADD" -> ui("fight.line_add", value, stat)
        "INCREASED" -> ui("fight.line_increased", value, stat)
        "MORE" -> ui("fight.line_more", value, stat)
        else -> ui("fight.line_set", value, stat)
    }
}
