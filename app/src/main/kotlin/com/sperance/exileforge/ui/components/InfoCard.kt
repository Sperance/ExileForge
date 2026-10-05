package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.ui.icons.GlyphIcon
import com.sperance.exileforge.ui.theme.Gold

/** Parchment note pinned to the stash wall; turns blood-red when it reports a failure. */
@Composable internal fun InfoCard(
    title: String,
    body: String,
    failure: Boolean = false,
    glyph: Glyph = if (failure) Glyph.ALERT else Glyph.INFO,
) {
    val accent = if (failure) MaterialTheme.colorScheme.error else Gold
    ForgePanel(accent = accent) {
        // Заголовок - чип цвета акцента (3.88.3): обычный, ошибка, Бездна различаются им, а не рамкой.
        Row(
            Modifier.background(accent.copy(alpha = .14f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlyphIcon(glyph, accent, Modifier.size(14.dp))
            Text(title, color = accent, style = MaterialTheme.typography.labelLarge)
        }
        SelectionContainer { MutedText(body) }
    }
}
