package com.sperance.exileforge.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.sperance.exileforge.presentation.ShellViewModel
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinViewModel

/**
 * Вкладка [tab] нажата снова, пока открыта (3.95.0): экран возвращается на свою главную - закрывает вложенное, что держит
 * сам, и прокручивается наверх ([onReselect]).
 */
@Composable fun OnReselect(tab: Int, onReselect: suspend () -> Unit) {
    val shell: ShellViewModel = koinViewModel()
    val action = rememberUpdatedState(onReselect)
    LaunchedEffect(tab) { shell.reselected.filter { it == tab }.collect { action.value() } }
}
