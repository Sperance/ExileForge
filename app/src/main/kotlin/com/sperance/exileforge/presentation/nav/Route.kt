package com.sperance.exileforge.presentation.nav

import androidx.navigation3.runtime.NavKey
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.TAB_ACCOUNT
import com.sperance.exileforge.presentation.state.TAB_ADMIN
import com.sperance.exileforge.presentation.state.TAB_CHRONICLE
import com.sperance.exileforge.presentation.state.TAB_CITY
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_CRAFTS
import com.sperance.exileforge.presentation.state.TAB_EXPEDITION
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.TAB_PETS
import com.sperance.exileforge.presentation.state.TAB_PROGRESS
import com.sperance.exileforge.presentation.state.TAB_REDEMPTION
import com.sperance.exileforge.presentation.state.TAB_SETTINGS
import com.sperance.exileforge.presentation.state.TAB_SKILLS
import com.sperance.exileforge.presentation.state.TAB_TREE
import com.sperance.exileforge.presentation.state.TAB_TRIALS
import kotlinx.serialization.Serializable

/**
 * Экраны приложения (3.80.23): типизированные ключи стека Navigation 3. [root] - корень нижней панели, под которым
 * экран лежит (`null` - экран кладётся поверх любого); [tab] и [building] - прежние числа вкладок, которыми ещё
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

    @Serializable data object Pets : Route {
        override val root: Route get() = Progress
        override val tab get() = TAB_PETS
    }

    @Serializable data object Trials : Route {
        override val root: Route get() = Progress
        override val tab get() = TAB_TRIALS
    }

    @Serializable data object Chronicle : Route {
        override val root: Route get() = Progress
        override val tab get() = TAB_CHRONICLE
    }

    /** Атлас - небо над вкладками: без шапки и панели, системный «назад» возвращает на «Развитие». */
    @Serializable data object Atlas : Route {
        override val root: Route get() = Progress
        override val tab get() = TAB_PROGRESS
        override val bars get() = false
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

    /** Найденные уникалки (3.90.2): экран над «Историей», «назад» ведёт в неё. */
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
     * Настройки; [page] (3.95.2) - страница, открытая сразу с экрана «Аккаунт». Страница - часть маршрута, а не разовое поле
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
        private val GAME: List<Route> = listOf(Hero, Tree, Grimoire, Expedition, Crafts, Progress, Forge, Pets, Trials, Chronicle, City, Account, Settings(), Admin, Redemption)

        /** Экран по прежнему номеру вкладки; неизвестный номер - герой. */
        fun ofTab(tab: Int): Route = GAME.firstOrNull { it.tab == tab } ?: Hero

        /** Здание Города или его площадь. */
        fun ofBuilding(building: Building?): Route = when (building) {
            Building.QUESTS -> Quests
            Building.MERCHANT -> Merchant
            Building.AUCTION -> Auction
            Building.GUILD -> Guild
            Building.HISTORY -> History
            null -> City
        }
    }
}
