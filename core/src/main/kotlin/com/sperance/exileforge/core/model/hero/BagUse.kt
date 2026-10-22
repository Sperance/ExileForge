package com.sperance.exileforge.core.model.hero

import com.sperance.exileforge.core.model.fate.FateView
import kotlinx.serialization.Serializable

/**
 * «Использовать» стопки сумки (сервер `hero/bag/use`): [code] - предмет, [name] - новое имя героя («Лист Угнетения»), [fate] - дар из
 * новой раздачи («Колода Рока»). Одна штука тратится той же транзакцией, что и действие.
 */
@Serializable data class BagUse(val code: String, val name: String = "", val fate: String = "")

/** Итог «Использовать»: новое имя героя или новое Предначертание аккаунта. */
@Serializable data class BagUsed(val name: String? = null, val fate: FateView? = null)
