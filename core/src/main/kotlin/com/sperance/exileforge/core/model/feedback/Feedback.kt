package com.sperance.exileforge.core.model.feedback

import com.sperance.exileforge.rules.content.MailGate
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What a report is: a bug or a player's suggestion; жалоба на письмо героя - [COMPLAINT]. */
@Serializable enum class FeedbackKind {
    BUG,
    APPEAL,
    COMPLAINT,

    /** Падение клиента (сервер API 86): стек шлёт сам клиент при следующем запуске; в списках игрока его нет. */
    CRASH,
}

/**
 * Где отчёт: [CREATED] - создан игроком, [SENT] - отправлен Issue в GitHub («Рассмотрения»
 * нет - первое слово модерации отправляет отчёт сразу), [REJECTED] - отклонён модерацией с причиной.
 * Куда отчёт может перейти и можно ли голосовать, решает машина состояний сервера: она приходит полями [AdminReport.moves] и
 * [KnownReport.votable].
 */
@Serializable enum class ReportStatus { CREATED, SENT, REJECTED }

/**
 * Судьба Issue отчёта (сервер `IssueState`): открыт - [OPEN] («В работе»; метки «в работе» нет);
 * закрыт выполненным - [FIXED] (с версией), отказом - [NOT_PLANNED], [DUPLICATE], [INVALID].
 */
@Serializable enum class IssueState(val open: Boolean) {
    OPEN(true),
    FIXED(false),
    NOT_PLANNED(false),
    DUPLICATE(false),
    INVALID(false),
}

/**
 * Issue отчёта: номер и ссылка в публичном репозитории, судьба, версия исправления, слово разработчика, дубль чего; с
 * вся переписка [notes].
 */
@Serializable data class IssueView(
    val number: Int,
    val url: String = "",
    val state: IssueState = IssueState.OPEN,
    val version: String = "",
    val comment: String = "",
    val duplicate: Int = 0,
    val notes: List<IssueNote> = emptyList(),
)

/** Кто написал в Issue: разработчик в GitHub, модерация из игры или автор отчёта из игры. */
@Serializable enum class NoteAuthor { DEVELOPER, MODERATION, AUTHOR }

/** Запись переписки Issue: кто, что и когда (ISO-8601). */
@Serializable data class IssueNote(val author: NoteAuthor, val text: String, val at: String = "")

/** Что модерация делает с Issue отчёта из игры (сервер `IssueMove`): ответить или закрыть с причиной. */
@Serializable enum class IssueMove { REPLY, FIXED, NOT_PLANNED, INVALID, DUPLICATE }

/** Текст ответа в Issue из игры: автора отчёта или модерации. */
@Serializable data class ReportAnswer(val text: String)

/** A player's vote on a report («+ / −» у ошибок): one per account, switched by tapping again. */
@Serializable enum class Vote { LIKE, DISLIKE, NONE }

/** A known report in the public list (ошибки, без предложений): no author — only the administrator sees who wrote it; [mine] marks the viewer's own. */
@Serializable data class KnownReport(
    val id: String,
    val text: String,
    val status: ReportStatus,
    val likes: Int,
    val dislikes: Int,
    val vote: Vote,
    val mine: Boolean,
    val createdAt: String = "",
    /** Можно ли зрителю голосовать: не своё и ещё ждёт решения (и пока Issue открыт) - решает сервер. */
    val votable: Boolean = false,
    /** Ошибка или предложение (известные проблемы - и ошибки). */
    val kind: FeedbackKind = FeedbackKind.BUG,
    val issue: IssueView? = null,
) {
    val rating: Int get() = likes - dislikes
}

