package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.HeroPage
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.ui.components.CollapsibleHeader
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Значок раздела в полосе героя. */
private val HeroPage.icon: ImageVector get() = when (this) {
    HeroPage.CHARACTER -> ForgeGlyphs.Exile
    HeroPage.WORN -> ForgeGlyphs.Helm
    HeroPage.STASH -> ForgeGlyphs.Stash
    HeroPage.BAG -> ForgeGlyphs.Orb
    HeroPage.TREE -> ForgeGlyphs.Constellation
    HeroPage.GRIMOIRE -> ForgeGlyphs.Grimoire
}

/** Короткая подпись раздела под значком. */
private val HeroPage.label: String get() = when (this) {
    HeroPage.CHARACTER -> ui("hero.section_character")
    HeroPage.WORN -> ui("hero.section_worn")
    HeroPage.STASH -> ui("hero.section_stash")
    HeroPage.BAG -> ui("hero.section_bag")
    HeroPage.TREE -> ui("nav.tree")
    HeroPage.GRIMOIRE -> ui("nav.skills")
}

/**
 * Шапка вкладки «Герой» в оболочке (3.90.3): строка героя, что уходит при прокрутке, и под ней полоса разделов. Раздел экрана
 * героя запоминает модель героя, дерево и гримуар открываются своей вкладкой [onTab]; вне разделов героя шапки нет.
 */
@Composable fun HeroChrome(game: GameUi, tab: Int, onTab: (Int) -> Unit) {
    val model = koinViewModel<HeroViewModel>()
    val page by model.page.collectAsStateWithLifecycle()
    val open = HeroPage.of(tab, page) ?: return
    val header = rememberHeroHeader(game)
    if (header != null) CollapsibleHeader { HeroLine(header) }
    HeroPageStrip(
        open,
        locked = { !game.unlocked(Feature.ofTab(it.tab)) },
        badge = { if (it == HeroPage.TREE) header?.tree?.available ?: 0 else 0 },
    ) { picked ->
        if (picked.tab == TAB_HERO) model.page(picked)
        if (picked.tab != tab) onTab(picked.tab)
    }
}

/** Ширина, меньше которой раздел полосы не сжимается: тогда полоса прокручивается вбок. */
private val PageMinWidth = 58.dp

/**
 * Полоса разделов героя (3.90.3): персонаж, надетое, тайник, сумка, дерево и гримуар - одна строка значков с подписями
 * вместо двух полос; открытый подчёркнут золотом. Запертый уровнем раздел носит замок ([locked]), [badge] - число на значке
 * (свободные очки дерева). На узком экране, где разделы не помещаются, полоса прокручивается.
 */
@Composable fun HeroPageStrip(selected: HeroPage, locked: (HeroPage) -> Boolean, badge: (HeroPage) -> Int, onSelect: (HeroPage) -> Unit) {
    Column {
        BoxWithConstraints(Modifier.fillMaxWidth().background(Abyss)) {
            val fits = maxWidth / HeroPage.entries.size >= PageMinWidth
            Row(if (fits) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())) {
                HeroPage.entries.forEach { page ->
                    PageTab(
                        page,
                        on = page == selected,
                        locked = locked(page),
                        badge = badge(page),
                        modifier = if (fits) Modifier.weight(1f) else Modifier.width(PageMinWidth + 8.dp),
                    ) { if (page != selected) onSelect(page) }
                }
            }
        }
        HorizontalDivider(color = PanelRaised)
    }
}

/** Один раздел полосы: значок (замок у запертого, число у отмеченного) над подписью и золотая черта у открытого. */
@Composable private fun PageTab(page: HeroPage, on: Boolean, locked: Boolean, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.selectable(selected = on, role = Role.Tab, onClick = onClick).padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BadgedBox(badge = { if (badge > 0 && !locked) Badge(containerColor = GoldBright, contentColor = Ink) { Text(badge.toString(), fontSize = 9.sp) } }) {
            Icon(if (locked) Icons.Outlined.Lock else page.icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(18.dp))
        }
        Text(
            page.label,
            color = if (on) GoldBright else Muted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) Gold else Color.Transparent))
    }
}
