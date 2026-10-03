package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.FlushOutcome
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.Outage
import com.sperance.exileforge.core.network.transportDetail
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.LinkState
import com.sperance.exileforge.presentation.state.NoticeKind
import com.sperance.exileforge.presentation.state.TAB_CITY
import com.sperance.exileforge.presentation.state.TAB_CRAFTS
import kotlinx.coroutines.CancellationException
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
class ConnectionViewModel(runtime: ForgeRuntime) :
    FeatureViewModel(runtime),
    com.sperance.exileforge.core.session.ConnectionEvents {
    private var watcher: Job? = null
    private var loop: Job? = null

    /** A wake for the running loop: it cuts the backoff pause or asks for one more pass — never the send in flight. */
    private val nudge = Channel<Unit>(Channel.CONFLATED)

    /** A new [GameApi]: its queue is read from the device and drawn in the top bar as it changes. */
    fun attach(api: GameApi) {
        with(runtime) {
            watcher?.cancel()
            val queue = api.commands ?: return
            watcher = scope.launch {
                val expired = queue.load()
                if (expired.isNotEmpty()) toast(ui("link.expired", expired.size), NoticeKind.DONE)
                // What the last launch left waiting goes out once there is a session to send it with.
                if (queue.waiting.value.isNotEmpty()) wake()
                queue.waiting.collect { waiting -> update { it.copy(link = it.link.copy(waiting = waiting)) } }
            }
        }
    }

    /** The network failed under a read or a command: the icon goes up with [error]'s cause, and the server is asked again. */
    override fun lost(error: Throwable?) {
        update { it.copy(link = it.link.down(error)) }
        wake()
    }

    /** The link marked down by [error] (3.79.0): its outage for the player, the transport's words for an administrator. */
    private fun LinkState.down(error: Throwable?): LinkState = copy(offline = true, outage = error?.let(Outage::of) ?: outage ?: Outage.NO_NETWORK, detail = error?.let(::transportDetail) ?: detail)

    /** A command joined the queue: it goes out as soon as the server can be reached. */
    override fun queued() {
        runtime.toast(ui(if (state.value.link.offline) "link.queued" else "link.queued_later"), NoticeKind.DONE)
        wake()
    }

    /**
     * The probe loop, one at a time: an offline link waits its step of the backoff first, an online one with
     * commands waiting sends them at once. It ends once the server answered and nothing waits. [now] skips the
     * pause the loop is in — the app came back to the foreground, or the session was just confirmed. A running
     * loop is only nudged: cancelling it could cut a command mid-flight and send its key again.
     */
    fun wake(now: Boolean = false) {
        with(runtime) {
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
                    val offline = state.value.link.offline
                    if (!offline && api.commands?.waiting?.value.isNullOrEmpty()) break
                    if (offline && !first) withTimeoutOrNull(BACKOFF_S[step.coerceAtMost(BACKOFF_S.lastIndex)] * 1_000L) { nudge.receive() }
                    first = false
                    val probe = probe()
                    if (probe != null) {
                        update { it.copy(link = it.link.down(probe)) }
                        step++
                        continue
                    }
                    val wasOffline = state.value.link.offline
                    update { it.copy(link = it.link.copy(offline = false, outage = null, detail = null)) }
                    if (wasOffline) sessionViewModel.restored()
                    var delivered = 0
                    var foreign = 0
                    val outcome = try {
                        api.flushCommands(
                            expired = { toast(ui("link.expired", it.size), NoticeKind.DONE) },
                            refused = { _, e -> report(e, writing = true) },
                            delivered = { delivered++ },
                            foreign = { foreign++ },
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        report(e, writing = true)
                        FlushOutcome.EMPTY
                    }
                    if (foreign > 0) toast(ui("link.foreign", foreign), NoticeKind.DONE)
                    if (wasOffline || delivered > 0) refreshScreen()
                    when (outcome) {
                        FlushOutcome.OFFLINE -> {
                            update { it.copy(link = it.link.down(null)) }
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
        runtime.api.health()
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
        with(runtime) {
            val now = state.value
            when (now.phase) {
                AppPhase.AUTH -> Unit

                AppPhase.CHARACTERS -> read(Reads.CHARACTERS, silent = true) { characterViewModel.readCharacters() }

                AppPhase.GAME -> {
                    read(Reads.HERO, silent = true) { heroViewModel.readHero() }
                    when (now.tab) {
                        TAB_CRAFTS -> craftsViewModel.load(silent = true)

                        TAB_CITY -> when (now.building) {
                            Building.QUESTS -> quests.load()
                            Building.MERCHANT -> auctionViewModel.loadMerchant()
                            Building.AUCTION -> auctionViewModel.loadAuction()
                            Building.GUILD -> guildViewModel.load()
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    /** A new session or a sign-out: nothing of the old link carries over but the queue on disk. */
    fun reset() {
        loop?.cancel()
        update { it.copy(link = it.link.copy(offline = false)) }
    }

    private companion object {
        /** The pauses between probes, in seconds; the last one repeats. */
        val BACKOFF_S = longArrayOf(2, 4, 8, 16, 30)
    }
}
