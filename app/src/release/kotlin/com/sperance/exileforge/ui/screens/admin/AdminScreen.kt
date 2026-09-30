package com.sperance.exileforge.ui.screens.admin

import androidx.compose.runtime.Composable
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState

/** Инструменты администратора живут только в отладочной сборке (3.44.0): в релизе вкладки нет. */
@Suppress("UNUSED_PARAMETER")
@Composable fun AdminScreen(s: ForgeState, vm: ForgeViewModel) = Unit
