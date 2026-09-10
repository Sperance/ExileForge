package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
