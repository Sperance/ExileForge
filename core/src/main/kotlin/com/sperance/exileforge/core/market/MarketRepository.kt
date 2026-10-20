package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.AuctionSlots
import com.sperance.exileforge.core.model.trade.MerchantStock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Вкладки рынка по порядку: витрина лотов за цену, торги («Аукцион»), свои лоты и ставки, история сделок. */
enum class MarketTab { SHOWCASE, AUCTION, LOTS, HISTORY }

/** Рынок и лавка торговца, как сервер ответил последним: витрина и торги под одним фильтром, свои лоты и ставки, сделки, полка. */
data class Market(
    /** Вкладка рынка; после выставления - свои лоты. */
    val tab: MarketTab = MarketTab.SHOWCASE,
    val showcase: AuctionPage = AuctionPage(),
    /** Торги под тем же фильтром, что витрина. */
    val auctions: AuctionPage = AuctionPage(),
    val filter: AuctionFilter = AuctionFilter(),
    val showOwnLots: Boolean = false,
    val myLots: List<AuctionLot> = emptyList(),
    /** Сделки героя за последние дни, новые сверху. */
    val history: List<AuctionLot> = emptyList(),
    /** Почему аукцион закрыт для героя, словами сервера; null - открыт. */
    val locked: String? = null,
    val slots: AuctionSlots? = null,
    /** Витрина не загрузилась: показан отказ с «Повторить», а не пустая витрина. */
    val showcaseFailed: Boolean = false,
    /** Открытые торги, где герой ставил: лидирует или перебит. */
    val myBids: List<AuctionLot> = emptyList(),
) {
    /**
     * Свои лоты: что ещё продаются, чей срок вышел и что ещё возвращаются ([AuctionLot.returning]), и ниже - истёкшие, чей товар уже
     * вернулся ([AuctionLot.expiredBack]).
     */
    val ownLots: List<AuctionLot> get() = myLots.filter { it.onSale || it.returning || it.expiredBack }.sortedBy { it.expiredBack }
}

/** Единственный источник правды о рынке. */
class MarketRepository {
    private val mutable = MutableStateFlow(Market())
    val state: StateFlow<Market> = mutable

    fun update(transform: (Market) -> Market) = mutable.update(transform)

    fun clear() = update { Market() }
}
