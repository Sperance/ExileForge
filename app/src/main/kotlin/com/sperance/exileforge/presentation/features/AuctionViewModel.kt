package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.Reads
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

/**
 * The player auction.
 *
 * Every rule belongs to the server: what a lot costs, who may trade, whether the goods are still
 * there. The client names a lot and prints the refusal. After a trade it re-reads only what the
 * trade actually changed — the bag after a purchase, the inventory after a listing — rather than
 * the whole hero, so a showcase stays usable while a character is being traded from. What a patch
 * cannot reach it marks stale instead: the character document itself is left for the Hero tab to
 * re-read, because a trade moves money the auction never looked at.
 */
class AuctionViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun auctionTab(tab: Int) { with(runtime) { mutable.update { it.copy(market = it.market.copy(tab = tab)) } } }
    fun auctionFilter(filter: AuctionFilter) { with(runtime) { mutable.update { it.copy(market = it.market.copy(filter = filter)) } } }
    fun showOwnLots(show: Boolean) { with(runtime) { mutable.update { it.copy(market = it.market.copy(showOwnLots = show)) } } }

    /** The showcase, page by page. `excludeSellerId` is what keeps a seller's own lots out of it. */
    fun loadShowcase(page: Int = 0) { with(runtime) { trade(restart = true) {
        val id = state.value.play.characterId.trim()
        val filter = state.value.market.filter.copy(
            excludeSellerId = if (state.value.market.showOwnLots) "" else id, lang = state.value.lang.code)
        val showcase = api.auction.search(id, filter, page)
        mutable.update { it.copy(market = it.market.copy(showcase = showcase)) }
    } } }

    fun loadMyLots() { with(runtime) { trade(key = Reads.LOTS) {
        val id = state.value.play.characterId.trim()
        val lots = api.auction.myLots(id)
        val slots = api.auction.slots(id)
        mutable.update { it.copy(market = it.market.copy(myLots = lots, slots = slots)) }
    } } }

    /** The merchant's shelf (0.34.0): the server rolls it every four hours, nobody renews it sooner. */
    fun loadMerchant() { with(runtime) { trade(key = Reads.MERCHANT) {
        // An offer is drawn as the stash draws an item, base and all: the catalogue comes first.
        ensureEquipment(); ensureDefinitions()
        val stock = api.merchant.stock(state.value.play.characterId.trim())
        mutable.update { it.copy(market = it.market.copy(merchant = stock)) }
    } } }

    /** Every list at once, for opening the tab and for a pull: separate reads, so none waits on another. */
    fun loadAuction() { loadShowcase(0); loadMyLots(); loadMerchant() }

    /** Buying from the merchant spends gold and adds to the stash: those, and the shelf, change. */
    fun buyOffer(offerId: String) { with(runtime) { trade(writing = true) {
        val id = state.value.play.characterId.trim()
        val purchase = api.merchant.buy(id, offerId)
        mutable.update { it.copy(message = ui("merchant.bought"), market = it.market.copy(
            merchant = it.market.merchant?.let { stock -> stock.copy(offers = stock.offers.filter { offer -> offer.id != offerId }) })) }
        gold(purchase.money)
        refreshInventory(id)
    } } }

    /** One more lot place for gold (0.34.0). */
    fun buySlot() { with(runtime) { trade(writing = true) {
        val slots = api.auction.buySlot(state.value.play.characterId.trim())
        mutable.update { it.copy(message = ui("auction.slot_bought", slots.limit), market = it.market.copy(slots = slots)) }
        gold(slots.money)
    } } }

    /** The purse the header prints, as the server answered it. */
    private fun gold(money: Long) { with(runtime) {
        mutable.update { s -> s.copy(play = s.play.copy(hero = s.play.hero?.let { it.copy(character = it.character.copy(money = money)) })) }
    } }

    /** Buying costs orbs out of the bag, so the bag and the showcase are what go stale. */
    fun buy(lotId: String) { with(runtime) { trade(writing = true) {
        val id = state.value.play.characterId.trim()
        val lot = api.auction.buy(id, lotId)
        mutable.update { it.copy(message = ui("auction.bought", lot.title)) }
        refreshBag(id)
        val filter = state.value.market.filter.copy(excludeSellerId = if (state.value.market.showOwnLots) "" else id, lang = state.value.lang.code)
        mutable.update { it.copy(market = it.market.copy(showcase = api.auction.search(id, filter, state.value.market.showcase.page))) }
    } } }

    /** Listing and withdrawing move goods between the character and the lot: both lists change. */
    fun sellEquipment(inventoryId: String, priceOrbId: String, price: Long) { with(runtime) { trade(writing = true) {
        val id = state.value.play.characterId.trim()
        val lot = api.auction.sellEquipment(id, inventoryId, priceOrbId, price)
        listed(id, lot.title)
    } } }

    fun sellItem(itemId: String, amount: Long, priceOrbId: String, price: Long) { with(runtime) { trade(writing = true) {
        val id = state.value.play.characterId.trim()
        val lot = api.auction.sellItem(id, itemId, amount, priceOrbId, price)
        listed(id, lot.title)
    } } }

    fun cancel(lotId: String) { with(runtime) { trade(writing = true) {
        val id = state.value.play.characterId.trim()
        val lot = api.auction.cancel(id, lotId)
        mutable.update { it.copy(message = ui("auction.withdrawn", lot.title)) }
        refreshInventory(id)
        mutable.update { it.copy(market = it.market.copy(myLots = api.auction.myLots(id), slots = api.auction.slots(id))) }
    } } }

    private suspend fun listed(characterId: String, title: String) { with(runtime) {
        mutable.update { it.copy(message = ui("auction.listed", title)) }
        refreshInventory(characterId)
        mutable.update { it.copy(market = it.market.copy(myLots = api.auction.myLots(characterId), slots = api.auction.slots(characterId), tab = 1)) }
    } }

    /** The bag alone: a purchase spends orbs and may hand over stacking goods. */
    private suspend fun refreshBag(characterId: String) { with(runtime) {
        val bag = api.hero.bag(characterId)
        mutable.update { state -> state.copy(play = state.play.copy(hero = state.play.hero?.copy(bag = bag), heroReadAt = 0)) }
    } }

    /** The inventory and the bag: a listing can move either kind of goods. */
    private suspend fun refreshInventory(characterId: String) { with(runtime) {
        val inventory = api.hero.inventory(characterId)
        val bag = api.hero.bag(characterId)
        mutable.update { state -> state.copy(play = state.play.copy(hero = state.play.hero?.copy(inventory = inventory, bag = bag), heroReadAt = 0)) }
    } }

    /**
     * The standard wrapper plus the auction's own gate.
     *
     * The level the auction opens at is a server constant, so the client never carries a copy: it
     * sends the request, and a refusal from the auction becomes the screen's explanation.
     */
    private fun trade(writing: Boolean = false, restart: Boolean = false, key: String = Reads.AUCTION, block: suspend () -> Unit) { with(runtime) {
        // A read shows the auction; a trade changes it and the goods it moved, and redoes them.
        if (writing) task(writing = true, touches = setOf(Reads.AUCTION, Reads.LOTS, Reads.HERO, Reads.MERCHANT)) { gated(block) }
        else read(key, restart) { gated(block) }
    } }

    private suspend fun gated(block: suspend () -> Unit) { with(runtime) {
        check(state.value.play.characterId.isNotBlank()) { ui("auction.choose_character") }
        try {
            block()
            mutable.update { it.copy(market = it.market.copy(locked = null)) }
        } catch (e: CancellationException) { throw e }
        catch (e: ApiFailure) {
            // AU_002 alone is the gate; every other auction refusal is an ordinary rejected command.
            if (e.code != LEVEL_GATE) throw e
            mutable.update { it.copy(market = it.market.copy(locked = locError(e.code, e.message.orEmpty()), showcase = AuctionPage(), myLots = emptyList())) }
            throw e
        }
    } }
}

/** The server's code for "this character's level is too low for the auction". */
private const val LEVEL_GATE = "AU_002"
