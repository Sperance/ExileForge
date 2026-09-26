package com.sperance.exileforge.core.model.crafts

import com.sperance.exileforge.core.model.hero.EquipmentInstance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A side find of a work and its chance per successful cycle, in percent, as the hero has it now. */
@Serializable data class JobExtra(val item: String = "", val chance: Double = 0.0)

/**
 * One work of a profession (since server 0.37.0): the server's base [seconds], and the numbers the
 * hero's level and gear make of it — [cycleMillis] and the [nothing] chance — worked out there.
 */
@Serializable data class JobView(
    val code: String,
    val level: Int = 1,
    val seconds: Double = 0.0,
    val cycleMillis: Long = 0,
    val nothing: Double = 0.0,
    val output: String = "",
    val experience: Double = 0.0,
    val extra: List<JobExtra> = emptyList(),
    /** Since server 0.38.0: what a successful cycle makes, what every cycle spends, and the crafts' own facts. */
    val kind: JobKind = JobKind.ITEM,
    val inputs: List<JobInput> = emptyList(),
    val band: List<Int> = emptyList(),
    val map: String = "",
    val additives: Boolean = false,
    /** A cartographer's chart: whether the hero has opened its location. */
    val open: Boolean = true,
)

/**
 * What a successful cycle makes (since server 0.38.0): a stack, the smith's gear or a cartographer's map;
 * since 0.69.0 an alchemist's flask and an enchanter's skill book of a class.
 */
@Serializable enum class JobKind { ITEM, EQUIPMENT, MAP, FLASK, BOOK }

/** A material a cycle spends. */
@Serializable data class JobInput(val item: String = "", val amount: Long = 0)

/** What gear and the tree give work, in percent. */
@Serializable data class WorkBonus(
    val speed: Double = 0.0,
    val yield: Double = 0.0,
    val luck: Double = 0.0,
    val experience: Double = 0.0,
    val find: Double = 0.0,
)

/** A profession of the hero: its level and experience, the tool in its slot, its bonus and works. */
@Serializable data class ProfessionView(
    val code: String,
    val tool: String = "",
    val level: Int = 1,
    val experience: Double = 0.0,
    val next: Double? = null,
    val equipped: EquipmentInstance? = null,
    val bonus: WorkBonus = WorkBonus(),
    val jobs: List<JobView> = emptyList(),
)

/** The work under way: cycles are counted up to [settledAt]; the next lands at [nextAt] (epoch milliseconds, server's clock). */
@Serializable data class WorkView(
    val profession: String,
    val job: String,
    val settledAt: Long = 0,
    val cycleMillis: Long = 0,
    val nextAt: Long = 0,
    val additives: List<String> = emptyList(),
    /** Since server 0.42.0: the work's seed and the number of its next cycle — what [com.sperance.exileforge.core.crafts.CraftCycle] throws it by. */
    val seed: Long = 0,
    val cycle: Long = 0,
    /** Since server 0.66.0: when the work was started, and what it has brought and spent since. */
    val startedAt: Long = 0,
    val totals: WorkTally = WorkTally(),
)

/** What a work has come to since it started (server 0.66.0): the numbers of [WorkGains], without the pieces. */
@Serializable data class WorkTally(
    val cycles: Long = 0,
    val nothing: Long = 0,
    val items: Map<String, Long> = emptyMap(),
    val spent: Map<String, Long> = emptyMap(),
    val made: Long = 0,
    val experience: Double = 0.0,
    val levels: Int = 0,
)

/** What the cycles counted by one answer brought. */
@Serializable data class WorkGains(
    val cycles: Int = 0,
    val nothing: Int = 0,
    val items: Map<String, Long> = emptyMap(),
    val experience: Double = 0.0,
    val levels: Int = 0,
    /** Since server 0.38.0: what the crafts spent, how many pieces they made, whether the bag ran dry, and the pieces themselves. */
    val spent: Map<String, Long> = emptyMap(),
    val made: Int = 0,
    val starved: Boolean = false,
    val equipment: List<EquipmentInstance> = emptyList(),
)

@Serializable data class CraftsRules(val offlineHours: Double = 8.0, val maxLevel: Int = 50)

/** The crafts of a hero, whole (since server 0.37.0): every answer of the crafts routes is this. */
@Serializable data class CraftsState(
    val now: Long = 0,
    val rules: CraftsRules = CraftsRules(),
    val professions: List<ProfessionView> = emptyList(),
    val work: WorkView? = null,
    val gains: WorkGains = WorkGains(),
    /** Which additive guarantees which handcrafted line, and how many a smelt takes (since server 0.38.0). */
    val additives: Map<String, String> = emptyMap(),
    val maxAdditives: Int = 0,
)

/** A material in the bag (since server 0.37.0): an `items` document of category `MATERIAL`, the profession as its sub-category. */
@Serializable data class MaterialItem(
    @SerialName("_id") val id: String,
    val code: String = "",
    val subCategory: String = "",
    val price: Long = 0,
) {
    companion object { const val CATEGORY = "MATERIAL" }
}
