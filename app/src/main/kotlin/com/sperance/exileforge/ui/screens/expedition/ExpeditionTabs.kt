package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.ui.components.StoneTab
import com.sperance.exileforge.ui.components.StoneTabs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.ModeHue
import com.sperance.exileforge.ui.theme.Rune
import org.koin.compose.viewmodel.koinViewModel

/** Страницы «Похода» (4.0.0): карта мира, испытания и Атлас - под одной вкладкой, каждая своим экраном стека; [hue] - цвет значка. */
enum class ExpeditionPage(val route: Route, private val title: String, val icon: ImageVector, val hue: Color) {
    MAP(Route.Expedition, "expedition.page_map", ForgeGlyphs.Portal, Gold),
    TRIALS(Route.Trials, "trials.title", ForgeGlyphs.Skull, ModeHue.Bosses),
    ATLAS(Route.Atlas, "atlas.title", ForgeGlyphs.Atlas, Rune),
    ;

    val label: String get() = ui(title)

    companion object {
        /** Страница, которой экран [route] является; не страница «Похода» - ничего. */
        fun of(route: Route): ExpeditionPage? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Полоса страниц «Похода» в оболочке (4.0.0, макет «Скрижали»; с 4.4.x - общие [StoneTabs]): каменная дорожка, у каждой
 * страницы значок над подписью. Вне страниц «Похода» полосы нет. Закрытая уровнем страница отвечает тостом навигатора,
 * Атлас готовит своё окно сам ([ExpeditionViewModel.openAtlas]).
 */
@Composable fun ExpeditionChrome(route: Route) {
    val page = ExpeditionPage.of(route) ?: return
    val vm = koinViewModel<ExpeditionViewModel>()
    val pages = ExpeditionPage.entries
    StoneTabs(pages.map { StoneTab(it.label, it.icon, it.hue) }, pages.indexOf(page), { vm.openPage(pages[it].route) }, scrollable = false)
}
