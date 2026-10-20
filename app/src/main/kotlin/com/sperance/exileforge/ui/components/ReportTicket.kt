package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.core.model.feedback.NoteAuthor
import com.sperance.exileforge.core.model.feedback.ReportTopic
import com.sperance.exileforge.ui.theme.*

/**
 * Карточка отчёта - тикет, один у игрока и у модерации: вид, номер Issue, судьба и дата сверху; под ними строка [author] (у модерации -
 * логин, герой ссылкой на его карточку и место); текст до трёх строк; переписка цитатами;
 * ответ автора; [version] - версия клиента плашкой в шапке (у модерации), пусто - нет; [extra] - своё место (у модерации - «Дополнение модерации», слово модерации, ведение Issue); подвал - голоса и действия
 * места, а с [issueLink] (по умолчанию - [LocalIssueLinks]) перед ними - «Issue #N ↗» ([IssueLink]). Свой отчёт, известная проблема и отчёт модерации - одна карточка, разное - в слотах. [dim] -
 * приглушена; [focused] - отчёт, ради которого открыт лист (у модерации - раскрытый): рамка `GoldBright`; [onHeader] - касание шапки
 * (у модерации сворачивает тикет в строку); [topic] - тема ошибки: у улучшения в шапке подпись «Идея».
 */
@Composable internal fun ReportTicket(
    kind: FeedbackKind,
    text: String,
    fate: ReportFate,
    issue: IssueView?,
    createdAt: String,
    thread: List<Pair<NoteAuthor, String>>,
    dim: Boolean,
    version: String = "",
    fresh: Boolean = false,
    focused: Boolean = false,
    onAnswer: ((String) -> Unit)? = null,
    onHeader: (() -> Unit)? = null,
    author: (@Composable () -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    topic: ReportTopic = ReportTopic.ERROR,
    issueLink: Boolean = LocalIssueLinks.current,
    footer: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().alpha(if (dim) DIM else 1f).clip(shape).background(Panel).border(if (focused) 2.dp else 1.dp, if (focused) GoldBright else Bronze, shape)) {
        Row(
            Modifier.fillMaxWidth().then(onHeader?.let { Modifier.clickable(onClick = it) } ?: Modifier).padding(start = 12.dp, end = 12.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportKindMark(kind, fate.tint)
            if (topic == ReportTopic.IMPROVEMENT) Text(ui("feedback.idea"), color = Rune, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            issue?.let { Text(ui("feedback.issue_number", it.number), color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
            FatePill(fate, kind, issue, fresh)
            if (version.isNotBlank()) VersionTag(version)
            Spacer(Modifier.weight(1f))
            Text(reportDate(createdAt), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        author?.let { Box(Modifier.padding(horizontal = 12.dp).padding(top = 4.dp)) { it() } }
        ExpandableText(text, Modifier.padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 10.dp), color = Parchment, style = MaterialTheme.typography.bodyMedium, folded = TEXT_LINES, more = ui("feedback.read_all"))
        thread.forEach { (who, words) -> ThreadNote(who, words) }
        onAnswer?.let { send -> AnswerField(send) }
        extra?.let { Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = it) }
        Row(
            Modifier.fillMaxWidth().background(PanelRaised).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (issueLink) issue?.let { IssueLink(it) }
            footer()
        }
    }
}

/** Версия клиента отчёта мелкой плашкой в нити: шапка тикета модерации и строка списка. */
@Composable internal fun VersionTag(version: String) {
    val shape = RoundedCornerShape(5.dp)
    Text(
        version,
        color = Parchment,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.border(1.dp, Seam, shape).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** День и месяц отчёта из ISO-времени сервера («2026-10-15T13:06:30» → «15.10»); не разобралось - пусто. */
internal fun reportDate(createdAt: String): String = createdAt.take(10).split('-').takeIf { it.size == 3 }?.let { (_, month, day) -> "$day.$month" }.orEmpty()

/**
 * Переписка карточки: слово модерации ([reason] - сервер шлёт его, только пока у отчёта нет Issue), затем вся переписка Issue по
 * порядку - разработчик, модерация из игры и автор; с Issue слово модерации - его комментарий, и дважды оно не встаёт.
 */
internal fun reportThread(issue: IssueView?, reason: String): List<Pair<NoteAuthor, String>> = listOfNotNull(reason.takeIf { it.isNotBlank() }?.let { NoteAuthor.MODERATION to it }) + issue?.notes.orEmpty().map { it.author to it.text }

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

/** Видна ли в тикетах игрока ссылка «Issue #N ↗»: лист отчётов задаёт её по [com.sperance.exileforge.core.app.GameUi.seesIssues]. */
internal val LocalIssueLinks = staticCompositionLocalOf { false }

/** «Issue #N ↗» - Issue отчёта на GitHub во внешнем браузере; пока у Issue нет адреса, погашена. */
@Composable private fun IssueLink(issue: IssueView) {
    val uri = LocalUriHandler.current
    ForgeButton(state = ButtonState.of(issue.hasUrl), onClick = { uri.openUri(issue.url) }, kind = ButtonKind.Secondary, size = ButtonSize.Small, accent = Gold) {
        Text(ui("feedback.issue_link", issue.number), maxLines = 1, softWrap = false)
    }
}

/** Ответ автора в Issue: поле и «Отправить»; уходит комментарием в GitHub и встаёт в переписку. */
@Composable private fun AnswerField(onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(text, { text = it.take(ANSWER_MAX) }, placeholder = { Text(ui("feedback.answer")) }, maxLines = 4, modifier = Modifier.weight(1f))
        ForgeButton(
            state = ButtonState.of(text.isNotBlank()),
            onClick = {
                onSend(text.trim())
                text = ""
            },
            kind = ButtonKind.Text,
        ) { Text(ui("feedback.answer_send"), style = MaterialTheme.typography.labelMedium) }
    }
}

/** Значок вида отчёта цветом [tint] на тёмной плашке: ошибка - жук, падение - молния, жалоба - конверт, апелляция - весы. */
@Composable internal fun ReportKindMark(kind: FeedbackKind, tint: Color = Muted) {
    Box(Modifier.size(24.dp).background(tint.copy(alpha = .12f), RoundedCornerShape(6.dp)).border(1.dp, tint.copy(alpha = .4f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        Icon(kind.icon, kindTitle(kind), tint = tint, modifier = Modifier.size(15.dp))
    }
}

/** Значок вида отчёта. */
private val FeedbackKind.icon: ImageVector
    get() = when (this) {
        FeedbackKind.BUG -> Icons.Outlined.BugReport
        FeedbackKind.CRASH -> Icons.Outlined.FlashOn
        FeedbackKind.COMPLAINT -> Icons.Outlined.MailOutline
        FeedbackKind.APPEAL -> Icons.Outlined.Balance
    }

/** Длина ответа автора в Issue из игры: сервер всё равно режет по `inputs.report`. */
private const val ANSWER_MAX = 1000

/** Сколько строк текста отчёта видно свёрнутым: касание раскрывает его на месте. */
private const val TEXT_LINES = 3

/** Закрытые свои отчёты приглушены. */
private const val DIM = .6f
