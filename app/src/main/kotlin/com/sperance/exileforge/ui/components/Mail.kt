package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.feedback.Feedback
import com.sperance.exileforge.core.feedback.LetterDraft
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.IgnoredHero
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind
import com.sperance.exileforge.core.model.feedback.MailMute
import com.sperance.exileforge.core.model.feedback.MailQuota
import com.sperance.exileforge.presentation.feedback.FeedbackViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.MailGate
import com.sperance.exileforge.rules.content.MailRules
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.screens.guild.GuildEmblem
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/*
 * Почта (4.6.3, утверждён макет «Свиток писем», вариант A): нынешний лист почты с вкладками, письмо - листом поверх, новое
 * письмо - своим листом. Письма героев видны лишь адресату - ящик читается глазами героя в игре.
 */

/** A letter's subject and body in the player's language: a system letter is the dictionary's, the administrator's as written. */
fun mailSubject(mail: Mail): String = if (mail.kind == MailKind.SYSTEM) systemLine(mail, "subject") else mail.subject
fun mailBody(mail: Mail): String = if (mail.kind == MailKind.SYSTEM) {
    listOf(
        systemLine(mail, "body"),
        mail.args.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { loc("${mail.key}.reason", listOf(it)) },
    ).filterNotNull().joinToString("\n\n")
} else {
    mail.body
}

/**
 * `mail.feedback_status`: its args are the kind, the status, an excerpt of the report and the administrator's word. The
 * auction's letters (3.79.0, server 1.74.0) name the lot's item by its code and its amount.
 */
private fun systemLine(mail: Mail, part: String): String {
    if (mail.key in AUCTION_MAIL) return loc("${mail.key}.$part", listOf(mail.args.getOrNull(0)?.let(::lotItemTitle).orEmpty(), mail.args.getOrNull(1).orEmpty()))
    val kind = mail.args.getOrNull(0)?.takeIf { it.isNotBlank() }?.let(::kindTitle).orEmpty()
    val status = mail.args.getOrNull(1)?.takeIf { it.isNotBlank() }?.let(::statusTitle).orEmpty()
    return when (part) {
        "subject" -> loc("${mail.key}.subject", listOf(kind))
        else -> loc("${mail.key}.body", listOf(status, mail.args.getOrNull(2).orEmpty()))
    }
}

/** What a letter carries, a line each: gold, stacks, things - с карточкой, которую строка открывает (4.3.2); у золота и питомца её нет. */
fun attachmentLines(mail: Mail): List<Pair<String, Inspect?>> = buildList {
    if (mail.attachment.gold > 0) add(ui("mail.gold", mail.attachment.gold) to null)
    mail.attachment.items.forEach { (code, amount) -> add("${itemTitle(code)} × $amount" to Inspect.Stack(code)) }
    mail.attachment.instances.forEach { add(equipmentTitle(it.template) + " · " + ui("enum.rarity.${it.rarity.name}") to Inspect.Copy(it)) }
    mail.attachment.equipment.forEach { add(equipmentTitle(it.template) + (it.rarity?.let { r -> " · " + ui("enum.rarity.${r.name}") } ?: "") to Inspect.Showcase(it.template)) }
    mail.attachment.pets.forEach { add(locOr("pet.${it.species}", it.species) + " · " + ui("enum.rarity.${it.rarity.name}") to null) }
}

/** Вкладка ящика (4.6.3): какие письма она держит. Новая вкладка - запись перечисления, не ветка экрана. */
internal enum class MailBox(val holds: (MailKind) -> Boolean) {
    ALL({ true }),
    PERSONAL({ it == MailKind.PLAYER }),
    GUILD({ it == MailKind.GUILD }),
    SYSTEM({ !it.written }),
    ;

    fun title(): String = when (this) {
        ALL -> ui("mail.tab.all")
        PERSONAL -> ui("mail.tab.personal")
        GUILD -> ui("mail.tab.guild")
        SYSTEM -> ui("mail.tab.system")
    }
}

/** Кто написал письмо - строкой: герой, «[TAG] Гильдия · автор», игра или администратор. */
internal fun senderLine(mail: Mail): String {
    val from = mail.from
    val guild = from?.guild
    return when {
        guild != null && mail.kind == MailKind.GUILD -> ui("mail.from_guild", guild.tag, from.name)
        from != null -> from.name
        mail.kind == MailKind.ADMIN -> ui("mail.sender.admin")
        else -> ui("mail.sender.game")
    }
}

/** Сколько целых дней письму осталось; меньше суток - 0. */
private fun daysLeft(mail: Mail, now: Long = System.currentTimeMillis()): Long = (mail.expiresAt - now).coerceAtLeast(0) / DAY_MS

