package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.feedback.OwnReportShelf
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FeedbackSort
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.InputLimits
import com.sperance.exileforge.rules.feedback.BugStatus
import com.sperance.exileforge.rules.feedback.FeedbackQueue
import com.sperance.exileforge.ui.components.AnswerField
import com.sperance.exileforge.ui.components.BackTrailHost
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ChordOverWindow
import com.sperance.exileforge.ui.components.FatePill
import com.sperance.exileforge.ui.components.FeedbackViewModel
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.Hold
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.LineMark
import com.sperance.exileforge.ui.components.LocalIssueLinks
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RefreshHost
import com.sperance.exileforge.ui.components.Refreshable
import com.sperance.exileforge.ui.components.ReportChip
import com.sperance.exileforge.ui.components.ReportChipRow
import com.sperance.exileforge.ui.components.ReportFate
import com.sperance.exileforge.ui.components.ReportLine
import com.sperance.exileforge.ui.components.ReportShotAside
import com.sperance.exileforge.ui.components.ReportSortTabs
import com.sperance.exileforge.ui.components.ReportTicket
import com.sperance.exileforge.ui.components.ReportsView
import com.sperance.exileforge.ui.components.SearchBar
import com.sperance.exileforge.ui.components.TrailLevel
import com.sperance.exileforge.ui.components.rememberPageRefresh
import com.sperance.exileforge.ui.components.reportDate
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Rune
import org.koin.compose.viewmodel.koinViewModel

/**
 * Окно своих отчётов во весь экран поверх игры и забега - его открывают письмо об отчёте и «Мои отчёты» листа отчёта об ошибке; корень
 * цепочки «назад» - откуда открыто ([ReportsView.root]), «назад» с окна закрывает его. Тело - [OwnReportsPage].
 */
@Composable internal fun OwnReportsOverlay(view: ReportsView, issueLinks: Boolean, inputs: InputLimits, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ChordOverWindow()
        Box(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) {
            BackTrailHost(view.root) {
                TrailLevel(ui("settings.reports"), onBack = onClose) {
                    RefreshHost(rememberPageRefresh(), Modifier.fillMaxSize()) { OwnReportsPage(issueLinks, inputs, view.focus) }
                }
            }
        }
    }
}

/**
 * Свои отчёты игрока - тот же список, что у модерации: строка поиска, чипы очередей со счётом под поиском («Все» первым, у очереди с
 * новыми ответами - красная точка), порядок «Новые / № Issue»; отбор, счёт и порядок - на месте ([OwnReportShelf]), падений нет.
 * Строка отчёта свёрнута ([ReportLine]: судьба, «новый ответ», номер Issue), касание раскрывает на месте тикет ([OwnTicket]: переписка,
 * ответ, «Закрыть отчёт»), касание его шапки сворачивает. Отчёт [focus] (письмо о нём) раскрыт, и список прокручен к нему.
 *
 * Открытое окно гасит новые ответы, точки этого показа остаются до следующего открытия. Не прочиталось ни разу - плашка «Не удалось
 * загрузить отчёты» с повтором; пустой отбор - «В этом разделе отчётов нет». [issueLinks] - тикеты несут «Issue #N ↗»: тестировщику и
 * модерации. Пределы поиска и ответа - правил ([inputs]). Потягивание перечитывает список.
 */
@Composable internal fun OwnReportsPage(issueLinks: Boolean, inputs: InputLimits, focus: String? = null) {
    val model = koinViewModel<FeedbackViewModel>().model
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val seen by model.seen.collectAsStateWithLifecycle()
    var asked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        model.loadReports()
        asked = true
    }
    val loading = Reads.FEEDBACK in activity.loading
    Refreshable(loading, model::loadReports)
    var query by rememberSaveable { mutableStateOf("") }
    var queue by rememberSaveable { mutableStateOf<FeedbackQueue?>(null) }
    var sort by rememberSaveable { mutableStateOf(FeedbackSort.NEWEST) }
    var open by rememberSaveable { mutableStateOf(focus) }
    val fresh = remember(feedback.mine) { feedback.mine.filter { own -> own.replyPrint?.let { it != seen[own.id] } == true }.mapTo(HashSet()) { it.id } }
    LaunchedEffect(feedback.mine) { model.markSeen(feedback.mine) }
    val shelf = remember(queue, query, sort) { OwnReportShelf(queue, query, sort) }
    val searched = remember(feedback.mine, query) { shelf.searched(feedback.mine) }
    val counts = remember(searched) { shelf.counts(searched) }
    val rows = remember(searched, queue, sort) { shelf.rows(searched) }
    val freshQueues = remember(searched, fresh) { searched.filter { it.id in fresh }.groupingBy { it.queue }.eachCount() }
    val list = rememberLazyListState()
    val focusAt = remember(rows, focus) { rows.indexOfFirst { it.id == focus }.takeIf { it >= 0 } }
    LaunchedEffect(focusAt) { focusAt?.let { list.animateScrollToItem(it + HEAD_ROWS) } }
    val failed = asked && !loading && !feedback.mineRead
    CompositionLocalProvider(LocalIssueLinks provides issueLinks) {
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = SettingsPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item(key = "search") { SearchBar(query, { query = it }, ui("feedback.search"), maxLength = inputs.search) }
            item(key = "queues") {
                ReportChipRow {
                    ReportChip(ui("feedback.mine.all"), counts[null] ?: 0, queue == null, dot = freshQueues.isNotEmpty(), dotDescription = ui("feedback.mine.replied", ui("feedback.mine.all"), freshQueues.values.sum())) { queue = null }
                    FeedbackQueue.entries.forEach { q ->
                        val title = ui(q.mineKey)
                        val replied = freshQueues[q] ?: 0
                        ReportChip(title, counts[q] ?: 0, queue == q, dot = replied > 0, dotDescription = ui("feedback.mine.replied", title, replied)) { queue = q }
                    }
                }
            }
            item(key = "order") { ReportSortTabs(sort) { sort = it } }
            when {
                failed -> item(key = "failed") { LoadFailed(!loading, model::loadReports) }
                rows.isEmpty() && feedback.mineRead -> item(key = "none") { MutedText(ui(if (feedback.mine.none { it.kind != FeedbackKind.CRASH }) "feedback.none_mine" else "feedback.mine.empty")) }
            }
            items(rows, key = { it.id }) { own ->
                if (own.id == open) {
                    OwnTicket(
                        own,
                        own.id in fresh,
                        !activity.busy,
                        inputs.report,
                        onCollapse = { open = null },
                        onAnswer = { text -> model.answerReport(own.id, text) },
                        attachment = if (own.shot.attached) {
                            { ReportShotAside(own.shot, own.createdAt, model.shots) }
                        } else {
                            null
                        },
                    ) { model.closeReport(own.id) }
                } else {
                    OwnReportLine(own, own.id in fresh) { open = own.id }
                }
            }
        }
    }
}

