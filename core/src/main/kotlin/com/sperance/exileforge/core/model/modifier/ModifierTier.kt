package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

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
