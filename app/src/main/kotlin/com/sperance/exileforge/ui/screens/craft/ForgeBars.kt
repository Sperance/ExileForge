package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.forge.EngraveLine
import com.sperance.exileforge.core.forge.ForgeTarget
import com.sperance.exileforge.core.forge.OrbChoice
import com.sperance.exileforge.core.forge.Smithy
import com.sperance.exileforge.core.forge.SmithyModel
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** The chosen essence over the navigation, with the held button: a common item becomes rare, a rare one is rolled anew. */
@Composable internal fun EssenceBar(game: GameUi, code: String, view: ItemView, enabled: Boolean, accepted: (String) -> Boolean, onApply: (String, String) -> Unit) {
    val owned = game.bagAmount(code) ?: 0L
    val index = game.index ?: return
    if (index.essence(code) == null || owned <= 0 || !accepted(code)) return
    ForgeBar {
        val fee = game.goldBill(index.rules.craftFee.essence(view.item.level(view.template)))
        BarFacts(stock(code, owned, 1), fee)
        ForgeButton(onClick = { onApply(view.item.id, code) }, modifier = Modifier.fillMaxWidth(), state = ButtonState.of(enabled && !view.item.corrupted && fee.covered), accent = Elder, hold = Hold()) {
            Text(fee.paying(ui("confirm.hold", ui("forge.apply_essence"))))
        }
    }
}

/**
 * The chosen orb over the navigation: what the bag keeps, the fee and the button that is held. Над любой целью
 * кузницы ([ForgeTarget]): [chosen] - сфера лотка, что ляжет на цель; нет её - полосы нет.
 *
 * Сфера качества, что сбросит качество другого вида ([lost]), сперва спрашивает подтверждение. Сфера, что идёт только со знамением, ждёт его. Сбор
 * золотом - по уровню предмета копии, одна функция правил. Сфера гравёра тратится в листе выбора строки ([EngraveSheet]).
 */
@Composable internal fun OrbBar(
    game: GameUi,
    smithy: Smithy,
    target: ForgeTarget,
    chosen: OrbChoice?,
    enabled: Boolean,
    lost: Int,
    onPickLine: () -> Unit,
    onApply: (String) -> Unit,
) {
    var confirming by remember(target.id, chosen, smithy.omen) { mutableStateOf(false) }
    if (chosen == null) return
    ForgeBar {
        val code = chosen.code
        val waiting = chosen.needsOmen && smithy.omen.isBlank()
        val resets = code == Orb.QUALITY_ORB.name && lost > 0
        val fee = game.index?.let { index -> Orb.of(code)?.let { game.goldBill(index.rules.craftFee.orb(it, target.itemLevel)) } }
        BarFacts(stock(code, game.bagAmount(code) ?: 0L, 1), fee)
        if (code == Orb.ENGRAVERS_ORB.name) {
            ForgeButton(onPickLine, Modifier.fillMaxWidth(), state = ButtonState.of(enabled && !target.corrupted && fee.covered)) { Text(fee.paying(ui("forge.engrave_pick"))) }
            return@ForgeBar
        }
        ForgeButton(
            onClick = { if (resets) confirming = true else onApply(code) },
            modifier = Modifier.fillMaxWidth(),
            state = ButtonState.of(enabled && !target.corrupted && !waiting && fee.covered),
            hold = Hold(),
        ) { Text(fee.paying(ui("confirm.hold", ui("forge.apply_orb")))) }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = PanelRaised,
            shape = DialogShape,
            tonalElevation = 0.dp,
            title = {
                ChordOverWindow()
                Text(ui("forge.quality_reset_title"), color = Gold)
            },
            text = { Text(ui("forge.quality_reset_note", lost), color = Parchment) },
            confirmButton = {
                ForgeButton(state = ButtonState.of(enabled), onClick = {
                    confirming = false
                    onApply(chosen.code)
                }, kind = ButtonKind.Text) { Text(ui("forge.quality_reset_do")) }
            },
            dismissButton = { ForgeButton(onClick = { confirming = false }, kind = ButtonKind.Text) { Text(ui("common.cancel")) } },
        )
    }
}

/**
 * Выбранная печать Атласа над навигацией: сколько в сумке и удерживаемая кнопка; печать, что ляжет только со
 * знамением, ждёт его - одна она сервером отвергнута. Нет выбранной - полосы нет.
 */
@Composable internal fun SealBar(game: GameUi, smithy: Smithy, target: ForgeTarget, chosen: OrbChoice?, enabled: Boolean, onApply: (String) -> Unit) {
    if (chosen == null) return
    ForgeBar {
        val code = chosen.code
        val waiting = chosen.needsOmen && smithy.omen.isBlank()
        BarFacts(stock(code, game.bagAmount(code) ?: 0L, 1), null)
        ForgeButton(onClick = { onApply(code) }, modifier = Modifier.fillMaxWidth(), state = ButtonState.of(enabled && !target.corrupted && !waiting), hold = Hold()) {
            Text(ui("confirm.hold", ui("forge.apply_seal")))
        }
    }
}

