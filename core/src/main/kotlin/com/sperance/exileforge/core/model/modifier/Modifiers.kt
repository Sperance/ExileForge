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
/** Where a modifier comes from; since server 0.38.0 also the smith's handcrafted lines and a map's alchemy lines, which no orb touches. */
@Serializable enum class ModifierSource { IMPLICIT, PREFIX, SUFFIX, UNIQUE, ENCHANTMENT, CORRUPTION, PASSIVE, HANDCRAFTED, ALCHEMY }

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
    /**
     * Since 0.23.0: two modifiers of one group never share an item, which is why a crafted "+life"
     * cannot stand beside a rolled one. Null means the group is the code itself. Enforced by the
     * server; the client only reads it to say why a bench line would be refused.
     */
    val group: String? = null,
    /**
     * Since server 0.39.0 a pool is a tag, and this is which pools the modifier sits in and how
     * heavily (`helmet`, `local:armor`, `influence:SHAPER`, `corruption`). A template names the tags it
     * rolls from; the server's roller draws by these weights. Never used here to roll.
     */
    val pools: Map<String, Int> = emptyMap(),
    /** The influence (`SHAPER`, `ELDER`) an item must carry for this modifier to roll; null for the rest. */
    val influence: String? = null,
    /** A bench modifier: no orb rolls it, the crafting bench places it, one per item. */
    val crafted: Boolean = false,
    /** Since server 0.54.0 the world bundle carries the tiers too: how good a roll is inside its own. */
    val tiers: List<TierRange> = emptyList(),
) {
    val composite: Boolean get() = effects.size > 1
    val family: String get() = group ?: code
    /**
     * The whole sentence the modifier reads as, with a placeholder per effect.
     *
     * Since 0.14.0 a definition carries no text at all: `modifier.<code>.name` is a template like
     * "+{0} to armour", and the rolled values fill it. So this is not a label above a number — it
     * is the line itself, and [com.sperance.exileforge.core.display.modifierText] completes it.
     */
    val template: String get() = locOr(LocaleKey.modifierName(code), code)
}

/** One tier as the world bundle carries it: its number and a `[min, max]` per effect. */
@Serializable data class TierRange(val tier: Int, val values: List<List<Double>> = emptyList())

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
    /** A fractured affix (Fracturing Orb, since 0.23.0): no orb removes, rerolls or changes it again. */
    val fractured: Boolean = false,
) {
    val rolled: Boolean get() = tierId.isNotBlank()
}

/**
 * One line of the crafting bench (`GET /api/v1/characterequipment/bench`, since 0.23.0): a crafted
 * modifier in one tier, and what placing it costs.
 *
 * The price, the tier's range and which slots take it are the server's. [values] is there so the
 * line reads "+(70–79) to maximum Life" before anything is crafted; the roll inside that range
 * happens on the server when the command arrives.
 */
@Serializable data class BenchRecipe(
    val code: String,
    val modifierId: String = "",
    val modifierCode: String = "",
    val tierId: String = "",
    val tier: Int = 1,
    val source: ModifierSource = ModifierSource.PREFIX,
    val group: String = "",
    val values: List<ModifierTierValue> = emptyList(),
    /** The orb's code, as `CurrencyOrb` names it. */
    val orb: String = "",
    /** The orb's `items` id: what the bag counts. */
    val orbItemId: String = "",
    val amount: Long = 1,
    /** The slots that take this line; empty means every slot. */
    val slots: List<String> = emptyList(),
) {
    fun fits(slot: String): Boolean = slots.isEmpty() || slot in slots
}
