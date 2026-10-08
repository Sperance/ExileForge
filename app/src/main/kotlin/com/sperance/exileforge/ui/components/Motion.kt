package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.sperance.exileforge.presentation.state.GameSettings

/** Whether the screens move (3.77.0, «Анимации» in the settings): the pulses, glows and marching roads stand still when off. */
val LocalMotion = staticCompositionLocalOf { true }

/** The player's settings for the screens that read one directly: the fight's numbers and its pause. */
val LocalSettings = staticCompositionLocalOf { GameSettings() }

/** Часы декора (4.0.0, прежде часы «Астролябии»): доля круга от 0 до 1 за [periodMs]; стоят на месте, когда анимации выключены. */
@Composable fun motionClock(periodMs: Int, label: String): Float {
    if (!LocalMotion.current) return remember { mutableFloatStateOf(0f) }.floatValue
    val time by rememberInfiniteTransition(label = label).animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = label)
    return time
}
