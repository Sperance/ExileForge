package com.sperance.exileforge.ui.screens.quests

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.questboard.QuestActions
import com.sperance.exileforge.core.questboard.QuestModel
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.quests.QuestTab
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
import kotlinx.coroutines.flow.StateFlow

/**
 * Экран квестов Города, вкладка квестов гильдии и сюжет «Похода»: доски из репозитория, действия общие,
 * раздел - свой.
 */
class QuestViewModel(
    private val actions: QuestActions,
    repository: QuestRepository,
    commands: CommandRunner,
    slice: GameSlice,
    private val navigator: Navigator,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val quests: StateFlow<Quests> = repository.state
    val activity: StateFlow<Activity> = commands.state

    private val model = QuestModel(actions, repository)

    val tab: StateFlow<QuestTab> = model.tab

    fun tab(tab: QuestTab) = model.tab(tab)

    fun load() = actions.load()
    fun loadGuild() = actions.loadGuild()

    /** Экран открылся: ответ моложе полуминуты не перечитывается. */
    fun open() = actions.load(fresh = true)
    fun openGuild() = actions.loadGuild(fresh = true)

    /** Доска для карты мира: читается, только если её ещё нет. */
    fun glance() = model.glance()
    fun claim(questId: String) = actions.claim(questId)
    fun take(offerId: String) = actions.take(offerId)
    fun reset(offerId: String) = actions.reset(offerId)
    fun abandon(questId: String) = actions.abandon(questId)

    /** «Сдать всё»: всё готовое на доске - разом. */
    fun claimAll() = actions.claimAll()

    /** Закон трона за пройденную главу: выбор навсегда, без перевыбора. */
    fun law(code: String) = actions.law(code)
    fun book(chapter: Int, code: String) = actions.book(chapter, code)
    fun claimGuild(questId: String? = null, goal: String? = null) = actions.claimGuild(questId, goal)

    /** Перечитать текст «после» сданного шага [step] главы [chapter]. */
    fun reread(chapter: Int, step: String) = actions.reread(chapter, step)

    /** Весь сюжет - экран над картой «Похода». */
    fun openStory() = navigator.tab(Route.Story)
    fun back() = navigator.back()
}
