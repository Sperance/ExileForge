package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.model.atlas.AtlasState
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.tree.TreeState
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.SheetCalculator

/**
 * Герой над контентом (3.80.33): что экраны выводят из пары «контент + герой». Одна логика для срезов экранов.
 */
internal object HeroLens {
    /** The view of an item over the content on screen, or null before the content has been read. */
    fun view(index: ContentIndex?, item: ItemInstance): ItemView? = index?.let { ItemView.of(item, it) }

    /** What the merchant pays for [item], by the rules' price; null until the content has arrived. */
    fun sellPrice(index: ContentIndex?, hero: HeroView?, item: ItemInstance): Long? = view(index, item)?.sellPrice(hero?.stats.orEmpty())

    /** What putting [item] on would change on the sheet; empty when nothing moves or nothing is known yet. */
    fun wearDelta(index: ContentIndex?, hero: HeroView?, item: ItemInstance): List<StatDelta> {
        index ?: return emptyList()
        hero ?: return emptyList()
        return Sheets.wearing(index, item, hero.level, hero.heroClass, hero.tree, hero.items, hero.stats, hero.pets.active)
    }

    /** The requirements the template [code] misses against the sheet, in the rules' words; empty means it can be worn. */
    fun unmet(index: ContentIndex?, hero: HeroView?, code: String): List<String> {
        index ?: return emptyList()
        hero ?: return emptyList()
        return Sheets.unmet(index, code, hero.level, hero.stats)
    }

    /** What the hero's auras hold of the mana; null until the hero and the content are both here. */
    fun manaReserve(index: ContentIndex?, hero: HeroView?): ManaReserve? {
        index ?: return null
        hero ?: return null
        val body = Combatant(hero.stats, hero.level, index.campaign.combat)
        return ManaReserve(body.maxMana, Loadout.of(hero.skills, index.skills, hero.heroClass, emptyList()).reserved(body))
    }

    /**
     * The hero's passive skills whose lines name [stat], each with what it adds to the sheet's figure: they are laid on in
     * a fight, not on the sheet, so a figure's window lists them apart. Empty until the hero and the content are here.
     */
    fun passiveShares(index: ContentIndex?, hero: HeroView?, stat: String): PassiveShares {
        index ?: return PassiveShares()
        hero ?: return PassiveShares()
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

    /** The orbs of the world, in the order of their price: what the forge and the auction offer. */
    fun orbs(index: ContentIndex?): List<Item> = index?.itemsByCategory?.get(Item.CURRENCY).orEmpty().sortedBy { it.price }

    /** The auction's money (server 1.65.0): the base orbs a lot is priced, bought and filtered in, cheapest first. */
    fun currencies(index: ContentIndex?): List<Item> = index?.let { i -> orbs(i).filter { i.rules.auction.trades(it.code) } }.orEmpty()

    /** The bench lines the hero has found; the rest of the bench stays hidden. */
    fun bench(index: ContentIndex?, hero: HeroView?): List<BenchRecipe> = index?.let { i -> hero?.let { h -> i.bench.filter { it.code in h.info.recipes } } }.orEmpty()

    /** Which zones the hero has passed and which are open: derived from the hero, no request. */
    fun progress(index: ContentIndex?, hero: HeroView?): CampaignProgress? = index?.let { i -> hero?.let { h -> CampaignProgress(h.campaign.cleared.filter { it in i.zones }, i.world.unlocked(h.campaign.cleared)) } }

    /** The hero's atlas as the rules count it: points earned and free. */
    fun atlasState(index: ContentIndex?, hero: HeroView?): AtlasState? = index?.let { i ->
        hero?.let { h ->
            AtlasState(
                listOf(i.atlasGraph.start) + h.info.atlas,
                h.info.earned,
                AtlasPoints.total(i.atlas.points, h.info.earned, i.atlas.cap),
                AtlasPoints.available(i.atlas.points, h.info.earned, h.info.atlas, i.atlas.cap),
            )
        }
    }

    /** The hero's tree as the rules count it: the point balance and what the taken nodes give. */
    fun treeState(index: ContentIndex?, hero: HeroView?): TreeState? = index?.let { i ->
        hero?.let { h ->
            val total = i.classes.pointsTotal(h.level) + h.info.bonusPoints
            val spent = i.tree.spent(h.tree)
            val calculator = SheetCalculator(i)
            TreeState(total, spent, total - spent, h.tree, calculator.contributions(calculator.expand(i.tree.lines(h.tree))))
        }
    }
}

fun GameUi.view(item: ItemInstance): ItemView? = HeroLens.view(index, item)
fun GameUi.sellPrice(item: ItemInstance): Long? = HeroLens.sellPrice(index, hero, item)
fun GameUi.wearDelta(item: ItemInstance): List<StatDelta> = HeroLens.wearDelta(index, hero, item)
fun GameUi.unmetFor(code: String): List<String> = HeroLens.unmet(index, hero, code)
fun GameUi.manaReserve(): ManaReserve? = HeroLens.manaReserve(index, hero)
fun GameUi.passiveShares(stat: String): PassiveShares = HeroLens.passiveShares(index, hero, stat)

/** The hero's mana [pool] and the [percent] of it the passive auras hold, as the fight reserves it. */
data class ManaReserve(val pool: Double, val percent: Double) {
    val held: Double get() = pool * percent / 100
    val free: Double get() = pool - held
}
