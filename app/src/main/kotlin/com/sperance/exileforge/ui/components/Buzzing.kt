package com.sperance.exileforge.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.sperance.exileforge.core.session.Buzzes

/**
 * Вибрации игры ([Buzzes] из DI) для экранов без своей модели: удержание кнопки, отметка в сумке, опасность в бою, проба в настройках.
 * Даёт их корень приложения; null - приложения нет (превью), вибрации молчат.
 */
val LocalBuzzes = staticCompositionLocalOf<Buzzes?> { null }
