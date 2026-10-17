package com.sperance.exileforge.presentation.history

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.presentation.hero.HeroReads
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import kotlinx.coroutines.flow.StateFlow

/** «История» Города: разделы прошлого героя - найденные уникальные вещи и Летопись, каждый экраном над зданием. */
class HistoryViewModel(slice: GameSlice, private val reads: HeroReads, private val navigator: Navigator) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    /** Находки уникалок героя [heroId], общие обоим экранам и свежие полминуты; null - чтение не удалось. */
    suspend fun uniques(heroId: String): Map<String, UniqueFind>? = reads.uniques(heroId)

    fun openUniques() = navigator.open(Route.Uniques)
    fun openChronicle() = navigator.open(Route.Chronicle)
    fun back() = navigator.back()

    /** На площадь Города: звено «Город» цепочки «назад». */
    fun city() = navigator.tab(Route.ofBuilding(null))
}
