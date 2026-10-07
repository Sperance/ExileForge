package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.SlotGroup
import com.sperance.exileforge.presentation.state.StashFilter
import com.sperance.exileforge.presentation.state.StashSort
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * «Быстрые фильтры» тайника (3.88.6, выбор владельца): над списком - переключатель снаряжения и инструментов во всю ширину
 * (3.90.2, макет Б1); ниже ряд редкостей с местами тайника и «+» и кнопкой поиска, ряд групп мест со счётом, затем порядок
 * и «без надетого». Всё, что раньше жило в листе фильтров, - на виду; лист остаётся для поиска.
 */
@Composable
internal fun StashBar(
    tools: Boolean,
    onTools: (Boolean) -> Unit,
    fill: @Composable () -> Unit,
    tweaks: Int,
    onSearch: () -> Unit,
    filter: StashFilter,
    onFilter: (StashFilter) -> Unit,
    rarities: List<Rarity>,
    groupCounts: Map<SlotGroup, Int>,
    shelfSize: Int,
    lang: Lang,
    sort: StashSort,
    onSort: (StashSort) -> Unit,
    hideWorn: Boolean?,
    onHideWorn: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().depthInset(RoundedCornerShape(14.dp)).padding(3.dp)) {
            ShelfSwitch(ForgeGlyphs.Helm, ui("hero.stash_gear"), !tools, Modifier.weight(1f)) { onTools(false) }
            ShelfSwitch(ForgeGlyphs.Anvil, ui("hero.stash_tools"), tools, Modifier.weight(1f)) { onTools(true) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipLine(Modifier.weight(1f)) {
                QuickChip(ui("common.all"), filter.rarities.isEmpty(), Parchment, pill = true) { onFilter(filter.copy(rarities = emptySet())) }
                rarities.forEach { rarity ->
                    QuickChip(ui("stash.rarity_many.${rarity.name}"), rarity in filter.rarities, rarityColor(rarity.name), pill = true) { onFilter(filter.toggle(rarity)) }
                }
            }
            fill()
            StashFilterButton(tweaks, onSearch)
        }
        if (groupCounts.size > 1) {
            ChipLine {
                QuickChip(ui("hero.slot_count", ui("stash.all_things"), shelfSize), filter.groups.isEmpty(), GoldBright) { onFilter(filter.copy(groups = emptySet())) }
                groupCounts.forEach { (group, count) ->
                    QuickChip(ui("hero.slot_count", group.title(lang), count), group in filter.groups, GoldBright) { onFilter(filter.toggle(group)) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortPicker(sort, onSort)
            Spacer(Modifier.weight(1f))
            hideWorn?.let { on ->
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Switch) { onHideWorn(!on) }.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(
                        checked = on,
                        onCheckedChange = null,
                        modifier = Modifier.height(20.dp),
                        colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink, uncheckedTrackColor = PanelRaised, uncheckedBorderColor = Bronze),
                    )
                    Text(ui("stash.no_worn"), color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Ряд фишек, что уходит вбок прокруткой, а не переносится. */
@Composable private fun ChipLine(modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable RowScope.() -> Unit) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

/**
 * Фишка быстрого фильтра: [pill] - редкость, контур в её цвете и светлая заливка у выбранной; иначе - группа мест, тихий
 * прямоугольник, выбранный светлее.
 */
@Composable private fun QuickChip(label: String, on: Boolean, tint: Color, pill: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (pill) 15.dp else 10.dp)
    val ground = when {
        pill && on -> Modifier.background(tint.copy(alpha = .9f), shape)
        pill -> Modifier.border(1.dp, tint.copy(alpha = .4f), shape)
        on -> Modifier.depthRaised(shape)
        else -> Modifier.depthInset(shape)
    }
    Box(
        Modifier.height(30.dp).clip(shape).then(ground).selectable(on, role = Role.Checkbox, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = when {
                pill && on -> Abyss
                pill -> tint
                on -> GoldBright
                else -> Color(0xFF8FA0AB)
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (pill && on) FontWeight.ExtraBold else FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Половинка переключателя полок (3.90.2): значок и подпись, выбранная - приподнята на светлой подложке. */
@Composable private fun ShelfSwitch(icon: ImageVector, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    val tint = if (on) GoldBright else Muted
    Row(
        modifier.height(34.dp).then(if (on) Modifier.depthRaised(shape) else Modifier).clip(shape).selectable(on, role = Role.Tab, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(16.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

/** «Сортировка: по цене ▾»: нажатие раскрывает порядки. */
@Composable private fun SortPicker(sort: StashSort, onSort: (StashSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.DropdownList) { open = true }.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(ui("stash.sort") + ":", color = Color(0xFF8FA0AB), style = MaterialTheme.typography.labelMedium)
            Text(ui("stash.sort.${sort.name.lowercase()}").lowercase() + " ▾", color = GoldBright, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            StashSort.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(ui("stash.sort.${entry.name.lowercase()}"), color = if (entry == sort) Gold else Parchment) },
                    onClick = {
                        open = false
                        onSort(entry)
                    },
                )
            }
        }
    }
}
