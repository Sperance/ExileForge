package com.sperance.exileforge.ui.screens.hall

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.hall.HallModel

/** Доска славы: модель ядра [model] живёт, пока жив экран. */
class HallViewModel(val model: HallModel) : ViewModel()
