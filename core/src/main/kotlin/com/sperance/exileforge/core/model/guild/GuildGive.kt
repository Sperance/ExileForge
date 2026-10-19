package com.sperance.exileforge.core.model.guild

import com.sperance.exileforge.core.model.trade.GoldBill

/** Почему вклад сейчас не внести - по старшинству: предел дня исчерпан, сумма больше остатка, не хватает золота, суммы нет. */
enum class GiveBlock { SPENT, OVER_LIMIT, SHORT, EMPTY }

/**
 * Форма вклада в казну: сумма [amount], остаток суточного предела [left] (считает сервер; null - участника не знаем) и счёт
 * кошелька [bill] (null - героя не знаем). Своих сравнений с кошельком экран не пишет - всё здесь.
 */
data class GiveForm(val amount: Long, val left: Long?, val bill: GoldBill?) {
    /** «Макс.»: сколько влезает и в остаток, и в кошелёк; null или 0 - подставлять нечего. */
    val most: Long? get() = listOfNotNull(left, bill?.have).minOrNull()

    /** Сумма больше остатка на сегодня. */
    val over: Boolean get() = left != null && amount > left

    /** Что мешает внести; null - можно. */
    val block: GiveBlock?
        get() = when {
            left == 0L -> GiveBlock.SPENT
            over -> GiveBlock.OVER_LIMIT
            bill?.affordable == false -> GiveBlock.SHORT
            amount <= 0 -> GiveBlock.EMPTY
            else -> null
        }
}
