package com.sperance.exileforge.core.hero

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Древо навыков на экране: узел под курсором и строка поиска. */
class TreeModel {
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
}
