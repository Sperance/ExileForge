package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.character.GearVerdicts
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.character.WearPlace
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.model.atlas.AtlasState
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.tree.TreeState
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
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

    /** What putting [item] on would change on the sheet - на место [place] (3.90.3) или туда, что выберут правила. */
    fun wearDelta(index: ContentIndex?, hero: HeroView?, item: ItemInstance, place: Slot? = null): List<StatDelta> {
        index ?: return emptyList()
        hero ?: return emptyList()
        return Sheets.wearing(index, item, hero.level, hero.heroClass, hero.tree, hero.items, hero.figures.values, hero.pets.active, place, hero.info.laws, hero.info.fate, hero.skills)
    }

    /**
     * Что изменит в листе взятие узла [code] дерева с вариантом [choice] (у узла с выбором); пусто - героя или контента нет
     * или узел уже взят. Считается клиентом без запроса - вызывать вне главного потока.
     */
    fun allocationDelta(index: ContentIndex?, hero: HeroView?, code: String, choice: Int?): List<StatDelta> {
        index ?: return emptyList()
        hero ?: return emptyList()
        if (hero.tree.any { it.code == code }) return emptyList()
        return Sheets.allocating(index, TakenNode(code, choice), hero.level, hero.heroClass, hero.tree, hero.items, hero.figures.values, hero.pets.active, hero.info.laws, hero.info.fate, hero.skills)
    }

    /** Память вердиктов (3.89.0): одна на приложение, сбрасывается сама с новым героем или контентом. */
    private val verdicts = GearVerdicts()

    /**
     * Урон и защита героя, если надеть [item] (3.89.0) - на место [place] (3.90.3), а без него или для слота с одним местом -
     * на лучшее; null - вещь не надевается, надета или не по требованиям.
     */
    fun verdict(index: ContentIndex?, hero: HeroView?, item: ItemInstance, place: Slot? = null): GearVerdict? = place?.let { wanted -> verdicts.places(index, hero, item).firstOrNull { it.slot == wanted } }?.verdict ?: verdicts.of(index, hero, item)

    /** Места [item] с тем, что там надето, и вердиктом каждого (3.90.3); пусто - вещь не надеть. */
    fun wearPlaces(index: ContentIndex?, hero: HeroView?, item: ItemInstance): List<WearPlace> = verdicts.places(index, hero, item)

    /** The requirements the template [code] misses against the sheet, in the rules' words; empty means it can be worn. */
    fun unmet(index: ContentIndex?, hero: HeroView?, code: String): List<String> {
        index ?: return emptyList()
        hero ?: return emptyList()
        return Sheets.unmet(index, code, hero.level, hero.stats)
    }

    /** What the hero's auras hold of the mana, as the fight holds it (4.6.4, лист боя); null until the hero's sheet is here. */
    fun manaReserve(hero: HeroView?): ManaReserve? = hero?.combat?.let { ManaReserve(it.body.maxMana, it.reserved) }

    /** The orbs of the world, in the order of their price: what the forge and the auction offer. */
    fun orbs(index: ContentIndex?): List<Item> = index?.itemsByCategory?.get(Item.CURRENCY).orEmpty().sortedBy { it.price }

    /** The auction's money (server 1.84.36: every orb, `AuctionRules.trades`): what a lot is priced, bought and filtered in, cheapest first. */
    fun currencies(index: ContentIndex?): List<Item> = index?.let { i -> orbs(i).filter(i.rules.auction::trades) }.orEmpty()

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
fun GameUi.wearDelta(item: ItemInstance, place: Slot? = null): List<StatDelta> = HeroLens.wearDelta(index, hero, item, place)
fun GameUi.allocationDelta(code: String, choice: Int?): List<StatDelta> = HeroLens.allocationDelta(index, hero, code, choice)
fun GameUi.gearVerdict(item: ItemInstance, place: Slot? = null): GearVerdict? = HeroLens.verdict(index, hero, item, place)
fun GameUi.wearPlaces(item: ItemInstance): List<WearPlace> = HeroLens.wearPlaces(index, hero, item)
fun GameUi.unmetFor(code: String): List<String> = HeroLens.unmet(index, hero, code)
fun GameUi.manaReserve(): ManaReserve? = HeroLens.manaReserve(hero)

/**
 * Цифры сводки героя (4.6.4): лист боя с пассивными умениями и Предначертанием ([CombatSheet]); до него - лист как есть. Сводка,
 * разбор и сравнения вещи и узла дерева читают одни и те же цифры.
 */
val HeroView.figures: StatFigures get() = combat?.figures ?: StatFigures(stats)

/** The hero's mana [pool] and the [percent] of it the passive auras hold, as the fight reserves it. */
data class ManaReserve(val pool: Double, val percent: Double) {
    val held: Double get() = pool * percent / 100
    val free: Double get() = pool - held
}
