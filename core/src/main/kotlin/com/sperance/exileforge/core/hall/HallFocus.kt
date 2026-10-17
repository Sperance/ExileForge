package com.sperance.exileforge.core.hall

import com.sperance.exileforge.core.app.Landing
import com.sperance.exileforge.core.app.Screens
import com.sperance.exileforge.rules.content.HallBoard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Куда открыть доску славы: таблица [board] и строка героя [heroId], к которой её прокрутить. */
data class HallTarget(val board: HallBoard, val heroId: String)

/**
 * Просьба открыть доску славы на таблице и строке героя - одна на приложение: место по Мощи в карточке игрока и в своей сводке
 * героя кладут её и открывают доску; доска выбирает таблицу, прокручивает к строке героя (если он среди первых) и гасит просьбу.
 */
class HallFocus(private val screens: Screens) {
    private val mutable = MutableStateFlow<HallTarget?>(null)
    val target: StateFlow<HallTarget?> = mutable.asStateFlow()

    /** Открыть доску славы на таблице [board], прокрученной к герою [heroId]. */
    fun open(board: HallBoard, heroId: String) {
        mutable.value = HallTarget(board, heroId)
        screens.tab(Landing.HALL)
    }

    /** Доска показала строку - просьба погашена. */
    fun done() {
        mutable.value = null
    }
}
