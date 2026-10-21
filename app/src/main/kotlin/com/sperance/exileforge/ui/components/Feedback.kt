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
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.feedbackKindTitle
import com.sperance.exileforge.core.feedback.Feedback
import com.sperance.exileforge.core.feedback.FeedbackModel
import com.sperance.exileforge.core.feedback.ReportSearch
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.IssueState
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.core.model.feedback.NoteAuthor
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.InputLimits
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

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
        fun of(status: ReportStatus, issue: IssueView?): ReportFate = when {
            status == ReportStatus.REJECTED -> REJECTED

            status == ReportStatus.WITHDRAWN -> WITHDRAWN

            issue == null -> when (status) {
                ReportStatus.SENT -> SENT
                ReportStatus.DISCUSSION -> DISCUSSION
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

/** Что показать в листе отчётов: отчёт [focus], к которому лист прокручен и который подсвечен; null - без него. */
data class ReportsView(val focus: String? = null)

/**
 * Свои отчёты (без падений) с судьбой их Issues и перепиской - тикеты-чаты; строка поиска ([SearchBar], [ReportSearch]) над ними.
 * Открытые выше закрытых. Список перечитывается потягиванием вниз.
 *
 * Открытый лист гасит новые ответы, точки этого показа остаются до следующего открытия. Отчёт [ReportsView.focus] (письмо о нём открыло
 * лист) подсвечен, и лист прокручен к нему. [issueLinks] - тикеты несут «Issue #N ↗» ([IssueLink]): тестировщику и модерации. Пределы
 * поиска и ответа - правил ([inputs]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsSheet(start: ReportsView, issueLinks: Boolean, inputs: InputLimits, onDismiss: () -> Unit) {
    val model = koinViewModel<FeedbackViewModel>().model
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val seen by model.seen.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.loadReports() }
    var query by rememberSaveable { mutableStateOf("") }
    val shownFresh = remember(feedback.mine) { feedback.mine.filter { own -> own.replyPrint?.let { it != seen[own.id] } == true }.mapTo(HashSet()) { it.id } }
    LaunchedEffect(feedback.mine) { model.markSeen(feedback.mine) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feedback.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            SearchBar(query, { query = it }, ui("feedback.search"), maxLength = inputs.search)
            PullToRefreshBox(isRefreshing = Reads.FEEDBACK in activity.loading, onRefresh = model::loadReports, modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(LocalIssueLinks provides issueLinks) {
                    ReportsList(feedback, query, shownFresh, start.focus, !activity.busy, inputs.report, model)
                }
            }
        }
    }
}

/**
 * Свои отчёты по поиску [query] ([ReportSearch]; отбор и порядок считаются заново только со сменой отчётов или запроса), [fresh] - с
 * новыми ответами, [limit] - предел ответа. Отчёт [focus] подсвечен, и список прокручен к нему, как только он пришёл.
 */
@Composable private fun ReportsList(feedback: Feedback, query: String, fresh: Set<String>, focus: String?, enabled: Boolean, limit: Int, model: FeedbackModel) {
    val list = rememberLazyListState()
    val mine = remember(feedback.mine, query) {
        val search = ReportSearch(query)
        feedback.mine.filter { search.matches(it.text, it.thread, it.issue) }.sortedBy { ReportFate.of(it.status, it.issue).closed }
    }
    val focusAt = remember(mine, focus) { mine.indexOfFirst { it.id == focus }.takeIf { it >= 0 } }
    LaunchedEffect(focusAt) { focusAt?.let { list.animateScrollToItem(it) } }
    LazyColumn(Modifier.fillMaxSize(), state = list, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (mine.isEmpty()) item { MutedText(ui(if (feedback.mine.isEmpty()) "feedback.none_mine" else "feedback.none_found")) }
        items(mine, key = { it.id }) { own -> OwnCard(own, own.id in fresh, own.id == focus, enabled, limit, onAnswer = { text -> model.answerReport(own.id, text) }) { model.closeReport(own.id) } }
    }
}

/**
 * Свой отчёт - тикет-чат: переписка пузырями, под ней поле ответа с круглой кнопкой - пока сервер разрешает ответ
 * ([OwnReport.answerable]: на модерации, до выгрузки или с открытым Issue), иначе его нет; пока идёт загрузка ([enabled] - нет), поле
 * погашено, но на месте. Новый и обсуждаемый отчёт автор закрывает сам полосой удержания «Закрыть отчёт» ([OwnReport.closable],
 * [onClose]) - без возврата; закрытый автором вместо поля несёт плашку «писать нельзя».
 */
@Composable private fun OwnCard(own: OwnReport, fresh: Boolean, focused: Boolean, enabled: Boolean, limit: Int, onAnswer: (String) -> Unit, onClose: () -> Unit) {
    val fate = ReportFate.of(own.status, own.issue)
    ReportTicket(
        own.kind,
        own.text,
        fate,
        own.issue,
        own.createdAt,
        own.thread,
        dim = fate.closed && !focused,
        fresh = fresh,
        focused = focused,
        topic = own.topic,
        below = {
            if (own.answerable) AnswerField(enabled, limit, onAnswer)
            if (own.closable) CloseBar(enabled, onClose)
            if (own.status == ReportStatus.WITHDRAWN) ClosedPlate()
        },
    ) {}
}

/** Закрыть свой отчёт - полоса во всю ширину тикета, удержанием: закрытие без возврата, писать в отчёт больше нельзя. */
@Composable private fun CloseBar(enabled: Boolean, onClose: () -> Unit) {
    ForgeButton(
        onClick = onClose,
        modifier = Modifier.fillMaxWidth(),
        kind = ButtonKind.Danger,
        size = ButtonSize.Wide,
        state = ButtonState.of(enabled),
        icon = Icons.Outlined.Close,
        hold = Hold(),
    ) { Text(ui("feedback.close_hold"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/** Плашка закрытого автором отчёта вместо поля ответа: писать нельзя, вернётся ошибка - новый отчёт. */
@Composable private fun ClosedPlate() {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().background(Quiet.copy(alpha = .08f), shape).border(1.dp, Quiet.copy(alpha = .25f), shape).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Lock, null, tint = Quiet, modifier = Modifier.size(16.dp))
        Text(ui("feedback.closed_note"), color = Quiet, style = MaterialTheme.typography.bodySmall)
    }
}

/** Вопрос перед удалением отчёта: удержание - [onDelete], отмена - [onDismiss]. */
@Composable fun ReportDeleteConfirm(busy: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    ConfirmSheet(ui("feedback.delete_q"), ui("feedback.delete"), onDismiss = onDismiss, note = ui("feedback.delete_note"), danger = true, busy = busy, onConfirm = onDelete)
}
