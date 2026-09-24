package com.sperance.exileforge.ui.forms

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.editor.InputSpec
import com.sperance.exileforge.core.editor.defaultObject
import kotlinx.serialization.json.*

internal fun JsonObject.changed(key: String, value: JsonElement) = JsonObject(this + (key to value))

internal fun inputDefault(spec: InputSpec): JsonElement = when (spec) {
    is InputSpec.Reference -> JsonPrimitive("")
    is InputSpec.Object -> defaultObject(spec.schema)
    is InputSpec.Text -> JsonPrimitive(spec.suggestions.firstOrNull().orEmpty())
    is InputSpec.Number -> JsonPrimitive(0)
    InputSpec.Flag -> JsonPrimitive(false)
    is InputSpec.ListOf -> JsonArray(emptyList())
    is InputSpec.Select -> JsonPrimitive(spec.options.first())
    InputSpec.Weights -> JsonObject(emptyMap())
}

internal fun summary(value: JsonElement): String = when (value) {
    is JsonObject -> listOf("name", "code", "stat", "itemId", "equipmentId", "modifierId").firstNotNullOfOrNull { value.text(it).takeIf(String::isNotBlank) }.orEmpty()
    is JsonPrimitive -> value.content
    else -> ""
}
