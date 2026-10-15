package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.replies by preferencesDataStore("report_replies")

/**
 * Прочитанные ответы на свои отчёты (0.14.1, макет A): по отчёту - отпечаток последнего ответа разработчика или модерации, что
 * игрок уже видел. Новый ответ - красная точка у статуса и число на вкладке «Мои»; открытая вкладка гасит их. Только на устройстве.
 */
class RepliesSeen(private val context: Context, private val scope: CoroutineScope) {
    /** Отпечатки виденных ответов по id отчёта. */
    val seen: Flow<Map<String, String>> = context.replies.data.map { prefs -> prefs.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() } }

    /** Ответы [replies] (id отчёта → отпечаток) видены. */
    fun mark(replies: Map<String, String>) {
        if (replies.isEmpty()) return
        scope.launch { context.replies.edit { prefs -> replies.forEach { (id, print) -> prefs[stringPreferencesKey(id)] = print } } }
    }
}
