package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.feedbackKindTitle
import com.sperance.exileforge.core.display.feedbackStatusTitle
import com.sperance.exileforge.core.feedback.Feedback
import com.sperance.exileforge.core.feedback.FeedbackModel
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.IssueState
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.core.model.feedback.KnownReport
import com.sperance.exileforge.core.model.feedback.NoteAuthor
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.model.feedback.Vote
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Who opens the inbox: the app hands it down to the banner's envelope. */
val LocalMailOpen = staticCompositionLocalOf<(() -> Unit)?> { null }

/** A report's status by name, in the server's words. */
fun statusTitle(status: ReportStatus): String = feedbackStatusTitle(status.name)
fun kindTitle(kind: FeedbackKind): String = feedbackKindTitle(kind.name)

/** Цвет статуса модерации: созданный - пергамент, на рассмотрении - руна, отправленный - зелёный, отклонённый - кровь. */
fun statusTint(status: ReportStatus): Color = when (status) {
    ReportStatus.CREATED -> Parchment
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
 * Судьба отчёта глазами игрока: модерация, отказ модерации, затем судьба его Issue. Цвет - кромка
 * карточки и плашка статуса; [closed] - закрытые идут ниже открытых и приглушены.
 */
enum class ReportFate(val tint: Color, val icon: ImageVector, val closed: Boolean = false) {
    MODERATION(Color(0xFF8FD3FF), Icons.Outlined.Schedule),
    REJECTED(Color(0xFFC9746F), Icons.Outlined.Shield, closed = true),
    SENT(Color(0xFFF2C94C), Icons.AutoMirrored.Outlined.Send),
    OPEN(Color(0xFFF2C94C), Icons.AutoMirrored.Outlined.FormatListBulleted),
    FIXED(Color(0xFF7BE0A6), Icons.Outlined.CheckCircle, closed = true),
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
        ReportFate.REJECTED -> ui(if (short) "feedback.fate.rejected_short" else "feedback.fate.rejected")
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

/** Вкладка листа отчётов: свои отчёты или известные проблемы. */
enum class ReportsTab { MINE, KNOWN }

/** Что показать в листе отчётов: вкладку [tab] и отчёт [focus], к которому лист прокручен и который подсвечен; null - без него. */
data class ReportsView(val tab: ReportsTab, val focus: String? = null)

/**
 * Отчёты (без предложений): вкладки «Мои» (свои отчёты с судьбой их Issues и перепиской;
 * число новых ответов на вкладке, открытая вкладка их гасит) и «Известные» (открытые ошибки из игры с голосованием «+ / −», поиск
 * по тексту и номеру). Открытые выше закрытых; известные - по рейтингу и номеру. Списки
 * перечитываются потягиванием вниз и нажатием на уже открытую вкладку.
 *
 * Открытая «Мои» гасит новые ответы, точки этого показа остаются до следующего открытия. Отчёт [ReportsView.focus] (письмо о нём
 * открыло лист) подсвечен, и лист прокручен к нему.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsSheet(start: ReportsView, onDismiss: () -> Unit) {
    val model = koinViewModel<FeedbackViewModel>().model
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val seen by model.seen.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.loadReports() }
    var tab by rememberSaveable { mutableStateOf(start.tab) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    val fresh = feedback.mine.filter { own -> own.replyPrint?.let { it != seen[own.id] } == true }.map { it.id }.toSet()
    val shownFresh = remember(tab, feedback.mine) { fresh }
    LaunchedEffect(tab, feedback.mine) { if (tab == ReportsTab.MINE) model.markSeen(feedback.mine) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feedback.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            val mineTitle = ui("feedback.tab.mine") + if (tab != ReportsTab.MINE && fresh.isNotEmpty()) " · ${fresh.size}" else ""
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    PillTabs(listOf(mineTitle, ui("feedback.tab.known") + " · ${feedback.known.size}"), tab.ordinal, { tab = ReportsTab.entries[it] }, onReselect = model::loadReports)
                }
                if (tab == ReportsTab.KNOWN) {
                    ForgeIconButton(Icons.Outlined.Search, ui("feedback.search_open"), onClick = {
                        searching = !searching
                        if (!searching) query = ""
                    }, kind = if (searching) ButtonKind.Secondary else ButtonKind.Text)
                }
            }
            if (tab == ReportsTab.KNOWN && searching) {
                OutlinedTextField(
                    query,
                    { query = it.take(SEARCH_MAX) },
                    placeholder = { Text(ui("feedback.search")) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PullToRefreshBox(isRefreshing = Reads.FEEDBACK in activity.loading, onRefresh = model::loadReports, modifier = Modifier.weight(1f)) {
                ReportsList(tab, feedback, query, shownFresh, start.focus, !activity.busy, model) { deleting = it }
            }
            deleting?.let { id -> ReportDeleteConfirm(activity.busy, onDismiss = { deleting = null }) { model.deleteReport(id) } }
        }
    }
}

/**
 * Список открытой вкладки [tab]: свои отчёты ([fresh] - с новыми ответами) или известные проблемы по поиску [query]. Свой отчёт
 * [focus] подсвечен, и «Мои» прокручены к нему, как только он пришёл.
 */
@Composable private fun ReportsList(tab: ReportsTab, feedback: Feedback, query: String, fresh: Set<String>, focus: String?, enabled: Boolean, model: FeedbackModel, onDelete: (String) -> Unit) {
    val list = rememberLazyListState()
    val mine = feedback.mine.sortedBy { ReportFate.of(it.status, it.issue).closed }
    val focusAt = mine.indexOfFirst { it.id == focus }.takeIf { tab == ReportsTab.MINE && it >= 0 }
    LaunchedEffect(focusAt) { focusAt?.let { list.animateScrollToItem(it) } }
    LazyColumn(Modifier.fillMaxSize(), state = list, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        when (tab) {
            ReportsTab.MINE -> {
                if (mine.isEmpty()) item { MutedText(ui("feedback.none_mine")) }
                items(mine, key = { it.id }) { own -> OwnCard(own, own.id in fresh, own.id == focus, enabled, onAnswer = { text -> model.answerReport(own.id, text) }) { onDelete(own.id) } }
            }

            ReportsTab.KNOWN -> {
                val known = knownList(feedback.known, query)
                if (known.isEmpty()) item { MutedText(ui("feedback.none_known")) }
                items(known, key = { it.id }) { KnownCard(it, enabled) { v -> model.vote(it.id, v) } }
            }
        }
    }
}

/** Известные проблемы по поиску [query] (текст или «#номер»): по рейтингу, затем по номеру. */
private fun knownList(all: List<KnownReport>, query: String): List<KnownReport> {
    val words = query.trim().removePrefix("#")
    return all.filter { words.isEmpty() || it.text.contains(words, ignoreCase = true) || it.issue?.number?.toString() == words }
        .sortedWith(compareByDescending<KnownReport> { it.rating }.thenBy { it.issue?.number ?: Int.MAX_VALUE })
}

/** Ответить в Issue можно, пока он открыт. */
@Composable private fun OwnCard(own: OwnReport, fresh: Boolean, focused: Boolean, enabled: Boolean, onAnswer: (String) -> Unit, onDelete: () -> Unit) {
    val fate = ReportFate.of(own.status, own.issue)
    val answer = onAnswer.takeIf { own.issue?.state?.open == true && enabled }
    ReportTicket(own.kind, own.text, fate, own.issue, own.createdAt, reportThread(own.issue, own.reason), dim = fate.closed && !focused, fresh = fresh, focused = focused, onAnswer = answer, topic = own.topic) {
        Score(own.likes, own.dislikes)
        if (own.deletable) ReportDeleteButton(enabled, onDelete)
    }
}

@Composable private fun KnownCard(known: KnownReport, enabled: Boolean, onVote: (Vote) -> Unit) {
    val fate = ReportFate.of(known.status, known.issue)
    ReportTicket(known.kind, known.text, fate, known.issue, known.createdAt, reportThread(known.issue, ""), dim = false) {
        if (known.mine) Text(ui("feedback.yours"), color = Gold, style = MaterialTheme.typography.labelSmall)
        VoteRow(known, enabled, onVote)
    }
}

/** Голоса «+N / −M». */
@Composable private fun Score(likes: Int, dislikes: Int) {
    Text(ui("feedback.score", likes, dislikes), color = Muted, style = MaterialTheme.typography.labelSmall)
}

@Composable private fun VoteRow(known: KnownReport, enabled: Boolean, onVote: (Vote) -> Unit) {
    VoteButton("+", ui("feedback.vote.like", known.likes), known.likes, known.vote == Vote.LIKE, Vital, enabled && known.votable) { onVote(Vote.LIKE) }
    VoteButton("−", ui("feedback.vote.dislike", known.dislikes), known.dislikes, known.vote == Vote.DISLIKE, LossText, enabled && known.votable) { onVote(Vote.DISLIKE) }
}

/** Удаление отчёта - всегда с вопросом: оно жёсткое и не возвращается. */
@Composable fun ReportDeleteButton(enabled: Boolean, onClick: () -> Unit) {
    ForgeButton(onClick = onClick, kind = ButtonKind.Danger, size = ButtonSize.Small, icon = Icons.Outlined.Delete, state = ButtonState.of(enabled)) { Text(ui("feedback.delete")) }
}

/** Вопрос перед удалением отчёта: удержание - [onDelete], отмена - [onDismiss]. */
@Composable fun ReportDeleteConfirm(busy: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    ConfirmSheet(ui("feedback.delete_q"), ui("feedback.delete"), onDismiss = onDismiss, note = ui("feedback.delete_note"), danger = true, busy = busy, onConfirm = onDelete)
}

/** Голос «+» или «−» со счётом - чип цвета стороны [tint]: свой голос выбран; [name] - имя для чтения с экрана («Нравится, 3»). */
@Composable private fun VoteButton(sign: String, name: String, count: Int, chosen: Boolean, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    ForgeChip("$sign$count", chosen, onClick, state = ButtonState.of(enabled), accent = tint, contentDescription = name)
}

/** Длина поиска известных проблем. */
private const val SEARCH_MAX = 60
