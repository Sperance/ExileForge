package com.sperance.exileforge.core.model.auction

import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.rules.content.AuctionRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.trade.AuctionFees
import com.sperance.exileforge.rules.trade.BidStanding
import com.sperance.exileforge.rules.trade.Bidding
import com.sperance.exileforge.rules.trade.LotMode

/** Лот - торги со ставками. */
val AuctionLot.isBidding: Boolean get() = mode == LotMode.BID

/** Текущая ставка торгов в сферах; 0 - ставок нет. */
val AuctionLot.currentBid: Long get() = bidding?.bid ?: 0

/** Сколько ставок сделано в торгах. */
val AuctionLot.bidCount: Int get() = bidding?.bids ?: 0

/** Ставка героя в этих торгах лучшая. */
val AuctionLot.leading: Boolean get() = standing == BidStanding.LEADING

/** Ставку героя в этих торгах перебили. */
val AuctionLot.outbid: Boolean get() = standing == BidStanding.OUTBID

/** Продавец может продать торги лучшей ставке досрочно: торги открыты и ставка есть. */
val AuctionLot.acceptable: Boolean get() = isBidding && onSale && currentBid > 0

/** Наименьшая ставка по правилам торгов [rules] ([Bidding.minBid]); не выше выкупа. */
fun AuctionLot.minBid(rules: AuctionRules): Long = bidding?.let { Bidding.minBid(rules, it, price) } ?: price

/** Ставка на шаг выше [amount] - шаг правил, не выше выкупа. */
fun AuctionLot.bidUp(rules: AuctionRules, amount: Long): Long = (amount + Bidding.step(rules, amount)).coerceAtMost(price)

/** Ставка на шаг ниже [amount], но не ниже наименьшей. */
fun AuctionLot.bidDown(rules: AuctionRules, amount: Long): Long = (amount - Bidding.step(rules, amount)).coerceAtLeast(minBid(rules))

/** Ставка [amount] - выкуп: не ниже цены выкупа. */
fun AuctionLot.buysOut(amount: Long): Boolean = amount >= price

/** Сбор золотом за ставку [amount] - та же точка [AuctionFees], что спишет сервер в залог. */
fun AuctionLot.bidFee(index: ContentIndex, amount: Long): Long = goods?.let { AuctionFees.buyer(index, it, priceOrb, amount) } ?: 0

/** Что спишет ставка [amount]: сферы и сбор в залог; ставка от выкупа - цена выкупа и её сбор ([AuctionLot.cost]). */
fun AuctionLot.bidCost(index: ContentIndex, amount: Long): Cost = if (buysOut(amount)) cost else Cost.item(priceOrb, amount) + Cost.gold(bidFee(index, amount))

/** Торги кончаются скоро: осталось не больше хвоста ставки (`auction.bidTailHours`) - полоса времени красная. */
fun AuctionLot.bidEnding(rules: AuctionRules, now: Long = System.currentTimeMillis()): Boolean = timeLeft(now)?.let { it <= rules.bidTailMillis } == true

/** Доля срока торгов, что ещё осталась, от полного срока лота (`auction.lotDays`): 0..1 - длина полосы времени. */
fun AuctionLot.timeShare(rules: AuctionRules, now: Long = System.currentTimeMillis()): Float = ((timeLeft(now) ?: 0L).toFloat() / rules.lotMillis).coerceIn(0f, 1f)

/**
 * Условия выставления: за цену или торгами ([mode]) с первой ставкой [minPrice]; цена листа - выкуп. Плата за место у обоих - по цене
 * ([AuctionFees.listing]).
 */
data class LotTerms(val mode: LotMode = LotMode.FIXED, val minPrice: Long = 0) {
    /** Параметры запроса выставления: лот за цену - без них. */
    fun query(): Map<String, String> = if (mode == LotMode.FIXED) emptyMap() else mapOf("mode" to mode.name, "minPrice" to minPrice.toString())

    /** Условия можно отправить при выкупе [price]: у торгов первая ставка - от 1 до выкупа. */
    fun valid(price: Long): Boolean = mode == LotMode.FIXED || minPrice in 1..price
}
