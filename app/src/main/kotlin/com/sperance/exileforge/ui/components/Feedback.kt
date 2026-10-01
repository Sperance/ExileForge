package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.model.feedback.Suggestion
import com.sperance.exileforge.core.model.feedback.Vote
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.theme.*

/** Who opens the inbox (3.73.0): the app hands it down to the banner's envelope. */
val LocalMailOpen = staticCompositionLocalOf<(() -> Unit)?> { null }

/** A report's status by name, in the server's words (3.73.0). */
fun statusTitle(status: ReportStatus): String = locOr("enum.BugStatus.${status.name}", status.name)
fun kindTitle(kind: FeedbackKind): String = locOr("enum.FeedbackKind.${kind.name}", kind.name)

/** Each status its colour: new muted, in progress gold, implemented green, closed red. */
fun statusTint(status: ReportStatus): Color = when (status) {
    ReportStatus.NEW -> Parchment
    ReportStatus.IN_PROGRESS -> GoldBright
    ReportStatus.DONE -> Vital
    ReportStatus.WONTFIX -> LifeRed
}

@Composable fun StatusBadge(status: ReportStatus) {
    Text(statusTitle(status), color = statusTint(status), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(statusTint(status).copy(alpha = .14f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
}

/**
 * Players' suggestions (3.73.0): everyone's open ones by rating — likes less dislikes — each with its counts and the viewer's
 * vote, the author never shown; and the viewer's own bugs and suggestions with where they stand and the administrator's word.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SuggestionsSheet(s: ForgeState, vm: ForgeViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadSuggestions() }
    var tab by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feedback.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            PillTabs(listOf(ui("feedback.all"), ui("feedback.mine")), tab, { tab = it })
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (tab == 0) {
                    if (s.feedback.suggestions.isEmpty()) item { MutedText(ui("feedback.none")) }
                    items(s.feedback.suggestions, key = { it.id }) { SuggestionCard(it, enabled = !s.busy, onVote = { v -> vm.vote(it.id, v) }) }
                } else {
                    if (s.feedback.mine.isEmpty()) item { MutedText(ui("feedback.none_mine")) }
                    items(s.feedback.mine, key = { it.id }) { own ->
                        ForgePanel {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(kindTitle(own.kind), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                StatusBadge(own.status)
                            }
                            Text(own.text, color = Parchment, style = MaterialTheme.typography.bodyMedium)
                            if (own.kind == FeedbackKind.SUGGESTION) MutedText(ui("feedback.votes", own.likes, own.dislikes))
                            if (own.reason.isNotBlank()) Text(ui("feedback.reason", own.reason), color = Rune, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
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
    Row(Modifier.background(if (chosen) tint.copy(alpha = .18f) else Color.Transparent, RoundedCornerShape(8.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = if (chosen) tint else Muted, modifier = Modifier.size(18.dp))
        Text(count.toString(), color = if (chosen) tint else Parchment, style = MaterialTheme.typography.labelLarge)
    }
}

/** The envelope of the banner (3.73.0): the unread count on it, the inbox under a tap. */
@Composable fun MailButton(unread: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box {
            Icon(Icons.Outlined.Mail, ui("mail.title"), tint = Gold, modifier = Modifier.size(22.dp))
            if (unread > 0) Text(if (unread > 9) "9+" else unread.toString(), color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp).background(LifeRed, CircleShape).padding(horizontal = 4.dp))
        }
    }
}

/** A letter's subject and body in the player's language: a system letter is the dictionary's, the administrator's as written. */
fun mailSubject(mail: Mail): String = if (mail.kind == MailKind.SYSTEM) systemLine(mail, "subject") else mail.subject
fun mailBody(mail: Mail): String = if (mail.kind == MailKind.SYSTEM) listOf(systemLine(mail, "body"),
    mail.args.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { loc("${mail.key}.reason", listOf(it)) }).filterNotNull().joinToString("\n\n") else mail.body

/** `mail.feedback_status`: its args are the kind, the status, an excerpt of the report and the administrator's word. */
private fun systemLine(mail: Mail, part: String): String {
    val kind = mail.args.getOrNull(0)?.let { name -> FeedbackKind.entries.firstOrNull { it.name == name } }?.let(::kindTitle).orEmpty()
    val status = mail.args.getOrNull(1)?.let { name -> ReportStatus.entries.firstOrNull { it.name == name } }?.let(::statusTitle).orEmpty()
    return when (part) {
        "subject" -> loc("${mail.key}.subject", listOf(kind))
        else -> loc("${mail.key}.body", listOf(status, mail.args.getOrNull(2).orEmpty()))
    }
}

/** What a letter carries, a line each: gold, stacks, things. */
fun attachmentLines(mail: Mail): List<String> = buildList {
    if (mail.attachment.gold > 0) add(ui("mail.gold", mail.attachment.gold))
    mail.attachment.items.forEach { (code, amount) -> add("${itemTitle(code)} × $amount") }
    mail.attachment.equipment.forEach { add(equipmentTitle(it.template) + (it.rarity?.let { r -> " · " + ui("enum.rarity.${r.name}") } ?: "")) }
}

/**
 * The inbox (3.73.0): letters newest first, the unread marked; a letter opens to its whole text and what it carries, taken by
 * the hero in play at a tap, once. Letters keep for thirty days.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MailSheet(s: ForgeState, vm: ForgeViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadMail() }
    var open by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("mail.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            MutedText(ui("mail.keep"))
            val letter = s.feedback.mail.firstOrNull { it.id == open }
            if (letter != null) LetterView(s, vm, letter) { open = null }
            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (s.feedback.mail.isEmpty()) item { MutedText(ui("mail.empty")) }
                items(s.feedback.mail, key = { it.id }) { mail ->
                    ForgePanel(Modifier.clickable { open = mail.id; if (!mail.read) vm.readMail(mail.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!mail.read) Box(Modifier.size(8.dp).background(LifeRed, CircleShape))
                            Text(mailSubject(mail), color = if (mail.read) Parchment else GoldBright, style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (mail.read) FontWeight.Normal else FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (mail.claimable) Text(ui("mail.has_gift"), color = Vital, style = MaterialTheme.typography.labelSmall)
                        }
                        MutedText(ui(if (mail.kind == MailKind.SYSTEM) "mail.from_game" else "mail.from_admin") + " · " + mail.createdAt.replace('T', ' ').take(16))
                    }
                }
            }
        }
    }
}

@Composable private fun LetterView(s: ForgeState, vm: ForgeViewModel, mail: Mail, onBack: () -> Unit) {
    ForgeTextButton(onClick = onBack) { Text(ui("mail.back")) }
    ForgePanel {
        Text(mailSubject(mail), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(mailBody(mail), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        val lines = attachmentLines(mail)
        if (lines.isNotEmpty()) {
            Engraved(ui("mail.attachment"))
            lines.forEach { Text(it, color = Vital, style = MaterialTheme.typography.labelLarge) }
            if (mail.claimable) ForgeButton(enabled = !s.busy && s.play.heroId.isNotBlank(), onClick = { vm.claimMail(mail.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("mail.claim", s.heroName))
            } else MutedText(ui("mail.claimed_already"))
        }
        ForgeOutlinedButton(enabled = !s.busy, onClick = { vm.deleteMail(mail.id); onBack() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Delete, null, tint = LifeRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(ui("mail.delete"), color = LifeRed)
        }
    }
}
