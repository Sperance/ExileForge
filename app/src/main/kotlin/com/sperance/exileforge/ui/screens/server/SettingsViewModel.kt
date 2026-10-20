package com.sperance.exileforge.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.core.world.WorldRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Экран «Настройки»: значения из [PreferencesRepository], правка - сразу на экран и на устройство. */
class SettingsViewModel(private val prefs: PreferencesRepository, private val world: WorldRepository) : ViewModel() {
    val settings: StateFlow<GameSettings> = prefs.settings

    /** Скорости боя на выбор - из правил (`fight.speeds`), не из клиента; следит за сменой контента, контента ещё нет - пусто. */
    val speeds: StateFlow<List<Int>> = world.state.map(::speedsOf)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARE_STOP_MS), speedsOf(world.state.value))

    /** Строка окна загрузки на этот показ из [keys], не та, что в прошлый; строк нет - null. */
    suspend fun launchLine(keys: List<String>): String? = prefs.launchLine(keys)

    fun change(edit: GameSettings.() -> GameSettings) {
        viewModelScope.launch { prefs.editSettings(edit) }
    }
}

private fun speedsOf(world: World): List<Int> = world.content?.rules?.fight?.speeds.orEmpty()

private const val SHARE_STOP_MS = 5_000L
