package com.sperance.exileforge.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.drafts by preferencesDataStore("feedback_drafts")

/** What the report sheet held when it closed (3.75.0). */
data class FeedbackDraft(val kind: FeedbackKind = FeedbackKind.BUG, val texts: Map<FeedbackKind, String> = emptyMap())

/**
 * The words of an unsent report or suggestion (3.75.0), one per kind, and the kind last open: they outlive the sheet and
 * the app, and go only once the server has taken them.
 */
class DraftStore(private val context: Context, private val scope: CoroutineScope) {
    private val kindKey = stringPreferencesKey("kind")
    private fun textKey(kind: FeedbackKind) = stringPreferencesKey("text:${kind.name}")

    suspend fun read(): FeedbackDraft {
        val prefs = context.drafts.data.first()
        return FeedbackDraft(
            FeedbackKind.entries.firstOrNull { it.name == prefs[kindKey] } ?: FeedbackKind.BUG,
            FeedbackKind.entries.associateWith { prefs[textKey(it)].orEmpty() },
        )
    }

    /** Kept on the app's scope, not the sheet's: the last keystroke before the sheet closes is written too. */
    fun save(kind: FeedbackKind, text: String) {
        scope.launch {
            context.drafts.edit {
                it[kindKey] = kind.name
                if (text.isEmpty()) {
                    it.remove(textKey(kind))
                } else {
                    it[textKey(kind)] = text
                }
            }
        }
    }

    suspend fun clear(kind: FeedbackKind) {
        context.drafts.edit { it.remove(textKey(kind)) }
    }
}
