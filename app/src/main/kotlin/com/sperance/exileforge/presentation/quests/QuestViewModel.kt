package com.sperance.exileforge.presentation.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.QuestTab
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.StoryFold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Экран квестов Города, вкладка квестов гильдии (3.80.10) и сюжет «Похода» (4.2.0): доски из репозитория, действия общие,
 * раздел - свой.
 */
class QuestViewModel(
    private val actions: QuestActions,
    repository: QuestRepository,
    commands: CommandRunner,
    slice: GameSlice,
    private val navigator: Navigator,
    private val prefs: PreferencesRepository,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val quests: StateFlow<Quests> = repository.state
    val activity: StateFlow<Activity> = commands.state

    private val mutableTab = MutableStateFlow(QuestTab.DAILY)
    val tab: StateFlow<QuestTab> = mutableTab

    fun tab(tab: QuestTab) {
        mutableTab.value = tab
    }

    fun load() = actions.load()
    fun loadGuild() = actions.loadGuild()

    /** Экран открылся (3.94.1): ответ моложе полуминуты не перечитывается. */
    fun open() = actions.load(fresh = true)
    fun openGuild() = actions.loadGuild(fresh = true)
    fun claim(questId: String) = actions.claim(questId)
    fun take(offerId: String) = actions.take(offerId)
    fun reset(offerId: String) = actions.reset(offerId)
    fun abandon(questId: String) = actions.abandon(questId)

    /** Закон трона за пройденную главу: выбор навсегда, без перевыбора. */
    fun law(code: String) = actions.law(code)
    fun claimGuild(questId: String? = null, goal: String? = null) = actions.claimGuild(questId, goal)

    /** Свёрнутость карточки сюжета (4.4.x), из настроек устройства. */
    val storyFold: StateFlow<StoryFold> = prefs.settings.map { it.storyFold }.stateIn(viewModelScope, SharingStarted.Eagerly, prefs.settings.value.storyFold)

    /** Свернуть или раскрыть карточку сюжета. */
    fun foldStory(collapsed: Boolean) = editFold { copy(collapsed = collapsed) }

    /** Карточка показала шаг [quest] с наградой [ready]: новый шаг или ждущая награда раскрывают её ([StoryFold.seen]). */
    fun seeStory(quest: String, ready: Boolean) = editFold { seen(quest, ready) }

    private fun editFold(edit: StoryFold.() -> StoryFold) {
        viewModelScope.launch { prefs.editSettings { copy(storyFold = storyFold.edit()) } }
    }

    /** Весь сюжет - экран над картой «Похода». */
    fun openStory() = navigator.tab(Route.Story)
    fun back() = navigator.back()
}
