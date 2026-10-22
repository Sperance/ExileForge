package com.sperance.exileforge.ui.screens.city

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.hero.HeroReads
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
import kotlinx.coroutines.flow.StateFlow

/** «История» Города: разделы прошлого героя - найденные уникальные вещи и Летопись, каждый экраном над зданием. */
class HistoryViewModel(slice: GameSlice, private val reads: HeroReads, private val navigator: Navigator) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    /** Находки уникалок героя [heroId], общие обоим экранам и свежие полминуты; null - чтение не удалось. */
    suspend fun uniques(heroId: String): Map<String, UniqueFind>? = reads.uniques(heroId)

    /** Статистика героя [heroId] для Бестиария (убийства, боссы, убийцы), свежая полминуты; null - чтение не удалось. */
    suspend fun stats(heroId: String): Map<String, Long>? = reads.stats(heroId)

    fun openUniques() = navigator.open(Route.Uniques)
    fun openBestiary() = navigator.open(Route.Bestiary)
    fun openChronicle() = navigator.open(Route.Chronicle)
    fun back() = navigator.back()

    /** На площадь Города: звено «Город» цепочки «назад». */
    fun city() = navigator.tab(Route.ofBuilding(null))
}
