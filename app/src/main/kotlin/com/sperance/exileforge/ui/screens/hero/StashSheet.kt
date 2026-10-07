package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.SellLot
import com.sperance.exileforge.rules.content.AutoSell
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.HoldButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * Лист тайника (3.90.5, из фишки мест в заголовке списка): сверху места и их докупка ([places]) и как продать пачкой -
 * удержанием плитки, ниже автопродажа (3.47.0, сервер 1.45.0; макет «Продажа» В): по редкости - «всё» переключателем и фишки
 * групп мест, затем «всё, что герой не может носить». Отмеченное торговец забирает из добычи захода сразу; уникальная, с
 * влиянием, осквернённая, расколотая и запертая вещь не уходит никогда. Внизу - «продать сейчас по правилам» то, что уже
 * лежит в тайнике ([lots] с отметкой правил), одним удержанием. Читается из снимка на каждом проходе: переключатель
 * поворачивается, как только сервер принял правило.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun StashSheet(rules: AutoSell, lots: List<SellLot>, busy: Boolean, model: HeroViewModel, places: @Composable () -> Unit, onDismiss: () -> Unit) {
    val matching = lots.filter { it.byRules }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Engraved(ui("stash.sheet_title"))
            places()
            MutedText(ui("stash.hold_hint"))
            HorizontalDivider(color = PanelRaised)
            Engraved(ui("autosell.title"))
            MutedText(ui("autosell.note"))
            AutoSell.SELLABLE.sortedBy { it.ordinal }.forEach { rarity -> RarityRule(rarity, rules.sell[rarity].orEmpty(), enabled = !busy, model) }
            HorizontalDivider(color = PanelRaised)
            RuleSwitch(ui("autosell.unwearable"), ui("autosell.unwearable_hint"), rules.unwearable, enabled = !busy, tint = Parchment) { model.autoSellUnwearable(it) }
            Spacer(Modifier.height(4.dp))
            HoldButton(
                ui("autosell.sell_now", matching.size),
                Gold,
                Modifier.fillMaxWidth(),
                enabled = !busy && matching.isNotEmpty(),
                rearm = true,
                icon = ForgeGlyphs.Coins,
                figure = matching.takeIf { it.isNotEmpty() }?.let { "+" + number(it.sumOf(SellLot::price).toDouble()) },
            ) { model.sellMany(matching.map { it.id }) }
        }
    }
}

/** Правило одной редкости: «всё» переключателем - каждая группа мест разом, ниже - фишки групп по одной. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RarityRule(rarity: Rarity, on: Set<SlotGroup>, enabled: Boolean, model: HeroViewModel) {
    val every = SlotGroup.entries.toSet()
    RuleSwitch(ui("stash.rarity_many.${rarity.name}"), ui("autosell.all"), on.containsAll(every), enabled, rarityColor(rarity.name)) {
        model.autoSell(rarity, if (it) every else emptySet())
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SlotGroup.entries.forEach { group ->
            FilterChip(
                selected = group in on,
                enabled = enabled,
                onClick = { model.autoSell(rarity, if (group in on) on - group else on + group) },
                label = { Text(ui("merchant.group.${group.name}")) },
            )
        }
    }
}

/** Строка правила: название в цвете правила, подпись мелко и переключатель справа. */
@Composable private fun RuleSwitch(title: String, hint: String, checked: Boolean, enabled: Boolean, tint: Color, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = tint, style = MaterialTheme.typography.labelLarge)
            MutedText(hint, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink, uncheckedTrackColor = PanelRaised, uncheckedBorderColor = Bronze),
        )
    }
}
