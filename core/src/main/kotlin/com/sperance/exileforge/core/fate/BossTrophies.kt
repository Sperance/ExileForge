package com.sperance.exileforge.core.fate

import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MonsterCode

/**
 * Трофеи одного босса у торговца: имя босса [title], [have] трофеев из цены обмена [price]; [block] - почему обмен закрыт словами,
 * null - обмен готов.
 */
data class BossTrophies(val boss: String, val title: String, val have: Int, val price: Int, val block: String?) {
    val tradable: Boolean get() = block == null
}

/**
 * Полка трофеев боссов (Предначертание «Трофейщик»): строки боссов с трофеями по имени и сколько из них готовы к обмену
 * ([tradable]). Пустая - полки нет.
 */
data class TrophyShelfView(val rows: List<BossTrophies>) {
    val tradable: Int = rows.count { it.tradable }
    val isEmpty: Boolean get() = rows.isEmpty()

    companion object {
        /**
         * Полка героя [hero] по контенту [index]. Цена обмена - на силе дара, как её считает сервер ([FateKit], `trophyPrice`); обмен
         * закрыт, пока дара нет, трофеев меньше цены (сервер - `FT_003`) или уникальных босса на уровне героя нет (`FT_004`, `trophyPool`).
         */
        fun of(index: ContentIndex, hero: HeroView): TrophyShelfView {
            val price = FateKit.of(index, hero.info.fate, hero.stats, hero.equipped.values).effects.trophyPrice()
            val rows = hero.info.trophies.filterValues { it > 0 }.map { (boss, have) ->
                val pool = index.monster(MonsterCode(boss))?.let { index.fates.trophyPool(index, it, hero.level) }.orEmpty()
                val block = when {
                    price == null -> ui("fate.trophies_no_fate")
                    have < price -> ui("fate.trophies_few", price - have)
                    pool.isEmpty() -> ui("fate.trophies_no_unique")
                    else -> null
                }
                BossTrophies(boss, monsterTitle(boss), have, price ?: 0, block)
            }
            return TrophyShelfView(rows.sortedBy { it.title })
        }
    }
}
