package com.sperance.exileforge.core.editor

import com.sperance.exileforge.core.model.EntitySource
import kotlinx.serialization.json.*

sealed interface InputSpec {
    data class Reference(val source: EntitySource) : InputSpec
    data class Text(val suggestions: List<String> = emptyList()) : InputSpec
    data class Number(val integer: Boolean = false, val min: Double? = null, val max: Double? = null) : InputSpec
    data object Flag : InputSpec
    data class Select(val options: List<String>) : InputSpec
    data class Object(val schema: String) : InputSpec
    data class ListOf(val element: InputSpec) : InputSpec
    data class Union(val variants: Map<String, String>) : InputSpec
}
