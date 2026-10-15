package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.IssueState
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.core.model.feedback.OwnReport
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

/** Цвет статуса модерации: созданный - пергамент, на рассмотрении - руна, отправленный - зелёный, отклонённый - кровь. */
fun statusTint(status: ReportStatus): Color = when (status) {
    ReportStatus.CREATED -> Parchment
    ReportStatus.REVIEW -> Rune
    ReportStatus.SENT -> Vital
    ReportStatus.REJECTED -> LifeRed
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
 * Судьба отчёта глазами игрока (0.14.1, GH-ISSUES, макет A): модерация, отказ модерации, затем судьба его Issue. Цвет - кромка
 * карточки и плашка статуса; [closed] - закрытые идут ниже открытых и приглушены.
 */
enum class ReportFate(val tint: Color, val icon: ImageVector, val closed: Boolean = false) {
    MODERATION(Color(0xFF8FD3FF), Icons.Outlined.Schedule),
    REJECTED(Color(0xFFC9746F), Icons.Outlined.Shield, closed = true),
    SENT(Color(0xFFF2C94C), Icons.AutoMirrored.Outlined.Send),
    OPEN(Color(0xFFF2C94C), Icons.AutoMirrored.Outlined.FormatListBulleted),
    IN_PROGRESS(Color(0xFFD9853B), Icons.Outlined.Build),
    FIXED(Color(0xFF7BE0A6), Icons.Outlined.CheckCircle, closed = true),
    DONE(Color(0xFF7BE0A6), Icons.Outlined.CheckCircle, closed = true),
    NOT_PLANNED(Color(0xFF9AA7B0), Icons.Outlined.Close, closed = true),
    INVALID(Color(0xFF9AA7B0), Icons.Outlined.Block, closed = true),
    DUPLICATE(Color(0xFF9AA7B0), Icons.Outlined.ContentCopy, closed = true),
    ;

    companion object {
        /** Судьба отчёта статуса модерации [status] с Issue [issue]. */
        fun of(status: ReportStatus, issue: IssueView?): ReportFate = when {
            status == ReportStatus.REJECTED -> REJECTED

            issue == null -> if (status == ReportStatus.SENT) SENT else MODERATION

            else -> when (issue.state) {
                IssueState.OPEN -> OPEN
                IssueState.IN_PROGRESS -> IN_PROGRESS
                IssueState.FIXED -> FIXED
                IssueState.DONE -> DONE
                IssueState.NOT_PLANNED -> NOT_PLANNED
                IssueState.INVALID -> INVALID
                IssueState.DUPLICATE -> DUPLICATE
            }
        }
    }
}

/** Слова судьбы [fate] отчёта вида [kind]: «Исправляется» у ошибки и «Делается» у предложения, версия и номер дубля; [short] - для узкой плашки. */
fun fateTitle(fate: ReportFate, kind: FeedbackKind, issue: IssueView?, short: Boolean = false): String {
    val version = issue?.version.orEmpty()
    return when (fate) {
        ReportFate.MODERATION -> ui("feedback.fate.moderation")
        ReportFate.REJECTED -> ui(if (short) "feedback.fate.rejected_short" else "feedback.fate.rejected")
        ReportFate.SENT -> ui("feedback.fate.sent")
        ReportFate.OPEN -> ui("feedback.fate.open")
        ReportFate.IN_PROGRESS -> ui(if (kind == FeedbackKind.SUGGESTION) "feedback.fate.doing" else "feedback.fate.fixing")
        ReportFate.FIXED -> if (version.isBlank()) ui("feedback.fate.fixed") else ui("feedback.fate.fixed_in", version)
        ReportFate.DONE -> if (version.isBlank()) ui("feedback.fate.done") else ui("feedback.fate.done_in", version)
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

/** Вкладка листа отчётов (0.14.1, макет A): свои отчёты или известные проблемы. */
enum class ReportsTab { MINE, KNOWN }

/** Фильтр известных проблем по виду. */
private enum class KindFilter(val key: String, val kind: FeedbackKind?) {
    ALL("feedback.filter.all", null),
    BUGS("feedback.filter.bugs", FeedbackKind.BUG),
    IDEAS("feedback.filter.ideas", FeedbackKind.SUGGESTION),
}

/**
 * Отчёты (0.14.1, GH-ISSUES, макет A - доработка прежнего листа предложений): вкладки «Мои» (свои отчёты с судьбой их Issues и
 * ответами; число новых ответов на вкладке, открытая вкладка их гасит) и «Известные» (открытые ошибки и предложения из игры с
 * голосованием; поиск по тексту и номеру, фильтр по виду). Открытые выше закрытых; ошибки - сначала «Исправляется», затем по
 * номеру, предложения - по рейтингу.
 */
@Composable
fun ReportsSheet(start: ReportsTab, onDismiss: () -> Unit) {
    val model = koinViewModel<FeedbackViewModel>()
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val seen by model.seen.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.loadReports() }
    var tab by rememberSaveable { mutableStateOf(start) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(KindFilter.ALL) }
    val fresh = feedback.mine.filter { own -> own.replyPrint?.let { it != seen[own.id] } == true }.map { it.id }.toSet()
    // Открытая «Мои» гасит новые ответы - но точки этого показа остаются до следующего открытия
    val shownFresh = remember(tab, feedback.mine) { fresh }
    LaunchedEffect(tab, feedback.mine) { if (tab == ReportsTab.MINE) model.markSeen(feedback.mine) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feedback.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            val mineTitle = ui("feedback.tab.mine") + if (tab != ReportsTab.MINE && fresh.isNotEmpty()) " · ${fresh.size}" else ""
            PillTabs(listOf(mineTitle, ui("feedback.tab.known") + " · ${feedback.known.size}"), tab.ordinal, { tab = ReportsTab.entries[it] })
            if (tab == ReportsTab.KNOWN) {
                OutlinedTextField(
                    query,
                    { query = it.take(SEARCH_MAX) },
                    placeholder = { Text(ui("feedback.search")) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PillTabs(KindFilter.entries.map { ui(it.key) }, filter.ordinal, { filter = KindFilter.entries[it] }, segmented = true)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                when (tab) {
                    ReportsTab.MINE -> {
                        val mine = feedback.mine.sortedBy { ReportFate.of(it.status, it.issue).closed }
                        if (mine.isEmpty()) item { MutedText(ui("feedback.none_mine")) }
                        items(mine, key = { it.id }) { own -> OwnCard(own, own.id in shownFresh, !activity.busy) { deleting = own.id } }
                    }

                    ReportsTab.KNOWN -> {
                        val known = knownList(feedback.known, query, filter.kind)
                        if (known.isEmpty()) item { MutedText(ui("feedback.none_known")) }
                        items(known, key = { it.id }) { KnownCard(it, enabled = !activity.busy) { v -> model.vote(it.id, v) } }
                    }
                }
            }
            deleting?.let { id -> ReportDeleteConfirm(activity.busy, onDismiss = { deleting = null }) { model.deleteReport(id) } }
        }
    }
}

/** Известные проблемы по поиску [query] (текст или «#номер») и виду [kind]: ошибки - «Исправляется» первыми, затем по номеру; предложения - по рейтингу. */
private fun knownList(all: List<Suggestion>, query: String, kind: FeedbackKind?): List<Suggestion> {
    val words = query.trim().removePrefix("#")
    val found = all.filter { kind == null || it.kind == kind }.filter { words.isEmpty() || it.text.contains(words, ignoreCase = true) || it.issue?.number?.toString() == words }
    val (bugs, ideas) = found.partition { it.kind == FeedbackKind.BUG }
    return bugs.sortedWith(compareBy<Suggestion> { it.issue?.state != IssueState.IN_PROGRESS }.thenBy { it.issue?.number ?: Int.MAX_VALUE }) +
        ideas.sortedByDescending { it.rating }
}

/** Карточка в две строки (макет A): вид, текст, судьба справа; номер Issue и голоса; ответ - третьей строкой, если он есть. */
@Composable private fun ReportCardFrame(fate: ReportFate, dim: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxWidth().alpha(if (dim) DIM else 1f).background(Panel, shape).border(1.dp, fate.tint.copy(alpha = .55f), shape).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable private fun KindMark(kind: FeedbackKind) {
    Icon(if (kind == FeedbackKind.SUGGESTION) Icons.Outlined.Lightbulb else Icons.Outlined.BugReport, kindTitle(kind), tint = Muted, modifier = Modifier.size(16.dp))
}

@Composable private fun OwnCard(own: OwnReport, fresh: Boolean, enabled: Boolean, onDelete: () -> Unit) {
    val fate = ReportFate.of(own.status, own.issue)
    ReportCardFrame(fate, fate.closed) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KindMark(own.kind)
            Text(own.text, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            FatePill(fate, own.kind, own.issue, fresh)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            own.issue?.let { Text(ui("feedback.issue_number", it.number), color = Gold, style = MaterialTheme.typography.labelSmall) }
            if (own.kind == FeedbackKind.SUGGESTION) Score(own.likes, own.dislikes)
            Spacer(Modifier.weight(1f))
            if (own.deletable) ReportDeleteButton(enabled, onDelete)
        }
        val word = own.issue?.comment?.takeIf { it.isNotBlank() }?.let { ui("feedback.dev_reply", it) } ?: own.reason.takeIf { it.isNotBlank() }?.let { ui("feedback.mod_reply", it) }
        word?.let { Text(it, color = Rune, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable private fun KnownCard(known: Suggestion, enabled: Boolean, onVote: (Vote) -> Unit) {
    val fate = ReportFate.of(known.status, known.issue)
    ReportCardFrame(fate, dim = false) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KindMark(known.kind)
            Text(known.text, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            FatePill(fate, known.kind, known.issue)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            known.issue?.let { Text(ui("feedback.issue_number", it.number), color = Gold, style = MaterialTheme.typography.labelSmall) }
            if (known.mine) Text(ui("feedback.yours"), color = Gold, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            if (known.kind == FeedbackKind.SUGGESTION) VoteRow(known, enabled, onVote)
        }
        known.issue?.comment?.takeIf { it.isNotBlank() }?.let { Text(ui("feedback.dev_reply", it), color = Rune, style = MaterialTheme.typography.labelMedium) }
    }
}

/**
 * Похожее уже известно (0.14.1, GH-ISSUES мелочи): под формой отчёта - до трёх открытых известных проблем, близких к черновику,
 * с голосованием за предложения: игрок видит, что об этом уже знают, и голосует, а не пишет дубль.
 */
@Composable fun SimilarKnown(similar: List<Suggestion>, enabled: Boolean, onVote: (String, Vote) -> Unit) {
    if (similar.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui("feedback.similar"), color = Rune, style = MaterialTheme.typography.labelLarge)
        similar.forEach { known -> KnownCard(known, enabled) { vote -> onVote(known.id, vote) } }
    }
}

/** Голоса «+N / −M». */
@Composable private fun Score(likes: Int, dislikes: Int) {
    Text(ui("feedback.score", likes, dislikes), color = Muted, style = MaterialTheme.typography.labelSmall)
}

@Composable private fun VoteRow(suggestion: Suggestion, enabled: Boolean, onVote: (Vote) -> Unit) {
    VoteButton("+", suggestion.likes, suggestion.vote == Vote.LIKE, Vital, enabled && suggestion.votable) { onVote(Vote.LIKE) }
    VoteButton("−", suggestion.dislikes, suggestion.vote == Vote.DISLIKE, LifeRed, enabled && suggestion.votable) { onVote(Vote.DISLIKE) }
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

/** Голос «+» или «−» со счётом: свой голос подсвечен цветом стороны (макет A). */
@Composable private fun VoteButton(sign: String, count: Int, chosen: Boolean, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Text(
        "$sign$count",
        color = if (chosen) {
            tint
        } else if (enabled) {
            Parchment
        } else {
            Muted
        },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.background(if (chosen) tint.copy(alpha = .18f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Закрытые свои отчёты приглушены. */
private const val DIM = .6f

/** Длина поиска известных проблем. */
private const val SEARCH_MAX = 60
