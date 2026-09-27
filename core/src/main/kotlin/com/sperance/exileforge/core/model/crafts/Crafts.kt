package com.sperance.exileforge.core.model.crafts

import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.content.JobExtra
import com.sperance.exileforge.rules.content.JobInput
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.roll.ActiveWork
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.ProfessionProgress
import com.sperance.exileforge.rules.roll.WorkBonus
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.roll.WorkTally
import kotlinx.serialization.Serializable

/** The crafts part of the hero snapshot: each profession's progress and the work under way. */
@Serializable data class WorkState(val professions: Map<String, ProfessionProgress> = emptyMap(), val work: ActiveWork? = null)

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
    val map: String = "",
    val additives: Boolean = false,
    /** A cartographer's chart: whether the hero has opened its zone. */
    val open: Boolean = true,
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
)

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
