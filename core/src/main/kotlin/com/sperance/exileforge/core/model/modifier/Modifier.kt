package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

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
