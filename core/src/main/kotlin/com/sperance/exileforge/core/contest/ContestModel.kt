package com.sperance.exileforge.core.contest

import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.rules.content.ContestBoard
import com.sperance.exileforge.rules.content.ContestKind
import com.sperance.exileforge.rules.content.ContestPast
import com.sperance.exileforge.rules.content.ContestTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Какая таблица открыта: состязание вида [kind] и лига [league] (null - лига героя, её называет ответ). */
data class ContestPick(val kind: ContestKind, val league: Int? = null)

/**
 * Состязания на экране: здание [board] (оба состязания в лиге героя), открытая таблица [pick] и её ответ [table] (null - ещё
 * читается); открытые итоги прошлого периода вида [pastKind] и их ответ [past] (null - ещё читаются); [hero] - чьи это состязания: у
 * другого героя (и его игрового мира) они другие.
 */
data class ContestUi(
    val board: ContestBoard? = null,
    val pick: ContestPick? = null,
    val table: ContestTable? = null,
    val hero: String = "",
    val pastKind: ContestKind? = null,
    val past: ContestPast? = null,
)

/**
 * Состязания Города: одно на приложение - плитка площади и здание читают одно здание. Здание читается не чаще [FRESH_MS] на героя
 * (площадь, открытая снова, сервер не спрашивает), по просьбе и со сменой периода - заново; таблица и итоги прошлого периода - при
 * каждом открытии. Ответ,
 * пришедший к уже сменённому выбору или герою, не показывается.
 */
class ContestModel(
    private val connection: ServerConnection,
    private val heroes: HeroRepository,
    private val commands: CommandRunner,
    slice: GameSlice,
) {
    val game: StateFlow<GameUi> = slice.ui
    val activity: StateFlow<Activity> = commands.state

    private val mutable = MutableStateFlow(ContestUi())
    val contests: StateFlow<ContestUi> = mutable.asStateFlow()

    /** Прочитать здание; [force] - даже если оно свежее (потянули вниз, период сменился). */
    fun load(force: Boolean = false) {
        val heroId = heroOnScreen() ?: return
        commands.read(Reads.CONTESTS, restart = force, silent = !force, tag = heroId, maxAgeMs = if (force) 0 else FRESH_MS) {
            val board = connection.api.contests.board(heroId)
            mutable.update { if (heroes.onScreen(heroId)) it.copy(board = board) else it }
        }
    }

    /** Открыть таблицу [pick]: прежняя таблица того же выбора остаётся на экране до ответа. */
    fun open(pick: ContestPick) {
        mutable.update { state -> state.copy(pick = pick, table = state.table.takeIf { state.pick == pick }) }
        loadTable()
    }

    /** Закрыть таблицу - назад к зданию. */
    fun close() = mutable.update { it.copy(pick = null, table = null) }

    /** Перечитать открытую таблицу. */
    fun loadTable() {
        val pick = mutable.value.pick ?: return
        val heroId = heroOnScreen() ?: return
        commands.read(Reads.CONTEST_TABLE, restart = true) {
            val table = connection.api.contests.table(heroId, pick.kind, pick.league)
            mutable.update { if (it.pick == pick && heroes.onScreen(heroId)) it.copy(table = table) else it }
        }
    }

    /** Открыть итоги прошлого периода вида [kind]: читаются заново при каждом открытии. */
    fun openPast(kind: ContestKind) {
        val heroId = heroOnScreen() ?: return
        mutable.update { it.copy(pastKind = kind, past = it.past?.takeIf { past -> past.kind == kind }) }
        commands.read(Reads.CONTEST_PAST, restart = true) {
            val past = connection.api.contests.past(heroId, kind)
            mutable.update { if (it.pastKind == kind && heroes.onScreen(heroId)) it.copy(past = past) else it }
        }
    }

    /** Закрыть итоги прошлого периода. */
    fun closePast() = mutable.update { it.copy(pastKind = null) }

    /** Герой на экране; сменился - состязания прежнего сброшены сразу, не ждут ответа. */
    private fun heroOnScreen(): String? {
        val heroId = heroes.heroId.takeIf { it.isNotBlank() } ?: return null
        mutable.update { if (it.hero == heroId) it else ContestUi(hero = heroId) }
        return heroId
    }

    private companion object {
        /** Сколько здание считается свежим: площадь, открытая снова за это время, сервер не спрашивает. */
        const val FRESH_MS = 30_000L
    }
}
