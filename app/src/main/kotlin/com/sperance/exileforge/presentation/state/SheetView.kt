package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.campaign.Combatant
import com.sperance.exileforge.core.campaign.Loadout
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

/** The hero's mana [pool] and the [percent] of it the passive auras hold, as the fight reserves it. */
data class ManaReserve(val pool: Double, val percent: Double) {
    val held: Double get() = pool * percent / 100
    val free: Double get() = pool - held
}

/** What the hero's auras hold of the mana; null until the hero and the content are both here. */
fun ForgeState.manaReserve(): ManaReserve? {
    val index = index ?: return null
    val hero = hero ?: return null
    val body = Combatant(hero.stats, hero.level, index.campaign.combat)
    return ManaReserve(body.maxMana, Loadout.of(hero.skills, index.skills, hero.heroClass, emptyList()).reserved(body))
}
