package com.sperance.exileforge.core.questboard

import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.QuestTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Квесты на экране: открытый раздел доски и доска для карты мира; команды - у [QuestActions]. */
class QuestModel(
    private val actions: QuestActions,
    private val repository: QuestRepository,
) {
    private val mutableTab = MutableStateFlow(QuestTab.DAILY)

    val tab: StateFlow<QuestTab> = mutableTab

    fun tab(tab: QuestTab) {
        mutableTab.value = tab
    }

    /** Доска для карты мира: читается, только если её ещё нет, - шаг сюжета двигают сдача и итоги захода, они её перечитывают. */
    fun glance() {
        if (repository.state.value.board == null) actions.load()
    }
}
