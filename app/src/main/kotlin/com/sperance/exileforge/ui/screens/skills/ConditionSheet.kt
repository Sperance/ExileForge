package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.ConditionText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.SlotAilment
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.SlotConditionGroup
import com.sperance.exileforge.rules.content.SlotConditionKind
import com.sperance.exileforge.rules.content.SlotParam
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ChipMode
import com.sperance.exileforge.ui.components.ChipSize
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeChip
import com.sperance.exileforge.ui.components.ForgeIconButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.icons.ConditionGlyphs
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.AbyssGlow
import com.sperance.exileforge.ui.theme.Acid
import com.sperance.exileforge.ui.theme.Boon
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.EmberText
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.ManaBlue
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.ShieldCyan
import com.sperance.exileforge.ui.theme.Vital
import com.sperance.exileforge.ui.theme.rarityColor
import kotlin.math.roundToInt

/** Цвет вида условия: знак и итог в ячейке, окантовка выбранной плитки, полоса параметра. */
internal fun conditionTint(kind: SlotConditionKind): Color = when (kind) {
    SlotConditionKind.READY -> Gold
    SlotConditionKind.MANUAL -> Quiet
    SlotConditionKind.LIFE, SlotConditionKind.BURST -> LossText
    SlotConditionKind.MANA -> ManaBlue
    SlotConditionKind.SHIELD -> ShieldCyan
    SlotConditionKind.AILING -> Acid
    SlotConditionKind.NO_EFFECT -> Vital
    SlotConditionKind.ENEMIES -> Brass
    SlotConditionKind.TARGET -> EmberText
    SlotConditionKind.RARITY -> Boon
    SlotConditionKind.CAST -> AbyssGlow
    SlotConditionKind.TIME -> Rune
}

/** Цвет недуга в выборе «Под недугом». */
private fun ailmentTint(ailment: SlotAilment): Color = when (ailment) {
    SlotAilment.BURN -> EmberText
    SlotAilment.COLD -> Rune
    SlotAilment.SHOCK -> Caution
    SlotAilment.POISON -> Acid
    SlotAilment.BLEED -> LossText
}

