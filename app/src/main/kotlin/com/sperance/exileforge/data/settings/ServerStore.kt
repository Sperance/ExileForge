package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.presentation.state.GameSettings
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val Context.settings by preferencesDataStore("server_settings")

/** The one server every player plays on (3.75.0): there is no address to type before the gate. */
const val DEFAULT_SERVER = "https://147.45.219.84.sslip.io/"

class ServerStore(private val context: Context) {
    // A new key (3.75.0): an address typed before it is left behind, only an administrator's choice from the Server page counts.
    private val key = stringPreferencesKey("server_override")
    val server = context.settings.data.map { it[key] ?: DEFAULT_SERVER }
    /** The administrator's address, or none — back to [DEFAULT_SERVER]. */
    suspend fun save(value: String?) { context.settings.edit { if (value == null || value == DEFAULT_SERVER) it.remove(key) else { it[key] = value } } }

    /**
     * The language the player chose, or nothing at all.
     *
     * Nothing is the answer that matters: on a first run there is no choice to honour, and the
     * app takes the device's own language instead of starting everybody in Russian.
     */
    private val languageKey = stringPreferencesKey("language")
    val language = context.settings.data.map { it[languageKey] }
    suspend fun saveLanguage(value: Lang) { context.settings.edit { it[languageKey] = value.code } }

    /** How the stash is sorted (3.30.0), by the name of the order; nothing is the default, newest first. */
    private val stashSortKey = stringPreferencesKey("stash_sort")
    val stashSort = context.settings.data.map { it[stashSortKey] }
    suspend fun saveStashSort(value: String) { context.settings.edit { it[stashSortKey] = value } }

    /** Whether the gear shelf hides what the hero wears (3.69.0); nothing is the default, everything shown. */
    private val hideWornKey = stringPreferencesKey("stash_worn_hidden")
    // Hidden unless the player shows it (3.77.0): the key is now "stash_worn_hidden", so an older «shown» is not kept.
    val stashHideWorn = context.settings.data.map { it[hideWornKey] != "false" }
    suspend fun saveStashHideWorn(value: Boolean) { context.settings.edit { it[hideWornKey] = value.toString() } }

    /** The player's settings (3.77.0) as one document; an unreadable one falls back to the defaults. */
    private val gameSettingsKey = stringPreferencesKey("game_settings")
    val gameSettings = context.settings.data.map { prefs ->
        prefs[gameSettingsKey]?.let { runCatching { WireJson.decodeFromString(GameSettings.serializer(), it) }.getOrNull() } ?: GameSettings()
    }
    suspend fun saveGameSettings(value: GameSettings) { context.settings.edit { it[gameSettingsKey] = WireJson.encodeToString(GameSettings.serializer(), value) } }

    /** Which shelves of the fight's log are shown (3.37.0), by their names; nothing is the default, blows and ailments. */
    private val logFilterKey = stringPreferencesKey("log_filter")
    val logFilter = context.settings.data.map { it[logFilterKey] }
    suspend fun saveLogFilter(value: String) { context.settings.edit { it[logFilterKey] = value } }

    /**
     * Which languages a server said it serves, kept so the picker is right before it answers.
     *
     * The manifest decides what may be chosen, and it arrives a moment after the first frame.
     * Without this the list would flicker from two entries to three on every launch.
     */
    suspend fun languages(server: String): List<String> =
        context.settings.data.first()[languagesKey(server)]?.split(',')?.filter { it.isNotBlank() }.orEmpty()

    suspend fun saveLanguages(server: String, codes: List<String>) {
        context.settings.edit { it[languagesKey(server)] = codes.joinToString(",") }
    }

    private fun languagesKey(server: String) = stringPreferencesKey("languages:$server")

    /**
     * The server's dictionary, stored verbatim per server and language.
     *
     * It is kept beside the hash the manifest gave it: the stored copy is reused only while the
     * server still reports that same fingerprint, so a reseeded server is never shown stale names.
     * Two servers may seed different text, so the base URL is part of the key.
     */
    suspend fun locale(server: String, language: String): Pair<String, String>? = locales.read(server, language)
    suspend fun saveLocale(server: String, language: String, hash: String, document: String) = locales.write(server, hash, document, language)

