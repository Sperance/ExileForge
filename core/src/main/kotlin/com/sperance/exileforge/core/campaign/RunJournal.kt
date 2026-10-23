package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.rules.run.ChestStock
import com.sperance.exileforge.rules.run.RunEvent
import kotlinx.serialization.Serializable

/**
 * Журнал на диске: события захода по порядку, номер первого, докуда их принял сервер, пачка в пути, вещи добычи, помеченные
 * к продаже ([saleMarks]), и запас сундуков захода ([chestStock]) - они переживают перезапуск приложения.
 */
@Serializable data class JournalState(
    val runId: String,
    val heroId: String,
    val zone: String,
    val events: List<RunEvent> = emptyList(),
    val applied: Int = 0,
    val base: Int = 0,
    val batch: JournalBatch? = null,
    val saleMarks: Set<String> = emptySet(),
    val chestStock: ChestStock = ChestStock(),
)

/**
 * A batch sent and not yet answered: its idempotency [key] and the number past its last event.
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
 * A run entered again goes on where the server stopped: [base] is the number its first
 * event here bears, so the numbers stay the server's; [applied] counts from the run's start, not from [base].
 */
class RunJournal(
    val runId: String,
    val heroId: String,
    val zone: String,
    events: List<RunEvent> = emptyList(),
    applied: Int = 0,
    val base: Int = 0,
    batch: JournalBatch? = null,
    saleMarks: Set<String> = emptySet(),
    chestStock: ChestStock = ChestStock(),
) {
    private val events = events.toMutableList()

    /** Id вещей добычи захода, помеченных к продаже: хранятся с журналом. */
    var saleMarks: Set<String> = saleMarks

    /** Содержимое сундуков захода по номеру сундука, как его скатал сервер: сундук показывает его сразу при открытии. */
    @Volatile var chestStock: ChestStock = chestStock

    private val end: Int get() = base + events.size
    var applied: Int = applied.coerceIn(base, base + events.size)
        private set

    /** The batch in flight, until the server answers it. */
    private var batch: JournalBatch? = batch?.takeIf { it.end > this.applied && it.end <= this.end }

    val size: Int get() = events.size
    val all: List<RunEvent> get() = events

    /** Events the server has not taken yet. */
    val pending: List<RunEvent> get() = events.drop(applied - base)
    val settled: Boolean get() = applied >= end

    /** A fall or an exit ends the run: nothing after it is recorded. */
    val closed: Boolean get() = events.any { it is RunEvent.Fall || it is RunEvent.Leave }

    /** Records the event [event] builds for the next number; a closed journal records nothing more. */
    fun record(event: (n: Int) -> RunEvent): RunEvent? {
        if (closed) return null
        return event(end).also { events += it }
    }

    /**
     * What to send now, and the key to send it with: the batch in flight again, the same events under the same
     * key, or — none in flight — up to [MAX_BATCH] pending under a [fresh] key, the rest after it. Null when nothing is pending.
     */
    fun outgoing(fresh: () -> String): Pair<JournalBatch, List<RunEvent>>? {
        if (settled) return null
        val sent = batch ?: JournalBatch(fresh(), minOf(end, applied + MAX_BATCH)).also { batch = it }
        return sent to events.subList(applied - base, sent.end - base).toList()
    }

    /** The server took the journal up to [applied]; the batch it answered is done with. */
    fun confirm(applied: Int) {
        this.applied = applied.coerceIn(this.applied, end)
        batch = null
    }

    /** The batch in flight was refused for good: its key holds that refusal, and the next send goes under a new one. */
    fun release() {
        batch = null
    }

    fun snapshot(): JournalState = JournalState(runId, heroId, zone, events.toList(), applied, base, batch, saleMarks, chestStock)
    fun encode(): String = WireJson.encodeToString(JournalState.serializer(), snapshot())

    companion object {
        /** The most events one batch carries: the server refuses a longer journal whole (CP_022). */
        const val MAX_BATCH = 64

        fun of(state: JournalState): RunJournal = RunJournal(state.runId, state.heroId, state.zone, state.events, state.applied, state.base, state.batch, state.saleMarks, state.chestStock)
        fun decode(text: String): RunJournal? = runCatching { of(WireJson.decodeFromString(JournalState.serializer(), text)) }.getOrNull()
    }
}