/** Условие кратко со знаком вида его цветом: ячейка Гримуара и место пояса. */
@Composable internal fun ConditionLine(condition: SlotCondition, modifier: Modifier = Modifier) {
    val tint = conditionTint(condition.kind)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(ConditionGlyphs.of(condition.kind), null, tint = tint, modifier = Modifier.size(10.dp))
        Text(ConditionText.short(condition), color = Rune, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Лист условия слота умения или места пояса: шапка - знак [icon], [title] и [subtitle]; виды плитками в две колонки по группам «Герой /
 * Враги / Бой» (вид только для фляг - лишь у фляги, [flask]); под ними параметр выбранного вида - полоса с «−/+», ряд 1–5, чипы
 * редкости или недугов, у вида без числа - пояснение. Выбор нового вида ставит его число по умолчанию, возврат к виду [current] - его
 * прежнее. Сохраняет кнопка подвала, на ней итог; [onSwap] - «Заменить умение» ячейки.
 */
@Composable
internal fun ConditionSheet(
    title: String,
    subtitle: String,
    current: SlotCondition,
    flask: Boolean,
    icon: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onSwap: (() -> Unit)? = null,
    onSave: (SlotCondition) -> Unit,
) {
    var draft by remember(current) { mutableStateOf(current) }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 10.dp),
        header = {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                icon()
                Column(Modifier.weight(1f)) {
                    Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        },
        footer = {
            ForgeButton(onClick = { onSave(draft) }, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), size = ButtonSize.Wide) {
                Text(ui("skills.condition.save", ConditionText.short(draft)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
    ) {
        Kinds(draft.kind, flask) { kind -> draft = if (kind == current.kind) current else SlotCondition.of(kind) }
        Parameter(draft) { draft = it }
        onSwap?.let { swap ->
            ForgeButton(onClick = swap, kind = ButtonKind.Text, accent = Rune) { Text(ui("skills.swap_skill")) }
        }
    }
}

/** Виды условия группами, плитки в две колонки; выбранный [chosen] - в окантовке и тоне своего цвета. */
@Composable private fun Kinds(chosen: SlotConditionKind, flask: Boolean, onPick: (SlotConditionKind) -> Unit) {
    SlotConditionGroup.entries.forEach { group ->
        val kinds = SlotConditionKind.entries.filter { it.group == group && (flask || !it.flaskOnly) }
        if (kinds.isEmpty()) return@forEach
        Caption(ConditionText.group(group))
        Column(verticalArrangement = Arrangement.spacedBy(KIND_GAP)) {
            kinds.chunked(KIND_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KIND_GAP)) {
                    row.forEach { kind -> KindTile(kind, kind == chosen, Modifier.weight(1f)) { onPick(kind) } }
                    repeat(KIND_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Плитка вида: знак его цветом и имя; у вида только для фляг - пометка под именем. */
@Composable private fun KindTile(kind: SlotConditionKind, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = conditionTint(kind)
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.heightIn(min = KIND_HEIGHT).clip(shape).background(if (on) tint.copy(alpha = .14f) else Abyss, shape)
            .border(if (on) 2.dp else 1.dp, if (on) tint else Bronze, shape)
            .selectable(on, role = Role.RadioButton, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(ConditionGlyphs.of(kind), null, tint = tint, modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(ConditionText.kind(kind), color = if (on) GoldBright else Parchment, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (kind.flaskOnly) Text(ui("skills.condition.flask_only"), color = Muted, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1)
        }
    }
}

/** Параметр условия [condition] по виду его числа; правка отдаётся [onChange] новым условием. */
@Composable private fun Parameter(condition: SlotCondition, onChange: (SlotCondition) -> Unit) {
    val kind = condition.kind
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ConditionText.note(kind), color = Muted, style = MaterialTheme.typography.labelMedium)
        when (kind.param) {
            SlotParam.PERCENT, SlotParam.SECONDS -> Threshold(condition, onChange)
            SlotParam.COUNT -> Choices(kind.range.toList(), condition.value, { it.toString() }, { conditionTint(kind) }) { onChange(condition.with(it)) }
            SlotParam.RARITY -> Choices(kind.range.toList(), condition.value, { ConditionText.rarity(MonsterRarity.entries[it]) }, { rarityColor(MonsterRarity.entries[it].name) }) { onChange(condition.with(it)) }
            SlotParam.AILMENTS -> Ailments(condition.ailments) { onChange(condition.with(it)) }
            SlotParam.NONE -> Unit
        }
    }
}

/** Порог числом: крупно значение, «−/+» с повтором при удержании и полоса пределов вида с подписями краёв и середины. */
@Composable private fun Threshold(condition: SlotCondition, onChange: (SlotCondition) -> Unit) {
    val kind = condition.kind
    val tint = conditionTint(kind)
    val range = kind.range
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ConditionText.value(condition), color = tint, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        ForgeIconButton(ForgeGlyphs.Minus, ui("skills.condition.less"), onClick = { onChange(condition.with(condition.value - 1)) }, kind = ButtonKind.Secondary, repeatOnHold = true)
        ForgeIconButton(ForgeGlyphs.Plus, ui("skills.condition.more"), onClick = { onChange(condition.with(condition.value + 1)) }, kind = ButtonKind.Secondary, repeatOnHold = true)
    }
    Slider(
        value = condition.value.toFloat(),
        onValueChange = { onChange(condition.with(it.roundToInt())) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint, inactiveTrackColor = Bronze),
    )
    Row(Modifier.fillMaxWidth()) {
        listOf(range.first, (range.first + range.last) / 2, range.last).forEachIndexed { i, at ->
            Text(
                ConditionText.value(condition.with(at)),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                textAlign = when (i) {
                    0 -> TextAlign.Start
                    1 -> TextAlign.Center
                    else -> TextAlign.End
                },
            )
        }
    }
}

/** Выбор одного из [options] чипами в ряд: подпись [label], цвет [accent]; выбранный - [chosen]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(options: List<Int>, chosen: Int, label: (Int) -> String, accent: (Int) -> Color, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            ForgeChip(label(option), option == chosen, { onPick(option) }, mode = ChipMode.Choice, size = ChipSize.Dense, accent = accent(option))
        }
    }
}

/** Недуги флажками: снять последний нельзя - набор «Под недугом» не бывает пуст. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Ailments(chosen: Set<SlotAilment>, onChange: (Set<SlotAilment>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SlotAilment.entries.forEach { ailment ->
            val on = ailment in chosen
            ForgeChip(ConditionText.ailment(ailment), on, { if (!on) onChange(chosen + ailment) else if (chosen.size > 1) onChange(chosen - ailment) }, size = ChipSize.Dense, accent = ailmentTint(ailment))
        }
    }
}

/** Плитка вида: высота не меньше [KIND_HEIGHT], [KIND_COLUMNS] колонки с зазором [KIND_GAP]. */
private val KIND_HEIGHT = 40.dp
private val KIND_GAP = 6.dp
private const val KIND_COLUMNS = 2
