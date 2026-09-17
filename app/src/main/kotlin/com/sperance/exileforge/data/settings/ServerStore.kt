package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sperance.exileforge.core.i18n.Lang
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.settings by preferencesDataStore("server_settings")
// The icon set is large and rarely written: its own store keeps it out of every settings rewrite.
private val Context.iconCache by preferencesDataStore("icon_cache")
class ServerStore(private val context: Context) {
    private val key = stringPreferencesKey("base_url")
    private val pendingKey = stringPreferencesKey("pending_inventory_request")
    val pending = context.settings.data.map { it[pendingKey] }
    suspend fun savePending(value: String?) { context.settings.edit { if(value == null) it.remove(pendingKey) else it[pendingKey] = value } }
    suspend fun filters(server: String, catalog: String): String? = context.settings.data.first()[stringPreferencesKey("filters:$server:$catalog")]
    suspend fun saveFilters(server: String, catalog: String, value: String) { context.settings.edit { it[stringPreferencesKey("filters:$server:$catalog")] = value } }
    suspend fun combatPending(scope: String): String? = context.settings.data.first()[stringPreferencesKey("combat:$scope")]
    suspend fun saveCombatPending(scope: String, value: String?) {
        context.settings.edit { prefs ->
            val key = stringPreferencesKey("combat:$scope")
            if (value == null) prefs.remove(key) else prefs[key] = value
        }
    }
    suspend fun passivePending(scope: String): String? = context.settings.data.first()[stringPreferencesKey("passives:$scope")]
    suspend fun savePassivePending(scope: String, value: String?) {
        context.settings.edit { prefs ->
            val key = stringPreferencesKey("passives:$scope")
            if (value == null) prefs.remove(key) else prefs[key] = value
        }
    }
    /** The whole icon set of one server, kept against its iconSetVersion: pictures are immutable. */
    suspend fun icons(server: String): String? = context.iconCache.data.first()[stringPreferencesKey("icons:$server")]
    suspend fun saveIcons(server: String, value: String?) {
        context.iconCache.edit { prefs ->
            val key = stringPreferencesKey("icons:$server")
            if (value == null) prefs.remove(key) else prefs[key] = value
        }
    }
    val server = context.settings.data.map { it[key] ?: "http://10.1.10.198:8080/" }
    suspend fun save(value: String) { context.settings.edit { it[key] = value } }
    private val languageKey = stringPreferencesKey("language")
    val language = context.settings.data.map { Lang.of(it[languageKey]) }
    suspend fun saveLanguage(value: Lang) { context.settings.edit { it[languageKey] = value.code } }
}
