package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.HeroPage
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.ui.components.PillTabs
import org.koin.compose.viewmodel.koinViewModel

/** Подпись раздела в полосе. */
private val HeroPage.label: String get() = when (this) {
    HeroPage.CHARACTER -> ui("hero.section_character")
    HeroPage.WORN -> ui("hero.section_worn")
    HeroPage.STASH -> ui("hero.section_stash")
    HeroPage.BAG -> ui("hero.section_bag")
}

/**
 * Полоса разделов вкладки «Герой» в оболочке (3.90.5): герой сам - в шапке игры ([com.sperance.exileforge.ui.ForgeBanner]),
 * здесь только четыре раздела его вещей; раздел запоминает модель героя. Вне вкладки героя полосы нет.
 */
@Composable fun HeroChrome(tab: Int) {
    if (tab != TAB_HERO) return
    val model = koinViewModel<HeroViewModel>()
    val page by model.page.collectAsStateWithLifecycle()
    HeroPageStrip(page) { model.page(it) }
}

/**
 * Полоса разделов героя (3.92.0, макет A): персонаж, надетое, тайник и сумка - вдавленная дорожка [PillTabs], как вкладки
 * везде в игре, прямо на фоне экрана, без своей подложки и черты.
 */
@Composable fun HeroPageStrip(selected: HeroPage, onSelect: (HeroPage) -> Unit) {
    PillTabs(
        HeroPage.entries.map { it.label },
        selected.ordinal,
        { HeroPage.entries[it].takeIf { page -> page != selected }?.let(onSelect) },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        segmented = true,
    )
}
