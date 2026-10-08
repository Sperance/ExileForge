package com.sperance.exileforge.presentation.quests

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.QuestTab
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    fun abandon(questId: String) = actions.abandon(questId)
    fun claimGuild(questId: String? = null, goal: String? = null) = actions.claimGuild(questId, goal)

    /** Весь сюжет - экран над картой «Похода». */
    fun openStory() = navigator.tab(Route.Story)
    fun back() = navigator.back()
}
