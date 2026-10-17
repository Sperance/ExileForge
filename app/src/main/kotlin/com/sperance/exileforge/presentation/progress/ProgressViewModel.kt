package com.sperance.exileforge.presentation.progress

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.presentation.hero.HeroSync
import kotlinx.coroutines.flow.StateFlow

/** Хаб «Развитие»: срез «игра» для плиток и свежий герой, чьи сферы и очки дерева на них считаются. */
class ProgressViewModel(slice: GameSlice, private val sync: HeroSync) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    fun ensure() = sync.ensure()
}
