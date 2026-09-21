package com.sperance.exileforge.core.model.modifier

import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How a rolled value reaches the final stat. Mirrors the server's EnumModifierOperation. */
@Serializable enum class ModifierOperation { ADD, INCREASED, MORE, SET }

/**
 * Where a modifier came from.
 *
 * PREFIX and SUFFIX are rolled onto an instance, the other item sources sit on every copy, and
 * PASSIVE never reaches an item at all — it is a skill-tree node's own bonus.
 */
@Serializable enum class ModifierSource { IMPLICIT, PREFIX, SUFFIX, UNIQUE, ENCHANTMENT, CORRUPTION, PASSIVE }

/**
 * One action of a modifier: which stat it touches and how.
 *
 * `stat` is a server enum name (STOCK_*, BOOL_*, PROFESSION_*, BATTLE_*) and stays a plain string
 * here — the client never interprets it, it only prints it.
 *
 * With [perStat] set the effect is a conversion: the server multiplies the value by how many whole
 * [perAmount] steps fit into the already-computed source stat ("+1 life per 2 strength"). Which
 * stat that is and what it ends up worth is the server's arithmetic; the client only names it.
 */
@Serializable data class ModifierEffect(
    val stat: String,
    val operation: ModifierOperation,
    val perStat: String? = null,
    val perAmount: Double = 1.0,
) {
    val conversion: Boolean get() = perStat != null
}

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
    /**
     * A local modifier is folded inside its own item and hands out the result; a global one applies
     * to the character. The server keeps two separate descriptions for the two, so this only says
     * which of them this is — the folding itself never happens here.
     */
    val isLocal: Boolean = false,
    // The server writes every nullable field, null included, so an absent tag list arrives as null.
    val tags: List<String>? = null,
) {
    val composite: Boolean get() = effects.size > 1
    /**
     * The whole sentence the modifier reads as, with a placeholder per effect.
     *
     * Since 0.14.0 a definition carries no text at all: `modifier.<code>.name` is a template like
     * "+{0} to armour", and the rolled values fill it. So this is not a label above a number — it
     * is the line itself, and [com.sperance.exileforge.core.display.modifierText] completes it.
     */
    val template: String get() = locOr(LocaleKey.modifierName(code), code)
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
 * An applied modifier: rolled onto an item instance, or fixed by a skill-tree node, a class or an
 * item's own base.
 *
 * `values` holds one number per effect of the description, in the same order. Rolling is the
 * server's job: the client never produces one of these itself. A fixed modifier has no tier at
 * all — [tierId] is empty and [tier] is zero — which is what [rolled] tells apart.
 */
@Serializable data class Modifier(
    val modifierId: String,
    val values: List<Double> = emptyList(),
    val tierId: String = "",
    val tier: Int = 0,
) {
    val rolled: Boolean get() = tierId.isNotBlank()
}
