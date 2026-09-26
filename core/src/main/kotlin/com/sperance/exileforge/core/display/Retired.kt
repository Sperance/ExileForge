package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.definition
import kotlinx.serialization.json.JsonObject

/**
 * What no fight delivers, so the client shows it nowhere — not on the sheet, not on a monster, not on an
 * item. Mana and spells left the game in 2.48.0 (server 0.43.0) and came back in 2.78.0 (server 0.69.0)
 * with the class skills and the monsters' spells; blocking a spell apart from a blow is still not a thing.
 */
val retiredStats = setOf("STOCK_SPELL_BLOCK")

fun retired(stat: String): Boolean = stat in retiredStats

/** A modifier whose every effect is mana or a spell: it is not shown. */
fun ModifierDefinition.retired(): Boolean = effects.isNotEmpty() && effects.all { it.stat in retiredStats }

/** An item's lines as a card draws them, the retired ones left out. */
fun shownLines(lines: List<JsonObject>, definitions: List<ModifierDefinition>): List<JsonObject> =
    lines.filterNot { line -> definitions.definition(line.text("modifierCode"))?.retired() == true }
