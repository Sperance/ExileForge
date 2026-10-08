package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.StatRegistry

/**
 * The groups the hero sheet is read in, top to bottom. Display only: which group a stat sits in says
 * nothing about how it is counted. A code the client does not know lands in [OTHER].
 */
enum class StatGroup {
    RESERVE,
    DEFENCE,
    RESISTANCE,
    ATTACK,
    AILMENT,
    ATTRIBUTE,
    OTHER,
    ;

    fun title(lang: Lang = uiLanguage): String = ui(lang, "enum.stat_group.$name")

    companion object {
        private val reserve = setOf(
            CoreStat.HEALTH.code, CoreStat.MANA.code, CoreStat.ENERGY_SHIELD.code, CoreStat.ENERGY.code, CoreStat.HEALTH_REGEN.code, CoreStat.MANA_REGEN.code,
            CoreStat.ENERGY_REGEN.code, CoreStat.ENERGY_REGEN_PERCENT.code, CoreStat.HEALTH_ON_KILL.code, CoreStat.HEALTH_ON_HIT.code,
        )
        private val defence = setOf(
            CoreStat.ARMOR.code, CoreStat.EVASION.code, CoreStat.BLOCK_CHANCE.code, CoreStat.STUN_THRESHOLD.code, CoreStat.SPELL_BLOCK.code,
            CoreStat.PHYSICAL_REDUCTION.code, CoreStat.AVOID_STUN.code, CoreStat.STUN_POOL.code, CoreStat.IMMUNE_ELECTROCUTE.code, CoreStat.DAMAGE_TAKEN.code, CoreStat.PHYSICAL_TAKEN.code, CoreStat.ELEMENTAL_TAKEN.code, CoreStat.CHAOS_TAKEN.code,
            CoreStat.RECOVERY_RATE.code, CoreStat.SHIELD_RECHARGE.code, CoreStat.THORNS.code, CoreStat.REFLECT.code,
        )
        private val ailment = setOf(
            CoreStat.IGNITE_CHANCE.code, CoreStat.FREEZE_CHANCE.code, CoreStat.STUN_BUILDUP.code, CoreStat.ELECTROCUTE_BUILDUP.code, CoreStat.SHOCK_CHANCE.code, CoreStat.POISON_CHANCE.code, CoreStat.BLEED_CHANCE.code,
            CoreStat.BURNING_DAMAGE.code, CoreStat.POISON_DAMAGE.code, CoreStat.BLEED_DAMAGE.code, CoreStat.AILMENT_DURATION.code, CoreStat.RECOVERY_SUPPRESSION.code,
        )
        private val attribute = setOf(CoreStat.STRENGTH.code, CoreStat.AGILITY.code, CoreStat.INTELLECT.code, CoreStat.ALL_ATTRIBUTES.code)
        private val attack = setOf(CoreStat.ELEMENTAL_DAMAGE.code, CoreStat.CAST_SPEED.code, CoreStat.SPELL_CRITICAL_CHANCE.code, CoreStat.SPELL_CRITICAL_MULTIPLIER.code)

        fun of(stat: String): StatGroup = when {
            stat in reserve -> RESERVE

            stat in defence -> DEFENCE

            stat.startsWith("STOCK_RESIST_") || stat == CoreStat.ALL_RESISTANCES.code -> RESISTANCE

            stat in ailment || stat.startsWith("STOCK_AVOID_") || stat.endsWith("_DURATION_ON_SELF") || stat.endsWith("_DURATION") -> AILMENT

            stat in attribute -> ATTRIBUTE

            stat.startsWith("STOCK_ATTACK_") || stat.startsWith("STOCK_CRITICAL_") || stat.startsWith("STOCK_LEECH_") || stat in attack ||
                stat.startsWith("STOCK_PENETRATE_") || stat.startsWith("STOCK_DAMAGE_VS_") -> ATTACK

            else -> OTHER
        }
    }
}

/**
 * The sheet sorted into [StatGroup]s: groups in their own order, empty ones left out, and inside each the registry's order.
 * The counts of what is worn (server 1.32.0) are the powers' own reading of the sheet, not a figure of the hero: not shown.
 */
fun groupedStats(stats: Map<String, Double>, registry: StatRegistry? = null): List<Pair<StatGroup, List<Pair<String, Double>>>> = stats.entries.filterNot { it.key.startsWith(WORN) }.groupBy { StatGroup.of(it.key) }.toSortedMap()
    .map { (group, entries) -> group to entries.sortedWith(compareBy({ registry?.order(it.key) ?: Int.MAX_VALUE }, { it.key })).map { it.key to it.value } }

/** The prefix of the sheet's counts of what is worn (server 1.32.0). */
private const val WORN = "STOCK_WORN_"
