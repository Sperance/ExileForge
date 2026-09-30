package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.requireItemId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.AuctionSlots
import com.sperance.exileforge.core.model.command.AUCTION_PAGE_SIZE

private const val AUCTION = "api/v1/auctionlot"

/** The player auction: the showcase, a hero's own lots, listing, buying and withdrawing. */
class AuctionClient internal constructor(private val http: Transport) {
    /** The showcase, narrowed and paged by the server: every field the filter compares is a snapshot the lot carries. */
    /** The showcase page after the lot [after] ("" — from the start), by cursor: [cursors] are the trail kept for «back». */
    suspend fun search(heroId: String, filter: AuctionFilter, cursors: List<String>): AuctionPage {
        val after = cursors.lastOrNull().orEmpty()
        val page: AuctionPage = http.get("$AUCTION/search", heroQuery(heroId, "after" to after, "size" to AUCTION_PAGE_SIZE.toString()) + filter.query())
        return page.copy(page = cursors.size - 1, cursors = cursors.ifEmpty { listOf("") })
    }

    suspend fun slots(heroId: String): AuctionSlots = http.get("$AUCTION/slots", heroQuery(heroId))
    /** Buys one more lot place for gold. Never retried. */
    suspend fun myLots(heroId: String): List<AuctionLot> = http.get("$AUCTION/my", heroQuery(heroId))

    /** Lists a copy; the price is in orbs, [priceOrb] the currency's item code. The copy has to be off the hero first. */
    suspend fun sellEquipment(heroId: String, itemId: String, priceOrb: String, price: Long): AuctionLot {
        requireItemId(itemId)
        return sell("equipment", heroId, priceOrb, price, mapOf("itemId" to itemId))
    }

    suspend fun sellItem(heroId: String, code: String, amount: Long, priceOrb: String, price: Long): AuctionLot {
        require(code.isNotBlank()) { ui("api.choose_item") }
        require(amount > 0) { ui("api.amount_positive") }
        return sell("item", heroId, priceOrb, price, mapOf("code" to code, "amount" to amount.toString()))
    }

    private suspend fun sell(what: String, heroId: String, priceOrb: String, price: Long, extra: Map<String, String>): AuctionLot {
        require(priceOrb.isNotBlank()) { ui("api.choose_orb") }
        require(price > 0) { ui("api.price_positive") }
        return http.post("$AUCTION/sell/$what", extra + heroQuery(heroId, "priceOrb" to priceOrb, "price" to price.toString()))
    }

    /** Payment, delivery and closing the lot are one server transaction. Never retried. */
    suspend fun buy(heroId: String, lotId: String): AuctionLot = lot("buy", heroId, lotId)
    suspend fun cancel(heroId: String, lotId: String): AuctionLot = lot("cancel", heroId, lotId)

    private suspend fun lot(operation: String, heroId: String, lotId: String): AuctionLot {
        requireId(lotId)
        return http.post("$AUCTION/$operation", heroQuery(heroId, "lotId" to lotId))
    }
}
