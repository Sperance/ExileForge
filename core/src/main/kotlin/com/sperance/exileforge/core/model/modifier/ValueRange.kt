package com.sperance.exileforge.core.model.modifier

import kotlinx.serialization.Serializable

@Serializable
data class ValueRange(val min: Double, val max: Double) {
    init { require(min.isFinite() && max.isFinite() && min <= max) { "Invalid value range" } }
}