    /**
     * The server's icon set, stored verbatim beside the fingerprint it was served with.
     *
     * Unlike the dictionary it has no language: a drawing says the same thing in both, so the key
     * is the server alone. Two servers may still seed different art, which is why it is a key.
     */
    suspend fun icons(server: String): Pair<String, String>? = iconSets.read(server)
    suspend fun saveIcons(server: String, hash: String, document: String) = iconSets.write(server, hash, document)

    /**
     * The server's portraits (since 2.31.0): every SVG verbatim beside the fingerprint it was served
     * with, by key, in one file per server — a changed file is fetched again, the rest never are.
     */
    suspend fun portraits(server: String): Map<String, Pair<String, String>> {
        val stored = portraitSets.read(server)?.second ?: return emptyMap()
        return withContext(Dispatchers.Default) {
            runCatching {
                WireJson.parseToJsonElement(stored).jsonObject.mapValues { (_, value) ->
                    value.jsonObject.let { it.getValue("hash").jsonPrimitive.content to it.getValue("body").jsonPrimitive.content }
                }
            }.getOrDefault(emptyMap())
        }
    }

    suspend fun savePortraits(server: String, portraits: Map<String, Pair<String, String>>) {
        val document = buildJsonObject {
            portraits.forEach { (key, file) -> put(key, buildJsonObject { put("hash", file.first); put("body", file.second) }) }
        }
        // The set has no fingerprint of its own: each portrait carries one, so the file is its own key.
        portraitSets.write(server, PORTRAITS_HASH, document.toString())
    }

    /**
     * One chunk of the world's content, kept per server and file beside the fingerprint the manifest gave it:
     * a chunk is fetched again only when its own fingerprint moves, so a changed table costs one file.
     */
    suspend fun chunk(server: String, file: String): Pair<String, String>? = chunks.read(server, file)

    /** The server's manifest as last served (3.1.0): a cold start without the network reads its content by it. */
    suspend fun manifest(server: String): String? = manifests.read(server)?.second
    suspend fun saveManifest(server: String, document: String) {
        if (manifests.read(server)?.second != document) manifests.write(server, document.hashCode().toString(), document)
    }
    suspend fun saveChunk(server: String, file: String, hash: String, document: String) = chunks.write(server, hash, document, file)

    /**
     * The journal of a hero's run, as text, so events the server has not taken survive the process: what
     * was not sent is sent on the next launch, as long as the hero's run is still the one it names.
     */
    suspend fun journal(heroId: String): String? = withContext(Dispatchers.IO) { journalFile(heroId).takeIf { it.isFile }?.readText() }
    suspend fun saveJournal(heroId: String, text: String) = withContext(Dispatchers.IO) { journalFile(heroId).apply { parentFile?.mkdirs() }.writeText(text) }
    suspend fun clearJournal(heroId: String) = withContext(Dispatchers.IO) { journalFile(heroId).delete(); Unit }
    private fun journalFile(heroId: String) = File(context.filesDir, "journal/$heroId.json")

    /**
     * The commands one server has not answered yet (3.30.0), in order, as text: they survive the process and
     * go out with their own keys on the next launch. The file is replaced whole, never torn.
     */
    suspend fun commands(server: String): String? = withContext(Dispatchers.IO) { serverFile("commands", server).takeIf { it.isFile }?.readText() }
    suspend fun saveCommands(server: String, text: String) = withContext(Dispatchers.IO) { replace(serverFile("commands", server), text) }

    /**
     * The last snapshot of one hero on one server, beside the API revision it was read under (3.30.0): the
     * fast start draws the hero from it before the server answers. Another revision reads as no copy at all.
     */
    suspend fun heroCopy(server: String, heroId: String): String? = withContext(Dispatchers.IO) { serverFile("heroes", "$server|$heroId").takeIf { it.isFile }?.readText() }
    suspend fun saveHeroCopy(server: String, heroId: String, text: String) = withContext(Dispatchers.IO) { replace(serverFile("heroes", "$server|$heroId"), text) }

    /** The `until` of the last «Пока вас не было» shown for a hero on a server (3.69.0): each catch-up is shown once. */
    suspend fun craftsAwaySeen(server: String, heroId: String): Long = context.settings.data.first()[awayKey(server, heroId)]?.toLongOrNull() ?: 0L
    suspend fun saveCraftsAwaySeen(server: String, heroId: String, until: Long) { context.settings.edit { it[awayKey(server, heroId)] = until.toString() } }
    private fun awayKey(server: String, heroId: String) = stringPreferencesKey("crafts_away:$server|$heroId")

