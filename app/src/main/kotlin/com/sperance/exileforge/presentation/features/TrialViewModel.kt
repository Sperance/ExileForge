package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.TrialStart
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.rules.content.TrialEvent
import com.sperance.exileforge.rules.content.TrialEventKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The trials (3.49.0, server 1.47.0): the boss rush of a cleared region and the endless tower, each an arena of its own.
 *
 * Entering spends the key on the server and brings the trial back; the arena is then built here and fought. Its
 * events — a boss down, a floor cleared, the end — go out as they happen, in order, a batch under one idempotency
 * key until it is answered; the answers name what each event brought and land on the arena. A trial the app lost
 * (closed mid-fight) stays open on the server until it is ended from the trials board.
 */
class TrialViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {
    private val mutableArena = MutableStateFlow<TrialArena?>(null)
    val arena: StateFlow<TrialArena?> = mutableArena.asStateFlow()
    private var owner = ""

    /** The trial the journal belongs to (server 1.68.0): a batch names it, and the server takes no other trial's. */
    private var runId = ""
    private val pending = mutableListOf<TrialEvent>()

    /** The batch in flight and its key: sent again as it was until the server answers it. */
    private var batch: Pair<String, List<TrialEvent>>? = null
    private val sends = Channel<Unit>(Channel.CONFLATED)
    private var retry: Job? = null
    private var failures = 0

    init {
        runtime.scope.launch { for (signal in sends) flush() }
    }

    /** «Раш»: a rush key is spent on the rush of [region]. */
    fun rush(region: String) = enter { runtime.api.trials.rush(it, region) }

    /** «Собрать ключ» (3.50.0): five crest fragments become a rush key. */
    fun forgeKey() {
        with(runtime) {
            if (state.value.busy) return
            task(writing = true, touches = setOf(Reads.HERO)) {
                api.trials.forgeKey(heroId)
                toast(ui("trials.key_forged"))
                if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
            }
        }
    }

    /** «Башня»: a seal is spent, and the tower begins at the last checkpoint. */
    fun tower() = enter { runtime.api.trials.tower(it) }

    private fun enter(call: suspend (String) -> TrialStart) {
        with(runtime) {
            if (mutableArena.value != null || state.value.busy) return
            task(writing = true, touches = setOf(Reads.HERO)) {
                val id = heroId
                val started = call(id)
                if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
                heroViewModel.drawn()
                begin(id, started)
            }
        }
    }

    private fun begin(id: String, started: TrialStart) {
        with(runtime) {
            val index = state.value.index ?: return
            val hero = state.value.hero?.takeIf { it.id == id } ?: return
            val gear = expeditionViewModel.gear() ?: return
            owner = id
            runId = started.run.id
            synchronized(pending) {
                pending.clear()
                batch = null
            }
            mutableArena.value = TrialArena(index, started.run, started.context, gear, hero.pets.pet(hero.pets.combat), onEvent = ::recorded)
                .also { a -> repeat(speedSteps) { a.send(RunCommand.Speed) } }
        }
    }

    /**
     * «Завершить»: a trial open on the server with no arena here — the app closed mid-trial — is ended, what it
     * brought kept: a rush's chest by the bosses it felled.
     */
    fun abandon() {
        with(runtime) {
            val open = state.value.hero?.campaign?.trials?.run ?: return
            if (mutableArena.value != null) return
            task(writing = true, touches = setOf(Reads.HERO)) {
                val report = api.trials.events(heroId, open.id, listOf(TrialEvent(open.applied, TrialEventKind.END)))
                report?.received?.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { toast(ui("stash.received", it.overflowed, it.sold, it.gold)) }
                toast(ui("trials.abandoned"))
                if (state.value.play.heroReadAt == 0L) heroViewModel.readHero()
            }
        }
    }

    fun send(command: RunCommand) {
        mutableArena.value?.send(command)
    }

    private fun recorded(event: TrialEvent) {
        synchronized(pending) { pending += event }
        sends.trySend(Unit)
    }

    /** Sends the batch in flight again, or the events pending under a new key; the answer lands on the arena. */
    private suspend fun flush() {
        with(runtime) {
            val (key, events) = synchronized(pending) {
                // At most a batch's worth at once (3.71.0): the server refuses a longer journal whole, and the rest goes after it
                batch ?: pending.take(RunJournal.MAX_BATCH).takeIf { it.isNotEmpty() }?.let {
                    (UUID.randomUUID().toString() to it).also { made ->
                        batch = made
                        pending.removeAll(it)
                    }
                }
            } ?: return
            try {
                val report = api.trials.events(owner, runId, events, key)
                failures = 0
                synchronized(pending) { batch = null }
                if (report == null) {
                    // Landed, its report not kept: the batch is taken, and its loot comes with the hero read again
                    mutableArena.value?.settle(events.last().n + 1, emptyMap())
                    heroViewModel.readHero()
                    if (synchronized(pending) { pending.isNotEmpty() }) sends.trySend(Unit)
                    return
                }
                mutableArena.value?.settle(report.applied, report.rewards.associate { it.n to it.reward.toReward() })
                if (report.rejected.isNotEmpty()) toast(ui("expedition.rejected", report.rejected.size))
                report.received.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { toast(ui("stash.received", it.overflowed, it.sold, it.gold)) }
                if (state.value.play.heroReadAt == 0L && onScreen(owner)) heroViewModel.readHero()
                if (synchronized(pending) { pending.isNotEmpty() }) sends.trySend(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiFailure) {
                // The trial is closed there, or the journal is of another trial: nothing of it waits any longer.
                if (e.code in TRIAL_CLOSED) {
                    synchronized(pending) {
                        batch = null
                        pending.clear()
                    }
                    mutableArena.value?.settle(Int.MAX_VALUE, emptyMap())
                }
                // Any other refusal leaves the events unsent, not settled (3.71.0): they go again under a new key a while later.
                else if (!CommandQueue.transient(e.status)) {
                    synchronized(pending) {
                        batch?.let { pending.addAll(0, it.second) }
                        batch = null
                    }
                    retryLater()
                }
                report(e, writing = true)
            } catch (_: Exception) {
                retryLater()
            }
        }
    }

    private fun retryLater() {
        with(runtime) {
            failures++
            retry?.cancel()
            retry = scope.launch {
                delay((RETRY_FIRST shl (failures - 1).coerceAtMost(RETRY_DOUBLINGS)))
                sends.trySend(Unit)
            }
        }
    }

    /** The trial's screen is closed: whatever is still unsent goes out, and the hero is read again. */
    fun close() {
        val arena = mutableArena.value ?: return
        // Closed mid-fight, the trial is left as it stands: ended on the server as a walk-away, what it brought kept.
        if (arena.hud.value.phase == TrialPhase.FIGHT) arena.send(RunCommand.Leave).also { arena.update(0.0) }
        mutableArena.value = null
        sends.trySend(Unit)
        runtime.heroViewModel.ensureHero()
    }

    /** Dropped without a word: the hero or the session it belonged to is gone. */
    fun drop() {
        mutableArena.value = null
        synchronized(pending) {
            pending.clear()
            batch = null
        }
    }

    private companion object {
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 3

        /** No trial is open (CP_023), or the journal is of another one (CP_026). */
        val TRIAL_CLOSED = setOf("CP_023", "CP_026")
    }
}
