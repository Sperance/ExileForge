package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.mailBody
import com.sperance.exileforge.core.display.mailSubject
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.feedback.Feedback
import com.sperance.exileforge.core.feedback.FeedbackModel
import com.sperance.exileforge.core.feedback.LetterDraft
import com.sperance.exileforge.core.feedback.MailCategory
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.IgnoredHero
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind
import com.sperance.exileforge.core.model.feedback.MailLink
import com.sperance.exileforge.core.model.feedback.MailLinkKind
import com.sperance.exileforge.core.model.feedback.MailMute
import com.sperance.exileforge.core.model.feedback.MailQuota
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.MailGate
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.listedAt
import com.sperance.exileforge.ui.screens.guild.GuildEmblem
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Вещи витрины во вложении письма (шаблоны, не копии) строкой каждая - с карточкой витрины, которую строка открывает. Золото, стопки и
 * копии вещей письмо показывает общим обликом добычи ([LootPanel]).
 */
private fun showcaseLines(mail: Mail): List<Pair<String, Inspect>> = mail.attachment.equipment.map { equipmentTitle(it.template) + (it.rarity?.let { r -> " · " + ui("enum.rarity.${r.name}") } ?: "") to Inspect.Showcase(it.template) }

/** Вкладка ящика: какие письма она держит. Новая вкладка - запись перечисления, не ветка экрана. */
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

/**
 * Кто написал письмо - строкой: герой, «[TAG] Гильдия · автор»; у письма игры и администратора - имя его категории
 * ([MailCategory]: «Рынок», «Награды», «Администрация»), его печатают цветом категории ([senderTint]).
 */
internal fun senderLine(mail: Mail): String {
    val from = mail.from
    val guild = from?.guild
    return when {
        guild != null && mail.kind == MailKind.GUILD -> ui("mail.from_guild", guild.tag, from.name)
        from != null -> from.name
        else -> MailCategory.of(mail).title()
    }
}

/** Цвет строки отправителя: у письма героя и гильдии - [person], у письма игры и администратора - цвет его категории. */
private fun senderTint(mail: Mail, person: Color): Color = if (mail.from != null) person else MailCategory.of(mail).tint

/** Цвет категории письма: им красятся знак, имя категории и кромка письма. */
private val MailCategory.tint: Color
    get() = when (this) {
        MailCategory.MARKET -> Boon
        MailCategory.PERSONAL -> Parchment
        MailCategory.GUILD -> Rune
        MailCategory.ADMIN -> LossText
        MailCategory.REWARDS -> ModeHue.Uniques
        MailCategory.REPORTS -> Crafted
        MailCategory.SYSTEM -> Quiet
    }

/** Знак категории письма: весы рынка, изгнанник, знамя, щит администрации, кубок, свиток отчёта, созвездие системы. */
private val MailCategory.glyph: ImageVector
    get() = when (this) {
        MailCategory.MARKET -> ForgeGlyphs.Scales
        MailCategory.PERSONAL -> ForgeGlyphs.Exile
        MailCategory.GUILD -> ForgeGlyphs.Banner
        MailCategory.ADMIN -> ForgeGlyphs.Kite
        MailCategory.REWARDS -> ForgeGlyphs.Trophy
        MailCategory.REPORTS -> ForgeGlyphs.Scroll
        MailCategory.SYSTEM -> ForgeGlyphs.Constellation
    }

/** Сколько целых дней письму осталось; меньше суток - 0. */
private fun daysLeft(mail: Mail, now: Long = System.currentTimeMillis()): Long = (mail.expiresAt - now).coerceAtLeast(0) / DAY_MS

/**
 * «сгорит через N дн.» или «сгорит сегодня»; письмо с невзятым вложением не сгорает - «хранится, пока не
 * заберёшь».
 */
internal fun burnText(mail: Mail): String = when {
    mail.claimable -> ui("mail.kept")
    else -> daysLeft(mail).let { if (it <= 0) ui("mail.burns_today") else ui("mail.burns_in", it) }
}

