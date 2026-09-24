package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.character.Sheet
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.model.hero.EquipmentInstance

/**
 * What the merchant pays for [item], worked out here by the server's rule (2.46.0); null until the
 * rule has arrived, so a price is never guessed.
 */
fun ForgeState.sellPrice(item: EquipmentInstance): Long? = world.statTables.sell.takeIf { it.rarity.isNotEmpty() }
    ?.let { Sheet.sellPrice(item, world.inventoryBases[item.equipmentId], it, play.hero?.stats.orEmpty()) }

/** What putting [item] on would change on the sheet; empty when nothing moves or nothing is known yet. */
fun ForgeState.wearDelta(item: EquipmentInstance): List<StatDelta> {
    val hero = play.hero ?: return emptyList()
    if (world.statTables.stats.isEmpty()) return emptyList()
    return Sheet.wearing(item, hero.character, world.classes.firstOrNull { it.id == hero.character.classId }, hero.tree.nodes,
        hero.inventory, world.inventoryBases, world.definitions, world.statTables, hero.stats)
}

/** The requirements [item] misses against the sheet, in the server's words; empty means it can be worn. */
fun ForgeState.unmetFor(equipmentId: String): List<String> = play.hero?.sheet?.unwearableBy?.get(equipmentId).orEmpty()
