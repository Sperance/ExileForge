package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.StoneTab
import com.sperance.exileforge.ui.components.StoneTabs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.nav.Route
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.ModeHue
import com.sperance.exileforge.ui.theme.Rune
import org.koin.compose.viewmodel.koinViewModel

/**
 * Страницы «Похода»: карта мира, испытания и Атлас - под одной вкладкой, каждая своим экраном стека; [hue] - цвет значка,
 * [alerts] - на странице есть что сделать (метка вкладки [StoneTab.alert]): у Атласа - свободные очки.
 */
enum class ExpeditionPage(val route: Route, private val title: String, val icon: ImageVector, val hue: Color, val alerts: (GameUi) -> Boolean = { false }) {
    MAP(Route.Expedition, "expedition.page_map", ForgeGlyphs.Portal, Gold),
    TRIALS(Route.Trials, "trials.title", ForgeGlyphs.Skull, ModeHue.Bosses),
    ATLAS(Route.Atlas, "atlas.title", ForgeGlyphs.Atlas, Rune, alerts = { it.freeAtlasPoints > 0 }),
    ;

    val label: String get() = ui(title)

    companion object {
        /** Страница, которой экран [route] является; не страница «Похода» - ничего. */
        fun of(route: Route): ExpeditionPage? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Полоса страниц «Похода» в оболочке (общие [StoneTabs]): каменная дорожка, у каждой
 * страницы значок над подписью. Вне страниц «Похода» полосы нет. Закрытая уровнем страница отвечает тостом навигатора,
 * Атлас готовит своё окно сам ([ExpeditionViewModel.openAtlas]).
 */
@Composable fun ExpeditionChrome(route: Route) {
    val page = ExpeditionPage.of(route) ?: return
    val vm = koinViewModel<ExpeditionViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val pages = ExpeditionPage.entries
    StoneTabs(pages.map { StoneTab(it.label, it.icon, it.hue, it.alerts(game)) }, pages.indexOf(page), { vm.openPage(pages[it].route) }, scrollable = false)
}
