package com.sperance.exileforge.ui.nav

import androidx.navigation3.runtime.NavKey
import com.sperance.exileforge.core.app.AppPhase
import com.sperance.exileforge.core.app.Building
import com.sperance.exileforge.core.app.TAB_ACCOUNT
import com.sperance.exileforge.core.app.TAB_ADMIN
import com.sperance.exileforge.core.app.TAB_CITY
import com.sperance.exileforge.core.app.TAB_CRAFT
import com.sperance.exileforge.core.app.TAB_CRAFTS
import com.sperance.exileforge.core.app.TAB_EXPEDITION
import com.sperance.exileforge.core.app.TAB_HERO
import com.sperance.exileforge.core.app.TAB_PROGRESS
import com.sperance.exileforge.core.app.TAB_REDEMPTION
import com.sperance.exileforge.core.app.TAB_SETTINGS
import com.sperance.exileforge.core.app.TAB_SKILLS
import com.sperance.exileforge.core.app.TAB_TREE
import com.sperance.exileforge.core.app.TAB_TRIALS
import com.sperance.exileforge.core.i18n.ui
import kotlinx.serialization.Serializable

/**
 * Экраны приложения: типизированные ключи стека Navigation 3. [root] - корень нижней панели, под которым
 * экран лежит (`null` - экран кладётся поверх любого); [tab] и [building] - числовые номера вкладок, которыми ещё
 * читают состояние не переведённые экраны; [bars] - рисуются ли шапка и нижняя панель.
 */
@Serializable
sealed interface Route : NavKey {
    val phase: AppPhase get() = AppPhase.GAME
    val root: Route? get() = this
    val tab: Int get() = TAB_HERO
    val building: Building? get() = null
    val bars: Boolean get() = true

    @Serializable data object Auth : Route {
        override val phase get() = AppPhase.AUTH
        override val bars get() = false
    }

    @Serializable data object Characters : Route {
        override val phase get() = AppPhase.CHARACTERS
        override val bars get() = false
    }

    /** Выбор Предначертания аккаунта: до списка героев, пока дар не выбран. */
    @Serializable data object Fate : Route {
        override val phase get() = AppPhase.CHARACTERS
        override val bars get() = false
    }

    @Serializable data object Hero : Route

    @Serializable data object Tree : Route {
        override val root: Route get() = Progress
        override val tab get() = TAB_TREE
    }

    @Serializable data object Grimoire : Route {
        override val root: Route get() = Progress
        override val tab get() = TAB_SKILLS
    }

    @Serializable data object Expedition : Route {
        override val tab get() = TAB_EXPEDITION
    }

    @Serializable data object Crafts : Route {
        override val tab get() = TAB_CRAFTS
    }

    @Serializable data object Progress : Route {
        override val tab get() = TAB_PROGRESS
    }

    @Serializable data object Forge : Route {
        override val root: Route get() = Progress
        override val tab get() = TAB_CRAFT
    }

    /** Испытания: страница «Похода», прежде «Развития». */
    @Serializable data object Trials : Route {
        override val root: Route get() = Expedition
        override val tab get() = TAB_TRIALS
    }

    /** Атлас: страница «Похода» под полосой страниц, прежде небо над вкладками «Развития». */
    @Serializable data object Atlas : Route {
        override val root: Route get() = Expedition
        override val tab get() = TAB_EXPEDITION
    }

    /** Сюжет: все главы и шаг под рукой, экран над картой «Похода»; открыт с первого уровня. */
    @Serializable data object Story : Route {
        override val root: Route get() = Expedition
        override val tab get() = TAB_EXPEDITION
    }

    @Serializable data object City : Route {
        override val tab get() = TAB_CITY
    }

