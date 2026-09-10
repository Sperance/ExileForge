package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

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
