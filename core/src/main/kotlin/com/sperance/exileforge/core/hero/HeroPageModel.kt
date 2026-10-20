package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.app.Landing
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.Screens
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemShelf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Открытый раздел героя: полоса разделов в оболочке и экран героя читают один и тот же. */
class HeroPageModel(private val prefs: PreferencesRepository, private val screens: Screens) {
    private val openPage = MutableStateFlow(HeroPage.CHARACTER)

    val page: StateFlow<HeroPage> = openPage.asStateFlow()

    fun page(page: HeroPage) {
        openPage.value = page
    }

    /**
     * Тайник с фильтром [filter] из другого экрана: фильтр ложится на устройство, и только когда он лёг, открывается раздел тайника
     * вкладки героя - тайник читает запомненный фильтр при открытии.
     */
    suspend fun showStash(filter: ItemFilter) {
        prefs.saveItemFilter(ItemShelf.STASH, filter)
        prefs.itemFilters.first { it[ItemShelf.STASH] == filter }
        openPage.value = HeroPage.STASH
        screens.tab(Landing.HERO)
    }
}