/** «сгорит через N дн.» или «сгорит сегодня». */
internal fun burnText(mail: Mail): String = daysLeft(mail).let { if (it <= 0) ui("mail.burns_today") else ui("mail.burns_in", it) }

/**
 * Знак отправителя слева у письма: портрет класса героя, герб гильдии рассылки или руна игры. Один знак на список, письмо и
 * плашку «Кому».
 */
@Composable internal fun SenderMark(game: GameUi, mail: Mail, size: Dp = MARK) {
    val from = mail.from
    val guild = from?.guild
    when {
        guild != null && mail.kind == MailKind.GUILD -> GuildEmblem(guild.emblem, guild.color, size)
        from != null -> ClassPortrait(from.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(size), round = true, ring = Bronze)
        else -> RuneMark(size)
    }
}

/** Руна игры и администратора: круг в нити руны. */
@Composable private fun RuneMark(size: Dp) {
    Box(Modifier.size(size).border(1.dp, Rune, CircleShape), contentAlignment = Alignment.Center) {
        Text(RUNE, color = Rune, fontSize = (size.value * .42f).sp)
    }
}

/**
 * Ящик (3.73.0; 4.6.3 - «Свиток писем»): вкладки Все / Личные / Гильдия / Система со счётом непрочитанного, письмо строкой со
 * знаком отправителя, темой, первой строкой и сроком; касание открывает письмо листом поверх. «Прочитать все» и «Удалить
 * прочитанные» - разом (3.94.0); список игнора - тут же.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailSheet(game: GameUi, onDismiss: () -> Unit) {
    val model = koinViewModel<FeedbackViewModel>()
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId) {
        model.loadMail()
        model.letters.loadIgnored()
    }
    var open by remember { mutableStateOf<String?>(null) }
    var box by remember { mutableStateOf(MailBox.ALL) }
    var ignoring by remember { mutableStateOf(false) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ui("mail.title"), style = relicName(19).copy(color = GoldBright), modifier = Modifier.weight(1f))
                ForgeTextButton(onClick = { ignoring = !ignoring }) {
                    Text(if (ignoring) ui("mail.back") else ui("mail.ignore.list", feedback.ignored.size), color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (ignoring) {
                IgnoreList(feedback, activity.busy) { hero -> model.letters.ignore(hero.heroId, hero.name, on = false) }
                return@Column
            }
            MutedText(ui("mail.keep", game.index?.rules?.mail?.keepDays ?: MailRules().keepDays))
            MailActionsRow(model, feedback, activity.busy)
            MailTabs(feedback.mail, box) { box = it }
            val shown = feedback.mail.filter { box.holds(it.kind) }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (shown.isEmpty()) item { MutedText(ui("mail.empty"), Modifier.padding(vertical = 12.dp)) }
                items(shown, key = { it.id }) { mail ->
                    MailRow(game, mail) {
                        open = mail.id
                        if (!mail.read) model.readMail(mail.id)
                    }
                }
            }
        }
    }
    feedback.mail.firstOrNull { it.id == open }?.let { letter -> LetterSheet(game, model, feedback, activity.busy, letter) { open = null } }
}

/** «Прочитать все» и «Удалить прочитанные» (3.94.0) - с вопросом, сколько уйдёт; письмо с вложением не удаляется. */
@Composable private fun MailActionsRow(model: FeedbackViewModel, feedback: Feedback, busy: Boolean) {
    var purging by remember { mutableStateOf(false) }
    val unread = feedback.mail.count { !it.read }
    val removable = feedback.mail.count { it.read && !it.claimable }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeOutlinedButton(onClick = model::readAllMail, enabled = !busy && unread > 0, modifier = Modifier.weight(1f)) {
            Text(ui("mail.read_all", unread), style = MaterialTheme.typography.labelMedium)
        }
        ForgeOutlinedButton(onClick = { purging = true }, enabled = !busy && removable > 0, modifier = Modifier.weight(1f)) {
            Text(ui("mail.delete_read", removable), style = MaterialTheme.typography.labelMedium)
        }
    }
    if (purging) {
        ConfirmSheet(ui("mail.delete_read_title", removable), ui("mail.delete_read_confirm"), onDismiss = { purging = false }, note = ui("mail.delete_read_note"), danger = true) {
            purging = false
            model.deleteReadMail()
        }
    }
}

