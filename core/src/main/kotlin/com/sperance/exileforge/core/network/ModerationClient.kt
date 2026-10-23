package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.paging.Page
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement

private const val MODERATION = "api/v1/admin/moderation"
private const val NOTICE = "api/v1/notice"

/** Вид санкции: бан на срок или удаление в корзину; немота - запрет писать письма, игру не закрывает. */
@Serializable enum class SanctionKind { BAN, DELETION, MUTE }

/** На что санкция: герой, аккаунт целиком или устройство. */
@Serializable enum class SanctionTarget { HERO, ACCOUNT, DEVICE }

/**
 * Категория причины - её название из словаря на языке игрока.
 *
 * @property voluntary удаление по воле владельца ([SELF]): место аккаунта свободно сразу, вернуть героя может
 * сам владелец; модерация такую причину не ставит
 */
@Serializable
enum class SanctionCategory(val voluntary: Boolean = false) {
    BOT,
    CHEATS,
    EXPLOIT,
    ABUSE,
    MULTI,
    OTHER,

    /** Самоудаление героя игроком: корзина на `moderation.selfDeleteDays` дней. */
    SELF(voluntary = true),
    ;

    companion object {
        /** Причины, что ставит модерация: без самоудаления. */
        val imposed: List<SanctionCategory> get() = entries.filterNot { it.voluntary }
    }
}

/** Роли аккаунта, как их называет сервер. Тестировщик - не роль аккаунта, а отметка героя ([DossierHero.tester]). */
@Serializable enum class AccountRole { USER, MODERATOR, ADMIN }

/** Новая роль аккаунта [user]: игрок или модератор. */
@Serializable data class RoleRequest(val user: String, val role: AccountRole)

/** Отметка тестировщика героя [hero]: ставит и снимает администратор в досье героя. */
@Serializable data class TesterRequest(val hero: String, val tester: Boolean)

/**
 * Санкция, как её видит модерация: [until] - конец бана или очистки корзины (null - бессрочно), [liftedAt] - снята досрочно,
 * [purgedAt] - удалённое стёрто, [appealed] - апелляция уже подана; [comment] - служебная заметка, [reason] - причина для
 * игрока; [targetId] - цель (герой, аккаунт, устройство), [parentId] - удаление аккаунта, вместе с которым удалён этот герой (пусто -
 * своё удаление: его можно стереть сразу). Игрок видит [SanctionNotice].
 */
@Serializable data class SanctionView(
    val id: String,
    val number: Long,
    val kind: SanctionKind,
    val target: SanctionTarget,
    val label: String,
    val category: SanctionCategory,
    val comment: String = "",
    val byLogin: String,
    val byRole: AccountRole,
    val at: String,
    val until: String? = null,
    val liftedAt: String? = null,
    val liftedBy: String = "",
    val purgedAt: String? = null,
    val appealed: Boolean = false,
    val reason: String = "",
    val targetId: String = "",
    val parentId: String = "",
) {
    val active: Boolean get() = liftedAt == null && purgedAt == null

    /** Самоудаление: герой в корзине по воле владельца - он сам вернёт или сотрёт его. */
    val voluntary: Boolean get() = kind == SanctionKind.DELETION && category.voluntary

    /** Действующее удаление, что лежит в корзине своей строкой - не вместе с аккаунтом: его можно стереть сразу. */
    val erasable: Boolean get() = active && kind == SanctionKind.DELETION && parentId.isEmpty()
}

/**
 * Санкция глазами наказанного (сервер `SanctionNotice`): экран санкции, апелляция, свои герои под санкцией. Вид, срок,
 * категория и [reason] - причина для игрока; заметка модератора, кто наказал и номер санкции - только модерации.
 */
