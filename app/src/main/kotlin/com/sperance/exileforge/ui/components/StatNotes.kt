package com.sperance.exileforge.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.sheet.StatDetail
import com.sperance.exileforge.core.sheet.StatNote
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.theme.*

/**
 * Строка характеристик, что объясняет себя: касание открывает окно у строки с пояснением каждой характеристики [stats] ([StatNotes]:
 * имя, фраза и подробное описание - тот же текст, что в разборе сводки героя); справа - тихий знак ⓘ. Без описаний строка не
 * нажимается и знака нет. Пояснения собираются при открытии окна; [tint] - рамка окна.
 */
@Composable fun StatNoted(
    stats: List<String>,
    index: ContentIndex?,
    modifier: Modifier = Modifier,
    tint: Color = Rune,
    content: @Composable RowScope.() -> Unit,
) {
    val lang = uiLanguage
    val described = remember(stats, lang) { stats.any { StatDetail.described(it, lang) } }
    var open by remember { mutableStateOf(false) }
    val opens = if (described) Modifier.clickable(role = Role.Button, onClickLabel = ui("stat.note.open")) { open = true } else Modifier
    Row(modifier.then(opens), verticalAlignment = Alignment.CenterVertically) {
        content()
        if (described) NoteMark(Modifier.padding(start = 6.dp))
        if (open) NotesCallout(stats, index, tint) { open = false }
    }
}

/**
 * Знак ⓘ в углу того, что касание уже выбирает (вариант узла древа): строки внутри не нажимаются, касание знака открывает окно с
 * пояснениями всех характеристик [stats] ([StatNotes]). Зона касания 44 dp лежит в рамке родителя и не раздувает его разметку; без
 * описаний знака нет.
 */
@Composable fun BoxScope.StatNoteCorner(stats: List<String>, index: ContentIndex?, tint: Color = Rune) {
    val lang = uiLanguage
    val described = remember(stats, lang) { stats.any { StatDetail.described(it, lang) } }
    if (!described) return
    var open by remember { mutableStateOf(false) }
    Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopEnd) {
        NoteMark(Modifier.clickable(role = Role.Button, onClickLabel = ui("stat.note.open")) { open = true }.touchZone())
        if (open) NotesCallout(stats, index, tint) { open = false }
    }
}

/** Тихий знак ⓘ строки с пояснением. */
@Composable private fun NoteMark(modifier: Modifier) {
    Icon(Icons.Outlined.Info, null, tint = Muted.copy(alpha = .6f), modifier = modifier.size(NoteMarkSide))
}

/** Окно у строки с пояснениями характеристик [stats] на языке интерфейса, числа - контента [index]. */
@Composable private fun NotesCallout(stats: List<String>, index: ContentIndex?, tint: Color, onClose: () -> Unit) {
    val lang = uiLanguage
    val notes = remember(stats, index, lang) { StatDetail.notes(stats, index, lang) }
    TipCallout(tint, onClose = onClose) { StatNotes(notes) }
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
