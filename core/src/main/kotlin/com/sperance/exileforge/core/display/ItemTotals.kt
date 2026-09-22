package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import kotlinx.serialization.json.*
import kotlin.math.round

/**
 * What an item is worth once its own modifiers have been applied to its own base.
 *
 * A helmet with 100 armour and "+20% increased armour" wears 120, and 120 is the number a player
 * weighs against another helmet — the two halves side by side are a puzzle, not an answer. Path of
 * Exile colours such a value to say it is not the base any more, and keeps the affix in the list
 * below, so the sum and its reason are both readable.
 *
 * Only *local* modifiers fold: a local one is counted inside its own item and hands the result out,
 * a global one applies to the character and has nothing to do with this line. Which is which is the
 * server's to say — `ModifierDefinition.isLocal` — and the arithmetic below is the same formula the
 * server uses, so the two never disagree:
 *
 * `total = (base + Σ ADD) * (1 + Σ INCREASED / 100) * Π (1 + MORE / 100)`, and SET replaces all of it.
 */

/** One value inside a property line: what the base said, and what the item actually carries. */
data class PropertyValue(val stat: String, val base: Double, val total: Double) {
    val augmented: Boolean get() = round(total * 10) != round(base * 10)
    val text: String get() = statNumber(stat, total)
    val baseText: String get() = statNumber(stat, base)
}

/**
 * One line of an item's base, ready to be drawn.
 *
 * [template] is the dictionary's sentence with a `{0}` per value; when the dictionary has no
 * sentence for this modifier it is empty and the caller falls back to naming the stats itself.
 */
data class BaseProperty(val modifierId: String, val template: String, val values: List<PropertyValue>) {
    val augmented: Boolean get() = values.any { it.augmented }
    /** The whole line as plain text, for a row that cannot colour part of a sentence. */
    fun line(): String {
        if (template.isNotBlank()) return values.foldIndexed(template) { index, text, value -> text.replace("{$index}", value.text) }
        return values.joinToString(" · ") { if (it.stat.isBlank()) it.text else "${it.text} ${statTitle(it.stat)}" }
    }
}

/**
 * The base of an item, with every local modifier of that item already in it.
 *
 * Reads `baseParams` for the lines and `params` for what was rolled onto this copy. A document
 * without a base — an `items` row, a template that carries none — answers with nothing.
 */
fun baseProperties(document: JsonObject, definitions: List<ModifierDefinition>): List<BaseProperty> {
    val base = (document["baseParams"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    if (base.isEmpty()) return emptyList()
    val rolled = (document["params"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

    val baseTotals = fold(base, definitions)
    val totals = fold(base + rolled, definitions)

    return base.map { modifier ->
        val definition = definitions.firstOrNull { it.id == modifier.text("modifierId") }
        // A definition the client has not read yet still prints its numbers: a value the server
        // wrote should never vanish because the reference tables have not arrived.
        val values = (modifier["values"] as? JsonArray).orEmpty().mapIndexedNotNull { index, raw ->
            val own = (raw as? JsonPrimitive)?.doubleOrNull ?: return@mapIndexedNotNull null
            val stat = definition?.effects?.getOrNull(index)?.stat.orEmpty()
            PropertyValue(stat, baseTotals[stat] ?: own, totals[stat] ?: own)
        }
        val template = definition?.template?.takeIf { it != definition.code }
        BaseProperty(modifier.text("modifierId"), template.orEmpty(), values)
    }.filter { it.values.isNotEmpty() }
}

/** Every local operation of these modifiers, swept into one number per characteristic. */
private fun fold(modifiers: List<JsonObject>, definitions: List<ModifierDefinition>): Map<String, Double> {
    val operations = mutableMapOf<String, MutableList<Pair<ModifierOperation, Double>>>()
    modifiers.forEach { modifier ->
        val definition = definitions.firstOrNull { it.id == modifier.text("modifierId") } ?: return@forEach
        if (!definition.isLocal) return@forEach
        val values = (modifier["values"] as? JsonArray).orEmpty()
        definition.effects.forEachIndexed { index, effect ->
            // A conversion reads another characteristic, and that one is the character's rather
            // than the item's — there is nothing inside an item to convert from.
            if (effect.conversion) return@forEachIndexed
            val value = (values.getOrNull(index) as? JsonPrimitive)?.doubleOrNull ?: return@forEachIndexed
            operations.getOrPut(effect.stat) { mutableListOf() } += effect.operation to value
        }
    }
    return operations.mapValues { (_, ops) -> apply(ops) }
}

/** The Path of Exile formula, as the server writes it in ModifierMath. */
private fun apply(operations: List<Pair<ModifierOperation, Double>>): Double {
    var result = operations.filter { it.first == ModifierOperation.ADD }.sumOf { it.second }
    result *= 1.0 + operations.filter { it.first == ModifierOperation.INCREASED }.sumOf { it.second } / 100.0
    operations.filter { it.first == ModifierOperation.MORE }.forEach { result *= 1.0 + it.second / 100.0 }
    operations.lastOrNull { it.first == ModifierOperation.SET }?.let { result = it.second }
    return round(result * 10) / 10
}

/**
 * The true/false states of a document — corrupted, mirrored, worn, socketed — as a list of codes.
 *
 * Read off the document rather than listed here, so a flag the server grows tomorrow shows up
 * without the client being rebuilt; [stateTitle] falls back to the field's own name until it has
 * one in the dictionary. Two states are not fields but readings of one: an item is worn when it
 * has a slot and socketed when it names a node.
 */
fun itemStates(document: JsonObject): List<String> {
    val flags = document.keys.filter { key ->
        key !in SERVICE_FLAGS && (document[key] as? JsonPrimitive)?.booleanOrNull == true
    }
    val worn = listOfNotNull(
        "equipped".takeIf { document.text("equippedSlot").isNotBlank() },
        "socketed".takeIf { document.text("socketCode").isNotBlank() },
    )
    return (flags + worn).sortedBy { STATE_ORDER.indexOf(it).takeIf { at -> at >= 0 } ?: STATE_ORDER.size }
}

/** Bookkeeping the server keeps on every document; none of it is a state of the thing itself. */
private val SERVICE_FLAGS = setOf("deleted", "isActive", "needOpenRecipe")

/** The order states are read in, so two items never list the same pair differently. */
private val STATE_ORDER = listOf("corrupted", "mirrored", "equipped", "socketed")

/**
 * What a state is called.
 *
 * A flag the dictionary has no name for yet reads as its own field, capitalised so it still looks
 * like a label rather than a leaked variable.
 */
fun stateTitle(state: String, lang: Lang = uiLanguage): String =
    uiOr(lang, "state.$state", displayName(state, lang).replaceFirstChar { it.uppercase() })
