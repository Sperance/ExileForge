package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
