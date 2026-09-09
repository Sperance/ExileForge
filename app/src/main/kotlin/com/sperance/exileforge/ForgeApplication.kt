package com.sperance.exileforge

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sperance.exileforge.core.RequestJournal
import kotlinx.coroutines.flow.map

private val Context.settings by preferencesDataStore("server_settings")
class ForgeApplication : Application() {
    val journal = RequestJournal()
    val serverStore by lazy { ServerStore(this) }
}
class ServerStore(private val context: Context) {
    private val key = stringPreferencesKey("base_url")
    val server = context.settings.data.map { it[key] ?: "http://10.1.10.198:8080/" }
    suspend fun save(value: String) { context.settings.edit { it[key] = value } }
}
