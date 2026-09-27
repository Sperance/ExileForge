package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ModifierDef

/** What no fight delivers, so the client shows it nowhere: blocking a spell apart from a blow is not a thing. */
val retiredStats = setOf("STOCK_SPELL_BLOCK")

fun retired(stat: String): Boolean = stat in retiredStats

/** A modifier whose every effect is retired is not shown. */
fun ModifierDef.retired(): Boolean = effects.isNotEmpty() && effects.all { it.stat in retiredStats }
