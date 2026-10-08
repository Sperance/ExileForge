package com.sperance.exileforge.presentation.app

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.FlushOutcome
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.Outage
import com.sperance.exileforge.core.network.transportDetail
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.Actions
import com.sperance.exileforge.presentation.Repositories
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.NoticeKind
import com.sperance.exileforge.presentation.state.TAB_CITY
import com.sperance.exileforge.presentation.state.TAB_CRAFTS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The link to the server (3.30.0): whether it is reachable, and the commands waiting for it.
 *
 * A lost connection is not a refusal: it shows as an icon in the top bar, not as the red strip, and the
 * server is asked again by itself — after [BACKOFF_S] seconds, the last step repeating — until it answers.
 * Then the waiting commands go out in order, and the screen the player is on is read again.
 */
class ConnectionActions(
    repositories: Repositories,
    actions: Actions,
    commands: CommandRunner,
    connection: ServerConnection,
    store: ServerStore,
    scope: CoroutineScope,
    private val navigator: Navigator,
    private val notices: Notices,
    private val lazySession: Lazy<SessionActions>,
    private val lazyCharacters: Lazy<CharacterActions>,
) : AppService(repositories, actions, commands, connection, store, scope),
    com.sperance.exileforge.core.session.ConnectionEvents {
    private val characterActions: CharacterActions get() = lazyCharacters.value
    private val sessionActions: SessionActions get() = lazySession.value
    private var watcher: Job? = null
    private var loop: Job? = null

    /** A wake for the running loop: it cuts the backoff pause or asks for one more pass — never the send in flight. */
    private val nudge = Channel<Unit>(Channel.CONFLATED)

    /** Связь нужно проверить, даже если она числится живой и ничего не ждёт (3.95.2): возврат из фона, вернувшаяся сеть. */
    @Volatile private var verifying = false

    /** A new [GameApi]: its queue is read from the device and drawn in the top bar as it changes. */
    fun attach(api: GameApi) {
        run {
            watcher?.cancel()
            val queue = api.commands ?: return
            watcher = scope.launch {
                val expired = queue.load()
                if (expired.isNotEmpty()) notices.toast(ui("link.expired", expired.size), NoticeKind.DONE)
                // What the last launch left waiting goes out once there is a session to send it with.
                if (queue.waiting.value.isNotEmpty()) wake()
                queue.waiting.collect { waiting -> links.update { it.copy(waiting = waiting) } }
            }
        }
    }

    /** The network failed under a read or a command: the icon goes up with [error]'s cause, and the server is asked again. */
    override fun lost(error: Throwable?) {
        links.update { it.down(error) }
        wake()
    }

    /** A command joined the queue: it goes out as soon as the server can be reached. */
    override fun queued() {
        notices.toast(ui(if (links.state.value.offline) "link.queued" else "link.queued_later"), NoticeKind.DONE)
        wake()
    }

    /**
     * Сеть устройства вернулась (3.95.2): сокеты прежней сети мертвы, связь проверяется сразу, без паузы повтора.
     */
    fun networkBack() {
        ForgeHttp.dropIdleConnections()
        wake(now = true, verify = true)
    }

    /**
     * The probe loop, one at a time: an offline link waits its step of the backoff first, an online one with
     * commands waiting sends them at once. It ends once the server answered and nothing waits. [now] skips the
     * pause the loop is in — the app came back to the foreground, or the session was just confirmed. A running
     * loop is only nudged: cancelling it could cut a command mid-flight and send its key again.
     */
    fun wake(now: Boolean = false, verify: Boolean = false) {
        run {
            if (verify) verifying = true
            if (loop?.isActive == true) {
                if (now) nudge.trySend(Unit)
                return
            }
            nudge.tryReceive()
            loop = scope.launch {
                var step = 0
                var first = now
                while (true) {
                    // The queue itself, not its reflection in the state, which may lag a frame behind a command just added.
                    val offline = links.state.value.offline
                    if (!offline && !verifying && api.commands?.waiting?.value.isNullOrEmpty()) break
                    // Толчок (возврат в игру, сеть вернулась, «Повторить») - новый повод: пауза повтора снова с первой ступени
                    if (offline && !first && withTimeoutOrNull(BACKOFF_S[step.coerceAtMost(BACKOFF_S.lastIndex)] * 1_000L) { nudge.receive() } != null) step = 0
                    first = false
                    val probe = probe()
                    verifying = false
                    if (probe != null) {
                        links.update { it.down(probe) }
                        step++
                        continue
                    }
                    val wasOffline = links.state.value.offline
                    links.update { it.up() }
                    if (wasOffline) sessionActions.restored()
                    var delivered = 0
                    var foreign = 0
                    val outcome = try {
                        api.flushCommands(
                            expired = { notices.toast(ui("link.expired", it.size), NoticeKind.DONE) },
                            refused = { _, e -> commands.report(e, writing = true) },
                            delivered = { delivered++ },
                            foreign = { foreign++ },
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        commands.report(e, writing = true)
                        FlushOutcome.EMPTY
                    }
                    if (foreign > 0) notices.toast(ui("link.foreign", foreign), NoticeKind.DONE)
                    if (wasOffline || delivered > 0) refreshScreen()
                    when (outcome) {
                        FlushOutcome.OFFLINE -> {
                            links.update { it.down(null) }
                            step++
                        }

                        // No session to send them with: the sign-in wakes the loop again — or already did, during this pass.
                        FlushOutcome.SIGNED_OUT, FlushOutcome.EMPTY -> if (nudge.tryReceive().isSuccess) first = true else break
                    }
                }
            }
        }
    }

    /**
     * The probe: the health route, cheap and unauthenticated. Any answer of the game — even a refusal — means the
     * server is there; null then, else what stood in the way (a proxy's 502 is the server restarting, not an answer).
     */
    private suspend fun probe(): Throwable? = try {
        // Сервер только что ответил настоящим запросом (3.94.1): проверять связь ещё раз незачем.
        if (links.state.value.offline || System.currentTimeMillis() - api.answeredAt > ANSWER_FRESH_MS) {
            // Проба коротка (3.95.2): ответа нет за [PROBE_MS] - связи нет, следующая проба - по паузе повтора
            withTimeoutOrNull(PROBE_MS) { api.health() } ?: throw java.net.SocketTimeoutException("probe")
        }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiFailure) {
        e.takeIf { Outage.of(it) != null }
    } catch (e: Exception) {
        e
    }

    /** What the player looks at, read again quietly: the hero always, and the tab's own data where it has any. */
    fun refreshScreen() {
        run {
            val now = navigator.current.value
            when (now.phase) {
                AppPhase.AUTH -> Unit

                AppPhase.CHARACTERS -> read(Reads.CHARACTERS, silent = true) { characterActions.readCharacters() }

                AppPhase.GAME -> {
                    read(Reads.HERO, silent = true) { heroSync.readHero() }
                    when (now.tab) {
                        TAB_CRAFTS -> crafts.load(silent = true)

                        TAB_CITY -> when (now.building) {
                            Building.QUESTS -> quests.load()
                            Building.AUCTION -> market.loadAuction()
                            Building.GUILD -> guild.load()
                            Building.MERCHANT, Building.HISTORY, Building.CHRONICLE, null -> Unit
                        }
                    }
                }
            }
        }
    }

    /** A new session or a sign-out: nothing of the old link carries over but the queue on disk. */
    fun reset() {
        loop?.cancel()
        links.update { it.copy(offline = false) }
    }

    private companion object {
        /** The pauses between probes, in seconds; the last one repeats. */
        val BACKOFF_S = longArrayOf(2, 4, 8, 16, 30)

        /** Сколько ждёт проба связи, мс. */
        const val PROBE_MS = 4_000L

        /** Ответ сервера моложе этого заменяет проверку связи (3.94.1). */
        const val ANSWER_FRESH_MS = 10_000L
    }
}
