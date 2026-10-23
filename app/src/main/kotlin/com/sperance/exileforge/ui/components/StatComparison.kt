package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.character.MightShift
import com.sperance.exileforge.core.display.Comparison
import com.sperance.exileforge.core.display.ComparisonLine
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/**
 * Сравнение «было → стало»: одно на итог героя, фляги и ремесла в карточке вещи и на панель узла дерева. Свёрнуто всегда, как
 * его покажут, и заново с каждой новой парой: виден один заголовок-кнопка ([FoldHeader]) - имя, счёт «N лучше · M хуже» и
 * сдвиг Мощи [might], если он стоит показа. Раскрытое - все строки: имя, «было → **стало**» и плашка разницы (рост `Vital`,
 * потеря `LifeRed`, без сдвига - приглушённая), под ними - блок «Теряешь» ([LostBlock]): строки заменяемой вещи [lost], которых
 * нет на новой. Пустое говорит, что ничего не изменится, без кнопки. Цвета подписей - из [look] карточки вещи, вне карточки -
 * приглушённые темы.
 */
@Composable fun StatComparison(
    title: String,
    comparison: Comparison,
    modifier: Modifier = Modifier,
    look: RelicLook? = null,
    lost: List<String> = emptyList(),
    might: MightShift? = null,
) {
    val muted = look?.muted ?: Muted
    val faint = look?.faint ?: Bronze
    val shift = might?.takeIf { it.shown }
    Column(modifier.fillMaxWidth()) {
        if (comparison.lines.isEmpty()) {
            Text(title, color = muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(ui("wear.nothing"), color = muted, style = MaterialTheme.typography.bodySmall)
            shift?.let { MightShiftLine(it, muted, Modifier.padding(top = 5.dp)) }
            LostBlock(lost, muted, Modifier.padding(top = 10.dp))
            return@Column
        }
        var open by remember(comparison.lines) { mutableStateOf(false) }
        FoldHeader(title, comparison, shift, open, muted) { open = !open }
        if (open) {
            Column(Modifier.fillMaxWidth().padding(start = 2.dp, top = 4.dp, end = 2.dp)) {
                comparison.lines.forEachIndexed { i, line ->
                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = faint.copy(alpha = .35f))
                    ComparisonRow(line, muted)
                }
                LostBlock(lost, muted, Modifier.padding(top = 10.dp))
            }
        }
    }
}

/**
 * Заголовок-кнопка сравнения в тонкой рамке (раскрытое - рамка акцента): имя, счёт строк без нулевых частей («хуже» красным
 * текстом) и шеврон, ниже - сдвиг Мощи [might]; диктор слышит всё одной фразой ([Comparison.speech]).
 */
@Composable private fun FoldHeader(title: String, comparison: Comparison, might: MightShift?, open: Boolean, muted: Color, onToggle: () -> Unit) {
    val speech = comparison.speech(title, might, open)
    FoldFrame(if (open) Gold else Seam, Modifier.clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = speech }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, modifier = Modifier.weight(1f))
            Text(tally(comparison, muted), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp, end = 6.dp))
            Text(if (open) "▴" else "▾", color = Parchment, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
        }
        might?.let { MightShiftLine(it, muted) }
    }
}

/**
 * Сравнение, которое ещё считается: заголовок [title] в той же рамке, что свёрнутое [StatComparison], без счёта, Мощи и кнопки -
 * числа встанут, когда итог сложится, а карточка не прыгнет.
 */
@Composable fun StatComparisonPending(title: String, modifier: Modifier = Modifier, look: RelicLook? = null) {
    val muted = look?.muted ?: Muted
    FoldFrame(Seam, modifier) {
        Text(title, color = muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp)
    }
}

/** Рамка заголовка сравнения: тонкая обводка [border], высота кнопки, отступы заголовка; [behaviour] - нажатие и озвучка. */
@Composable private fun FoldFrame(border: Color, behaviour: Modifier, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(FoldShape)
            .border(1.dp, border, FoldShape)
            .then(behaviour)
            .padding(start = 12.dp, top = 7.dp, end = 10.dp, bottom = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) { content() }
}

/** Счёт строк «N лучше · M хуже»: лучше - `Vital`, хуже - [LossText], нулевые части не пишутся. */
private fun tally(comparison: Comparison, muted: Color) = buildAnnotatedString {
    if (comparison.better > 0) withStyle(SpanStyle(color = Vital)) { append(ui("compare.better", comparison.better)) }
    if (comparison.better > 0 && comparison.worse > 0) withStyle(SpanStyle(color = muted)) { append(" · ") }
    if (comparison.worse > 0) withStyle(SpanStyle(color = LossText)) { append(ui("compare.worse", comparison.worse)) }
}

/** Блок «Теряешь»: строки старой вещи, что пропадут с заменой, - красным текстом; пусто - блока нет. */
@Composable fun LostBlock(lines: List<String>, muted: Color, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(ui("compare.lost"), color = muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp)
        lines.forEach { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Строка сравнения: имя, «было → стало» и плашка разницы цвета сдвига. */
@Composable private fun ComparisonRow(line: ComparisonLine, muted: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(line.title, color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        ComparisonFigures(line.before, line.after, muted)
        DiffPill(line.diff, line.better, muted)
    }
}

/**
 * Сравнение коротко, без заголовка и раскрытия: первые [limit] важнейших строк - имя и «было → **стало**» цвета сдвига, под ними
 * «и ещё N». Для строки выбора, где сравнений много сразу (лист самоцветов гнезда); пустое - ничего.
 */
@Composable fun ComparisonDigest(comparison: Comparison, modifier: Modifier = Modifier, limit: Int = DIGEST_LINES, muted: Color = Muted) {
    if (comparison.lines.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        comparison.lines.take(limit).forEach { line ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(line.title, color = muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    buildAnnotatedString {
                        append(line.before)
                        append(" → ")
                        withStyle(SpanStyle(color = shiftInk(line.better), fontWeight = FontWeight.Bold)) { append(line.after) }
                    },
                    color = muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        val rest = comparison.lines.size - limit
        if (rest > 0) Text(ui("compare.more", rest), color = muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Цвет «стало» по сдвигу: рост `Vital`, потеря [LossText], без сдвига - [Parchment]. */
private fun shiftInk(better: Boolean?): Color = when (better) {
    true -> Vital
    false -> LossText
    null -> Parchment
}

/** Сколько строк [ComparisonDigest] показывает по умолчанию. */
private const val DIGEST_LINES = 3

/** «было → **стало**» строки сравнения (общее со строкой Мощи вещи). */
@Composable internal fun ComparisonFigures(before: String, after: String, muted: Color) {
    Text(
        buildAnnotatedString {
            append(before)
            append(" → ")
            withStyle(SpanStyle(color = Parchment, fontWeight = FontWeight.Bold)) { append(after) }
        },
        color = muted,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        softWrap = false,
    )
}

/** Плашка разницы цвета сдвига [better]: рост `Vital`, потеря - текст [LossText] на заливке `LifeRed`, без сдвига - [muted]. */
@Composable internal fun DiffPill(diff: String, better: Boolean?, muted: Color) {
    val (ink, fill) = when (better) {
        true -> Vital to Vital
        false -> LossText to LifeRed
        null -> muted to muted
    }
    Text(
        diff,
        color = ink,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.5.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.widthIn(min = 54.dp).background(fill.copy(alpha = .15f), PillShape).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

private val PillShape = RoundedCornerShape(10.dp)
private val FoldShape = RoundedCornerShape(10.dp)
