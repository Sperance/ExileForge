package com.sperance.exileforge.core.network

sealed interface FailureState {
    data object Offline : FailureState
    data object SessionExpired : FailureState
    data object Conflict : FailureState
    data object Forbidden : FailureState
    data object UncertainWrite : FailureState
    data class Rejected(val message: String) : FailureState
    companion object {
        fun from(error: Exception, writing: Boolean): FailureState = when {
            error is ApiFailure && error.status == 401 -> SessionExpired
            error is ApiFailure && error.status == 403 -> Forbidden
            error is ApiFailure && error.status == 409 -> Conflict
            writing && (error is java.io.IOException || error is ApiFailure && (error.status ?: 500) >= 500) -> UncertainWrite
            error is java.io.IOException -> Offline
            else -> Rejected(error.message ?: "Не удалось выполнить запрос")
        }
    }
}
