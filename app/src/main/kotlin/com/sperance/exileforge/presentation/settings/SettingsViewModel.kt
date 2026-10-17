package com.sperance.exileforge.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.presentation.state.fightSpeeds
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Экран «Настройки»: значения из [PreferencesRepository], правка - сразу на экран и на устройство. */
class SettingsViewModel(private val prefs: PreferencesRepository, private val world: WorldRepository) : ViewModel() {
    val settings: StateFlow<GameSettings> = prefs.settings

    /** Скорости боя на выбор - из правил (`fight.speeds`), не из клиента. */
    val speeds: List<Int> get() = fightSpeeds(world.state.value.content)

    fun change(edit: GameSettings.() -> GameSettings) {
        viewModelScope.launch { prefs.saveSettings(settings.value.edit()) }
    }
}
