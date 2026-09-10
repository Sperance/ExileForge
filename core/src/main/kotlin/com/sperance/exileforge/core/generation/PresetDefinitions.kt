package com.sperance.exileforge.core.generation

import com.sperance.exileforge.core.contract.starterDefinition
import com.sperance.exileforge.core.editor.statSuggestions
import kotlinx.serialization.json.*

val presetDefinitions: List<JsonObject> = statSuggestions.map { stat ->
    val effect = buildJsonObject {
        put("type", "stat")
        put("stat", stat)
        put("operation", "FLAT")
        put("value", buildJsonObject {
            put("type", "modifier_value")
            put("index", 0)
        })
    }
    JsonObject(starterDefinition() + mapOf(
        "id" to JsonPrimitive(stat),
        "name" to JsonPrimitive(stat),
        "effects" to JsonArray(listOf(effect))
    ))
}
