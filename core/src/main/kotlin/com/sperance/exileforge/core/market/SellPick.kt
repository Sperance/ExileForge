package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
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
data class SellLot(val piece: ItemView, val price: Long, val byRules: Boolean) {
    val id: String get() = piece.id

    /** Ценная вещь ([BulkSale.valuable]): уникальная, мифическая, с влиянием, осквернённая или расколотая. */
    val valuable: Boolean get() = BulkSale.valuable(piece.item)
}

/** Быстрый набор выбора: «по правилам автопродажи» или вся одна продаваемая редкость. */
sealed interface SellPreset {
    fun takes(lot: SellLot): Boolean

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

/**
 * Выбор продажи, общий для режима продажи тайника и окна конца захода: отмеченные id поверх текущих лотов.
 * Проданное или ушедшее из лотов само выпадает из выбора - счёт и сумма читаются только по живым лотам.
 */
data class SellSelection(val chosen: Set<String> = emptySet()) {
    fun picked(lots: List<SellLot>): List<SellLot> = lots.filter { it.id in chosen }

    fun toggle(id: String) = copy(chosen = if (id in chosen) chosen - id else chosen + id)

    /**
     * Отмечает лоты фильтра автопродажи ([SellLot.byRules]), которых нет в [seen], - отложенная продажа добычи захода. Уже виденный лот
     * не отмечается снова: снятая отметка не возвращается с новыми лотами.
     */
    fun byRules(lots: List<SellLot>, seen: Set<String>) = copy(chosen = chosen + lots.filter { it.byRules && it.id !in seen }.map { it.id })

    /** Набор включён, когда в нём есть лоты и все они отмечены. */
    fun covers(lots: List<SellLot>, preset: SellPreset): Boolean = lots.filter(preset::takes).let { set -> set.isNotEmpty() && set.all { it.id in chosen } }

    /** Включённый набор снимается целиком, иначе добавляется к выбору. */
    fun flip(lots: List<SellLot>, preset: SellPreset): SellSelection {
        val ids = lots.filter(preset::takes).mapTo(HashSet()) { it.id }
        return copy(chosen = if (covers(lots, preset)) chosen - ids else chosen + ids)
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
