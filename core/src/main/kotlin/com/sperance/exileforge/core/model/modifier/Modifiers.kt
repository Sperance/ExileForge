package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How a rolled value reaches the final stat. Mirrors the server's EnumModifierOperation. */
@Serializable enum class ModifierOperation { ADD, INCREASED, MORE, SET }

/** Where a modifier came from. PREFIX and SUFFIX are rolled, the rest sit on every instance. */
@Serializable enum class ModifierSource { IMPLICIT, PREFIX, SUFFIX, UNIQUE, ENCHANTMENT, CORRUPTION }

/**
 * One action of a modifier: which stat it touches and how.
 *
 * `stat` is a server enum name (STOCK_*, BOOL_*, PROFESSION_*, BATTLE_*) and stays a plain string
 * here — the client never interprets it, it only prints it.
 */
@Serializable data class ModifierEffect(val stat: String, val operation: ModifierOperation)

/**
 * Description of a possible modifier (collection `ModifierDefinition`).
 *
 * Carries no values and no tier: value ranges live in [ModifierTier], one document per tier.
 * More than one effect means a composite modifier — one roll changing several stats at once.
 */
@Serializable data class ModifierDefinition(
    @SerialName("_id") val id: String = "",
    val code: String = "",
    val effects: List<ModifierEffect> = emptyList(),
    val source: ModifierSource = ModifierSource.PREFIX,
    val name: String? = null,
    // The server writes every nullable field, null included, so an absent tag list arrives as null.
    val tags: List<String>? = null,
) {
    val composite: Boolean get() = effects.size > 1
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: code
}

/** Value range of one effect inside a tier. */
@Serializable data class ModifierTierValue(val valueMin: Double, val valueMax: Double)

/** One tier of one [ModifierDefinition]; tier 1 is the best, as in PoE. */
@Serializable data class ModifierTier(
    @SerialName("_id") val id: String = "",
    val modifierId: String = "",
    val tier: Int = 1,
    val values: List<ModifierTierValue> = emptyList(),
    val minItemLevel: Int = 1,
    val weight: Int = 1,
)

/**
 * A modifier rolled onto one instance of an item.
 *
 * `values` holds one number per effect of the description, in the same order. Rolling is the
 * server's job: the client never produces one of these itself.
 */
@Serializable data class Modifier(
    val modifierId: String,
    val tierId: String = "",
    val tier: Int = 1,
    val values: List<Double> = emptyList(),
)
