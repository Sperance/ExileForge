package com.sperance.exileforge.ui.screens.craft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.forge.SmithyModel
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.hero.HeroSync
import com.sperance.exileforge.core.session.CommandRunner

/** Кузница: модель ядра [model] в области экрана - выбор инструмента живёт, пока жив экран. */
class SmithyViewModel(hero: HeroActions, sync: HeroSync, commands: CommandRunner, slice: GameSlice) : ViewModel() {
    val model = SmithyModel(hero, sync, commands, slice, viewModelScope)
}
