package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement

private const val MODERATION = "api/v1/admin/moderation"
private const val NOTICE = "api/v1/notice"

/** Вид санкции (3.88.5, server 1.80.8): бан на срок или удаление в корзину. */
@Serializable enum class SanctionKind { BAN, DELETION }

/** На что санкция: герой, аккаунт целиком или устройство. */
@Serializable enum class SanctionTarget { HERO, ACCOUNT, DEVICE }

/** Категория причины - её название из словаря на языке игрока. */
@Serializable enum class SanctionCategory { BOT, CHEATS, EXPLOIT, ABUSE, MULTI, OTHER }

/** Роли аккаунта, как их называет сервер. */
@Serializable enum class AccountRole { USER, MODERATOR, ADMIN, TESTER }

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
}

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
)

@Serializable data class ModerationPage(val total: Long = 0, val page: Int = 0, val size: Int = 0, val rows: List<ModerationRow> = emptyList())

@Serializable data class DossierRights(val ban: Boolean = false, val banDevice: Boolean = false, val delete: Boolean = false)

@Serializable data class DossierAccount(
    val userId: String,
    val login: String,
    val role: AccountRole,
    val registeredAt: String,
    val lastLoginAt: String? = null,
    val clientVersion: String = "",
    val heroes: Int = 0,
    val deleted: Boolean = false,
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

@Serializable enum class ModerationAction { BAN, UNBAN, DELETE, RESTORE, PURGE }

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
    suspend fun rows(query: String, segment: ModerationSegment, page: Int): ModerationPage = http.get("$MODERATION/rows", mapOf("q" to query.trim(), "segment" to segment.name, "page" to page.toString()))

    /** Досье героя [heroId] или, без героя, аккаунта [userId]. */
    suspend fun dossier(heroId: String, userId: String): Dossier = http.get("$MODERATION/dossier", mapOf("hero" to heroId, "user" to userId))

    suspend fun ban(request: SanctionRequest): SanctionView = http.post("$MODERATION/ban", body = WireJson.encodeToJsonElement(request))

    /** Снимает бан или восстанавливает удалённое. */
    suspend fun lift(sanctionId: String): SanctionView = http.post("$MODERATION/lift", mapOf("id" to sanctionId))

    suspend fun delete(request: DeletionRequest): SanctionView = http.post("$MODERATION/delete", body = WireJson.encodeToJsonElement(request))

    suspend fun log(page: Int, actor: String = ""): List<ModerationEntryView> = http.get("$MODERATION/log", mapOf("page" to page.toString(), "actor" to actor))

    suspend fun notice(id: String): SanctionView = decode(http.request("GET", NOTICE, mapOf("id" to id)))

    suspend fun appeal(id: String, text: String): SanctionView = decode(http.request("POST", "$NOTICE/appeal", mapOf("id" to id), body = WireJson.encodeToJsonElement(AppealBody(text.trim()))))

    private fun decode(json: kotlinx.serialization.json.JsonElement): SanctionView = WireJson.decodeFromJsonElement(SanctionView.serializer(), json)
}
