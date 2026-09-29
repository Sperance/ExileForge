package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.FlushOutcome
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.NoticeKind
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.TAB_CITY
import com.sperance.exileforge.presentation.state.TAB_CRAFTS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The link to the server (3.30.0): whether it is reachable, and the commands waiting for it.
 *
 * A lost connection is not a refusal: it shows as an icon in the top bar, not as the red strip, and the
 * server is asked again by itself — after [BACKOFF_S] seconds, the last step repeating — until it answers.
 * Then the waiting commands go out in order, and the screen the player is on is read again.
 */
class ConnectionViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {
    private var watcher: Job? = null
    private var loop: Job? = null

    /** A new [GameApi]: its queue is read from the device and drawn in the top bar as it changes. */
    fun attach(api: GameApi) { with(runtime) {
        watcher?.cancel()
        val queue = api.commands ?: return
        watcher = scope.launch {
            val expired = queue.load()
            if (expired.isNotEmpty()) toast(ui("link.expired", expired.size), NoticeKind.DONE)
            // What the last launch left waiting goes out once there is a session to send it with.
            if (queue.waiting.value.isNotEmpty()) wake()
            queue.waiting.collect { waiting -> update { it.copy(link = it.link.copy(waiting = waiting)) } }
        }
    } }

    /** The network failed under a read or a command: the icon goes up, and the server is asked again. */
    fun lost() {
        update { it.copy(link = it.link.copy(offline = true)) }
        wake()
    }

    /** A command joined the queue: it goes out as soon as the server can be reached. */
    fun queued() {
        runtime.toast(ui(if (state.value.link.offline) "link.queued" else "link.queued_later"), NoticeKind.DONE)
        wake()
    }

    /**
     * The probe loop, one at a time: an offline link waits its step of the backoff first, an online one with
     * commands waiting sends them at once. It ends once the server answered and nothing waits. [now] skips the
     * pause the loop is in — the app came back to the foreground, or the session was just confirmed.
     */
    fun wake(now: Boolean = false) { with(runtime) {
        if (loop?.isActive == true) { if (!now) return; loop?.cancel() }
        loop = scope.launch {
            var step = 0
            var first = now
            while (true) {
                // The queue itself, not its reflection in the state, which may lag a frame behind a command just added.
                val offline = state.value.link.offline
                if (!offline && api.commands?.waiting?.value.isNullOrEmpty()) break
                if (offline && !first) delay(BACKOFF_S[step.coerceAtMost(BACKOFF_S.lastIndex)] * 1_000L)
                first = false
                if (!reachable()) { step++; continue }
                val wasOffline = state.value.link.offline
                update { it.copy(link = it.link.copy(offline = false)) }
                if (wasOffline) sessionViewModel.restored()
                var delivered = 0
                val outcome = try {
                    api.flushCommands(
                        expired = { toast(ui("link.expired", it.size), NoticeKind.DONE) },
                        refused = { _, e -> report(e, writing = true) },
                        delivered = { delivered++ })
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { report(e, writing = true); FlushOutcome.EMPTY }
                if (wasOffline || delivered > 0) refreshScreen()
                when (outcome) {
                    FlushOutcome.OFFLINE -> { update { it.copy(link = it.link.copy(offline = true)) }; step++ }
                    // No session to send them with: the sign-in wakes the loop again.
                    FlushOutcome.SIGNED_OUT, FlushOutcome.EMPTY -> break
                }
            }
        }
    } }

    /** The probe: the health route, cheap and unauthenticated. Any answer — even a refusal — means the server is there. */
    private suspend fun reachable(): Boolean = try { runtime.api.health(); true }
        catch (e: CancellationException) { throw e }
        catch (_: ApiFailure) { true }
        catch (_: Exception) { false }

    /** What the player looks at, read again quietly: the hero always, and the tab's own data where it has any. */
    fun refreshScreen() { with(runtime) {
        val now = state.value
        when (now.phase) {
            AppPhase.AUTH -> Unit
            AppPhase.CHARACTERS -> read(Reads.CHARACTERS, silent = true) { characterViewModel.readCharacters() }
            AppPhase.GAME -> {
                read(Reads.HERO, silent = true) { heroViewModel.readHero() }
                when (now.tab) {
                    TAB_CRAFTS -> craftsViewModel.load(silent = true)
                    TAB_CITY -> when (now.building) {
                        Building.QUESTS -> questViewModel.load()
                        Building.MERCHANT -> auctionViewModel.loadMerchant()
                        Building.AUCTION -> auctionViewModel.loadAuction()
                        Building.GUILD -> guildViewModel.load()
                        null -> Unit
                    }
                }
            }
        }
    } }

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
