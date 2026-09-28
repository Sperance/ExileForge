package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.Ceiling
import com.sperance.exileforge.rules.content.CombatRules

/**
 * A figure a fight counts only up to a ceiling (3.12.0): a resistance under its maximum; block, critical chance and
 * physical reduction under their own (3.13.0).
 * [effective] is the figure as the fight reads it — an element's resistance with «all resistances» — [base] the
 * rules' own cap, [raises] the stats that lift it with what they give now, [cap] where it stands, and [hard] the
 * ceiling no raise passes, when there is one.
 */
data class StatLimit(val stat: String, val effective: Double, val base: Double, val raises: List<Pair<String, Double>>, val cap: Double, val hard: Double?) {
    /** What lies above the cap and does nothing in a fight. */
    val over: Double get() = (effective - cap).coerceAtLeast(0.0)
}

/** The ceilings of the hero's figures, read exactly as `Combatant` applies them. */
object StatLimits {
    private const val CHAOS = "STOCK_RESIST_CHAOS"
    private const val ALL = "STOCK_RESIST_ALL"
    private const val MAX_ALL = "STOCK_RESIST_MAX_ALL"
    private val ELEMENTS = setOf("STOCK_RESIST_FIRE", "STOCK_RESIST_COLD", "STOCK_RESIST_LIGHTNING")

    fun of(stat: String, stats: Map<String, Double>, rules: CombatRules): StatLimit? = when (stat) {
        in ELEMENTS, CHAOS -> resistance(stat, stats, rules)
        else -> ceilings(rules)[stat]?.let { ceiling(stat, stats, it) }
    }

    private fun ceilings(rules: CombatRules): Map<String, Ceiling> = mapOf(
        "STOCK_BLOCK_CHANCE" to rules.ceilings.block,
        "STOCK_CRITICAL_CHANCE" to rules.ceilings.critical,
        "STOCK_PHYSICAL_REDUCTION" to rules.ceilings.physical,
    )

    private fun ceiling(stat: String, stats: Map<String, Double>, limit: Ceiling): StatLimit {
        val raised = stats[limit.raise] ?: 0.0
        val raises = listOf(limit.raise to raised).filter { it.second != 0.0 }
        return StatLimit(stat, stats[stat] ?: 0.0, limit.base, raises, limit.at(raised), limit.hard)
    }

    /** Chaos stands alone; the three elements also take «all resistances» and «all maximum resistances». */
    private fun resistance(stat: String, stats: Map<String, Double>, rules: CombatRules): StatLimit {
        val chaos = stat == CHAOS
        val raises = listOfNotNull(stat.replace("STOCK_RESIST_", "STOCK_RESIST_MAX_"), MAX_ALL.takeUnless { chaos })
            .map { it to (stats[it] ?: 0.0) }.filter { it.second != 0.0 }
        val cap = (rules.resistCap + raises.sumOf { it.second }).coerceIn(0.0, rules.resistHardCap)
        val effective = (stats[stat] ?: 0.0) + if (chaos) 0.0 else stats[ALL] ?: 0.0
        return StatLimit(stat, effective, rules.resistCap, raises, cap, rules.resistHardCap)
    }
}
