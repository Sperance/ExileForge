package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.CoreStat
import kotlin.math.roundToLong

/**
 * Снимок героя для отчёта об ошибке: защита и запасы из листа героя, умения в слотах и надетое кодами шаблонов -
 * строками «что: значение». Без имени героя и без его id: в публичный Issue уходит только техническое.
 */
fun HeroView.reportSheet(): List<String> = buildList {
    val numbers = SHEET.mapNotNull { (title, stat) -> stats[stat.code]?.let { "$title ${it.roundToLong()}" } }
    if (numbers.isNotEmpty()) add(numbers.joinToString(", "))
    val resists = RESISTS.map { (title, stat) -> "$title ${(stats[stat.code] ?: 0.0).roundToLong()}%" }
    add("Сопротивления: " + resists.joinToString(" / "))
    skills.active.filterNotNull().takeIf { it.isNotEmpty() }?.let { slots -> add("Умения: " + slots.joinToString(", ") { "${it.skill} ${skills.learned[it.skill] ?: 1}" }) }
    skills.passive.filterNotNull().takeIf { it.isNotEmpty() }?.let { add("Пассивные: " + it.joinToString(", ")) }
    equipped.entries.sortedBy { it.key.ordinal }.forEach { (slot, item) -> add("$slot: ${item.template} (${item.rarity}, ур. ${item.itemLevel})") }
}

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
