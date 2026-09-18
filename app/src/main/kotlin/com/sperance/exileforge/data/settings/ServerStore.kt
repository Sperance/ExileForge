package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sperance.exileforge.core.i18n.Lang
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settings by preferencesDataStore("server_settings")

class ServerStore(private val context: Context) {
    private val key = stringPreferencesKey("base_url")
    val server = context.settings.data.map { it[key] ?: "http://10.0.2.2:8080/" }
    suspend fun save(value: String) { context.settings.edit { it[key] = value } }

    suspend fun filters(server: String, catalog: String): String? = context.settings.data.first()[stringPreferencesKey("filters:$server:$catalog")]
    suspend fun saveFilters(server: String, catalog: String, value: String) { context.settings.edit { it[stringPreferencesKey("filters:$server:$catalog")] = value } }

    private val languageKey = stringPreferencesKey("language")
    val language = context.settings.data.map { Lang.of(it[languageKey]) }
    suspend fun saveLanguage(value: Lang) { context.settings.edit { it[languageKey] = value.code } }
}
