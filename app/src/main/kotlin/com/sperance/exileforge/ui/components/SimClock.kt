package com.sperance.exileforge.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.sperance.exileforge.core.campaign.Simulation
import kotlinx.coroutines.flow.first

/** Шаг симуляции за кадр не длиннее этого, с: кадр после долгой задержки не прыгает боем вперёд. */
private const val MAX_STEP_S = .05

/**
 * Часы симуляции - одни на заход, испытание и бой Разлома: пока [sim] бодра ([Simulation.awake]), каждый кадр - один её шаг и
 * секунды на экране (значение - их сумма); уснула (пауза, лист поверх, лобби, итоги) - кадров не берут и ждут, пока её разбудит
 * команда или ответ сервера. Значение читают в рисовании - боем и сценой перерисовывается только кадр, не композиция.
 */
@Composable fun rememberSimClock(sim: Simulation): FloatState {
    val clock = remember(sim) { mutableFloatStateOf(0f) }
    LaunchedEffect(sim) {
        while (true) {
            sim.awake.first { it }
            var last = 0L
            while (sim.awake.value) {
                withFrameNanos { now ->
                    if (last != 0L) {
                        val dt = ((now - last) / 1e9).coerceAtMost(MAX_STEP_S)
                        sim.update(dt)
                        clock.floatValue += dt.toFloat()
                    }
                    last = now
                }
            }
        }
    }
    return clock
}