/**
 * Знак отправителя слева у письма: портрет класса героя, герб гильдии рассылки, у письма игры и администратора - знак его категории
 * ([CategoryMark]). Один знак на список и письмо.
 */
@Composable internal fun SenderMark(game: GameUi, mail: Mail, size: Dp = MARK) {
    val from = mail.from
    val guild = from?.guild
    when {
        guild != null && mail.kind == MailKind.GUILD -> GuildEmblem(guild.emblem, guild.color, size)
        from != null -> ClassPortrait(from.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(size), round = true, ring = Bronze)
        else -> CategoryMark(MailCategory.of(mail), size)
    }
}

/** Знак категории [category]: её глиф её цветом на плашке, тонированной тем же цветом, в его кромке; чтение с экрана слышит имя категории. */
@Composable private fun CategoryMark(category: MailCategory, size: Dp) {
    val tint = category.tint
    val shape = RoundedCornerShape(size * MARK_RADIUS)
    val name = category.title()
    Box(
        Modifier.size(size).background(tint.copy(alpha = PLATE_FILL).compositeOver(Panel), shape).border(1.dp, tint.copy(alpha = PLATE_RIM), shape)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.glyph, null, tint = tint, modifier = Modifier.size(size * GLYPH_SHARE))
    }
}

/**
 * Ящик («Свиток писем»): вкладки Все / Личные / Гильдия / Система со счётом непрочитанного, письмо строкой со
 * знаком отправителя, темой, первой строкой и сроком; касание открывает письмо листом поверх, письмо со ссылкой - её цель ([MailLinks]). «Прочитать все» и «Удалить
 * прочитанные» - разом; список игнора - тут же.
 *
 * Почта: лист с вкладками, письмо - листом поверх, новое письмо - своим листом; [focus] - письмо, открытое сразу (итоги состязания). Письма героев видны лишь адресату - ящик читается глазами
 * героя в игре.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailSheet(game: GameUi, focus: String? = null, onDismiss: () -> Unit) {
    val model = koinViewModel<FeedbackViewModel>().model
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId) {
        model.loadMail()
        model.letters.loadIgnored()
        focus?.let(model::readMail)
    }
    val sheets = LocalSystemSheets.current
    var open by remember { mutableStateOf(focus) }
    var box by remember { mutableStateOf(MailBox.ALL) }
    var ignoring by remember { mutableStateOf(false) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ui("mail.title"), style = relicName(19).copy(color = GoldBright), modifier = Modifier.weight(1f))
                ForgeButton(onClick = { ignoring = !ignoring }, kind = ButtonKind.Text, accent = Muted) {
                    Text(if (ignoring) ui("mail.back") else ui("mail.ignore.list", feedback.ignored.size), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (ignoring) {
                IgnoreList(feedback, activity.busy) { hero -> model.letters.ignore(hero.heroId, hero.name, on = false) }
                return@Column
            }
            game.index?.rules?.mail?.keepDays?.let { MutedText(ui("mail.keep", it)) }
            MailActionsRow(model, feedback, activity.busy)
            MailTabs(feedback.mail, box) { box = it }
            val shown = feedback.mail.filter { box.holds(it.kind) }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (shown.isEmpty()) item { MutedText(ui("mail.empty"), Modifier.padding(vertical = 12.dp)) }
                items(shown, key = { it.id }) { mail ->
                    MailRow(game, mail) {
                        if (!mail.read) model.readMail(mail.id)
                        if (!MailLinks.open(mail.link, sheets)) open = mail.id
                    }
                }
            }
        }
    }
    feedback.mail.firstOrNull { it.id == open }?.let { letter -> LetterSheet(game, model, feedback, activity.busy, letter) { open = null } }
}

/**
 * Открытие ссылки письма ([Mail.link]): касание письма со ссылкой известного вида открывает цель вместо листа письма. Новый вид -
 * запись [MailLinkKind] и его ветка здесь.
 */
