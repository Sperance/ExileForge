package com.sperance.exileforge.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.presentation.state.GameSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Экран «Настройки»: значения из [PreferencesRepository], правка - сразу на экран и на устройство. */
class SettingsViewModel(private val prefs: PreferencesRepository) : ViewModel() {
    val settings: StateFlow<GameSettings> = prefs.settings

    fun change(edit: GameSettings.() -> GameSettings) {
        viewModelScope.launch { prefs.saveSettings(settings.value.edit()) }
    }
}
