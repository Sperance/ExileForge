package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

/**
 * The player auction.
 *
 * Every rule belongs to the server: what a lot costs, who may trade, whether the goods are still
 * there. The client names a lot and prints the refusal. After a trade it re-reads only what the
 * trade actually changed — the bag after a purchase, the inventory after a listing — rather than
 * the whole hero, so a showcase stays usable while a character is being traded from.
 */
class AuctionViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun auctionTab(tab: Int) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(auctionTab = tab) } } }
    fun auctionFilter(filter: AuctionFilter) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(auctionFilter = filter) } } }
    fun showOwnLots(show: Boolean) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(showOwnLots = show) } } }

    /** The showcase, page by page. `excludeSellerId` is what keeps a seller's own lots out of it. */
    fun loadShowcase(page: Int = 0) { with(runtime) { trade {
        val id = state.value.characterId.trim()
        val filter = state.value.auctionFilter.copy(
            excludeSellerId = if (state.value.showOwnLots) "" else id)
        val showcase = api.auctionSearch(id, filter, page)
        mutable.update { it.copy(showcase = showcase) }
    } } }

    fun loadMyLots() { with(runtime) { trade {
        val lots = api.myLots(state.value.characterId.trim())
        mutable.update { it.copy(myLots = lots) }
    } } }

    /** Both lists at once, for opening the tab and for the refresh button. */
    fun loadAuction() { with(runtime) { trade {
        val id = state.value.characterId.trim()
        val filter = state.value.auctionFilter.copy(excludeSellerId = if (state.value.showOwnLots) "" else id)
        val showcase = api.auctionSearch(id, filter, 0)
        val lots = api.myLots(id)
        mutable.update { it.copy(showcase = showcase, myLots = lots) }
    } } }

    /** Buying costs orbs out of the bag, so the bag and the showcase are what go stale. */
    fun buy(lotId: String) { with(runtime) { trade(writing = true) {
        val id = state.value.characterId.trim()
        val lot = api.buyLot(id, lotId)
        mutable.update { it.copy(message = tr("Куплено: ${lot.title}", "Bought: ${lot.title}")) }
        refreshBag(id)
        val filter = state.value.auctionFilter.copy(excludeSellerId = if (state.value.showOwnLots) "" else id)
        mutable.update { it.copy(showcase = api.auctionSearch(id, filter, state.value.showcase.page)) }
    } } }

    /** Listing and withdrawing move goods between the character and the lot: both lists change. */
    fun sellEquipment(inventoryId: String, priceOrbId: String, price: Long) { with(runtime) { trade(writing = true) {
        val id = state.value.characterId.trim()
        val lot = api.sellEquipment(id, inventoryId, priceOrbId, price)
        listed(id, lot.title)
    } } }

    fun sellItem(itemId: String, amount: Long, priceOrbId: String, price: Long) { with(runtime) { trade(writing = true) {
        val id = state.value.characterId.trim()
        val lot = api.sellItem(id, itemId, amount, priceOrbId, price)
        listed(id, lot.title)
    } } }

    fun cancel(lotId: String) { with(runtime) { trade(writing = true) {
        val id = state.value.characterId.trim()
        val lot = api.cancelLot(id, lotId)
        mutable.update { it.copy(message = tr("Снято с продажи: ${lot.title}", "Withdrawn: ${lot.title}")) }
        refreshInventory(id)
        mutable.update { it.copy(myLots = api.myLots(id)) }
    } } }

    private suspend fun listed(characterId: String, title: String) { with(runtime) {
        mutable.update { it.copy(message = tr("Выставлено: $title", "Listed: $title")) }
        refreshInventory(characterId)
        mutable.update { it.copy(myLots = api.myLots(characterId), auctionTab = 1) }
    } }

    /** The bag alone: a purchase spends orbs and may hand over stacking goods. */
    private suspend fun refreshBag(characterId: String) { with(runtime) {
        val bag = api.bag(characterId)
        mutable.update { state -> state.copy(hero = state.hero?.copy(bag = bag)) }
    } }

    /** The inventory and the bag: a listing can move either kind of goods. */
    private suspend fun refreshInventory(characterId: String) { with(runtime) {
        val inventory = api.inventory(characterId)
        val bag = api.bag(characterId)
        mutable.update { state -> state.copy(hero = state.hero?.copy(inventory = inventory, bag = bag)) }
    } }

    /**
     * The standard wrapper plus the auction's own gate.
     *
     * The level the auction opens at is a server constant, so the client never carries a copy: it
     * sends the request, and a refusal from the auction becomes the screen's explanation.
     */
    private fun trade(writing: Boolean = false, block: suspend () -> Unit) { with(runtime) { task(writing) {
        check(state.value.characterId.isNotBlank()) { tr("Выберите персонажа", "Choose a character") }
        try {
            block()
            mutable.update { it.copy(auctionLocked = null) }
        } catch (e: CancellationException) { throw e }
        catch (e: ApiFailure) {
            // AU_002 alone is the gate; every other auction refusal is an ordinary rejected command.
            if (e.code != LEVEL_GATE) throw e
            mutable.update { it.copy(auctionLocked = e.message, showcase = AuctionPage(), myLots = emptyList()) }
            throw e
        }
    } } }
}

/** The server's code for "this character's level is too low for the auction". */
private const val LEVEL_GATE = "AU_002"
