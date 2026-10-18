package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.rules.roll.WorkGains

/** Два итога вместе: что сессия принесла до сих пор и что принёс ещё один ответ, с циклами впустую по причинам. */
operator fun WorkGains.plus(other: WorkGains): WorkGains = WorkGains(
    cycles = cycles + other.cycles, nothing = nothing + other.nothing, items = merge(items, other.items),
    experience = Math.round((experience + other.experience) * 10) / 10.0, levels = levels + other.levels,
    spent = merge(spent, other.spent), made = made + other.made, starved = starved || other.starved, equipment = equipment + other.equipment,
    missed = (missed.keys + other.missed.keys).associateWith { (missed[it] ?: 0) + (other.missed[it] ?: 0) }, done = done || other.done,
)

private fun merge(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> = (a.keys + b.keys).associateWith { (a[it] ?: 0) + (b[it] ?: 0) }.filterValues { it != 0L }
