package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
