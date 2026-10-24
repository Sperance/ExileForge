package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.fullIfShortened
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.SellLot
import com.sperance.exileforge.core.market.SellPreset
import com.sperance.exileforge.core.pick.PickPreset
import com.sperance.exileforge.core.pick.PickSelection
import com.sperance.exileforge.core.pick.Pickable
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Выбор пачкой - одна абстракция на режим продажи тайника, окно конца захода и окно вклада в хранилище гильдии: текущие [lots] и
 * отмеченное поверх них. Отметить можно только лот - то, что уходит пачкой; ушедшее выпадает из выбора само, с новыми лотами. Быстрые
 * наборы берут только [shown] - лоты открытого пункта рейки, вкладки и фильтров; отмеченное в других пунктах остаётся.
 */
@Stable
class Pick<T : Pickable> internal constructor(val lots: List<T>, private val shown: List<T>, private val state: MutableState<PickSelection>) {
    private val byId = lots.associateBy { it.id }

    /** Отмеченные живые лоты - в порядке [lots]. */
    val picked: List<T> get() = state.value.picked(lots)

    fun sellable(id: String): Boolean = id in byId
    fun chosen(id: String): Boolean = id in byId && id in state.value.chosen

    fun toggle(id: String) {
        if (sellable(id)) state.value = state.value.toggle(id)
    }

    fun covers(preset: PickPreset<T>): Boolean = state.value.covers(shown, preset)
    fun offers(preset: PickPreset<T>): Boolean = shown.any(preset::takes)

    fun flip(preset: PickPreset<T>) {
        state.value = state.value.flip(shown, preset)
    }

    fun clear() {
        state.value = PickSelection()
    }

    /** Отмечает [ids], которых ещё нет в [seen], и запоминает их там ([PickSelection.mark]). */
    fun markNew(ids: Collection<String>, seen: MutableSet<String>) {
        state.value = state.value.mark(ids, seen)
        seen += ids
    }
}

/** Выбор продажи. */
typealias SellPick = Pick<SellLot>

/** Сумма отмеченного к продаже. */
val SellPick.total: Long get() = picked.sumOf { it.price }

/**
 * Выбор над [lots]: отмеченное живёт, пока жив вызывающий экран, и переживает смену лотов. Заранее отмечено [initial]
 * (помеченное к продаже посреди захода). [shown] - id на экране, по которым работают наборы; null - все лоты.
 */
@Composable fun <T : Pickable> rememberPick(lots: List<T>, shown: Set<String>? = null, initial: Set<String> = emptySet()): Pick<T> {
    val state = remember { mutableStateOf(PickSelection(initial)) }
    return remember(lots, shown) { Pick(lots, shown?.let { ids -> lots.filter { it.id in ids } } ?: lots, state) }
}

/** Выбор продажи над [lots] ([rememberPick]). */
@Composable fun rememberSellPick(lots: List<SellLot>, shown: Set<String>? = null, initial: Set<String> = emptySet()): SellPick = rememberPick(lots, shown, initial)

/** Быстрые наборы над списком [presets]: подпись и цвет набора - [look]; включённый снимается тем же нажатием. */
@Composable fun <T : Pickable, P : PickPreset<T>> PresetRow(pick: Pick<T>, presets: List<P>, modifier: Modifier = Modifier, look: (P) -> Pair<String, Color>) {
    val cue = rememberRowScrollCue()
    Row(modifier.fillMaxWidth().scrollCue(cue).horizontalScroll(cue.state), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        presets.forEach { preset ->
            val (label, tint) = look(preset)
            PresetChip(label, tint, on = pick.covers(preset), enabled = pick.offers(preset)) { pick.flip(preset) }
        }
    }
}

/** Быстрые наборы продажи: «по правилам автопродажи» и по редкости. */
@Composable fun SellPresetRow(pick: SellPick, modifier: Modifier = Modifier) {
    PresetRow(pick, SellPreset.ALL, modifier) { preset ->
        when (preset) {
            SellPreset.Rules -> ui("sell.preset_rules") to GoldBright
            is SellPreset.OfRarity -> ui("stash.rarity_many.${preset.rarity.name}") to rarityColor(preset.rarity.name)
        }
    }
}