object MailLinks {
    /** Открывает цель [link] системными листами [sheets]; false - ссылки нет или вид незнаком сборке: письмо открывается как обычно. */
    fun open(link: MailLink?, sheets: SystemSheets?): Boolean {
        if (link == null || sheets == null) return false
        return when (link.target) {
            MailLinkKind.REPORT -> {
                sheets.reports = ReportsView(root = ui("mail.title"), focus = link.id)
                true
            }

            null -> false
        }
    }
}

/** «Прочитать все» и «Удалить прочитанные» - с вопросом, сколько уйдёт; письмо с вложением не удаляется. */
@Composable private fun MailActionsRow(model: FeedbackModel, feedback: Feedback, busy: Boolean) {
    var purging by remember { mutableStateOf(false) }
    val unread = feedback.mail.count { !it.read }
    val removable = feedback.mail.count { it.read && !it.claimable }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeButton(onClick = model::readAllMail, state = ButtonState.of(!busy && unread > 0), modifier = Modifier.weight(1f), kind = ButtonKind.Secondary) {
            Text(ui("mail.read_all", unread), style = MaterialTheme.typography.labelMedium)
        }
        ForgeButton(onClick = { purging = true }, state = ButtonState.of(!busy && removable > 0), modifier = Modifier.weight(1f), kind = ButtonKind.Secondary) {
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

/** Вкладки ящика: подчёркнута выбранная, у каждой - счёт непрочитанного ([UnreadCount], число без плашки). */
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
                UnreadCount(unread)
            }
        }
    }
}

/**
 * Строка письма: слева кромка цвета категории ([MailCategory], у прочитанного приглушена), знак отправителя, кто (у непрочитанного -
 * точка; у письма игры и администратора - имя категории её цветом), тема, первая строка; справа время и срок.
 */
