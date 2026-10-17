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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.rangedLine
import com.sperance.exileforge.core.display.recipeText
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.presentation.forge.EngraveLine
import com.sperance.exileforge.presentation.forge.ForgeTarget
import com.sperance.exileforge.presentation.forge.OrbChoice
import com.sperance.exileforge.presentation.forge.Smithy
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** Полосы кузницы над навигацией: выбранная сфера, эссенция или строка верстака с удерживаемой кнопкой. */
/**
 * The chosen essence over the navigation, with the held button: a common item becomes rare, a rare one is rolled anew. Под именем - что гарантия даст именно этой вещи: «Даст T5 из 6: +(12–18)% к урону огнём» (`OrbApplier.essenceGuarantee`).
 */
@Composable internal fun EssenceBar(game: GameUi, code: String, view: ItemView, enabled: Boolean, accepted: (String) -> Boolean, onApply: (String, String) -> Unit) {
    val owned = game.bagAmount(code) ?: 0L
    val index = game.index
    val essence = index?.essence(code)?.takeIf { owned > 0 && accepted(code) }
    ForgeBar {
        if (index == null || essence == null) {
            Text(ui("forge.pick_essence"), color = Muted)
            return@ForgeBar
        }
        BarTitle(ForgeGlyphs.Shard, Elder, itemTitle(code), stock(code, owned, 1))
        val gives = remember(essence, view.item, index) {
            OrbApplier(index).essenceGuarantee(essence, view.item, view.template)?.let { roll ->
                index.modifier(roll.code)?.let { def -> def.tier(roll.tier)?.let { tier -> ui("forge.essence_gives", roll.tier, def.tiers.size, rangedLine(index, def.code, tier.values)) } }
            }
        }
        gives?.let { Text(it, color = Elder, style = MaterialTheme.typography.bodySmall) }
        val fee = game.goldBill(index.rules.craftFee.essence(view.item.level(view.template)))
        FeeChip(fee)
        HoldButton(fee.paying(ui("confirm.hold", ui("forge.apply_essence"))), Elder, enabled = enabled && !view.item.corrupted && fee.covered, rearm = true) {
            onApply(view.item.id, code)
        }
    }
}

/**
 * The chosen orb over the navigation: what it does, what the bag keeps, and the button that is held. Над любой целью
 * кузницы ([ForgeTarget]): [chosen] - сфера лотка, что ляжет на цель; нет её - полоса просит выбрать.
 *
 * Сфера качества, что сбросит качество другого вида, сперва спрашивает подтверждение. Сфера, что идёт только со знамением, ждёт его. Сбор
 * золотом - по уровню предмета копии, одна функция правил. Сфера гравёра тратится в листе выбора строки ([EngraveSheet]).
 */
@Composable internal fun OrbBar(
    game: GameUi,
    smithy: Smithy,
    target: ForgeTarget,
    chosen: OrbChoice?,
    enabled: Boolean,
    lost: Int,
    chanceUniques: List<String>,
    onPickLine: () -> Unit,
    onApply: (String) -> Unit,
) {
    var confirming by remember(target.id, chosen, smithy.omen) { mutableStateOf(false) }
    ForgeBar {
        if (chosen == null) {
            Text(ui("forge.pick_orb"), color = Muted)
            return@ForgeBar
        }
        val code = chosen.code
        val owned = game.bagAmount(code) ?: 0L
        val waiting = chosen.needsOmen && smithy.omen.isBlank()
        BarTitle(
            ForgeGlyphs.Orb,
            Gold,
            itemTitle(code) + smithy.omen.takeIf { it.isNotBlank() }?.let { " + ${itemTitle(it)}" }.orEmpty(),
            if (waiting) ui("forge.needs_omen") to true else stock(code, owned, 1),
            code = code,
        )
        val resets = code == Orb.QUALITY_ORB.name && lost > 0
        if (resets) Text(ui("forge.quality_reset", lost), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        if (code == Orb.ORB_OF_CHANCE.name && chanceUniques.isNotEmpty()) {
            Text(ui("forge.chance_uniques", chanceUniques.joinToString(", ") { equipmentTitle(it) }), color = Parchment, style = MaterialTheme.typography.bodySmall)
        }
        val fee = game.index?.let { index -> Orb.of(code)?.let { game.goldBill(index.rules.craftFee.orb(it, target.itemLevel)) } }
        FeeChip(fee)
        if (code == Orb.ENGRAVERS_ORB.name) {
            ForgeButton(onPickLine, Modifier.fillMaxWidth(), enabled = enabled && !target.corrupted && fee.covered) { Text(fee.paying(ui("forge.engrave_pick"))) }
            return@ForgeBar
        }
        HoldButton(fee.paying(ui("confirm.hold", ui("forge.apply_orb"))), Gold, enabled = enabled && !target.corrupted && !waiting && fee.covered, rearm = true) {
            if (resets) confirming = true else onApply(code)
        }
    }
    if (confirming && chosen != null) {
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
                ForgeTextButton(enabled = enabled, onClick = {
                    confirming = false
                    onApply(chosen.code)
                }) { Text(ui("forge.quality_reset_do")) }
            },
            dismissButton = { ForgeTextButton(onClick = { confirming = false }) { Text(ui("common.cancel")) } },
        )
    }
}

