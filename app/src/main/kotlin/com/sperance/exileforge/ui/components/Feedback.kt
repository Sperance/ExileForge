package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.model.feedback.Suggestion
import com.sperance.exileforge.core.model.feedback.Vote
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Who opens the inbox (3.73.0): the app hands it down to the banner's envelope. */
val LocalMailOpen = staticCompositionLocalOf<(() -> Unit)?> { null }

/** A report's status by name, in the server's words (3.73.0). */
fun statusTitle(status: ReportStatus): String = statusTitle(status.name)
fun kindTitle(kind: FeedbackKind): String = kindTitle(kind.name)

/** Статус и вид по имени из письма: незнакомое клиенту имя показывается словарём сервера или самим именем, не пустотой. */
fun statusTitle(name: String): String = locOr("enum.BugStatus.$name", name)
fun kindTitle(name: String): String = locOr("enum.FeedbackKind.$name", name)

/** Цвет статуса: созданный - пергамент, на рассмотрении - руна, отправленный - зелёный. */
fun statusTint(status: ReportStatus): Color = when (status) {
    ReportStatus.CREATED -> Parchment
    ReportStatus.REVIEW -> Rune
    ReportStatus.SENT -> Vital
}

@Composable fun StatusBadge(status: ReportStatus) {
    Text(
        statusTitle(status),
        color = statusTint(status),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(statusTint(status).copy(alpha = .14f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * Players' suggestions (3.73.0): everyone's open ones by rating — likes less dislikes — each with its counts and the viewer's
 * vote, the author never shown; and the viewer's own bugs and suggestions with where they stand and the administrator's word.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsSheet(onDismiss: () -> Unit) {
    val model = koinViewModel<FeedbackViewModel>()
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.loadSuggestions() }
    var tab by remember { mutableIntStateOf(0) }
    var deleting by remember { mutableStateOf<String?>(null) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feedback.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            PillTabs(listOf(ui("feedback.all"), ui("feedback.mine")), tab, { tab = it })
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (tab == 0) {
                    if (feedback.suggestions.isEmpty()) item { MutedText(ui("feedback.none")) }
                    items(feedback.suggestions, key = { it.id }) { SuggestionCard(it, enabled = !activity.busy, onVote = { v -> model.vote(it.id, v) }) }
                } else {
                    if (feedback.mine.isEmpty()) item { MutedText(ui("feedback.none_mine")) }
                    items(feedback.mine, key = { it.id }) { own ->
                        ForgePanel {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(kindTitle(own.kind), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                StatusBadge(own.status)
                            }
                            Text(own.text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
                            if (own.kind == FeedbackKind.SUGGESTION) MutedText(ui("feedback.votes", own.likes, own.dislikes))
                            if (own.reason.isNotBlank()) Text(ui("feedback.reason", own.reason), color = Rune, style = MaterialTheme.typography.labelMedium)
                            if (own.deletable) ReportDeleteButton(!activity.busy) { deleting = own.id }
                        }
                    }
                }
            }
            deleting?.let { id -> ReportDeleteConfirm(activity.busy, onDismiss = { deleting = null }) { model.deleteReport(id) } }
        }
    }
}

/** Удаление отчёта (4.3.0) - всегда с вопросом: оно жёсткое и не возвращается. */
@Composable fun ReportDeleteButton(enabled: Boolean, onClick: () -> Unit) {
    ForgeTextButton(enabled = enabled, onClick = onClick) {
        Icon(Icons.Outlined.Delete, null, tint = LifeRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(ui("feedback.delete"), color = LifeRed, style = MaterialTheme.typography.labelMedium)
    }
}

/** Вопрос перед удалением отчёта: удержание - [onDelete], отмена - [onDismiss]. */
@Composable fun ReportDeleteConfirm(busy: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    ConfirmSheet(ui("feedback.delete_q"), ui("feedback.delete"), onDismiss = onDismiss, note = ui("feedback.delete_note"), danger = true, blocked = busy, onConfirm = onDelete)
}

@Composable private fun SuggestionCard(suggestion: Suggestion, enabled: Boolean, onVote: (Vote) -> Unit) {
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (suggestion.mine) ui("feedback.yours") else "", color = Gold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            StatusBadge(suggestion.status)
        }
        Text(suggestion.text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            VoteButton(Icons.Outlined.ThumbUp, suggestion.likes, suggestion.vote == Vote.LIKE, Vital, enabled && suggestion.votable) { onVote(Vote.LIKE) }
            VoteButton(Icons.Outlined.ThumbDown, suggestion.dislikes, suggestion.vote == Vote.DISLIKE, LifeRed, enabled && suggestion.votable) { onVote(Vote.DISLIKE) }
            Spacer(Modifier.weight(1f))
            Text(ui("feedback.rating", suggestion.rating), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun VoteButton(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, chosen: Boolean, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.background(if (chosen) tint.copy(alpha = .18f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = if (chosen) tint else Muted, modifier = Modifier.size(18.dp))
        Text(count.toString(), color = if (chosen) tint else Parchment, style = MaterialTheme.typography.labelLarge)
    }
}