@Serializable data class SanctionNotice(
    val id: String,
    val kind: SanctionKind,
    val target: SanctionTarget,
    val category: SanctionCategory,
    val reason: String = "",
    val at: String,
    val until: String? = null,
    val liftedAt: String? = null,
    val purgedAt: String? = null,
    val appealed: Boolean = false,
) {
    /** Самоудаление: герой в корзине по воле владельца - он сам вернёт или сотрёт его. */
    val voluntary: Boolean get() = kind == SanctionKind.DELETION && category.voluntary

    /** Сколько полных или начатых дней до [until] (конец бана или очистка корзины); null - бессрочно или не прочитать. */
    fun daysLeft(now: Long = System.currentTimeMillis()): Long? = daysUntil(until, now)
}

/** Полных или начатых дней от [now] до метки сервера [until] (UTC); null - бессрочно или не прочитать. */
private fun daysUntil(until: String?, now: Long): Long? = epochOfStamp(until)?.let { ((it - now).coerceAtLeast(0) + DAY_MS - 1) / DAY_MS }

/** Мс эпохи по метке сервера (`LocalDateTime` в UTC); пусто или не читается - null. */
fun epochOfStamp(stamp: String?): Long? = stamp?.let { runCatching { java.time.LocalDateTime.parse(it).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }.getOrNull() }

private const val DAY_MS = 86_400_000L

@Serializable enum class ModerationSegment { ALL, BANNED, TRASH }

/**
 * Аккаунт строки списка модерации: логин, роль, санкция и версия клиента. Присутствие - у героя строки ([ModerationHero.lastSeenAt]).
 *
 * @property protected роль владельца выше смотрящего - действия недоступны (в корзине - не администратор)
 * @property sanction действующая санкция аккаунта - удаление, бан, немота; null - чист
 * @property fate Предначертание аккаунта - код дара `fates.json`; null - не выбрано
 * @property clientVersion версия клиента, на которой он играет; пусто - неизвестна
 * @property heroCount сколько героев сотрёт стирание удалённого аккаунта (строка аккаунта корзины), иначе 0
 */
@Serializable data class ModerationAccount(
    val userId: String,
    val login: String = "",
    val role: AccountRole = AccountRole.USER,
    val protected: Boolean = false,
    val sanction: SanctionView? = null,
    val fate: String? = null,
    val clientVersion: String = "",
    val heroCount: Int = 0,
)

/**
 * Строка списка модерации - герой [hero] и его аккаунт [account]; в корзине без героя - удалённый аккаунт целиком, его удаление -
 * [ModerationAccount.sanction].
 */
@Serializable data class ModerationRow(val account: ModerationAccount, val hero: ModerationHero? = null) {
    /** Ключ строки в списке: удаление, что держит её в корзине, иначе герой (у строки без героя - аккаунт). */
    val key: String get() = deletion?.id ?: hero?.heroId ?: account.userId

    /** Удаление, что держит строку в корзине: героя или аккаунта целиком; null - строка не в корзине. */
    val deletion: SanctionView? get() = (hero?.sanction ?: account.sanction)?.takeIf { it.active && it.kind == SanctionKind.DELETION }
}

/**
 * Запрос списка модерации (маршрут `rows`): раздел [segment], страница [page] героев, поиск [text], порядок [sort], отбор по
 * Предначертанию [fate], чипы «Удалённые» [deleted] и «Старая версия» [outdated] - старее версии этой сборки (её шлёт каждый запрос).
 */
data class ModerationQuery(
    val segment: ModerationSegment = ModerationSegment.ALL,
    val page: Int = 0,
    val text: String = "",
    val sort: ModerationSort = ModerationSort.ACTIVITY,
    val fate: ModerationFate = ModerationFate.All,
    val deleted: Boolean = false,
    val outdated: Boolean = false,
) {
    /** Параметры маршрута. */
    val params: Map<String, String>
        get() = mapOf(
            "segment" to segment.name,
            "page" to page.toString(),
            "q" to text.trim(),
            "sort" to sort.name,
            "fate" to fate.param,
            "deleted" to deleted.toString(),
            "outdated" to outdated.toString(),
        )
}

