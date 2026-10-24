package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.theme.Gold

/**
 * Parchment note pinned to the stash wall; turns blood-red when it reports a failure.
 *
 * Заголовок - чип цвета акцента: обычный, ошибка, Бездна различаются им, а не рамкой.
 */
@Composable internal fun InfoCard(
    title: String,
    body: String,
    failure: Boolean = false,
    glyph: Glyph = if (failure) Glyph.ALERT else Glyph.INFO,
) {
    val accent = if (failure) MaterialTheme.colorScheme.error else Gold
    ForgePanel(accent = accent) {
        ToneTag(title, TagLook.Filled.tone(accent), size = TagSize.Medium, style = MaterialTheme.typography.labelLarge, leading = { GlyphIcon(glyph, accent, Modifier.size(14.dp)) })
        SelectionContainer { MutedText(body) }
    }
}
