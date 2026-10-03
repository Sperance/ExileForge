package com.sperance.exileforge.data.settings

import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.presentation.state.StashSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Настройки игрока на устройстве как единственный источник правды (3.80.6): экраны читают потоки
 * отсюда, а не копию в общем состоянии. Запись идёт в [ServerStore], поток обновляется сам.
 */
class PreferencesRepository(private val store: ServerStore, scope: CoroutineScope) {
    val settings: StateFlow<GameSettings> = store.gameSettings.stateIn(scope, SharingStarted.Eagerly, GameSettings())
    val stashSort: StateFlow<StashSort> = store.stashSort.map { StashSort.of(it) }.stateIn(scope, SharingStarted.Eagerly, StashSort.NEWEST)
    val stashHideWorn: StateFlow<Boolean> = store.stashHideWorn.stateIn(scope, SharingStarted.Eagerly, true)

    suspend fun saveSettings(value: GameSettings) = store.saveGameSettings(value)

    suspend fun saveStashSort(value: StashSort) = store.saveStashSort(value.name)

    suspend fun saveStashHideWorn(value: Boolean) = store.saveStashHideWorn(value)
}
