package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.tr

sealed interface FailureState {
    data object Offline : FailureState
    data object SessionExpired : FailureState
    data object Conflict : FailureState
    data object Forbidden : FailureState
    data object UncertainWrite : FailureState
    data class Rejected(val message: String) : FailureState
    companion object {
        /**
         * An [ApiFailure] is an answer, not a lost connection.
         *
         * It carries an HTTP status and the server's own message, so it is classified on its own
         * before the transport branches — it extends IOException, and letting it fall through would
         * report every 4xx as "offline" and hide what the server actually said.
         */
        fun from(error: Exception, writing: Boolean): FailureState = when {
            error is ApiFailure -> when {
                error.status == 401 -> SessionExpired
                error.status == 403 -> Forbidden
                error.status == 409 -> Conflict
                writing && (error.status ?: 500) >= 500 -> UncertainWrite
                else -> Rejected(error.message ?: tr("Сервер отклонил запрос", "The server rejected the request"))
            }
            writing && error is java.io.IOException -> UncertainWrite
            error is java.io.IOException -> Offline
            else -> Rejected(error.message ?: tr("Не удалось выполнить запрос", "The request could not be completed"))
        }
    }
}
