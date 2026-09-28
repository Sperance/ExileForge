package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.party.PartyMessage
import com.sperance.exileforge.rules.party.PartyFrame
import com.sperance.exileforge.rules.party.PartyLaunch
import com.sperance.exileforge.rules.party.PartyView
import com.sperance.exileforge.rules.party.PartyWire
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val PARTY = "api/v1/party"

/**
 * Co-op (3.25.0, server 1.23.0): the lobby a host gathers before a zone — by a code, or among the guild —
 * the host's start that takes everyone in, and the socket the heroes talk over while the run lasts.
 */
class PartyClient internal constructor(private val http: Transport) {
    suspend fun current(heroId: String): PartyView? = http.get(PARTY, heroQuery(heroId))
    suspend fun guild(heroId: String): List<PartyView> = http.get("$PARTY/guild", heroQuery(heroId))
    suspend fun launch(heroId: String): PartyLaunch? = http.get("$PARTY/launch", heroQuery(heroId))
    suspend fun create(heroId: String, mapCode: String, itemId: String?): PartyView = http.post("$PARTY/create", heroQuery(heroId, "mapCode" to mapCode, "itemId" to itemId))
    suspend fun join(heroId: String, code: String): PartyView = http.post("$PARTY/join", heroQuery(heroId, "code" to code.trim().uppercase()))
    suspend fun leave(heroId: String) { http.request("POST", "$PARTY/leave", heroQuery(heroId), authenticated = true) }
    suspend fun kick(heroId: String, memberId: String) { http.request("POST", "$PARTY/kick", heroQuery(heroId, "memberId" to memberId), authenticated = true) }
    /** The host sets out: never retried — a repeat would find the party already on its way. */
    suspend fun start(heroId: String): PartyLaunch = http.post("$PARTY/start", heroQuery(heroId))

    /** The lobby's socket for [heroId]: it opens at once and opens again after a drop until it is closed. */
    fun socket(heroId: String, scope: CoroutineScope, onFrame: (PartyFrame) -> Unit, onLink: (Boolean) -> Unit = {}): PartySocket =
        PartySocket({ listener -> http.socket("$PARTY/ws", heroQuery(heroId), listener) }, scope, onFrame, onLink).also { it.open() }
}

/**
 * One hero's link to the lobby: frames in to [onFrame], messages out by [send]. A dropped link is opened again
 * after a pause that doubles up to a ceiling; the server keeps the hero's place for its reconnect window meanwhile.
 */
class PartySocket internal constructor(
    private val connect: (WebSocketListener) -> WebSocket,
    private val scope: CoroutineScope,
    private val onFrame: (PartyFrame) -> Unit,
    private val onLink: (Boolean) -> Unit,
) {
    @Volatile private var socket: WebSocket? = null
    @Volatile private var closed = false
    @Volatile var online = false
        private set
    private var failures = 0

    internal fun open() { if (!closed) socket = connect(listener) }

    /** Sends [message] to the host — or, from the host, to guest [to] or to all; false when the link is down. */
    fun send(message: PartyMessage, to: String? = null): Boolean =
        socket?.takeIf { online }?.send(PartyWire.encode(PartyFrame.Send(PartyMessage.encode(message), to))) ?: false

    fun close() {
        closed = true
        socket?.close(NORMAL, null)
        socket = null
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) { failures = 0; online = true; onLink(true) }
        override fun onMessage(webSocket: WebSocket, text: String) { PartyWire.decode(text)?.let(onFrame) }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(NORMAL, null) }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = dropped(webSocket)
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = dropped(webSocket)
    }

    private fun dropped(webSocket: WebSocket) {
        if (socket !== webSocket) return
        online = false
        onLink(false)
        if (closed) return
        val pause = (FIRST_RETRY shl failures.coerceAtMost(DOUBLINGS)).coerceAtMost(LONGEST)
        failures++
        scope.launch { delay(pause); open() }
    }

    private companion object {
        const val NORMAL = 1000
        const val FIRST_RETRY = 1_000L
        const val DOUBLINGS = 4
        const val LONGEST = 15_000L
    }
}
