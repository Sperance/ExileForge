package com.sperance.exileforge.core.app

import kotlinx.coroutines.flow.Flow

/**
 * Мир сервера, сохранённый на устройстве: словарь, иконки, портреты, манифест и куски контента - каждый рядом с отпечатком,
 * под которым сервер его отдал, - и языки сервера. Ключ - адрес сервера: два сервера могут отдать разное.
 */
interface WorldCache {
    /** Языки, что сервер назвал своими: список языков верен до его ответа. */
    suspend fun languages(server: String): List<String>

    suspend fun saveLanguages(server: String, codes: List<String>)

    /** Словарь сервера на языке [language]: отпечаток и документ. */
    suspend fun locale(server: String, language: String): Pair<String, String>?

    suspend fun saveLocale(server: String, language: String, hash: String, document: String)

    /** Набор иконок сервера: отпечаток и документ. */
    suspend fun icons(server: String): Pair<String, String>?

    suspend fun saveIcons(server: String, hash: String, document: String)

    /** Портреты сервера по ключу: отпечаток и SVG. */
    suspend fun portraits(server: String): Map<String, Pair<String, String>>

    suspend fun savePortraits(server: String, portraits: Map<String, Pair<String, String>>)

    /** Кусок контента [file]: отпечаток и документ. */
    suspend fun chunk(server: String, file: String): Pair<String, String>?

    suspend fun saveChunk(server: String, file: String, hash: String, document: String)

    /** Манифест сервера, как он пришёл последним: холодный старт без сети читает контент по нему. */
    suspend fun manifest(server: String): String?

    suspend fun saveManifest(server: String, document: String)

    /** Мир других правил забыт: копии героев, очередь команд, журналы забегов и контент старой версии правил; забыто ли что-то. */
    suspend fun forgetWorldUnless(rules: Int): Boolean
}

/** Герой на устройстве: журнал забега, неотвеченные команды, последняя копия героя, последний герой сервера и выбор кузнеца. */
interface HeroCache {
    /** Журнал забега героя [heroId] текстом: не принятое сервером уходит на следующем запуске. */
    suspend fun journal(heroId: String): String?

    suspend fun saveJournal(heroId: String, text: String)

    suspend fun clearJournal(heroId: String)

    /** Команды сервера, на которые он ещё не ответил, по порядку, текстом. */
    suspend fun commands(server: String): String?

    suspend fun saveCommands(server: String, text: String)

    /** Последний снимок героя [heroId] на сервере: быстрый старт рисует героя по нему до ответа. */
    suspend fun heroCopy(server: String, heroId: String): String?

    suspend fun saveHeroCopy(server: String, heroId: String, text: String)

    /** Последний запущенный выбор кузнеца героя на сервере. */
    suspend fun smithChoice(server: String, heroId: String): String?

    suspend fun saveSmithChoice(server: String, heroId: String, choice: String)

    /** Герой, в которого последним играли на сервере: запуск с сохранённой сессией открывается сразу в нём. */
    suspend fun lastHero(server: String): String?

    suspend fun saveLastHero(server: String, heroId: String?)
}

/** Вход с устройства: сессия, токен и секрет устройства на сервере, отпечаток устройства. */
interface Credentials {
    /** Последняя сессия шла на аккаунте этого устройства: её можно завести молча. */
    val deviceSession: Flow<Boolean>

    suspend fun saveDeviceSession(value: Boolean)

    /** Токен сессии сервера; хранилище ключей не ответило - токена будто нет. */
    suspend fun token(server: String): String?

    suspend fun saveToken(server: String, value: String?)

    /** Секрет устройства на сервере: аккаунт устройства - тот, кто его держит. */
    suspend fun deviceSecret(server: String): String?

    suspend fun saveDeviceSecret(server: String, value: String?)

    /** Отпечаток устройства: хеш его постоянного номера; пусто - система номера не дала. */
    fun deviceFingerprint(): String
}

/** Всё, что ядро хранит на устройстве: настройки, мир сервера, герой и вход. Реализует `:app` (DataStore и файлы). */
interface DeviceStore :
    SettingsStore,
    WorldCache,
    HeroCache,
    Credentials
