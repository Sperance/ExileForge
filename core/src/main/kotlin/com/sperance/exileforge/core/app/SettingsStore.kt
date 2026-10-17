package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.display.ItemFilters
import com.sperance.exileforge.core.i18n.Lang
import kotlinx.coroutines.flow.Flow

/** Единственный сервер, на котором играют все: адреса до входа никто не вводит. */
const val DEFAULT_SERVER = "https://147.45.219.84.sslip.io/"

/**
 * Настройки игрока на устройстве глазами ядра: адрес сервера, язык, настройки игры, фильтры списков и журнала боя. Хранит их
 * `:app` (DataStore), ядро читает потоки и пишет через этот интерфейс.
 */
interface SettingsStore {
    /** Адрес сервера: выбор администратора или [DEFAULT_SERVER]. */
    val server: Flow<String>

    /** Язык, выбранный игроком; null - не выбирал. */
    val language: Flow<String?>

    val gameSettings: Flow<GameSettings>

    val itemFilters: Flow<ItemFilters>

    /** Видимые строки журнала боя записью `LogKind.write`; null - обычные. */
    val logFilter: Flow<String?>

    /** Адрес администратора или null - назад к [DEFAULT_SERVER]. */
    suspend fun save(value: String?)

    suspend fun saveLanguage(value: Lang)

    suspend fun saveGameSettings(value: GameSettings)

    suspend fun saveItemFilters(value: ItemFilters)

    suspend fun saveLogFilter(value: String)
}
