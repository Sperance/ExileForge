package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CoreAttribute
import com.sperance.exileforge.rules.content.SmithCategory
import com.sperance.exileforge.rules.content.SmithChoice
import com.sperance.exileforge.ui.components.PillTabs

/** Режим листа кузнеца (3.89.0): случайная вещь из одних слитков или выбранные группа и атрибут. */
internal enum class SmithMode(val label: String) {
    RANDOM("crafts.mode.random"),
    PICK("crafts.mode.pick"),
    ;

    companion object {
        fun of(choice: SmithChoice): SmithMode = if (choice == SmithChoice.RANDOM) RANDOM else PICK
    }
}

/**
 * Выбор кузнеца (3.89.0): переключатель «Случайно / Выбрать», а в «Выбрать» - группа (оружие, доспех, бижутерия) и
 * атрибут; у бижутерии атрибута нет. Пара сводится к [SmithChoice.of]; группа и атрибут помнятся, пока лист открыт,
 * так что «Случайно» и обратно возвращает прежний выбор.
 */
@Composable
internal fun SmithPicker(choice: SmithChoice, onChoice: (SmithChoice) -> Unit) {
    var lastCategory by remember { mutableStateOf(SmithCategory.WEAPON) }
    var lastAttribute by remember { mutableStateOf(CoreAttribute.STRENGTH) }
    LaunchedEffect(choice) {
        choice.category?.let { lastCategory = it }
        choice.attribute?.let { lastAttribute = it }
    }
    val category = choice.category ?: lastCategory
    val attribute = choice.attribute ?: lastAttribute
    val mode = SmithMode.of(choice)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PillTabs(SmithMode.entries.map { ui(it.label) }, mode.ordinal, { picked ->
            onChoice(if (SmithMode.entries[picked] == SmithMode.RANDOM) SmithChoice.RANDOM else SmithChoice.of(category, attribute))
        }, segmented = true)
        if (mode == SmithMode.PICK) {
            ChipRow(SmithCategory.entries, category, ::categoryTitle) { onChoice(SmithChoice.of(it, attribute)) }
            if (category.attributed) ChipRow(CoreAttribute.entries, attribute, ::attributeTitle) { onChoice(SmithChoice.of(category, it)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(values: List<T>, selected: T, title: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value -> FilterChip(selected = value == selected, onClick = { onPick(value) }, label = { Text(title(value)) }) }
    }
}

private fun categoryTitle(category: SmithCategory): String = when (category) {
    SmithCategory.WEAPON -> ui("merchant.group.WEAPON")
    SmithCategory.ARMOUR -> ui("merchant.group.ARMOUR")
    SmithCategory.JEWELLERY -> ui("merchant.group.JEWELLERY")
}

private fun attributeTitle(attribute: CoreAttribute): String = when (attribute) {
    CoreAttribute.STRENGTH -> ui("req.strength")
    CoreAttribute.DEXTERITY -> ui("req.dexterity")
    CoreAttribute.INTELLIGENCE -> ui("req.intelligence")
}