/** Вкладки ящика: подчёркнута выбранная, у каждой - счёт непрочитанного плашкой. */
@Composable private fun MailTabs(mail: List<Mail>, chosen: MailBox, onChoose: (MailBox) -> Unit) {
    Row(
        Modifier.fillMaxWidth().drawBehind { drawLine(Bronze, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MailBox.entries.forEach { box ->
            val on = box == chosen
            val unread = mail.count { !it.read && box.holds(it.kind) }
            Row(
                Modifier.clickable { onChoose(box) }
                    .drawBehind { if (on) drawLine(Gold, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx()) }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(box.title(), color = if (on) GoldBright else Muted, style = MaterialTheme.typography.labelMedium)
                if (unread > 0) {
                    Text(
                        unread.toString(),
                        color = Ink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Gold, RoundedCornerShape(8.dp)).padding(horizontal = 5.dp),
                    )
                }
            }
        }
    }
}

/** Строка письма: знак отправителя, кто (у непрочитанного - точка), тема, первая строка; справа время и срок. */
@Composable private fun MailRow(game: GameUi, mail: Mail, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SenderMark(game, mail)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!mail.read) Box(Modifier.size(7.dp).background(Gold, CircleShape))
                    Text(
                        senderLine(mail),
                        color = if (mail.read) Muted else GoldBright,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (mail.read) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(mailSubject(mail), color = if (mail.read) Muted else Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(mailBody(mail).lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(listedAt(mail.createdAt)?.take(SHORT_STAMP).orEmpty(), color = Muted, style = MaterialTheme.typography.labelSmall)
                if (mail.kind.written) Text(ui("mail.days_left", daysLeft(mail).coerceAtLeast(0)), color = Caution, fontSize = 10.sp)
                if (mail.claimable) Text(ui("mail.has_gift"), color = Vital, style = MaterialTheme.typography.labelSmall)
            }
        }
        HorizontalDivider(color = Bronze.copy(alpha = .47f), thickness = 1.dp)
    }
}

/**
 * Письмо листом поверх ящика: отправитель (имя, класс, уровень, время, срок), тема серифом, текст, цитата ответа, вложение.
 * «Ответить» - главная; ниже «Удалить», «Игнор» и «Жалоба» - у писем героев.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LetterSheet(game: GameUi, model: FeedbackViewModel, feedback: Feedback, busy: Boolean, mail: Mail, onClose: () -> Unit) {
    var reporting by remember { mutableStateOf(false) }
    val from = mail.from
    val ignored = from != null && feedback.ignored.any { it.heroId == from.heroId }
    ForgeSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PanelRaised, Panel)), shape).border(1.dp, Bronze, shape).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SenderMark(game, mail)
                    Column(Modifier.weight(1f)) {
                        Text(senderLine(mail), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val about = listOfNotNull(
                            from?.let { classTitle(it.heroClass) },
                            from?.level?.toString(),
                            listedAt(mail.createdAt),
                            burnText(mail).takeIf { mail.kind.written },
                        )
                        MutedText(about.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(mailSubject(mail), style = relicName(16).copy(color = GoldBright), modifier = Modifier.padding(top = 4.dp))
                Text(mailBody(mail), color = Parchment, fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 22.sp)
                if (mail.quote.isNotBlank()) Quote(ui("mail.quote_of_mine"), mail.quote)
                Attachment(game, model, busy, mail)
            }
            if (mail.answerable) {
                ForgeButton(onClick = {
                    onClose()
                    model.letters.reply(mail, game.index?.rules?.mail?.subject ?: MailRules().subject)
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(ui("mail.reply")) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ForgeOutlinedButton(onClick = {
                    model.deleteMail(mail.id)
                    onClose()
                }, enabled = !busy, modifier = Modifier.weight(1f)) { Text(ui("mail.delete"), color = Muted) }
                if (from != null && mail.answerable) {
                    ForgeOutlinedButton(onClick = { model.letters.ignore(from.heroId, from.name, on = !ignored) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(ui(if (ignored) "mail.ignore.off" else "mail.ignore.on"), color = Muted)
                    }
                    ForgeOutlinedButton(onClick = { reporting = true }, enabled = !busy && !mail.reported, modifier = Modifier.weight(1f)) {
                        Text(ui(if (mail.reported) "mail.report.done" else "mail.report"), color = LifeRed)
                    }
                }
            }
        }
    }
    if (reporting) {
        ConfirmSheet(ui("mail.report.title"), ui("mail.report"), onDismiss = { reporting = false }, note = ui("mail.report.note"), danger = true) {
            reporting = false
            model.letters.report(mail.id)
        }
    }
}

/** Вложение письма: строки с карточкой вещи и «Забрать» героем в игре, один раз. */
@Composable private fun Attachment(game: GameUi, model: FeedbackViewModel, busy: Boolean, mail: Mail) {
    val lines = attachmentLines(mail)
    if (lines.isEmpty()) return
    Engraved(ui("mail.attachment"))
    val inspect = rememberInspect()
    lines.forEach { (line, card) ->
        Text(line, color = Vital, style = MaterialTheme.typography.labelLarge, modifier = card?.let { Modifier.clickable { inspect(it) } } ?: Modifier)
    }
    if (mail.claimable) {
        ForgeButton(enabled = !busy && game.heroId.isNotBlank(), onClick = { model.claimMail(mail.id, game.heroId) }, modifier = Modifier.fillMaxWidth()) {
            Text(ui("mail.claim", game.heroName))
        }
    } else {
        MutedText(ui("mail.claimed_already"))
    }
}

