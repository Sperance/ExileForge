package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.Comparison
import com.sperance.exileforge.core.display.ComparisonLine
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/**
 * Сравнение «было → стало» (макет «Изменения характеристик», вариант B): одно на итог героя и фляги в карточке вещи и на
 * панель узла дерева. Строка - имя, «было → **стало**» и плашка разницы (рост `Vital`, потеря `LifeRed`, без сдвига -
 * приглушённая). Больше [Comparison.LEADING] строк - свёрнуто до важнейших ([Comparison.leading]), под ними контурная
 * кнопка во всю ширину «Показать ещё N · X лучше · Y хуже» / «Свернуть». Пустое говорит, что ничего не изменится.
 * Цвета подписей - из [look] карточки вещи, вне карточки - приглушённые темы.
 */
@Composable fun StatComparison(title: String, comparison: Comparison, modifier: Modifier = Modifier, look: RelicLook? = null) {
    val muted = look?.muted ?: Muted
    val faint = look?.faint ?: Bronze
    var open by rememberSaveable(comparison.lines.size) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(title, color = muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, modifier = Modifier.padding(bottom = 4.dp))
        if (comparison.lines.isEmpty()) {
            Text(ui("wear.nothing"), color = muted, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        val shown = if (open || !comparison.foldable) comparison.lines else comparison.leading
        shown.forEachIndexed { i, line ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = faint.copy(alpha = .35f))
            ComparisonRow(line, muted, faint)
        }
        if (comparison.foldable) {
            ComparisonToggle(open, comparison.hidden, faint) { open = !open }
        }
    }
}

/** Строка сравнения: имя, «было → стало» и плашка разницы цвета сдвига. */
@Composable private fun ComparisonRow(line: ComparisonLine, muted: Color, faint: Color) {
    val body = MaterialTheme.typography.bodySmall
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(line.title, color = Parchment, style = body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            buildAnnotatedString {
                append(line.before)
                withStyle(SpanStyle(color = faint)) { append(" → ") }
                withStyle(SpanStyle(color = Parchment, fontWeight = FontWeight.Bold)) { append(line.after) }
            },
            color = muted,
            style = body,
            maxLines = 1,
            softWrap = false,
        )
        val tint = when (line.better) {
            true -> Vital
            false -> LifeRed
            null -> muted
        }
        Text(
            line.diff,
            color = tint,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.widthIn(min = 54.dp).background(tint.copy(alpha = .15f), PillShape).padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/**
 * Кнопка сворачивания во всю ширину, в тонкой нити: свёрнутое - «Показать ещё N · X лучше · Y хуже» по скрытым строкам
 * [hidden] (нулевые части не пишутся), раскрытое - «Свернуть».
 */
@Composable private fun ComparisonToggle(open: Boolean, hidden: List<ComparisonLine>, faint: Color, onToggle: () -> Unit) {
    val label = if (open) {
        buildAnnotatedString { append(ui("common.collapse")) }
    } else {
        buildAnnotatedString {
            append(ui("compare.more", hidden.size))
            val better = hidden.count { it.better == true }
            val worse = hidden.count { it.better == false }
            if (better > 0) {
                append(" · ")
                withStyle(SpanStyle(color = Vital)) { append(ui("compare.better", better)) }
            }
            if (worse > 0) {
                append(" · ")
                withStyle(SpanStyle(color = LifeRed)) { append(ui("compare.worse", worse)) }
            }
        }
    }
    Text(
        label,
        color = Gold,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().border(1.dp, faint, ToggleShape).clip(ToggleShape).clickable(onClick = onToggle).padding(vertical = 6.dp),
    )
}

private val PillShape = RoundedCornerShape(10.dp)
private val ToggleShape = RoundedCornerShape(10.dp)
