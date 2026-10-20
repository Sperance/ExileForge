package com.sperance.exileforge.ui.screens.hero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.expedition.ExpeditionActions
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.hall.HallFocus
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.hero.HeroFate
import com.sperance.exileforge.core.hero.HeroFateModel
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.hero.HeroPage
import com.sperance.exileforge.core.hero.HeroPageModel
import com.sperance.exileforge.core.hero.HeroReads
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.hero.HeroSync
import com.sperance.exileforge.core.market.MarketActions
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.core.model.hero.PlayerCard
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.rules.content.HallBoard
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Экран героя: сундук, снаряжение, сумка и хроника; команды - общие действия героя и рынка. */
class HeroViewModel(
    private val hero: HeroActions,
    private val sync: HeroSync,
    private val market: MarketActions,
    private val reads: HeroReads,
    heroes: HeroRepository,
    commands: CommandRunner,
    slice: GameSlice,
    private val pages: HeroPageModel,
    private val hall: HallFocus,
    fates: HeroFateModel,
    private val expedition: ExpeditionActions,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val holding: StateFlow<HeroHolding> = heroes.state
    val activity: StateFlow<Activity> = commands.state

    /** Дар героя со счётом - лента листа героя. */
    val fate: StateFlow<HeroFate?> = fates.fate

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

    /** Продажа торговцу; проданная вещь сумки захода снимается и с отметок к продаже - счётчик сумки её больше не считает. */
    fun sellForGold(itemId: String, unequip: Boolean = false) = hero.sellForGold(itemId, unequip) { expedition.forget(itemId) }
    fun sellMany(itemIds: Collection<String>) = hero.sellMany(itemIds)
    fun temper(itemId: String) = hero.temper(itemId)
    fun unsocketJewel(itemId: String) = hero.unsocketJewel(itemId)

    /** Статистика героя, свежая полминуты; null - чтение не удалось. */
    suspend fun heroStats(heroId: String): Map<String, Long>? = reads.stats(heroId)

    /** Своя карточка игрока героя [heroId]: место на доске Мощи для сводки; null - чтение не удалось. */
    suspend fun ownCard(heroId: String): PlayerCard? = reads.ownCard(heroId)

    /** Доска славы «Мощь», прокрученная к своему герою [heroId]. */
    fun openMightBoard(heroId: String) = hall.open(HallBoard.MIGHT, heroId)

    suspend fun priceHint(itemCode: String, rarity: Rarity?, itemLevel: Int): PriceHint? = market.priceHint(itemCode, rarity, itemLevel)
    fun sellEquipment(itemId: String, priceOrb: String, price: Long) = market.sellEquipment(itemId, priceOrb, price)
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long) = market.sellItem(code, amount, priceOrb, price)

    /** Выдачи окна тестирования и код награды, как их просят страницы настроек; пустые редкость и слот случайной вещи - любые. */
    fun grant(template: String, rarity: Rarity? = null) = hero.grant(template, rarity)
    fun grantRandom(rarity: String, slot: String) = hero.grantRandom(rarity, slot)
    fun addExperience(amount: Double) = hero.addExperience(amount)
    fun grantItem(code: String, amount: Long) = hero.grantItem(code, amount)
    fun testerGrant(what: String, vararg params: Pair<String, String?>) = hero.testerGrant(what, *params)
    fun redeem(code: String) = hero.redeem(code)

    fun autoSell(rarity: Rarity, groups: Set<SlotGroup>) = hero.autoSell(rarity, groups)
    fun autoSellUnwearable(on: Boolean) = hero.autoSellUnwearable(on)
    fun autoSellBelow(levels: Int, rarities: Set<Rarity>) = hero.autoSellBelow(levels, rarities)

    /** Открытый раздел героя: полоса разделов в оболочке и экран героя читают один и тот же. */
    val page: StateFlow<HeroPage> = pages.page

    fun page(page: HeroPage) = pages.page(page)

    /** Тайник с фильтром [filter] из другого экрана: раздел тайника вкладки героя открывается, когда фильтр лёг на устройство. */
    fun showStash(filter: ItemFilter) {
        viewModelScope.launch { pages.showStash(filter) }
    }
}
