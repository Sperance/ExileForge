package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.acceptableAt
import com.sperance.exileforge.core.model.auction.isBidding
import com.sperance.exileforge.core.model.auction.outbid
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.rules.content.AuctionRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

/** Раздел «Моих лотов» по порядку: свои торги на витрине, свои ставки в чужих торгах, лоты за цену и вернувшиеся. */
enum class MyLotKind { AUCTIONS, BIDS, FIXED }

/** Раздел «Моих лотов» [kind] и его лоты [lots] по порядку строк. */
data class MyLotSection(val kind: MyLotKind, val lots: List<AuctionLot>)

/**
 * Непустые разделы «Моих лотов» по порядку - одно место для строк вкладки и дел рынка: свои торги, что ещё продаются, свои ставки, ниже -
 * лоты за цену, истёкшие и возвращающиеся ([Market.ownLots]).
 */
val Market.myLotSections: List<MyLotSection> get() {
    val (auctions, fixed) = ownLots.partition { it.isBidding && it.onSale }
    return listOf(MyLotSection(MyLotKind.AUCTIONS, auctions), MyLotSection(MyLotKind.BIDS, myBids), MyLotSection(MyLotKind.FIXED, fixed)).filter { it.lots.isNotEmpty() }
}

/**
 * Дела рынка героя - одна модель для счётчика и строк плитки Города, точки вкладки «Мои лоты», заголовков её разделов и строк лотов:
 * [lots] - дело каждого лота, что его ждёт, в порядке строк вкладки ([myLotSections]).
 */
data class MarketChores(val lots: Map<String, MarketDue> = emptyMap()) {
    /** Сколько дел всего - счётчик плитки. */
    val total: Int get() = lots.size

    /** Первая строка вкладки, что ждёт действия: к ней ведёт вход с плитки; null - дел нет. */
    val first: String? get() = lots.keys.firstOrNull()

    /** Дело лота [lotId]; null - не ждёт. */
    operator fun get(lotId: String): MarketDue? = lots[lotId]

    /** Сколько дел вида [due]. */
    fun count(due: MarketDue): Int = lots.values.count { it == due }

    /** Сколько дел среди лотов [among] - счёт раздела вкладки. */
    fun count(among: List<AuctionLot>): Int = among.count { it.id in lots }

    companion object {
        /** Дела рынка [market] героя [heroId] к «сейчас» [now] часов сервера; правил ещё нет - дел нет. */
        fun of(market: Market, heroId: String, rules: AuctionRules?, now: Long): MarketChores {
            if (rules == null || heroId.isEmpty()) return MarketChores()
            val lots = market.myLotSections.flatMap { it.lots }.mapNotNull { lot -> lot.due(heroId, rules, now)?.let { lot.id to it } }
            return MarketChores(lots.toMap())
        }
    }
}

/**
 * Дела рынка одним потоком на приложение: плитка Города, вкладки рынка и строки «Моих лотов» подписываются на него, а не считают сами.
 * Пересчёт - когда меняются рынок, герой или правила рынка и раз в [TICK_MS] по часам сервера ([ServerClock]): окно продления и сроки
 * торгов наступают сами; часы идут, только пока есть подписчики.
 */
class MarketChoresModel(repository: MarketRepository, slice: GameSlice, scope: CoroutineScope) {
    private val ticks = flow {
        while (true) {
            emit(ServerClock.now())
            delay(TICK_MS)
        }
    }

    val chores: StateFlow<MarketChores> = combine(repository.state, slice.ui.map { Inputs(it.heroId, it.index?.rules?.auction) }.distinctUntilChanged(), ticks) { market, inputs, now ->
        MarketChores.of(market, inputs.heroId, inputs.rules, now)
    }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(STOP_MS), MarketChores())

    /** Что из среза игры нужно делам: герой и правила рынка. */
    private data class Inputs(val heroId: String, val rules: AuctionRules?)

    private companion object {
        /** Шаг часов дел: окно продления и сроки торгов - с точностью до полуминуты. */
        const val TICK_MS = 30_000L

        /** Сколько поток живёт без подписчиков: смена экрана его не перезапускает. */
        const val STOP_MS = 5_000L
    }
}
