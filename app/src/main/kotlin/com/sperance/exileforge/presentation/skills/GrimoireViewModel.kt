package com.sperance.exileforge.presentation.skills

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.StateFlow

/** Гримуар (3.80.19): книги, ячейки навыков, условия и пояс флаконов; команды - общие действия героя. */
class GrimoireViewModel(
    private val hero: HeroActions,
    private val sync: HeroSync,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val activity: StateFlow<Activity> = commands.state

    fun ensure() = sync.ensure()
    fun learnSkill(code: String) = hero.learnSkill(code)
    fun slotSkill(kind: String, index: Int, code: String?, condition: String? = null) = hero.slotSkill(kind, index, code, condition)
    fun flaskCondition(index: Int, condition: String?) = hero.flaskCondition(index, condition)
    fun exchangeBooks(books: List<String>, code: String) = hero.exchangeBooks(books, code)
}
