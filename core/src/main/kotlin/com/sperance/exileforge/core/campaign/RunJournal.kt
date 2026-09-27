package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunEventKind
import kotlinx.serialization.Serializable

/** The journal on disk: one run's events in order and how far the server has applied them. */
@Serializable data class JournalState(val runId: String, val heroId: String, val zone: String, val events: List<RunEvent> = emptyList(), val applied: Int = 0)

/**
 * The journal of one run: every event the hero caused, numbered from zero, and how many of them the server
 * has taken. The run records, whoever listens flushes what is pending — at checkpoints, at the exit, at a
 * death, in the background — and the server's `applied` moves the mark. A journal outlives the process:
 * what was not sent is sent on the next launch, as long as the hero's run is still this one.
 */
class RunJournal(val runId: String, val heroId: String, val zone: String, events: List<RunEvent> = emptyList(), applied: Int = 0) {
    private val events = events.toMutableList()
    var applied: Int = applied.coerceIn(0, events.size)
        private set
    /** The events the server refused: numbers of the journal, kept for the screen. */
    private val refused = mutableListOf<Int>()

    val size: Int get() = events.size
    val all: List<RunEvent> get() = events
    val rejected: List<Int> get() = refused
    /** Events the server has not taken yet. */
    val pending: List<RunEvent> get() = events.drop(applied)
    val settled: Boolean get() = applied >= events.size
    /** A fall or an exit ends the run: nothing after it is recorded. */
    val closed: Boolean get() = events.any { it.kind == RunEventKind.FALL || it.kind == RunEventKind.LEAVE }

    /** Records one event with the next number; a closed journal records nothing more. */
    fun record(kind: RunEventKind, i: Int = 0, m: Int = 0, index: Int = 0, depth: Int = 0, fallen: Boolean = false, vaal: Boolean = false): RunEvent? {
        if (closed) return null
        return RunEvent(events.size, kind, i, m, index, depth, fallen, vaal).also { events += it }
    }

    /** The server took the journal up to [applied] and refused [rejected] of the numbers. */
    fun confirm(applied: Int, rejected: List<Int> = emptyList()) {
        this.applied = applied.coerceIn(this.applied, events.size)
        rejected.forEach { if (it !in refused) refused += it }
    }

    fun snapshot(): JournalState = JournalState(runId, heroId, zone, events.toList(), applied)
    fun encode(): String = WireJson.encodeToString(JournalState.serializer(), snapshot())

    companion object {
        fun of(state: JournalState): RunJournal = RunJournal(state.runId, state.heroId, state.zone, state.events, state.applied)
        fun decode(text: String): RunJournal? = runCatching { of(WireJson.decodeFromString(JournalState.serializer(), text)) }.getOrNull()
    }
}
