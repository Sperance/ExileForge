package com.sperance.exileforge.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory

/** The pinned server's world, read from the test APK's assets: the screens are drawn over real content and real names. */
object TestWorld {
    private val assets get() = InstrumentationRegistry.getInstrumentation().context.assets
    private fun read(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

    val index: ContentIndex by lazy { ContentLoader.load { read("content/$it") } }
    val russian: LocaleBundle by lazy { LocaleBundle.parse("ru", "test", read("locale/ru.json")) }

    /** A rolled copy of the first common base of [slot], at [rarity], from a fixed seed. */
    fun roll(id: String, slot: Slot, rarity: Rarity, seed: Long = 1L): ItemInstance {
        val template = index.templates.values.first { it.slot == slot && !it.unique && !it.demanding }
        return ItemFactory(index).create(id, template, rarity, Dice(seed))
    }
}
