package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.StatSource
import kotlin.math.abs

/**
 * Сдвиги листа героя по источникам - одна точка счёта для эффектов карты ([MapEffects.heroShifts]), листа захода ([RunSheet.shifts])
 * и источников цифры сводки ([CombatSheet.sources]): порог следа округления [MOVED], разбор листа по частям ([split]) и свод по
 * источнику ([bySource]).
 */
object SourceShifts {
    /** Сдвиг листа меньше этого - след округления, не сдвиг. */
    const val MOVED = 0.05

    /**
     * Сдвиги листа [before] по источникам: каждая часть [parts] - лист под ней одной, а что части делают только вместе (лист под всеми -
     * [joint]), ложится на последний сдвиг характеристики - сумма сдвигов равна `joint - before`.
     */
    fun split(before: Map<String, Double>, parts: List<Pair<StatSource, Map<String, Double>>>, joint: Map<String, Double>): Map<String, List<Shift>> {
        val shifts = LinkedHashMap<String, MutableList<Shift>>()
        parts.forEach { (source, sheet) ->
            sheet.forEach { (stat, moved) ->
                val delta = moved - (before[stat] ?: 0.0)
                if (abs(delta) >= MOVED) shifts.getOrPut(stat) { mutableListOf() } += Shift(source, delta)
            }
        }
        joint.forEach { (stat, together) ->
            val own = shifts[stat] ?: return@forEach
            val rest = together - (before[stat] ?: 0.0) - own.sumOf { it.delta }
            if (abs(rest) >= MOVED) own[own.lastIndex] = own.last().let { it.copy(delta = it.delta + rest) }
        }
        return shifts
    }

    /** Сдвиги [shifts], сложенные по источнику в порядке первого появления; сумма меньше [MOVED] - след округления, не источник. */
    fun bySource(shifts: List<Shift>): List<Shift> =
        shifts.groupBy { it.source }.map { (source, own) -> Shift(source, own.sumOf { it.delta }) }.filter { abs(it.delta) >= MOVED }
}
