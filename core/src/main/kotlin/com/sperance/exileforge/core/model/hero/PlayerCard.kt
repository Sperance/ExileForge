package com.sperance.exileforge.core.model.hero

import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.rules.content.GuildRole
import kotlinx.serialization.Serializable

/** Гильдия на карточке игрока (сервер 4.5.1): кто она и кем в ней герой. */
@Serializable
data class CardGuild(val id: String, val name: String, val tag: String, val emblem: String = "", val color: String = "", val role: GuildRole = GuildRole.MEMBER)

/**
 * Что модератор может сделать с героем карточки (сервер 4.5.1, по `ModerationPolicy` сервера): бан героя, аккаунта, устройства
 * и удаление. [userId] и [login] - аккаунт владельца: досье, письмо администратора.
 */
@Serializable
data class CardModeration(
    val userId: String,
    val login: String = "",
    val banHero: Boolean = false,
    val banAccount: Boolean = false,
    val banDevice: Boolean = false,
    val delete: Boolean = false,
) {
    /** Есть ли хоть одно действие модерации. */
    val any: Boolean get() = banHero || banAccount || banDevice || delete
}

/**
 * Что смотрящий может сделать с героем карточки (сервер 4.5.1): [mail] - написать письмо (пишет только администратор, по логину
 * из [moderation]), [invite] - позвать в свою гильдию, [moderation] - только модератору и администратору. Права считает сервер,
 * клиент лишь прячет недоступное.
 */
@Serializable
data class CardRights(val mail: Boolean = false, val invite: Boolean = false, val moderation: CardModeration? = null)

/**
 * Карточка игрока (сервер 4.5.1, `GET hero/card`): открытый профиль героя глазами героя смотрящего, на его игровом сервере.
 *
 * @property title код титула летописи; пусто - без титула
 * @property league номер лиги Разлома по уровню героя (граница - `rift.leagues`)
 * @property online последняя команда героя свежая; [lastSeenAt] - её время (мс эпохи, 0 - неизвестно)
 * @property lots сколько лотов героя на витрине аукциона
 * @property fate Предначертание аккаунта героя (4.6.0); null - аккаунт его ещё не выбрал
 */
@Serializable
data class PlayerCard(
    val heroId: String,
    val name: String,
    val heroClass: String = "",
    val level: Int = 1,
    val title: String = "",
    val league: Int = 0,
    val guild: CardGuild? = null,
    val online: Boolean = false,
    val lastSeenAt: Long = 0,
    val lots: Int = 0,
    val rights: CardRights = CardRights(),
    val fate: FateCard? = null,
)

/** Что станет с гильдией после ухода героя (сервер 4.5.1): он просто выходит, главенство переходит наследнику или гильдия распускается. */
@Serializable
enum class GuildOutcome { LEAVE, TRANSFER, DISBAND }

/**
 * Гильдия героя перед его уходом (сервер 4.5.1): [role] - его роль, [outcome] - что станет с гильдией; при
 * [GuildOutcome.TRANSFER] - наследник главенства [heirId]/[heirName] и его нынешняя роль [heirRole].
 */
@Serializable
data class GuildDeparture(
    val guildId: String,
    val name: String,
    val tag: String = "",
    val role: GuildRole = GuildRole.MEMBER,
    val outcome: GuildOutcome = GuildOutcome.LEAVE,
    val heirId: String? = null,
    val heirName: String? = null,
    val heirRole: GuildRole? = null,
)

/**
 * Последствия самоудаления героя (сервер 4.5.1, `GET hero/deletion/preview`) до подтверждения: [purgeDays] - сколько дней его
 * можно вернуть, [guild] - что станет с его гильдией (null - вне гильдии), [lots] - сколько лотов снимется и вернётся письмом.
 */
@Serializable
data class DeletionPreview(val heroId: String, val name: String, val purgeDays: Int, val guild: GuildDeparture? = null, val lots: Int = 0)

/** Подтверждение самоудаления или стирания: имя героя, как его ввёл игрок (сервер сверяет без учёта регистра). */
@Serializable
data class DeletionConfirm(val name: String)
