package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.update.ReleaseNotes
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Карточка релиза «Что нового» (4.2.0): все релизы одной плитой басальта в бронзовой нити, без выделения по весу версии и
 * без анимаций. Касание раскрывает заметки.
 */
@Composable fun ReleaseCard(release: ReleaseNotes, open: Boolean, onToggle: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Panel, SHAPE)
            .border(1.dp, Bronze, SHAPE)
            .clickable(onClick = onToggle)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Text(ui("app.version", release.version), style = MaterialTheme.typography.titleSmall, color = Parchment, fontWeight = FontWeight.SemiBold)
        release.published?.take(10)?.let { MutedText(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp)) }
        if (open) {
            release.body.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("## ") }.forEach { line ->
                Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.padding(top = 5.dp).size(9.dp)) { drawCircle(Muted, radius = 2.dp.toPx()) }
                    Text(line.removePrefix("- ").removePrefix("* "), color = Parchment, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private val SHAPE = RoundedCornerShape(12.dp)
