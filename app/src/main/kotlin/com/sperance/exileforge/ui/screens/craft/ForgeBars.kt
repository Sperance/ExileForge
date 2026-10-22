package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.forge.BatchLimit
import com.sperance.exileforge.core.forge.EngraveLine
import com.sperance.exileforge.core.forge.ForgeTarget
import com.sperance.exileforge.core.forge.HiddenOrb
import com.sperance.exileforge.core.forge.OrbChoice
import com.sperance.exileforge.core.forge.QualityBatch
import com.sperance.exileforge.core.forge.Smithy
import com.sperance.exileforge.core.forge.SmithyModel
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * Полоса эссенции над навигацией: рецепт, остаток, сбор и удерживаемая кнопка - обычный предмет станет редким, редкий перекатится.
 * Эссенция не выбрана, кончилась или не ляжет ([accepted]) - кнопка погашена, почему - говорит рецепт.
 */
@Composable internal fun EssenceBar(
    game: GameUi,
    recipe: Recipe,
    code: String,
    view: ItemView,
    enabled: Boolean,
    accepted: (String) -> Boolean,
    onApply: (String, String) -> Unit,
) {
    val index = game.index ?: return
    val owned = game.bagAmount(code) ?: 0L
    val ready = index.essence(code) != null && owned > 0 && accepted(code)
    ForgeBar(recipe, view.item.corrupted) {
        val fee = if (ready) game.goldBill(index.rules.craftFee.essence(view.item.level(view.template))) else null
        BarFacts(if (ready) stock(code, owned, 1) else null, fee)
        ForgeButton(
            onClick = { onApply(view.item.id, code) },
            modifier = Modifier.fillMaxWidth(),
            state = ButtonState.of(ready && enabled && !view.item.corrupted && fee.covered),
            accent = Elder,
            hold = Hold(),
        ) { Text(fee.paying(ui("confirm.hold", ui("forge.apply_essence")))) }
    }
}

/**
 * Полоса сфер над навигацией: рецепт, ряд сфер сумки, что лягут на цель ([choices], листается вбок; спрятанные - [hidden]), остаток,
 * сбор и удерживаемая кнопка. Над любой целью кузницы ([ForgeTarget]): [chosen] - сфера ряда, что ляжет на цель; не выбрана - кнопка
 * погашена; сфер нет - только рецепт и спрятанные.
 *
 * Сфера качества, что сбросит качество другого вида ([lost]), сперва спрашивает подтверждение. Сфера, что идёт только со знамением, ждёт его. Сбор
 * золотом - по уровню предмета копии, одна функция правил. Сфера гравёра тратится в листе выбора строки ([EngraveSheet]).
 *
 * Сфера, что правила дают пачкой ([batch], [QualityBatch.of]), идёт ей ([BatchRow]): число по умолчанию - наибольшее, кнопка удержания применяет их одним запросом и несёт
 * цену всей пачки ([QualityBatch.cost]) в золотой плашке ([ButtonGold]), остаток сумки и качество после - [QualityBatch.summary]; [onApply] получает сферу и число.
 */