/** One of the viewer's own reports, with the administrator's word. */
@Serializable data class OwnReport(
    val id: String,
    val kind: FeedbackKind,
    val text: String,
    val status: ReportStatus,
    val reason: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
    val createdAt: String = "",
    /** Автор может удалить его: ошибку и предложение - да, апелляцию - нет. */
    val deletable: Boolean = false,
    /** Issue отчёта; null - ещё не отправлен. */
    val issue: IssueView? = null,
) {
    /** Последний ответ автору (последняя запись переписки не от автора): слово разработчика или модерации; пусто - ответа нет. */
    val reply: String get() = issue?.notes?.lastOrNull { it.author != NoteAuthor.AUTHOR }?.text ?: issue?.comment?.takeIf { it.isNotBlank() } ?: reason

    /** Отпечаток последнего ответа: сменился - ответ новый; null - ответа нет. */
    val replyPrint: String? get() = reply.takeIf { it.isNotBlank() }?.hashCode()?.toString()
}

/**
 * Отчёт целиком глазами модерации - без системного (устройство, ошибки сети, действия, лист героя - только в теле Issue): [place] -
 * где был игрок, [screen] - раздел; [context] - обстоятельства дела, только у апелляции и жалобы.
 */
@Serializable data class FullReport(
    @SerialName("_id") val id: String,
    val text: String,
    val screen: String = "",
    val place: String = "",
    val context: Map<String, String> = emptyMap(),
    val userId: String? = null,
    /** Герой автора: свой герой его аккаунта и имя на момент отчёта; null - без героя. */
    val heroId: String? = null,
    val heroName: String = "",
    /** Класс и уровень героя автора на момент отчёта. */
    val heroClass: String = "",
    val heroLevel: Int = 0,
    val status: ReportStatus = ReportStatus.CREATED,
    val kind: FeedbackKind = FeedbackKind.BUG,
    /** Голоса «+» и «−» числом. */
    val likes: Int = 0,
    val dislikes: Int = 0,
    val reason: String = "",
    val createdAt: String = "",
    /** Issue отчёта; null - не выгружен. */
    val issue: IssueView? = null,
    /** Код последнего отказа GitHub: выгрузка не удалась; пусто - всё ушло. */
    val issueError: String = "",
    /** Повторы падения и версии, где оно было; у прочих отчётов - 1 и пусто. */
    val occurrences: Int = 1,
    val versions: List<String> = emptyList(),
    /** Дополнение модерации: раздел тела Issue, автору в игре не отдаётся; пусто - его нет. */
    val moderationNote: String = "",
)

/**
 * Отчёт для модерации: с логином и ролью автора; [moves] - куда его можно перевести по машине состояний сервера,
 * [deletable] - может ли смотрящий его удалить.
 */
@Serializable data class AdminReport(
    val report: FullReport,
    val login: String? = null,
    val role: com.sperance.exileforge.core.network.AccountRole? = null,
    val moves: List<ReportStatus> = emptyList(),
    val deletable: Boolean = false,
)

/** A letter's thing to take: a template and its rarity, rolled at the hero's level when taken. */
@Serializable data class MailEquipment(val template: String, val rarity: Rarity? = null)

/** What a letter carries: gold, stacks of the bag by code, things; one hero of the account takes it, once. */
@Serializable data class MailAttachment(
    val gold: Long = 0,
    val items: Map<String, Long> = emptyMap(),
    val equipment: List<MailEquipment> = emptyList(),
    /** Items as they are: an expired lot's goods come back by mail with their rolls. */
    val instances: List<com.sperance.exileforge.rules.roll.ItemInstance> = emptyList(),
) {
    val empty: Boolean get() = gold <= 0 && items.isEmpty() && equipment.isEmpty() && instances.isEmpty()
}

/**
 * Чьё письмо: игры, администратора; героя герою ([PLAYER]) и рассылка гильдии ([GUILD]).
 *
 * @property written письмо пишет герой: на него отвечают, жалуются и по нему игнорируют автора
 * @property answerable на него отвечают и до уровня права писать - на рассылку гильдии отвечают сразу (как сервер)
 */
@Serializable enum class MailKind(val written: Boolean = false, val answerable: Boolean = false) {
    SYSTEM,
    ADMIN,
    PLAYER(written = true),
    GUILD(written = true, answerable = true),
}

/** Гильдия рассылки - снимок на момент письма. */
@Serializable data class MailGuild(val id: String, val name: String, val tag: String, val emblem: String = "", val color: String = "")