/** Цитата ответа: бронзовая черта слева, приглушённый текст. */
@Composable private fun Quote(author: String, text: String) {
    Text(
        ui("mail.quote", author, text),
        color = Muted,
        style = MaterialTheme.typography.bodySmall,
        maxLines = QUOTE_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().drawBehind { drawLine(Bronze, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx()) }.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
    )
}

/** Список игнора: чьи письма герой не принимает; «Снять» возвращает их. */
@Composable private fun IgnoreList(feedback: Feedback, busy: Boolean, onLift: (IgnoredHero) -> Unit) {
    MutedText(ui("mail.ignore.note"))
    if (feedback.ignored.isEmpty()) MutedText(ui("mail.ignore.empty"), Modifier.padding(vertical = 12.dp))
    feedback.ignored.forEach { hero ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(hero.name, color = Parchment, style = MaterialTheme.typography.labelLarge)
                MutedText(classTitle(hero.heroClass), style = MaterialTheme.typography.labelSmall)
            }
            ForgeTextButton(onClick = { onLift(hero) }, enabled = !busy) { Text(ui("mail.ignore.lift"), color = Gold) }
        }
        HorizontalDivider(color = Bronze.copy(alpha = .47f), thickness = 1.dp)
    }
}

/**
 * Хозяин окна нового письма (4.6.3): черновик в почте ([Feedback.letter]) открывает лист поверх всего - из карточки игрока,
 * «Ответить» и рассылки гильдии. Кому - плашка героя или гильдии, не ввод; тема и текст со счётчиками, цитата ответа, квота
 * «Сегодня X из N · этому герою Y из M»; немота - причина и срок сверху, кнопка погашена.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterHost(game: GameUi) {
    val model = koinViewModel<FeedbackViewModel>()
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val draft = feedback.letter ?: return
    val rules = game.index?.rules?.mail ?: MailRules()
    var subject by remember(draft) { mutableStateOf(draft.subject) }
    var body by remember(draft) { mutableStateOf("") }
    val quota = feedback.quota
    val now by produceState(System.currentTimeMillis(), quota?.readyAt) {
        while ((quota?.readyAt ?: 0) > value) {
            delay(TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    ForgeSheet(onDismissRequest = model.letters::close) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui(if (draft.kind == MailKind.GUILD) "mail.letter.broadcast_title" else "mail.letter.title"), style = relicName(19).copy(color = GoldBright))
            quota?.mute?.let { MuteNote(it) }
            Field(ui("mail.letter.to")) { Addressee(game, draft, quota) }
            OutlinedTextField(
                subject,
                { subject = it.take(rules.subject) },
                label = { Text(ui("mail.subject")) },
                singleLine = true,
                supportingText = { LengthCounter(subject, rules.subject) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                body,
                { body = it.take(rules.body) },
                label = { Text(ui("mail.body")) },
                minLines = 4,
                supportingText = { LengthCounter(body, rules.body) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (draft.quote.isNotBlank()) Quote(draft.quoteBy, draft.quote.take(rules.quote))
            val gate = quota?.let { effectiveGate(it, draft) }
            quota?.let { QuotaBar(it, draft) }
            gate?.takeIf { it != MailGate.OPEN && it != MailGate.MUTED }?.let { Text(gateText(it, quota, rules), color = Caution, style = MaterialTheme.typography.labelMedium) }
            val wait = quota?.let { ((it.readyAt - now + SECOND_MS - 1) / SECOND_MS).coerceAtLeast(0L) } ?: 0L
            if (wait > 0) MutedText(ui("mail.letter.cooldown", wait))
            val guildFull = draft.kind == MailKind.GUILD && quota?.guild?.let { it.sent >= it.perDay } != false
            ForgeButton(
                onClick = { model.letters.send(subject, body) },
                enabled = !activity.busy && gate == MailGate.OPEN && wait == 0L && !guildFull && subject.isNotBlank() && body.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui("mail.send")) }
        }
    }
}

/** Ответ на рассылку гильдии пишут и до уровня права писать (как сервер): уровень ему не помеха. */
private fun effectiveGate(quota: MailQuota, draft: LetterDraft): MailGate = if (quota.gate == MailGate.LEVEL && draft.early) MailGate.OPEN else quota.gate