    /** The hero last played on a server: the one a launch with a kept session opens straight into. */
    suspend fun lastHero(server: String): String? = context.settings.data.first()[lastHeroKey(server)]
    suspend fun saveLastHero(server: String, heroId: String?) {
        context.settings.edit { if (heroId == null) it.remove(lastHeroKey(server)) else it[lastHeroKey(server)] = heroId }
    }
    private fun lastHeroKey(server: String) = stringPreferencesKey("last_hero:$server")

    private fun serverFile(kind: String, name: String) = File(context.filesDir, "$kind/" + UUID.nameUUIDFromBytes(name.toByteArray()) + ".json")

    /** Written beside and moved over: a process killed mid-write leaves the old file, not half of the new one. */
    private fun replace(target: File, text: String) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(text)
        if (!temp.renameTo(target)) { target.delete(); temp.renameTo(target) }
    }

    private val locales = Documents("locale")
    private val iconSets = Documents("icons")
    private val portraitSets = Documents("portraits")
    private val chunks = Documents("content")
    private val manifests = Documents("manifest")

    /**
     * Served documents kept on the device: the body in a file, its fingerprint in DataStore.
     *
     * A preference file is read whole before the first value comes out of it and rewritten whole on
     * every edit, so a dictionary or an icon set kept there would slow down reading a token at start.
     * The hash is dropped before the file is written and set after, so a torn write reads as a
     * missing document, never as a stale one under a fresh hash.
     */
    private inner class Documents(private val kind: String) {
        private fun hashKey(server: String, name: String) =
            stringPreferencesKey(listOf(kind, server, name, "hash").filter { it.isNotEmpty() }.joinToString(":"))
        private fun file(server: String, name: String) =
            File(context.filesDir, "$kind/" + UUID.nameUUIDFromBytes((if (name.isEmpty()) server else "$server|$name").toByteArray()) + ".json")

        suspend fun read(server: String, name: String = ""): Pair<String, String>? = withContext(Dispatchers.IO) {
            val hash = context.settings.data.first()[hashKey(server, name)] ?: return@withContext null
            file(server, name).takeIf { it.isFile }?.readText()?.let { hash to it }
        }

        suspend fun write(server: String, hash: String, document: String, name: String = "") {
            withContext(Dispatchers.IO) {
                context.settings.edit { it.remove(hashKey(server, name)) }
                file(server, name).apply { parentFile?.mkdirs() }.writeText(document)
                context.settings.edit { it[hashKey(server, name)] = hash }
            }
        }
    }

    /**
     * Whether the last session was played on this device's own account.
     *
     * A kept token restores a session; this bit says that one may also be *made* silently when the
     * token is gone or refused. It is set by playing and cleared by signing out, so an explicit
     * sign-out is not undone by the next launch.
     */
    private val deviceKey = stringPreferencesKey("device_session")
    val deviceSession = context.settings.data.map { it[deviceKey] == "true" }
    suspend fun saveDeviceSession(value: Boolean) { context.settings.edit { it[deviceKey] = value.toString() } }

    /** The session token for one server, sealed by the Keystore ([SecretBox], 3.48.0). */
    suspend fun token(server: String): String? = secret(tokenKey(server))
    suspend fun saveToken(server: String, value: String?) = saveSecret(tokenKey(server), value)
    private fun tokenKey(server: String) = stringPreferencesKey("token:$server")

    /**
     * The device's own secret on one server (3.48.0): the server issued it at registration and knows only its hash — the
     * account of this device is whoever holds it. Sealed like the token; losing it (a data wipe) means a new account.
     */
    suspend fun deviceSecret(server: String): String? = secret(deviceSecretKey(server))
    suspend fun saveDeviceSecret(server: String, value: String?) = saveSecret(deviceSecretKey(server), value)
    private fun deviceSecretKey(server: String) = stringPreferencesKey("device:$server")

    private suspend fun secret(key: Preferences.Key<String>): String? = context.settings.data.first()[key]?.let(SecretBox::open)
    private suspend fun saveSecret(key: Preferences.Key<String>, value: String?) {
        val sealed = value?.let(SecretBox::seal)
        context.settings.edit { if (sealed == null) it.remove(key) else it[key] = sealed }
    }

    private companion object {
        const val PORTRAITS_HASH = "set"
    }
}
