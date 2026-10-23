package com.sperance.exileforge.core.model.auction

import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.rules.content.AuctionRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.trade.AuctionFees
import com.sperance.exileforge.rules.trade.BidLine
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

/**
 * Торги выставил другой герой аккаунта [userId] (`sellerUser` сервера): ставить и выкупать их нельзя (сервер - `AU_021`); свой лот -
 * [AuctionLot.belongsTo].
 */
fun AuctionLot.sameAccount(userId: String?): Boolean = isBidding && !userId.isNullOrEmpty() && sellerUser == userId

/** Продавец может продать торги лучшей ставке досрочно к «сейчас» [now] (часы сервера): торги открыты и ставка есть. */
fun AuctionLot.acceptableAt(now: Long): Boolean = isBidding && onSaleAt(now) && currentBid > 0

/** Наименьшая ставка по правилам торгов [rules] ([Bidding.minBid]); не выше выкупа. */
fun AuctionLot.minBid(rules: AuctionRules): Long = bidding?.let { Bidding.minBid(rules, it, price) } ?: price

/** Ставка на шаг выше [amount] - шаг правил, не выше выкупа. */
fun AuctionLot.bidUp(rules: AuctionRules, amount: Long): Long = (amount + Bidding.step(rules, amount)).coerceAtMost(price)

/** Ставка на шаг ниже [amount], но не ниже наименьшей. */
fun AuctionLot.bidDown(rules: AuctionRules, amount: Long): Long = (amount - Bidding.step(rules, amount)).coerceAtLeast(minBid(rules))

/**
 * Выкуп торгов, если ставку [amount] по правилам [rules] примут, - та же точка [Bidding.buyoutAfterBid], что поднимет его на сервере:
 * ставка, поднятая отношением выкупа при выставлении к первой ставке, но не меньше нынешнего выкупа с шагом; лот за цену - нынешняя цена.
 */
fun AuctionLot.buyoutAfterBid(rules: AuctionRules, amount: Long): Long = bidding?.let { Bidding.buyoutAfterBid(rules, it, price, amount) } ?: price

/** Ставка [amount] - выкуп: не ниже цены выкупа. */
fun AuctionLot.buysOut(amount: Long): Boolean = amount >= price

/** Сбор золотом за ставку [amount] со скидкой героя [cut] (древо гильдии) - та же точка [AuctionFees], что спишет сервер в залог. */
fun AuctionLot.bidFee(index: ContentIndex, amount: Long, cut: Double): Long = goods?.let { AuctionFees.cut(AuctionFees.buyer(index, it, priceOrb, amount), cut) } ?: 0

/** Что спишет ставка [amount]: сферы и сбор в залог; ставка от выкупа - цена выкупа и её сбор ([AuctionLot.cost]). */
fun AuctionLot.bidCost(index: ContentIndex, amount: Long, cut: Double): Cost = if (buysOut(amount)) cost else Cost.item(priceOrb, amount) + Cost.gold(bidFee(index, amount, cut))

/** Торги кончаются скоро к «сейчас» [now] (часы сервера): осталось не больше хвоста ставки (`auction.bidTailHours`) - полоса времени красная. */
fun AuctionLot.bidEnding(rules: AuctionRules, now: Long): Boolean = timeLeft(now)?.let { it <= rules.bidTailMillis } == true

/** Доля срока торгов, что ещё осталась к «сейчас» [now], от полного срока лота (`auction.lotDays`): 0..1 - длина полосы времени. */
fun AuctionLot.timeShare(rules: AuctionRules, now: Long): Float = ((timeLeft(now) ?: 0L).toFloat() / rules.lotMillis).coerceIn(0f, 1f)

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

/** Строка ленты ставок для показа: ставка [line] без имени (своя - [BidLine.own]) и [leading] - она лидирует сейчас. */
data class FeedLine(val line: BidLine, val leading: Boolean)

/**
 * Лента ставок торгов, новые первыми (`BidState.lines` сервера). Лидирует только верхняя - и только пока в лоте лежит залог лидера с её
 * суммой: у проданных, снятых торгов и торгов, чей лидер ушёл из мира, лидера в ленте нет.
 */
val AuctionLot.feedLines: List<FeedLine> get() {
    val lines = bidding?.lines.orEmpty().asReversed()
    val leader = bidding?.stake?.takeIf { status == LotStatus.ACTIVE }?.orbs
    return lines.mapIndexed { at, line -> FeedLine(line, at == 0 && line.orbs == leader) }
}

/** Последняя своя ставка в ленте - перебитому она и её сбор вернулись письмом; null - своих ставок в ленте нет. */
val AuctionLot.lastOwnBid: Long? get() = bidding?.lines?.lastOrNull { it.own }?.orbs

/** Как ставка сейчас двигает конец торгов ([Bidding.endsAfterBid]). */
enum class BidTail {
    /** Торги кончатся через хвост ставки (`auction.bidTailHours`) - не раньше нынешнего конца. */
    EXTENDS,

    /** Через хвост ставки - раньше нынешнего конца: первая ставка сокращает долгий срок. */
    SHORTENS,

    /** Упирается в крайний срок лота: дальше ставки его не продлят. */
    CAPPED,
}

/** Когда кончатся торги, если поставить к «сейчас» [now] (часы сервера) ([Bidding.endsAfterBid]); у лота за цену - его срок. */
fun AuctionLot.endsAfterBid(rules: AuctionRules, now: Long): Long = bidding?.let { Bidding.endsAfterBid(rules, it, now) } ?: expiresAt

/** Что ставка к «сейчас» [now] сделает со сроком торгов ([BidTail]). */
fun AuctionLot.bidTail(rules: AuctionRules, now: Long): BidTail {
    val ends = endsAfterBid(rules, now)
    return when {
        bidding != null && ends >= bidding.until -> BidTail.CAPPED
        expiresAt > 0 && ends < expiresAt -> BidTail.SHORTENS
        else -> BidTail.EXTENDS
    }
}

/** Место суммы [amount] на шкале цены торгов от первой ставки до выкупа: 0..1. */
fun AuctionLot.priceShare(amount: Long): Float {
    val low = bidding?.minPrice ?: 0L
    val span = (price - low).coerceAtLeast(1L)
    return ((amount - low).toFloat() / span).coerceIn(0f, 1f)
}

/** Медиана похожих сделок [hint] в сфере цены лота за весь товар (стопка - на её число); подсказка в другой сфере - null. */
fun AuctionLot.median(hint: PriceHint?): Long? = hint?.takeIf { it.priceOrb == priceOrb && it.price > 0 }?.let { it.price * amount }
