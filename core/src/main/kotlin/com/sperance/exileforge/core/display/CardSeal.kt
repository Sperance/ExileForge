package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.rules.content.MapInfluenceRule

/**
 * Захват карты влиянием [influence] действующими числами правила с узлами Атласа героя: [power] - сила монстров (проценты
 * здоровья и урона), [items] - шанс волшебной и редкой вещи нести влияние (проценты), [cracks] - расщелин Бездны сверх окна.
 */
data class MapCapture(val influence: Influence, val power: Double, val items: Double, val cracks: Int) {
    companion object {
        /**
         * Захват карты влиянием [influence] по правилу [rule] с прибавками Атласа [atlas] ([MapInfluenceRule.powerAt],
         * [MapInfluenceRule.itemsAt], [MapInfluenceRule.cracksOf]); карта без влияния или влияние, что карту не захватывает, - null.
         */
        fun of(rule: MapInfluenceRule, influence: Influence?, atlas: (String) -> Double): MapCapture? = influence?.takeIf(rule::accepts)?.let { MapCapture(it, rule.powerAt(atlas), rule.itemsAt(atlas), rule.cracksOf(it)) }
    }
}

/** Плашка особого блока карточки, что не строка вещи: она встаёт первой, над строками особых источников. */
sealed interface CardSeal {
    /** Карта захвачена влиянием: имя влияния и его числа. */
    data class Capture(val capture: MapCapture) : CardSeal

    /** Качество карты [quality] процентами и что оно даёт: [quantity] процентов к количеству добычи с Атласом героя. */
    data class MapQuality(val quality: Int, val quantity: Double) : CardSeal
}
