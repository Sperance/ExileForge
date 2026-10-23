package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.key
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.SellLot
import com.sperance.exileforge.rules.content.AutoSell
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.ui.components.ButtonFigure
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.CheckMark
import com.sperance.exileforge.ui.components.ChipMode
import com.sperance.exileforge.ui.components.ChordOverWindow
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeChip
import com.sperance.exileforge.ui.components.ForgeIconButton
import com.sperance.exileforge.ui.components.Hold
import com.sperance.exileforge.ui.components.InfoMark
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.components.TabBadge
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.relicLook
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** Вкладка окна автопродажи: правила снаряжения или полки сумки. */
private enum class AutoSellTab(val key: String) {
    GEAR("autosell.tab.gear"),
    BAG("autosell.tab.bag"),
}

/**
 * Окно автопродажи и мест тайника (из фишки мест в заголовке списка) - отдельное окно по центру, вкладки «Снаряжение / Сумка» с
 * числом отметок каждой. «Снаряжение» - таблица групп мест × продаваемых редкостей с флажками в ячейках (касание шапки столбца
 * отмечает или снимает весь столбец), под ней правило «ниже героя на N уровней и более» - фишки «Выкл» и ступени контента
 * (`autoSell.belowSteps`), у включённого - редкости, что оно берёт, - и «не может носить» переключателем с пояснением в ⓘ. «Сумка» -
 * полки сумки ([AutoSell.SHELVES]) по флажку на полку: у стопок редкости нет, продаётся только новое из добычи. Ниже - что фильтр не
 * трогает никогда, «продать по правилам» то, что уже лежит в тайнике ([lots] с отметкой правил), одним удержанием и места тайника с
 * докупкой ([StashPlacesBar]). Весь текст переносится, а не режется. Читается из снимка на каждом проходе: отметка поворачивается,
 * как только сервер принял правило.
 */
@Composable
internal fun StashWindow(game: GameUi, rules: AutoSell, lots: List<SellLot>, model: HeroViewModel, onDismiss: () -> Unit) {
    val enabled = !game.busy
    val matching = lots.filter { it.byRules }
    val steps = game.index?.rules?.autoSell?.belowSteps.orEmpty()
    var tab by rememberSaveable { mutableStateOf(AutoSellTab.GEAR) }
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
            PillTabs(
                labels = AutoSellTab.entries.map { ui(it.key) },
                selected = tab.ordinal,
                onSelect = { tab = AutoSellTab.entries[it] },
                segmented = true,
                badges = listOf(TabBadge(rules.gearMarks.toString()), TabBadge(rules.shelves.size.toString())),
            )
            when (tab) {
                AutoSellTab.GEAR -> {
                    GearTable(rules, enabled, model)
                    BelowRule(rules, (steps + listOfNotNull(rules.below?.levels)).distinct().sorted(), enabled, model)
                    UnwearableRule(rules.unwearable, enabled) { model.autoSellUnwearable(it) }
                }

                AutoSellTab.BAG -> ShelfTable(rules, enabled, model)
            }
            Text(ui("autosell.never"), color = relicLook(Rarity.UNIQUE).name, style = MaterialTheme.typography.labelSmall)
            ForgeButton(
                onClick = { model.sellMany(matching.map { it.id }) },
                modifier = Modifier.fillMaxWidth(),
                state = ButtonState.of(enabled && matching.isNotEmpty()),
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

/** Рамка таблицы окна: шапка [header] и строки [rows] под чертой; [enabled] = false - всё серое, пока сервер отвечает. */
@Composable
private fun AutoSellTable(enabled: Boolean, header: @Composable RowScope.() -> Unit, rows: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().alpha(if (enabled) 1f else BUSY_ALPHA).background(Abyss, shape).border(1.dp, PanelRaised, shape).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically, content = header)
        HorizontalDivider(color = PanelRaised)
        rows()
    }
}

/** Имя группы в первом столбце таблицы: в полную ширину столбца, переносится. */
@Composable
private fun RowScope.GroupName(text: String, color: Color = Parchment) {
    Text(text, color = color, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(GROUP_WEIGHT).padding(vertical = 6.dp))
}

/**
 * Таблица снаряжения: строки - группы мест, столбцы - продаваемые редкости полными словами в их цвете; касание шапки столбца
 * отмечает все группы редкости, а если отмечены все - снимает их.
 */
@Composable
private fun GearTable(rules: AutoSell, enabled: Boolean, model: HeroViewModel) {
    val rarities = AutoSell.SELLABLE.sortedBy { it.ordinal }
    val every = SlotGroup.entries.toSet()
    AutoSellTable(
        enabled,
        header = {
            GroupName(ui("autosell.group"), Muted)
            rarities.forEach { rarity ->
                val all = rules.sell[rarity].orEmpty().containsAll(every)
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, role = Role.Button) { model.autoSell(rarity, if (all) emptySet() else every) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(ui("autosell.column.${rarity.name}"), color = rarityColor(rarity.name), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    MutedText(ui("autosell.all"), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
    ) {
        SlotGroup.entries.forEach { group ->
            Row(Modifier.fillMaxWidth().heightIn(min = ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                GroupName(ui("merchant.group.${group.name}"))
                rarities.forEach { rarity ->
                    val on = rules.sell[rarity].orEmpty()
                    val checked = group in on
                    MarkCell(checked, enabled, Modifier.weight(1f)) { model.autoSell(rarity, if (checked) on - group else on + group) }
                }
            }
        }
    }
}

/** Таблица сумки: полки, что фильтр может продавать ([AutoSell.SHELVES]), по флажку на полку; под ней - что продаётся только новое. */
@Composable
private fun ShelfTable(rules: AutoSell, enabled: Boolean, model: HeroViewModel) {
    AutoSellTable(
        enabled,
        header = {
            GroupName(ui("autosell.group"), Muted)
            Text(ui("autosell.sell_shelf"), color = Muted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        },
    ) {
        AutoSell.SHELVES.forEach { shelf ->
            val checked = shelf in rules.shelves
            Row(Modifier.fillMaxWidth().heightIn(min = ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                GroupName(ui(shelf.key))
                MarkCell(checked, enabled, Modifier.weight(1f)) { model.autoSellShelves(if (checked) rules.shelves - shelf else rules.shelves + shelf) }
            }
        }
    }
    MutedText(ui("autosell.bag_note"), style = MaterialTheme.typography.labelSmall)
}

/** Ячейка-флажок таблицы: нажимается вся ячейка высотой строки. */
@Composable
private fun MarkCell(checked: Boolean, enabled: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    Box(
        modifier.heightIn(min = ROW_HEIGHT).clip(RoundedCornerShape(8.dp)).toggleable(checked, enabled = enabled, role = Role.Checkbox) { onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        CheckMark(checked)
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

/** Правило «не может носить»: название, пояснение - в ⓘ, переключатель справа. */
@Composable
private fun UnwearableRule(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val title = ui("autosell.unwearable")
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        InfoMark(title, { Tip(title, ui("autosell.unwearable_hint")) })
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

/** Наименьшая высота строки таблицы и шапки. */
private val ROW_HEIGHT = 40.dp

/** Доля ширины первого столбца (имена групп) против столбца редкости. */
private const val GROUP_WEIGHT = 1.4f

/** Прозрачность таблицы, пока сервер отвечает. */
private const val BUSY_ALPHA = .5f
