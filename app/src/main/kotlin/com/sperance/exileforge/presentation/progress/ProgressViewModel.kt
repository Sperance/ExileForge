package com.sperance.exileforge.presentation.progress

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.StateFlow

/** Хаб «Развитие» (3.80.35): срез «игра» для плиток и свежий герой, чьи сферы, питомцы и хроника на них считаются. */
class ProgressViewModel(slice: GameSlice, private val sync: HeroSync) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    fun ensure() = sync.ensure()
}
