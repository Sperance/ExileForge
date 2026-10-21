package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.filter.ItemFilters
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.i18n.Lang
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Настройки ядра над хранилищем `:app`: потоки идут из [SettingsStore], правки уходят в него. */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {
    /** Хранилище устройства в памяти: считает записи настроек игры. */
    private class MemoryStore : SettingsStore {
        override val server = MutableStateFlow(DEFAULT_SERVER)
        override val language = MutableStateFlow<String?>(null)
        override val gameSettings = MutableStateFlow(GameSettings())
        override val itemFilters = MutableStateFlow(ItemFilters())
        override val logFilter = MutableStateFlow<String?>(null)
        var settingsWrites = 0

        override suspend fun save(value: String?) {
            server.value = value ?: DEFAULT_SERVER
        }

        override suspend fun saveLanguage(value: Lang) {
            language.value = value.code
        }

        override suspend fun editGameSettings(edit: (GameSettings) -> GameSettings) {
            val next = edit(gameSettings.value)
            if (next == gameSettings.value) return
            settingsWrites++
            gameSettings.value = next
        }

        override suspend fun saveItemFilters(value: ItemFilters) {
            itemFilters.value = value
        }

        override suspend fun saveLogFilter(value: String) {
            logFilter.value = value
        }
    }

    /** Правка настроек ложится в хранилище и возвращается потоком; правка без изменений на устройство не пишется. */
    @Test
    fun edits_go_through_the_store_and_unchanged_ones_are_not_written() = runTest {
        val store = MemoryStore()
        val prefs = PreferencesRepository(store, backgroundScope)
        prefs.editSettings { copy(autoPause = 30) }
        runCurrent()
        assertEquals(30, prefs.settings.value.autoPause)
        assertEquals(1, store.settingsWrites)
        prefs.editSettings { this }
        assertEquals(1, store.settingsWrites)
    }

    /** Фильтр списка и журнала боя пишутся своими записями и читаются обратно. */
    @Test
    fun item_and_log_filters_round_trip() = runTest {
        val store = MemoryStore()
        val prefs = PreferencesRepository(store, backgroundScope)
        val filter = ItemShelf.STASH.defaults.copy(crafted = true)
        prefs.saveItemFilter(ItemShelf.STASH, filter)
        prefs.saveLogFilter(setOf(LogKind.entries.first()))
        runCurrent()
        assertEquals(filter, prefs.itemFilters.value[ItemShelf.STASH])
        assertEquals(setOf(LogKind.entries.first()), prefs.logFilter.value)
    }
}
