package com.sperance.exileforge.core.model.feedback

import com.sperance.exileforge.core.model.paging.Page
import com.sperance.exileforge.rules.content.MailGate
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Вид отчёта: ошибка, апелляция, жалоба на письмо героя - [COMPLAINT]; улучшение - тема ошибки ([ReportTopic]). */
@Serializable enum class FeedbackKind {
    BUG,
    APPEAL,
    COMPLAINT,

    /** Падение клиента (сервер API 86): стек шлёт сам клиент при следующем запуске; в списках игрока его нет. */
    CRASH,
}

/**
 * Где отчёт (сервер `BugStatus`): [CREATED] - создан игроком и ждёт модерацию; [DISCUSSION] - модерация написала комментарий, идёт
 * переписка с автором; [SENT] - модерация отдала его в работу (Issue в GitHub); [REJECTED] - отклонён модерацией с причиной;
 * [WITHDRAWN] - закрыт автором. Куда отчёт может перейти, решает машина состояний сервера: она приходит полями [AdminReport.moves],
 * [AdminReport.discussable] и [OwnReport.closable].
 */
@Serializable enum class ReportStatus { CREATED, DISCUSSION, SENT, REJECTED, WITHDRAWN }

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
) {
    /** Адрес Issue уже есть: пока Issue выгружается, его нет и ссылка погашена. */
    val hasUrl: Boolean get() = url.startsWith("http")
}

/** Кто написал в Issue: разработчик в GitHub, модерация из игры или автор отчёта из игры. */
@Serializable enum class NoteAuthor { DEVELOPER, MODERATION, AUTHOR }

/** Запись переписки Issue: кто, что и когда (ISO-8601). */
@Serializable data class IssueNote(val author: NoteAuthor, val text: String, val at: String = "")

/** Что модерация делает с Issue отчёта из игры (сервер `IssueMove`): ответить или закрыть с причиной. */
@Serializable enum class IssueMove { REPLY, FIXED, NOT_PLANNED, INVALID, DUPLICATE }

/** Текст ответа в Issue из игры: автора отчёта или модерации. */
@Serializable data class ReportAnswer(val text: String)

/** One of the viewer's own reports, with the administrator's word. */
@Serializable data class OwnReport(
    val id: String,
    val kind: FeedbackKind,
    val text: String,
    val status: ReportStatus,
    val reason: String = "",
    val createdAt: String = "",
    /** Issue отчёта; null - ещё не отправлен. */
    val issue: IssueView? = null,
    /** Тема ошибки: у улучшения карточка несёт «Идея». */
    val topic: ReportTopic = ReportTopic.ERROR,
    /** Переписка, пока у отчёта нет Issue - комментарии модерации и свои ответы: сервер перенесёт её в Issue; с Issue она - в [IssueView.notes]. */
    val answers: List<IssueNote> = emptyList(),
    /** Можно ли ответить - решает сервер: отчёт на модерации или ждёт выгрузки, либо его Issue открыт; иначе поля ответа нет. */
    val answerable: Boolean = false,
    /** Можно ли закрыть отчёт самому - решает сервер: новый или в обсуждении; закрытый ([ReportStatus.WITHDRAWN]) не возвращается. */
    val closable: Boolean = false,
) {
    /** Последний ответ автору (последняя запись переписки не от автора): слово разработчика или модерации; пусто - ответа нет. */
    val reply: String get() = issue?.notes?.lastOrNull { it.author != NoteAuthor.AUTHOR }?.text ?: issue?.comment?.takeIf { it.isNotBlank() } ?: reason

    /** Отпечаток последнего ответа: сменился - ответ новый; null - ответа нет. */
    val replyPrint: String? get() = reply.takeIf { it.isNotBlank() }?.hashCode()?.toString()

    /** Переписка карточки автора по порядку ([conversation]). */
    val thread: List<IssueNote> get() = conversation(reason, answers, issue)
}

/**
 * Тема отчёта об ошибке: починить ([ERROR]) или улучшить ([IMPROVEMENT]). Улучшение подаёт только администратор: от прочих сервер
 * принимает его ошибкой.
 */
@Serializable enum class ReportTopic { ERROR, IMPROVEMENT }

/**
 * Отчёт целиком глазами модерации - строка списка: из системного только версия клиента и счёт падений и ошибок запросов, остальное
 * техническое - только в теле Issue. [place] - где был игрок, [screen] - раздел; [context] - обстоятельства
 * дела, только у апелляции и жалобы.
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
    /** Версия клиента, с которой пришёл отчёт; пусто - клиент её не прислал. */
    val clientVersion: String = "",
    /** Сколько падений клиента и неудачных запросов несёт отчёт: сами они - в теле Issue. */
    val crashCount: Int = 0,
    val failureCount: Int = 0,
    /** Тема ошибки: у улучшения карточка несёт «Идея». */
    val topic: ReportTopic = ReportTopic.ERROR,
    /** Переписка до Issue - комментарии модерации и ответы автора; с Issue она - в переписке [IssueView.notes]. */
    val answers: List<IssueNote> = emptyList(),
    /** «Новый ответ»: в обсуждении последнее слово переписки - автора, модерации ждёт его ответ; решает сервер. */
    val awaitsModeration: Boolean = false,
) {
    /** Переписка тикета модерации по порядку ([conversation]). */
    val thread: List<IssueNote> get() = conversation(reason, answers, issue)
}

