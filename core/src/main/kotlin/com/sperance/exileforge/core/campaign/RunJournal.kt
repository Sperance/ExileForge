package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.FightTally
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunEventKind
import kotlinx.serialization.Serializable

/** The journal on disk: one run's events in order, the number the first of them bears, how far the server has applied them, and the batch in flight. */
@Serializable data class JournalState(val runId: String, val heroId: String, val zone: String, val events: List<RunEvent> = emptyList(), val applied: Int = 0, val base: Int = 0,
                                      val batch: JournalBatch? = null, val carry: StageCarry? = null)

/**
 * A batch sent and not yet answered (server 1.30.0): its idempotency [key] and the number past its last event.
 * The answer carries the batch's rewards, so a lost one is asked for again with the same key and the same
 * events — the server repeats what it stored — and only an answer frees the key.
 */
@Serializable data class JournalBatch(val key: String, val end: Int)

/**
 * The journal of one run: every event the hero caused, numbered from zero, and how many of them the server
 * has taken. The run records, whoever listens flushes what is pending — at checkpoints, at the exit, at a
 * death, in the background — and the server's `applied` moves the mark. A journal outlives the process:
 * what was not sent is sent on the next launch, as long as the hero's run is still this one.
 *
 * A run entered again (server 1.1.0) goes on where the server stopped: [base] is the number its first
 * event here bears, so the numbers stay the server's; [applied] counts from the run's start, not from [base].
 */
class RunJournal(val runId: String, val heroId: String, val zone: String, events: List<RunEvent> = emptyList(), applied: Int = 0, val base: Int = 0,
                 batch: JournalBatch? = null, carry: StageCarry? = null) {
    private val events = events.toMutableList()
    /** What the staged fight under way carries to its next stage (3.32.1): kept on disk, so a restart mid-fight goes on with it. */
    var carry: StageCarry? = carry
        set(value) { if (field != value) { field = value; onCarry() } }
    /** Told when [carry] changes: whoever keeps the journal on disk writes it again. */
    var onCarry: () -> Unit = {}
    private val end: Int get() = base + events.size
    var applied: Int = applied.coerceIn(base, base + events.size)
        private set
    /** The events the server refused: numbers of the journal, kept for the screen. */
    private val refused = mutableListOf<Int>()
    /** The batch in flight, until the server answers it. */
    private var batch: JournalBatch? = batch?.takeIf { it.end > this.applied && it.end <= this.end }

    val size: Int get() = events.size
    val all: List<RunEvent> get() = events
    val rejected: List<Int> get() = refused
    /** Events the server has not taken yet. */
    val pending: List<RunEvent> get() = events.drop(applied - base)
    val settled: Boolean get() = applied >= end
    /** A fall or an exit ends the run: nothing after it is recorded. */
    val closed: Boolean get() = events.any { it.kind == RunEventKind.FALL || it.kind == RunEventKind.LEAVE }

    /** Records one event with the next number; a closed journal records nothing more. */
    fun record(kind: RunEventKind, i: Int = 0, m: Int = 0, index: Int = 0, depth: Int = 0, fallen: Boolean = false, vaal: Boolean = false,
               fight: FightTally? = null): RunEvent? {
        if (closed) return null
        return RunEvent(end, kind, i, m, index, depth, fallen, vaal, fight).also { events += it }
    }

    /**
     * What to send now, and the key to send it with: the batch in flight again, the same events under the same
     * key, or — none in flight — everything pending under a [fresh] key. Null when nothing is pending.
     */
    fun outgoing(fresh: () -> String): Pair<JournalBatch, List<RunEvent>>? {
        if (settled) return null
        val sent = batch ?: JournalBatch(fresh(), end).also { batch = it }
        return sent to events.subList(applied - base, sent.end - base).toList()
    }

    /** The server took the journal up to [applied] and refused [rejected] of the numbers; the batch it answered is done with. */
    fun confirm(applied: Int, rejected: List<Int> = emptyList()) {
        this.applied = applied.coerceIn(this.applied, end)
        rejected.forEach { if (it !in refused) refused += it }
        batch = null
    }

    /** The batch in flight was refused for good: its key holds that refusal, and the next send goes under a new one. */
    fun release() { batch = null }

    fun snapshot(): JournalState = JournalState(runId, heroId, zone, events.toList(), applied, base, batch, carry)
    fun encode(): String = WireJson.encodeToString(JournalState.serializer(), snapshot())

    companion object {
        fun of(state: JournalState): RunJournal = RunJournal(state.runId, state.heroId, state.zone, state.events, state.applied, state.base, state.batch, state.carry)
        fun decode(text: String): RunJournal? = runCatching { of(WireJson.decodeFromString(JournalState.serializer(), text)) }.getOrNull()
    }
}
