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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.PLAYER_TABS
import com.sperance.exileforge.core.app.TAB_CITY
import com.sperance.exileforge.core.app.TAB_CRAFTS
import com.sperance.exileforge.core.app.TAB_EXPEDITION
import com.sperance.exileforge.core.app.TAB_HERO
import com.sperance.exileforge.core.app.TAB_PROGRESS
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.TipCallout
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

/**
 * Кнопка нижней панели: номер вкладки, ключ подписи и значок; [freeReason] - ключ причины числа свободных очков в углу, [alertReason] -
 * ключ причины точки (долгое нажатие на вкладку называет их).
 */
private enum class BarTab(val tab: Int, val title: String, val icon: ImageVector, val freeReason: String? = null, val alertReason: String? = null) {
    HERO(TAB_HERO, "nav.hero", ForgeGlyphs.Helm),
    PROGRESS(TAB_PROGRESS, "nav.progress", ForgeGlyphs.Sigil, freeReason = "nav.reason.tree_points"),
    EXPEDITION(TAB_EXPEDITION, "nav.expedition", ForgeGlyphs.Swords, freeReason = "nav.reason.atlas_points"),
    CITY(TAB_CITY, "nav.city", ForgeGlyphs.Keep),
    CRAFTS(TAB_CRAFTS, "nav.crafts", ForgeGlyphs.Anvil, alertReason = "nav.reason.crafts_away"),
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
 * той же кривой, что и сдвиг экрана. Пять вкладок у всех. Вкладка, нажатая снова
 * изнутри, возвращает к своему корню.
 *
 * Свободные очки отмечают вкладку, откуда их тратят: атлас - «Поход», дерево - «Развитие»; досчёт ремёсел после отлучки, ещё не показанный
 * листом, - точкой на «Ремёслах». Долгое нажатие на вкладку с замком, числом или точкой называет причину окном у неё.
 */
@Composable internal fun GameBar(game: GameUi, route: Route) {
    val shell: ShellViewModel = koinViewModel()
    val craftsAway by shell.craftsAway.collectAsStateWithLifecycle()
    val plate = RoundedCornerShape(24.dp)
    Row(
        Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 8.dp).depthPanel(plate, elevation = 16.dp).clip(plate)
            .fillMaxWidth().height(68.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PLAYER_TABS.forEach { index ->
            val free = when (index) {
                TAB_EXPEDITION -> game.freeAtlasPoints
                TAB_PROGRESS -> game.freeTreePoints
                else -> 0
            }
            val tab = BarTab.of(index)
            val feature = Feature.ofTab(index)
            val locked = !game.unlocked(feature)
            val alert = index == TAB_CRAFTS && craftsAway
            val reason = when {
                locked -> feature?.let { ui("unlock.locked", ui(it.title), it.level(game.index?.rules)) }
                free > 0 -> tab.freeReason?.let { ui(it, free) }
                alert -> tab.alertReason?.let { ui(it) }
                else -> null
            }
            BarCapsule(tab, selected = route.root?.tab == index, locked = locked, free = free, alert = alert, reason = reason) { shell.tab(index) }
        }
    }
}

/**
 * Кнопка панели: капсула с залитой иконкой и подписью, когда [selected], иначе значок; в углу замок, число очков или точка [alert].
 * Долгое нажатие открывает [reason] - почему в углу знак; нет причины - долгого нажатия нет.
 */
@Composable private fun RowScope.BarCapsule(tab: BarTab, selected: Boolean, locked: Boolean, free: Int, alert: Boolean, reason: String?, onClick: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val spec = tween<Float>(TAB_SHIFT_MS, easing = TabShiftEasing)
    val grow by animateFloatAsState(if (selected) 1f else 0f, spec, label = "capsule")
    val tint by animateColorAsState(if (selected) Gold else Muted, tween(TAB_SHIFT_MS, easing = TabShiftEasing), label = "capsuleTint")
    val shape = RoundedCornerShape(22.dp)
    val label = ui(tab.title)
    Row(
        Modifier.weight(1f + (CAPSULE_GROW - 1f) * grow).height(44.dp).clip(shape)
            .background(PanelRaised.copy(alpha = grow))
            .border(1.dp, Gold.copy(alpha = .4f * grow), shape)
            .combinedClickable(
                role = Role.Tab,
                onLongClickLabel = reason?.let { ui("hint.hold.info") },
                onLongClick = reason?.let { { open = true } },
                onClick = onClick,
            )
            .semantics { this.selected = selected },
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
        if (open && reason != null) TipCallout(Tip(body = reason)) { open = false }
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
