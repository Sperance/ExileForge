package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Muted

/**
 * Administrator tools for one character's inventory.
 *
 * The headline action rolls an item: the client picks a template of the chosen rarity and category,
 * the server rolls its modifiers, their tiers and their values when the instance is created.
 */
@Composable fun AdminGrantPanel(s: ForgeState, vm: ForgeViewModel) {
    if (!s.adminTools || s.characterId.isBlank()) return
    var expanded by remember(s.characterId) { mutableStateOf(true) }
    val enabled = !s.busy && s.signedIn && s.characterId.isNotBlank()
    val any = tr("Любая", "Any")
    TextButton(onClick = { expanded = !expanded }) {
        Text(tr("Выдача предметов · ${if (expanded) "свернуть" else "показать"}", "Granting items · ${if (expanded) "hide" else "show"}"))
    }
    if (!expanded) return
    ForgePanel {
        Engraved(tr("Случайный предмет", "Random item"))
        Spinner(tr("Редкость", "Rarity"), s.grantRarity, mapOf("" to any) + rarities.associateWith { rarityTitle(it, s.lang) }, enabled, vm::grantRarity)
        Spinner(tr("Категория", "Category"), s.grantSlot, mapOf("" to any) + slots.associateWith { slotTitle(it, s.lang) }, enabled, vm::grantSlot)
        Button(enabled = enabled, onClick = vm::grantRandom, modifier = Modifier.fillMaxWidth()) {
            Icon(ForgeGlyphs.Anvil, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(tr("Получить предмет с рандомными роллами", "Roll an item with random modifiers"))
        }
        Text(tr("Сервер выбирает префиксы и суффиксы по редкости шаблона и роллит тир и значение каждого.",
                "The server picks prefixes and suffixes by the template's rarity and rolls a tier and a value for each."),
            color = Muted, style = MaterialTheme.typography.bodySmall)

        OrnateDivider()
        Engraved(tr("Конкретный шаблон", "A named template"))
        var equipmentId by remember(s.characterId) { mutableStateOf("") }
        EntitySpinner(tr("Выдать экипировку", "Grant equipment"), equipmentId, EntitySource.EQUIPMENT, enabled) { equipmentId = it }
        Button(enabled = enabled && equipmentId.isNotBlank(), onClick = { vm.grant(equipmentId) }) { Text(tr("Выдать выбранный предмет", "Grant the chosen item")) }

        OrnateDivider()
        Engraved(tr("Сферы на предметах", "Orbs on items"))
        Text(tr("Любая сфера сервера на любом предмете инвентаря — для проверки правил на живом сервере.",
                "Any of the server's orbs on any item of the inventory — for trying the rules against a live server."),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        AdminOrbPanel(s, vm)

        OrnateDivider()
        Engraved(tr("Простые предметы", "Stacking items"))
        var itemId by remember(s.characterId) { mutableStateOf("") }
        var amount by remember(s.characterId) { mutableStateOf("1") }
        EntitySpinner(tr("Предмет", "Item"), itemId, EntitySource.ITEM, enabled) { itemId = it }
        OutlinedTextField(amount, { amount = it }, label = { Text(tr("Добавить / списать количество", "Add / remove amount")) },
            supportingText = { Text(tr("Отрицательное число списывает предметы.", "A negative number removes items.")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = enabled && itemId.isNotBlank() && amount.toLongOrNull()?.let { it != 0L } == true,
            onClick = { vm.adjustItems(itemId, amount.toLong()) }) { Text(tr("Изменить количество", "Change the amount")) }
    }
}
