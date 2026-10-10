package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement

private const val MODERATION = "api/v1/admin/moderation"
private const val NOTICE = "api/v1/notice"

/** Вид санкции (3.88.5, server 1.80.8): бан на срок или удаление в корзину; немота (4.6.3) - запрет писать письма, игру не закрывает. */
@Serializable enum class SanctionKind { BAN, DELETION, MUTE }

/** На что санкция: герой, аккаунт целиком или устройство. */
@Serializable enum class SanctionTarget { HERO, ACCOUNT, DEVICE }

/**
 * Категория причины - её название из словаря на языке игрока.
 *
 * @property voluntary удаление по воле владельца (сервер 4.5.1, [SELF]): место аккаунта свободно сразу, вернуть героя может
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

/** Роли аккаунта, как их называет сервер. */
@Serializable enum class AccountRole { USER, MODERATOR, ADMIN, TESTER }

/** Новая роль аккаунта [user]: игрок, тестировщик или модератор (3.88.7). */
@Serializable data class RoleRequest(val user: String, val role: AccountRole)

/**
 * Санкция, как её показывают игроку и модератору: [until] - конец бана или очистки корзины (null - бессрочно), [liftedAt] -
 * снята досрочно, [purgedAt] - удалённое стёрто, [appealed] - апелляция уже подана.
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
) {
    val active: Boolean get() = liftedAt == null && purgedAt == null

    /** Самоудаление (4.5.1): герой в корзине по воле владельца - он сам вернёт или сотрёт его. */
    val voluntary: Boolean get() = kind == SanctionKind.DELETION && category.voluntary

    /** Сколько полных или начатых дней до [until] (конец бана или очистка корзины); null - бессрочно или не прочитать. */
    fun daysLeft(now: Long = System.currentTimeMillis()): Long? = until
        ?.let { runCatching { java.time.LocalDateTime.parse(it).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }.getOrNull() }
        ?.let { ((it - now).coerceAtLeast(0) + DAY_MS - 1) / DAY_MS }
}

private const val DAY_MS = 86_400_000L

@Serializable enum class ModerationSegment { ALL, BANNED, TRASH }

/** Строка списка: герой с владельцем; в корзине аккаунт целиком - с пустым [heroId]. [protected] - роль владельца выше смотрящего. */
@Serializable data class ModerationRow(
    val heroId: String,
    val heroName: String,
    val heroClass: String = "",
    val level: Int = 0,
    val userId: String,
    val login: String = "",
    val role: AccountRole = AccountRole.USER,
    val sanction: SanctionView? = null,
    val protected: Boolean = false,
    /** Последняя команда героя (3.94.0, server 1.81.14), мс эпохи; 0 - неизвестно. */
    val lastSeenAt: Long = 0,
    /** Предначертание аккаунта (4.6.3) - код дара `fates.json`; null - не выбрано или сервер старее. */
    val fate: String? = null,
)

/**
 * Отбор списка модерации по Предначертанию (4.6.3), параметр `fate` маршрута `rows`: [param] - его значение, пусто - отбора нет.
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

/** Порядок списка модерации (3.94.0): последняя активность, уровень, дата создания, имя. */
@Serializable enum class ModerationSort { ACTIVITY, LEVEL, CREATED, NAME }

@Serializable data class ModerationPage(val total: Long = 0, val page: Int = 0, val size: Int = 0, val rows: List<ModerationRow> = emptyList())

@Serializable data class DossierRights(val ban: Boolean = false, val banDevice: Boolean = false, val delete: Boolean = false, val mute: Boolean = false)

@Serializable data class DossierAccount(
    val userId: String,
    val login: String,
    val role: AccountRole,
    val registeredAt: String,
    val lastLoginAt: String? = null,
    val clientVersion: String = "",
    val heroes: Int = 0,
    val deleted: Boolean = false,
    /** Предначертание аккаунта (4.6.3) - код дара `fates.json`; null - не выбрано. */
    val fate: String? = null,
    /** Когда выбрано Предначертание (4.6.3), мс эпохи; null - не выбрано или выбрано раньше, чем сервер это пишет. */
    val fateAt: Long? = null,
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
    /** Игровой сервер героя (3.91.0, сервер 1.81.10) - код. */
    val server: String = "MAIN",
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
)

@Serializable enum class ModerationAction { BAN, UNBAN, DELETE, RESTORE, PURGE, MUTE, UNMUTE }

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
)

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
)

@Serializable data class DeletionRequest(val target: SanctionTarget, val id: String, val category: SanctionCategory, val comment: String)

@Serializable private data class AppealBody(val text: String)

/**
 * Модерация (3.88.5, server 1.80.8): список, досье, бан и его снятие, удаление в корзину и восстановление, журнал - модераторам
 * и администратору. Экран санкции и апелляция ([notice], [appeal]) - без входа: забаненный уже без сессии.
 */
class ModerationClient internal constructor(private val http: Transport) {
    /** Страница [page] раздела [segment]: поиск [query], порядок [sort] и отбор по Предначертанию [fate] (4.6.3). */
    suspend fun rows(
        query: String,
        segment: ModerationSegment,
        page: Int,
        sort: ModerationSort = ModerationSort.ACTIVITY,
        fate: ModerationFate = ModerationFate.All,
    ): ModerationPage = http.get("$MODERATION/rows", mapOf("q" to query.trim(), "segment" to segment.name, "page" to page.toString(), "sort" to sort.name, "fate" to fate.param))

    /** Досье героя [heroId] или, без героя, аккаунта [userId]. */
    suspend fun dossier(heroId: String, userId: String): Dossier = http.get("$MODERATION/dossier", mapOf("hero" to heroId, "user" to userId))

    suspend fun ban(request: SanctionRequest): SanctionView = http.post("$MODERATION/ban", body = WireJson.encodeToJsonElement(request))

    /** Снимает бан или восстанавливает удалённое. */
    suspend fun lift(sanctionId: String): SanctionView = http.post("$MODERATION/lift", mapOf("id" to sanctionId))

    /** Новая роль аккаунта (3.88.7, сервер 1.80.9): только администратор, ответ - роль, что теперь у аккаунта. */
    suspend fun setRole(userId: String, role: AccountRole): AccountRole = http.post("$MODERATION/role", body = WireJson.encodeToJsonElement(RoleRequest(userId, role)))

    suspend fun delete(request: DeletionRequest): SanctionView = http.post("$MODERATION/delete", body = WireJson.encodeToJsonElement(request))

    suspend fun log(page: Int, actor: String = ""): List<ModerationEntryView> = http.get("$MODERATION/log", mapOf("page" to page.toString(), "actor" to actor))

    suspend fun notice(id: String): SanctionView = decode(http.request("GET", NOTICE, mapOf("id" to id)))

    suspend fun appeal(id: String, text: String): SanctionView = decode(http.request("POST", "$NOTICE/appeal", mapOf("id" to id), body = WireJson.encodeToJsonElement(AppealBody(text.trim()))))

    private fun decode(json: kotlinx.serialization.json.JsonElement): SanctionView = WireJson.decodeFromJsonElement(SanctionView.serializer(), json)
}