/** Строк над отчётами в списке окна: поиск, чипы, порядок. */
private const val HEAD_ROWS = 3

/** Подпись чипа очереди в окне своих отчётов - словами игрока. */
private val FeedbackQueue.mineKey: String
    get() = when (this) {
        FeedbackQueue.NEW -> "feedback.mine.queue_new"
        FeedbackQueue.DISCUSSION -> "feedback.mine.queue_discussion"
        FeedbackQueue.WORK -> "feedback.mine.queue_work"
        FeedbackQueue.CLOSED -> "feedback.mine.queue_closed"
        FeedbackQueue.REJECTED -> "feedback.mine.queue_rejected"
    }

/** Свёрнутый свой отчёт: справа дата; метки - судьба плашкой ([FatePill]), «новый ответ» ([fresh]) и номер Issue. */
@Composable private fun OwnReportLine(own: OwnReport, fresh: Boolean, onOpen: () -> Unit) {
    val fate = ReportFate.of(own.status, own.issue)
    ReportLine(own.text, own.kind, fate, reportDate(own.createdAt), onOpen) {
        FatePill(fate, own.kind, own.issue)
        if (fresh) LineMark(ui("feedback.line.new_reply"), Rune)
        own.issue?.let { Text(ui("feedback.issue_number", it.number), color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
    }
}

/** Плашка «Не удалось загрузить отчёты» и «Повторить» ([onRetry]; [enabled] - чтение не идёт): список ни разу не прочитался. */
@Composable private fun LoadFailed(enabled: Boolean, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoCard(ui("feedback.mine.failed"), ui("feedback.mine.failed_hint"), failure = true)
        ForgeButton(state = ButtonState.of(enabled), onClick = onRetry, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, icon = Icons.Outlined.Refresh) { Text(ui("feedback.mine.retry")) }
    }
}

/**
 * Свой отчёт раскрытым тикетом-чатом ([ReportTicket]): переписка пузырями, под ней поле ответа с круглой кнопкой - пока сервер разрешает
 * ответ ([OwnReport.answerable]: на модерации, до выгрузки или с открытым Issue), иначе его нет; пока идёт команда ([enabled] - нет),
 * поле погашено, но на месте. Новый и обсуждаемый отчёт автор закрывает сам полосой удержания «Закрыть отчёт» ([OwnReport.closable],
 * [onClose]) - без возврата; закрытый автором вместо поля несёт плашку «писать нельзя». [attachment] - вложение справа от текста
 * (снимок экрана, [ReportShotAside]; null - вложений нет); касание шапки - [onCollapse].
 */
@Composable private fun OwnTicket(
    own: OwnReport,
    fresh: Boolean,
    enabled: Boolean,
    limit: Int,
    onCollapse: () -> Unit,
    onAnswer: (String) -> Unit,
    attachment: (@Composable () -> Unit)?,
    onClose: () -> Unit,
) {
    ReportTicket(
        own.kind,
        own.text,
        ReportFate.of(own.status, own.issue),
        own.issue,
        own.createdAt,
        own.thread,
        dim = false,
        fresh = fresh,
        focused = true,
        onHeader = onCollapse,
        topic = own.topic,
        aside = attachment,
        below = {
            if (own.answerable) AnswerField(enabled, limit, onAnswer)
            if (own.closable) CloseBar(enabled, onClose)
            if (own.status == BugStatus.WITHDRAWN) ClosedPlate()
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Lock, null, tint = Quiet, modifier = Modifier.size(16.dp))
        Text(ui("feedback.closed_note"), color = Quiet, style = MaterialTheme.typography.bodySmall)
    }
}
