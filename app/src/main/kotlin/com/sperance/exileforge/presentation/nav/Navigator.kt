package com.sperance.exileforge.presentation.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Стек экранов (3.80.23): один на приложение, его рисует `NavDisplay`, а меняют только фичи - через этот класс.
 * Вкладка нижней панели сбрасывает стек до своего корня; экран с корнем ложится над ним; аккаунт и настройки -
 * поверх чего угодно. Системный «назад» снимает верх, пока в стеке больше одного экрана.
 */
class Navigator {
    val stack: SnapshotStateList<Route> = mutableStateListOf(Route.Auth)

    private val mutable = MutableStateFlow<Route>(Route.Auth)

    /** Экран сверху стека. */
    val current: StateFlow<Route> = mutable

    /** Смена фазы: стек из одного экрана. */
    fun reset(route: Route) = replace(listOf(route))

    /** Вкладка или экран под ней: корень и, если это не он сам, экран над корнем; без корня - поверх открытого. */
    fun tab(route: Route) {
        val root = route.root ?: return open(route)
        replace(if (route == root) listOf(root) else listOf(root, route))
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
