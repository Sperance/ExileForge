package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.rules.roll.WorkGains

/** Two tallies added up: what the session brought so far and what one more answer brought. */
operator fun WorkGains.plus(other: WorkGains): WorkGains = WorkGains(
    cycles = cycles + other.cycles, nothing = nothing + other.nothing, items = merge(items, other.items),
    experience = Math.round((experience + other.experience) * 10) / 10.0, levels = levels + other.levels,
    spent = merge(spent, other.spent), made = made + other.made, starved = starved || other.starved, equipment = equipment + other.equipment,
)

private fun merge(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> = (a.keys + b.keys).associateWith { (a[it] ?: 0) + (b[it] ?: 0) }.filterValues { it != 0L }
