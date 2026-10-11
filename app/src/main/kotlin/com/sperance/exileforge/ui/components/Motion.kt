package com.sperance.exileforge.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sperance.exileforge.presentation.state.GameSettings

/** Whether the screens move (3.77.0, «Анимации» in the settings): the pulses, glows and marching roads stand still when off. */
val LocalMotion = staticCompositionLocalOf { true }

/** The player's settings for the screens that read one directly: the fight's numbers and its pause. */
val LocalSettings = staticCompositionLocalOf { GameSettings() }

/**
 * Общие часы декора (4.6.6): секунды одним циклом на всё приложение, шагом [STEP_NS] (30 раз в секунду) - декор не держит
 * экран в 60 кадрах и не заводит свой бесконечный переход на каждый пульс. Стоят, когда анимации выключены (`LocalMotion`) и
 * когда приложение не на экране. Бой и сцена забега идут своими часами - их анимация не декор.
 */
@Stable
class DecorClock {
    internal val seconds = mutableFloatStateOf(0f)

    /** Секунды декора; чтение подписывает читателя на шаг часов. */
    val now: Float get() = seconds.floatValue
}

val LocalDecorClock = staticCompositionLocalOf<DecorClock?> { null }

/** Хозяин часов декора: под ним [motionClock] и [LocalDecorClock] идут общими часами. */
@Composable fun DecorClockHost(content: @Composable () -> Unit) {
    val clock = remember { DecorClock() }
    val moving = LocalMotion.current
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(moving, lifecycle) {
        if (!moving) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last == 0L) last = now
                    if (now - last >= STEP_NS) {
                        clock.seconds.floatValue += ((now - last) / 1e9f).coerceAtMost(MAX_STEP_S)
                        last = now
                    }
                }
            }
        }
    }
    CompositionLocalProvider(LocalDecorClock provides clock, content = content)
}

/** Часы декора (4.0.0; 4.6.6 - от общих часов): доля круга от 0 до 1 за [periodMs]; стоят на месте, когда анимации выключены. */
@Composable fun motionClock(periodMs: Int, @Suppress("UNUSED_PARAMETER") label: String = ""): Float {
    if (!LocalMotion.current) return 0f
    val seconds = LocalDecorClock.current?.now ?: return 0f
    return (seconds * MS_PER_S % periodMs) / periodMs
}

/**
 * Доля круга за [periodMs] состоянием (4.6.6): читать в фазе рисования - декор перерисовывается шагом часов без перекомпозиции;
 * без анимаций - [still].
 */
@Composable fun motionPhase(periodMs: Int, still: Float = 0f): State<Float> {
    val clock = LocalDecorClock.current
    val moving = LocalMotion.current
    return remember(clock, moving, periodMs, still) { derivedStateOf { if (!moving || clock == null) still else (clock.now * MS_PER_S % periodMs) / periodMs } }
}

/** Часы декора в секундах (4.6.6) - для рисунков, что сами считают фазы. */
@Composable fun decorSeconds(): Float = if (LocalMotion.current) LocalDecorClock.current?.now ?: 0f else 0f

/** Шаг часов декора: 30 раз в секунду. */
private const val STEP_NS = 33_333_333L

/** Шаг после паузы (приложение вернулось на экран) не прыгает дальше этого. */
private const val MAX_STEP_S = .1f

private const val MS_PER_S = 1000f
