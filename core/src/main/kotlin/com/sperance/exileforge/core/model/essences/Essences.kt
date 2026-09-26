package com.sperance.exileforge.core.model.essences

import kotlinx.serialization.Serializable

/** A step of the essences (server 0.69.0): from what zone level it falls and whether it rerolls a rare item. */
@Serializable data class EssenceTier(val code: String, val level: Int = 1, val rerollsRare: Boolean = false)

/**
 * A kind of essence: the modifier it guarantees on a weapon, on armour and on jewellery — codes of the
 * server's modifiers — and the one its crystal's guardian carries.
 */
@Serializable data class EssenceKind(val code: String, val weapon: String = "", val armour: String = "", val jewellery: String = "", val monster: String = "")

/**
 * The crystals of a zone (server 0.69.0): a window like the chests', essences of the zone's step inside,
 * a rare monster of the zone guarding them. [vaal] weighs what a Vaal orb on a crystal does, [stronger]
 * is how many percent more life and damage a guardian it strengthened has.
 */
@Serializable data class CrystalRule(
    val count: List<Int> = listOf(0, 2),
    val refreshHours: Double = 6.0,
    val essences: List<Int> = listOf(1, 3),
    val lowerChance: Double = 0.0,
    val modifierPools: List<String> = emptyList(),
    val bookChance: Double = 0.0,
    val vaal: Map<String, Int> = emptyMap(),
    val stronger: Double = 50.0,
)

/** Alchemy's condensing: [inputs] alike make one a step higher; [levels] is the craft level each step asks. */
@Serializable data class CondenseRule(val inputs: Int = 3, val levels: List<Int> = emptyList(), val seconds: Int = 0)

/** An essence in the bag: its kind, its step — zero for a special one — and whether it is special. */
data class Essence(val kind: EssenceKind, val tier: Int, val special: Boolean)

/**
 * `essences` of `world.json` (server 0.69.0). Its items are `ESSENCE_<kind>_<step>` and
 * `ESSENCE_<special kind>`, category [CATEGORY] of the item collection.
 */
@Serializable data class EssenceBook(
    val tiers: List<EssenceTier> = emptyList(),
    val kinds: List<EssenceKind> = emptyList(),
    val specials: List<EssenceKind> = emptyList(),
    val crystals: CrystalRule = CrystalRule(),
    val condense: CondenseRule = CondenseRule(),
) {
    private val byCode: Map<String, Essence> by lazy {
        (kinds.flatMap { kind -> (1..tiers.size).map { Essence(kind, it, false) } } + specials.map { Essence(it, 0, true) })
            .associateBy { code(it.kind.code, it.tier, it.special) }
    }

    /** The essence an item of the bag is; null for any other item. */
    fun essence(itemCode: String): Essence? = byCode[itemCode]

    /** The guardian's modifier an essence brings to its crystal. */
    fun monster(itemCode: String): String? = essence(itemCode)?.kind?.monster?.takeIf { it.isNotBlank() }

    companion object {
        const val CATEGORY = "ESSENCE"
        const val PREFIX = "ESSENCE_"
        const val VAAL_UPGRADE = "UPGRADE"
        const val VAAL_SPECIAL = "SPECIAL"
        const val VAAL_STRONGER = "STRONGER"

        fun code(kind: String, tier: Int, special: Boolean): String = if (special) "$PREFIX$kind" else "$PREFIX${kind}_$tier"
    }
}

/**
 * A crystal of essences in a zone (server 0.69.0): the item codes of its [essences], its [guardian] —
 * a monster of the zone that stands up rare with the modifiers of those essences. A Vaal orb passes
 * over a crystal once ([vaal]) and may make its guardian [stronger].
 */
@Serializable data class Crystal(val essences: List<String> = emptyList(), val guardian: String = "", val stronger: Boolean = false, val vaal: Boolean = false)

/** The crystals of a zone for one hero: what stands, and when the window is thrown anew (epoch milliseconds). */
@Serializable data class CrystalState(val crystals: List<Crystal> = emptyList(), val refreshAt: Long = 0)

/** What a Vaal orb on a crystal did: its outcome, the crystal after it and the zone's crystals as they stand. */
@Serializable data class CrystalVaal(val outcome: String = "", val crystal: Crystal = Crystal(), val state: CrystalState = CrystalState())
