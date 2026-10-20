package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.RegenPool
import com.sperance.exileforge.rules.content.CoreStat

/**
 * Мера строки-потока сводки: в секунду или за убийство. [key] - ключ словаря числа с мерой («+23.9/с», «+12 за убийство»),
 * [shareKey] - доли максимума запаса в этой мере («0.7% максимума в секунду»); [stats] - строки сводки в этой мере: итог и вклады
 * их разбора печатаются с ней.
 */
enum class FlowMeasure(val key: String, val shareKey: String, private val stats: List<String>) {
    PER_SECOND(
        "unit.per_second",
        "stat.flow.share_per_second",
        listOf(RegenPool.LIFE.flatStat, RegenPool.SHIELD.flatStat, CoreStat.MANA_REGEN_FLAT.code),
    ),
    PER_KILL(
        "unit.per_kill",
        "stat.flow.share_per_kill",
        listOf(CoreStat.HEALTH_ON_KILL.code, CoreStat.MANA_ON_KILL.code, CoreStat.SHIELD_ON_KILL.code),
    ),
    ;

    companion object {
        private val byStat: Map<String, FlowMeasure> = entries.flatMap { measure -> measure.stats.map { it to measure } }.toMap()

        /** Мера строки сводки [stat], если она поток; иначе null. */
        fun of(stat: String): FlowMeasure? = byStat[stat]
    }
}
