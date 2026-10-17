package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.display.ItemFilter
import com.sperance.exileforge.core.display.ItemFilters
import com.sperance.exileforge.core.display.ItemShelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Настройки игрока на устройстве как единственный источник правды: экраны читают потоки
 * отсюда, а не копию в общем состоянии. Запись идёт в [SettingsStore], поток обновляется сам.
 */
class PreferencesRepository(private val store: SettingsStore, scope: CoroutineScope) {
    val settings: StateFlow<GameSettings> = store.gameSettings.stateIn(scope, SharingStarted.Eagerly, GameSettings())

    /** Запомненные фильтры списков предметов. */
    val itemFilters: StateFlow<ItemFilters> = store.itemFilters.stateIn(scope, SharingStarted.Eagerly, ItemFilters())

    /** Какие строки журнала боя видны (из общего состояния сюда). */
    val logFilter: StateFlow<Set<LogKind>> = store.logFilter.map { LogKind.parse(it) }.stateIn(scope, SharingStarted.Eagerly, LogKind.DEFAULT)

    suspend fun saveSettings(value: GameSettings) = store.saveGameSettings(value)

    /** Правка настроек от записанного на устройстве, а не от потока: на старте поток ещё может держать умолчания. */
    suspend fun editStored(edit: GameSettings.() -> GameSettings) {
        val stored = store.gameSettings.first()
        val next = stored.edit()
        if (next != stored) store.saveGameSettings(next)
    }

    /** Правка настроек от их текущего значения; ничего не изменилось - на устройство не пишется. */
    suspend fun editSettings(edit: GameSettings.() -> GameSettings) {
        val next = settings.value.edit()
        if (next != settings.value) store.saveGameSettings(next)
    }

    suspend fun saveItemFilter(shelf: ItemShelf, value: ItemFilter) = store.saveItemFilters(itemFilters.value.with(shelf, value))

    suspend fun saveLogFilter(value: Set<LogKind>) = store.saveLogFilter(LogKind.write(value))
}
