package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.SlotGroup
import com.sperance.exileforge.presentation.state.StashSort
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.TipCallout
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.SlotIcon
import com.sperance.exileforge.ui.theme.*

/**
 * Строка управления тайника (3.90.3, макет «Тайник» В): места «31/200 +» с докупкой ([fill]), порядок списком, дверь
 * автопродажи с числом включённых правил, «Продать» - режим выбора, и фильтры листом. Редкости, группы и порядок больше
 * не лежат рядами над списком: группа - рейка слева, остальное - лист фильтров.
 */
@Composable
internal fun StashTopBar(
    fill: @Composable () -> Unit,
    sort: StashSort,
    onSort: (StashSort) -> Unit,
    autoSellMarks: Int,
    onAutoSell: () -> Unit,
    canSell: Boolean,
    onSell: () -> Unit,
    tweaks: Int,
    onFilters: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        fill()
        SortPicker(sort, onSort, Modifier.weight(1f))
        AutoSellDoor(autoSellMarks, onAutoSell)
        SellDoor(canSell, onSell)
        StashFilterButton(tweaks, onFilters)
    }
}

/** «Новые ▾»: порядок тайника коротким словом, нажатие раскрывает все порядки. */
@Composable private fun SortPicker(sort: StashSort, onSort: (StashSort) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Text(
            ui("stash.sort_short.${sort.name.lowercase()}") + " ▾",
            color = GoldBright,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.DropdownList, onClickLabel = ui("stash.sort")) { open = true }
                .padding(horizontal = 6.dp, vertical = 8.dp),
        )
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

/** Дверь в правила автопродажи: значок и число включённых отметок. */
@Composable private fun AutoSellDoor(marks: Int, onClick: () -> Unit) {
    BadgedBox(badge = { if (marks > 0) Badge(containerColor = Vital, contentColor = Ink) { Text(marks.toString(), fontSize = 9.sp, maxLines = 1) } }) {
        IconButton(onClick = onClick, modifier = Modifier.requiredSize(36.dp).border(1.dp, Gold.copy(alpha = .6f), CircleShape)) {
            Icon(Icons.Outlined.AutoMode, ui("autosell.title"), tint = Gold, modifier = Modifier.size(18.dp))
        }
    }
}

/** «Продать»: монета и слово, вход в режим выбора; без продаваемого пачкой - погашена. */
@Composable private fun SellDoor(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val tint = if (enabled) GoldBright else Muted
    Row(
        Modifier.height(36.dp).clip(shape).border(1.dp, tint.copy(alpha = .6f), shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(ForgeGlyphs.Coins, null, tint = tint, modifier = Modifier.size(14.dp))
        Text(ui("hero.sell_do"), color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * Рейка тайника (3.90.3, макет В): узкий столбец значков мест - те же, что у надетого, - со счётом вещей; первым «всё»,
 * инструменты - пунктом в конце. Выбранный приподнят и в золоте; долгое нажатие называет пункт подсказкой.
 */
@Composable
internal fun StashRail(groups: List<Pair<SlotGroup, Int>>, total: Int, selected: SlotGroup?, lang: Lang, onSelect: (SlotGroup?) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.width(56.dp).verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RailEntry(ui("stash.all_things"), total, selected == null, { onSelect(null) }) { tint -> Icon(ForgeGlyphs.Stash, null, tint = tint, modifier = Modifier.size(22.dp)) }
        groups.forEach { (group, count) ->
            RailEntry(group.title(lang), count, group == selected, { onSelect(group) }) { tint ->
                if (group.isTool) Icon(ForgeGlyphs.Anvil, null, tint = tint, modifier = Modifier.size(22.dp)) else SlotIcon(group.slot, tint, Modifier.size(22.dp), tint = tint)
            }
        }
    }
}

/** Пункт рейки: значок в ячейке и счёт в углу; подпись - только подсказкой по долгому нажатию и для чтения с экрана. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RailEntry(label: String, count: Int, on: Boolean, onClick: () -> Unit, icon: @Composable (Color) -> Unit) {
    var tip by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(48.dp).then(if (on) Modifier.depthRaised(shape) else Modifier.depthInset(shape)).clip(shape)
            .semantics { contentDescription = ui("hero.slot_count", label, count) }
            .combinedClickable(role = Role.Tab, onLongClick = { tip = true }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon(if (on) Gold else Muted)
        Text(
            count.toString(),
            color = if (on) GoldBright else Muted,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
        )
        if (on) Box(Modifier.align(Alignment.CenterStart).width(2.dp).height(24.dp).background(Gold, RoundedCornerShape(1.dp)))
        if (tip) TipCallout(Tip(label)) { tip = false }
    }
}
