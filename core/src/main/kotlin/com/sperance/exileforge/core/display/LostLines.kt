package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.roll.ItemInstance

/**
 * Строки, что пропадут с заменой: строки всего, что снимет надевание ([com.sperance.exileforge.core.character.Sheets.takenOff]:
 * вещь на месте и вытесненное - оружие и щит под двуручным, щит под луком), чьи характеристики у героя после замены уходят в ноль:
 * их не даёт ни новая вещь, ни прочее надетое, и итог листа героя с новой вещью по ним - ноль. Текстом, как они написаны на карточках
 * снятых вещей (с условиями), в порядке показа. Характеристика, что остаётся (другим тиром, вариантом, семейством, с другой вещи или
 * из дерева), не теряется: её число сравнивает итог «было → стало».
 */
object LostLines {
    /**
     * Что теряет надевание [item] вместо [removed]; ничего не снимается - ничего не теряется. [worn] - вещи героя (надетое среди них
     * остаётся, кроме снятого), [after] - итоги листа героя с [item]; у фляги и инструмента, чьи строки только свои, - оба пусты.
     */
    fun of(
        index: ContentIndex?,
        item: ItemInstance,
        removed: List<ItemInstance>,
        worn: List<ItemInstance> = emptyList(),
        after: Map<String, Double> = emptyMap(),
    ): List<String> {
        index ?: return emptyList()
        if (removed.isEmpty()) return emptyList()
        val gone = removed.mapTo(HashSet()) { it.id }
        val staying = worn.filter { it.equipped && it.id != item.id && it.id !in gone } + item
        val kept = staying.mapNotNull { ItemView.of(it, index) }.flatMap { it.lines }.flatMapTo(HashSet(), ::stats)
        return removed.mapNotNull { ItemView.of(it, index) }.flatMap { it.orderedLines }
            .filter { line -> stats(line).none { it in kept || (after[it] ?: 0.0) != 0.0 } }
            .map { it.text }
    }

    /** Характеристики строки; строка без описания в контенте - сама своя характеристика. */
    private fun stats(line: ItemLine): List<String> = line.stats.ifEmpty { listOf(line.code.value) }
}
