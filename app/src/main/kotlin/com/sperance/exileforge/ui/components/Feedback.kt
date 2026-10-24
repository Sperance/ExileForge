package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.feedbackKindTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.rules.feedback.BugStatus
import com.sperance.exileforge.rules.feedback.IssueState
import com.sperance.exileforge.ui.theme.*

/** Who opens the inbox: the app hands it down to the banner's envelope. */
val LocalMailOpen = staticCompositionLocalOf<(() -> Unit)?> { null }

fun kindTitle(kind: FeedbackKind): String = feedbackKindTitle(kind.name)

/**
 * Судьба отчёта глазами игрока: модерация, обсуждение с модерацией, отказ модерации, закрыт автором, затем судьба его Issue. Цвет -
 * кромка карточки и плашка статуса; [closed] - закрытые идут ниже открытых и приглушены.
 */
enum class ReportFate(val tint: Color, val icon: ImageVector, val closed: Boolean = false) {
    MODERATION(Color(0xFF8FD3FF), Icons.Outlined.Schedule),
    DISCUSSION(Rune, Icons.Outlined.Forum),
    REJECTED(Color(0xFFC9746F), Icons.Outlined.Shield, closed = true),
    WITHDRAWN(Quiet, Icons.Outlined.Lock, closed = true),
    SENT(Color(0xFFF2C94C), Icons.AutoMirrored.Outlined.Send),
    OPEN(Color(0xFFF2C94C), Icons.AutoMirrored.Outlined.FormatListBulleted),
    FIXED(Color(0xFF7BE0A6), Icons.Outlined.CheckCircle, closed = true),
    NOT_PLANNED(Color(0xFF9AA7B0), Icons.Outlined.Close, closed = true),
    INVALID(Color(0xFF9AA7B0), Icons.Outlined.Block, closed = true),
    DUPLICATE(Color(0xFF9AA7B0), Icons.Outlined.ContentCopy, closed = true),
    ;

    companion object {
        /** Судьба отчёта статуса модерации [status] с Issue [issue]. */
        fun of(status: BugStatus, issue: IssueView?): ReportFate = when {
            status == BugStatus.REJECTED -> REJECTED

            status == BugStatus.WITHDRAWN -> WITHDRAWN

            issue == null -> when (status) {
                BugStatus.SENT -> SENT
                BugStatus.DISCUSSION -> DISCUSSION
                else -> MODERATION
            }

            else -> when (issue.state) {
                IssueState.OPEN -> OPEN
                IssueState.FIXED -> FIXED
                IssueState.NOT_PLANNED -> NOT_PLANNED
                IssueState.INVALID -> INVALID
                IssueState.DUPLICATE -> DUPLICATE
            }
        }
    }
}

/** Слова судьбы [fate] отчёта вида [kind]: «В работе», версия и номер дубля; [short] - для узкой плашки. */
fun fateTitle(fate: ReportFate, kind: FeedbackKind, issue: IssueView?, short: Boolean = false): String {
    val version = issue?.version.orEmpty()
    return when (fate) {
        ReportFate.MODERATION -> ui("feedback.fate.moderation")
        ReportFate.DISCUSSION -> ui("feedback.fate.discussion")
        ReportFate.REJECTED -> ui(if (short) "feedback.fate.rejected_short" else "feedback.fate.rejected")
        ReportFate.WITHDRAWN -> ui("feedback.fate.withdrawn")
        ReportFate.SENT -> ui("feedback.fate.sent")
        ReportFate.OPEN -> ui("feedback.fate.open")
        ReportFate.FIXED -> if (version.isBlank()) ui("feedback.fate.fixed") else ui("feedback.fate.fixed_in", version)
        ReportFate.NOT_PLANNED -> ui(if (short) "feedback.fate.wontfix_short" else "feedback.fate.wontfix")
        ReportFate.INVALID -> ui("feedback.fate.invalid")
        ReportFate.DUPLICATE -> issue?.duplicate?.takeIf { it > 0 }?.let { ui("feedback.fate.duplicate_of", it) } ?: ui("feedback.fate.duplicate")
    }
}

/** Плашка судьбы: значок и слова цветом судьбы; [fresh] - красная точка нового ответа. */
@Composable fun FatePill(fate: ReportFate, kind: FeedbackKind, issue: IssueView?, fresh: Boolean = false, short: Boolean = true) {
    Row(
        Modifier.background(fate.tint.copy(alpha = .14f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(fate.icon, null, tint = fate.tint, modifier = Modifier.size(12.dp))
        Text(fateTitle(fate, kind, issue, short), color = fate.tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        if (fresh) Box(Modifier.size(6.dp).background(LifeRed, CircleShape))
    }
}

/**
 * Что показать в окне своих отчётов: отчёт [focus] раскрыт, и окно прокручено к нему (null - без него); [root] - откуда окно открыто,
 * корень его цепочки «назад».
 */
data class ReportsView(val root: String, val focus: String? = null)

/** Вопрос перед удалением отчёта: удержание - [onDelete], отмена - [onDismiss]. */
@Composable fun ReportDeleteConfirm(busy: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    ConfirmSheet(ui("feedback.delete_q"), ui("feedback.delete"), onDismiss = onDismiss, note = ui("feedback.delete_note"), danger = true, busy = busy, onConfirm = onDelete)
}