/**
 * Герой строки списка модерации: [name] - у копии, стёртой владельцем, имя до стирания; [deleted] - в корзине, [sanction] - удаление,
 * что его держит (автором или модерацией), бан или немота героя; [might] - Мощь, null - не посчитана; [lastSeenAt] - последняя команда
 * героя, мс эпохи (0 - неизвестно); [guild] - название гильдии, null - без гильдии; [createdAt] - метка сервера создания и [server] -
 * код игрового сервера, пусто - документа героя нет (корзина).
 */
@Serializable data class ModerationHero(
    val heroId: String,
    val name: String,
    val heroClass: String = "",
    val level: Int = 0,
    val tester: Boolean = false,
    val might: Double? = null,
    val deleted: Boolean = false,
    val sanction: SanctionView? = null,
    val lastSeenAt: Long = 0,
    val guild: String? = null,
    val gold: Long = 0,
    val createdAt: String = "",
    val server: String = "",
)

/** Смена имени героя [hero] модерацией на [name]; [reason] - причина для игрока, обязательна: она уйдёт ему письмом. */
@Serializable data class RenameRequest(val hero: String, val name: String, val reason: String)

/**
 * Отбор списка модерации по Предначертанию, параметр `fate` маршрута `rows`: [param] - его значение, пусто - отбора нет.
 */
sealed interface ModerationFate {
    val param: String

    /** Все герои. */
    data object All : ModerationFate {
        override val param: String = ""
    }

    /** Аккаунт ещё не выбрал дар. */
    data object Fateless : ModerationFate {
        override val param: String = "none"
    }

    /** Аккаунт выбрал дар [code]. */
    data class Chosen(val code: String) : ModerationFate {
        override val param: String get() = code
    }
}

/** Порядок списка модерации: последняя активность, уровень, дата создания, имя. */
@Serializable enum class ModerationSort { ACTIVITY, LEVEL, CREATED, NAME }

/**
 * Что смотрящий может сделать с целью досье: [fate] - сменить её Предначертание (сервер `ModerationPolicy.canRedrawFate`), [mail] -
 * написать игроку от Администрации, [rename] - сменить имя героя досье.
 */
@Serializable data class DossierRights(
    val ban: Boolean = false,
    val banDevice: Boolean = false,
    val delete: Boolean = false,
    val mute: Boolean = false,
    val fate: Boolean = false,
    val mail: Boolean = false,
    val rename: Boolean = false,
)

@Serializable data class DossierAccount(
    val userId: String,
    val login: String,
    val role: AccountRole,
    val registeredAt: String,
    val lastLoginAt: String? = null,
    val clientVersion: String = "",
    val heroes: Int = 0,
    val deleted: Boolean = false,
    /** Предначертание аккаунта - код дара `fates.json`; null - не выбрано. */
    val fate: String? = null,
    /** Когда выбрано Предначертание, мс эпохи; null - не выбрано или выбрано раньше, чем сервер это пишет. */
    val fateAt: Long? = null,
    /**
     * Когда аккаунт был в игре: последнее использование его самой свежей сессии, метка сервера (UTC); null - живых
     * сессий нет. Вход паролем - [lastLoginAt].
     */
    val lastSeenAt: String? = null,
)

@Serializable data class DossierHero(
    val heroId: String,
    val name: String,
    val heroClass: String = "",
    val level: Int = 1,
    val gold: Long = 0,
    val createdAt: String = "",
    val fights: Long = 0,
    val fightHours: Double = 0.0,
    val levelsPerHour: Double? = null,
    val guild: String? = null,
    val deleted: Boolean = false,
    /** Игровой сервер героя - код. */
    val server: String = "MAIN",
    /** Герой - тестировщик: переключатель администратора в досье. */
    val tester: Boolean = false,
    /** Последняя команда героя, мс эпохи; 0 - неизвестно. */
    val lastSeenAt: Long = 0,
    /** Бывшие имена героя, старые первыми. */
    val formerNames: List<String> = emptyList(),
)

@Serializable data class DossierEconomy(val gearValue: Long = 0, val uniques: Int = 0, val mythics: Int = 0, val tradesPerDay: Int = 0)

