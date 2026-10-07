package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.SellLot
import com.sperance.exileforge.presentation.state.SellPreset
import com.sperance.exileforge.presentation.state.SellSelection
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Выбор продажи (3.90.3) - одна абстракция на режим продажи тайника и окно конца захода: текущие [lots] и отмеченное поверх
 * них. Отметить можно только лот - то, что уходит пачкой; проданное выпадает из выбора само, с новыми лотами.
 */
@Stable
class SellPick internal constructor(val lots: List<SellLot>, private val state: MutableState<SellSelection>) {
    private val byId = lots.associateBy { it.id }

    /** Отмеченные живые лоты, их число и сумма. */
    val picked: List<SellLot> get() = state.value.picked(lots)
    val total: Long get() = picked.sumOf { it.price }

    fun sellable(id: String): Boolean = id in byId
    fun chosen(id: String): Boolean = id in byId && id in state.value.chosen

    fun toggle(id: String) {
        if (sellable(id)) state.value = state.value.toggle(id)
    }

    fun covers(preset: SellPreset): Boolean = state.value.covers(lots, preset)
    fun offers(preset: SellPreset): Boolean = lots.any(preset::takes)

    fun flip(preset: SellPreset) {
        state.value = state.value.flip(lots, preset)
    }

    fun clear() {
        state.value = SellSelection()
    }
}

/** Выбор над [lots]: отмеченное живёт, пока жив вызывающий экран, и переживает смену лотов. Ничего не отмечено заранее. */
@Composable fun rememberSellPick(lots: List<SellLot>): SellPick {
    val state = remember { mutableStateOf(SellSelection()) }
    return remember(lots) { SellPick(lots, state) }
}

/** Быстрые наборы над списком: «по правилам автопродажи» и по редкости; включённый снимается тем же нажатием. */
@Composable fun SellPresetRow(pick: SellPick, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SellPreset.ALL.forEach { preset ->
            val (label, tint) = when (preset) {
                SellPreset.Rules -> ui("sell.preset_rules") to GoldBright
                is SellPreset.OfRarity -> ui("stash.rarity_many.${preset.rarity.name}") to rarityColor(preset.rarity.name)
            }
            PresetChip(label, tint, on = pick.covers(preset), enabled = pick.offers(preset)) { pick.flip(preset) }
        }
    }
}

/** Фишка набора: контур в цвете набора, у включённого - заливка. */
@Composable private fun PresetChip(label: String, tint: Color, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    val ink = when {
        !enabled -> Muted.copy(alpha = .5f)
        on -> Abyss
        else -> tint
    }
    Box(
        Modifier.height(30.dp).clip(shape).then(if (on) Modifier.background(tint.copy(alpha = .9f), shape) else Modifier.border(1.dp, ink.copy(alpha = .45f), shape))
            .toggleable(on, enabled = enabled, role = Role.Checkbox) { onClick() }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1) }
}

/** Отметка строки: у вещи, что пачкой не продаётся (уникальная, запертая, надетая), - погашена. */
@Composable fun SellMark(pick: SellPick, id: String) {
    Checkbox(
        checked = pick.chosen(id),
        onCheckedChange = { pick.toggle(id) },
        enabled = pick.sellable(id),
        colors = CheckboxDefaults.colors(checkedColor = Vital, uncheckedColor = Muted, checkmarkColor = Ink, disabledUncheckedColor = Muted.copy(alpha = .25f)),
    )
}

/** Строка списка в режиме выбора: отметка слева, [content] - сама вещь; нажатие на вещь отмечает её. */
@Composable fun SellLine(pick: SellPick, id: String, content: @Composable (onToggle: () -> Unit) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SellMark(pick, id)
        Box(Modifier.weight(1f)) { content { pick.toggle(id) } }
    }
}

/**
 * Кнопка снизу «Продать N · +◎ сумма», удерживаемая: продажа не отменяется. [onCancel] - «Отмена» рядом (режим тайника);
 * без отмеченного - подсказка вместо суммы, кнопка погашена.
 */
@Composable fun SellDock(pick: SellPick, enabled: Boolean, modifier: Modifier = Modifier, onCancel: (() -> Unit)? = null, onSell: (List<String>) -> Unit) {
    val picked = pick.picked
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        onCancel?.let { ForgeOutlinedButton(onClick = it) { Text(ui("common.cancel")) } }
        HoldButton(
            if (picked.isEmpty()) ui("sell.pick_hint") else ui("sell.do_n", picked.size),
            Gold,
            Modifier.weight(1f),
            enabled = enabled && picked.isNotEmpty(),
            rearm = true,
            icon = ForgeGlyphs.Coins,
            figure = picked.takeIf { it.isNotEmpty() }?.let { "+" + number(pick.total.toDouble()) },
        ) { onSell(picked.map { it.id }) }
    }
}