@Composable internal fun OrbBar(
    game: GameUi,
    smithy: Smithy,
    target: ForgeTarget,
    recipe: Recipe,
    choices: List<OrbChoice>,
    hidden: List<HiddenOrb>,
    enabled: Boolean,
    lost: Int,
    batch: QualityBatch?,
    onSelect: (String) -> Unit,
    onPickLine: () -> Unit,
    onApply: (String, Int) -> Unit,
) {
    val chosen = choices.firstOrNull { it.code == smithy.orb }
    var confirming by remember(target.id, chosen, smithy.omen) { mutableStateOf(false) }
    val pack = batch?.takeIf { it.orb.name == chosen?.code && it.max > 0 }
    var picked by remember(target.id, smithy.omen, pack?.max) { mutableIntStateOf(pack?.max ?: 1) }
    val count = pack?.let { picked.coerceIn(1, it.max) } ?: 1
    ForgeBar(recipe, target.corrupted) {
        OrbStrip(game, smithy.orb, choices, hidden, onSelect)
        if (choices.isEmpty()) return@ForgeBar
        val code = chosen?.code
        val waiting = chosen?.needsOmen == true && smithy.omen.isBlank()
        val resets = code == Orb.QUALITY_ORB.name && lost > 0
        val fee = pack?.let { game.goldBill(it.cost(count)) } ?: code?.let(Orb::of)?.let { orb -> game.index?.let { index -> game.goldBill(index.rules.craftFee.orb(orb, target.itemLevel)) } }
        pack?.let { BatchRow(game, it, count) { n -> picked = n } }
        val left = pack?.let { it.summary(count) to false } ?: code?.let { stock(it, game.bagAmount(it) ?: 0L, 1L) }
        BarFacts(left, fee.takeIf { pack == null })
        val ready = code != null && enabled && !target.corrupted && fee.covered
        if (code == Orb.ENGRAVERS_ORB.name) {
            ForgeButton(onPickLine, Modifier.fillMaxWidth(), state = ButtonState.of(ready)) { Text(fee.paying(ui("forge.engrave_pick"))) }
            return@ForgeBar
        }
        ForgeButton(
            onClick = { if (resets) confirming = true else code?.let { onApply(it, count) } },
            modifier = Modifier.fillMaxWidth(),
            state = ButtonState.of(ready && !waiting),
            hold = Hold(),
        ) {
            if (pack == null) {
                Text(fee.paying(ui("confirm.hold", ui("forge.apply_orb"))))
            } else {
                Text(ui("confirm.hold", ui("forge.apply_orbs", orbsApplied(count))), Modifier.weight(1f, fill = false), maxLines = 2, overflow = TextOverflow.Ellipsis)
                fee?.takeIf { it.price > 0 }?.let { ButtonGold(it.price, Modifier.padding(start = 6.dp)) }
            }
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
                ForgeButton(state = ButtonState.of(enabled), onClick = {
                    confirming = false
                    onApply(chosen.code, count)
                }, kind = ButtonKind.Text) { Text(ui("forge.quality_reset_do")) }
            },
            dismissButton = { ForgeButton(onClick = { confirming = false }, kind = ButtonKind.Text) { Text(ui("common.cancel")) } },
        )
    }
}

/**
 * Строка пачки сфер качества: «Сколько сфер» − N + ([NumberStepper] от одной до [QualityBatch.max]) и чип «Максимум», под ней - что держит
 * предел, словами ([batchReason]); золото держит предел - строка цветом предупреждения со знаком «⚠».
 */
@Composable private fun BatchRow(game: GameUi, batch: QualityBatch, count: Int, onCount: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui("forge.batch_count"), color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        NumberStepper(count, 1..batch.max, enabled = true, less = ui("forge.batch_less"), more = ui("forge.batch_more"), onChange = onCount)
        ForgeChip(ui("forge.batch_max"), selected = count == batch.max, onClick = { onCount(batch.max) }, mode = ChipMode.Choice)
    }
    val warned = count == batch.max && batch.limit == BatchLimit.GOLD
    Text(
        (if (warned) "⚠ " else "") + batchReason(game, batch, count),
        color = if (warned) Caution else Muted,
        style = MaterialTheme.typography.labelMedium,
    )
}

/**
 * Что держит пачку сфер качества: выбрано меньше наибольшей - сколько можно; иначе предел - потолок качества («До предела 20% нужно 13
 * сфер»), золото («Золота 1 900 - хватит на 5 сфер из 13 нужных»), сферы или катализаторы в сумке.
 */