/** Подозрение досье: `LEVEL_RATE`, `TRADES`, `SHARED_DEVICE` и значение. */
@Serializable data class DossierFlag(val code: String, val value: String)

@Serializable data class DeviceView(val hardware: String, val model: String = "", val clientVersion: String = "", val firstAt: String = "", val lastAt: String = "", val sharedWith: List<String> = emptyList())

@Serializable data class Dossier(
    val account: DossierAccount,
    val hero: DossierHero? = null,
    val economy: DossierEconomy? = null,
    val flags: List<DossierFlag> = emptyList(),
    val devices: List<DeviceView> = emptyList(),
    val heroes: List<DossierHero> = emptyList(),
    val sanctions: List<SanctionView> = emptyList(),
    val appeals: Int = 0,
    val reports: Int = 0,
    val rights: DossierRights = DossierRights(),
) {
    /** Последняя команда цели досье, мс эпохи: у героя - его, у аккаунта - самая свежая из его героев; 0 - неизвестно. */
    val seenAt: Long get() = hero?.lastSeenAt ?: heroes.maxOfOrNull { it.lastSeenAt } ?: 0

    /** Удаление героя досье, что можно стереть сразу ([SanctionView.erasable]); null - герой не в корзине своей строкой или досье аккаунта. */
    val heroDeletion: SanctionView? get() = hero?.let { focus -> sanctions.firstOrNull { it.erasable && it.target == SanctionTarget.HERO && it.targetId == focus.heroId } }
}

/**
 * Действие журнала модерации; [FATE] - смена Предначертания аккаунту: номера санкции нет, в заметке - прежний и новый код дара через
 * « → » ([ModerationEntryView.fateChange]), причина для игрока - [ModerationEntryView.reason]; [RENAME] - модерация сменила имя героя: в
 * заметке - прежнее и новое имя тем же разделителем; [MAIL] - письмо игроку от Администрации из досье: в заметке - тема.
 */
@Serializable enum class ModerationAction { BAN, UNBAN, DELETE, RESTORE, PURGE, MUTE, UNMUTE, FATE, RENAME, MAIL }

@Serializable data class ModerationEntryView(
    val id: String,
    val action: ModerationAction,
    val actorLogin: String = "",
    val actorRole: AccountRole? = null,
    val number: Long,
    val target: SanctionTarget,
    val label: String,
    val category: SanctionCategory,
    val comment: String = "",
    val until: String? = null,
    val at: String,
    /** Причина для игрока: у смены Предначертания чужому аккаунту и имени героя; пусто - нет. */
    val reason: String = "",
    /** Цель строки без санкции: герой смены имени, аккаунт письма; пусто - нет. */
    val targetId: String = "",
) {
    /**
     * Смена дара строки [ModerationAction.FATE]: прежний код (null - дара не было) и новый - из заметки сервера
     * «прежний → новый» (`ModerationEntry.fate`); у прочих действий - null.
     */
    val fateChange: Pair<String?, String>? get() = comment.takeIf { action == ModerationAction.FATE }?.split(FATE_ARROW, limit = 2)
        ?.takeIf { it.size == 2 }?.let { (from, to) -> from.ifBlank { null } to to }
}

/** Разделитель прежнего и нового дара в заметке строки [ModerationAction.FATE]. */
private const val FATE_ARROW = " → "

/** Бан: цель - герой [heroId], аккаунт [userId] или устройство [hardware] аккаунта [userId]; [hours] null - бессрочно. */
@Serializable data class SanctionRequest(
    val target: SanctionTarget,
    val kind: SanctionKind = SanctionKind.BAN,
    val heroId: String = "",
    val userId: String = "",
    val hardware: String = "",
    val hours: Int? = null,
    val category: SanctionCategory = SanctionCategory.OTHER,
    val comment: String = "",
    /** Причина для игрока: её видит наказанный; [comment] - служебная заметка модерации. */
    val reason: String = "",
)

@Serializable data class DeletionRequest(val target: SanctionTarget, val id: String, val category: SanctionCategory, val comment: String, val reason: String = "")

