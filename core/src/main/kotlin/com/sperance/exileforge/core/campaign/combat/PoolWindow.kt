package com.sperance.exileforge.core.campaign.combat

import kotlin.math.ceil
import kotlin.math.max

/**
 * Запас бойца за последние [window] секунд боя шагами [step]: кольцо замеров без выделений на шаг. [drop] - насколько запас упал от
 * наибольшего в окне до последнего замера - потеря «Резкого урона»; лечение внутри окна её уменьшает.
 */
internal class PoolWindow(window: Double, step: Double) {
    private val samples = DoubleArray(ceil(window / step - EPSILON).toInt() + 1)
    private var next = 0
    private var filled = 0

    /** Замер шага: запас [pool] сейчас. */
    fun record(pool: Double) {
        samples[next] = pool
        next = (next + 1) % samples.size
        if (filled < samples.size) filled++
    }

    /** Падение запаса за окно: наибольший замер окна минус последний; замеров нет - 0. */
    fun drop(): Double {
        if (filled == 0) return 0.0
        var peak = 0.0
        for (i in 0 until filled) peak = max(peak, samples[i])
        return (peak - samples[(next - 1 + samples.size) % samples.size]).coerceAtLeast(0.0)
    }
}

/** Допуск деления окна на шаги: 2 с по 1/60 - ровно 120 шагов, а не 121. */
private const val EPSILON = 1e-9
