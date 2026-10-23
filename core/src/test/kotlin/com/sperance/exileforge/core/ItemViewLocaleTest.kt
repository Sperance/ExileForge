package com.sperance.exileforge.core

import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.sheet.HeroLens
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Roll
import kotlin.test.Test
import kotlin.test.assertNotEquals

/**
 * Вид копии из общей памяти видов печатает строки на языке словаря сервера, что сейчас на экране: смена языка не оставляет
 * модификаторы на прежнем.
 */
class ItemViewLocaleTest {
    private val index = TestContent.index

    @Test
    fun view_follows_language_switch() {
        val def = index.definitions.first { it.affix && it.effects.size == 1 && it.tiers.isNotEmpty() }
        val template = index.templates.values.first { it.slot == Slot.RING && !it.unique }.code
        val ring = ItemInstance("ring", template, slot = Slot.RING, rolls = listOf(Roll(def.code, tier = 1, share = 0.5)))
        val locales = TestContent.serverLocales()
        val was = serverLocale
        try {
            serverLocale = LocaleBundle("ru", "", locales.getValue("ru"))
            val ru = HeroLens.view(index, ring)!!.orderedLines.map { it.text }
            serverLocale = LocaleBundle("en", "", locales.getValue("en"))
            val en = HeroLens.view(index, ring)!!.orderedLines.map { it.text }
            assertNotEquals(ru, en)
        } finally {
            serverLocale = was
        }
    }
}
