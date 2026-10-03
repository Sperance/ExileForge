package com.sperance.exileforge.presentation.crafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.data.settings.ServerStore
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
) : ViewModel() {
    val crafts: StateFlow<Crafts> = repository.state
    val activity: StateFlow<Activity> = commands.state

    private val mutableProfession = MutableStateFlow("")

    /** Код профессии, чьё окно открыто; пусто - плитки. */
    val profession: StateFlow<String> = mutableProfession

    fun openProfession(code: String) {
        mutableProfession.value = code
    }

    fun load(silent: Boolean = false) = actions.load(silent)
    fun start(job: String, choice: String = "", additives: List<String> = emptyList()) = actions.start(job, choice, additives)
    fun stop() = actions.stop()
    fun equipTool(itemId: String) = actions.equipTool(itemId)

    /** «Пока вас не было» (3.69.0): последний показанный итог героя, и отметка показанного. */
    suspend fun craftsAwaySeen(heroId: String): Long = store.craftsAwaySeen(sessions.state.value.server, heroId)

    fun markCraftsAwaySeen(heroId: String, until: Long) {
        val server = sessions.state.value.server
        viewModelScope.launch { store.saveCraftsAwaySeen(server, heroId, until) }
    }
}
