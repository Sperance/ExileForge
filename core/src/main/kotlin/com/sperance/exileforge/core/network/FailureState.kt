package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.ui

/**
 * Why the server cannot be reached (3.79.0), as a player can act on it: no network on the device, a server too slow
 * to answer, a server restarting or down for maintenance (the proxy answers in its place, or the port is closed),
 * or an answer that is not the game's. The technical text stays for administrators: see [transportDetail].
 */
enum class Outage {
    NO_NETWORK, TIMEOUT, MAINTENANCE, BAD_ANSWER;

    val title: String get() = ui("net.outage.${name.lowercase()}")
    val hint: String get() = ui("net.outage_hint.${name.lowercase()}")

    companion object {
        /** The proxy's statuses: the game itself never answers with these, so the request did not reach it. */
        private val GATEWAY = setOf(502, 503)

        /** The outage behind [error], or null where the server did answer as itself. */
        fun of(error: Throwable): Outage? = when (error) {
            is ApiFailure -> when {
                error.code == null && error.status in GATEWAY -> MAINTENANCE
                error.code == null && error.status == 504 -> TIMEOUT
                error.malformed -> BAD_ANSWER
                else -> null
            }
            is java.net.SocketTimeoutException -> TIMEOUT
            is java.net.ConnectException -> MAINTENANCE
            is java.io.IOException -> NO_NETWORK
            else -> null
        }
    }
}

sealed interface FailureState {
    data class Offline(val cause: Outage) : FailureState
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
            // An unreadable answer to a write that went through may still have done it: that is uncertain, not offline.
            writing && error is ApiFailure && error.malformed && error.status in 200..299 -> UncertainWrite
            error is ApiFailure && Outage.of(error) != null -> Offline(Outage.of(error)!!)
            error is ApiFailure -> when {
                error.status == 401 -> SessionExpired
                error.status == 403 -> Forbidden
                error.status == 409 -> Conflict
                writing && (error.status ?: 500) >= 500 -> UncertainWrite
                else -> Rejected(error.message ?: ui("net.rejected"))
            }
            writing && error is java.io.IOException -> UncertainWrite
            error is java.io.IOException -> Offline(Outage.of(error) ?: Outage.NO_NETWORK)
            else -> Rejected(error.message ?: ui("net.failed"))
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
        ui("net.cleartext")
    error is java.net.UnknownHostException ->
        ui("net.unknown_host", error.message)
    // OkHttp raises the same exception for a dead handshake and a silent server; only the text tells
    // them apart, and they point at opposite things: a dropped SYN is a firewall, not a slow server.
    error is java.net.SocketTimeoutException && error.message?.startsWith("failed to connect") == true ->
        ui("net.no_route", error.message)
    error is java.net.SocketTimeoutException ->
        ui("net.timeout", error.message)
    error is java.net.ConnectException ->
        ui("net.refused", error.message)
    else -> error.message ?: error::class.java.simpleName
}

/**
 * The line a refusal is shown as.
 *
 * A player reads the sentence and nothing else: `HTTP 403 AUTH_004` is not something anyone can act
 * on, an administrator included, so the status and the code stay in the request journal and never
 * reach the strip. The sentence is the server's template for the code in the player's language; a
 * refusal the dictionary has no template for, and whose own text is nothing but that code and its
 * arguments (a rules refusal), reads as the plain "the server rejected the request" instead.
 */
fun refusalLine(error: Throwable): String {
    val code = (error as? ApiFailure)?.code
    val text = if (error is ApiFailure) locError(code, error.message.orEmpty(), error.args) else error.message.orEmpty()
    return when {
        text.isBlank() -> ui("runtime.request_failed")
        !code.isNullOrBlank() && code in text -> ui("net.rejected")
        else -> text
    }
}
