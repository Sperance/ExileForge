package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.pick.PickPreset
import com.sperance.exileforge.core.pick.Pickable
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.rules.content.AutoSell
import com.sperance.exileforge.rules.content.BulkSale
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.sheet.Requirements

/**
 * Вещь, которую можно отметить к продаже пачкой: копия, цена торговца и подходит ли она под правила автопродажи
 * героя. Лотом становится всё, что правила отмечают к продаже ([BulkSale.pickable]: не надето, не вставлено, не заперто) - то же
 * правило проверяет сервер; ценное ([valuable]) продаётся только после подтверждения со списком.
 */
data class SellLot(val piece: ItemView, val price: Long, val byRules: Boolean) : Pickable {
    override val id: String get() = piece.id

    /** Ценная вещь ([BulkSale.valuable]): уникальная, мифическая, с влиянием, осквернённая или расколотая. */
    val valuable: Boolean get() = BulkSale.valuable(piece.item)
}

/** Быстрый набор выбора: «по правилам автопродажи» или вся одна продаваемая редкость. */
sealed interface SellPreset : PickPreset<SellLot> {
    data object Rules : SellPreset {
        override fun takes(lot: SellLot) = lot.byRules
    }

    data class OfRarity(val rarity: Rarity) : SellPreset {
        override fun takes(lot: SellLot) = lot.piece.rarity == rarity
    }

    companion object {
        val ALL: List<SellPreset> = listOf(Rules) + AutoSell.SELLABLE.sortedBy { it.ordinal }.map(::OfRarity)
    }
}

/** Лоты продажи среди [pieces]: что правила отмечают к продаже, с известной ценой; «по правилам» - как сервер читает фильтр добычи. */
fun GameUi.sellLots(pieces: List<ItemView>): List<SellLot> {
    val hero = hero ?: return emptyList()
    val index = index ?: return emptyList()
    val rules = hero.info.autoSell
    return pieces.mapNotNull { piece ->
        val template = index.template(piece.item.template)
        val price = sellPrice(piece.item)
        if (!BulkSale.pickable(piece.item) || template == null || price == null) return@mapNotNull null
        SellLot(piece, price, rules.sells(template, piece.item, hero.level) { Requirements.barred(template, hero.heroClass, hero.stats) })
    }
}
