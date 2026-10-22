package com.sperance.exileforge.core.model.hero

import com.sperance.exileforge.core.model.fate.FateView
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Что нужно действию стопки (сервер `BagAct`): вид - дискриминатор `type`, своё поле у каждого действия. */
@Serializable
sealed interface BagAct {
    /** Новое имя текущему герою («Лист Угнетения»). */
    @Serializable
    @SerialName("RENAME")
    data class Rename(val name: String) : BagAct

    /** Дар из новой раздачи («Колода Рока»). */
    @Serializable
    @SerialName("REDEAL")
    data class Redeal(val fate: String) : BagAct
}

/**
 * «Использовать» стопки сумки (сервер `hero/bag/use`): предмет [code] и что нужно его действию [act]. Одна штука тратится той же
 * транзакцией, что и действие.
 */
@Serializable data class BagUse(val code: String, val act: BagAct)

/** Итог «Использовать»: новое имя героя или новое Предначертание аккаунта. */
@Serializable data class BagUsed(val name: String? = null, val fate: FateView? = null)
