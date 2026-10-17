package com.sperance.exileforge.ui.screens.tree

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.hero.TreeModel
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.rules.content.TakenNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Древо навыков: команды - общие действия героя; узел под курсором и строка поиска - состояние экрана. */
class TreeViewModel(
    private val hero: HeroActions,
    commands: CommandRunner,
    slice: GameSlice,
    private val cursor: TreeModel,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val activity: StateFlow<Activity> = commands.state

    /** Код узла под курсором; пусто - ничего не выбрано. */
    val selected: StateFlow<String> = cursor.selected

    /** Строка поиска по названию узла и его статам. */
    val query: StateFlow<String> = cursor.query

    fun select(code: String) = cursor.select(code)
    fun query(text: String) = cursor.query(text)

    fun allocate(code: String, choice: Int? = null) = hero.allocateNode(code, choice)
    fun refund(code: String) = hero.refundNode(code)
    fun refundBranch(code: String) = hero.refundBranch(code)
    fun rechoose(code: String, choice: Int) = hero.rechooseNode(code, choice)
    fun reset() = hero.resetTree()
    fun socket(itemId: String, nodeCode: String) = hero.socketJewel(itemId, nodeCode)
    fun unsocket(itemId: String) = hero.unsocketJewel(itemId)
}