/**
 * Выбранная печать Атласа над навигацией: что она, сколько в сумке и удерживаемая кнопка; печать, что ляжет только со
 * знамением, ждёт его - одна она сервером отвергнута.
 */
@Composable internal fun SealBar(game: GameUi, smithy: Smithy, target: ForgeTarget, chosen: OrbChoice?, enabled: Boolean, onApply: (String) -> Unit) {
    ForgeBar {
        if (chosen == null) {
            Text(ui("forge.pick_seal"), color = Muted)
            return@ForgeBar
        }
        val code = chosen.code
        val waiting = chosen.needsOmen && smithy.omen.isBlank()
        BarTitle(
            ForgeGlyphs.Sigil,
            Gold,
            itemTitle(code) + smithy.omen.takeIf { it.isNotBlank() }?.let { " + ${itemTitle(it)}" }.orEmpty(),
            if (waiting) ui("forge.needs_omen") to true else stock(code, game.bagAmount(code) ?: 0L, 1),
            code = code,
        )
        HoldButton(ui("confirm.hold", ui("forge.apply_seal")), Gold, enabled = enabled && !target.corrupted && !waiting, rearm = true) { onApply(code) }
    }
}

/** The chosen bench line over the navigation, priced, with the same held button; рецепт, что не встанет, не предлагается. */
@Composable internal fun BenchBar(game: GameUi, vm: SmithyViewModel, item: ItemView, chosen: String, enabled: Boolean) {
    val index = game.index ?: return
    val hero = game.hero ?: return
    val instance = item.item
    val refusals = benchRefusals(index, hero, item)
    val recipe = game.bench.firstOrNull { it.code == chosen && it.code !in refusals }
    ForgeBar {
        when {
            chosen == UNCRAFT -> {
                val scouring = index.rules.bench.uncraftOrb
                val owned = game.bagAmount(scouring.name) ?: 0L
                BarTitle(ForgeGlyphs.Anvil, Crafted, ui("bench.remove"), stock(scouring.name, owned, 1))
                val fee = game.goldBill(index.rules.craftFee.uncraft(instance.level(item.template)))
                FeeChip(fee)
                HoldButton(fee.paying(ui("confirm.hold", ui("forge.remove_bench"))), Crafted, enabled = enabled && owned >= 1 && fee.covered, rearm = true) { vm.uncraft(instance.id) }
            }

            recipe != null -> {
                val owned = game.bagAmount(recipe.orb.name) ?: 0L
                BarTitle(ForgeGlyphs.Anvil, Crafted, recipeText(index, recipe), stock(recipe.orb.name, owned, recipe.amount))
                val fee = game.goldBill(index.rules.craftFee.bench(instance.level(item.template)))
                FeeChip(fee)
                HoldButton(fee.paying(ui("confirm.hold", ui("forge.apply_bench"))), Crafted, enabled = enabled && owned >= recipe.amount && fee.covered, rearm = true) { vm.craft(instance.id, recipe.code) }
            }

            else -> Text(ui("forge.pick_line"), color = Muted)
        }
    }
}

/**
 * Сбор золотом за действие кузницы: чип «цена / есть» ([GoldCost]) - цена считается функцией правил
 * `CraftFeeRules` по уровню предмета копии; сбора нет (печати, нулевой вес) - чипа нет.
 */
@Composable private fun FeeChip(fee: GoldBill?) {
    fee?.takeIf { it.price > 0 }?.let { GoldCost(it) }
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

@Composable internal fun ForgeBar(content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = Bronze)
    Column(
        Modifier.fillMaxWidth().background(Abyss).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Заголовок полосы: рисунок стопки [code] (сфера - своим стеклом), иначе значок [icon]; имя и что останется в сумке. */
@Composable internal fun BarTitle(icon: ImageVector, accent: Color, title: String, stock: Pair<String, Boolean>, code: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (code != null) {
            BagIcon(code, Modifier.size(34.dp), kind = ItemVisualKind.CURRENCY)
        } else {
            Icon(icon, null, tint = accent, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stock.first, color = if (stock.second) LifeRed else Muted, style = MaterialTheme.typography.labelMedium)
        }
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
            HoldButton(ui("forge.engrave_do"), Gold, Modifier.padding(top = 6.dp), enabled = enabled && chosen != null, rearm = true) {
                chosen?.let(onEngrave)
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
