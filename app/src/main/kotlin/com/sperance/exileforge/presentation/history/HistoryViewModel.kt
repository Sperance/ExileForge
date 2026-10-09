package com.sperance.exileforge.presentation.history

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.presentation.hero.HeroReads
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.StateFlow

/** «История» Города (3.90.2): разделы прошлого героя; пока - найденные уникальные вещи, экраном над зданием. */
class HistoryViewModel(slice: GameSlice, private val reads: HeroReads, private val navigator: Navigator) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    /** Находки уникалок героя [heroId], общие обоим экранам и свежие полминуты (3.94.1); null - чтение не удалось. */
    suspend fun uniques(heroId: String): Map<String, UniqueFind>? = reads.uniques(heroId)

    fun openUniques() = navigator.open(Route.Uniques)
    fun back() = navigator.back()

    /** На площадь Города: звено «Город» цепочки «назад» (4.4.x). */
    fun city() = navigator.tab(Route.ofBuilding(null))
}
