package com.sperance.exileforge.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Wire contract: ktor-bestgame 5fb037f3ba6a60f5e45da9da35832e2165339432.
@Serializable
data class Modifier(

    val definitionId: String,

    val values: List<ModifierValue>,

    val tier: Int,

    val source: ModifierSource,

    val tags: Set<ModifierTag> = emptySet(),
    val definitionRevision: Int = 1
) {

    val value: Double
        get() = values.firstOrNull()?.value ?: 0.0

    fun value(index: Int): Double {
        return values.getOrNull(index)?.value ?: 0.0
    }
}

@Serializable
data class ModifierDefinition(

    val id: String,

    val name: String,

    val source: ModifierSource,

    val scope: ModifierScope = ModifierScope.ITEM,

    val affixType: AffixType? = null,

    val tiers: List<ModifierTier> = emptyList(),

    val tags: Set<ModifierTag> = emptySet(),

    val conditions: List<ModifierCondition> = emptyList(),

    val effects: List<ModifierEffect> = emptyList(),

    val priority: Int = 0,

    val rollable: Boolean = true,

    val stackable: Boolean = false,

    val revision: Int = 1,
    val enabled: Boolean = true,
    val _id: String? = null
)

@Serializable
data class ModifierValue(
    val value: Double
)

@Serializable
enum class ModifierSource {

    BASE_ITEM,

    PREFIX,

    SUFFIX,

    UNIQUE,

    ENCHANTMENT,

    CORRUPTION,

    PASSIVE,

    SKILL,

    AURA,

    FLASK,

    JEWEL,

    MAP,

    MONSTER,

    TEMPORARY,

    SYSTEM
}

@Serializable
@JvmInline
value class ModifierTag(
    val value: String
) {
    init {
        require(value.isNotBlank()) {
            "ModifierTag cannot be blank"
        }
    }

    override fun toString(): String = value
}

@Serializable
data class ModifierTier(

    val tier: Int,

    val minItemLevel: Int = 1,

    val weight: Int = 100,

    val values: List<ValueRange> = emptyList()
) {

    init {
        require(tier > 0) {
            "Tier must be greater than zero"
        }

        require(minItemLevel >= 1) {
            "minItemLevel must be >= 1"
        }

        require(weight >= 0) {
            "weight cannot be negative"
        }
    }
}

@Serializable
sealed interface ModifierEffect {

    @Serializable
    @SerialName("stat")
    data class Stat(
        val stat: StatId,
        val operation: ModifierOperation,
        val value: ValueExpression
    ) : ModifierEffect

    @Serializable
    @SerialName("derived_stat")
    data class DerivedStat(
        val targetStat: StatId,
        val sourceStat: StatId,
        val operation: ModifierOperation,
        val value: ValueExpression
    ) : ModifierEffect

    @Serializable
    @SerialName("damage_conversion")
    data class DamageConversion(
        val from: String,
        val to: String,
        val percentage: ValueExpression
    ) : ModifierEffect

    @Serializable
    @SerialName("damage_taken_as")
    data class DamageTakenAs(
        val from: String,
        val to: String,
        val percentage: ValueExpression
    ) : ModifierEffect

    @Serializable
    @SerialName("penetration")
    data class Penetration(
        val damageType: String,
        val percentage: ValueExpression
    ) : ModifierEffect

    @Serializable
    @SerialName("gain_resource")
    data class GainResource(
        val resource: String,
        val amount: ValueExpression
    ) : ModifierEffect

    @Serializable
    @SerialName("chance_to_apply")
    data class ChanceToApply(
        val effectId: String,
        val chance: ValueExpression
    ) : ModifierEffect

    @Serializable
    @SerialName("add_tag")
    data class AddTag(
        val tag: String
    ) : ModifierEffect

    @Serializable
    @SerialName("remove_tag")
    data class RemoveTag(
        val tag: String
    ) : ModifierEffect

    @Serializable
    @SerialName("grant_effect")
    data class GrantEffect(
        val effectId: String
    ) : ModifierEffect
}

@Serializable
sealed interface ValueExpression {

    @Serializable
    @SerialName("constant")
    data class Constant(
        val value: Double
    ) : ValueExpression

    @Serializable
    @SerialName("stat")
    data class Stat(
        val stat: StatId
    ) : ValueExpression

    @Serializable
    @SerialName("modifier_value")
    data class ModifierValue(
        val index: Int = 0
    ) : ValueExpression

    @Serializable
    @SerialName("add")
    data class Add(
        val left: ValueExpression,
        val right: ValueExpression
    ) : ValueExpression

    @Serializable
    @SerialName("subtract")
    data class Subtract(
        val left: ValueExpression,
        val right: ValueExpression
    ) : ValueExpression

    @Serializable
    @SerialName("multiply")
    data class Multiply(
        val left: ValueExpression,
        val right: ValueExpression
    ) : ValueExpression

    @Serializable
    @SerialName("divide")
    data class Divide(
        val left: ValueExpression,
        val right: ValueExpression
    ) : ValueExpression

    @Serializable
    @SerialName("percentage")
    data class Percentage(
        val expression: ValueExpression
    ) : ValueExpression

    @Serializable
    @SerialName("min")
    data class Min(
        val left: ValueExpression,
        val right: ValueExpression
    ) : ValueExpression

    @Serializable
    @SerialName("max")
    data class Max(
        val left: ValueExpression,
        val right: ValueExpression
    ) : ValueExpression
}

@Serializable
sealed interface ModifierCondition {

    @Serializable
    @SerialName("always")
    data object Always : ModifierCondition

    @Serializable
    @SerialName("stat_at_least")
    data class StatAtLeast(
        val stat: StatId,
        val value: Double
    ) : ModifierCondition

    @Serializable
    @SerialName("stat_at_most")
    data class StatAtMost(
        val stat: StatId,
        val value: Double
    ) : ModifierCondition

    @Serializable
    @SerialName("has_tag")
    data class HasTag(
        val tag: String
    ) : ModifierCondition

    @Serializable
    @SerialName("target_has_tag")
    data class TargetHasTag(
        val tag: String
    ) : ModifierCondition

    @Serializable
    @SerialName("full_life")
    data object FullLife : ModifierCondition

    @Serializable
    @SerialName("low_life")
    data object LowLife : ModifierCondition

    @Serializable
    @SerialName("has_effect")
    data class HasEffect(
        val effectId: String
    ) : ModifierCondition

    @Serializable
    @SerialName("and")
    data class And(
        val conditions: List<ModifierCondition>
    ) : ModifierCondition

    @Serializable
    @SerialName("or")
    data class Or(
        val conditions: List<ModifierCondition>
    ) : ModifierCondition

    @Serializable
    @SerialName("not")
    data class Not(
        val condition: ModifierCondition
    ) : ModifierCondition
}

@Serializable
enum class ModifierScope {

    ITEM,

    CHARACTER,

    SKILL,

    ATTACK,

    SPELL,

    HIT,

    TARGET,

    AREA,

    PARTY
}

@Serializable
enum class ModifierOperation {

    FLAT,

    INCREASED,

    REDUCED,

    MORE,

    LESS,

    SET,

    MIN,

    MAX
}

@Serializable
enum class AffixType {

    PREFIX,

    SUFFIX
}

@JvmInline
@Serializable
value class StatId(
    val value: String
) {
    init {
        require(value.isNotBlank()) {
            "StatId cannot be blank"
        }
    }

    override fun toString(): String = value
}

@Serializable
data class ValueRange(val min: Double, val max: Double) {
    init { require(min.isFinite() && max.isFinite() && min <= max) { "Invalid value range" } }
}
