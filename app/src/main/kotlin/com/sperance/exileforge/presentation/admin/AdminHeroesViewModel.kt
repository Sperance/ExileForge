package com.sperance.exileforge.presentation.admin

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.AdminHeroPage
import com.sperance.exileforge.core.network.AdminHeroSort
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Список героев администратора (3.81.0): поиск, порядок, номер страницы и сама страница с сервера. */
data class AdminHeroes(
    val query: String = "",
    val sort: AdminHeroSort = AdminHeroSort.LEVEL,
    val page: AdminHeroPage = AdminHeroPage(),
)

/**
 * Герои всех аккаунтов глазами администратора (3.81.0, сервер 1.76.0): поиск по имени, порядок по уровню, дате или имени,
 * блокировка с причиной и её снятие. Заблокированный герой не играет; аккаунт создаёт нового.
 */
class AdminHeroesViewModel(
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val sessions: SessionRepository,
    private val notices: Notices,
) : ViewModel() {
    private val mutable = MutableStateFlow(AdminHeroes())
    val heroes: StateFlow<AdminHeroes> = mutable.asStateFlow()
    val activity = commands.state

    fun search(query: String) = mutable.update { it.copy(query = query) }

    fun sort(sort: AdminHeroSort) {
        mutable.update { it.copy(sort = sort) }
        load(0)
    }

    /** Страница [page] под текущими поиском и порядком. */
    fun load(page: Int = mutable.value.page.page) = commands.read(Reads.ADMIN_HEROES, restart = true) {
        check(sessions.state.value.isAdmin) { ui("hero.grant_admin_only") }
        val state = mutable.value
        val answer = connection.api.admin.heroes(state.query, state.sort, page.coerceAtLeast(0), PAGE)
        mutable.update { it.copy(page = answer) }
    }

    /** Блок с причиной или его снятие; строка героя заменяется ответом сервера. */
    fun block(heroId: String, blocked: Boolean, reason: String) = commands.task(writing = true) {
        check(sessions.state.value.isAdmin) { ui("hero.grant_admin_only") }
        check(!blocked || reason.isNotBlank()) { ui("admin.heroes_reason_needed") }
        val row = connection.api.admin.blockHero(heroId, blocked, reason)
        mutable.update { s -> s.copy(page = s.page.copy(heroes = s.page.heroes.map { if (it.id == row.id) row else it })) }
        notices.toast(ui(if (blocked) "admin.heroes_blocked" else "admin.heroes_unblocked", row.name))
    }

    private companion object {
        const val PAGE = 30
    }
}
