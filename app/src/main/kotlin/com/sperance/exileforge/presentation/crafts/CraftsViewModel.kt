package com.sperance.exileforge.presentation.crafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.SmithChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Экран ремёсел (3.80.14): ремёсла из репозитория, действия общие; открытое окно профессии - состояние экрана. */
class CraftsViewModel(
    private val actions: CraftsActions,
    repository: CraftsRepository,
    commands: CommandRunner,
    private val store: ServerStore,
    private val sessions: SessionRepository,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val crafts: StateFlow<Crafts> = repository.state
    val activity: StateFlow<Activity> = commands.state

    private val mutableProfession = MutableStateFlow("")

    /** Код профессии, чьё окно открыто; пусто - плитки. */
    val profession: StateFlow<String> = mutableProfession

    fun openProfession(code: String) {
        mutableProfession.value = code
    }

    fun load(silent: Boolean = false) = actions.load(silent)
    fun watch(on: Boolean) = actions.watch(on)
    fun start(job: String, choice: String = "", additives: List<String> = emptyList()) = actions.start(job, choice, additives)
    fun stop() = actions.stop()
    fun equipTool(itemId: String) = actions.equipTool(itemId)

    /** Последний запущенный выбор кузнеца героя на этом устройстве (3.89.0): лист открывается в его режиме. */
    suspend fun smithChoice(heroId: String): SmithChoice? = SmithChoice.of(store.smithChoice(sessions.state.value.server, heroId).orEmpty())

    fun rememberSmithChoice(heroId: String, choice: SmithChoice) {
        val server = sessions.state.value.server
        viewModelScope.launch { store.saveSmithChoice(server, heroId, choice.name) }
    }
}
