package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.guides by preferencesDataStore("guides")

/** Which first-visit guides (3.14.0) this device has read: once per device, not per account or hero. */
class GuideStore(private val context: Context) {
    private val key = stringSetPreferencesKey("read")

    val read: Flow<Set<String>> = context.guides.data.map { it[key].orEmpty() }

    suspend fun markRead(guide: String) {
        context.guides.edit { it[key] = it[key].orEmpty() + guide }
    }

    suspend fun reset() {
        context.guides.edit { it.remove(key) }
    }
}
