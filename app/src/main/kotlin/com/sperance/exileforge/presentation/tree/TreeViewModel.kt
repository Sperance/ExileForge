package com.sperance.exileforge.presentation.tree

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.TakenNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Древо навыков (3.80.16): команды - общие действия героя; узел под курсором и строка поиска - состояние экрана. */
class TreeViewModel(
    private val hero: HeroActions,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val activity: StateFlow<Activity> = commands.state

    private val mutableSelected = MutableStateFlow("")
    private val mutableQuery = MutableStateFlow("")

    /** Код узла под курсором; пусто - ничего не выбрано. */
    val selected: StateFlow<String> = mutableSelected

    /** Строка поиска по названию узла и его статам. */
    val query: StateFlow<String> = mutableQuery

    fun select(code: String) {
        mutableSelected.value = code
    }

    fun query(text: String) {
        mutableQuery.value = text
    }

    fun allocate(code: String, choice: Int? = null) = hero.allocateNode(code, choice)
    fun allocatePath(code: String, choice: Int? = null) = hero.allocatePath(code, choice)
    fun refund(code: String) = hero.refundNode(code)
    fun refundBranch(code: String) = hero.refundBranch(code)
    fun rechoose(code: String, choice: Int) = hero.rechooseNode(code, choice)
    fun reset() = hero.resetTree()
    fun plan(nodes: List<TakenNode>) = hero.planTree(nodes)
    fun socket(itemId: String, nodeCode: String) = hero.socketJewel(itemId, nodeCode)
    fun unsocket(itemId: String) = hero.unsocketJewel(itemId)
}