/** Автор письма героя - снимок на момент письма; у рассылки - и гильдия. Ответ уходит лично ему, [heroId]. */
@Serializable data class MailSender(val heroId: String, val name: String, val heroClass: String = "", val level: Int = 1, val guild: MailGuild? = null)

/** Вид цели ссылки письма (как сервер): что открывает касание письма. */
@Serializable enum class MailLinkKind {
    /** Отчёт игрока: письмо о его судьбе открывает этот отчёт. */
    REPORT,
}

/** Ссылка письма на то, о чём оно: вид цели строкой (вид, которого эта сборка не знает, не ломает ящик) и id цели. */
@Serializable data class MailLink(val kind: String, val id: String) {
    /** Вид цели, известный этой сборке; null - неизвестный: письмо открывается как обычно. */
    val target: MailLinkKind? get() = MailLinkKind.entries.firstOrNull { it.name == kind }
}

/**
 * A letter of the account: a system one names a dictionary [key] with its [args], the administrator's one its
 * [subject] and [body] as written. [claimedBy] — the hero that took the attachment; [expiresAt] — epoch millis. Письмо героя: [from] - автор, [quote] - начало письма, на которое это ответ, [reported] - жалоба уже подана.
 */
@Serializable data class Mail(
    @SerialName("_id") val id: String,
    val kind: MailKind = MailKind.ADMIN,
    val key: String = "",
    val args: List<String> = emptyList(),
    val subject: String = "",
    val body: String = "",
    val attachment: MailAttachment = MailAttachment(),
    val heroId: String? = null,
    val read: Boolean = false,
    val claimedBy: String? = null,
    val expiresAt: Long = 0,
    val createdAt: String = "",
    val from: MailSender? = null,
    val quote: String = "",
    val reported: Boolean = false,
    /** О чём письмо: касание письма открывает цель; null - ссылки нет (прежние письма). */
    val link: MailLink? = null,
) {
    val claimable: Boolean get() = claimedBy == null && !attachment.empty

    /** Письмо героя с автором: на него отвечают, жалуются, по нему игнорируют. */
    val answerable: Boolean get() = kind.written && from != null
}

/**
 * Письмо героя (сервер `POST mail/letter`): личное герою [to], ответ на письмо [replyTo] (адресат - его автор) или
 * рассылка гильдии ([MailKind.GUILD]).
 */
@Serializable data class LetterRequest(val kind: MailKind = MailKind.PLAYER, val to: String = "", val replyTo: String = "", val subject: String, val body: String)

/** Немота глазами немого: категория, причина для игрока (без номера и заметки модератора) и срок. */
@Serializable data class MailMute(val category: String, val reason: String = "", val until: String? = null)

/** Рассылка гильдии главе или офицеру: кому уйдёт и сколько рассылок гильдии за сутки. */
@Serializable data class GuildMailQuota(val tag: String, val name: String, val emblem: String = "", val color: String = "", val members: Int = 0, val sent: Int = 0, val perDay: Int = 0)

/**
 * Почта героя сейчас (сервер `GET mail/quota`): [gate] - можно ли писать (адресату, если назван) или почему нет;
 * письма за сутки [sent] из [perDay], этому герою [toRecipient] из [perRecipient]; [readyAt] - конец паузы между письмами (мс
 * эпохи); [level] - с какого уровня пишут; [mute] - немота; [guild] - рассылка, если роль её допускает.
 */
@Serializable data class MailQuota(
    val gate: MailGate,
    val sent: Int = 0,
    val perDay: Int = 0,
    val toRecipient: Int = 0,
    val perRecipient: Int = 0,
    val readyAt: Long = 0,
    val level: Int = 1,
    val mute: MailMute? = null,
    val guild: GuildMailQuota? = null,
)

/** Строка списка игнора: чьи письма герой не принимает и с какого времени (мс эпохи). */
@Serializable data class IgnoredHero(val heroId: String, val name: String, val heroClass: String = "", val at: Long = 0)

/** The administrator's letter: [login] one account, blank every account. */
@Serializable data class MailRequest(val login: String = "", val subject: String, val body: String, val attachment: MailAttachment = MailAttachment())
