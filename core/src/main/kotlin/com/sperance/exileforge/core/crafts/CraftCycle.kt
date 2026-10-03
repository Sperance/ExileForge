package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.roll.Work
import com.sperance.exileforge.rules.roll.WorkBonus
import com.sperance.exileforge.rules.roll.WorkGains
import kotlin.math.floor
import kotlin.math.max

/**
 * One cycle of a work, thrown here the moment it ends. The server throws cycle N of a work from `(seed, N)`
 * with the rules' dice in a fixed order — the «nothing» chance, the extra unit, each side find — so the same
 * seed and number give the same cycle here. [JobView.nothing] and each find's chance already carry the
 * hero's level and gear as the server worked them out; the server settles the same cycle afterwards.
 */
object CraftCycle {
    /** What a cycle spends: the work's inputs and one of each additive. */
    fun spent(job: JobView, additives: List<String>): Map<String, Long> = (job.inputs.map { it.item to it.amount } + additives.map { it to 1L }).groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }

    fun roll(seed: Long, index: Long, job: JobView, bonus: WorkBonus, additives: List<String> = emptyList()): WorkGains {
        val dice = Work.cycleDice(seed, index)
        val spent = spent(job, additives)
        if (dice.percent(job.nothing)) return WorkGains(cycles = 1, nothing = 1, spent = spent)
        val extra = max(0.0, bonus.yield) / 100
        val whole = floor(extra).toLong()
        val units = 1 + whole + if (dice.chance(extra - whole)) 1 else 0
        val items = mutableMapOf<String, Long>()
        if (job.kind == JobKind.ITEM) items.merge(job.output, units, Long::plus)
        job.extra.forEach { find -> if (dice.percent(find.chance)) items.merge(find.item, 1, Long::plus) }
        val experience = Math.round(job.experience * (1 + max(0.0, bonus.experience) / 100) * 10) / 10.0
        return WorkGains(cycles = 1, items = items, experience = experience, spent = spent, made = if (job.kind == JobKind.ITEM) 0 else units.toInt())
    }
}

/** Two tallies added up: what the session brought so far and what one more answer or cycle brought. */
operator fun WorkGains.plus(other: WorkGains): WorkGains = WorkGains(
    cycles = cycles + other.cycles, nothing = nothing + other.nothing, items = merge(items, other.items, 1),
    experience = Math.round((experience + other.experience) * 10) / 10.0, levels = levels + other.levels,
    spent = merge(spent, other.spent, 1), made = made + other.made, starved = starved || other.starved, equipment = equipment + other.equipment,
)

/** What [other] adds beyond this one, stack by stack — negative where the server counted less. */
operator fun WorkGains.minus(other: WorkGains): WorkGains = WorkGains(
    cycles = cycles - other.cycles, nothing = nothing - other.nothing, items = merge(items, other.items, -1),
    experience = Math.round((experience - other.experience) * 10) / 10.0, levels = levels - other.levels,
    spent = merge(spent, other.spent, -1), made = made - other.made, starved = starved, equipment = equipment,
)

private fun merge(a: Map<String, Long>, b: Map<String, Long>, sign: Long): Map<String, Long> = (a.keys + b.keys).associateWith { (a[it] ?: 0) + sign * (b[it] ?: 0) }.filterValues { it != 0L }
