package com.sperance.exileforge.presentation.state

/**
 * Разделы вкладки «Герой» (3.90.3) одной полосой: персонаж, надетое, тайник и сумка - страницы экрана героя ([TAB_HERO]),
 * дерево и гримуар - свои экраны. Полоса оболочки ведёт на вкладку раздела и помнит страницу экрана героя.
 */
enum class HeroPage(val tab: Int) {
    CHARACTER(TAB_HERO),
    WORN(TAB_HERO),
    STASH(TAB_HERO),
    BAG(TAB_HERO),
    TREE(TAB_TREE),
    GRIMOIRE(TAB_SKILLS),
    ;

    companion object {
        /** Открытый раздел по вкладке: у экрана героя - его страница [hero], у прочих - их раздел; null - вкладка не героя. */
        fun of(tab: Int, hero: HeroPage): HeroPage? = if (tab == TAB_HERO) hero else entries.firstOrNull { it.tab == tab }
    }
}
