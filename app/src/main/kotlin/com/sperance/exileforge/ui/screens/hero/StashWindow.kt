package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.SellLot
import com.sperance.exileforge.rules.content.AutoSell
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.ui.components.ButtonFigure
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ChipMode
import com.sperance.exileforge.ui.components.ChordOverWindow
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeChip
import com.sperance.exileforge.ui.components.ForgeIconButton
import com.sperance.exileforge.ui.components.Hold
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.relicLook
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Окно автопродажи и мест тайника (из фишки мест в заголовке списка) - отдельное окно по центру, «Три колонки»: под шапкой
 * три колонки редкостей - в шапке колонки переключатель «всё», ниже отметки шести групп мест; затем правило «ниже героя на N
 * уровней и более» - фишки «Выкл» и ступени контента (`autoSell.belowSteps`), у включённого - редкости, что оно берёт; «не может
 * носить» переключателем; что фильтр не трогает никогда; «продать по правилам» то, что уже лежит в тайнике ([lots] с отметкой
 * правил), одним удержанием; в подвале - места тайника и докупка ([StashPlacesBar]). Читается из снимка на каждом проходе:
 * отметка поворачивается, как только сервер принял правило.
 */
@Composable
internal fun StashWindow(game: GameUi, rules: AutoSell, lots: List<SellLot>, model: HeroViewModel, onDismiss: () -> Unit) {
    val busy = game.busy
    val matching = lots.filter { it.byRules }
    val steps = game.index?.rules?.autoSell?.belowSteps.orEmpty()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ChordOverWindow()
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 20.dp).widthIn(max = 480.dp).fillMaxWidth().depthPanel(shape).clip(shape)
                .verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(20.dp))
                Text(ui("autosell.title"), color = GoldBright, fontFamily = Display, fontSize = 16.sp, modifier = Modifier.weight(1f))
                ForgeIconButton(Icons.Outlined.Close, ui("common.close"), onClick = onDismiss)
            }
            MutedText(ui("autosell.note"), style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AutoSell.SELLABLE.sortedBy { it.ordinal }.forEach { rarity ->
                    RarityColumn(rarity, rules.sell[rarity].orEmpty(), enabled = !busy, Modifier.weight(1f).fillMaxHeight(), model)
                }
            }
            BelowRule(rules, (steps + listOfNotNull(rules.below?.levels)).distinct().sorted(), enabled = !busy, model)
            RuleSwitch(ui("autosell.unwearable"), ui("autosell.unwearable_hint"), rules.unwearable, enabled = !busy) { model.autoSellUnwearable(it) }
            Text(ui("autosell.never"), color = relicLook(Rarity.UNIQUE).name, style = MaterialTheme.typography.labelSmall)
            ForgeButton(
                onClick = { model.sellMany(matching.map { it.id }) },
                modifier = Modifier.fillMaxWidth(),
                state = ButtonState.of(!busy && matching.isNotEmpty()),
                icon = ForgeGlyphs.Coins,
                hold = Hold(),
            ) {
                Text(ui("autosell.sell_now", matching.size), Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (matching.isNotEmpty()) ButtonFigure("+" + compactNumber(matching.sumOf(SellLot::price)))
            }
            StashPlacesBar(game, model)
        }
    }
}

/** Колонка редкости: имя в её цвете и «всё» переключателем - каждая группа мест разом, ниже - отметки групп по одной. */
@Composable
private fun RarityColumn(rarity: Rarity, on: Set<SlotGroup>, enabled: Boolean, modifier: Modifier, model: HeroViewModel) {
    val every = SlotGroup.entries.toSet()
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.background(Abyss, shape).border(1.dp, PanelRaised, shape).padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(ui("autosell.column.${rarity.name}"), color = rarityColor(rarity.name), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            ForgeSwitch(on.containsAll(every), enabled, Modifier.scale(.8f)) { model.autoSell(rarity, if (it) every else emptySet()) }
        }
        HorizontalDivider(color = PanelRaised)
        SlotGroup.entries.forEach { group ->
            val checked = group in on
            Row(
                Modifier.fillMaxWidth().heightIn(min = 32.dp).clip(RoundedCornerShape(7.dp))
                    .toggleable(checked, enabled = enabled, role = Role.Checkbox) { model.autoSell(rarity, if (checked) on - group else on + group) }.padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(if (checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null, tint = if (checked) Gold else Muted, modifier = Modifier.size(16.dp))
                Text(ui("merchant.group.${group.name}"), color = if (checked) Parchment else Muted, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Правило «ниже героя на N ур. и более»: фишки «Выкл» и ступени [steps]; у включённого правила под ними - редкости, что оно
 * берёт. Включая правило, окно берёт его прежние редкости, а без них - все продаваемые.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BelowRule(rules: AutoSell, steps: List<Int>, enabled: Boolean, model: HeroViewModel) {
    val below = rules.below
    val rarities = below?.rarities ?: AutoSell.SELLABLE
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui("autosell.below_title"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StepChip(ui("autosell.below_off"), below == null, enabled) { model.autoSellBelow(0, emptySet()) }
            steps.forEach { levels -> StepChip(levels.toString(), below?.levels == levels, enabled) { model.autoSellBelow(levels, rarities) } }
        }
        if (below != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                MutedText(ui("autosell.below_for"), style = MaterialTheme.typography.labelSmall)
                AutoSell.SELLABLE.sortedBy { it.ordinal }.forEach { rarity ->
                    val on = rarity in below.rarities
                    StepChip(ui("autosell.column.${rarity.name}"), on, enabled, rarityColor(rarity.name)) {
                        model.autoSellBelow(below.levels, if (on) below.rarities - rarity else below.rarities + rarity)
                    }
                }
            }
        }
    }
}

/** Чип выбора порога или редкости автопродажи; [accent] - цвет редкости. */
@Composable
private fun StepChip(label: String, selected: Boolean, enabled: Boolean, accent: Color? = null, onClick: () -> Unit) {
    ForgeChip(label, selected, onClick, state = ButtonState.of(enabled), accent = accent, mode = ChipMode.Choice)
}

/** Строка правила: название, подпись мелко и переключатель справа. */
@Composable
private fun RuleSwitch(title: String, hint: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            MutedText(hint, style = MaterialTheme.typography.labelSmall)
        }
        ForgeSwitch(checked, enabled, onChange = onChange)
    }
}

/** Переключатель окна в цветах игры. */
@Composable
private fun ForgeSwitch(checked: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink, uncheckedTrackColor = PanelRaised, uncheckedBorderColor = Bronze),
    )
}
