package com.sperance.exileforge.ui.screens.session

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.admin.SanctionModel

/** Экран санкции: модель ядра [model] живёт, пока жив экран. */
class NoticeViewModel(val model: SanctionModel) : ViewModel()
