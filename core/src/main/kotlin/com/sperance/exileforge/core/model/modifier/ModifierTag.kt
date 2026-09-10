package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

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
