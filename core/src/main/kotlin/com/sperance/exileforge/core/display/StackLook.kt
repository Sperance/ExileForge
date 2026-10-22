package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.StackValue
import com.sperance.exileforge.rules.roll.LootChests

/**
 * Облик стопки в ячейке: ценность [value] (ореол; ступени - правила `stackValue`) и тир [tier], что ячейка пишет арабской цифрой
 * на малой табличке в углу - тир сундука-добычи и ступень эссенции (у особой эссенции тира нет); прочим - null.
 */
data class StackLook(val value: StackValue, val tier: Int?) {
    companion object {
        /** Облик стопки [code] по контенту [index]; без контента или предмета - обычная без тира. */
        fun of(index: ContentIndex?, code: String): StackLook {
            index ?: return StackLook(StackValue.COMMON, null)
            val item = index.item(ItemCode(code))
            val tier = index.essence(code)?.takeUnless { it.special }?.tier ?: LootChests(index).resolve(code)?.second
            return StackLook(index.rules.stackValue.of(item?.price ?: 0), tier)
        }
    }
}
