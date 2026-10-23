package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.roll.Roll
import kotlin.math.roundToLong

/**
 * Снимок героя для отчёта об ошибке: защита, запасы и сопротивления - цифрами сводки героя ([figures]: лист боя с пассивными умениями
 * и Предначертанием, общее сопротивление - в каждой стихии), как их видит игрок; умения в слотах и надетое кодами шаблонов с
 * модификаторами - строками «что: значение». Модификатор вещи - код, тир и числа по контенту [index] (без контента - код и тир);
 * расколотый - с «F». Без имени героя и без его id: в публичный Issue уходит только техническое.
 */
fun HeroView.reportSheet(index: ContentIndex?): List<String> = buildList {
    val shown = figures.values
    val numbers = SHEET.mapNotNull { (title, stat) -> shown[stat.code]?.let { "$title ${it.roundToLong()}" } }
    if (numbers.isNotEmpty()) add(numbers.joinToString(", "))
    val resists = RESISTS.map { (title, stat) -> "$title ${(shown[stat.code] ?: 0.0).roundToLong()}%" }
    add("Сопротивления: " + resists.joinToString(" / "))
    skills.active.filterNotNull().takeIf { it.isNotEmpty() }?.let { slots -> add("Умения: " + slots.joinToString(", ") { "${it.skill} ${skills.learned[it.skill] ?: 1}" }) }
    skills.passive.filterNotNull().takeIf { it.isNotEmpty() }?.let { add("Пассивные: " + it.joinToString(", ")) }
    equipped.entries.sortedBy { it.key.ordinal }.forEach { (slot, item) ->
        val lines = item.rolls.joinToString("; ") { reportLine(it, index) }
        add("$slot: ${item.template} (${item.rarity}, ур. ${item.itemLevel})" + if (lines.isEmpty()) "" else ": $lines")
    }
}

/** Модификатор вещи для отчёта: «CODE T2 34/12», расколотый - «CODE T2F 34»; числа - по контенту [index], без него не пишутся. */
private fun reportLine(roll: Roll, index: ContentIndex?): String {
    val values = index?.let { roll.values(it) }.orEmpty().joinToString("/", transform = ::reportNumber)
    val tier = if (roll.tier > 0) " T${roll.tier}" + if (roll.fractured) "F" else "" else ""
    return "${roll.code.value}$tier" + if (values.isEmpty()) "" else " $values"
}

/** Число модификатора в отчёте: целое без дроби, прочее - до сотых. */
private fun reportNumber(value: Double): String = if (value == Math.rint(value)) value.roundToLong().toString() else (Math.round(value * 100) / 100.0).toString()

private val SHEET = listOf(
    "Здоровье" to CoreStat.HEALTH,
    "Мана" to CoreStat.MANA,
    "ЭЩ" to CoreStat.ENERGY_SHIELD,
    "Броня" to CoreStat.ARMOR,
    "Уклонение" to CoreStat.EVASION,
)

private val RESISTS = listOf(
    "огонь" to CoreStat.RESIST_FIRE,
    "холод" to CoreStat.RESIST_COLD,
    "молния" to CoreStat.RESIST_LIGHTNING,
    "хаос" to CoreStat.RESIST_CHAOS,
)
