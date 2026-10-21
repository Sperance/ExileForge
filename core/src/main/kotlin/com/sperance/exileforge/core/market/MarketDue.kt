package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.acceptableAt
import com.sperance.exileforge.core.model.auction.isBidding
import com.sperance.exileforge.core.model.auction.outbid
import com.sperance.exileforge.rules.content.AuctionRules

/** Дело рынка, что ждёт героя, - все на вкладке «Мои лоты»; порядок - порядок строк плитки Города. */
enum class MarketDue {
    /** Свой лот за цену в окне продления, продления не кончились. */
    EXTEND,

    /** Ставку героя в открытых торгах перебили. */
    OUTBID,

    /** Свои открытые торги со ставкой: их можно продать ставке сейчас. */
    SELL,
}

/** Дело лота для героя [heroId] к «сейчас» [now] часов сервера по правилам [rules]; null - лот ничего не ждёт. */
fun AuctionLot.due(heroId: String, rules: AuctionRules, now: Long): MarketDue? = when {
    belongsTo(heroId) && extendable(rules, now) -> MarketDue.EXTEND
    belongsTo(heroId) && acceptableAt(now) -> MarketDue.SELL
    outbid && onSaleAt(now) -> MarketDue.OUTBID
    else -> null
}

/**
 * Дела рынка героя - одна модель для счётчика и строк плитки Города, точки вкладки «Мои лоты», заголовков её разделов и строк лотов:
 * [lots] - дело каждого лота, что его ждёт, в порядке строк вкладки (свои торги, свои ставки, лоты за цену).
 */
data class MarketChores(val lots: Map<String, MarketDue> = emptyMap()) {
    /** Сколько дел всего - счётчик плитки. */
    val total: Int get() = lots.size

    /** Первая строка вкладки, что ждёт действия: её подсвечивает вход с плитки; null - дел нет. */
    val first: String? get() = lots.keys.firstOrNull()

    /** Дело лота [lotId]; null - не ждёт. */
    operator fun get(lotId: String): MarketDue? = lots[lotId]

    /** Сколько дел вида [due]. */
    fun count(due: MarketDue): Int = lots.values.count { it == due }

    /** Сколько дел среди лотов [among] - счёт раздела вкладки. */
    fun count(among: List<AuctionLot>): Int = among.count { it.id in lots }

    companion object {
        /**
         * Дела рынка [market] героя [heroId] к «сейчас» [now] часов сервера; правил ещё нет - дел нет. Порядок - как строки «Моих лотов»
         * ([Market.ownAuctions], [Market.myBids], [Market.ownFixed]).
         */
        fun of(market: Market, heroId: String, rules: AuctionRules?, now: Long): MarketChores {
            if (rules == null || heroId.isEmpty()) return MarketChores()
            val lots = (market.ownAuctions + market.myBids + market.ownFixed).mapNotNull { lot -> lot.due(heroId, rules, now)?.let { lot.id to it } }
            return MarketChores(lots.toMap())
        }
    }
}

/** Свои торги на витрине - верхний раздел «Моих лотов». */
val Market.ownAuctions: List<AuctionLot> get() = ownLots.filter { it.isBidding && it.onSale }

/** Свои лоты за цену и вернувшиеся - нижний раздел «Моих лотов». */
val Market.ownFixed: List<AuctionLot> get() = ownLots.filterNot { it.isBidding && it.onSale }