@Serializable private data class AppealBody(val text: String, val token: String)

/** Токен апелляции своей санкции (сервер `POST notice/token`). */
@Serializable private data class AppealToken(val token: String)

/**
 * Модерация: список, досье, бан и его снятие, удаление в корзину и восстановление, журнал - модераторам
 * и администратору. Экран санкции и апелляция ([notice], [appeal]) - без входа: забаненный уже без сессии.
 */
class ModerationClient internal constructor(private val http: Transport) {
    /** Страница строк героев по запросу [query] (всего - [Page.total]); в корзине - строки удалений героев и аккаунтов. */
    suspend fun rows(query: ModerationQuery): Page<ModerationRow> = http.get("$MODERATION/rows", query.params)

    /** Новое имя героя: модератору и администратору по правам досье; ответ - имя, что теперь у героя. */
    suspend fun rename(request: RenameRequest): String = http.post("$MODERATION/rename", body = WireJson.encodeToJsonElement(request))

    /** Письмо игроку от Администрации одному аккаунту по id: модератор - без вложения и не администратору; ответ - сколько писем ушло. */
    suspend fun mail(request: MailRequest): Int = http.post("$MODERATION/mail", body = WireJson.encodeToJsonElement(request))

    /** Досье героя [heroId] или, без героя, аккаунта [userId]. */
    suspend fun dossier(heroId: String, userId: String): Dossier = http.get("$MODERATION/dossier", mapOf("hero" to heroId, "user" to userId))

    suspend fun ban(request: SanctionRequest): SanctionView = http.post("$MODERATION/ban", body = WireJson.encodeToJsonElement(request))

    /** Снимает бан или восстанавливает удалённое. */
    suspend fun lift(sanctionId: String): SanctionView = http.post("$MODERATION/lift", mapOf("id" to sanctionId))

    /** Стирает строку корзины - героя или аккаунт со всеми героями - сразу, не дожидаясь срока: только администратор. */
    suspend fun purge(sanctionId: String): SanctionView = http.post("$MODERATION/purge", mapOf("id" to sanctionId))

    /** Новая роль аккаунта: только администратор, ответ - роль, что теперь у аккаунта. */
    suspend fun setRole(userId: String, role: AccountRole): AccountRole = http.post("$MODERATION/role", body = WireJson.encodeToJsonElement(RoleRequest(userId, role)))

    /** Отметка тестировщика героя [heroId]: только администратор, ответ - отметка, что теперь у героя. */
    suspend fun setTester(heroId: String, tester: Boolean): Boolean = http.post("$MODERATION/tester", body = WireJson.encodeToJsonElement(TesterRequest(heroId, tester)))

    suspend fun delete(request: DeletionRequest): SanctionView = http.post("$MODERATION/delete", body = WireJson.encodeToJsonElement(request))

    /** Страница [page] журнала модерации, новые сверху, с числом записей и признаком, что старше есть; [actor] - действия одного логина. */
    suspend fun log(page: Int, actor: String = ""): Page<ModerationEntryView> = http.get("$MODERATION/log", mapOf("page" to page.toString(), "actor" to actor))

    suspend fun notice(id: String): SanctionNotice = decode(http.request("GET", NOTICE, mapOf("id" to id)))

    /**
     * Апелляция на санкцию [id] - с секретным токеном наказанного: [token] из отказа входа, а без него (герой под санкцией
     * в списке героев - вход есть) токен своей санкции берётся у сервера первым.
     */
    suspend fun appeal(id: String, text: String, token: String? = null): SanctionNotice {
        val secret = token?.takeIf { it.isNotBlank() } ?: http.post<AppealToken>("$NOTICE/token", mapOf("id" to id)).token
        return decode(http.request("POST", "$NOTICE/appeal", mapOf("id" to id), body = WireJson.encodeToJsonElement(AppealBody(text.trim(), secret))))
    }

    private fun decode(json: kotlinx.serialization.json.JsonElement): SanctionNotice = WireJson.decodeFromJsonElement(SanctionNotice.serializer(), json)
}
