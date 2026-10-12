package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sperance.exileforge.presentation.state.GameSettings
import kotlinx.coroutines.flow.first

/** Whether the screens move (3.77.0, «Анимации» in the settings): the pulses, glows and marching roads stand still when off. */
val LocalMotion = staticCompositionLocalOf { true }

/** The player's settings for the screens that read one directly: the fight's numbers and its pause. */
val LocalSettings = staticCompositionLocalOf { GameSettings() }

/**
 * Общие часы декора (4.7.0): секунды одним циклом на всё приложение, шагом [STEP_NS] (30 раз в секунду) - декор не держит
 * экран в 60 кадрах и не заводит свой бесконечный переход на каждый пульс. Стоят, когда анимации выключены (`LocalMotion`) и
 * когда приложение не на экране. Бой и сцена забега идут своими часами - их анимация не декор. С 4.9.0 стоят и без
 * читателей ([readers]): экран без декора не будит кадры; вернувшийся читатель продолжает с прежней секунды.
 */
@Stable
class DecorClock {
    internal val seconds = mutableFloatStateOf(0f)

    /** Сколько читателей декора сейчас в композиции ([DecorReader]). */
    internal val readers = mutableIntStateOf(0)

    /** Секунды декора; чтение подписывает читателя на шаг часов. */
    val now: Float get() = seconds.floatValue
}

/** Читатель часов декора на время своей жизни в композиции: пока он есть, часы идут. */
@Composable private fun DecorReader(clock: DecorClock?, moving: Boolean) {
    if (clock == null || !moving) return
    DisposableEffect(clock) {
        clock.readers.intValue++
        onDispose { clock.readers.intValue-- }
    }
}

val LocalDecorClock = staticCompositionLocalOf<DecorClock?> { null }

/** Хозяин часов декора: под ним [motionPhase], [decorTime] и [LocalDecorClock] идут общими часами. */
@Composable fun DecorClockHost(content: @Composable () -> Unit) {
    val clock = remember { DecorClock() }
    val moving = LocalMotion.current
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(moving, lifecycle) {
        if (!moving) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                // Нет читателей - нет кадров (4.9.0): часы ждут первого, а не просят каждый vsync
                snapshotFlow { clock.readers.intValue > 0 }.first { it }
                var last = 0L
                // Бесконечный кадр (не `withFrameNanos`): политика бесконечных анимаций (тесты UI) останавливает часы, как прежние переходы
                while (clock.readers.intValue > 0) {
                    withInfiniteAnimationFrameNanos { now ->
                        if (last == 0L) last = now
                        if (now - last >= STEP_NS) {
                            clock.seconds.floatValue += ((now - last) / 1e9f).coerceAtMost(MAX_STEP_S)
                            last = now
                        }
                    }
                }
            }
        }
    }
    CompositionLocalProvider(LocalDecorClock provides clock, content = content)
}

/**
 * Доля круга за [periodMs] состоянием (4.7.0): читать в фазе рисования - декор перерисовывается шагом часов без перекомпозиции;
 * без анимаций - [still].
 */
@Composable fun motionPhase(periodMs: Int, still: Float = 0f): State<Float> {
    val clock = LocalDecorClock.current
    val moving = LocalMotion.current
    DecorReader(clock, moving)
    return remember(clock, moving, periodMs, still) { derivedStateOf { if (!moving || clock == null) still else (clock.now * MS_PER_S % periodMs) / periodMs } }
}

/**
 * Часы декора в секундах состоянием (4.9.0): читать в фазе рисования - рисунок, что сам считает фазы, перерисовывается шагом
 * часов без перекомпозиции; без анимаций - 0.
 */
@Composable fun decorTime(): State<Float> {
    val clock = LocalDecorClock.current
    val moving = LocalMotion.current
    DecorReader(clock, moving)
    return remember(clock, moving) { derivedStateOf { if (!moving || clock == null) 0f else clock.now } }
}

/** Шаг часов декора: 30 раз в секунду. */
private const val STEP_NS = 33_333_333L

/** Шаг после паузы (приложение вернулось на экран) не прыгает дальше этого. */
private const val MAX_STEP_S = .1f

private const val MS_PER_S = 1000f
