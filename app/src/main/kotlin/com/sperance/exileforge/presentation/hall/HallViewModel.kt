package com.sperance.exileforge.presentation.hall

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.HallBoard
import com.sperance.exileforge.rules.content.HallTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Какая таблица доски славы открыта (4.2.0): [board] из реестра [HallBoard], её раздел [scope] (раш - `регион:ступень`,
 * профессия - код). Лига - всегда лига героя (4.4.x), её называет ответ.
 */
data class HallPick(val board: HallBoard, val scope: String = "")

/**
 * Доска славы на экране: выбранная таблица [pick] и её ответ [table] (null - ещё читается или не прочитана); [hero] - чья это
 * таблица: у другого героя (и его игрового мира) она другая.
 */
data class HallUi(val pick: HallPick? = null, val table: HallTable? = null, val hero: String = "")

/**
 * Доска славы Города (4.2.0, сервер 1.84.0): одна таблица за раз, читается заново при каждом выборе. Ответ, пришедший к
 * уже сменённому выбору, не показывается.
 */
class HallViewModel(
    private val connection: ServerConnection,
    private val heroes: HeroRepository,
    private val commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui
    val activity: StateFlow<Activity> = commands.state

    private val mutable = MutableStateFlow(HallUi())
    val hall: StateFlow<HallUi> = mutable.asStateFlow()

    /** Открыть таблицу [pick]: прежняя таблица того же выбора остаётся на экране до ответа. */
    fun pick(pick: HallPick) {
        mutable.update { state -> HallUi(pick, state.table.takeIf { state.pick == pick }, state.hero) }
        load()
    }

    /** Перечитать открытую таблицу; сменился герой - таблица прежнего сброшена сразу, не ждёт ответа. */
    fun load() {
        val pick = mutable.value.pick ?: return
        val heroId = heroes.heroId.takeIf { it.isNotBlank() } ?: return
        mutable.update { if (it.hero == heroId) it else it.copy(table = null, hero = heroId) }
        commands.read(Reads.HALL, restart = true) {
            val table = connection.api.hall.table(heroId, pick.board, pick.scope)
            mutable.update { if (it.pick == pick && heroes.onScreen(heroId)) it.copy(table = table) else it }
        }
    }
}
