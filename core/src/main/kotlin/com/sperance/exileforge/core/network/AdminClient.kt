package com.sperance.exileforge.core.network

import kotlinx.serialization.Serializable

private const val TESTERS = "api/v1/admin/testers"
private const val HEROES = "api/v1/admin/heroes"

/** A tester's account as the administrator sees it (3.73.0); [password] comes only with a new account or a reset. */
@Serializable data class TesterAccount(val id: String, val login: String, val active: Boolean, val lastLogin: String? = null, val password: String? = null)

/** One hero of every account as the administrator sees it (3.81.0, server 1.76.0). */
@Serializable data class AdminHeroRow(
    val id: String,
    val name: String,
    val heroClass: String,
    val level: Int,
    val createdAt: String,
    val userId: String,
    val login: String,
    val blocked: Boolean = false,
    val blockReason: String = "",
)

/** A page of the heroes under a search: [total] of them, [heroes] the page [page] of [size]. */
@Serializable data class AdminHeroPage(val total: Long = 0, val page: Int = 0, val size: Int = 0, val heroes: List<AdminHeroRow> = emptyList())

/** The order of the heroes' list: the highest, the newest, or by name. */
enum class AdminHeroSort { LEVEL, CREATED, NAME }

/** The administrator's accounts window (3.73.0, server 1.69.0): the testers, a new one, a new password, switching one off. */
class AdminClient internal constructor(private val http: Transport) {
    suspend fun testers(): List<TesterAccount> = http.get(TESTERS)

    suspend fun createTester(login: String): TesterAccount {
        require(login.isNotBlank()) { com.sperance.exileforge.core.i18n.ui("api.credentials") }
        return http.post(TESTERS, mapOf("login" to login.trim()))
    }

    suspend fun resetTester(id: String): TesterAccount = http.post("$TESTERS/reset", mapOf("userId" to id))

    suspend fun setTesterActive(id: String, active: Boolean): TesterAccount = http.post("$TESTERS/active", mapOf("userId" to id, "active" to active.toString()))

    /** Every hero (3.81.0, server 1.76.0): searched by name, in the order asked, a page at a time. */
    suspend fun heroes(query: String, sort: AdminHeroSort, page: Int, size: Int): AdminHeroPage = http.get(HEROES, mapOf("q" to query.trim(), "sort" to sort.name, "page" to page.toString(), "size" to size.toString()))

    /** A block with its reason, or its lifting; the server answers with the hero's row as it now stands. */
    suspend fun blockHero(heroId: String, blocked: Boolean, reason: String): AdminHeroRow = http.post("$HEROES/block", mapOf("heroId" to heroId, "blocked" to blocked.toString(), "reason" to reason.trim()))
}
