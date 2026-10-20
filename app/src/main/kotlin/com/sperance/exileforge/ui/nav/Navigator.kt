package com.sperance.exileforge.ui.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.Landing
import com.sperance.exileforge.core.app.PLAYER_TABS
import com.sperance.exileforge.core.app.Screens
import com.sperance.exileforge.core.app.Spot
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.ui.screens.server.SettingsPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Стек экранов: один на приложение, его рисует `NavDisplay`, а меняют только фичи - через этот класс.
 * Вкладка нижней панели сбрасывает стек до своего корня; экран с корнем ложится над ним; аккаунт и настройки -
 * поверх чего угодно. Системный «назад» снимает верх, пока в стеке больше одного экрана. Экран, закрытый уровнем
 * героя, не открывается: тост говорит, с какого уровня; тестеры и администраторы проходят.
 */
class Navigator(
    private val heroes: HeroRepository,
    private val sessions: SessionRepository,
    private val notices: Notices,
    private val world: com.sperance.exileforge.core.world.WorldRepository,
) : Screens {
    val stack: SnapshotStateList<Route> = mutableStateListOf(Route.Auth)

    private val mutable = MutableStateFlow<Route>(Route.Auth)

    /** Экран сверху стека. */
    val current: StateFlow<Route> = mutable

    /**
     * Шаг по нижней панели последней смены стека: +1 - вкладка правее прежней, -1 - левее, 0 - смена не вкладки (экран поверх,
     * «назад», фаза). По нему переход экранов выбирает сторону сдвига.
     */
    var tabStep: Int = 0
        private set

    override val spot: Spot get() = current.value.let { Spot(it.phase, it.tab, it.building) }

    /** Смена фазы: стек из одного экрана. */
    fun reset(route: Route) = replace(listOf(route))

    override fun reset(landing: Landing) = reset(landing.route)

    override fun tab(landing: Landing) = tab(landing.route)

    override fun open(landing: Landing) = open(landing.route)

    /** Вкладка или экран под ней: корень и, если это не он сам, экран над корнем; без корня - поверх открытого. */
    fun tab(route: Route) {
        if (!gate(route)) return
        val root = route.root ?: return open(route)
        replace(if (route == root) listOf(root) else listOf(root, route))
    }

    /** Открыт ли экран уровню героя; закрытый отвечает тостом. */
    fun gate(route: Route): Boolean {
        val feature = Feature.ofBuilding(route.building) ?: Feature.ofTab(route.tab) ?: return true
        val holding = heroes.state.value
        if (sessions.state.value.freeProgress(holding)) return true
        val level = holding.hero?.level ?: sessions.state.value.characters.firstOrNull { it.id == holding.heroId }?.level ?: 1
        val needed = feature.level(world.state.value.content?.rules)
        if (level >= needed) return true
        notices.toast(ui("unlock.locked", ui(feature.title), needed))
        return false
    }

    /** Экран поверх открытого, если он уже не сверху. */
    fun open(route: Route) {
        if (stack.lastOrNull() == route) return
        tabStep = 0
        stack += route
        mutable.value = route
    }

    /** Экран под верхним: над ним открыт аккаунт или настройки; в стеке один экран - null. */
    val below: Route? get() = stack.getOrNull(stack.lastIndex - 1)

    /** Назад: верх снимается, последний экран остаётся. */
    fun back() {
        if (stack.size <= 1) return
        tabStep = 0
        stack.removeAt(stack.lastIndex)
        mutable.value = stack.last()
    }

    private fun replace(routes: List<Route>) {
        if (stack.toList() == routes) return
        tabStep = barStep(stack.lastOrNull(), routes.last())
        stack.clear()
        stack += routes
        mutable.value = routes.last()
    }

    /** Сторона перехода между вкладками [from] и [to] по порядку нижней панели; экран без панели, не вкладка или та же вкладка - 0. */
    private fun barStep(from: Route?, to: Route): Int {
        val old = from?.barSlot ?: return 0
        val new = to.barSlot ?: return 0
        return new.compareTo(old)
    }
}

/** Экран, к которому ведёт сервис ядра. */
private val Landing.route: Route
    get() = when (this) {
        Landing.AUTH -> Route.Auth
        Landing.CHARACTERS -> Route.Characters
        Landing.FATE -> Route.Fate
        Landing.HERO -> Route.Hero
        Landing.ATLAS -> Route.Atlas
        Landing.AUCTION -> Route.Auction
        Landing.GUILD -> Route.Guild
        Landing.HALL -> Route.Hall
        Landing.MAIL -> Route.Settings(SettingsPage.MAIL.name)
        Landing.MODERATION -> Route.Settings(SettingsPage.MODERATION.name)
    }

/** Место экрана в нижней панели слева направо ([PLAYER_TABS]): по его корню; экран без панели или не вкладка панели - null. */
val Route.barSlot: Int? get() = takeIf { it.bars }?.root?.let { PLAYER_TABS.indexOf(it.tab) }?.takeIf { it >= 0 }
