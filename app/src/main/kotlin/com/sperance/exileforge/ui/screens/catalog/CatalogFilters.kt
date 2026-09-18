package com.sperance.exileforge.ui.screens.catalog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.weapons
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Spinner
import com.sperance.exileforge.ui.theme.Muted

@Composable fun CatalogFilters(s: ForgeState, vm: ForgeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text(tr("Фильтры · ${if (expanded) "свернуть" else "показать"}", "Filters · ${if (expanded) "hide" else "show"}")) }
    if (!expanded) return
    val f = s.filter
    val any = tr("Все", "All")
    Spinner(tr("Слот", "Slot"), f.slot, mapOf("" to any) + slots.associateWith { slotTitle(it, s.lang) }, !s.busy) { vm.filter(f.copy(slot = it)) }
    Spinner(tr("Редкость", "Rarity"), f.rarity, mapOf("" to any) + rarities.associateWith { rarityTitle(it, s.lang) }, !s.busy) { vm.filter(f.copy(rarity = it)) }
    Spinner(tr("Тип оружия", "Weapon type"), f.weaponType, mapOf("" to any) + weapons.associateWith { weaponTitle(it, s.lang) }, !s.busy) { vm.filter(f.copy(weaponType = it)) }
    OutlinedTextField(f.minLevel, { vm.filter(f.copy(minLevel = it)) }, label = { Text(tr("Уровень от", "Level from")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(f.maxLevel, { vm.filter(f.copy(maxLevel = it)) }, label = { Text(tr("Уровень до", "Level to")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    TextButton(enabled = !s.busy, onClick = vm::loadDefinitions) { Text(tr("Загрузить модификаторы", "Load modifiers")) }
    Spinner(tr("Модификатор в пуле", "Modifier in the pool"), f.modifierId,
        mapOf("" to tr("Любой", "Any")) + s.definitions.associate { it.id to it.title }, !s.busy) { vm.filter(f.copy(modifierId = it)) }
    Text(tr("Сервер не умеет искать по каталогу, поэтому клиент читает коллекцию и фильтрует её сам.",
            "The server cannot search the catalogue, so the client reads the collection and filters it here."),
        color = Muted, style = MaterialTheme.typography.bodySmall)
}
