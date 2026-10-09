package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.rules.trade.LotGoods

/** Строка таблички истории аукциона (4.3.0): что пришло или ушло - стопка, золото или товар лота. */
sealed interface DealEntry {
    /** Стопка сумки или сфера [code]: одинаковые суммируются в [amount]. */
    data class Stack(val code: String, val amount: Long) : DealEntry

    /** Золото сборов покупателя: сгорает, суммируется в одну строку. */
    data class Gold(val amount: Long) : DealEntry

    /** Вещь или питомец сделки [lot] - каждый своей строкой, карточка открывается из неё. */
    data class Goods(val lot: AuctionLot) : DealEntry
}

/**
 * Итог сделок героя (4.3.0) двумя табличками. [received] - сферы за свои проданные лоты и купленные товары; [spent] - сферы за
 * покупки, золото сборов и свои проданные товары. Стопки и валюта одного кода суммируются, вещи и питомцы идут по одной.
 */
class DealLedger(val received: List<DealEntry>, val spent: List<DealEntry>) {
    companion object {
        /** Таблички по сделкам [deals] (как их показывает история: [AuctionLot.deal]) героя [heroId]. */
        fun of(deals: List<AuctionLot>, heroId: String): DealLedger {
            val (sold, bought) = deals.partition { it.belongsTo(heroId) }
            return DealLedger(
                received = merged(sold.map(::payment) + bought.mapNotNull(::goods)),
                spent = merged(bought.map(::payment) + bought.map { DealEntry.Gold(it.fee) } + sold.mapNotNull(::goods)),
            )
        }

        private fun payment(lot: AuctionLot) = DealEntry.Stack(lot.priceOrb, lot.price)

        private fun goods(lot: AuctionLot): DealEntry? = when (val goods = lot.goods) {
            is LotGoods.Stack -> DealEntry.Stack(goods.code, goods.amount)
            is LotGoods.Equipment, is LotGoods.Beast -> DealEntry.Goods(lot)
            null -> null
        }

        /** Одинаковые стопки и золото - одной строкой с суммой, в порядке первой встречи; пустые суммы выпадают. */
        private fun merged(entries: List<DealEntry>): List<DealEntry> {
            val rows = LinkedHashMap<Any, DealEntry>()
            entries.forEach { entry ->
                when (entry) {
                    is DealEntry.Stack -> rows.merge("stack" to entry.code, entry) { a, _ -> (a as DealEntry.Stack).copy(amount = a.amount + entry.amount) }
                    is DealEntry.Gold -> rows.merge(DealEntry.Gold::class, entry) { a, _ -> DealEntry.Gold((a as DealEntry.Gold).amount + entry.amount) }
                    is DealEntry.Goods -> rows["lot" to entry.lot.id] = entry
                }
            }
            return rows.values.filter { (it as? DealEntry.Stack)?.amount != 0L && (it as? DealEntry.Gold)?.amount != 0L }
        }
    }
}
