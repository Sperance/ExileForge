package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.session.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Оболочка приложения глазами ядра: какие вкладки открыты аккаунту, повторное нажатие на открытую вкладку, просьба листа
 * отчёта и узел древа, на котором оно откроется.
 */
class ShellModel(private val sessions: SessionRepository) {
    /** Открыта ли аккаунту вкладка [tab]: промокоды - только администратору, прочие - всем. */
    fun opens(tab: Int): Boolean = tab != TAB_REDEMPTION || sessions.state.value.isAdmin

    private val reselects = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Повторное нажатие на открытую вкладку: её экран закрывает вложенное и прокручивается наверх. */
    val reselected: SharedFlow<Int> = reselects

    fun reselect(tab: Int) {
        reselects.tryEmit(tab)
    }

    private val bugAsks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Просьбы открыть лист отчёта (аккорд громкости): его откроет хозяин системных листов. */
    val bugRequests: SharedFlow<Unit> = bugAsks

    fun requestBug() {
        bugAsks.tryEmit(Unit)
    }

    /** Узел дерева, на котором оно откроется: гнездо для самоцвета из карточки. */
    private var treeNode: String? = null

    fun aimTree(node: String) {
        treeNode = node
    }

    /** Узел, на котором открыть дерево, - один раз. */
    fun takeTreeNode(): String? = treeNode.also { treeNode = null }
}
