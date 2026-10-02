package com.sperance.exileforge.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.sperance.exileforge.presentation.state.GameSettings

/** Whether the screens move (3.77.0, «Анимации» in the settings): the pulses, glows and marching roads stand still when off. */
val LocalMotion = staticCompositionLocalOf { true }

/** The player's settings for the screens that read one directly: the fight's numbers and its pause. */
val LocalSettings = staticCompositionLocalOf { GameSettings() }
