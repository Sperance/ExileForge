package com.sperance.exileforge.core.model.crafts

import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.content.JobExtra
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.roll.ActiveWork
import com.sperance.exileforge.rules.roll.CraftsAway
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.ProfessionProgress
import com.sperance.exileforge.rules.roll.WorkBonus
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import kotlinx.serialization.Serializable

/**
 * The crafts part of the hero snapshot: each profession's progress, the work under way and the last catch-up of an
 * absence of five minutes or more (3.69.0, server 1.66.0), shown once as «Пока вас не было».
 */
@Serializable data class WorkState(val professions: Map<String, ProfessionProgress> = emptyMap(), val work: ActiveWork? = null, val away: CraftsAway? = null)

/** One work of a profession as the hero has it now: the rules' seconds, and the cycle and the «nothing» chance their gear makes of it. */
@Serializable data class JobView(
    val code: String,
    val level: Int = 1,
    val seconds: Double = 0.0,
    val cycleMillis: Long = 0,
    val nothing: Double = 0.0,
    val output: String = "",
    val experience: Double = 0.0,
    val extra: List<JobExtra> = emptyList(),
    val kind: JobKind = JobKind.ITEM,
    val inputs: List<JobInput> = emptyList(),
    val band: List<Int> = emptyList(),
    /** A cartographer's chart (3.45.0): the region whose open zones it maps, one at random. */
    val region: String = "",
    val additives: Boolean = false,
    /** Whether the hero may take it: a chart needs an open zone of its region, a choosing work a variant. */
    val open: Boolean = true,
    /** A variant of a choosing work (3.45.0): what was chosen — the item it makes; blank on the work itself. */
    val choice: String = "",
    /** The variants of a choosing work — a condensed essence, a skill book of the hero's class — each a plain work of its own. */
    val options: List<JobView> = emptyList(),
)

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
    val seed: Long = 0,
    val cycle: Long = 0,
    val startedAt: Long = 0,
    val totals: WorkTally = WorkTally(),
    /** The variant chosen (3.45.0), blank for a work without a choice. */
    val choice: String = "",
)

/** The work [code] as the hero runs it: the chosen variant of a choosing work, else the work itself. */
fun ProfessionView.job(code: String, choice: String = ""): JobView? =
    jobs.firstOrNull { it.code == code }?.let { job -> job.options.firstOrNull { it.choice == choice } ?: job.takeIf { choice.isEmpty() } }

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
    /** The last catch-up of an absence (3.69.0), the same as the snapshot's [WorkState.away]. */
    val away: CraftsAway? = null,
)

/** The work under way, as its profession lists it: the chosen variant when it has one. */
val CraftsState.running: JobView? get() = work?.let { w -> professions.firstOrNull { it.code == w.profession }?.job(w.job, w.choice) }
