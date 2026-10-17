package com.sperance.exileforge.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.fightSpeeds
import com.sperance.exileforge.core.i18n.LaunchLines
import com.sperance.exileforge.core.world.WorldRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Экран «Настройки»: значения из [PreferencesRepository], правка - сразу на экран и на устройство. */
class SettingsViewModel(private val prefs: PreferencesRepository, private val world: WorldRepository) : ViewModel() {
    val settings: StateFlow<GameSettings> = prefs.settings

    /** Скорости боя на выбор - из правил (`fight.speeds`), не из клиента. */
    val speeds: List<Int> get() = fightSpeeds(world.state.value.content)

    /**
     * Строка окна загрузки на этот показ из [keys]: не та, что в прошлый ([GameSettings.lastLaunchLine], прочитано с устройства), выбор
     * помнится; строк нет - null.
     */
    suspend fun launchLine(keys: List<String>): String? {
        var picked: String? = null
        prefs.editStored {
            val next = LaunchLines.next(keys, lastLaunchLine)
            picked = next
            next?.let { copy(lastLaunchLine = it) } ?: this
        }
        return picked
    }

    fun change(edit: GameSettings.() -> GameSettings) {
        viewModelScope.launch { prefs.saveSettings(settings.value.edit()) }
    }
}
