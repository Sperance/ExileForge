package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.roll.LootChests

/**
 * Ценность стопки - цвет круглого ореола под её значком в ячейке ([com.sperance.exileforge.rules.content.Item.price] предмета):
 * обычная, ходовая, ценная, редчайшая; [floor] - цена, с которой ступень начинается.
 */
enum class StackValue(val floor: Long) {
    COMMON(0),
    POPULAR(100),
    VALUABLE(1_000),
    RAREST(10_000),
    ;

    companion object {
        /** Ступень цены [price]: высшая, чей порог она достаёт. */
        fun of(price: Long): StackValue = entries.last { price >= it.floor }
    }
}

/**
 * Облик стопки в ячейке: ценность [value] (ореол) и тир [tier], что ячейка пишет арабской цифрой на малой табличке в углу - тир
 * сундука-добычи и ступень эссенции (у особой эссенции тира нет); прочим - null.
 */
data class StackLook(val value: StackValue, val tier: Int?) {
    companion object {
        /** Облик стопки [code] по контенту [index]; без контента или предмета - обычная без тира. */
        fun of(index: ContentIndex?, code: String): StackLook {
            index ?: return StackLook(StackValue.COMMON, null)
            val item = index.item(ItemCode(code))
            val tier = index.essence(code)?.takeUnless { it.special }?.tier ?: LootChests(index).resolve(code)?.second
            return StackLook(StackValue.of(item?.price ?: 0), tier)
        }
    }
}
