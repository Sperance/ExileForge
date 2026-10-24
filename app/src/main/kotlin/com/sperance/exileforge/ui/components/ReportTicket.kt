package com.sperance.exileforge.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.IssueNote
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.core.model.feedback.NoteAuthor
import com.sperance.exileforge.core.model.feedback.ReportTopic
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.koinInject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Карточка отчёта - тикет-чат, один у игрока и у модерации: вид, номер Issue, судьба и дата сверху; под ними строка [author] (у модерации -
 * логин, герой ссылкой на его карточку и место); текст до трёх строк ([ReportText]: выделяется, раскрывается ссылкой); номер Issue -
 * касанием в буфер обмена; [aside] - место справа от текста (снимок экрана отчёта, [ReportShotAside]; текст рядом с ним свёрнут до
 * [ASIDE_LINES] строк); [extra] - своё место под текстом (у модерации - обстоятельства
 * дела и «Дополнение модерации»); переписка пузырями ([ThreadChat]: модерация и разработчик слева, автор справа - у игрока «Вы», у
 * модерации его логин [authorName]; [newReply] - последний ответ автора помечен «новый ответ»); [below] - место под перепиской (у игрока
 * поле ответа и «Закрыть отчёт», у модерации - поле комментария и решения); [version] - версия клиента плашкой в шапке (у модерации),
 * пусто - нет; подвал - действия места, а с [issueLink] (по умолчанию - [LocalIssueLinks]) перед ними - «Issue #N ↗»
 * ([IssueLink]). Свой отчёт, известная проблема и отчёт модерации - одна карточка, разное - в слотах. [dim] - приглушена; [focused] -
 * отчёт, ради которого открыт лист (у модерации - раскрытый): рамка `GoldBright`; [onHeader] - касание шапки (у модерации сворачивает
 * тикет в строку); [topic] - тема ошибки: у улучшения в шапке подпись «Идея».
 */
@Composable internal fun ReportTicket(
    kind: FeedbackKind,
    text: String,
    fate: ReportFate,
    issue: IssueView?,
    createdAt: String,
    thread: List<IssueNote>,
    dim: Boolean,
    version: String = "",
    fresh: Boolean = false,
    focused: Boolean = false,
    authorName: String? = null,
    newReply: Boolean = false,
    onHeader: (() -> Unit)? = null,
    author: (@Composable () -> Unit)? = null,
    aside: (@Composable () -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    below: (@Composable ColumnScope.() -> Unit)? = null,
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
            issue?.let { CopiedNumber(ui("feedback.issue_number", it.number)) }
            FatePill(fate, kind, issue, fresh)
            if (version.isNotBlank()) VersionTag(version)
            Spacer(Modifier.weight(1f))
            Text(reportDate(createdAt), color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        author?.let { Box(Modifier.padding(horizontal = 12.dp).padding(top = 4.dp)) { it() } }
        val textPadding = Modifier.padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 10.dp)
        if (aside == null) {
            ReportText(text, TEXT_LINES, textPadding)
        } else {
            Row(textPadding, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportText(text, ASIDE_LINES, Modifier.weight(1f))
                aside()
            }
        }
        extra?.let { Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = it) }
        if (thread.isNotEmpty()) ThreadChat(thread, authorName, newReply)
        below?.let { Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = it) }
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

/**
 * Текст отчёта: выделяется ([SelectionContainer]: долгое нажатие выделяет и даёт «Копировать»), свёрнут до [lines] строк; раскрытие -
 * отдельной ссылкой «Развернуть» под текстом, а не касанием текста: касание и выделение не спорят.
 */
@Composable private fun ReportText(text: String, lines: Int, modifier: Modifier) {
    var open by remember(text) { mutableStateOf(false) }
    var clipped by remember(text) { mutableStateOf(false) }
    Column(modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SelectionContainer {
            Text(
                text,
                color = Parchment,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (open) Int.MAX_VALUE else lines,
                overflow = if (open) TextOverflow.Clip else TextOverflow.Ellipsis,
                onTextLayout = { if (!open) clipped = it.hasVisualOverflow },
            )
        }
        if (open || clipped) {
            Text(
                ui(if (open) "feedback.collapse" else "feedback.expand"),
                color = Gold,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable(role = Role.Button) { open = !open }.padding(vertical = 2.dp),
            )
        }
    }
}

