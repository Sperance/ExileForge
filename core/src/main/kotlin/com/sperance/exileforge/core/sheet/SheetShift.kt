package com.sperance.exileforge.core.sheet

import kotlin.math.abs
import kotlin.math.max

/** Куда сдвинулась цифра листа против прежней. */
enum class Trend { UP, DOWN }

/**
 * Сдвиг цифр листа против прежних: какие выросли ([up]) и какие упали ([down]). Цифра, которой прежде не было, сравнивается с нулём;
 * разница меньше тысячной доли числа - шум счёта, не сдвиг.
 */
class SheetShift(val up: Set<String>, val down: Set<String>) {
    /** Сдвиг цифры [key]; не сдвинулась - null. */
    fun of(key: String): Trend? = when (key) {
        in up -> Trend.UP
        in down -> Trend.DOWN
        else -> null
    }

    val isEmpty: Boolean get() = up.isEmpty() && down.isEmpty()

    companion object {
        val NONE = SheetShift(emptySet(), emptySet())

        /** Сдвиг листа [now] против прежнего [before]: по каждой цифре обоих листов. */
        fun of(before: Map<String, Double>, now: Map<String, Double>): SheetShift {
            val up = mutableSetOf<String>()
            val down = mutableSetOf<String>()
            (before.keys + now.keys).forEach { key ->
                val was = before[key] ?: 0.0
                val delta = (now[key] ?: 0.0) - was
                if (abs(delta) >= NOISE * max(1.0, abs(was))) (if (delta > 0) up else down) += key
            }
            return if (up.isEmpty() && down.isEmpty()) NONE else SheetShift(up, down)
        }

        /** Доля числа, ниже которой разница - шум счёта. */
        private const val NOISE = 1e-3
    }
}
