package com.sperance.exileforge.core.questboard

import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.StoryFold
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.QuestTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Квесты на экране: открытый раздел доски, доска для карты мира и свёрнутость карточки сюжета из настроек устройства; команды - у
 * [QuestActions].
 */
class QuestModel(
    private val actions: QuestActions,
    private val repository: QuestRepository,
    private val prefs: PreferencesRepository,
    /** Область экрана: правки свёрнутости пишутся, пока он жив. */
    private val scope: CoroutineScope,
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

    /** Свёрнутость карточки сюжета, из настроек устройства. */
    val storyFold: StateFlow<StoryFold> = prefs.settings.map { it.storyFold }.stateIn(scope, SharingStarted.Eagerly, prefs.settings.value.storyFold)

    /** Свернуть или раскрыть карточку сюжета. */
    fun foldStory(collapsed: Boolean) = editFold { copy(collapsed = collapsed) }

    /** Карточка показала шаг [quest] с наградой [ready]: новый шаг или ждущая награда раскрывают её ([StoryFold.seen]). */
    fun seeStory(quest: String, ready: Boolean) = editFold { seen(quest, ready) }

    private fun editFold(edit: StoryFold.() -> StoryFold) {
        scope.launch { prefs.editSettings { copy(storyFold = storyFold.edit()) } }
    }
}
