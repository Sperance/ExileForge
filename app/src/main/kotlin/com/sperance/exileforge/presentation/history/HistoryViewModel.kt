package com.sperance.exileforge.presentation.history

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.model.hero.UniqueFind
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.StateFlow

/** «История» Города (3.90.2): разделы прошлого героя; пока - найденные уникальные вещи, экраном над зданием. */
class HistoryViewModel(slice: GameSlice, private val connection: ServerConnection, private val navigator: Navigator) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    /** Находки уникалок героя [heroId], читаются при открытии экрана; null - чтение не удалось. */
    suspend fun uniques(heroId: String): Map<String, UniqueFind>? = runCatching { connection.api.hero.uniques(heroId).finds }.getOrNull()

    fun openUniques() = navigator.open(Route.Uniques)
    fun back() = navigator.back()
}
