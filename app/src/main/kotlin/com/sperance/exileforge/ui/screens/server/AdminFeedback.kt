package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.display.PickOptions
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.feedback.FeedbackModel
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FeedbackQueue
import com.sperance.exileforge.core.model.feedback.FeedbackQueuePage
import com.sperance.exileforge.core.model.feedback.FeedbackSort
import com.sperance.exileforge.core.model.feedback.FullReport
import com.sperance.exileforge.core.model.feedback.IssueMove
import com.sperance.exileforge.core.model.feedback.MailAddressee
import com.sperance.exileforge.core.model.feedback.MailAttachment
import com.sperance.exileforge.core.model.feedback.MailEquipment
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.FeedbackViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Отчёты для модерации - очередь сервера страницами ([PagedColumn], `rules.pageSize`): строка поиска ([SearchBar]: текст, переписка,
 * номер Issue, логин автора - ищет сервер по клавиатуре и очистке поля), чипы очереди со счётом под тем же поиском ([QueueChip]: у
 * «Обсуждения» - точка, пока обсуждения ждут ответа модерации) и в конце их ряда - вид выпадающим выбором, ниже - порядок сегментами без
 * подписи ([FeedbackSort]: новые, рейтинг, номер Issue). Любая смена отбора - и уже выбранный чип - читает очередь с первой страницы,
 * листание до края дочитывает следующую. Строка на отчёт - в две линии ([ReportLine]); касание раскрывает на месте полный тикет - тот же,
 * что у игрока ([AdminTicket] над [ReportTicket]), касание его шапки сворачивает; касание автора - досье модерации ([DossierView])
 * уровнем цепочки «назад». Комментарий модерации к новому отчёту переводит его в «Обсуждение», «В работу» - отправляет в GitHub.
 * Потягивание страницы перечитывает первую страницу.
 */
@Composable internal fun FeedbackAdminPage(account: AccountUi) {
    val vm = koinViewModel<FeedbackViewModel>().model
    val moderation = koinViewModel<ModerationViewModel>().model
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf(feedback.queue.query) }
    var open by remember { mutableStateOf<String?>(null) }
    var dossier by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.loadQueue() }
    if (dossier) {
        val state by moderation.state.collectAsStateWithLifecycle()
        TrailLevel(ui("moderation.dossier"), onBack = {
            moderation.close()
            dossier = false
        }) { SettingsScroll { DossierView(account, moderation, state.dossier) } }
        return
    }
    val loading = Reads.FEEDBACK_ADMIN in activity.loading
    Refreshable(loading) { vm.loadQueue() }
    val queue = feedback.queue
    val page = queue.page
    val reset = remember(queue.kind, queue.queue, queue.query, queue.sort) { Any() }
    PagedColumn(page.items.size, page.more, loading, vm::moreQueue, Modifier.fillMaxSize(), reset = reset, contentPadding = SettingsPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item(key = "search") { SearchBar(query, { query = it }, ui("feedback.admin.search"), onSearch = { vm.filterQueue { it.copy(query = query) } }, maxLength = SEARCH) }
        item(key = "queues") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FeedbackQueue.entries.forEach { q -> QueueChip(q, page, queue.queue == q) { vm.filterQueue { it.copy(queue = q) } } }
                Picker(kindTitle(queue.kind), FeedbackKind.entries.map { k -> kindTitle(k) to { vm.filterQueue { it.copy(kind = k) } } })
            }
        }
        item(key = "order") { PillTabs(FeedbackSort.entries.map { ui(it.key) }, queue.sort.ordinal, { picked -> vm.filterQueue { it.copy(sort = FeedbackSort.entries[picked]) } }, segmented = true) }
        if (page.items.isEmpty() && !loading) item(key = "none") { MutedText(ui("feedback.none")) }
        items(page.items, key = { it.report.id }) { entry ->
            if (entry.report.id == open) {
                AdminTicket(account, vm, entry, onCollapse = { open = null }) { userId ->
                    moderation.open(entry.report.heroId.orEmpty(), userId)
                    dossier = true
                }
            } else {
                ReportLine(entry) { open = entry.report.id }
            }
        }
    }
}

