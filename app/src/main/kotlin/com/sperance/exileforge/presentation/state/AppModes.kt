package com.sperance.exileforge.presentation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Режим приложения (3.80.32): игрок или инструменты администратора; выход из аккаунта возвращает игрока. */
class AppModes {
    private val mutable = MutableStateFlow(AppMode.PLAYER)
    val mode: StateFlow<AppMode> = mutable.asStateFlow()

    fun set(mode: AppMode) {
        mutable.value = mode
    }
}