/** Номер отчёта или Issue в шапке: касание кладёт его в буфер обмена и говорит «Скопировано». */
@Composable private fun CopiedNumber(number: String) {
    val clipboard = LocalClipboardManager.current
    val notices = koinInject<Notices>()
    Text(
        number,
        color = Gold,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = ui("tester.copy")) {
            clipboard.setText(AnnotatedString(number))
            notices.toast(ui("common.copied"))
        },
    )
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
 * Переписка пузырями, как в мессенджере: модерация и разработчик - слева, автор - справа; пузырь не шире [BUBBLE_SHARE] тикета,
 * подпись - кто (у автора - [authorName], без него «Вы»), время записи - мелко внизу справа. [newReply] - последний ответ автора
 * обведён руной и подписан «новый ответ» (модерации ждёт его ответ).
 */
@Composable private fun ThreadChat(thread: List<IssueNote>, authorName: String?, newReply: Boolean) {
    val last = if (newReply) thread.indexOfLast { it.author == NoteAuthor.AUTHOR } else NO_REPLY
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        thread.forEachIndexed { i, note -> Bubble(note, authorName, fresh = i == last) }
    }
}

/**
 * Пузырь записи [note]: цвет и сторона - по автору, [fresh] - новый ответ автора. Полоса своей стороны - [BUBBLE_SHARE] ширины
 * тикета, пузырь в ней - по тексту, прижат к своему краю.
 */
@Composable private fun Bubble(note: IssueNote, authorName: String?, fresh: Boolean) {
    val mine = note.author == NoteAuthor.AUTHOR
    val tint = when (note.author) {
        NoteAuthor.DEVELOPER -> Rune
        NoteAuthor.MODERATION -> Gold
        NoteAuthor.AUTHOR -> if (fresh) Rune else Seam
    }
    val shape = if (mine) MineShape else TheirsShape
    val who = when {
        !mine -> ui("feedback.who.${note.author.name.lowercase()}")
        authorName == null -> ui("feedback.who.author")
        else -> authorName
    }
    val side = if (mine) Alignment.CenterEnd else Alignment.CenterStart
    Box(Modifier.fillMaxWidth(), contentAlignment = side) {
        Box(Modifier.fillMaxWidth(BUBBLE_SHARE), contentAlignment = side) { BubbleBody(note, who, mine, fresh, tint, shape) }
    }
}

/**
 * Тело пузыря: подпись [who], текст (выделяется, долгое нажатие - «Копировать») и время записи; рамка и подложка - цветом [tint] по стороне
 * [mine].
 */
@Composable private fun BubbleBody(note: IssueNote, who: String, mine: Boolean, fresh: Boolean, tint: Color, shape: Shape) {
    Column(
        Modifier.clip(shape).background(if (mine) PanelRaised else tint.copy(alpha = .10f)).border(1.dp, tint.copy(alpha = if (mine && !fresh) 1f else .45f), shape)
            .padding(start = 11.dp, end = 11.dp, top = 7.dp, bottom = 5.dp),
    ) {
        Text(
            if (fresh) ui("feedback.who.fresh", who) else who,
            color = if (mine && !fresh) {
                Parchment
            } else if (note.author == NoteAuthor.MODERATION) {
                Vital
            } else {
                tint
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SelectionContainer { Text(note.text, color = Parchment, style = MaterialTheme.typography.bodySmall) }
        remember(note.at) { noteTime(note.at) }.takeIf { it.isNotEmpty() }?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), modifier = Modifier.align(Alignment.End)) }
    }
}

/** Время записи переписки из ISO-мгновения сервера или GitHub по часам устройства («17.10 21:18»); не разобралось - пусто. */
private fun noteTime(at: String): String = if (at.isBlank()) "" else runCatching { Instant.parse(at).atZone(ZoneId.systemDefault()).format(NOTE_TIME) }.getOrDefault("")

