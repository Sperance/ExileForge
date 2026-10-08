package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.ui.components.motionClock
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import org.koin.compose.viewmodel.koinViewModel

/** Страницы «Похода» (4.0.0): карта мира, испытания и Атлас - под одной вкладкой, каждая своим экраном стека. */
enum class ExpeditionPage(val route: Route, private val title: String, val icon: ImageVector) {
    MAP(Route.Expedition, "expedition.page_map", ForgeGlyphs.Portal),
    TRIALS(Route.Trials, "trials.title", ForgeGlyphs.Skull),
    ATLAS(Route.Atlas, "atlas.title", ForgeGlyphs.Atlas),
    ;

    val label: String get() = ui(title)

    companion object {
        /** Страница, которой экран [route] является; не страница «Похода» - ничего. */
        fun of(route: Route): ExpeditionPage? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Полоса страниц «Похода» в оболочке (4.0.0, макет «Скрижали», компактный): каменная дорожка, у каждой страницы значок
 * и подпись в строку, под открытой пульсирует руна. Вне страниц «Похода» полосы нет. Закрытая уровнем страница
 * отвечает тостом навигатора, Атлас готовит своё окно сам ([ExpeditionViewModel.openAtlas]).
 */
@Composable fun ExpeditionChrome(route: Route) {
    val page = ExpeditionPage.of(route) ?: return
    val vm = koinViewModel<ExpeditionViewModel>()
    val track = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clip(track)
            .background(Brush.verticalGradient(listOf(TrackTop, TrackBottom))).border(1.dp, Bronze, track).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ExpeditionPage.entries.forEach { each ->
            PageTab(each, each == page, Modifier.weight(1f)) { vm.openPage(each.route) }
        }
    }
}

@Composable private fun PageTab(page: ExpeditionPage, on: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    val tint = if (on) GoldBright else Muted
    Box(
        modifier.clip(shape)
            .then(if (on) Modifier.background(Brush.verticalGradient(listOf(StoneTop, StoneBottom))).border(1.dp, Gold.copy(alpha = .35f), shape) else Modifier)
            .selectable(selected = on, role = Role.Tab) { if (!on) onSelect() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(page.icon, null, tint = tint, modifier = Modifier.size(15.dp))
            Text(page.label, color = tint, style = relicName(12), maxLines = 1)
        }
        if (on) {
            // Руна под открытой страницей: дышит 2,4 с
            val pulse = (1 - kotlin.math.cos(motionClock(RUNE_MS, "page-rune") * 2 * Math.PI).toFloat()) / 2
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp).width(28.dp).height(2.dp).background(Gold.copy(alpha = 1f - .55f * pulse), RoundedCornerShape(1.dp)))
        }
    }
}

private val TrackTop = Color(0xFF0B1116)
private val TrackBottom = Color(0xFF0A0F13)
private val StoneTop = Color(0xFF1C2A33)
private val StoneBottom = Color(0xFF121B22)
private const val RUNE_MS = 2400
