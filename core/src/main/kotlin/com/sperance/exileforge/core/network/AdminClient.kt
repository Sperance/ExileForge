package com.sperance.exileforge.core.network

import kotlinx.serialization.Serializable

private const val TESTERS = "api/v1/admin/testers"
private const val ADMIN = "api/v1/admin"
private const val SERVERS = "api/v1/gameserver"

/** Игровой сервер (3.91.0, сервер 1.81.10): мир героев; [code] несут герой, лот и гильдия, [name] - как назвал администратор. */
@Serializable data class GameServer(val code: String, val name: String = "")

/** Итог переноса героя (3.91.0): куда, вышел ли он из гильдии и сколько лотов вернулось письмами. */
@Serializable data class HeroMove(val heroId: String, val name: String = "", val server: String, val leftGuild: Boolean = false, val lotsReturned: Int = 0)

/** A tester's account as the administrator sees it (3.73.0); [password] comes only with a new account or a reset. */
@Serializable data class TesterAccount(val id: String, val login: String, val active: Boolean, val lastLogin: String? = null, val password: String? = null)

/** The administrator's accounts window (3.73.0, server 1.69.0): the testers, a new one, a new password, switching one off. */
class AdminClient internal constructor(private val http: Transport) {

    /** Новый аккаунт [login] с ролью [role] - тестировщик или модератор (3.88.8, сервер 1.80.11); пароль - в ответе один раз. */
    suspend fun createTester(login: String, role: AccountRole = AccountRole.TESTER): TesterAccount {
        require(login.isNotBlank()) { com.sperance.exileforge.core.i18n.ui("api.credentials") }
        return http.post(TESTERS, mapOf("login" to login.trim(), "role" to role.name))
    }

    /** Все игровые серверы (3.91.0): основной есть всегда. */
    suspend fun servers(): List<GameServer> = http.get(SERVERS)

    /** Новый сервер с кодом [code] и названием [name] (3.91.0). */
    suspend fun createServer(code: String, name: String): GameServer {
        require(code.isNotBlank() && name.isNotBlank()) { com.sperance.exileforge.core.i18n.ui("admin.server_required") }
        return http.post("$ADMIN/servers", mapOf("code" to code.trim(), "name" to name.trim()))
    }

    /** Переносит героя [heroId] на сервер [server] (3.91.0): он выходит из гильдии, его лоты возвращаются письмами. */
    suspend fun moveHero(heroId: String, server: String): HeroMove = http.post("$ADMIN/hero/server", mapOf("heroId" to heroId, "server" to server))
}
