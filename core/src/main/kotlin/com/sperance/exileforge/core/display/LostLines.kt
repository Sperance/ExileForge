package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.roll.ItemInstance

/**
 * Строки, что пропадут с заменой: строки всего, что снимет надевание ([com.sperance.exileforge.core.character.Sheets.takenOff]:
 * вещь на месте и вытесненное - оружие и щит под двуручным, щит под луком), чьи характеристики после замены уходят в ноль -
 * новая вещь не двигает ни одной из них. Текстом, как они написаны на карточках снятых вещей (с условиями), в порядке показа.
 * Характеристика, что новая вещь тоже даёт (другим тиром, вариантом или семейством), не теряется: её число сравнивает итог «было → стало».
 */
object LostLines {
    /** Что теряет надевание [item] вместо [removed]; ничего не снимается - ничего не теряется. */
    fun of(index: ContentIndex?, item: ItemInstance, removed: List<ItemInstance>): List<String> {
        index ?: return emptyList()
        if (removed.isEmpty()) return emptyList()
        val after = ItemView.of(item, index) ?: return emptyList()
        val kept = after.lines.flatMapTo(HashSet(), ::stats)
        return removed.mapNotNull { ItemView.of(it, index) }.flatMap { it.orderedLines }.filter { line -> stats(line).none { it in kept } }.map { it.text }
    }

    /** Характеристики строки; строка без описания в контенте - сама своя характеристика. */
    private fun stats(line: ItemLine): List<String> = line.stats.ifEmpty { listOf(line.code.value) }
}
