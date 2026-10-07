package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.HeroPage
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Значок раздела в полосе героя. */
private val HeroPage.icon: ImageVector get() = when (this) {
    HeroPage.CHARACTER -> ForgeGlyphs.Exile
    HeroPage.WORN -> ForgeGlyphs.Helm
    HeroPage.STASH -> ForgeGlyphs.Stash
    HeroPage.BAG -> ForgeGlyphs.Orb
}

/** Короткая подпись раздела под значком. */
private val HeroPage.label: String get() = when (this) {
    HeroPage.CHARACTER -> ui("hero.section_character")
    HeroPage.WORN -> ui("hero.section_worn")
    HeroPage.STASH -> ui("hero.section_stash")
    HeroPage.BAG -> ui("hero.section_bag")
}

/**
 * Полоса разделов вкладки «Герой» в оболочке (3.90.5): герой сам - в шапке игры ([com.sperance.exileforge.ui.ForgeBanner]),
 * здесь только четыре раздела его вещей; раздел запоминает модель героя. Вне вкладки героя полосы нет.
 */
@Composable fun HeroChrome(tab: Int) {
    if (tab != TAB_HERO) return
    val model = koinViewModel<HeroViewModel>()
    val page by model.page.collectAsStateWithLifecycle()
    HeroPageStrip(page) { model.page(it) }
}

/**
 * Полоса разделов героя (3.90.5): персонаж, надетое, тайник и сумка - строка значков с подписями на всю ширину;
 * открытый подчёркнут золотом.
 */
@Composable fun HeroPageStrip(selected: HeroPage, onSelect: (HeroPage) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().background(Abyss)) {
            HeroPage.entries.forEach { page ->
                PageTab(page, on = page == selected, Modifier.weight(1f)) { if (page != selected) onSelect(page) }
            }
        }
        HorizontalDivider(color = PanelRaised)
    }
}

/** Один раздел полосы: значок над подписью и золотая черта у открытого. */
@Composable private fun PageTab(page: HeroPage, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.selectable(selected = on, role = Role.Tab, onClick = onClick).padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(page.icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(18.dp))
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
