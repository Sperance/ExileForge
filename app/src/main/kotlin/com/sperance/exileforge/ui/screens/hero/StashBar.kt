package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.TipCallout
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.SlotIcon
import com.sperance.exileforge.ui.theme.*

/**
 * Фишка тайника в заголовке списка (3.90.5): «44/200» - места, значок автопродажи с числом включённых правил; нажатие
 * открывает лист тайника ([StashSheet]) - докупка мест и правила. Полный тайник - в красном.
 */
@Composable
internal fun StashChip(held: Int, capacity: Int, autoSellMarks: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val full = held >= capacity
    Row(
        Modifier.height(28.dp).clip(shape).border(1.dp, (if (full) LifeRed else Gold).copy(alpha = .5f), shape)
            .clickable(role = Role.Button, onClickLabel = ui("stash.sheet_title"), onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(ui("stash.places_chip", held, capacity), color = if (full) LifeRed else GoldBright, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Icon(Icons.Outlined.AutoMode, ui("autosell.title"), tint = if (autoSellMarks > 0) Vital else Muted, modifier = Modifier.size(14.dp))
        if (autoSellMarks > 0) Text(autoSellMarks.toString(), color = Vital, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
