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

/**
 * The hero's passive skills whose lines name [stat], each with what it adds to the sheet's figure: they are laid on in
 * a fight, not on the sheet, so a figure's window lists them apart. Empty until the hero and the content are here.
 */
fun ForgeState.passiveShares(stat: String): PassiveShares {
    val index = index ?: return PassiveShares()
    val hero = hero ?: return PassiveShares()
    val model = hero.sheet.model ?: return PassiveShares()
    val before = model.plain[stat] ?: 0.0
    val body = Combatant(hero.stats, hero.level, index.campaign.combat)
    val rows = Loadout.of(hero.skills, index.skills, hero.heroClass, emptyList()).passiveSources(body).mapNotNull { (kit, lines) ->
        val own = lines.filter { it.stat == stat }.ifEmpty { return@mapNotNull null }
        PassiveShare(kit.skill.code, own, (model.with(own)[stat] ?: 0.0) - before, kit.skill.lowLife)
    }
    val steady = rows.filterNot { it.lowLife }.flatMap { it.lines }
    return PassiveShares(rows, if (steady.isEmpty()) 0.0 else (model.with(steady)[stat] ?: 0.0) - before)
}
