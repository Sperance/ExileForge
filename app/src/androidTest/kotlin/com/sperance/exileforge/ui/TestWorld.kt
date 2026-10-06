package com.sperance.exileforge.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.rules.roll.ItemInstance

/** The pinned server's world, read from the test APK's assets: the screens are drawn over real content and real names. */
object TestWorld {
    private val assets get() = InstrumentationRegistry.getInstrumentation().context.assets
    private fun read(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

    val index: ContentIndex by lazy { ContentLoader.load { read("content/$it") } }
    val russian: LocaleBundle by lazy { LocaleBundle.parse("ru", "test", read("locale/ru.json")) }

    /** A rolled copy of the least demanding common base of [slot], at [rarity], from a fixed seed. */
    fun roll(id: String, slot: Slot, rarity: Rarity, seed: Long = 1L): ItemInstance {
        // Every base of a slot may ask for a level or an attribute: the one that asks least stands in
        val template = index.templates.values.filter { it.slot == slot && !it.unique }
            .minWith(compareBy({ it.demanding }, { it.requiredLevel }, { it.code }))
        return ItemFactory(index).create(id, template, rarity, Dice(seed))
    }

    /** Копия первой по коду вещи редкости [rarity] - уникальной или мифической - с номером экземпляра [serial]. */
    fun legend(id: String, rarity: Rarity, serial: Long): ItemInstance {
        val template = index.templates.values.filter { it.rarity == rarity }.minBy { it.code }
        return ItemFactory(index).create(id, template, rarity, Dice(1L)).apply { this.serial = serial }
    }
}
