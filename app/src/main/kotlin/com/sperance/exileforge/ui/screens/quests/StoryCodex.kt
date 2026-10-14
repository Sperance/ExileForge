package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.LoreTerm
import com.sperance.exileforge.core.display.regionLore
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.StoryChapter
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * Кодекс сюжета (0.13.3, LORE-01): девять тронов - по одному абзацу на главу, уже открытую героем ([reached] - число открытых
 * глав, чтобы кодекс не забегал вперёд сюжета), и «слова мира» [LoreTerm]. Свёрнут, пока его не раскроют.
 */
@Composable internal fun StoryCodex(chapters: List<StoryChapter>, reached: Int) {
    var open by remember { mutableStateOf(false) }
    ForgePanel {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Engraved(ui("story.codex"))
            Text(ui(if (open) "common.collapse" else "common.expand"), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        if (!open) return@ForgePanel
        val thrones = chapters.take(reached).mapNotNull { chapter -> regionLore(chapter.region)?.let { loc(LocaleKey.regionName(chapter.region)) to it } }
        if (thrones.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            MutedText(ui("story.codex_thrones"))
            thrones.forEach { (region, lore) -> CodexEntry(region, lore) }
        }
        Spacer(Modifier.height(8.dp))
        MutedText(ui("story.codex_terms"))
        LoreTerm.entries.forEach { term -> CodexEntry(term.title, term.text) }
    }
}

@Composable private fun CodexEntry(title: String, text: String) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(title, color = GoldBright, style = MaterialTheme.typography.titleSmall)
        Text(text, color = Parchment, style = MaterialTheme.typography.bodySmall)
    }
}
