package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.character.stockStats

/**
 * The groups the character sheet is read in, top to bottom.
 *
 * Display only: which group a characteristic sits in says nothing about how the server counts it.
 * A code the client does not know — a stat the server grows tomorrow — lands in [OTHER], so the
 * sheet never drops a number it was sent.
 */
enum class StatGroup {
    RESERVE, DEFENCE, RESISTANCE, ATTACK, ATTRIBUTE, OTHER;

    fun title(lang: Lang = uiLanguage): String = ui(lang, "enum.stat_group.$name")

    companion object {
        private val reserve = setOf("STOCK_HEALTH", "STOCK_MANA", "STOCK_ENERGY_SHIELD", "STOCK_ENERGY",
            "STOCK_HEALTH_REGEN", "STOCK_MANA_REGEN", "STOCK_ENERGY_REGEN")
        private val defence = setOf("STOCK_ARMOR", "STOCK_EVASION", "STOCK_BLOCK_CHANCE", "STOCK_STUN_THRESHOLD")
        private val attribute = setOf("STOCK_STRENGTH", "STOCK_AGILITY", "STOCK_INTELLECT", "STOCK_CONSTITUTION")
        private val attack = setOf("STOCK_CAST_SPEED", "STOCK_CAST_STRENGTH")

        fun of(stat: String): StatGroup = when {
            stat in reserve -> RESERVE
            stat in defence -> DEFENCE
            stat.startsWith("STOCK_RESIST_") -> RESISTANCE
            stat in attribute -> ATTRIBUTE
            stat.startsWith("STOCK_ATTACK_") || stat.startsWith("STOCK_CRITICAL_") || stat.startsWith("STOCK_LEECH_") || stat in attack -> ATTACK
            else -> OTHER
        }
    }
}

/**
 * The sheet the server sent, sorted into [StatGroup]s: groups in their own order, empty ones left
 * out, and inside each the server's own enum order, so two sheets always read the same way.
 */
fun groupedStats(stats: Map<String, Double>): List<Pair<StatGroup, List<Pair<String, Double>>>> {
    val order = stockStats.withIndex().associate { (index, stat) -> stat to index }
    return stats.entries.groupBy { StatGroup.of(it.key) }.toSortedMap()
        .map { (group, entries) -> group to entries.sortedWith(compareBy({ order[it.key] ?: Int.MAX_VALUE }, { it.key })).map { it.key to it.value } }
}