/** Немота: писать нельзя до срока (или навсегда), причина и слово модератора. */
@Composable private fun MuteNote(mute: MailMute) {
    val term = mute.until?.let { listedAt(it) }?.let { ui("mail.mute.until", it) } ?: ui("mail.mute.forever")
    val reason = listOf(ui("moderation.category.${mute.category}"), mute.comment).filter { it.isNotBlank() }.joinToString(" · ")
    Text(
        ui("mail.mute.note", term, reason, mute.number),
        color = LifeRed.copy(alpha = .85f),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().background(LifeRed.copy(alpha = .08f), RoundedCornerShape(10.dp)).border(1.dp, LifeRed.copy(alpha = .33f), RoundedCornerShape(10.dp)).padding(10.dp),
    )
}

/** Поле окна письма: подпись мелко сверху, содержимое в панели. */
@Composable private fun Field(label: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, Bronze, shape).padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Muted, fontSize = 10.sp)
        content()
    }
}

/** Плашка «Кому»: портрет и «Имя · уровень» героя или герб и «Вся гильдия · N» - не редактируется. */
@Composable private fun Addressee(game: GameUi, draft: LetterDraft, quota: MailQuota?) {
    val guild = quota?.guild?.takeIf { draft.kind == MailKind.GUILD }
    val tint = if (draft.kind == MailKind.GUILD) Caution else Parchment
    Row(
        Modifier.border(1.dp, if (draft.kind == MailKind.GUILD) Caution else Bronze, RoundedCornerShape(20.dp)).padding(start = 3.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (draft.kind == MailKind.GUILD) {
            guild?.let { GuildEmblem(it.emblem, it.color, TAG) }
            Text(ui("mail.letter.to_guild", guild?.members ?: 0), color = tint, style = MaterialTheme.typography.labelMedium)
        } else {
            ClassPortrait(draft.toClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(TAG), round = true, ring = Bronze)
            Text(ui("mail.letter.to_hero", draft.toName, draft.toLevel), color = tint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Квота: «Сегодня X из N» и «этому герою Y из M»; у рассылки - рассылки гильдии за сутки. */
@Composable private fun QuotaBar(quota: MailQuota, draft: LetterDraft) {
    Row(Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(ui("mail.quota.day", quota.sent, quota.perDay), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        val right = if (draft.kind == MailKind.GUILD) {
            quota.guild?.let { ui("mail.quota.guild", it.sent, it.perDay) }
        } else {
            ui("mail.quota.recipient", quota.toRecipient, quota.perRecipient)
        }
        right?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
    }
}

/** Почему писать нельзя - словами, с числом правил: уровень, письма за сутки, письма этому герою. */
internal fun gateText(gate: MailGate, quota: MailQuota?, rules: MailRules, unlockLevel: Int = quota?.level ?: 1): String = when (gate) {
    MailGate.OPEN -> ui("mail.gate.open")
    MailGate.MUTED -> ui("mail.gate.muted")
    MailGate.LEVEL -> ui("mail.gate.level", unlockLevel)
    MailGate.DAY_LIMIT -> ui("mail.gate.day_limit", quota?.perDay ?: rules.perDay)
    MailGate.RECIPIENT_LIMIT -> ui("mail.gate.recipient_limit", quota?.perRecipient ?: rules.perRecipient)
}

/** The auction's system letters (3.79.0): a lot come back, a lot about to leave. */
private val AUCTION_MAIL = setOf("mail.auction_expired", "mail.auction_expiring")

/** A lot's item by its code: an equipment template or a stack of the bag. */
private fun lotItemTitle(code: String): String = if (com.sperance.exileforge.core.i18n.serverLocale.contains(com.sperance.exileforge.rules.text.LocaleKey.equipmentName(code))) equipmentTitle(code) else itemTitle(code)

/** Знак игры и администратора. */
private const val RUNE = "ᚱ"
private val MARK = 34.dp
private val TAG = 22.dp
private const val SHORT_STAMP = 5
private const val QUOTE_LINES = 3
private const val DAY_MS = 86_400_000L
private const val SECOND_MS = 1_000L
private const val TICK_MS = 1_000L
