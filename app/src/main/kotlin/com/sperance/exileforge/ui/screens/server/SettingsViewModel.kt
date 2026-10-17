package com.sperance.exileforge.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.fightSpeeds
import com.sperance.exileforge.core.world.WorldRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Экран «Настройки»: значения из [PreferencesRepository], правка - сразу на экран и на устройство. */
class SettingsViewModel(private val prefs: PreferencesRepository, private val world: WorldRepository) : ViewModel() {
    val settings: StateFlow<GameSettings> = prefs.settings

    /** Скорости боя на выбор - из правил (`fight.speeds`), не из клиента. */
    val speeds: List<Int> get() = fightSpeeds(world.state.value.content)

    /** Строка окна загрузки на этот показ из [keys], не та, что в прошлый; строк нет - null. */
    suspend fun launchLine(keys: List<String>): String? = prefs.launchLine(keys)

    fun change(edit: GameSettings.() -> GameSettings) {
        viewModelScope.launch { prefs.editSettings(edit) }
    }
}