private val NOTE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

/** Пузыри: хвост - уголок к своей стороне. */
private val MineShape = RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)
private val TheirsShape = RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp)

/** Доля ширины тикета, шире которой пузырь не растёт. */
private const val BUBBLE_SHARE = .86f

/** В переписке нет нового ответа автора. */
private const val NO_REPLY = -1

/** Видна ли в тикетах игрока ссылка «Issue #N ↗»: лист отчётов задаёт её по [com.sperance.exileforge.core.app.GameUi.seesIssues]. */
internal val LocalIssueLinks = staticCompositionLocalOf { false }

/** «Issue #N ↗» - Issue отчёта на GitHub во внешнем браузере; пока у Issue нет адреса, погашена. */
@Composable private fun IssueLink(issue: IssueView) {
    val uri = LocalUriHandler.current
    ForgeButton(state = ButtonState.of(issue.hasUrl), onClick = { uri.openUri(issue.url) }, kind = ButtonKind.Secondary, size = ButtonSize.Small, accent = Gold) {
        Text(ui("feedback.issue_link", issue.number), maxLines = 1, softWrap = false)
    }
}

/**
 * Поле ответа автора под перепиской - не длиннее [limit] (`inputs.report` правил): поле и кнопка «Отправить» - [onSend] получает текст; с Issue он уходит его комментарием,
 * до Issue копится в отчёте - и встаёт в переписку. [enabled] - нет, пока идёт загрузка: поле гаснет, но остаётся на месте.
 */
@Composable internal fun AnswerField(enabled: Boolean, limit: Int, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(text, { text = it.take(limit) }, enabled = enabled, placeholder = { Text(ui("feedback.answer")) }, maxLines = 4, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f))
        ForgeIconButton(
            Icons.AutoMirrored.Outlined.Send,
            ui("feedback.answer_send"),
            onClick = {
                onSend(text.trim())
                text = ""
            },
            kind = ButtonKind.Primary,
            state = ButtonState.of(enabled && text.isNotBlank()),
        )
    }
}

/** Значок вида отчёта цветом [tint] на тёмной плашке: ошибка - жук, падение - молния, жалоба - конверт, апелляция - весы. */
@Composable internal fun ReportKindMark(kind: FeedbackKind, tint: Color = Muted) {
    Box(Modifier.size(24.dp).background(tint.copy(alpha = .12f), RoundedCornerShape(6.dp)).border(1.dp, tint.copy(alpha = .4f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        Icon(kind.icon, kindTitle(kind), tint = tint, modifier = Modifier.size(15.dp))
    }
}

/** Вид отчёта значком и словом своего цвета ([tint]) - без подложки: строка очереди модерации, где все виды вместе. */
@Composable internal fun ReportKindLabel(kind: FeedbackKind) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(kind.icon, null, tint = kind.tint, modifier = Modifier.size(13.dp))
        Text(kindTitle(kind), color = kind.tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}

/** Цвет вида отчёта: ошибка - серый, падение - красный, модерация (апелляция) - бронзовый, жалоба - фиолетовый. */
internal val FeedbackKind.tint: Color
    get() = when (this) {
        FeedbackKind.BUG -> Quiet
        FeedbackKind.CRASH -> LossText
        FeedbackKind.APPEAL -> Brass
        FeedbackKind.COMPLAINT -> Elder
    }

/** Значок вида отчёта. */
private val FeedbackKind.icon: ImageVector
    get() = when (this) {
        FeedbackKind.BUG -> Icons.Outlined.BugReport
        FeedbackKind.CRASH -> Icons.Outlined.FlashOn
        FeedbackKind.COMPLAINT -> Icons.Outlined.MailOutline
        FeedbackKind.APPEAL -> Icons.Outlined.Balance
    }

/** Сколько строк текста отчёта видно свёрнутым: касание раскрывает его на месте. */
private const val TEXT_LINES = 3

/** Сколько строк текста видно свёрнутым рядом со снимком экрана: снимок выше трёх строк. */
private const val ASIDE_LINES = 7

/** Закрытые свои отчёты приглушены. */
private const val DIM = .6f
