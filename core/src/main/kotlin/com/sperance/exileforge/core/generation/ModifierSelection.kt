package com.sperance.exileforge.core.generation

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ValueRange
import kotlinx.serialization.json.*

fun definitionsOf(document: JsonObject): List<JsonObject> = listOf("modifierDefinitions", "modifierDefinitionsStock")
    .flatMap { (document[it] as? JsonArray).orEmpty() }.mapNotNull { it as? JsonObject }
fun modifierFromDefinition(definition: JsonObject, tierNumber: Int? = null): JsonObject {
    val d = WireJson.decodeFromJsonElement(ModifierDefinition.serializer(), definition)
    val tier = d.tiers.firstOrNull { it.tier == tierNumber } ?: d.tiers.firstOrNull()
    return buildJsonObject {
        put("definitionRevision", d.revision); put("definitionId", d.id); put("tier", tier?.tier ?: 1); put("source", d.source.name)
        put("values", buildJsonArray { (tier?.values ?: listOf(ValueRange(1.0, 1.0))).forEach { add(buildJsonObject { put("value", it.min) }) } })
        put("tags", JsonArray(d.tags.map { JsonPrimitive(it.value) }))
    }
}
/** Legacy offline simulator; PoE inventory generation is server-authoritative. */
fun definitionReference(definition: JsonObject): JsonObject = buildJsonObject {
    put("definitionId", definition.text("id")); put("revision", definition.text("revision").toIntOrNull() ?: 1)
}
fun attachSelectedDefinitions(document: JsonObject, modifiers: JsonElement, available: List<JsonObject>): JsonObject {
    val refs = (document["modifierDefinitionRefs"] as? JsonArray).orEmpty() + (modifiers as? JsonArray).orEmpty().map {
        val mod = it.jsonObject
        buildJsonObject { put("definitionId", mod.text("definitionId")); put("revision", mod.text("definitionRevision").toIntOrNull() ?: 1) }
    }
    return JsonObject(document + mapOf("modifiers" to modifiers, "modifierDefinitionRefs" to JsonArray(refs.distinct())))
}
