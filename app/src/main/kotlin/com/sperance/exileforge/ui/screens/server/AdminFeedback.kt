package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FullReport
import com.sperance.exileforge.core.model.feedback.IssueMove
import com.sperance.exileforge.core.model.feedback.MailAttachment
import com.sperance.exileforge.core.model.feedback.MailEquipment
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Отчёты для модерации: поиск по тексту, «#номеру» и
 * логину, чипы очереди со счётом ([Queue]) и вида; строка на отчёт - точка судьбы, текст, автор и метки; нажатие раскрывает
 * отчёт в списке: контекст, ошибки сети, действия, герой, переписка Issue и действия одной строкой. «Рассмотрения» нет: слово
 * модерации к новому отчёту отправляет его в GitHub сразу. Любой чип, и уже выбранный, и потягивание страницы перечитывают
 * список с сервера.
 */
@Composable internal fun FeedbackAdminPage(account: AccountUi) {
    val vm = koinViewModel<FeedbackViewModel>()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var queue by remember { mutableStateOf(Queue.NEW) }
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.loadReports(kind, null) }
    Refreshable(Reads.FEEDBACK_ADMIN in activity.loading) { vm.loadReports(kind, null) }
    val found = feedback.reports.filter { it.matches(query) }
    OutlinedTextField(
        query,
        { query = it.take(SEARCH) },
        placeholder = { Text(ui("feedback.admin.search")) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Queue.entries.forEach { q ->
            CountChip(ui(q.key), found.count { q.holds(it) }, queue == q) {
                queue = q
                vm.loadReports(kind, null)
            }
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FeedbackKind.entries.forEach { k ->
            CountChip(kindTitle(k), null, kind == k) {
                kind = k
                vm.loadReports(k, null)
            }
        }
    }
    val shown = found.filter { queue.holds(it) }.sortedByDescending { it.report.createdAt }
    if (shown.isEmpty()) MutedText(ui("feedback.none"))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).border(1.dp, Bronze, RoundedCornerShape(12.dp))) {
        shown.forEachIndexed { i, entry ->
            if (i > 0) HorizontalDivider(color = Bronze)
            ReportRow(account, vm, entry, entry.report.id == open) { open = if (open == entry.report.id) null else entry.report.id }
        }
    }
}

/** Очередь модерации: новые ждут решения, в GitHub - Issue открыт или ещё выгружается, закрытые, отклонённые. */
private enum class Queue(val key: String) {
    NEW("feedback.queue.new"),
    GITHUB("feedback.queue.github"),
    CLOSED("feedback.queue.closed"),
    REJECTED("feedback.queue.rejected"),
    ;

    fun holds(entry: AdminReport): Boolean {
        val report = entry.report
        return when (this) {
            NEW -> report.status == ReportStatus.CREATED
            GITHUB -> report.status == ReportStatus.SENT && report.issue?.state?.open != false
            CLOSED -> report.status == ReportStatus.SENT && report.issue?.state?.open == false
            REJECTED -> report.status == ReportStatus.REJECTED
        }
    }
}

/** Отчёт под поиском [query]: текст, «#номер» Issue или логин автора; пусто - любой. */
private fun AdminReport.matches(query: String): Boolean {
    val words = query.trim()
    if (words.isEmpty()) return true
    val number = words.removePrefix("#")
    return report.text.contains(words, ignoreCase = true) || report.issue?.number?.toString() == number || login?.contains(words, ignoreCase = true) == true
}

/** Чип фильтра: подпись, счёт (null - без счёта), выбран ли. */
@Composable private fun CountChip(title: String, count: Int?, chosen: Boolean, onClick: () -> Unit) {
    FilterChip(selected = chosen, onClick = onClick, label = { Text(listOfNotNull(title, count?.toString()).joinToString(" ")) })
}

