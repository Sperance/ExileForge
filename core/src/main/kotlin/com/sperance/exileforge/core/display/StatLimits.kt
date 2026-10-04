package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.Ceiling
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.CoreStat

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
    private val CHAOS: String = CoreStat.RESIST_CHAOS.code
    private val ALL: String = CoreStat.RESIST_ALL.code
    private val MAX_ALL: String = CoreStat.RESIST_MAX_ALL.code
    private val ELEMENTS = setOf(CoreStat.RESIST_FIRE.code, CoreStat.RESIST_COLD.code, CoreStat.RESIST_LIGHTNING.code)

    fun of(stat: String, stats: Map<String, Double>, rules: CombatRules): StatLimit? = when (stat) {
        in ELEMENTS, CHAOS -> resistance(stat, stats, rules)
        else -> ceilings(rules)[stat]?.let { ceiling(stat, stats, it) }
    }

    private fun ceilings(rules: CombatRules): Map<String, Ceiling> = mapOf(
        CoreStat.BLOCK_CHANCE.code to rules.ceilings.block,
        CoreStat.CRITICAL_CHANCE.code to rules.ceilings.critical,
        CoreStat.SPELL_CRITICAL_CHANCE.code to rules.ceilings.critical,
        CoreStat.PHYSICAL_REDUCTION.code to rules.ceilings.physical,
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
