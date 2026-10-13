package com.sperance.exileforge.data.settings

import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.presentation.state.ItemFilter
import com.sperance.exileforge.presentation.state.ItemFilters
import com.sperance.exileforge.presentation.state.ItemShelf
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

    /** Запомненные фильтры списков предметов (4.2.0). */
    val itemFilters: StateFlow<ItemFilters> = store.itemFilters.stateIn(scope, SharingStarted.Eagerly, ItemFilters())

    /** Какие строки журнала боя видны (3.80.32: из общего состояния сюда). */
    val logFilter: StateFlow<Set<LogKind>> = store.logFilter.map { LogKind.parse(it) }.stateIn(scope, SharingStarted.Eagerly, LogKind.DEFAULT)

    suspend fun saveSettings(value: GameSettings) = store.saveGameSettings(value)

    /** Правка настроек от их текущего значения; ничего не изменилось - на устройство не пишется. */
    suspend fun editSettings(edit: GameSettings.() -> GameSettings) {
        val next = settings.value.edit()
        if (next != settings.value) store.saveGameSettings(next)
    }

    suspend fun saveItemFilter(shelf: ItemShelf, value: ItemFilter) = store.saveItemFilters(itemFilters.value.with(shelf, value))

    suspend fun saveLogFilter(value: Set<LogKind>) = store.saveLogFilter(LogKind.write(value))
}
