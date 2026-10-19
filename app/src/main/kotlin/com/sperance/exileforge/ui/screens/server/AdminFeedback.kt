package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.platform.LocalUriHandler
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
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.feedback.FeedbackModel
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
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.FeedbackViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Отчёты для модерации: поиск по тексту, «#номеру» и логину, чипы очереди со счётом ([Queue]) и вида, порядок чипами ([Order]: новые,
 * рейтинг, номер Issue) - на клиенте, список вида приходит целиком. Строка на отчёт - в две линии ([ReportLine]); касание раскрывает на месте полный тикет - тот же, что у игрока
 * ([AdminTicket] над [ReportTicket]), касание его шапки сворачивает;
 * касание автора - досье модерации ([DossierView]) уровнем цепочки «назад». «Рассмотрения» нет: слово модерации к новому
 * отчёту отправляет его в GitHub сразу. Любой чип очереди и вида, и уже выбранный, и потягивание страницы перечитывают список с сервера.
 */
@Composable internal fun FeedbackAdminPage(account: AccountUi) {
    val vm = koinViewModel<FeedbackViewModel>().model
    val moderation = koinViewModel<ModerationViewModel>().model
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var queue by remember { mutableStateOf(Queue.NEW) }
    var order by remember { mutableStateOf(Order.NEWEST) }
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }
    var dossier by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.loadReports(kind, null) }
    if (dossier) {
        val state by moderation.state.collectAsStateWithLifecycle()
        TrailLevel(ui("moderation.dossier"), onBack = {
            moderation.close()
            dossier = false
        }) { DossierView(account, moderation, state.dossier) }
        return
    }
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
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MutedText(ui("feedback.order"), style = MaterialTheme.typography.labelMedium)
        Order.entries.forEach { o -> CountChip(ui(o.key), null, order == o) { order = o } }
    }
    val shown = found.filter { queue.holds(it) }.sortedWith(order.comparator)
    if (shown.isEmpty()) MutedText(ui("feedback.none"))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.forEach { entry ->
            key(entry.report.id) {
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

/** Порядок списка модерации - на клиенте: свежие первыми, по рейтингу «+ − −» или по номеру Issue (без Issue - в конце); равные - свежие первыми. */
private enum class Order(val key: String, val comparator: Comparator<AdminReport>) {
    NEWEST("feedback.order.newest", compareByDescending { it.report.createdAt }),
    RATING("feedback.order.rating", compareByDescending<AdminReport> { it.report.likes - it.report.dislikes }.thenByDescending { it.report.createdAt }),
    ISSUE("feedback.order.issue", compareByDescending<AdminReport> { it.report.issue?.number ?: 0 }.thenByDescending { it.report.createdAt }),
}

/** Где был автор, когда писал: место и раздел отчёта. */
private val FullReport.where: String get() = place.ifBlank { screen }

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

/**
 * Строка отчёта в две линии: значок вида цветом судьбы, первая строка текста и «×повторы · дата»; ниже - версия клиента, «⚠ N» падений и
 * «⛔ N» ошибок запросов (только ненулевые), «#Issue» у отправленного, «+N / −M» при голосах и логин автора; касание - [onOpen].
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
            if (report.clientVersion.isNotBlank()) VersionTag(report.clientVersion)
            if (report.crashCount > 0) Badge(ui("feedback.admin.crashes", report.crashCount), ui("feedback.admin.crashes_hint", report.crashCount), LossText)
            if (report.failureCount > 0) Badge(ui("feedback.admin.failures", report.failureCount), ui("feedback.admin.failures_hint", report.failureCount), Caution)
            report.issue?.let { Text(ui("feedback.issue_number", it.number), color = Gold, style = small, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
            if (report.likes + report.dislikes > 0) Text(ui("feedback.admin.plus_minus", report.likes, report.dislikes), color = Muted, style = small, maxLines = 1, softWrap = false)
            Text(entry.login ?: ui("feedback.anonymous"), color = Muted, style = small, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

/** Значок счёта строки: [text] цветом [tint], для чтеца экрана - [description]. */
@Composable private fun Badge(text: String, description: String, tint: Color) {
    Text(text, color = tint, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false, modifier = Modifier.semantics { contentDescription = description })
}

/**
 * Раскрытый отчёт модерации - тикет игрока ([ReportTicket]) с версией клиента в шапке и слотами модерации: строка автора ([AuthorLine],
 * касание логина - [onDossier] с аккаунтом автора), обстоятельства дела (у апелляции и жалобы), «Дополнение модерации», слово модерации у нового и ведение Issue по «Закрыть…»; подвал - голоса (если есть) и действия
 * очереди: у нового «Отклонить» (со словом - причиной автору) и «В GitHub» (апелляция и жалоба не уходят наружу - «Принять»), у
 * отправленного - «Закрыть…» (пока Issue открыт) и «Issue #N ↗»; удаление - значком, по праву. Касание шапки - [onCollapse].
 */
@Composable internal fun AdminTicket(account: AccountUi, vm: FeedbackModel, entry: AdminReport, onCollapse: () -> Unit, onDossier: (String) -> Unit) {
    val report = entry.report
    val fate = ReportFate.of(report.status, report.issue)
    val fresh = report.status == ReportStatus.CREATED
    val issue = report.issue
    var deleting by remember(report.id) { mutableStateOf(false) }
    var steering by remember(report.id) { mutableStateOf(false) }
    var word by remember(report.id) { mutableStateOf(report.reason) }
    val uri = LocalUriHandler.current
    ReportTicket(
        report.kind,
        report.text,
        fate,
        issue,
        report.createdAt,
        reportThread(issue, report.reason),
        dim = false,
        version = report.clientVersion,
        focused = true,
        onHeader = onCollapse,
        author = { AuthorLine(entry, onDossier) },
        extra = {
            if (!report.kind.exported) Circumstances(report.context)
            ModerationNote(account, vm, report)
            if (report.issueError.isNotBlank() && issue == null) {
                Text(ui("feedback.issue_failed", report.issueError), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                ForgeOutlinedButton(enabled = !account.busy, onClick = { vm.reportToIssue(report.id) }) { Text(ui("feedback.issue_retry")) }
            }
            if (fresh) {
                OutlinedTextField(
                    word,
                    { word = it.take(REASON) },
                    placeholder = { Text(ui(if (report.kind.exported) "feedback.admin.word_github" else "feedback.reason_field")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (steering && issue?.state?.open == true) IssueSteer(account, vm, report.id)
        },
    ) {
        if (report.likes + report.dislikes > 0) {
            Text(ui("feedback.admin.score", report.likes, report.dislikes), color = Muted, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false)
        }
        Row(Modifier.weight(1f).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            when {
                fresh -> {
                    if (ReportStatus.REJECTED in entry.moves) {
                        ForgeOutlinedButton(enabled = !account.busy && word.isNotBlank(), onClick = { vm.setReportStatus(report.id, ReportStatus.REJECTED, word) }) {
                            Text(ui("feedback.admin.reject"), color = LifeRed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (ReportStatus.SENT in entry.moves) {
                        ForgeButton(enabled = !account.busy, onClick = { vm.setReportStatus(report.id, ReportStatus.SENT, word) }) {
                            Text(ui(if (report.kind.exported) "feedback.admin.to_github" else "feedback.admin.accept"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                issue != null -> {
                    if (issue.state.open) ForgeOutlinedButton(enabled = !account.busy, onClick = { steering = !steering }) { Text(ui("feedback.admin.steer"), maxLines = 1) }
                    ForgeOutlinedButton(enabled = issue.url.startsWith("http"), onClick = { uri.openUri(issue.url) }) {
                        Text(ui("feedback.admin.issue_link", issue.number), color = Gold, maxLines = 1, softWrap = false)
                    }
                }
            }
            if (entry.deletable) {
                IconButton(enabled = !account.busy, onClick = { deleting = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, ui("feedback.delete"), tint = LifeRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    if (deleting) ReportDeleteConfirm(account.busy, onDismiss = { deleting = false }) { vm.deleteReport(report.id) }
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
        ForgeTextButton(onClick = {
            note = report.moderationNote
            editing = false
        }) { Text(ui("common.cancel")) }
        ForgeOutlinedButton(enabled = !account.busy && note.trim() != report.moderationNote, onClick = {
            vm.noteReport(report.id, note)
            editing = false
        }) { Text(ui("feedback.note.save")) }
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
    val vm = koinViewModel<FeedbackViewModel>().model
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