private fun batchReason(game: GameUi, batch: QualityBatch, count: Int): String = if (count < batch.max) {
    ui("forge.batch_why_less", batch.max)
} else {
    when (batch.limit) {
        BatchLimit.CAP -> ui("forge.batch_why_cap", batch.climb.cap, ui(pluralKey("forge.batch.need", batch.need), batch.need))
        BatchLimit.GOLD -> ui("forge.batch_why_gold", compactNumber(game.hero?.money ?: 0L), orbsApplied(batch.max), batch.need)
        BatchLimit.ORBS -> ui("forge.batch_why_orbs", ui(pluralKey("forge.batch.orbs", batch.max), batch.max))
        BatchLimit.OMENS -> ui("forge.batch_why_omens", batch.max)
    }
}

/** «5 сфер» в винительном - сколько сфер применит пачка. */
private fun orbsApplied(count: Int): String = ui(pluralKey("forge.batch.apply", count), count)

/**
 * Полоса печати Атласа над навигацией: рецепт, сколько в сумке и удерживаемая кнопка; печать, что ляжет только со знамением, ждёт его -
 * одна она сервером отвергнута. Не выбрана - кнопка погашена; печатей нет ([empty]) - только рецепт.
 */
@Composable internal fun SealBar(
    game: GameUi,
    smithy: Smithy,
    target: ForgeTarget,
    recipe: Recipe,
    chosen: OrbChoice?,
    empty: Boolean,
    enabled: Boolean,
    onApply: (String) -> Unit,
) {
    ForgeBar(recipe, target.corrupted) {
        if (empty) return@ForgeBar
        val code = chosen?.code
        val waiting = chosen?.needsOmen == true && smithy.omen.isBlank()
        BarFacts(code?.let { stock(it, game.bagAmount(it) ?: 0L, 1) }, null)
        ForgeButton(
            onClick = { code?.let(onApply) },
            modifier = Modifier.fillMaxWidth(),
            state = ButtonState.of(code != null && enabled && !target.corrupted && !waiting),
            hold = Hold(),
        ) { Text(ui("confirm.hold", ui("forge.apply_seal"))) }
    }
}

/**
 * Полоса верстака над навигацией: рецепт, цена строки и та же удерживаемая кнопка; рецепт, что не встанет, не предлагается. Строка не
 * выбрана - кнопка погашена.
 */
