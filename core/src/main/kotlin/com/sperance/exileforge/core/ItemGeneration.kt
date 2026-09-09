package com.sperance.exileforge.core

import kotlin.random.Random
import kotlin.math.round
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
fun definitionsOf(document: JsonObject): List<JsonObject> = listOf("modifierDefinitions", "modifierDefinitionsStock")
    .flatMap { (document[it] as? JsonArray).orEmpty() }.mapNotNull { it as? JsonObject }
fun modifierFromDefinition(definition: JsonObject, tierNumber: Int? = null): JsonObject {
    val d = WireJson.decodeFromJsonElement(ModifierDefinition.serializer(), definition)
    val tier = d.tiers.firstOrNull { it.tier == tierNumber } ?: d.tiers.firstOrNull()
    return buildJsonObject {
        put("definitionId", d.id); put("tier", tier?.tier ?: 1); put("source", d.source.name)
        put("values", buildJsonArray { (tier?.values ?: listOf(ValueRange(1.0, 1.0))).forEach { add(buildJsonObject { put("value", it.min) }) } })
        put("tags", JsonArray(d.tags.map { JsonPrimitive(it.value) }))
    }
}
/** Mirrors the current server's rarity counts, tier eligibility and weights without allocating a weighted list. */
class ItemGenerator(private val random: Random = Random.Default) {
    private fun <T> pick(values: List<T>, weight: (T) -> Double): T? {
        if(values.isEmpty()) return null
        val total = values.sumOf(weight)
        if(total <= 0.0) return values.last()
        var roll = random.nextDouble() * total
        return values.firstOrNull { roll -= weight(it); roll < 0 } ?: values.last()
    }
    fun roll(definitions: List<ModifierDefinition>, level: Int, count: Int): List<Modifier> {
        val available = definitions.filter { it.rollable && it.tiers.any { tier -> tier.minItemLevel <= level } }
        val result = mutableListOf<Modifier>()
        repeat(count.coerceAtLeast(0)) {
            val definition = pick(available.filter { it.stackable || result.none { mod -> mod.definitionId == it.id } }) { d -> d.tiers.sumOf { it.weight.toDouble() }.coerceAtLeast(1.0) } ?: return@repeat
            val eligible = definition.tiers.filter { it.minItemLevel <= level }
            val tier = if(eligible.sumOf { it.weight.toDouble() } <= 0) eligible.maxBy { it.tier } else pick(eligible) { it.weight.toDouble() }!!
            result += Modifier(definition.id, tier.values.map {
                val unit = random.nextDouble()
                val raw = it.min * (1.0 - unit) + it.max * unit
                val value = if(it.min == it.max) it.min else if (kotlin.math.abs(raw) < 1e15) (round(raw * 10.0) / 10.0).coerceIn(it.min, it.max) else raw
                ModifierValue(value)
            }, tier.tier, definition.source, definition.tags)
        }
        return result
    }
    fun generate(base: JsonObject): JsonObject {
        validate(base, Catalog.EQUIPMENT)
        val level = base.getValue("itemLevel").jsonPrimitive.int
        val counts = when(base.text("rarity")) { "EPIC" -> 2 to 1; "LEGENDARY" -> 2 to 2; "MYTHICAL" -> 3 to 3; else -> 1 to 1 }
        fun definitions(key: String) = (base[key] as? JsonArray).orEmpty().map { WireJson.decodeFromJsonElement(ModifierDefinition.serializer(), it) }
        val defs = definitions("modifierDefinitions")
        val stock = definitions("modifierDefinitionsStock")
        val rolled = roll(defs.filter { it.affixType == AffixType.PREFIX }, level, counts.first) + roll(defs.filter { it.affixType == AffixType.SUFFIX }, level, counts.second) + roll(stock, level, stock.size)
        return JsonObject(base.filterKeys { it !in protectedFields || it == "type" } + ("modifiers" to buildJsonArray { rolled.forEach { mod ->
            add(buildJsonObject { put("definitionId", mod.definitionId); put("tier", mod.tier); put("source", mod.source.name); put("tags", JsonArray(mod.tags.map { JsonPrimitive(it.value) })); put("values", buildJsonArray { mod.values.forEach { add(buildJsonObject { put("value", it.value) }) } }) })
        } }))
    }
}

fun attachSelectedDefinitions(document: JsonObject, modifiers: JsonElement, available: List<JsonObject>): JsonObject {
    val ids = (modifiers as? JsonArray).orEmpty().map { it.jsonObject.text("definitionId") }.toSet()
    val existing = definitionsOf(document).map { it.text("id") }.toSet()
    val missing = available.filter { it.text("id") in ids && it.text("id") !in existing }.distinctBy { it.text("id") }
    val changed = document + ("modifiers" to modifiers)
    return if(missing.isEmpty()) JsonObject(changed) else JsonObject(changed + ("modifierDefinitions" to JsonArray((document["modifierDefinitions"] as? JsonArray).orEmpty() + missing)))
}
