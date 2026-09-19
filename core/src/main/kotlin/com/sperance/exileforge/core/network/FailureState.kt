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

/**
 * What a transport failure actually was.
 *
 * "No connection" sends the reader to look at their Wi-Fi, and that is almost never where the
 * problem is on a developer machine: the exception already names it — a blocked cleartext socket,
 * a refused port, a name that does not resolve — so it is shown instead of being swallowed.
 */
fun transportDetail(error: Throwable): String = when {
    // Android blocks plain HTTP unless the manifest allows it; only the debug build does.
    error is java.net.UnknownServiceException || error.message?.contains("CLEARTEXT", ignoreCase = true) == true ->
        tr("открытый HTTP запрещён политикой сети приложения. Установите debug-сборку или используйте HTTPS",
           "cleartext HTTP is blocked by the app's network policy. Install the debug build or use HTTPS")
    error is java.net.UnknownHostException ->
        tr("имя хоста не разрешается: ${error.message}", "the host name does not resolve: ${error.message}")
    error is java.net.SocketTimeoutException ->
        tr("сервер не ответил вовремя: ${error.message}", "the server did not answer in time: ${error.message}")
    error is java.net.ConnectException ->
        tr("порт не принимает соединение: ${error.message}", "the port refused the connection: ${error.message}")
    else -> error.message ?: error::class.java.simpleName
}
