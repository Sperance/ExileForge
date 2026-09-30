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
 * The player auction. Every rule belongs to the server; the client names a lot and prints the refusal.
 * A trade re-reads the hero — one request of what moved — and the lists it touched.
 */
class AuctionViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun auctionTab(tab: Int) = update { it.copy(market = it.market.copy(tab = tab)) }
    fun auctionFilter(filter: AuctionFilter) = update { it.copy(market = it.market.copy(filter = filter)) }
    fun showOwnLots(show: Boolean) = update { it.copy(market = it.market.copy(showOwnLots = show)) }

    fun loadShowcase(page: Int = 0) { with(runtime) { trade(restart = true) {
        val id = heroId
        val filter = state.value.market.filter.copy(excludeSellerId = if (state.value.market.showOwnLots) "" else id, lang = state.value.lang.code)
        val showcase = api.auction.search(id, filter, state.value.market.showcase.trailTo(page))
        mutable.update { it.copy(market = it.market.copy(showcase = showcase)) }
    } } }

    /** [glance] is the City square's (3.22.0): no strip, and a hero below the auction's level is told on its card rather than by a refusal. */
    fun loadMyLots(glance: Boolean = false) { with(runtime) { trade(key = Reads.LOTS, glance = glance) {
        val id = heroId
        val lots = api.auction.myLots(id)
        val slots = api.auction.slots(id)
        mutable.update { it.copy(market = it.market.copy(myLots = lots, slots = slots)) }
    } } }

    /** The merchant's shelf: it comes with the hero snapshot, and is read afresh here when the building opens. */
    fun loadMerchant() { with(runtime) { trade(key = Reads.MERCHANT) {
        ensureContent()
        val stock = api.merchant.stock(heroId)
        mutable.update { it.copy(market = it.market.copy(merchant = stock)) }
    } } }

    /** The auction's own two lists; the merchant is a building of its own since 3.22.0 and reads its shelf itself. */
    fun loadAuction() { loadShowcase(0); loadMyLots() }

    fun buyOffer(offerId: String) { with(runtime) { trade(writing = true) {
        val id = heroId
        val purchase = api.merchant.buy(id, offerId)
        mutable.update { it.copy(market = it.market.copy(merchant = it.market.merchant?.let { stock -> stock.copy(offers = stock.offers.filter { offer -> offer.id != offerId }) })) }
        gold(purchase.money)
        refreshHero(id)
    } } }

    fun buyOrb(code: String) { with(runtime) { trade(writing = true) {
        val id = heroId
        val purchase = api.merchant.buyOrb(id, code)
        mutable.update { it.copy(market = it.market.copy(merchant = it.market.merchant?.let { stock ->
            stock.copy(orbs = stock.orbs.map { orb -> if (orb.code == code) orb.copy(price = purchase.next, bought = orb.bought + 1, left = orb.left?.let { (it - 1).coerceAtLeast(0) }) else orb })
        })) }
        gold(purchase.money)
        refreshHero(id)
    } } }

    private fun gold(money: Long) { with(runtime) {
        mutable.update { s -> s.copy(play = s.play.copy(hero = s.play.hero?.let { it.copy(info = it.info.copy(money = money)) })) }
    } }

    fun buy(lotId: String) { with(runtime) { trade(writing = true) {
        val id = heroId
        val lot = api.auction.buy(id, lotId)
        toast(ui("toast.bought", lot.title))
        afterTrade {
            refreshHero(id)
            val filter = state.value.market.filter.copy(excludeSellerId = if (state.value.market.showOwnLots) "" else id, lang = state.value.lang.code)
            val showcase = api.auction.search(id, filter, state.value.market.showcase.cursors)
            mutable.update { it.copy(market = it.market.copy(showcase = showcase)) }
        }
    } } }

    /** Lists a copy for a price in orbs — [priceOrb] the currency's item code. */
    fun sellEquipment(itemId: String, priceOrb: String, price: Long) { with(runtime) { trade(writing = true) {
        val id = heroId
        toast(ui("toast.listed", api.auction.sellEquipment(id, itemId, priceOrb, price).title))
        listed(id)
    } } }

    /** Lists a stack of the bag by its code. */
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long) { with(runtime) { trade(writing = true) {
        val id = heroId
        toast(ui("toast.listed", api.auction.sellItem(id, code, amount, priceOrb, price).title))
        listed(id)
    } } }

    fun cancel(lotId: String) { with(runtime) { trade(writing = true) {
        val id = heroId
        toast(ui("toast.withdrawn", api.auction.cancel(id, lotId).title))
        afterTrade {
            refreshHero(id)
            val lots = api.auction.myLots(id)
            val slots = api.auction.slots(id)
            mutable.update { it.copy(market = it.market.copy(myLots = lots, slots = slots)) }
        }
    } } }

    private suspend fun listed(heroId: String) { with(runtime) {
        afterTrade {
            refreshHero(heroId)
            val lots = api.auction.myLots(heroId)
            val slots = api.auction.slots(heroId)
            mutable.update { it.copy(market = it.market.copy(myLots = lots, slots = slots, tab = 1)) }
        }
    } }

    /** The reads after a trade the server already made: their failure is not the trade's. */
    private suspend fun afterTrade(block: suspend () -> Unit) { with(runtime) {
        try { block() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = ui("auction.done_refresh"), error = true) } }
    } }

    private suspend fun refreshHero(heroId: String) { if (onScreen(heroId)) runtime.heroViewModel.readHero() }

    private fun trade(writing: Boolean = false, restart: Boolean = false, key: String = Reads.AUCTION, glance: Boolean = false, block: suspend () -> Unit) { with(runtime) {
        if (writing) task(writing = true, touches = setOf(Reads.AUCTION, Reads.LOTS, Reads.HERO, Reads.MERCHANT)) { gated(block) }
        else read(key, restart, silent = glance) { gated(block, glance) }
    } }

    private suspend fun gated(block: suspend () -> Unit, glance: Boolean = false) { with(runtime) {
        check(state.value.play.heroId.isNotBlank()) { ui("auction.choose_character") }
        try {
            block()
            mutable.update { it.copy(market = it.market.copy(locked = null)) }
        } catch (e: CancellationException) { throw e }
        catch (e: ApiFailure) {
            if (e.code != LEVEL_GATE) throw e
            mutable.update { it.copy(market = it.market.copy(locked = locError(e.code, e.message.orEmpty()), showcase = AuctionPage(), myLots = emptyList())) }
            if (!glance) throw e
        }
    } }
}

/** The server's code for "this hero's level is too low for the auction". */
private const val LEVEL_GATE = "AU_002"
