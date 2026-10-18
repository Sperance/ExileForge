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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.rules.content.ContentIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Whether the screens move («Анимации» in the settings): the pulses, glows and marching roads stand still when off. */
val LocalMotion = staticCompositionLocalOf { true }

/**
 * Уходит ли экран сменой экрана (`NavDisplay` доводит его выход): пока уходит, его декор стоит на последней фазе и не перерисовывается
 * шагом часов. Читается в рисовании; вне стека экранов - никогда.
 */
val LocalScreenAway = staticCompositionLocalOf<State<Boolean>> { NEVER_AWAY }

private val NEVER_AWAY: State<Boolean> = mutableStateOf(false)

/** The player's settings for the screens that read one directly: the fight's numbers and its pause. */
val LocalSettings = staticCompositionLocalOf { GameSettings() }

/**
 * Контент игры с сервера для мест, что печатают его числа (строки характеристик боя, Разлома и испытаний): задаёт корень игры;
 * null - контент ещё не пришёл (или превью), строка печатается общей.
 */
val LocalContentIndex = staticCompositionLocalOf<ContentIndex?> { null }

/**
 * Правка настроек устройства для мест без своей модели (свёртки карточки предмета): правка ложится сразу на экран и на устройство;
 * null - приложения нет (превью), место держит состояние само.
 */
val LocalSettingsEdit = staticCompositionLocalOf<((GameSettings.() -> GameSettings) -> Unit)?> { null }

/**
 * Общие часы декора: секунды одним циклом на всё приложение, шагом около 30 раз в секунду (между шагами спят [SLEEP_MS]) - декор не держит
 * экран в 60 кадрах и не заводит свой бесконечный переход на каждый пульс. Стоят, когда анимации выключены (`LocalMotion`) и
 * когда приложение не на экране. Бой и сцена забега идут своими часами - их анимация не декор. Стоят и без
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

/**
 * Хозяин часов декора: под ним [motionPhase], [decorTime] и [LocalDecorClock] идут общими часами.
 *
 * Нет читателей - нет кадров: часы ждут первого. Кадр бесконечный, а не `withFrameNanos`: политика бесконечных анимаций тестов UI
 * останавливает часы. Между шагами часы спят, а не просыпаются каждый vsync.
 */
@Composable fun DecorClockHost(content: @Composable () -> Unit) {
    val clock = remember { DecorClock() }
    val moving = LocalMotion.current
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(moving, lifecycle) {
        if (!moving) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                snapshotFlow { clock.readers.intValue > 0 }.first { it }
                var last = 0L
                while (clock.readers.intValue > 0) {
                    withInfiniteAnimationFrameNanos { now ->
                        if (last != 0L) clock.seconds.floatValue += ((now - last) / 1e9f).coerceAtMost(MAX_STEP_S)
                        last = now
                    }
                    delay(SLEEP_MS)
                }
            }
        }
    }
    CompositionLocalProvider(LocalDecorClock provides clock, content = content)
}

/**
 * Доля круга за [periodMs] состоянием: читать в фазе рисования - декор перерисовывается шагом часов без перекомпозиции;
 * без анимаций - [still].
 */
@Composable fun motionPhase(periodMs: Int, still: Float = 0f): State<Float> {
    val clock = LocalDecorClock.current
    val moving = LocalMotion.current
    DecorReader(clock, moving)
    val away = LocalScreenAway.current
    return remember(clock, moving, away, periodMs, still) { held(still, away) { if (!moving || clock == null) still else (clock.now * MS_PER_S % periodMs) / periodMs } }
}

/**
 * Часы декора в секундах состоянием: читать в фазе рисования - рисунок, что сам считает фазы, перерисовывается шагом
 * часов без перекомпозиции; без анимаций - 0.
 */
@Composable fun decorTime(): State<Float> {
    val clock = LocalDecorClock.current
    val moving = LocalMotion.current
    DecorReader(clock, moving)
    val away = LocalScreenAway.current
    return remember(clock, moving, away) { held(0f, away) { if (!moving || clock == null) 0f else clock.now } }
}

/** Значение [read], что стоит на последнем прочитанном, пока экран уходит ([away]): часы его тогда не будят. */
private fun held(start: Float, away: State<Boolean>, read: () -> Float): State<Float> {
    var last = start
    return derivedStateOf { if (away.value) last else read().also { last = it } }
}

/** Сон часов декора между шагами: с ожиданием следующего кадра шаг - около 30 раз в секунду. */
private const val SLEEP_MS = 24L

/** Шаг после паузы (приложение вернулось на экран) не прыгает дальше этого. */
private const val MAX_STEP_S = .1f

private const val MS_PER_S = 1000f