    @Serializable data object Quests : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.QUESTS
    }

    @Serializable data object Merchant : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.MERCHANT
    }

    @Serializable data object Auction : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.AUCTION
    }

    @Serializable data object Guild : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.GUILD
    }

    @Serializable data object History : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.HISTORY
    }

    /** Летопись: вторая плитка «Истории», экран над ней - «назад» ведёт в неё. */
    @Serializable data object Chronicle : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.HISTORY
    }

    /** Доска славы: здание Города, таблицы реестра `HallBoard`. */
    @Serializable data object Hall : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.HALL
    }

    /** Найденные уникалки: экран над «Историей», «назад» ведёт в неё. */
    @Serializable data object Uniques : Route {
        override val root: Route get() = City
        override val tab get() = TAB_CITY
        override val building get() = Building.HISTORY
    }

    /** Аккаунт и настройки ложатся поверх открытой вкладки: «назад» возвращает на неё. */
    @Serializable data object Account : Route {
        override val root: Route? get() = null
        override val tab get() = TAB_ACCOUNT
    }

    /**
     * Настройки; [page] - страница, открытая сразу с экрана «Аккаунт». Страница - часть маршрута, а не разовое поле
     * модели: у каждой свой ключ сохранённого состояния, и быстрый повторный вход не поднимает прежнюю страницу.
     */
    @Serializable data class Settings(val page: String? = null) : Route {
        override val root: Route? get() = null
        override val tab get() = TAB_SETTINGS
    }

    @Serializable data object Admin : Route {
        override val tab get() = TAB_ADMIN
    }

    @Serializable data object Redemption : Route {
        override val root: Route get() = Admin
        override val tab get() = TAB_REDEMPTION
    }

    companion object {
        private val GAME: List<Route> = listOf(Hero, Tree, Grimoire, Expedition, Crafts, Progress, Forge, Trials, City, Account, Settings(), Admin, Redemption)

        /** Экран по прежнему номеру вкладки; неизвестный номер - герой. */
        fun ofTab(tab: Int): Route = GAME.firstOrNull { it.tab == tab } ?: Hero

        /** Здание Города или его площадь. */
        fun ofBuilding(building: Building?): Route = when (building) {
            Building.QUESTS -> Quests
            Building.MERCHANT -> Merchant
            Building.AUCTION -> Auction
            Building.GUILD -> Guild
            Building.HISTORY -> History
            Building.HALL -> Hall
            null -> City
        }
    }
}

/**
 * Где игрок, для отчёта: [name] - для человека («Город › Auction»), [area] - раздел игры, что сервер пишет в Issue
 * строкой «Раздел» (`IssueBody.AREAS` сервера). Код экрана (`TAB:17`) остаётся в поле `screen` отчёта.
 */
data class ReportPlace(val name: String, val area: String)

/** Ключ имени вкладки нижней панели [tab]; не вкладка панели - null. */
private fun barKey(tab: Int): String? = when (tab) {
    TAB_HERO -> "nav.hero"
    TAB_PROGRESS -> "nav.progress"
    TAB_EXPEDITION -> "nav.expedition"
    TAB_CITY -> "nav.city"
    TAB_CRAFTS -> "nav.crafts"
    TAB_ADMIN -> "nav.admin"
    else -> null
}

/** Имя, каким экран [route] называет корень цепочки «назад» над ним: вкладка панели под ним, настройки или «Врата мира». */
fun routeTitle(route: Route): String = barKey((route.root ?: route).tab)?.let(::ui) ?: ui(if (route.tab == TAB_SETTINGS) "settings.title" else "account.title")

/** Место экрана [route]: вне игры - фаза запуска, в игре - вкладка нижней панели и экран под ней. */
fun reportPlace(route: Route): ReportPlace {
    if (route.phase != AppPhase.GAME) return ReportPlace(route.phase.name.lowercase(), "start")
    val screen = route::class.simpleName.orEmpty()
    val root = route.root ?: route
    val area = when (root.tab) {
        TAB_PROGRESS -> "progress"

        TAB_EXPEDITION -> if (route == Route.Atlas) "atlas" else "run"

        TAB_CITY -> when (route) {
            Route.Guild -> "guild"
            Route.Auction -> "auction"
            else -> "city"
        }

        TAB_CRAFTS -> "crafts"

        TAB_ADMIN -> "admin"

        TAB_SETTINGS -> "settings"

        else -> "hero"
    }
    val title = barKey(root.tab)?.let(::ui)
    return ReportPlace(listOfNotNull(title, screen.takeIf { it.isNotBlank() && (title == null || it != root::class.simpleName) }).joinToString(" › "), area)
}
