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
import com.sperance.exileforge.core.app.TAB_EXPEDITION
import com.sperance.exileforge.core.app.TAB_GUILD
import com.sperance.exileforge.core.app.TAB_HERO
import com.sperance.exileforge.core.app.TAB_PROGRESS
import com.sperance.exileforge.core.app.lockedText
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.EngineRules
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.TipCallout
import com.sperance.exileforge.ui.components.toastFloor
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.solid
import com.sperance.exileforge.ui.nav.Route
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.depthPanel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Из чего вкладки панели читают свой знак: игра [game], досчёт ремёсел после отлучки, ещё не показанный листом, [craftsAway] и
 * приглашения в гильдию [invites].
 */
private class BarState(val game: GameUi, val craftsAway: Boolean, val invites: Int)

/** Знак в углу вкладки панели; [reason] - почему он стоит, его называет долгое нажатие на вкладку. */
private sealed interface BarBadge {
    fun reason(): String

    /** Раздел [feature] закрыт уровнем героя: замок; уровень открытия - по правилам [rules]. */
    class Locked(val feature: Feature, val rules: EngineRules?) : BarBadge {
        override fun reason(): String = feature.lockedText(rules)
    }

    /** Сколько ждёт решения: число [count]; [key] - ключ причины с этим числом. */
    class Free(val count: Int, val key: String) : BarBadge {
        override fun reason(): String = ui(key, count)
    }

    /** Что-то ждёт взгляда без числа: точка; [key] - ключ причины. */
    class Alert(val key: String) : BarBadge {
        override fun reason(): String = ui(key)
    }
}

/** Число [count] с причиной [key]; ноль - знака нет. */
private fun free(count: Int, key: String): BarBadge? = if (count > 0) BarBadge.Free(count, key) else null

/** Точка с причиной [key], пока [on]. */
private fun alert(on: Boolean, key: String): BarBadge? = if (on) BarBadge.Alert(key) else null

/**
 * Кнопка нижней панели: номер вкладки, ключ подписи, значок и [waits] - что ждёт на ней (число или точка); замок закрытого уровнем
 * раздела ставит [badge] сам.
 */
private enum class BarTab(val tab: Int, val title: String, val icon: ImageVector, private val waits: (BarState) -> BarBadge? = { null }) {
    HERO(TAB_HERO, "nav.hero", ForgeGlyphs.Helm),
    PROGRESS(TAB_PROGRESS, "nav.progress", ForgeGlyphs.Sigil, { free(it.game.freeTreePoints, "nav.reason.tree_points") ?: alert(it.craftsAway, "nav.reason.crafts_away") }),
    EXPEDITION(TAB_EXPEDITION, "nav.expedition", ForgeGlyphs.Swords, { free(it.game.freeAtlasPoints, "nav.reason.atlas_points") }),
    CITY(TAB_CITY, "nav.city", ForgeGlyphs.Keep),
    GUILD(TAB_GUILD, "nav.guild", ForgeGlyphs.Banner, { free(it.invites, "nav.reason.guild_invites") }),
    ;

    /** Знак вкладки в [state]: замок, пока раздел закрыт уровнем героя, иначе то, что ждёт на ней; ничего - null. */
    fun badge(state: BarState): BarBadge? {
        val feature = Feature.ofTab(tab)
        return if (feature != null && !state.game.unlocked(feature)) BarBadge.Locked(feature, state.game.index?.rules) else waits(state)
    }

    companion object {
        fun of(tab: Int): BarTab = entries.first { it.tab == tab }
    }
}

/** Во сколько раз капсула выбранной вкладки шире кнопки-значка. */
private const val CAPSULE_GROW = 2.4f

/**
 * Нижняя панель: парящая плита «Глубины»; у выбранной вкладки
 * кнопка раскрывается в капсулу - иконка заливается и растёт, рядом выезжает подпись; прочие - одни значки. Всё за 220 мс
 * той же кривой, что и сдвиг экрана. Пять вкладок у всех; закрытая уровнем героя - с замком, касание открывает её экран замка. Вкладка,
 * нажатая снова изнутри, возвращает к своему корню.
 *
 * Знак в углу вкладки - её [BarTab.badge]: замок, число того, что ждёт там решения, или точка (есть число - точка ждёт). Долгое
 * нажатие на вкладку со знаком называет причину окном у неё.
 */
@Composable internal fun GameBar(game: GameUi, route: Route) {
    val shell: ShellViewModel = koinViewModel()
    val craftsAway by shell.craftsAway.collectAsStateWithLifecycle()
    val invites by shell.guildInvites.collectAsStateWithLifecycle()
    val plate = RoundedCornerShape(24.dp)
    Row(
        Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 8.dp).toastFloor().depthPanel(plate, elevation = 16.dp).clip(plate)
            .fillMaxWidth().height(68.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val state = remember(game, craftsAway, invites) { BarState(game, craftsAway, invites) }
        PLAYER_TABS.forEach { index ->
            val tab = BarTab.of(index)
            BarCapsule(tab, selected = route.root?.tab == index, badge = tab.badge(state)) { shell.tab(index) }
        }
    }
}

/**
 * Кнопка панели: капсула с залитой иконкой и подписью, когда [selected], иначе значок; в углу знак [badge] - замок, число или точка.
 * Долгое нажатие открывает причину знака ([BarBadge.reason], собирается при открытии); нет знака - долгого нажатия нет.
 */
@Composable private fun RowScope.BarCapsule(tab: BarTab, selected: Boolean, badge: BarBadge?, onClick: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val spec = tween<Float>(TAB_SHIFT_MS, easing = TabShiftEasing)
    val grow by animateFloatAsState(if (selected) 1f else 0f, spec, label = "capsule")
    val tint by animateColorAsState(if (selected) Gold else Muted, tween(TAB_SHIFT_MS, easing = TabShiftEasing), label = "capsuleTint")
    val shape = RoundedCornerShape(24.dp)
    val label = ui(tab.title)
    Row(
        Modifier.weight(1f + (CAPSULE_GROW - 1f) * grow).height(48.dp).clip(shape)
            .background(PanelRaised.copy(alpha = grow))
            .border(1.dp, Gold.copy(alpha = .4f * grow), shape)
            .combinedClickable(
                role = Role.Tab,
                onLongClickLabel = badge?.let { ui("hint.hold.info") },
                onLongClick = badge?.let { { open = true } },
                onClick = onClick,
            )
            .semantics { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BadgedBox(badge = {
            when (badge) {
                is BarBadge.Locked -> Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.size(12.dp))
                is BarBadge.Free -> Badge(containerColor = GoldBright, contentColor = Ink) { Text(badge.count.toString(), fontSize = 9.sp) }
                is BarBadge.Alert -> Badge(containerColor = Caution)
                null -> Unit
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
        if (open && badge != null) TipCallout(Tip(body = badge.reason())) { open = false }
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
