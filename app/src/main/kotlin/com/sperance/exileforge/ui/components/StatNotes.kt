package com.sperance.exileforge.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.StatDetail
import com.sperance.exileforge.core.sheet.StatNote
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.theme.*

/** Где строка с пояснением ловит касание: вся строка или только её знак ⓘ (строка внутри того, что касание уже выбирает). */
enum class NoteTap { LINE, MARK }

/**
 * Строка характеристик, что объясняет себя: касание открывает окно у строки с пояснением каждой характеристики [stats] ([StatNotes]:
 * имя, фраза и подробное описание - тот же текст, что в разборе сводки героя); справа - тихий знак ⓘ. Без описаний строка не
 * нажимается и знака нет. [tap] - что ловит касание: строка или, внутри выбирающего касанием (вариант узла), только знак с зоной
 * касания 44 dp. Пояснения собираются при открытии окна; [tint] - рамка окна.
 */
@Composable fun StatNoted(
    stats: List<String>,
    index: ContentIndex?,
    modifier: Modifier = Modifier,
    tint: Color = Rune,
    tap: NoteTap = NoteTap.LINE,
    content: @Composable RowScope.() -> Unit,
) {
    val described = remember(stats) { stats.any { StatDetail.described(it) } }
    var open by remember { mutableStateOf(false) }
    val opens = Modifier.clickable(role = Role.Button, onClickLabel = ui("stat.note.open")) { open = true }
    Row(modifier.then(if (described && tap == NoteTap.LINE) opens else Modifier), verticalAlignment = Alignment.CenterVertically) {
        content()
        if (described) {
            val mark = Modifier.padding(start = 6.dp)
            Icon(
                Icons.Outlined.Info,
                null,
                tint = Muted.copy(alpha = .6f),
                modifier = (if (tap == NoteTap.MARK) mark.then(opens).touchZone() else mark).size(NoteMarkSide),
            )
        }
        if (open) TipCallout(tint, onClose = { open = false }) { StatNotes(remember(stats, index) { StatDetail.notes(stats, index) }) }
    }
}

/**
 * Пояснения характеристик столбиком: имя, фраза серифом в кавычках и абзацы описания с точками - как в разборе сводки героя. Без
 * [titled] имя не печатается - его уже назвал заголовок места.
 */
@Composable fun ColumnScope.StatNotes(notes: List<StatNote>, titled: Boolean = true) {
    notes.forEach { note ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (titled) Text(note.title, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            note.quote?.let { Text("«$it»", color = Muted, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic) }
            note.paragraphs.forEach { paragraph ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text(paragraph, color = Parchment, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Сторона тихого знака ⓘ строки с пояснением. */
private val NoteMarkSide = 13.dp
