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
    reply: Pair<String, String>?,
    dim: Boolean,
    fresh: Boolean = false,
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
        reply?.let { (who, words) ->
            Column(
                Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp).fillMaxWidth()
                    .background(Rune.copy(alpha = .08f), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .drawBehind { drawRect(Rune, size = Size(3.dp.toPx(), size.height)) }
                    .padding(start = 11.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Text(who, color = Rune, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(words, color = Parchment, style = MaterialTheme.typography.bodySmall)
            }
        }
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
 * Путь отчёта для шкалы (0.14.2, #23): четыре шага - принят, отправлен, в работе, итог; [reached] - сколько пройдено (последний
 * пройденный - «сейчас»), [tint] - цвет судьбы; последний шаг - итог: у отказа это сам отказ, у дубля - его номер.
 */
private data class StepPath(val labels: List<String>, val reached: Int, val tint: Color) {
    companion object {
        fun of(fate: ReportFate, kind: FeedbackKind, issue: IssueView?): StepPath {
            val idea = kind == FeedbackKind.SUGGESTION
            val work = ui(if (idea) "feedback.fate.doing" else "feedback.fate.fixing")
            val done = ui(if (idea) "feedback.fate.done" else "feedback.fate.fixed")
            val base = listOf(ui("feedback.step.accepted"), ui("feedback.fate.sent"), work)
            return when (fate) {
                ReportFate.MODERATION -> StepPath(base + done, 1, fate.tint)
                ReportFate.REJECTED -> StepPath(listOf(base[0], fateTitle(fate, kind, issue, short = true)), 2, fate.tint)
                ReportFate.SENT, ReportFate.OPEN -> StepPath(base + done, 2, fate.tint)
                ReportFate.IN_PROGRESS -> StepPath(base + done, 3, fate.tint)
                ReportFate.FIXED, ReportFate.DONE -> StepPath(base + fateTitle(fate, kind, issue), 4, fate.tint)
                ReportFate.NOT_PLANNED, ReportFate.INVALID, ReportFate.DUPLICATE -> StepPath(base.take(2) + fateTitle(fate, kind, issue, short = true), 3, fate.tint)
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

/** Кто ответил последним: слово разработчика в Issue главнее слова модерации; ответа нет - null. */
private fun lastWord(issue: IssueView?, reason: String): Pair<String, String>? =
    issue?.comment?.takeIf { it.isNotBlank() }?.let { ui("feedback.who.dev") to it } ?: reason.takeIf { it.isNotBlank() }?.let { ui("feedback.who.mod") to it }

@Composable private fun KindMark(kind: FeedbackKind) {
    Icon(if (kind == FeedbackKind.SUGGESTION) Icons.Outlined.Lightbulb else Icons.Outlined.BugReport, kindTitle(kind), tint = Muted, modifier = Modifier.size(16.dp))
}

@Composable private fun OwnCard(own: OwnReport, fresh: Boolean, enabled: Boolean, onDelete: () -> Unit) {
    val fate = ReportFate.of(own.status, own.issue)
    ReportTicket(own.kind, own.text, fate, own.issue, own.createdAt, lastWord(own.issue, own.reason), dim = fate.closed, fresh = fresh) {
        if (own.kind == FeedbackKind.SUGGESTION) Score(own.likes, own.dislikes)
        if (own.deletable) ReportDeleteButton(enabled, onDelete)
    }
}

@Composable private fun KnownCard(known: Suggestion, enabled: Boolean, onVote: (Vote) -> Unit) {
    val fate = ReportFate.of(known.status, known.issue)
    ReportTicket(known.kind, known.text, fate, known.issue, known.createdAt, lastWord(known.issue, ""), dim = false) {
        if (known.mine) Text(ui("feedback.yours"), color = Gold, style = MaterialTheme.typography.labelSmall)
        if (known.kind == FeedbackKind.SUGGESTION) VoteRow(known, enabled, onVote)
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
