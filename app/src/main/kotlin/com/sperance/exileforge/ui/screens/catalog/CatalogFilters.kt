package com.sperance.exileforge.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Spinner

@Composable fun CatalogFilters(s: ForgeState, vm: ForgeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text(tr("Фильтры · ${if(expanded) "свернуть" else "показать"}", "Filters · ${if(expanded) "hide" else "show"}")) }
    if(!expanded) return
    val f = s.filter
    val any = tr("Все", "All")
    Spinner(tr("Слот", "Slot"), f.slot, mapOf("" to any) + slots.associateWith { com.sperance.exileforge.core.display.slotTitle(it, s.lang) }, !s.busy, { vm.filter(f.copy(slot = it)) })
    Spinner(tr("Редкость", "Rarity"), f.rarity, mapOf("" to any) + rarities.associateWith { com.sperance.exileforge.core.display.rarityTitle(it, s.lang) }, !s.busy, { vm.filter(f.copy(rarity = it)) })
    OutlinedTextField(f.minLevel, { vm.filter(f.copy(minLevel = it)) }, label = { Text(tr("Уровень от", "Level from")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(f.maxLevel, { vm.filter(f.copy(maxLevel = it)) }, label = { Text(tr("Уровень до", "Level to")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spinner(tr("Свойство", "Property"), f.stat, mapOf("" to tr("Любое", "Any"),
        "damage_min" to tr("Минимальный урон", "Minimum damage"), "damage_max" to tr("Максимальный урон", "Maximum damage"),
        "defense" to tr("Защита", "Defence"), "attackSpeed" to tr("Скорость атаки", "Attack speed")), !s.busy,
        { vm.filter(f.copy(stat = it, minStat = if(it.isBlank()) "" else f.minStat.ifBlank { "0" })) })
    if(f.stat.isNotBlank()) OutlinedTextField(f.minStat, { vm.filter(f.copy(minStat = it)) }, label = { Text(tr("Минимальное значение", "Minimum value")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(s.definitionQuery, vm::definitionQuery, label = { Text(tr("Найти модификатор", "Find a modifier")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    TextButton(enabled = !s.busy, onClick = { vm.loadDefinitions() }) { Text(tr("Загрузить модификаторы", "Load modifiers")) }
    Spinner(tr("Модификатор", "Modifier"), f.modifierId, mapOf("" to tr("Любой", "Any")) + s.definitions.associate { it.text("id") to it.text("name") }, !s.busy, { vm.filter(f.copy(modifierId = it)) })
}
