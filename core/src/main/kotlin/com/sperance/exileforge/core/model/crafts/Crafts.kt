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
)

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
    val startedAt: Long = 0,
    val settledAt: Long = 0,
    val cycleMillis: Long = 0,
    val nextAt: Long = 0,
)

/** What the cycles counted by one answer brought. */
@Serializable data class WorkGains(
    val cycles: Int = 0,
    val nothing: Int = 0,
    val items: Map<String, Long> = emptyMap(),
    val experience: Double = 0.0,
    val levels: Int = 0,
)

@Serializable data class CraftsRules(val offlineHours: Double = 8.0, val maxLevel: Int = 50)

/** The crafts of a hero, whole (since server 0.37.0): every answer of the crafts routes is this. */
@Serializable data class CraftsState(
    val now: Long = 0,
    val rules: CraftsRules = CraftsRules(),
    val professions: List<ProfessionView> = emptyList(),
    val work: WorkView? = null,
    val gains: WorkGains = WorkGains(),
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
