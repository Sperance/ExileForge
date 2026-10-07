package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.AuctionSlots
import com.sperance.exileforge.core.model.trade.MerchantStock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Аукцион и лавка торговца, как сервер ответил последним: витрина под фильтром, свои лоты, сделки, полка. */
data class Market(
    /** Вкладка аукциона: витрина, свои лоты, история; после выставления - свои лоты. */
    val tab: Int = 0,
    val showcase: AuctionPage = AuctionPage(),
    val filter: AuctionFilter = AuctionFilter(),
    val showOwnLots: Boolean = false,
    val myLots: List<AuctionLot> = emptyList(),
    /** Сделки героя за последние дни (3.73.0), новые сверху. */
    val history: List<AuctionLot> = emptyList(),
    /** Почему аукцион закрыт для героя, словами сервера; null - открыт. */
    val locked: String? = null,
    val slots: AuctionSlots? = null,
) {
    /** Свои лоты, что ещё продаются. */
    val ownLots: List<AuctionLot> get() = myLots.filter { it.onSale }
}

/** Единственный источник правды о рынке (3.80.11). */
class MarketRepository {
    private val mutable = MutableStateFlow(Market())
    val state: StateFlow<Market> = mutable

    fun update(transform: (Market) -> Market) = mutable.update(transform)

    fun clear() = update { Market() }
}