/** Строка отчёта: точка судьбы, текст и метки; раскрытая [open] - весь отчёт и действия. */
@Composable private fun ReportRow(account: AccountUi, vm: FeedbackViewModel, entry: AdminReport, open: Boolean, onToggle: () -> Unit) {
    val report = entry.report
    val fate = ReportFate.of(report.status, report.issue)
    var deleting by remember(report.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(if (open) PanelRaised else Color.Transparent).clickable(onClick = onToggle).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(fate.tint, CircleShape))
            Text(report.text, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = if (open) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(report.createdAt.take(10).split('-').takeIf { it.size == 3 }?.let { (_, m, d) -> "$d.$m" }.orEmpty(), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        MutedText(rowMeta(entry), style = MaterialTheme.typography.labelSmall)
        if (open) ReportDetail(account, vm, entry) { deleting = true }
    }
    if (deleting) ReportDeleteConfirm(account.busy, onDismiss = { deleting = false }) { vm.deleteReport(report.id) }
}

/** Метки строки: автор и герой, Issue и его судьба, голоса, повторы падения. */
private fun rowMeta(entry: AdminReport): String {
    val report = entry.report
    return listOfNotNull(
        entry.login ?: ui("feedback.anonymous"),
        report.heroClass.takeIf { it.isNotBlank() }?.let { "$it ${report.heroLevel}" },
        report.issue?.let { "#${it.number} · ${fateTitle(ReportFate.of(report.status, it), report.kind, it, short = true)}" },
        "▲${report.likes.size} ▼${report.dislikes.size}".takeIf { report.likes.size + report.dislikes.size > 0 },
        "×${report.occurrences}".takeIf { report.occurrences > 1 },
        report.reason.takeIf { report.status == ReportStatus.REJECTED && it.isNotBlank() }?.let { "«$it»" },
    ).joinToString(" · ")
}

/** Раскрытый отчёт: контекст, ошибки сети, действия, герой, переписка - и что модерация может с ним сделать. */
@Composable private fun ReportDetail(account: AccountUi, vm: FeedbackViewModel, entry: AdminReport, onDelete: () -> Unit) {
    val report = entry.report
    report.heroId?.takeIf { it.isNotBlank() }?.let { hero ->
        Text(ui("feedback.hero", report.heroName.ifBlank { "…" + hero.takeLast(6) }), color = Gold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.opensPlayer(hero))
    }
    Facts(ui("bug.context"), report.context.map { (key, value) -> "$key: $value" } + listOfNotNull(report.screen.takeIf { it.isNotBlank() }?.let { "screen: $it" }))
    Facts(ui("bug.failures"), report.failures.map { f -> ui("bug.failure", f.at, f.method, f.path, f.status, f.code) })
    Facts(ui("feedback.admin.trail"), report.trail)
    Facts(ui("feedback.admin.hero_sheet"), report.heroSheet)
    report.issue?.notes?.forEach { note -> MutedText("${ui("feedback.who.${note.author.name.lowercase()}")}: ${note.text}", style = MaterialTheme.typography.labelSmall) }
    ModerationNote(account, vm, report)
    if (report.issueError.isNotBlank() && report.issue == null) {
        Text(ui("feedback.issue_failed", report.issueError), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        ForgeOutlinedButton(enabled = !account.busy, onClick = { vm.reportToIssue(report.id) }) { Text(ui("feedback.issue_retry")) }
    }
    when {
        report.status == ReportStatus.CREATED -> NewReportActions(account, vm, entry, onDelete)
        report.issue?.state?.open == true -> IssueSteer(account, vm, report.id)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        report.issue?.let { issue ->
            val uri = LocalUriHandler.current
            ForgeTextButton(enabled = issue.url.startsWith("http"), onClick = { uri.openUri(issue.url) }) { Text(ui("feedback.in_issue", issue.number)) }
        }
        Spacer(Modifier.weight(1f))
        if (entry.deletable && report.status != ReportStatus.CREATED) ReportDeleteButton(!account.busy, onDelete)
    }
}

/**
 * Дополнение модерации: его пишут и правят модератор и администратор, оно уходит разделом в тело Issue; автору в игре не видно,
 * письма нет. «Сохранить» горит, когда текст разошёлся с сохранённым; пустой текст стирает дополнение.
 */
@Composable private fun ModerationNote(account: AccountUi, vm: FeedbackViewModel, report: FullReport) {
    var note by remember(report.id, report.moderationNote) { mutableStateOf(report.moderationNote) }
    Engraved(ui("feedback.note.title"))
    OutlinedTextField(note, { note = it.take(NOTE) }, placeholder = { Text(ui("feedback.note.hint")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
    ForgeOutlinedButton(enabled = !account.busy && note.trim() != report.moderationNote, onClick = { vm.noteReport(report.id, note) }) { Text(ui("feedback.note.save")) }
}

/** Блок фактов отчёта мелким шрифтом: заголовок и строки; пустой - ничего. */
@Composable private fun Facts(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Engraved(title)
    lines.forEach { MutedText(it, style = MaterialTheme.typography.labelSmall) }
}

/**
 * Решение по новому отчёту: слово модерации и «В GitHub» (Issue со словом первым комментарием) или «Отклонить» (слово -
 * причина автору); апелляция и жалоба не уходят наружу - у них «Принять» вместо «В GitHub».
 */
@Composable private fun NewReportActions(account: AccountUi, vm: FeedbackViewModel, entry: AdminReport, onDelete: () -> Unit) {
    val report = entry.report
    var word by remember(report.id) { mutableStateOf(report.reason) }
    OutlinedTextField(
        word,
        { word = it.take(REASON) },
        placeholder = { Text(ui(if (report.kind.exported) "feedback.admin.word_github" else "feedback.reason_field")) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ReportStatus.SENT in entry.moves) {
            ForgeButton(enabled = !account.busy, onClick = { vm.setReportStatus(report.id, ReportStatus.SENT, word) }) {
                Text(ui(if (report.kind.exported) "feedback.admin.to_github" else "feedback.admin.accept"))
            }
        }
        if (ReportStatus.REJECTED in entry.moves) {
            ForgeOutlinedButton(enabled = !account.busy && word.isNotBlank(), onClick = { vm.setReportStatus(report.id, ReportStatus.REJECTED, word) }) {
                Text(ui("feedback.admin.reject"), color = LifeRed)
            }
        }
        if (entry.deletable) ReportDeleteButton(!account.busy, onDelete)
    }
}

/** Виды, что уходят в GitHub (как на сервере): ошибка и падение. */
private val FeedbackKind.exported: Boolean get() = this == FeedbackKind.BUG || this == FeedbackKind.CRASH

private const val REASON = 400

/** Длина дополнения модерации: сервер всё равно режет по `inputs.report`. */
private const val NOTE = 2000
private const val SEARCH = 60

/**
 * Ведение Issue из игры: слово модерации (уходит комментарием) и действие - ответить, закрыть
 * исправленным, отказом или дублем (с номером оригинала). Судьба приходит из GitHub сразу.
 */
@Composable private fun IssueSteer(account: AccountUi, vm: FeedbackViewModel, id: String) {
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
            ForgeOutlinedButton(enabled = !account.busy && ready, onClick = { vm.steerIssue(id, move, note, original.toIntOrNull() ?: 0) }) {
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
 */
@Composable internal fun MailComposePage(account: AccountUi) {
    val index = account.index ?: return
    val vm = koinViewModel<FeedbackViewModel>()
    var login by remember { mutableStateOf(vm.takeMailTo()) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var gold by remember { mutableStateOf("") }
    val items = remember { mutableStateMapOf<String, Long>() }
    val equipment = remember { mutableStateListOf<MailEquipment>() }
    ForgePanel {
        OutlinedTextField(
            login,
            { login = it.filterNot(Char::isWhitespace).take(account.inputs.login) },
            label = { Text(ui("mail.to")) },
            supportingText = { Text(ui(if (login.isBlank()) "mail.to_all" else "mail.to_one")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
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
        var item by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("1") }
        Spinner(ui("tester.item"), item, index.items.keys.associate { it.value to itemTitle(it.value) }, !account.busy) { item = it }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                amount,
                { amount = it.filter(Char::isDigit).take(account.inputs.number) },
                label = { Text(ui("tester.amount")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            ForgeOutlinedButton(enabled = item.isNotBlank() && (amount.toLongOrNull() ?: 0) > 0, onClick = { items[item] = (items[item] ?: 0) + amount.toLong() }) {
                Text(ui("mail.add"))
            }
        }
        var template by remember { mutableStateOf("") }
        var rarity by remember { mutableStateOf<Rarity?>(null) }
        Spinner(ui("mail.template"), template, index.templates.values.associate { it.code to equipmentTitle(it.code) }, !account.busy) { template = it }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = rarity == null, onClick = { rarity = null }, label = { Text(ui("mail.template_rarity")) })
            listOf(Rarity.COMMON, Rarity.MAGIC, Rarity.RARE).forEach { r ->
                FilterChip(selected = rarity == r, onClick = { rarity = r }, label = { Text(ui("enum.rarity.${r.name}")) })
            }
        }
        ForgeOutlinedButton(enabled = template.isNotBlank(), onClick = { equipment += MailEquipment(template, rarity) }, modifier = Modifier.fillMaxWidth()) { Text(ui("mail.add")) }
        items.forEach { (code, n) -> AttachedLine("${itemTitle(code)} × $n") { items.remove(code) } }
        equipment.forEachIndexed { i, piece -> AttachedLine(equipmentTitle(piece.template) + (piece.rarity?.let { " · " + ui("enum.rarity.${it.name}") } ?: "")) { equipment.removeAt(i) } }
    }
    ForgeButton(enabled = !account.busy && subject.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
        vm.sendMail(MailRequest(login, subject, body, MailAttachment(gold.toLongOrNull() ?: 0, items.toMap(), equipment.toList())))
        subject = ""
        body = ""
        gold = ""
        items.clear()
        equipment.clear()
    }) { Text(ui(if (login.isBlank()) "mail.send_all" else "mail.send")) }
}

@Composable private fun AttachedLine(text: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Vital, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        ForgeTextButton(onClick = onRemove) { Text(ui("mail.remove"), color = LifeRed) }
    }
}
