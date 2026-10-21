package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.requireItemId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionMine
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.LotTerms
import com.sperance.exileforge.core.model.command.AUCTION_PAGE_SIZE
import com.sperance.exileforge.rules.trade.LotMode

private const val AUCTION = "api/v1/auctionlot"

/** Рынок игроков: витрина лотов за цену и торгов, свои лоты и ставки, выставление, покупка, ставка, выкуп и снятие. */
class AuctionClient internal constructor(private val http: Transport) {
    /**
     * Страница витрины по курсору, суженная фильтром [filter] на сервере (каждое сравниваемое поле - снимок, что несёт лот): после
     * последнего курсора следа [cursors] ("" - с начала), след хранится для «назад». [mode] - лоты за цену (витрина) или торги
     * (вкладка «Аукцион»).
     */
    suspend fun search(heroId: String, filter: AuctionFilter, cursors: List<String>, mode: LotMode = LotMode.FIXED): AuctionPage {
        val after = cursors.lastOrNull().orEmpty()
        val page: AuctionPage = http.get("$AUCTION/search", heroQuery(heroId, "after" to after, "size" to AUCTION_PAGE_SIZE.toString(), "mode" to mode.name) + filter.query())
        return page.copy(page = cursors.size - 1, cursors = cursors.ifEmpty { listOf("") })
    }

    /** Свой аукцион героя одним запросом: лоты, места и сделки последних дней. */
    suspend fun mine(heroId: String): AuctionMine = http.get("$AUCTION/mine", heroQuery(heroId))

    /** Lists a copy; the price is in orbs, [priceOrb] the currency's item code. The copy has to be off the hero first. Торгами - [terms], цена - выкуп. */
    suspend fun sellEquipment(heroId: String, itemId: String, priceOrb: String, price: Long, terms: LotTerms = LotTerms()): AuctionMine {
        requireItemId(itemId)
        return sell("equipment", heroId, priceOrb, price, mapOf("itemId" to itemId) + terms.query())
    }

    suspend fun sellItem(heroId: String, code: String, amount: Long, priceOrb: String, price: Long, terms: LotTerms = LotTerms()): AuctionMine {
        require(code.isNotBlank()) { ui("api.choose_item") }
        require(amount > 0) { ui("api.amount_positive") }
        return sell("item", heroId, priceOrb, price, mapOf("code" to code, "amount" to amount.toString()) + terms.query())
    }

    /** Выставление отвечает своим аукционом с новым лотом и снимком героя. */
    private suspend fun sell(what: String, heroId: String, priceOrb: String, price: Long, extra: Map<String, String>): AuctionMine {
        require(priceOrb.isNotBlank()) { ui("api.choose_orb") }
        require(price > 0) { ui("api.price_positive") }
        return http.post("$AUCTION/sell/$what", extra + heroQuery(heroId, "priceOrb" to priceOrb, "price" to price.toString()))
    }

    /** Покупка - одна транзакция сервера (оплата, товар, закрытие лота); отвечает своим рынком с лотом. Повтор - только под своим ключом. */
    suspend fun buy(heroId: String, lotId: String): AuctionMine = lot("buy", heroId, lotId)

    /** Снятие отвечает своим рынком; у торгов со ставкой сервер возвращает залог лидеру письмом. */
    suspend fun cancel(heroId: String, lotId: String): AuctionMine = lot("cancel", heroId, lotId)

    /** Ставка [amount] сфер в торгах: ответ - свой рынок с лотом глазами героя; ставка от выкупа - выкуп. Повтор - только под своим ключом. */
    suspend fun bid(heroId: String, lotId: String, amount: Long): AuctionMine {
        requireId(lotId)
        return http.post("$AUCTION/bid", heroQuery(heroId, "lotId" to lotId, "amount" to amount.toString()))
    }

    /** Выкуп торгов за цену выкупа в любой момент: отвечает своим рынком с лотом. Повтор - только под своим ключом. */
    suspend fun buyout(heroId: String, lotId: String): AuctionMine = lot("buyout", heroId, lotId)

    /** Продажа своих торгов лучшей ставке досрочно: отвечает своим рынком. */
    suspend fun accept(heroId: String, lotId: String): AuctionMine = lot("accept", heroId, lotId)

    /** Another week on the showcase: only the author, only on the lot's last day. Repeated only under its key. */
    suspend fun extend(heroId: String, lotId: String): AuctionMine = lot("extend", heroId, lotId)

    /** What the like of a lot sold for lately: null with fewer deals than the rules ask. */
    suspend fun priceHint(heroId: String, itemCode: String, rarity: com.sperance.exileforge.rules.content.Rarity?, itemLevel: Int): com.sperance.exileforge.core.model.auction.PriceHint? = http.get("$AUCTION/price", heroQuery(heroId, "itemCode" to itemCode, "rarity" to rarity?.name, "itemLevel" to itemLevel.toString()))

    private suspend inline fun <reified T> lot(operation: String, heroId: String, lotId: String): T {
        requireId(lotId)
        return http.post("$AUCTION/$operation", heroQuery(heroId, "lotId" to lotId))
    }
}