@Composable private fun MailRow(game: GameUi, mail: Mail, onOpen: () -> Unit) {
    val edge = MailCategory.of(mail).tint.let { if (mail.read) it.copy(alpha = READ_EDGE) else it }
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth()
                .drawBehind {
                    val inset = EDGE_INSET.toPx()
                    drawRoundRect(edge, Offset(0f, inset), Size(EDGE.toPx(), size.height - 2 * inset), CornerRadius(EDGE.toPx() / 2))
                }
                .padding(start = 12.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SenderMark(game, mail)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!mail.read) Box(Modifier.size(7.dp).background(Gold, CircleShape))
                    Text(
                        senderLine(mail),
                        modifier = Modifier.opensPlayer(mail.from?.heroId),
                        color = senderTint(mail, if (mail.read) Muted else GoldBright),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = when {
                            !mail.read -> FontWeight.SemiBold
                            mail.from == null -> FontWeight.Medium
                            else -> FontWeight.Normal
                        },
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
 * Письмо листом поверх ящика («Свиток»): плашка отправителя ([SenderPlate]) закреплена сверху, у письма героя - с «⋯» ([LetterMenu]:
 * игнор и жалоба); тема Cinzel, текст серифом, цитата ответа и вложение рамкой с «Забрать» листаются под ней. В подвале листа -
 * «Ответить» (у писем, на которые отвечают) и «Удалить» одним рядом.
 */
@Composable
private fun LetterSheet(game: GameUi, model: FeedbackModel, feedback: Feedback, busy: Boolean, mail: Mail, onClose: () -> Unit) {
    var reporting by remember { mutableStateOf(false) }
    val from = mail.from
    val ignored = from != null && feedback.ignored.any { it.heroId == from.heroId }
    val menu: (@Composable RowScope.() -> Unit)? = if (from != null && mail.answerable) {
        {
            LetterMenu(
                name = from.name,
                ignored = ignored,
                reported = mail.reported,
                enabled = !busy,
                onIgnore = { model.letters.ignore(from.heroId, from.name, on = !ignored) },
                onReport = { reporting = true },
            )
        }
    } else {
        null
    }
    ForgeSheet(
        onDismissRequest = onClose,
        body = SheetBody.Scroll(spacing = 12.dp),
        header = { SenderPlate(game, mail, menu) },
        footer = {
            if (mail.answerable) {
                ForgeButton(onClick = {
                    onClose()
                    model.letters.reply(mail, game.index?.rules?.mail?.subject ?: Int.MAX_VALUE)
                }, state = ButtonState.of(!busy), modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Outlined.Reply) { Label(ui("mail.reply")) }
            }
            ForgeButton(onClick = {
                model.deleteMail(mail.id)
                onClose()
            }, state = ButtonState.of(!busy), modifier = Modifier.weight(1f), kind = ButtonKind.Danger, icon = Icons.Outlined.Delete) { Label(ui("mail.delete")) }
        },
    ) {
        Text(mailSubject(mail), style = relicName(16).copy(color = GoldBright, lineHeight = 21.sp))
        Text(mailBody(mail), color = Parchment, fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 24.sp)
        if (mail.quote.isNotBlank()) Quote(ui("mail.quote_of_mine"), mail.quote)
        Attachment(game, model, busy, mail)
    }
    if (reporting) {
        ConfirmSheet(ui("mail.report.title"), ui("mail.report"), onDismiss = { reporting = false }, note = ui("mail.report.note"), danger = true) {
            reporting = false
            model.letters.report(mail.id)
        }
    }
}

/** Подпись кнопки подвала письма: в одну строку, длинная обрезается. */
@Composable private fun RowScope.Label(label: String) {
    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
}

/**
 * Плашка отправителя письма: знак, имя (длинное переносится, не режется), «класс · уровень · время · срок» - срок `Caution`, невзятое
 * вложение «хранится, пока не заберёшь» `Vital`; справа [actions], если есть.
 */
@Composable private fun SenderPlate(game: GameUi, mail: Mail, actions: (@Composable RowScope.() -> Unit)? = null) {
    val about = remember(mail) { senderAbout(mail) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .background(Brush.verticalGradient(listOf(PanelRaised, Panel)), shape)
            .border(1.dp, Bronze, shape)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SenderMark(game, mail, PLATE_MARK)
        Column(Modifier.weight(1f).padding(top = 2.dp)) {
            Text(senderLine(mail), color = senderTint(mail, GoldBright), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.opensPlayer(mail.from?.heroId))
            Text(about, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        actions?.invoke(this)
    }
}

/** Сведения об отправителе строкой; срок письма героя - цветом: сгорит - `Caution`, хранится ради вложения - `Vital`. */
private fun senderAbout(mail: Mail): AnnotatedString = buildAnnotatedString {
    val from = mail.from
    append(listOfNotNull(from?.let { classTitle(it.heroClass) }, from?.level?.toString(), listedAt(mail.createdAt)).joinToString(" · "))
    if (mail.kind.written) {
        if (length > 0) append(" · ")
        withStyle(SpanStyle(color = if (mail.claimable) Vital else Caution)) { append(burnText(mail)) }
    }
}

/**
 * «⋯» письма героя: «Игнорировать» / «Не игнорировать» отправителя [name] с пояснением и «Пожаловаться» / «Жалоба подана». Меню -
 * [ForgeMenu]: окна у закрытого меню нет, оно появляется на касание и уходит с анимацией закрытия.
 */
@Composable private fun LetterMenu(name: String, ignored: Boolean, reported: Boolean, enabled: Boolean, onIgnore: () -> Unit, onReport: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ForgeIconButton(Icons.Outlined.MoreHoriz, ui("common.more"), onClick = { open = true }, kind = if (open) ButtonKind.Secondary else ButtonKind.Text, size = ButtonSize.Compact, shape = IconButtonShape.Circle)
        ForgeMenu(expanded = open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(ui(if (ignored) "mail.ignore.off" else "mail.ignore.on"), color = Parchment)
                        Text(ui(if (ignored) "mail.ignore.off_note" else "mail.ignore.on_note", name), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.Block, null, tint = Parchment) },
                enabled = enabled,
                onClick = {
                    open = false
                    onIgnore()
                },
            )
            HorizontalDivider(color = Bronze.copy(alpha = .47f), thickness = 1.dp)
            val tint = if (reported) Muted else LifeRed
            DropdownMenuItem(
                text = { Text(ui(if (reported) "mail.report.done" else "mail.report"), color = tint) },
                leadingIcon = { Icon(if (reported) Icons.Outlined.Check else Icons.Outlined.Flag, null, tint = tint) },
                enabled = enabled && !reported,
                onClick = {
                    open = false
                    onReport()
                },
            )
        }
    }
}

/**
 * Вложение письма - рамкой в конце текста: добыча общим обликом ([LootPanel]) - копии вещей короткими строками ([LootRow]: вердикт к
 * надетому и цена торговца), стопки ячейками, золото чипом; ниже строки вещей витрины; касание - карточка; внизу «Забрать» героем в
 * игре, один раз.
 */
@Composable private fun Attachment(game: GameUi, model: FeedbackModel, busy: Boolean, mail: Mail) {
    val lines = showcaseLines(mail)
    val copies = remember(mail.attachment.instances, game.index) { mail.attachment.instances.mapNotNull { item -> game.view(item)?.let { item to it } } }
    val attached = mail.attachment
    if (lines.isEmpty() && copies.isEmpty() && attached.items.isEmpty() && attached.gold <= 0) return
    val inspect = rememberInspect()
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp).background(Abyss, shape).border(1.dp, Bronze, shape).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(ui("mail.attachment"), color = Muted, style = MaterialTheme.typography.labelMedium)
        LootPanel(game.index, copies, attached.items, lootSums(attached.gold, 0.0), onStack = { inspect(Inspect.Stack(it)) }, modifier = Modifier.padding(vertical = 3.dp)) { (item, piece) ->
            key(item.id) { LootRow(piece, verdict = rememberGearVerdict(game, item), price = piece.sellPrice()) { inspect(Inspect.Copy(item)) } }
        }
        lines.forEach { (line, card) ->
            Box(Modifier.fillMaxWidth().heightIn(min = ATTACHMENT_LINE).clickable { inspect(card) }, contentAlignment = Alignment.CenterStart) {
                Text(line, color = Vital, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (mail.claimable) {
            ForgeButton(state = ButtonState.of(!busy && game.heroId.isNotBlank()), onClick = { model.claimMail(mail.id, game.heroId) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(ui("mail.claim", game.heroName), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            MutedText(ui("mail.claimed_already"))
        }
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
            Column(Modifier.weight(1f).opensPlayer(hero.heroId)) {
                Text(hero.name, color = Parchment, style = MaterialTheme.typography.labelLarge)
                MutedText(classTitle(hero.heroClass), style = MaterialTheme.typography.labelSmall)
            }
            ForgeButton(onClick = { onLift(hero) }, state = ButtonState.of(!busy), kind = ButtonKind.Text, accent = Gold) { Text(ui("mail.ignore.lift")) }
        }
        HorizontalDivider(color = Bronze.copy(alpha = .47f), thickness = 1.dp)
    }
}

/**
 * Хозяин окна нового письма: черновик в почте ([Feedback.letter]) открывает лист поверх всего - из карточки игрока,
 * «Ответить» и рассылки гильдии. Кому - плашка героя или гильдии рядом с подписью, не ввод; тема и текст со счётчиками - поле текста
 * растёт вместе с листающимся телом листа, курсор оно держит на виду само; цитата ответа; немота - причина и срок сверху.
 * «Отправить» - в подвале листа, над клавиатурой; нельзя (немота, рассылка без права) - погашена.
 */
@Composable
fun LetterHost(game: GameUi) {
    val model = koinViewModel<FeedbackViewModel>().model
    val feedback by model.feedback.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val draft = feedback.letter ?: return
    val rules = game.index?.rules?.mail ?: return
    var subject by remember(draft) { mutableStateOf(draft.subject) }
    var body by remember(draft) { mutableStateOf("") }
    val quota = feedback.quota
    val barred = draft.kind == MailKind.GUILD && quota?.guild == null
    ForgeSheet(
        onDismissRequest = model.letters::close,
        body = SheetBody.Scroll(spacing = 8.dp),
        footer = {
            ForgeButton(
                onClick = { model.letters.send(subject, body) },
                state = ButtonState.of(!activity.busy && quota?.gate == MailGate.OPEN && !barred && subject.isNotBlank() && body.isNotBlank()),
                modifier = Modifier.weight(1f),
            ) { Text(ui("mail.send")) }
        },
    ) {
        Text(ui(if (draft.kind == MailKind.GUILD) "mail.letter.broadcast_title" else "mail.letter.title"), style = relicName(19).copy(color = GoldBright))
        quota?.mute?.let { MuteNote(it) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("mail.letter.to"), color = Muted, style = MaterialTheme.typography.labelMedium)
            Addressee(game, draft, quota, Modifier.weight(1f, fill = false))
        }
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
    }
}

/** Немота: писать нельзя до срока (или навсегда), категория и причина для игрока. */
@Composable private fun MuteNote(mute: MailMute) {
    val term = mute.until?.let { listedAt(it) }?.let { ui("mail.mute.until", it) } ?: ui("mail.mute.forever")
    val reason = listOf(ui("moderation.category.${mute.category}"), mute.reason).filter { it.isNotBlank() }.joinToString(" · ")
    Text(
        ui("mail.mute.note", term, reason),
        color = LifeRed.copy(alpha = .85f),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().background(LifeRed.copy(alpha = .08f), RoundedCornerShape(10.dp)).border(1.dp, LifeRed.copy(alpha = .33f), RoundedCornerShape(10.dp)).padding(10.dp),
    )
}

/** Плашка «Кому»: портрет и «Имя · уровень» героя или герб и «Вся гильдия · N» - не редактируется; длинное имя обрезается. */
@Composable private fun Addressee(game: GameUi, draft: LetterDraft, quota: MailQuota?, modifier: Modifier = Modifier) {
    val guild = quota?.guild?.takeIf { draft.kind == MailKind.GUILD }
    val tint = if (draft.kind == MailKind.GUILD) Caution else Parchment
    Row(
        modifier.border(1.dp, if (draft.kind == MailKind.GUILD) Caution else Bronze, RoundedCornerShape(20.dp)).padding(start = 3.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (draft.kind == MailKind.GUILD) {
            guild?.let { GuildEmblem(it.emblem, it.color, TAG) }
            Text(ui("mail.letter.to_guild", guild?.members ?: 0), color = tint, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            ClassPortrait(draft.toClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(TAG), round = true, ring = Bronze)
            Text(ui("mail.letter.to_hero", draft.toName, draft.toLevel), color = tint, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Почему писать нельзя - словами. */
internal fun gateText(gate: MailGate): String = when (gate) {
    MailGate.OPEN -> ui("mail.gate.open")
    MailGate.MUTED -> ui("mail.gate.muted")
}

/** Плашка знака категории: скругление долей стороны, заливка и кромка - доли цвета категории, глиф - доля стороны. */
private const val MARK_RADIUS = .26f
private const val PLATE_FILL = .16f
private const val PLATE_RIM = .35f
private const val GLYPH_SHARE = .53f

/** Кромка категории у строки письма: ширина, отступ от верха и низа строки и приглушение у прочитанного. */
private val EDGE = 3.dp
private val EDGE_INSET = 6.dp
private const val READ_EDGE = .45f

private val MARK = 34.dp
private val PLATE_MARK = 44.dp
private val MORE = 44.dp
private val ATTACHMENT_LINE = 44.dp
private val TAG = 22.dp
private const val SHORT_STAMP = 5
private const val QUOTE_LINES = 3
private const val DAY_MS = 86_400_000L
