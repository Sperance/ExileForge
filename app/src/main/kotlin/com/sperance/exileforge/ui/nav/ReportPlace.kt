package com.sperance.exileforge.ui.nav

import com.sperance.exileforge.core.app.AppPhase
import com.sperance.exileforge.core.app.TAB_ADMIN
import com.sperance.exileforge.core.app.TAB_CITY
import com.sperance.exileforge.core.app.TAB_CRAFTS
import com.sperance.exileforge.core.app.TAB_EXPEDITION
import com.sperance.exileforge.core.app.TAB_HERO
import com.sperance.exileforge.core.app.TAB_PROGRESS
import com.sperance.exileforge.core.app.TAB_SETTINGS
import com.sperance.exileforge.core.i18n.ui

/**
 * Где игрок, для отчёта: [name] - для человека («Город › Auction»), [area] - раздел игры, что сервер пишет в Issue
 * строкой «Раздел» (`IssueBody.AREAS` сервера). Код экрана (`TAB:17`) остаётся в поле `screen` отчёта.
 */
data class ReportPlace(val name: String, val area: String)

/** Место экрана [route]: вне игры - фаза запуска, в игре - вкладка нижней панели и экран под ней. */
fun reportPlace(route: Route): ReportPlace {
    if (route.phase != AppPhase.GAME) return ReportPlace(route.phase.name.lowercase(), "start")
    val screen = route::class.simpleName.orEmpty()
    val root = route.root ?: route
    val (tab, area) = when (root.tab) {
        TAB_HERO -> "nav.hero" to "hero"

        TAB_PROGRESS -> "nav.progress" to "progress"

        TAB_EXPEDITION -> "nav.expedition" to if (route == Route.Atlas) "atlas" else "run"

        TAB_CITY -> "nav.city" to when (route) {
            Route.Guild -> "guild"
            Route.Auction -> "auction"
            else -> "city"
        }

        TAB_CRAFTS -> "nav.crafts" to "crafts"

        TAB_ADMIN -> "nav.admin" to "admin"

        TAB_SETTINGS -> null to "settings"

        else -> null to "hero"
    }
    val title = tab?.let(::ui)
    return ReportPlace(listOfNotNull(title, screen.takeIf { it.isNotBlank() && (title == null || it != root::class.simpleName) }).joinToString(" › "), area)
}
