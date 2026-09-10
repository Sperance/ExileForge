package com.sperance.exileforge.core.generation

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.modifier.AffixType
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierValue
import kotlin.math.round
import kotlin.random.Random
import kotlinx.serialization.json.*

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
