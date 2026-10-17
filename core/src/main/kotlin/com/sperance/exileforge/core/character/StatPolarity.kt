package com.sperance.exileforge.core.character

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.StatGain

/**
 * Оценка сдвига характеристики - одна точка клиента над полярностью правил (`StatDef.gain`,
 * `StatRegistry.better`): её читают сравнение «было → стало» (вещи, узлы дерева), строки фляг и окно разбора характеристики. Знак
 * числа сам ничего не решает: «−4% получаемого урона» - к лучшему, нейтральная характеристика (`NONE`) - без цвета.
 */
object StatPolarity {
    /** Полярность характеристики [stat]; без контента или вне реестра - «больше лучше». */
    fun of(index: ContentIndex?, stat: String): StatGain = index?.stats?.get(stat)?.gain ?: StatGain.HIGHER

    /** Сдвиг [change] характеристики [stat] к лучшему - true, к худшему - false, без оценки (нейтральная, ноль) - null. */
    fun better(index: ContentIndex?, stat: String, change: Double): Boolean? = index?.stats?.better(stat, change) ?: StatGain.HIGHER.better(change)
}
