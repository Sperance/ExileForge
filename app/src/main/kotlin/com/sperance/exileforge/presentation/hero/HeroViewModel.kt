package com.sperance.exileforge.presentation.hero

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.market.MarketActions
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.HeroPage
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Экран героя: сундук, снаряжение, сумка и хроника; команды - общие действия героя и рынка. */
class HeroViewModel(
    private val hero: HeroActions,
    private val sync: HeroSync,
    private val market: MarketActions,
    private val reads: HeroReads,
    heroes: HeroRepository,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
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
    fun learnSkill(code: String) = hero.learnSkill(code)
    fun openChest(code: String) = hero.openChest(code)
    fun dismissChest() = hero.dismissChest()
    fun setTitle(title: String) = hero.setTitle(title)
    fun sellForGold(itemId: String) = hero.sellForGold(itemId)
    fun sellMany(itemIds: Collection<String>) = hero.sellMany(itemIds)
    fun temper(itemId: String) = hero.temper(itemId)
    fun unsocketJewel(itemId: String) = hero.unsocketJewel(itemId)

    /** Статистика героя, свежая полминуты; null - чтение не удалось. */
    suspend fun heroStats(heroId: String): Map<String, Long>? = reads.stats(heroId)

    suspend fun priceHint(itemCode: String, rarity: Rarity?, itemLevel: Int): PriceHint? = market.priceHint(itemCode, rarity, itemLevel)
    fun sellEquipment(itemId: String, priceOrb: String, price: Long) = market.sellEquipment(itemId, priceOrb, price)
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long) = market.sellItem(code, amount, priceOrb, price)

    /** Выдачи администратора и тестера и код награды, как их просят страницы настроек. */
    fun grant(template: String, rarity: Rarity? = null) = hero.grant(template, rarity)
    fun grantItem(code: String, amount: Long) = hero.grantItem(code, amount)
    fun testerGrant(what: String, vararg params: Pair<String, String?>) = hero.testerGrant(what, *params)
    fun redeem(code: String) = hero.redeem(code)

    fun autoSell(rarity: Rarity, groups: Set<SlotGroup>) = hero.autoSell(rarity, groups)
    fun autoSellUnwearable(on: Boolean) = hero.autoSellUnwearable(on)

    private val openPage = MutableStateFlow(HeroPage.CHARACTER)

    /** Открытый раздел героя: полоса разделов в оболочке и экран героя читают один и тот же. */
    val page: StateFlow<HeroPage> = openPage.asStateFlow()

    fun page(page: HeroPage) {
        openPage.value = page
    }
}
