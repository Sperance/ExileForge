package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierSource
import com.sperance.exileforge.core.model.modifier.definition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * How a line rolled inside its tier (2.60.0, the trade table): 0 is the bottom of the range, 1 its top.
 *
 * A line whose ranges are all fixed rolled the only value it could, so it counts as perfect; a line
 * with no tier the client knows — a base, a tree node, an old bundle — has no quality at all.
 */
fun rollQuality(modifier: JsonObject, definitions: List<ModifierDefinition>): Double? {
    val ranges = tierRanges(modifier, definitions) ?: return null
    val values = rolledNumbers(modifier)
    val shares = ranges.zip(values).mapNotNull { (range, value) ->
        val (low, high) = range.takeIf { it.size == 2 } ?: return@mapNotNull null
        if (high <= low) 1.0 else ((value - low) / (high - low)).coerceIn(0.0, 1.0)
    }
    return shares.takeIf { it.isNotEmpty() }?.average()
}

/** The tier's ranges as the card prints them: "70–79", a fixed one as its single number, effects split by " / ". */
fun rollRange(modifier: JsonObject, definitions: List<ModifierDefinition>): String? {
    val definition = definitions.definition(modifier.text("modifierCode"))
    val ranges = tierRanges(modifier, definitions) ?: return null
    return ranges.mapIndexedNotNull { index, range ->
        val (low, high) = range.takeIf { it.size == 2 } ?: return@mapIndexedNotNull null
        val stat = definition?.effects?.getOrNull(index)?.stat.orEmpty()
        val from = statNumber(stat, low); val to = statNumber(stat, high)
        if (from == to) from else "$from–$to"
    }.joinToString(" / ").ifBlank { null }
}

/**
 * What the head of a card reads before any line: how well the item rolled on average, how many
 * affix places are still open, and the best tier among its rolls. Each is null where it means nothing.
 */
data class RollSummary(val quality: Int?, val openSlots: Int?, val bestTier: Int?)

fun rollSummary(document: JsonObject, lines: List<JsonObject>, definitions: List<ModifierDefinition>): RollSummary {
    val qualities = lines.mapNotNull { rollQuality(it, definitions) }
    val affixes = lines.count { line ->
        definitions.definition(line.text("modifierCode"))?.source.let { it == ModifierSource.PREFIX || it == ModifierSource.SUFFIX }
    }
    return RollSummary(
        quality = qualities.takeIf { it.isNotEmpty() }?.let { Math.round(it.average() * 100).toInt() },
        openSlots = affixPlaces(document.text("rarity"))?.let { (it - affixes).coerceAtLeast(0) },
        bestTier = lines.filter { affixMarks(it, definitions).kind in RANKED }.mapNotNull { it.text("tier").toIntOrNull()?.takeIf { tier -> tier > 0 } }.minOrNull(),
    )
}

/** Prefix and suffix places together, as the server's rarities hold them; null for a rarity that rolls none. */
fun affixPlaces(rarity: String): Int? = when (rarity) {
    "UNCOMMON" -> 2
    "RARE" -> 6
    else -> null
}

private val RANKED = setOf(AffixKind.PREFIX, AffixKind.SUFFIX, AffixKind.FRACTURED, AffixKind.CRAFTED)

private fun tierRanges(modifier: JsonObject, definitions: List<ModifierDefinition>): List<List<Double>>? {
    val tier = modifier.text("tier").toIntOrNull()?.takeIf { it > 0 } ?: return null
    return definitions.definition(modifier.text("modifierCode"))?.tier(tier)?.values?.takeIf { it.isNotEmpty() }
}

private fun rolledNumbers(modifier: JsonObject): List<Double> =
    (modifier["values"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
