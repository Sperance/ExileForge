package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.model.command.AccountNotice
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.session.Session

/** Кто вошёл, как его называет карточка аккаунта: гость устройства (без логина) или роль аккаунта с логином. */
enum class SealRole { GUEST, PLAYER, MODERATOR, ADMIN }

/**
 * Карточка аккаунта страницы «Вход» и стартового экрана - чистые данные: имя [name], роль [role], логин [login] (у гостя пусто - вход по
 * устройству), героев [heroes] из мест [slots], Предначертание [fate] (код; null - не выбрано), с какого мига аккаунт в игре [since]
 * (null - сервер не прислал) и действующие санкции [sanctions] - немоты и баны героев полосой.
 */
data class AccountSeal(
    val name: String,
    val role: SealRole,
    val login: String,
    val heroes: Int,
    val slots: Int,
    val fate: String?,
    val since: Long?,
    val sanctions: List<AccountNotice>,
) {
    val guest: Boolean get() = role == SealRole.GUEST

    companion object {
        /** Карточка вошедшего аккаунта сессии [session] при [slots] местах на аккаунте; не вошёл - null. */
        fun of(session: Session, slots: Int): AccountSeal? {
            val profile = session.profile?.takeIf { session.signedIn } ?: return null
            val role = when {
                profile.login.isBlank() -> SealRole.GUEST
                profile.role == AccountRole.ADMIN.name -> SealRole.ADMIN
                profile.role == AccountRole.MODERATOR.name -> SealRole.MODERATOR
                else -> SealRole.PLAYER
            }
            val heroes = if (session.charactersRead) session.slotHolders else profile.countCharacters
            return AccountSeal(session.title, role, profile.login, heroes, slots, profile.fate, profile.registeredAt.takeIf { it > 0 }, profile.sanctions)
        }
    }
}