/** Чип набора цветом набора [tint]: включённый - выбран. */
@Composable private fun PresetChip(label: String, tint: Color, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    ForgeChip(label, on, onClick, state = ButtonState.of(enabled), accent = tint)
}

/** Что станет с отмеченным в выборе пачкой - знак в углу отмеченной строки ([PickMarked]): продастся или ляжет в хранилище гильдии. */
enum class PickMark {
    SALE,
    DEPOSIT,
    ;

    /** Значок знака: монета продажи или сундук хранилища. */
    val icon: ImageVector get() = when (this) {
        SALE -> ForgeGlyphs.Coins
        DEPOSIT -> ForgeGlyphs.Stash
    }

    /** Знак для чтения с экрана. */
    val label: String get() = ui(if (this == SALE) "sell.marked" else "guild.deposit_marked")
}

/**
 * Строка списка в режиме выбора пачкой - одна для продажи и вклада в хранилище гильдии: без галочки сбоку - [content] (сама вещь
 * или стопка) рисует отмеченную в золотой рамке, знак [mark] в углу говорит, что с ней станет; ширина строки не меняется.
 * [content] получает, отмечена ли строка, и переключатель: касание отмечает, удержание открывает карточку - это решает [content].
 */
@Composable fun <T : Pickable> PickLine(
    pick: Pick<T>,
    id: String,
    mark: PickMark,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(chosen: Boolean, onToggle: () -> Unit) -> Unit,
) {
    val chosen = pick.chosen(id)
    PickMarked(chosen, mark, modifier) { content(chosen) { pick.toggle(id) } }
}

/** Строка [content] со знаком [mark] в правом верхнем углу, когда она отмечена [marked]. */
@Composable fun PickMarked(marked: Boolean, mark: PickMark, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier) {
        content()
        if (marked) CornerMark(mark.icon, mark.label, Gold, Modifier.align(Alignment.TopEnd))
    }
}

/** Круглый знак [icon] на заливке [ground] за правым верхним углом строки выбора; [label] - для чтения с экрана. */
@Composable fun CornerMark(icon: ImageVector, label: String, ground: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.offset(x = 4.dp, y = (-4).dp).size(22.dp).background(ground, CircleShape).border(1.dp, Ink.copy(alpha = .6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Ink, modifier = Modifier.size(13.dp)) }
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
        ledger = valuable.map { gainLine(it.piece.title, it.price) } + gainLine(ui("sell.valuable_total", lots.size), lots.sumOf { it.price }),
        note = ui("sell.valuable_note"),
        danger = true,
        onConfirm = onConfirm,
    )
}

/** Строка выручки золотом [gold]: «+150к» короткой записью, сокращённое - с полным числом по касанию. */
private fun gainLine(label: String, gold: Long): LedgerLine = LedgerLine(label, "+" + compactNumber(gold), Tone.GAIN, fullIfShortened(gold)?.let { "+$it" })

@Composable private fun SellRow(pick: SellPick, picked: List<SellLot>, enabled: Boolean, verb: String, onCancel: (() -> Unit)?, onSell: (List<String>) -> Unit) {
    PickDock(
        label = if (picked.isEmpty()) ui("sell.pick_hint") else ui(verb, picked.size),
        icon = ForgeGlyphs.Coins,
        enabled = enabled && picked.isNotEmpty(),
        figure = ("+" + compactNumber(pick.total)).takeIf { picked.isNotEmpty() },
        onCancel = onCancel,
    ) { onSell(picked.map { it.id }) }
}

/**
 * Строка дока выбора пачкой: «Отмена» ([onCancel]) и удерживаемая кнопка действия [label] со значком [icon] и числом [figure]
 * справа; погашена, пока [enabled] нет. Продажа и вклад в хранилище гильдии - одна строка.
 */
@Composable fun PickDock(label: String, icon: ImageVector, enabled: Boolean, figure: String?, onCancel: (() -> Unit)?, modifier: Modifier = Modifier, onConfirm: () -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        onCancel?.let { ForgeButton(onClick = it, kind = ButtonKind.Secondary) { Text(ui("common.cancel")) } }
        ForgeButton(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            state = ButtonState.of(enabled),
            icon = icon,
            hold = Hold(),
        ) {
            Text(label, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            figure?.let { ButtonFigure(it) }
        }
    }
}
