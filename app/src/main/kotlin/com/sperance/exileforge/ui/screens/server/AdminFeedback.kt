package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.MailAttachment
import com.sperance.exileforge.core.model.feedback.MailEquipment
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The administrator's reading of players' reports (3.73.0): bugs and suggestions apart, filtered by status, each with its
 * author, where it was written and the journal tail; a new status with a word for the author, who gets a letter about it.
 */
@Composable internal fun FeedbackAdminPage(account: AccountUi) {
    val vm = koinViewModel<FeedbackViewModel>()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(FeedbackKind.BUG) }
    var status by remember { mutableStateOf<ReportStatus?>(null) }
    LaunchedEffect(kind, status) { vm.loadReports(kind, status) }
    PillTabs(FeedbackKind.entries.map(::kindTitle), kind.ordinal, { kind = FeedbackKind.entries[it] }, segmented = true)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = status == null, onClick = { status = null }, label = { Text(ui("feedback.all")) })
        ReportStatus.entries.forEach { st -> FilterChip(selected = status == st, onClick = { status = st }, label = { Text(statusTitle(st)) }) }
    }
    if (feedback.reports.isEmpty()) MutedText(ui("feedback.none"))
    feedback.reports.forEach { ReportCard(account, vm, it) }
}

@Composable private fun ReportCard(account: AccountUi, vm: FeedbackViewModel, entry: AdminReport) {
    val report = entry.report
    var open by remember(report.id) { mutableStateOf(false) }
    var reason by remember(report.id, report.reason) { mutableStateOf(report.reason) }
    var chosen by remember(report.id, report.status) { mutableStateOf(report.status) }
    ForgePanel(Modifier.clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.login ?: ui("feedback.anonymous"), color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            RoleMark(entry.role, 14.dp)
            Spacer(Modifier.weight(1f))
            StatusBadge(report.status)
        }
        Text(report.text, color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = if (open) Int.MAX_VALUE else 3)
        MutedText(
            listOfNotNull(
                report.createdAt.replace('T', ' ').take(16),
                ui("feedback.votes", report.likes.size, report.dislikes.size).takeIf { report.kind == FeedbackKind.SUGGESTION },
                report.screen.takeIf { it.isNotBlank() },
            ).joinToString(" · "),
        )
        if (open) {
            if (report.context.isNotEmpty()) {
                Engraved(ui("bug.context"))
                report.context.forEach { (key, value) -> MutedText("$key: $value", style = MaterialTheme.typography.labelSmall) }
            }
            Engraved(ui("feedback.set_status"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReportStatus.entries.forEach { st -> FilterChip(selected = chosen == st, onClick = { chosen = st }, label = { Text(statusTitle(st)) }) }
            }
            OutlinedTextField(
                reason,
                { reason = it.take(REASON) },
                label = { Text(ui("feedback.reason_field")) },
                minLines = 2,
                supportingText = { LengthCounter(reason, REASON) },
                modifier = Modifier.fillMaxWidth(),
            )
            ForgeButton(
                enabled = !account.busy && (chosen != report.status || reason != report.reason),
                onClick = { vm.setReportStatus(report.id, chosen, reason) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui("feedback.save_status")) }
            // Asana (3.75.0; сама с «В работе» с 3.88.7, без ручной кнопки с 3.89.0): отказ Asana - красной строкой с повтором, выгруженный - ссылкой.
            if (report.asanaError.isNotBlank()) {
                Text(ui("feedback.asana_failed", report.asanaError), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                ForgeOutlinedButton(enabled = !account.busy, onClick = { vm.reportToAsana(report.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("feedback.asana_retry"))
                }
            }
            if (report.asanaUrl.isNotBlank()) {
                val uri = LocalUriHandler.current
                ForgeTextButton(enabled = report.asanaUrl.startsWith("http"), onClick = { uri.openUri(report.asanaUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("feedback.in_asana"))
                }
            }
        }
    }
}

private const val REASON = 400

/**
 * The administrator's letter (3.73.0): to one account by login or, blank, to every account; a subject, a text, and what it
 * carries — gold, stacks of any item, things of any template at a chosen rarity — built line by line.
 */
@Composable internal fun MailComposePage(account: AccountUi) {
    val index = account.index ?: return
    val vm = koinViewModel<FeedbackViewModel>()
    var login by remember { mutableStateOf("") }
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
