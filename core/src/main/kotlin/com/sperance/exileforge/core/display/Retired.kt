package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import kotlinx.serialization.json.JsonObject

/**
 * Mana and spells left the game in 2.48.0 (server 0.43.0, `RETIRED_STATS` there), the life flask in
 * 2.57.0 (server 0.50.0): no fight casts or drinks, no pool rolls them any more, and the client shows none of it — not on the sheet, not on a monster,
 * not on an item a player already owns, where such a line would promise what nothing delivers.
 */
val retiredStats = setOf(
    "STOCK_MANA", "STOCK_SPELL_BLOCK", "STOCK_ATTACK_MAGICAL", "STOCK_CAST_SPEED", "STOCK_MANA_REGEN",
    "STOCK_LEECH_MAGICAL", "STOCK_MANA_ON_KILL", "STOCK_MANA_ON_HIT", "STOCK_CAST_STRENGTH",
    "STOCK_FLASK_CHARGES", "STOCK_FLASK_RECOVERY", "MAP_HERO_FLASK",
)

fun retired(stat: String): Boolean = stat in retiredStats

/** A modifier whose every effect is mana or a spell: it is not shown. */
fun ModifierDefinition.retired(): Boolean = effects.isNotEmpty() && effects.all { it.stat in retiredStats }

/** An item's lines as a card draws them, the retired ones left out. */
fun shownLines(lines: List<JsonObject>, definitions: List<ModifierDefinition>): List<JsonObject> =
    lines.filterNot { line -> definitions.firstOrNull { it.id == line.text("modifierId") }?.retired() == true }
