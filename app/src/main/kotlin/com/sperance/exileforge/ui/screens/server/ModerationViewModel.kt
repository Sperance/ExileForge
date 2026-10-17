package com.sperance.exileforge.ui.screens.server

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.admin.ModerationModel

/** Окно модерации: модель ядра [model] живёт, пока жив экран. */
class ModerationViewModel(val model: ModerationModel) : ViewModel()
