package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.SellLot
import com.sperance.exileforge.core.market.SellPreset
import com.sperance.exileforge.core.market.SellSelection
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Выбор продажи - одна абстракция на режим продажи тайника и окно конца захода: текущие [lots] и отмеченное поверх
 * них. Отметить можно только лот - то, что уходит пачкой; проданное выпадает из выбора само, с новыми лотами. Быстрые наборы берут только [shown] - лоты открытого пункта рейки и фильтров; отмеченное в других пунктах остаётся.
 */
@Stable
class SellPick internal constructor(val lots: List<SellLot>, private val shown: List<SellLot>, private val state: MutableState<SellSelection>) {
    private val byId = lots.associateBy { it.id }

    /** Отмеченные живые лоты, их число и сумма. */
    val picked: List<SellLot> get() = state.value.picked(lots)
    val total: Long get() = picked.sumOf { it.price }

    fun sellable(id: String): Boolean = id in byId
    fun chosen(id: String): Boolean = id in byId && id in state.value.chosen

    fun toggle(id: String) {
        if (sellable(id)) state.value = state.value.toggle(id)
    }

    fun covers(preset: SellPreset): Boolean = state.value.covers(shown, preset)
    fun offers(preset: SellPreset): Boolean = shown.any(preset::takes)

    fun flip(preset: SellPreset) {
        state.value = state.value.flip(shown, preset)
    }

    fun clear() {
        state.value = SellSelection()
    }
}

/**
 * Выбор над [lots]: отмеченное живёт, пока жив вызывающий экран, и переживает смену лотов. Заранее отмечено [initial]
 * (помеченное к продаже посреди захода). [shown] - id на экране, по которым работают наборы; null - все лоты.
 */
@Composable fun rememberSellPick(lots: List<SellLot>, shown: Set<String>? = null, initial: Set<String> = emptySet()): SellPick {
    val state = remember { mutableStateOf(SellSelection(initial)) }
    return remember(lots, shown) { SellPick(lots, shown?.let { ids -> lots.filter { it.id in ids } } ?: lots, state) }
}

/** Быстрые наборы над списком: «по правилам автопродажи» и по редкости; включённый снимается тем же нажатием. */
@Composable fun SellPresetRow(pick: SellPick, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SellPreset.ALL.forEach { preset ->
            val (label, tint) = when (preset) {
                SellPreset.Rules -> ui("sell.preset_rules") to GoldBright
                is SellPreset.OfRarity -> ui("stash.rarity_many.${preset.rarity.name}") to rarityColor(preset.rarity.name)
            }
            PresetChip(label, on = pick.covers(preset), enabled = pick.offers(preset)) { pick.flip(preset) }
        }
    }
}

/** Чип набора: включённый - выбран. */
@Composable private fun PresetChip(label: String, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    ForgeChip(label, on, onClick, enabled = enabled)
}

/**
 * Строка списка в режиме выбора: без галочки сбоку - [content] (сама вещь) рисует отмеченную в золотой рамке, монета
 * в углу говорит, что она продастся; ширина строки не меняется. [content] получает, отмечена ли вещь, и переключатель.
 */
@Composable fun SellLine(pick: SellPick, id: String, content: @Composable (chosen: Boolean, onToggle: () -> Unit) -> Unit) {
    SaleMarked(pick.chosen(id)) { content(pick.chosen(id)) { pick.toggle(id) } }
}

/** Вещь [content] с монетой «продастся» в правом верхнем углу, когда она отмечена [marked]. */
@Composable fun SaleMarked(marked: Boolean, content: @Composable () -> Unit) {
    Box {
        content()
        if (marked) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(22.dp).background(Gold, CircleShape).border(1.dp, Ink.copy(alpha = .6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(ForgeGlyphs.Coins, ui("sell.marked"), tint = Ink, modifier = Modifier.size(13.dp)) }
        }
    }
}

/**
 * Кнопка снизу «Продать N · +◎ сумма», удерживаемая: продажа не отменяется. [onCancel] - «Отмена» рядом (режим тайника);
 * без отмеченного - подсказка вместо суммы, кнопка погашена. [verb] - ключ подписи с числом: в итогах захода - «Продать N и вернуться».
 *
 * Что продажа даст осколками - над кнопкой, до продажи. Среди отмеченного есть ценное ([SellLot.valuable]) - продажа ждёт
 * подтверждения со списком ценных вещей и их ценой ([ValuableConfirm]).
 */
@Composable fun SellDock(
    pick: SellPick,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    verb: String = "sell.do_n",
    shards: (List<SellLot>) -> String? = { null },
    onCancel: (() -> Unit)? = null,
    onSell: (List<String>) -> Unit,
) {
    val picked = pick.picked
    var asking by remember { mutableStateOf<List<SellLot>?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        shards(picked)?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally)) }
        SellRow(pick, picked, enabled, verb, onCancel) { ids ->
            val chosen = picked.filter { it.id in ids }
            if (chosen.any { it.valuable }) asking = chosen else onSell(ids)
        }
    }
    asking?.let { lots -> ValuableConfirm(lots, onDismiss = { asking = null }) { onSell(lots.map { it.id }) } }
}

/**
 * Подтверждение продажи с ценным: ценные вещи списком - имя в цвете редкости и цена торговца, затем сколько вещей уйдёт всего
 * и за сколько; удержание продаёт все отмеченные [lots].
 */
@Composable private fun ValuableConfirm(lots: List<SellLot>, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val valuable = lots.filter { it.valuable }
    ConfirmSheet(
        title = ui("sell.valuable_title"),
        confirm = ui("sell.valuable_confirm"),
        onDismiss = onDismiss,
        subtitle = ui("sell.valuable_sub", valuable.size, lots.size),
        ledger = valuable.map { LedgerLine(it.piece.title, "+" + number(it.price.toDouble()), Tone.GAIN) } + LedgerLine(ui("sell.valuable_total", lots.size), "+" + number(lots.sumOf { it.price }.toDouble()), Tone.GAIN),
        note = ui("sell.valuable_note"),
        danger = true,
        onConfirm = onConfirm,
    )
}

@Composable private fun SellRow(pick: SellPick, picked: List<SellLot>, enabled: Boolean, verb: String, onCancel: (() -> Unit)?, onSell: (List<String>) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        onCancel?.let { ForgeButton(onClick = it, kind = ButtonKind.Secondary) { Text(ui("common.cancel")) } }
        ForgeButton(
            onClick = { onSell(picked.map { it.id }) },
            modifier = Modifier.weight(1f),
            enabled = enabled && picked.isNotEmpty(),
            icon = ForgeGlyphs.Coins,
            hold = Hold(rearm = true),
        ) {
            Text(if (picked.isEmpty()) ui("sell.pick_hint") else ui(verb, picked.size), Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (picked.isNotEmpty()) ButtonFigure("+" + number(pick.total.toDouble()))
        }
    }
}
