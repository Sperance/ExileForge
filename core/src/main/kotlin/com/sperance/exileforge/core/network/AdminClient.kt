package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.fate.FateView
import kotlinx.serialization.Serializable

private const val TESTERS = "api/v1/admin/testers"
private const val ADMIN = "api/v1/admin"
private const val SERVERS = "api/v1/gameserver"

/** Игровой сервер: мир героев; [code] несут герой, лот и гильдия, [name] - как назвал администратор. */
@Serializable data class GameServer(val code: String, val name: String = "")

/** Итог переноса героя: куда, вышел ли он из гильдии и сколько лотов вернулось письмами. */
@Serializable data class HeroMove(val heroId: String, val name: String = "", val server: String, val leftGuild: Boolean = false, val lotsReturned: Int = 0)

/** A tester's account as the administrator sees it; [password] comes only with a new account or a reset. */
@Serializable data class TesterAccount(val id: String, val login: String, val active: Boolean, val lastLogin: String? = null, val password: String? = null)

/** The administrator's accounts window: the testers, a new one, a new password, switching one off. */
class AdminClient internal constructor(private val http: Transport) {

    /** Новый аккаунт [login] с ролью [role] - тестировщик или модератор; пароль - в ответе один раз. */
    suspend fun createTester(login: String, role: AccountRole = AccountRole.USER): TesterAccount {
        require(login.isNotBlank()) { com.sperance.exileforge.core.i18n.ui("api.credentials") }
        return http.post(TESTERS, mapOf("login" to login.trim(), "role" to role.name))
    }

    /** Все игровые серверы: основной есть всегда. */
    suspend fun servers(): List<GameServer> = http.get(SERVERS)

    /** Новый сервер с кодом [code] и названием [name]. */
    suspend fun createServer(code: String, name: String): GameServer {
        require(code.isNotBlank() && name.isNotBlank()) { com.sperance.exileforge.core.i18n.ui("admin.server_required") }
        return http.post("$ADMIN/servers", mapOf("code" to code.trim(), "name" to name.trim()))
    }

    /** Переносит героя [heroId] на сервер [server]: он выходит из гильдии, его лоты возвращаются письмами. */
    suspend fun moveHero(heroId: String, server: String): HeroMove = http.post("$ADMIN/hero/server", mapOf("heroId" to heroId, "server" to server))

    /**
     * Предначертание [code] аккаунту [user] (сервер `POST admin/fate`): свой аккаунт - без причины; чужому - с причиной [reason]
     * для игрока (пустая - `MOD_012`), игрок получит письмо, чужой администратор - `MOD_001`. Любой дар контента; счётчики
     * прежнего дара у героев с нуля, начатый заход доигрывает со старым. Неизвестный дар - `FT_006`.
     */
    suspend fun redrawFate(code: String, user: String, reason: String = ""): FateView = http.post("$ADMIN/fate", mapOf("code" to code, "user" to user, "reason" to reason.trim()))
}