/** Подпись чипа очереди модерации. */
private val FeedbackQueue.key: String
    get() = when (this) {
        FeedbackQueue.NEW -> "feedback.queue.new"
        FeedbackQueue.DISCUSSION -> "feedback.queue.discussion"
        FeedbackQueue.WORK -> "feedback.queue.work"
        FeedbackQueue.CLOSED -> "feedback.queue.closed"
        FeedbackQueue.REJECTED -> "feedback.queue.rejected"
    }

/** Подпись сегмента порядка очереди модерации. */
private val FeedbackSort.key: String
    get() = when (this) {
        FeedbackSort.NEWEST -> "feedback.order.newest"
        FeedbackSort.RATING -> "feedback.order.rating"
        FeedbackSort.ISSUE -> "feedback.order.issue"
    }

/**
 * Чип очереди [queue] со счётом страницы [page] (под тем же видом и поиском; до первого ответа - без счёта): у «Обсуждения» - красная
 * точка, пока обсуждения ждут ответа модерации ([FeedbackQueuePage.replied]), их число - для чтения с экрана.
 */
@Composable private fun QueueChip(queue: FeedbackQueue, page: FeedbackQueuePage, chosen: Boolean, onClick: () -> Unit) {
    val title = ui(queue.key)
    val replied = page.replied.takeIf { queue == FeedbackQueue.DISCUSSION && it > 0 }
    ForgeChip(
        title,
        chosen,
        onClick,
        mode = ChipMode.Choice,
        count = page.counts[queue]?.toInt(),
        leading = ReplyDot.takeIf { replied != null },
        contentDescription = replied?.let { ui("feedback.admin.replied", title, it) },
    )
}

/** Красная точка чипа «Обсуждения»: обсуждения ждут ответа модерации. */
private val ReplyDot: @Composable () -> Unit = { Box(Modifier.size(8.dp).background(LifeRed, CircleShape)) }

/** Где был автор, когда писал: место и раздел отчёта. */
private val FullReport.where: String get() = place.ifBlank { screen }

/**
 * Строка отчёта в две линии: значок вида цветом судьбы, первая строка текста и «×повторы · дата»; ниже - метка «новый ответ» (последнее
 * слово обсуждения - автора) или «Закрыт автором», версия клиента, «⚠ N» падений и «⛔ N» ошибок запросов (только ненулевые), «#Issue» у
 * отправленного, «+N / −M» при голосах и логин автора; касание - [onOpen].
 */