@Composable internal fun BenchBar(game: GameUi, vm: SmithyModel, recipe: Recipe, item: ItemView, chosen: String, enabled: Boolean) {
    val index = game.index ?: return
    val hero = game.hero ?: return
    val instance = item.item
    val refusals = benchVerdict(hero, item).refusals
    val line = game.bench.firstOrNull { it.code == chosen && it.code !in refusals }
    ForgeBar(recipe, instance.corrupted) {
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
                val owned = line?.let { game.bagAmount(it.orb.name) ?: 0L } ?: 0L
                val fee = line?.let { game.goldBill(index.rules.craftFee.bench(instance.level(item.template))) }
                BarFacts(line?.let { stock(it.orb.name, owned, it.amount) }, fee)
                ForgeButton(
                    onClick = { line?.let { vm.craft(instance.id, it.code) } },
                    modifier = Modifier.fillMaxWidth(),
                    state = ButtonState.of(line != null && enabled && owned >= line.amount && fee.covered),
                    accent = Crafted,
                    hold = Hold(),
                ) { Text(fee.paying(ui("confirm.hold", ui("forge.apply_bench")))) }
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
 * Рецепт кузницы - строка полосы над её содержимым, единственное место, что называет связку: что ляжет на цель ([tool]), знамение к
 * нему ([omen]) и итог одной фразой ([effect] цветом [tone]). Касание строки открывает выноску с описанием правил ([detail]); пустое
 * описание - строка не нажимается. Нет инструмента - [effect] просит выбрать или говорит, что выбирать нечего. [warnings] - красные
 * строки того, что действие снимет или сбросит.
 */
internal class Recipe(
    val tool: String?,
    val effect: String?,
    val tone: Color = Vital,
    val omen: String? = null,
    val detail: String = "",
    val warnings: List<String> = emptyList(),
)

/**
 * Полоса кузницы над навигацией: сверху рецепт ([RecipeLine]; осквернённая цель [corrupted] - причина отказа в красной рамке), под ним
 * содержимое раздела - ряд сфер, остаток в сумке, сбор золотом и удерживаемая кнопка. Наковальня наверху держит только предмет.
 */
@Composable internal fun ForgeBar(recipe: Recipe, corrupted: Boolean, content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = Bronze)
    Column(
        Modifier.fillMaxWidth().background(Abyss).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (corrupted) Refusal(ui("orb.corrupted")) else RecipeLine(recipe)
        content()
    }
}

/**
 * Строка рецепта: «инструмент + знамение · итог» одной фразой и под ней красные предупреждения со знаком «⚠»; с предупреждением -
 * в красной рамке. Описание правил - выноской ([TipCallout]) по касанию строки, значок «i» справа говорит, что оно есть. Ни
 * инструмента, ни итога - строки нет.
 */
@Composable private fun RecipeLine(recipe: Recipe) {
    val effect = recipe.effect?.takeIf { it.isNotBlank() }
    if (recipe.tool == null && effect == null && recipe.warnings.isEmpty()) return
    var open by remember(recipe.detail) { mutableStateOf(false) }
    val tappable = recipe.detail.isNotBlank()
    val warned = recipe.warnings.isNotEmpty()
    val shape = RoundedCornerShape(10.dp)
    Box {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(shape)
                .background(if (warned) LifeRed.copy(alpha = .07f).compositeOver(Panel) else Panel, shape)
                .border(1.dp, if (warned) LifeRed.copy(alpha = .5f) else Bronze, shape)
                .then(if (tappable) Modifier.clickable(role = Role.Button, onClickLabel = recipe.tool) { open = true } else Modifier)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(recipePhrase(recipe, effect), style = MaterialTheme.typography.bodySmall, color = Muted)
                recipe.warnings.forEach { warning ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(WARN_SIGN, color = LifeRed, style = MaterialTheme.typography.labelMedium)
                        Text(warning, color = LifeRed, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (tappable) Icon(Icons.Outlined.Info, null, tint = Quiet, modifier = Modifier.size(20.dp))
        }
        if (open) {
            TipCallout(recipe.tone, { open = false }) {
                recipe.tool?.let { Text(it, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                Text(recipe.detail, color = Parchment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Фраза рецепта: имя инструмента золотом, «+ знамение» цветом рун и итог цветом рецепта через « · »; без инструмента итог приглушён. */
private fun recipePhrase(recipe: Recipe, effect: String?): AnnotatedString = buildAnnotatedString {
    recipe.tool?.let { withStyle(SpanStyle(color = GoldBright, fontWeight = FontWeight.SemiBold)) { append(it) } }
    recipe.omen?.let { withStyle(SpanStyle(color = Rune)) { append(" + $it") } }
    effect?.let {
        if (recipe.tool != null) append(" · ")
        withStyle(SpanStyle(color = if (recipe.tool == null) Muted else recipe.tone)) { append(it) }
    }
}

/** Знак предупреждения рецепта. */
private const val WARN_SIGN = "⚠"

/** Отказ кузницы вместо рецепта: значок запрета и причина в красной рамке. */
@Composable private fun Refusal(text: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().background(LifeRed.copy(alpha = .08f), shape).border(1.dp, LifeRed.copy(alpha = .35f), shape).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Block, null, tint = LifeRed, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(text, color = LifeRed, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Строка фактов полосы: слева что останется в сумке ([stock], нехватка - красным; ничего не выбрано - пусто), справа сбор золотом
 * ([GoldCost]) - цена считается функцией правил `CraftFeeRules` по уровню предмета копии; сбора нет (печати, нулевой вес) - чипа нет.
 */
@Composable private fun BarFacts(stock: Pair<String, Boolean>?, fee: GoldBill?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stock?.first.orEmpty(), color = if (stock?.second == true) LifeRed else Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
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
