package com.sperance.exileforge.core.model.crafts

import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.JobExtra
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.roll.ActiveWork
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.ProfessionProgress
import com.sperance.exileforge.rules.roll.Work
import com.sperance.exileforge.rules.roll.WorkBonus
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import kotlinx.serialization.Serializable

/**
 * The crafts part of the hero snapshot: each profession's progress, the work under way and the last catch-up of an
 * absence of five minutes or more (3.69.0, server 1.66.0), shown once as «Пока вас не было».
 */
@Serializable data class WorkState(val professions: Map<String, ProfessionProgress> = emptyMap(), val work: ActiveWork? = null)

/** One work of a profession as the hero has it now: the rules' seconds, and the cycle and the «nothing» chance their gear makes of it. */
@Serializable data class JobView(
    /** The work itself (server 1.75.0): each kind with its own fields — a stack's output, a smith's band, a chart's region. */
    val job: Job,
    val cycleMillis: Long = 0,
    /** The hero's own odds: the empty cycle's and each find's, after level and gear. */
    val nothing: Double = 0.0,
    val extra: List<JobExtra> = emptyList(),
    /** Whether the hero may take it: a chart needs an open zone of its region, a choosing work a variant. */
    val open: Boolean = true,
    /** A variant of a choosing work (3.45.0): what was chosen — the item it makes; blank on the work itself. */
    val choice: String = "",
    /** The variants of a choosing work — a condensed essence, a skill book of the hero's class — each a plain work of its own. */
    val options: List<JobView> = emptyList(),
) {
    val code: String get() = job.code
    val kind: JobKind get() = job.kind
    val level: Int get() = job.level
    val seconds: Double get() = job.seconds
    val output: String get() = job.output
    val experience: Double get() = job.experience
    val inputs: List<JobInput> get() = job.inputs
    val additives: Boolean get() = job.additives

    /** Расход цикла с добавками [chosen] (3.89.0) - тот же, что проверяет сервер при запуске. */
    fun cycleCost(chosen: List<String> = emptyList()): Cost = Cost(Work.perCycle(job, chosen))
}

/** A profession of the hero: its level and experience, the tool in its slot, its bonus and works. */
@Serializable data class ProfessionView(
    val code: String,
    val tool: String = "",
    val level: Int = 1,
    val experience: Double = 0.0,
    val next: Double? = null,
    val equipped: ItemInstance? = null,
    val bonus: WorkBonus = WorkBonus(),
    val jobs: List<JobView> = emptyList(),
)

/** The work under way: cycles are counted up to [settledAt]; the next lands at [nextAt] (epoch millis, the server's clock). */
@Serializable data class WorkView(
    val profession: String,
    val job: String,
    val settledAt: Long = 0,
    val cycleMillis: Long = 0,
    val nextAt: Long = 0,
    val additives: List<String> = emptyList(),
    val startedAt: Long = 0,
    val totals: WorkTally = WorkTally(),
    /** The variant chosen (3.45.0), blank for a work without a choice. */
    val choice: String = "",
) {
    /**
     * Сколько прошло текущего цикла на часах сервера [now] (3.90.0): по кругу от [settledAt], так что полоса идёт
     * дальше, даже пока сервер не пересчитал работу, а не стоит на 100%. До [settledAt] - ноль.
     */
    fun phase(now: Long): Long = if (cycleMillis > 0) (now - settledAt).coerceAtLeast(0L) % cycleMillis else 0L

    /** Ближайшая граница цикла позже [now] на часах сервера (3.90.0): на ней работа пересчитывается с сервера. */
    fun nextBoundary(now: Long): Long = maxOf(now, settledAt) - phase(now) + cycleMillis
}

/** The work [code] as the hero runs it: the chosen variant of a choosing work, else the work itself. */
fun ProfessionView.job(code: String, choice: String = ""): JobView? = jobs.firstOrNull { it.code == code }?.let { job -> job.options.firstOrNull { it.choice == choice } ?: job.takeIf { choice.isEmpty() } }

/** The crafts of a hero, whole: every answer of the crafts routes is this. */
@Serializable data class CraftsState(
    val now: Long = 0,
    val rules: CraftsRules? = null,
    val professions: List<ProfessionView> = emptyList(),
    val work: WorkView? = null,
    val gains: WorkGains = WorkGains(),
    /** Which additive guarantees which handcrafted line, and how many a smelt takes. */
    val additives: Map<String, String> = emptyMap(),
    val maxAdditives: Int = 0,
)

/** The work under way, as its profession lists it: the chosen variant when it has one. */
val CraftsState.running: JobView? get() = work?.let { w -> professions.firstOrNull { it.code == w.profession }?.job(w.job, w.choice) }
