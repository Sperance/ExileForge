package com.sperance.exileforge.presentation.hero

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.presentation.market.MarketActions
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotGroup
import kotlinx.coroutines.flow.StateFlow

/** Экран героя (3.80.17): сундук, снаряжение, сумка, зверинец и хроника; команды - общие действия героя и рынка. */
class HeroViewModel(
    private val hero: HeroActions,
    private val sync: HeroSync,
    private val market: MarketActions,
    private val connection: ServerConnection,
    heroes: HeroRepository,
    commands: CommandRunner,
) : ViewModel() {
    val holding: StateFlow<HeroHolding> = heroes.state
    val activity: StateFlow<Activity> = commands.state

    fun load() = sync.load()
    fun ensure() = sync.ensure()
    fun selectEquipment(itemId: String) = hero.selectEquipment(itemId)
    fun equip(itemId: String, slot: Slot? = null) = hero.equip(itemId, slot)
    fun unequip(itemId: String) = hero.unequip(itemId)
    fun lockItem(itemId: String, locked: Boolean) = hero.lockItem(itemId, locked)
    fun expandStash() = hero.expandStash()
    fun claimOverflow(itemId: String? = null) = hero.claimOverflow(itemId)
    fun sellOverflow(itemId: String) = hero.sellOverflow(itemId)
    fun incubatePet(egg: String, slot: Int? = null) = hero.incubatePet(egg, slot)
    fun collectPet(slot: Int) = hero.collectPet(slot)
    fun petOrb(petId: String, orb: String, omen: String? = null) = hero.petOrb(petId, orb, omen)
    fun choosePetLine(petId: String, choice: Int) = hero.choosePetLine(petId, choice)
    fun activatePet(petId: String) = hero.activatePet(petId)
    fun releasePet(petId: String) = hero.releasePet(petId)
    fun breedPets(first: String, second: String) = hero.breedPets(first, second)
    fun learnSkill(code: String) = hero.learnSkill(code)
    fun openChest(code: String) = hero.openChest(code)
    fun dismissChest() = hero.dismissChest()
    fun setTitle(title: String) = hero.setTitle(title)
    fun sellForGold(itemId: String) = hero.sellForGold(itemId)
    fun temper(itemId: String) = hero.temper(itemId)
    fun unsocketJewel(itemId: String) = hero.unsocketJewel(itemId)

    /** Статистика героя (3.51.0), читается при открытии хроники; null - чтение не удалось. */
    suspend fun heroStats(heroId: String): Map<String, Long>? = runCatching { connection.api.hero.stats(heroId).values }.getOrNull()

    suspend fun priceHint(itemCode: String, rarity: Rarity?, itemLevel: Int): PriceHint? = market.priceHint(itemCode, rarity, itemLevel)
    fun sellEquipment(itemId: String, priceOrb: String, price: Long) = market.sellEquipment(itemId, priceOrb, price)
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long) = market.sellItem(code, amount, priceOrb, price)

    /** Выдачи администратора и тестера (3.80.30) и код награды, как их просят страницы настроек. */
    fun grant(template: String, rarity: Rarity? = null) = hero.grant(template, rarity)
    fun grantItem(code: String, amount: Long) = hero.grantItem(code, amount)
    fun testerGrant(what: String, vararg params: Pair<String, String?>) = hero.testerGrant(what, *params)
    fun redeem(code: String) = hero.redeem(code)

    fun claimPath() = hero.claimPath()
    fun autoSell(rarity: Rarity, groups: Set<SlotGroup>) = hero.autoSell(rarity, groups)
}
