package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.definition
import kotlinx.serialization.json.JsonObject

/**
 * Mana and spells left the hero's game in 2.48.0 (server 0.43.0): no fight of theirs casts. Their
 * modifiers are gone from the server since 0.56.0, but a monster still carries mana and spell damage
 * and a few bases keep such an implicit, so the client shows none of it — not on the sheet, not on a
 * monster, not on an item — where such a line would promise what nothing delivers.
 */
val retiredStats = setOf(
    "STOCK_MANA", "STOCK_SPELL_BLOCK", "STOCK_ATTACK_MAGICAL", "STOCK_CAST_SPEED", "STOCK_MANA_REGEN",
)

fun retired(stat: String): Boolean = stat in retiredStats

/** A modifier whose every effect is mana or a spell: it is not shown. */
fun ModifierDefinition.retired(): Boolean = effects.isNotEmpty() && effects.all { it.stat in retiredStats }

/** An item's lines as a card draws them, the retired ones left out. */
fun shownLines(lines: List<JsonObject>, definitions: List<ModifierDefinition>): List<JsonObject> =
    lines.filterNot { line -> definitions.definition(line.text("modifierCode"))?.retired() == true }