@Composable internal fun ReportLine(entry: AdminReport, onOpen: () -> Unit) {
    val report = entry.report
    val fate = ReportFate.of(report.status, report.issue)
    val shape = RoundedCornerShape(10.dp)
    val small = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Panel).border(1.dp, Bronze, shape).clickable(onClick = onOpen).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReportKindMark(report.kind, fate.tint)
            Text(
                report.text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
                color = Parchment,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                listOfNotNull("×${report.occurrences}".takeIf { report.occurrences > 1 }, reportDate(report.createdAt).takeIf { it.isNotBlank() }).joinToString(" · "),
                color = Muted,
                style = small,
                maxLines = 1,
                softWrap = false,
            )
        }
        Row(Modifier.padding(start = 34.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                report.awaitsModeration -> LineMark(ui("feedback.admin.new_reply"), Rune)
                report.status == ReportStatus.WITHDRAWN -> LineMark(ui("feedback.fate.withdrawn"), Quiet)
            }
            if (report.clientVersion.isNotBlank()) VersionTag(report.clientVersion)
            if (report.crashCount > 0) Badge(ui("feedback.admin.crashes", report.crashCount), ui("feedback.admin.crashes_hint", report.crashCount), LossText)
            if (report.failureCount > 0) Badge(ui("feedback.admin.failures", report.failureCount), ui("feedback.admin.failures_hint", report.failureCount), Caution)
            report.issue?.let { Text(ui("feedback.issue_number", it.number), color = Gold, style = small, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
            if (report.likes + report.dislikes > 0) Text(ui("feedback.admin.plus_minus", report.likes, report.dislikes), color = Muted, style = small, maxLines = 1, softWrap = false)
            Text(entry.login ?: ui("feedback.anonymous"), color = Muted, style = small, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

/** Метка строки очереди - [text] на подложке своего цвета [tint]: «новый ответ», «Закрыт автором». */
@Composable private fun LineMark(text: String, tint: Color) {
    Text(
        text,
        color = tint,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.background(tint.copy(alpha = .14f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Значок счёта строки: [text] цветом [tint], для чтеца экрана - [description]. */
@Composable private fun Badge(text: String, description: String, tint: Color) {
    Text(text, color = tint, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false, modifier = Modifier.semantics { contentDescription = description })
}

/**
 * Раскрытый отчёт модерации - тот же тикет-чат, что у игрока ([ReportTicket]), с версией клиента в шапке и слотами модерации: строка
 * автора ([AuthorLine], касание логина - [onDossier] с аккаунтом автора), обстоятельства дела (у апелляции и жалобы), «Дополнение
 * модерации»; в переписке автор - своим логином, его последний ответ в обсуждении - «новый ответ». Под перепиской у нового и обсуждаемого
 * - поле и решения ([Decisions]): «Комментировать» (автору, отчёт - в «Обсуждении»), «Отклонить» (поле - причина автору) и «В работу»
 * (апелляция и жалоба не уходят наружу - «Принять»); у отправленного - ведение Issue по «Закрыть…». Подвал - голоса (если есть),
 * «Закрыть…» (пока Issue открыт) и «Issue #N ↗»; удаление - значком, по праву. Касание шапки - [onCollapse].
 */
@Composable internal fun AdminTicket(account: AccountUi, vm: FeedbackModel, entry: AdminReport, onCollapse: () -> Unit, onDossier: (String) -> Unit) {
    val report = entry.report
    val fate = ReportFate.of(report.status, report.issue)
    val issue = report.issue
    var deleting by remember(report.id) { mutableStateOf(false) }
    var steering by remember(report.id) { mutableStateOf(false) }
    ReportTicket(
        report.kind,
        report.text,
        fate,
        issue,
        report.createdAt,
        report.thread,
        dim = false,
        version = report.clientVersion,
        focused = true,
        authorName = entry.login ?: ui("feedback.anonymous"),
        newReply = report.awaitsModeration,
        onHeader = onCollapse,
        topic = report.topic,
        issueLink = true,
        author = { AuthorLine(entry, onDossier) },
        extra = {
            if (!report.kind.exported) Circumstances(report.context)
            ModerationNote(account, vm, report)
            if (report.issueError.isNotBlank() && issue == null) {
                Text(ui("feedback.issue_failed", report.issueError), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                ForgeButton(state = ButtonState.of(!account.busy), onClick = { vm.reportToIssue(report.id) }, kind = ButtonKind.Secondary) { Text(ui("feedback.issue_retry")) }
            }
        },
        below = {
            if (entry.moves.isNotEmpty() || entry.discussable) Decisions(account, vm, entry)
            if (steering && issue?.state?.open == true) IssueSteer(account, vm, report.id)
        },
    ) {
        if (report.likes + report.dislikes > 0) {
            Text(ui("feedback.admin.score", report.likes, report.dislikes), color = Muted, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false)
        }
        Row(Modifier.weight(1f).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            if (issue?.state?.open == true) ForgeButton(state = ButtonState.of(!account.busy), onClick = { steering = !steering }, kind = ButtonKind.Secondary) { Text(ui("feedback.admin.steer"), maxLines = 1) }
            if (entry.deletable) {
                ForgeIconButton(Icons.Outlined.Delete, ui("feedback.delete"), onClick = { deleting = true }, kind = ButtonKind.Danger, size = ButtonSize.Compact, state = ButtonState.of(!account.busy))
            }
        }
    }
    if (deleting) ReportDeleteConfirm(account.busy, onDismiss = { deleting = false }) { vm.deleteReport(report.id) }
}

/**
 * Решения модерации о новом и обсуждаемом отчёте: одно поле - комментарий автору, причина отказа или слово при отправке; под ним кнопки,
 * не встающие в ширину - переносом, прижатые вправо: «Комментировать» ([AdminReport.discussable]: комментарий в переписку, автору письмо),
 * «Отклонить» (поле - причина, без неё погашена) и «В работу» (слово необязательно; апелляция и жалоба - «Принять») - по
 * [AdminReport.moves]. Отправленное действие очищает поле.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Decisions(account: AccountUi, vm: FeedbackModel, entry: AdminReport) {
    val report = entry.report
    var word by remember(report.id) { mutableStateOf("") }
    val ready = !account.busy
    val said = word.isNotBlank()
    OutlinedTextField(
        word,
        { word = it.take(REASON) },
        placeholder = { Text(ui(if (entry.discussable) "feedback.admin.comment_hint" else "feedback.reason_field")) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (entry.discussable) {
            ForgeButton(state = ButtonState.of(ready && said), onClick = {
                vm.commentReport(report.id, word.trim())
                word = ""
            }, kind = ButtonKind.Secondary) { Text(ui("feedback.admin.comment"), maxLines = 1) }
        }
        if (ReportStatus.REJECTED in entry.moves) {
            ForgeButton(state = ButtonState.of(ready && said), onClick = {
                vm.setReportStatus(report.id, ReportStatus.REJECTED, word.trim())
                word = ""
            }, kind = ButtonKind.Danger, icon = Icons.Outlined.Close) { Text(ui("feedback.admin.reject"), maxLines = 1) }
        }
        if (ReportStatus.SENT in entry.moves) {
            ForgeButton(state = ButtonState.of(ready), onClick = {
                vm.setReportStatus(report.id, ReportStatus.SENT, word.trim())
                word = ""
            }) { Text(ui(if (report.kind.exported) "feedback.admin.to_work" else "feedback.admin.accept"), maxLines = 1) }
        }
    }
}

/**
 * Кто и откуда: логин автора («логин ›» - касание открывает его досье, [onDossier] с аккаунтом), герой («Морвен, 25 ур.») ссылкой на его
 * карточку и место отчёта; чего нет - не пишется.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuthorLine(entry: AdminReport, onDossier: (String) -> Unit) {
    val report = entry.report
    val small = MaterialTheme.typography.labelSmall
    val hero = report.heroName.ifBlank { report.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle).orEmpty() }
        .takeIf { it.isNotBlank() }?.let { name -> if (report.heroLevel > 0) ui("feedback.admin.hero_line", name, report.heroLevel) else name }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val author = report.userId
        if (author == null) {
            Text(entry.login ?: ui("feedback.anonymous"), color = Muted, style = small)
        } else {
            Text(
                ui("feedback.admin.author", entry.login ?: ui("feedback.anonymous")),
                color = Rune,
                style = small,
                modifier = Modifier.clickable(role = Role.Button, onClickLabel = ui("moderation.dossier")) { onDossier(author) },
            )
        }
        hero?.let {
            Text("·", color = Muted, style = small)
            Text(it, color = Rune, style = small, textDecoration = TextDecoration.Underline, modifier = Modifier.opensPlayer(report.heroId))
        }
        report.where.takeIf { it.isNotBlank() }?.let {
            Text("·", color = Muted, style = small)
            Text(it, color = Muted, style = small)
        }
    }
}

/** Обстоятельства дела апелляции и жалобы строками «ключ: значение»; пусто - ничего. */
@Composable private fun Circumstances(context: Map<String, String>) {
    if (context.isEmpty()) return
    Engraved(ui("bug.context"))
    context.forEach { (key, value) -> MutedText("$key: $value", style = MaterialTheme.typography.labelSmall) }
}

/**
 * Дополнение модерации: его пишут и правят модератор и администратор, оно уходит разделом в тело Issue; автору в игре не видно,
 * письма нет. Свёрнутое - пунктирная плашка с текстом или подсказкой, касание открывает правку; «Сохранить» горит, когда текст разошёлся
 * с сохранённым; пустой текст стирает дополнение.
 */
@Composable private fun ModerationNote(account: AccountUi, vm: FeedbackModel, report: FullReport) {
    var note by remember(report.id, report.moderationNote) { mutableStateOf(report.moderationNote) }
    var editing by remember(report.id) { mutableStateOf(false) }
    if (!editing) {
        val shape = RoundedCornerShape(8.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).dashedBorder(Bronze, shape).clickable { editing = true }.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(ui("feedback.note.title"), color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            if (report.moderationNote.isBlank()) {
                MutedText(ui("feedback.note.tap"), style = MaterialTheme.typography.bodySmall)
            } else {
                Text(report.moderationNote, color = Parchment, style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }
    OutlinedTextField(note, { note = it.take(NOTE) }, label = { Text(ui("feedback.note.title")) }, placeholder = { Text(ui("feedback.note.hint")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
        ForgeButton(onClick = {
            note = report.moderationNote
            editing = false
        }, kind = ButtonKind.Text) { Text(ui("common.cancel")) }
        ForgeButton(state = ButtonState.of(!account.busy && note.trim() != report.moderationNote), onClick = {
            vm.noteReport(report.id, note)
            editing = false
        }, kind = ButtonKind.Secondary) { Text(ui("feedback.note.save")) }
    }
}

/** Пунктирная рамка цвета [color] по форме [shape]. */
private fun Modifier.dashedBorder(color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val dash = 4.dp.toPx()
    drawOutline(outline, color, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))))
}

/** Виды, что уходят в GitHub (как на сервере): ошибка и падение. */
private val FeedbackKind.exported: Boolean get() = this == FeedbackKind.BUG || this == FeedbackKind.CRASH

private const val REASON = 400

/** Длина дополнения модерации: сервер всё равно режет по `inputs.report`. */
private const val NOTE = 2000
private const val SEARCH = 60

/** Редкости вещи-вложения письма на выбор. */
private val MAIL_RARITIES = listOf(Rarity.COMMON, Rarity.MAGIC, Rarity.RARE)

/**
 * Ведение Issue из игры: слово модерации (уходит комментарием) и действие - ответить, закрыть
 * исправленным, отказом или дублем (с номером оригинала). Судьба приходит из GitHub сразу.
 */
@Composable private fun IssueSteer(account: AccountUi, vm: FeedbackModel, id: String) {
    var note by remember(id) { mutableStateOf("") }
    var original by remember(id) { mutableStateOf("") }
    OutlinedTextField(note, { note = it.take(REASON) }, label = { Text(ui("feedback.move.text")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        original,
        { original = it.filter(Char::isDigit).take(DUPLICATE_DIGITS) },
        label = { Text(ui("feedback.move.duplicate")) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IssueMove.entries.forEach { move ->
            val ready = when (move) {
                IssueMove.REPLY -> note.isNotBlank()
                IssueMove.DUPLICATE -> original.isNotBlank()
                else -> true
            }
            ForgeButton(state = ButtonState.of(!account.busy && ready), onClick = { vm.steerIssue(id, move, note, original.toIntOrNull() ?: 0) }, kind = ButtonKind.Secondary) {
                Text(ui("feedback.move.${move.name}"))
            }
        }
    }
}

/** Номер Issue-оригинала у дубля - не длиннее. */
private const val DUPLICATE_DIGITS = 6

/**
 * The administrator's letter: to one account by login or, blank, to every account; a subject, a text, and what it
 * carries — gold, stacks of any item, things of any template at a chosen rarity — built line by line.
 * Адресат из карточки игрока ([MailAddressee]) - плашкой без ввода: письмо уходит по id аккаунта, а имя адресата идёт логином, так что
 * сервер, что id не знает, не разошлёт письмо всем; сброс плашки возвращает ввод логина.
 */
@Composable internal fun MailComposePage(account: AccountUi) {
    val index = account.index ?: return
    val vm = koinViewModel<FeedbackViewModel>().model
    var addressee by remember { mutableStateOf(vm.takeMailTo()) }
    var login by remember { mutableStateOf("") }
    val single = addressee != null || login.isNotBlank()
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var gold by remember { mutableStateOf("") }
    val items = remember { mutableStateMapOf<String, Long>() }
    val equipment = remember { mutableStateListOf<MailEquipment>() }
    ForgePanel {
        val fixed = addressee
        if (fixed != null) {
            OutlinedTextField(
                fixed.name,
                {},
                readOnly = true,
                label = { Text(ui("mail.to")) },
                trailingIcon = { ForgeIconButton(Icons.Outlined.Close, ui("mail.to_clear"), onClick = { addressee = null }) },
                supportingText = { Text(ui("mail.to_one")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                login,
                { login = it.filterNot(Char::isWhitespace).take(account.inputs.login) },
                label = { Text(ui("mail.to")) },
                supportingText = { Text(ui(if (single) "mail.to_one" else "mail.to_all")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            subject,
            { subject = it.take(account.inputs.mailSubject) },
            label = { Text(ui("mail.subject")) },
            supportingText = { LengthCounter(subject, account.inputs.mailSubject) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            body,
            { body = it.take(account.inputs.mailBody) },
            label = { Text(ui("mail.body")) },
            minLines = 4,
            supportingText = { LengthCounter(body, account.inputs.mailBody) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    ForgePanel {
        Engraved(ui("mail.attachment"))
        OutlinedTextField(
            gold,
            { gold = it.filter(Char::isDigit).take(account.inputs.number) },
            label = { Text(ui("tester.gold")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        var item by remember { mutableStateOf<String?>(null) }
        var amount by remember { mutableStateOf("1") }
        val itemOptions = remember(index, account.lang) { PickOptions.items(index) }
        ForgePicker(ui("tester.item"), item, itemOptions, enabled = !account.busy) { item = it }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                amount,
                { amount = it.filter(Char::isDigit).take(account.inputs.number) },
                label = { Text(ui("tester.amount")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            ForgeButton(state = ButtonState.of(item != null && (amount.toLongOrNull() ?: 0) > 0), onClick = { item?.let { code -> items[code] = (items[code] ?: 0) + amount.toLong() } }, kind = ButtonKind.Secondary) {
                Text(ui("mail.add"))
            }
        }
        var template by remember { mutableStateOf<String?>(null) }
        var rarity by remember { mutableStateOf<Rarity?>(null) }
        val templates = remember(index, account.lang) { PickOptions.templates(index) }
        ForgePicker(ui("mail.template"), template, templates, enabled = !account.busy) { template = it }
        RarityChoice(MAIL_RARITIES, { it == rarity }, { rarity = it }, any = ui("mail.template_rarity"))
        ForgeButton(state = ButtonState.of(template != null), onClick = { template?.let { equipment += MailEquipment(it, rarity) } }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) { Text(ui("mail.add")) }
        items.forEach { (code, n) -> AttachedLine("${itemTitle(code)} × $n") { items.remove(code) } }
        equipment.forEachIndexed { i, piece -> AttachedLine(equipmentTitle(piece.template) + (piece.rarity?.let { " · " + ui("enum.rarity.${it.name}") } ?: "")) { equipment.removeAt(i) } }
    }
    ForgeButton(state = ButtonState.of(!account.busy && subject.isNotBlank()), modifier = Modifier.fillMaxWidth(), onClick = {
        val attachment = MailAttachment(gold.toLongOrNull() ?: 0, items.toMap(), equipment.toList())
        vm.sendMail(addressee?.let { MailRequest(it.login, subject, body, attachment, userId = it.userId) } ?: MailRequest(login, subject, body, attachment))
        subject = ""
        body = ""
        gold = ""
        items.clear()
        equipment.clear()
    }) { Text(ui(if (single) "mail.send" else "mail.send_all")) }
}

@Composable private fun AttachedLine(text: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Vital, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        ForgeButton(onClick = onRemove, kind = ButtonKind.Danger, icon = Icons.Outlined.Delete) { Text(ui("mail.remove")) }
    }
}

/**
 * Компактный выпадающий выбор в ряду чипов: значение «Ошибка ▾» в нити, зона касания - общая 44 dp ([touchZone]); пункт [options] -
 * подпись и что он делает.
 */
@Composable private fun Picker(value: String, options: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box {
        Text(
            "$value ▾",
            color = Rune,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.clickable(role = Role.Button) { expanded = true }.touchZone().clip(shape).border(1.dp, Bronze, shape).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        ForgeMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, pick) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    pick()
                })
            }
        }
    }
}
