package com.sperance.exileforge.core.forge

import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.refusalText
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.roll.Bench
import java.util.WeakHashMap

/**
 * Что верстак скажет над вещью - словами игрока, из правил ([Bench]), как сервер: [refusals] - почему не встанет каждый найденный
 * рецепт ([Bench.blockedBy]; рецепта, что встанет, в карте нет), [uncraft] - почему нельзя снять строку верстака
 * ([Bench.uncraftBlockedBy]; снять можно - null).
 */
class BenchVerdict private constructor(val refusals: Map<String, String>, val uncraft: String?) {
    companion object {
        /** Верстак правил - один на загруженный контент: новый контент - новый индекс и новый ключ. */
        private val benches = WeakHashMap<ContentIndex, Bench>()

        /** Приговор верстака над вещью [item] при найденных героем рецептах [known]. */
        fun of(item: ItemView, known: List<String>): BenchVerdict {
            val index = item.index
            val bench = synchronized(benches) { benches.getOrPut(index) { Bench(index) } }
            val refusals = index.bench.filter { it.code in known }.mapNotNull { recipe ->
                bench.blockedBy(item.item, item.template, recipe, known)?.let { recipe.code to refusalText(it) }
            }.toMap()
            return BenchVerdict(refusals, bench.uncraftBlockedBy(item.item, item.template)?.let { refusalText(it) })
        }
    }
}
