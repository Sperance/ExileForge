package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.rules.roll.ItemInstance

/** The view of an item over the content on screen, or null before the content has been read. */
fun ForgeState.view(item: ItemInstance): ItemView? = index?.let { ItemView.of(item, it) }

/** What the merchant pays for [item], by the rules' price; null until the content has arrived. */
fun ForgeState.sellPrice(item: ItemInstance): Long? = view(item)?.sellPrice(hero?.stats.orEmpty())

/** What putting [item] on would change on the sheet; empty when nothing moves or nothing is known yet. */
fun ForgeState.wearDelta(item: ItemInstance): List<StatDelta> {
    val index = index ?: return emptyList()
    val hero = hero ?: return emptyList()
    return Sheets.wearing(index, item, hero.level, hero.heroClass, hero.tree, hero.items, hero.stats, hero.pets.active)
}

/** The requirements the template [code] misses against the sheet, in the rules' words; empty means it can be worn. */
fun ForgeState.unmetFor(code: String): List<String> {
    val index = index ?: return emptyList()
    val hero = hero ?: return emptyList()
    return Sheets.unmet(index, code, hero.level, hero.stats)
}
