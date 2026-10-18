package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.roll.ItemInstance

/**
 * Строки, что пропадут с заменой вещи: строки старой вещи, чьего семейства ([com.sperance.exileforge.rules.content.ModifierDef.family])
 * нет среди строк новой, - текстом, как они написаны на карточке старой вещи (с условиями), в порядке показа. Та же строка
 * другого тира или варианта не теряется: её число сравнивает итог «было → стало».
 */
object LostLines {
    /** Что теряет замена [replaced] вещью [item]; пустое гнездо (null) ничего не теряет. */
    fun of(index: ContentIndex?, item: ItemInstance, replaced: ItemInstance?): List<String> {
        val before = replaced?.let { index?.let { content -> ItemView.of(it, content) } } ?: return emptyList()
        val after = ItemView.of(item, before.index) ?: return emptyList()
        val kept = after.lines.mapTo(HashSet(), ::family)
        return before.orderedLines.filterNot { family(it) in kept }.map { it.text }
    }

    private fun family(line: ItemLine): String = line.definition?.family ?: line.code.value
}
