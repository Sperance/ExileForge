package com.sperance.exileforge.presentation.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.presentation.state.Feature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Стек экранов (3.80.23): один на приложение, его рисует `NavDisplay`, а меняют только фичи - через этот класс.
 * Вкладка нижней панели сбрасывает стек до своего корня; экран с корнем ложится над ним; аккаунт и настройки -
 * поверх чего угодно. Системный «назад» снимает верх, пока в стеке больше одного экрана. Экран, закрытый уровнем
 * героя (3.76.0), не открывается: тост говорит, с какого уровня; тестеры и администраторы проходят.
 */
class Navigator(
    private val heroes: HeroRepository,
    private val sessions: SessionRepository,
    private val notices: Notices,
) {
    val stack: SnapshotStateList<Route> = mutableStateListOf(Route.Auth)

    private val mutable = MutableStateFlow<Route>(Route.Auth)

    /** Экран сверху стека. */
    val current: StateFlow<Route> = mutable

    /** Смена фазы: стек из одного экрана. */
    fun reset(route: Route) = replace(listOf(route))

    /** Вкладка или экран под ней: корень и, если это не он сам, экран над корнем; без корня - поверх открытого. */
    fun tab(route: Route) {
        if (!gate(route)) return
        val root = route.root ?: return open(route)
        replace(if (route == root) listOf(root) else listOf(root, route))
    }

    /** Открыт ли экран уровню героя; закрытый отвечает тостом. */
    fun gate(route: Route): Boolean {
        val feature = Feature.ofBuilding(route.building) ?: Feature.ofTab(route.tab) ?: return true
        if (sessions.state.value.isTester) return true
        val holding = heroes.state.value
        val level = holding.hero?.level ?: sessions.state.value.characters.firstOrNull { it.id == holding.heroId }?.level ?: 1
        if (level >= feature.level) return true
        notices.toast(ui("unlock.locked", ui(feature.title), feature.level))
        return false
    }

    /** Экран поверх открытого, если он уже не сверху. */
    fun open(route: Route) {
        if (stack.lastOrNull() == route) return
        stack += route
        mutable.value = route
    }

    /** Назад: верх снимается, последний экран остаётся. */
    fun back() {
        if (stack.size <= 1) return
        stack.removeAt(stack.lastIndex)
        mutable.value = stack.last()
    }

    private fun replace(routes: List<Route>) {
        if (stack.toList() == routes) return
        stack.clear()
        stack += routes
        mutable.value = routes.last()
    }
}
