package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.settings by preferencesDataStore("server_settings")
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
    val server = context.settings.data.map { it[key] ?: "http://10.1.10.198:8080/" }
    suspend fun save(value: String) { context.settings.edit { it[key] = value } }
}
