package com.sperance.exileforge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.PLAYER_TABS
import com.sperance.exileforge.core.app.TAB_ADMIN
import com.sperance.exileforge.core.app.TAB_CITY
import com.sperance.exileforge.core.app.TAB_CRAFTS
import com.sperance.exileforge.core.app.TAB_EXPEDITION
import com.sperance.exileforge.core.app.TAB_HERO
import com.sperance.exileforge.core.app.TAB_PROGRESS
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.solid
import com.sperance.exileforge.ui.nav.Route
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.depthPanel
import org.koin.compose.viewmodel.koinViewModel

/** Кнопка нижней панели: номер вкладки, ключ подписи и значок. */
private enum class BarTab(val tab: Int, val title: String, val icon: ImageVector) {
    HERO(TAB_HERO, "nav.hero", ForgeGlyphs.Helm),
    PROGRESS(TAB_PROGRESS, "nav.progress", ForgeGlyphs.Sigil),
    EXPEDITION(TAB_EXPEDITION, "nav.expedition", ForgeGlyphs.Swords),
    CITY(TAB_CITY, "nav.city", ForgeGlyphs.Keep),
    CRAFTS(TAB_CRAFTS, "nav.crafts", ForgeGlyphs.Anvil),
    ADMIN(TAB_ADMIN, "nav.admin", ForgeGlyphs.Scroll),
    ;

    companion object {
        fun of(tab: Int): BarTab = entries.first { it.tab == tab }
    }
}

/** Во сколько раз капсула выбранной вкладки шире кнопки-значка. */
private const val CAPSULE_GROW = 2.4f

/**
 * Нижняя панель: парящая плита «Глубины»; у выбранной вкладки
 * кнопка раскрывается в капсулу - иконка заливается и растёт, рядом выезжает подпись; прочие - одни значки. Всё за 220 мс
 * той же кривой, что и сдвиг экрана. Пять вкладок - игра, администратор получает ровно ещё одну. Вкладка, нажатая снова
 * изнутри, возвращает к своему корню.
 *
 * Свободные очки отмечают вкладку, откуда их тратят: атлас - «Поход», дерево - «Развитие»; досчёт ремёсел после отлучки, ещё не показанный
 * листом, - точкой на «Ремёслах».
 */
@Composable internal fun GameBar(game: GameUi, route: Route) {
    val shell: ShellViewModel = koinViewModel()
    val craftsAway by shell.craftsAway.collectAsStateWithLifecycle()
    val plate = RoundedCornerShape(24.dp)
    val destinations = PLAYER_TABS + listOfNotNull(TAB_ADMIN.takeIf { game.adminTools })
    Row(
        Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 8.dp).depthPanel(plate, elevation = 16.dp).clip(plate)
            .fillMaxWidth().height(68.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        destinations.forEach { index ->
            val free = when (index) {
                TAB_EXPEDITION -> game.freeAtlasPoints
                TAB_PROGRESS -> game.freeTreePoints
                else -> 0
            }
            BarCapsule(
                BarTab.of(index),
                selected = route.root?.tab == index,
                locked = !game.unlocked(Feature.ofTab(index)),
                free = free,
                alert = index == TAB_CRAFTS && craftsAway,
            ) { shell.tab(index) }
        }
    }
}

/** Кнопка панели: капсула с залитой иконкой и подписью, когда [selected], иначе значок; в углу замок, число очков или точка [alert]. */
@Composable private fun RowScope.BarCapsule(tab: BarTab, selected: Boolean, locked: Boolean, free: Int, alert: Boolean, onClick: () -> Unit) {
    val spec = tween<Float>(TAB_SHIFT_MS, easing = TabShiftEasing)
    val grow by animateFloatAsState(if (selected) 1f else 0f, spec, label = "capsule")
    val tint by animateColorAsState(if (selected) Gold else Muted, tween(TAB_SHIFT_MS, easing = TabShiftEasing), label = "capsuleTint")
    val shape = RoundedCornerShape(22.dp)
    val label = ui(tab.title)
    Row(
        Modifier.weight(1f + (CAPSULE_GROW - 1f) * grow).height(44.dp).clip(shape)
            .background(PanelRaised.copy(alpha = grow))
            .border(1.dp, Gold.copy(alpha = .4f * grow), shape)
            .selectable(selected, role = Role.Tab, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BadgedBox(badge = {
            if (locked) {
                Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(12.dp))
            } else if (free > 0) {
                Badge(containerColor = GoldBright, contentColor = Ink) { Text(free.toString(), fontSize = 9.sp) }
            } else if (alert) {
                Badge(containerColor = GoldBright)
            }
        }) {
            val scale = 1f + .08f * grow
            Box(
                Modifier.size(22.dp).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                val solid = remember(tab) { tab.icon.solid() }
                Icon(solid, null, tint = tint, modifier = Modifier.size(22.dp).graphicsLayer { alpha = grow })
                Icon(tab.icon, label, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        AnimatedVisibility(
            selected,
            enter = expandHorizontally(tween(TAB_SHIFT_MS, easing = TabShiftEasing)) + fadeIn(spec),
            exit = shrinkHorizontally(tween(TAB_SHIFT_MS, easing = TabShiftEasing)) + fadeOut(spec),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    color = GoldBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}
