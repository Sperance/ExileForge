package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.feedback.Feedback
import com.sperance.exileforge.core.i18n.locOr
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

/** Вкладка листа отчётов (0.14.1, макет A): свои отчёты или известные проблемы. */
enum class ReportsTab { MINE, KNOWN }

/**
 * Отчёты (0.14.1, GH-ISSUES, макет A; 0.14.2 - без предложений): вкладки «Мои» (свои отчёты с судьбой их Issues и перепиской;
 * число новых ответов на вкладке, открытая вкладка их гасит) и «Известные» (открытые ошибки из игры с голосованием «+ / −», поиск
 * по тексту и номеру). Открытые выше закрытых; известные - по рейтингу и номеру. Списки
 * перечитываются (0.14.4, #33) потягиванием вниз и нажатием на уже открытую вкладку.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var searching by rememberSaveable { mutableStateOf(false) }
    val fresh = feedback.mine.filter { own -> own.replyPrint?.let { it != seen[own.id] } == true }.map { it.id }.toSet()
    // Открытая «Мои» гасит новые ответы - но точки этого показа остаются до следующего открытия
    val shownFresh = remember(tab, feedback.mine) { fresh }
    LaunchedEffect(tab, feedback.mine) { if (tab == ReportsTab.MINE) model.markSeen(feedback.mine) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feedback.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            val mineTitle = ui("feedback.tab.mine") + if (tab != ReportsTab.MINE && fresh.isNotEmpty()) " · ${fresh.size}" else ""
            // Шапка (0.14.2, макет «Отчёты у модерации», вариант A для игрока): вкладки и значок поиска, поле - по нажатию
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    PillTabs(listOf(mineTitle, ui("feedback.tab.known") + " · ${feedback.known.size}"), tab.ordinal, { tab = ReportsTab.entries[it] }, onReselect = model::loadReports)
                }
                if (tab == ReportsTab.KNOWN) {
                    IconButton(onClick = {
                        searching = !searching
                        if (!searching) query = ""
                    }) { Icon(Icons.Outlined.Search, ui("feedback.search_open"), tint = if (searching) GoldBright else Muted) }
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
                ReportsList(tab, feedback, query, shownFresh, !activity.busy, model) { deleting = it }
            }
            deleting?.let { id -> ReportDeleteConfirm(activity.busy, onDismiss = { deleting = null }) { model.deleteReport(id) } }
        }
    }
}

/** Список открытой вкладки [tab]: свои отчёты ([fresh] - с новыми ответами) или известные проблемы по поиску [query]. */
@Composable private fun ReportsList(tab: ReportsTab, feedback: Feedback, query: String, fresh: Set<String>, enabled: Boolean, model: FeedbackViewModel, onDelete: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        when (tab) {
            ReportsTab.MINE -> {
                val mine = feedback.mine.sortedBy { ReportFate.of(it.status, it.issue).closed }
                if (mine.isEmpty()) item { MutedText(ui("feedback.none_mine")) }
                items(mine, key = { it.id }) { own -> OwnCard(own, own.id in fresh, enabled, onAnswer = { text -> model.answerReport(own.id, text) }) { onDelete(own.id) } }
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

/**
 * Карточка отчёта - тикет (0.14.2, #23, макет A): вид, номер Issue, судьба и дата сверху; текст до трёх строк; путь отчёта шкалой
 * из четырёх шагов ([StepPath]); ответ разработчика или модерации цитатой; подвал - голоса, ссылка на Issue, удаление. Закрытые
 * свои приглушены. Свой отчёт и известная проблема - одна карточка, разное - в подвале.
 */
@Composable private fun ReportTicket(
    kind: FeedbackKind,
    text: String,
    fate: ReportFate,
    issue: IssueView?,
    createdAt: String,
    thread: List<Pair<NoteAuthor, String>>,
    dim: Boolean,
    fresh: Boolean = false,
    onAnswer: ((String) -> Unit)? = null,
    footer: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().alpha(if (dim) DIM else 1f).clip(shape).background(Panel).border(1.dp, Bronze, shape)) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindMark(kind)
            issue?.let { Text(ui("feedback.issue_number", it.number), color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
            FatePill(fate, kind, issue, fresh)
            Spacer(Modifier.weight(1f))
            Text(shortDate(createdAt), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        Text(text, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        FateSteps(StepPath.of(fate, kind, issue), Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp))
        thread.forEach { (who, words) -> ThreadNote(who, words) }
        onAnswer?.let { send -> AnswerField(send) }
        Row(
            Modifier.fillMaxWidth().background(PanelRaised).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            footer()
            Spacer(Modifier.weight(1f))
            issue?.url?.takeIf { it.isNotBlank() }?.let { link ->
                ForgeTextButton(onClick = { runCatching { uri.openUri(link) } }) { Text(ui("feedback.github"), color = Gold, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

/**
 * Путь отчёта для шкалы (0.14.2, #23; 0.14.4 - три шага): принят, в работе (Issue открыт - отправка и работа теперь одно), итог;
 * [reached] - сколько пройдено (последний пройденный - «сейчас»), [tint] - цвет судьбы; последний шаг - итог: у отказа это сам
 * отказ, у дубля - его номер.
 */
private data class StepPath(val labels: List<String>, val reached: Int, val tint: Color) {
    companion object {
        fun of(fate: ReportFate, kind: FeedbackKind, issue: IssueView?): StepPath {
            val done = ui("feedback.fate.fixed")
            val base = listOf(ui("feedback.step.accepted"), ui("feedback.fate.open"))
            return when (fate) {
                ReportFate.MODERATION -> StepPath(base + done, 1, fate.tint)
                ReportFate.REJECTED -> StepPath(listOf(base[0], fateTitle(fate, kind, issue, short = true)), 2, fate.tint)
                ReportFate.SENT, ReportFate.OPEN -> StepPath(base + done, 2, fate.tint)
                ReportFate.FIXED -> StepPath(base + fateTitle(fate, kind, issue), 3, fate.tint)
                ReportFate.NOT_PLANNED, ReportFate.INVALID, ReportFate.DUPLICATE -> StepPath(base + fateTitle(fate, kind, issue, short = true), 3, fate.tint)
            }
        }
    }
}

/** Шкала шагов: пройденные залиты цветом судьбы, текущий - с ореолом, линии между ними - пройденные тоже цветом. */
@Composable private fun FateSteps(steps: StepPath, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        steps.labels.forEachIndexed { i, label ->
            val passed = i < steps.reached
            val now = i == steps.reached - 1
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                    val centre = Offset(size.width / 2, size.height / 2)
                    val line = 2.dp.toPx()
                    if (i > 0) drawRect(if (passed) steps.tint else Bronze, Offset(0f, centre.y - line / 2), Size(centre.x, line))
                    if (i < steps.labels.lastIndex) drawRect(if (i + 1 < steps.reached) steps.tint else Bronze, Offset(centre.x, centre.y - line / 2), Size(centre.x, line))
                    if (now) drawCircle(steps.tint.copy(alpha = .25f), 7.dp.toPx(), centre)
                    drawCircle(if (passed) steps.tint else Ink, 5.dp.toPx(), centre)
                    drawCircle(if (passed) steps.tint else Bronze, 5.dp.toPx(), centre, style = Stroke(2.dp.toPx()))
                }
                Text(
                    label,
                    color = if (now) GoldBright else Muted,
                    fontSize = 10.sp,
                    fontWeight = if (now) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/** День и месяц отчёта из ISO-времени сервера («2026-10-15T13:06:30» → «15.10»); не разобралось - пусто. */
private fun shortDate(createdAt: String): String = createdAt.take(10).split('-').takeIf { it.size == 3 }?.let { (_, month, day) -> "$day.$month" }.orEmpty()

/**
 * Переписка карточки (0.14.2): слово модерации при решении ([reason]), затем вся переписка Issue по порядку - разработчик,
 * модерация из игры и автор.
 */
private fun thread(issue: IssueView?, reason: String): List<Pair<NoteAuthor, String>> = listOfNotNull(reason.takeIf { it.isNotBlank() }?.let { NoteAuthor.MODERATION to it }) + issue?.notes.orEmpty().map { it.author to it.text }

/** Запись переписки цитатой: разработчик - руной слева, модерация - золотом слева, автор - пергаментом справа. */
@Composable private fun ThreadNote(who: NoteAuthor, words: String) {
    val tint = when (who) {
        NoteAuthor.DEVELOPER -> Rune
        NoteAuthor.MODERATION -> Gold
        NoteAuthor.AUTHOR -> Parchment
    }
    val mine = who == NoteAuthor.AUTHOR
    Column(
        Modifier.padding(start = if (mine) 36.dp else 12.dp, end = if (mine) 12.dp else 36.dp).padding(bottom = 8.dp).fillMaxWidth()
            .background(tint.copy(alpha = .08f), RoundedCornerShape(8.dp))
            .drawBehind { drawRect(tint, topLeft = Offset(if (mine) size.width - 3.dp.toPx() else 0f, 0f), size = Size(3.dp.toPx(), size.height)) }
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(ui("feedback.who.${who.name.lowercase()}"), color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(words, color = Parchment, style = MaterialTheme.typography.bodySmall)
    }
}

/** Ответ автора в Issue (0.14.2): поле и «Отправить»; уходит комментарием в GitHub и встаёт в переписку. */
@Composable private fun AnswerField(onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(text, { text = it.take(ANSWER_MAX) }, placeholder = { Text(ui("feedback.answer")) }, maxLines = 4, modifier = Modifier.weight(1f))
        ForgeTextButton(
            enabled = text.isNotBlank(),
            onClick = {
                onSend(text.trim())
                text = ""
            },
        ) { Text(ui("feedback.answer_send"), color = Gold, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable private fun KindMark(kind: FeedbackKind) {
    Icon(Icons.Outlined.BugReport, kindTitle(kind), tint = Muted, modifier = Modifier.size(16.dp))
}

@Composable private fun OwnCard(own: OwnReport, fresh: Boolean, enabled: Boolean, onAnswer: (String) -> Unit, onDelete: () -> Unit) {
    val fate = ReportFate.of(own.status, own.issue)
    // Ответить в Issue можно, пока он открыт (0.14.2)
    val answer = onAnswer.takeIf { own.issue?.state?.open == true && enabled }
    ReportTicket(own.kind, own.text, fate, own.issue, own.createdAt, thread(own.issue, own.reason), dim = fate.closed, fresh = fresh, onAnswer = answer) {
        Score(own.likes, own.dislikes)
        if (own.deletable) ReportDeleteButton(enabled, onDelete)
    }
}

@Composable private fun KnownCard(known: KnownReport, enabled: Boolean, onVote: (Vote) -> Unit) {
    val fate = ReportFate.of(known.status, known.issue)
    ReportTicket(known.kind, known.text, fate, known.issue, known.createdAt, thread(known.issue, ""), dim = false) {
        if (known.mine) Text(ui("feedback.yours"), color = Gold, style = MaterialTheme.typography.labelSmall)
        VoteRow(known, enabled, onVote)
    }
}

/** Голоса «+N / −M». */
@Composable private fun Score(likes: Int, dislikes: Int) {
    Text(ui("feedback.score", likes, dislikes), color = Muted, style = MaterialTheme.typography.labelSmall)
}

@Composable private fun VoteRow(known: KnownReport, enabled: Boolean, onVote: (Vote) -> Unit) {
    VoteButton("+", known.likes, known.vote == Vote.LIKE, Vital, enabled && known.votable) { onVote(Vote.LIKE) }
    VoteButton("−", known.dislikes, known.vote == Vote.DISLIKE, LifeRed, enabled && known.votable) { onVote(Vote.DISLIKE) }
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

/** Длина ответа автора в Issue из игры (0.14.2): сервер всё равно режет по `inputs.report`. */
private const val ANSWER_MAX = 1000

/** Закрытые свои отчёты приглушены. */
private const val DIM = .6f

/** Длина поиска известных проблем. */
private const val SEARCH_MAX = 60
