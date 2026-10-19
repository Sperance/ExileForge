package com.sperance.exileforge.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.app.PreferencesRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Экран «Настройки»: значения из [PreferencesRepository], правка - сразу на экран и на устройство. */
class SettingsViewModel(private val prefs: PreferencesRepository) : ViewModel() {
    val settings: StateFlow<GameSettings> = prefs.settings

    /** Строка окна загрузки на этот показ из [keys], не та, что в прошлый; строк нет - null. */
    suspend fun launchLine(keys: List<String>): String? = prefs.launchLine(keys)

    fun change(edit: GameSettings.() -> GameSettings) {
        viewModelScope.launch { prefs.editSettings(edit) }
    }
}
