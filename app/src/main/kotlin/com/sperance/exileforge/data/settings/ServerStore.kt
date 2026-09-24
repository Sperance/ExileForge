package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.Lang
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Context.settings by preferencesDataStore("server_settings")

class ServerStore(private val context: Context) {
    private val key = stringPreferencesKey("base_url")
    val server = context.settings.data.map { it[key] ?: "http://10.0.2.2:8080/" }
    suspend fun save(value: String) { context.settings.edit { it[key] = value } }

    suspend fun filters(server: String, catalog: String): String? = context.settings.data.first()[stringPreferencesKey("filters:$server:$catalog")]
    suspend fun saveFilters(server: String, catalog: String, value: String) { context.settings.edit { it[stringPreferencesKey("filters:$server:$catalog")] = value } }

    /**
     * The language the player chose, or nothing at all.
     *
     * Nothing is the answer that matters: on a first run there is no choice to honour, and the
     * app takes the device's own language instead of starting everybody in Russian.
     */
    private val languageKey = stringPreferencesKey("language")
    val language = context.settings.data.map { it[languageKey] }
    suspend fun saveLanguage(value: Lang) { context.settings.edit { it[languageKey] = value.code } }

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
    suspend fun locale(server: String, language: String): Pair<String, String>? {
        val stored = context.settings.data.first()
        val hash = stored[hashKey(server, language)] ?: return null
        val document = stored[documentKey(server, language)] ?: return null
        return hash to document
    }

    suspend fun saveLocale(server: String, language: String, hash: String, document: String) {
        context.settings.edit { it[hashKey(server, language)] = hash; it[documentKey(server, language)] = document }
    }

    /**
     * The server's icon set, stored verbatim beside the fingerprint it was served with.
     *
     * Unlike the dictionary it has no language: a drawing says the same thing in both, so the key
     * is the server alone. Two servers may still seed different art, which is why it is a key.
     */
    suspend fun icons(server: String): Pair<String, String>? {
        val stored = context.settings.data.first()
        val hash = stored[iconHashKey(server)] ?: return null
        val document = stored[iconBodyKey(server)] ?: return null
        return hash to document
    }

    suspend fun saveIcons(server: String, hash: String, document: String) {
        context.settings.edit { it[iconHashKey(server)] = hash; it[iconBodyKey(server)] = document }
    }

    private fun iconHashKey(server: String) = stringPreferencesKey("icons:$server:hash")
    private fun iconBodyKey(server: String) = stringPreferencesKey("icons:$server:body")

    /**
     * The server's portraits (since 2.31.0): every SVG verbatim beside the fingerprint it was served
     * with, by key, in one entry per server — a changed file is fetched again, the rest never are.
     */
    suspend fun portraits(server: String): Map<String, Pair<String, String>> {
        val stored = context.settings.data.first()[portraitKey(server)] ?: return emptyMap()
        return runCatching {
            WireJson.parseToJsonElement(stored).jsonObject.mapValues { (_, value) ->
                value.jsonObject.let { it.getValue("hash").jsonPrimitive.content to it.getValue("body").jsonPrimitive.content }
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun savePortraits(server: String, portraits: Map<String, Pair<String, String>>) {
        val document = buildJsonObject {
            portraits.forEach { (key, file) -> put(key, buildJsonObject { put("hash", file.first); put("body", file.second) }) }
        }
        context.settings.edit { it[portraitKey(server)] = document.toString() }
    }

    private fun portraitKey(server: String) = stringPreferencesKey("portraits:$server")

    /**
     * The world's reference tables (server 0.48.0), kept per server as a file beside its hash.
     *
     * The file is the size of the whole catalogue, too big for a preference; the hash stays in
     * DataStore so a torn write is a missing file, never a stale one under a fresh hash.
     */
    suspend fun world(server: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val hash = context.settings.data.first()[worldHashKey(server)] ?: return@withContext null
        worldFile(server).takeIf { it.isFile }?.readText()?.let { hash to it }
    }

    suspend fun saveWorld(server: String, hash: String, document: String) {
        withContext(Dispatchers.IO) {
            context.settings.edit { it.remove(worldHashKey(server)) }
            worldFile(server).apply { parentFile?.mkdirs() }.writeText(document)
            context.settings.edit { it[worldHashKey(server)] = hash }
        }
    }

    private fun worldHashKey(server: String) = stringPreferencesKey("world:$server:hash")
    private fun worldFile(server: String) =
        java.io.File(context.filesDir, "world/" + java.util.UUID.nameUUIDFromBytes(server.toByteArray()) + ".json")

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

    /**
     * The session token for one server. It is a secret, but on this device it is the account's own:
     * losing it costs a sign-in, and DataStore lives in the app's private storage.
     */
    suspend fun token(server: String): String? = context.settings.data.first()[tokenKey(server)]
    suspend fun saveToken(server: String, value: String?) {
        context.settings.edit { if (value == null) it.remove(tokenKey(server)) else it[tokenKey(server)] = value }
    }
    private fun tokenKey(server: String) = stringPreferencesKey("token:$server")

    private fun hashKey(server: String, language: String) = stringPreferencesKey("locale:$server:$language:hash")
    private fun documentKey(server: String, language: String) = stringPreferencesKey("locale:$server:$language:body")
}
