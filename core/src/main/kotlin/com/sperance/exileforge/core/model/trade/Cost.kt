package com.sperance.exileforge.core.model.trade

/**
 * Цена действия (3.89.0): предметы сумки по кодам и золото. Одна модель на аукцион, торговца, ремёсла и верстак -
 * экран складывает цену и спрашивает [shortfall], а строку недостачи пишет один форматтер.
 */
data class Cost(val items: Map<String, Long> = emptyMap(), val gold: Long = 0) {
    operator fun plus(other: Cost): Cost = Cost(
        (items.keys + other.items.keys).associateWith { (items[it] ?: 0L) + (other.items[it] ?: 0L) },
        gold + other.gold,
    )

    /** Чего не хватает при сумке [bag] и кошельке [money]; порядок предметов - порядок цены. */
    fun shortfall(bag: (String) -> Long, money: Long): Shortfall = Shortfall(
        items.mapValues { (code, need) -> need - bag(code) }.filterValues { it > 0 },
        (gold - money).coerceAtLeast(0),
    )

    companion object {
        val FREE = Cost()

        fun item(code: String, amount: Long): Cost = Cost(mapOf(code to amount))

        fun gold(amount: Long): Cost = Cost(gold = amount)
    }
}

/** Недостача (3.89.0): сколько каждого предмета ([items], код - число) и золота не хватает; пустая - хватает всего. */
data class Shortfall(val items: Map<String, Long> = emptyMap(), val gold: Long = 0) {
    val isEmpty: Boolean get() = items.isEmpty() && gold <= 0
}