/** The chosen bench line over the navigation, priced, with the same held button; рецепт, что не встанет, не предлагается. */
@Composable internal fun BenchBar(game: GameUi, vm: SmithyModel, item: ItemView, chosen: String, enabled: Boolean) {
    val index = game.index ?: return
    val hero = game.hero ?: return
    val instance = item.item
    val refusals = benchRefusals(index, hero, item)
    val recipe = game.bench.firstOrNull { it.code == chosen && it.code !in refusals }
    if (chosen != UNCRAFT && recipe == null) return
    ForgeBar {
        when {
            chosen == UNCRAFT -> {
                val scouring = index.rules.bench.uncraftOrb
                val owned = game.bagAmount(scouring.name) ?: 0L
                val fee = game.goldBill(index.rules.craftFee.uncraft(instance.level(item.template)))
                BarFacts(stock(scouring.name, owned, 1), fee)
                ForgeButton(onClick = { vm.uncraft(instance.id) }, modifier = Modifier.fillMaxWidth(), state = ButtonState.of(enabled && owned >= 1 && fee.covered), accent = Crafted, hold = Hold()) {
                    Text(fee.paying(ui("confirm.hold", ui("forge.remove_bench"))))
                }
            }

            else -> {
                val line = recipe ?: return@ForgeBar
                val owned = game.bagAmount(line.orb.name) ?: 0L
                val fee = game.goldBill(index.rules.craftFee.bench(instance.level(item.template)))
                BarFacts(stock(line.orb.name, owned, line.amount), fee)
                ForgeButton(onClick = { vm.craft(instance.id, line.code) }, modifier = Modifier.fillMaxWidth(), state = ButtonState.of(enabled && owned >= line.amount && fee.covered), accent = Crafted, hold = Hold()) {
                    Text(fee.paying(ui("confirm.hold", ui("forge.apply_bench"))))
                }
            }
        }
    }
}

/** Сбор покрыт: золота хватает или сбора нет. */
private val GoldBill?.covered: Boolean get() = this == null || affordable

/** Подпись удерживаемой кнопки со сбором: золота не хватает - недостача ([payLabel]), иначе [ready]. */
private fun GoldBill?.paying(ready: String): String = this?.payLabel(ready) ?: ready

/**
 * What the bag keeps after one use. It is printed only when the bag can pay; when it cannot, the
 * bar says so in red and the button stays off.
 * Нехватка - общей строкой «Не хватает: …».
 */
internal fun stock(code: String, have: Long, need: Long): Pair<String, Boolean> = Cost.item(code, need).shortfall({ have }, 0).text()?.let { it to true } ?: (ui("forge.orb_left", have, have - need) to false)

/**
 * Полоса кузницы над навигацией: только остаток в сумке, сбор золотом и удерживаемая кнопка. Что ляжет на вещь и что выйдет,
 * называет рецепт наковальни ([Recipe]); пока ничего не выбрано, полосы нет.
 */
@Composable internal fun ForgeBar(content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = Bronze)
    Column(
        Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/**
 * Строка фактов полосы: слева что останется в сумке ([stock], нехватка - красным), справа сбор золотом ([GoldCost]) - цена считается
 * функцией правил `CraftFeeRules` по уровню предмета копии; сбора нет (печати, нулевой вес) - чипа нет.
 */
@Composable private fun BarFacts(stock: Pair<String, Boolean>, fee: GoldBill?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stock.first, color = if (stock.second) LifeRed else Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        fee?.takeIf { it.price > 0 }?.let { GoldCost(it) }
    }
}

/**
 * Лист Сферы гравёра: «Какую строку гравировать?» - каждая строка
 * вещи [target] с вилкой своего тира; строка, что правила не примут, тусклая, с причиной ([ForgeTarget.engraving]). Выбранная - в
 * золотой рамке; удержание «Гравировать · 1 сфера» тратит сферу на неё ([onEngrave] - номер строки в `item.rolls`).
 */
@Composable internal fun EngraveSheet(game: GameUi, target: ForgeTarget, enabled: Boolean, onDismiss: () -> Unit, onEngrave: (Int) -> Unit) {
    val index = game.index ?: return
    val lines = remember(target, index) { target.engraving(index) }
    var chosen by remember(target.id) { mutableStateOf<Int?>(null) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(ui("forge.engrave_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line -> EngraveRow(line, line.line == chosen) { chosen = line.line } }
            ForgeButton(onClick = { chosen?.let(onEngrave) }, modifier = Modifier.padding(top = 6.dp).fillMaxWidth(), state = ButtonState.of(enabled && chosen != null), hold = Hold()) {
                Text(ui("forge.engrave_do"))
            }
        }
    }
}

/** Строка листа гравёра: текст и справа вилка тира; недоступная - тусклая, справа причина отказа. */
@Composable private fun EngraveRow(line: EngraveLine, selected: Boolean, onPick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val open = line.refusal == null
    Row(
        Modifier.fillMaxWidth().alpha(if (open) 1f else .5f).clip(shape).background(if (selected) Gold.copy(alpha = .10f) else Panel, shape)
            .border(1.dp, if (selected) Gold else PanelRaised, shape)
            .then(if (open) Modifier.clickable(role = Role.RadioButton, onClick = onPick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(line.text, color = if (open) ModBlue else Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        (line.refusal ?: line.range)?.let { Text(it, color = if (open) Parchment else LifeRed, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp)) }
    }
}
