package com.sperance.exileforge.core.network

import kotlinx.serialization.Serializable

private const val TESTERS = "api/v1/admin/testers"

/** A tester's account as the administrator sees it (3.73.0); [password] comes only with a new account or a reset. */
@Serializable data class TesterAccount(val id: String, val login: String, val active: Boolean, val lastLogin: String? = null, val password: String? = null)

/** The administrator's accounts window (3.73.0, server 1.69.0): the testers, a new one, a new password, switching one off. */
class AdminClient internal constructor(private val http: Transport) {
    suspend fun testers(): List<TesterAccount> = http.get(TESTERS)

    suspend fun createTester(login: String): TesterAccount {
        require(login.isNotBlank()) { com.sperance.exileforge.core.i18n.ui("api.credentials") }
        return http.post(TESTERS, mapOf("login" to login.trim()))
    }

    suspend fun resetTester(id: String): TesterAccount = http.post("$TESTERS/reset", mapOf("userId" to id))

    suspend fun setTesterActive(id: String, active: Boolean): TesterAccount = http.post("$TESTERS/active", mapOf("userId" to id, "active" to active.toString()))
}