/**
 * Переписка отчёта по порядку: переписка до Issue [answers] (комментарии модерации и ответы автора), затем слово решения модерации
 * [reason] - сервер шлёт их, пока у отчёта нет Issue, - затем переписка [issue] ([IssueView.notes]): разработчик, модерация из игры и
 * автор. Выгрузка кладёт переписку и слово первыми комментариями Issue в том же порядке, поэтому с Issue слово берётся только оттуда и
 * дважды не встаёт, даже если сервер прислал его и полем.
 */
private fun conversation(reason: String, answers: List<IssueNote>, issue: IssueView?): List<IssueNote> = answers + listOfNotNull(reason.takeIf { it.isNotBlank() && issue == null }?.let { IssueNote(NoteAuthor.MODERATION, it) }) + issue?.notes.orEmpty()

/**
 * Отчёт для модерации: с логином и ролью автора; [moves] - куда его можно перевести решением по машине состояний сервера,
 * [deletable] - может ли смотрящий его удалить, [discussable] - можно ли написать автору комментарий (отчёт - в «Обсуждении»), [queue] -
 * очередь модерации, где он стоит.
 */
@Serializable data class AdminReport(
    val report: FullReport,
    val login: String? = null,
    val role: com.sperance.exileforge.core.network.AccountRole? = null,
    val moves: List<ReportStatus> = emptyList(),
    val deletable: Boolean = false,
    val discussable: Boolean = false,
    val queue: FeedbackQueue = FeedbackQueue.NEW,
)

/** Очередь модерации - чип над списком отчётов (сервер `FeedbackQueue`): новые, в обсуждении, в работе, закрыто, отклонено. */
@Serializable enum class FeedbackQueue { NEW, DISCUSSION, WORK, CLOSED, REJECTED }

/** Порядок очереди модерации (сервер `FeedbackSort`): свежие первыми, по номеру Issue. */
@Serializable enum class FeedbackSort { NEWEST, ISSUE }

/** Счёт очередей модерации под одним видом и поиском: отчётов в каждой очереди [queues] и обсуждений, что ждут модерацию, - [replied]. */
@Serializable data class QueueCounts(val queues: Map<FeedbackQueue, Long> = emptyMap(), val replied: Long = 0)

/** Страница очереди модерации (`GET admin/feedback`): отчёты [page] и счёт очередей [counts] - только у первой страницы. */
@Serializable data class FeedbackQueuePage(val page: Page<AdminReport> = Page(), val counts: QueueCounts? = null)

/** Вид [kind] (null - любой) и поиск [query], под которыми модерация смотрит очередь: по ним сервер считает счёт очередей. */
data class QueueSight(val kind: FeedbackKind?, val query: String) {
    /** Параметры запроса: `kind` и непустой `q`. */
    val params: Map<String, String>
        get() = buildMap {
            kind?.let { put("kind", it.name) }
            query.trim().takeIf { it.isNotEmpty() }?.let { put("q", it) }
        }
}

/** Ответ на действие модерации с отчётом: отчёт [report], как он теперь стоит (null - удалён), и свежий счёт очередей [counts]. */
@Serializable data class FeedbackMove(val report: AdminReport? = null, val counts: QueueCounts = QueueCounts())

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
 */
@Serializable enum class MailKind(val written: Boolean = false) {
    SYSTEM,
    ADMIN,
    PLAYER(written = true),
    GUILD(written = true),
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

/** Рассылка гильдии главе или офицеру: кому она уйдёт. */
@Serializable data class GuildMailQuota(val tag: String, val name: String, val emblem: String = "", val color: String = "", val members: Int = 0)

/** Почта героя сейчас (сервер `GET mail/quota`): [gate] - можно ли писать или почему нет; [mute] - немота; [guild] - рассылка, если роль её допускает. */
@Serializable data class MailQuota(val gate: MailGate, val mute: MailMute? = null, val guild: GuildMailQuota? = null)

/** Строка списка игнора: чьи письма герой не принимает и с какого времени (мс эпохи). */
@Serializable data class IgnoredHero(val heroId: String, val name: String, val heroClass: String = "", val at: Long = 0)

/**
 * Письмо администратора: [userId] - одному аккаунту по id (письмо из карточки игрока), он важнее [login]; иначе [login] - одному
 * аккаунту по логину, пусто - всем.
 */
@Serializable data class MailRequest(
    val login: String = "",
    val subject: String,
    val body: String,
    val attachment: MailAttachment = MailAttachment(),
    val userId: String = "",
)

/**
 * Адресат письма администратора из карточки игрока: аккаунт [userId], [name] - как его видит модерация (логин или имя гостя).
 * [login] - логин для письма ([MailRequest.login]): у аккаунта без логина - заведомо несуществующий ([of]), чтобы сервер, что адресует
 * только по логину, отказал, а не нашёл чужой аккаунт с логином - именем героя или разослал письмо всем.
 */
data class MailAddressee(val userId: String, val name: String, val login: String) {
    companion object {
        /** Логина нет - «#» и id аккаунта: в логине такого знака не бывает. */
        fun of(userId: String, login: String, heroName: String) = MailAddressee(userId, login.ifBlank { heroName }, login.ifBlank { "#$userId" })
    }
}
