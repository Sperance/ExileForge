package com.sperance.exileforge.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Spinner

@Composable fun CatalogFilters(s: ForgeState, vm: ForgeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text("Фильтры · ${if(expanded) "свернуть" else "показать"}") }
    if(!expanded) return
    val f = s.filter
    Spinner("Слот", f.slot, mapOf("" to "Все") + slots.associateWith { com.sperance.exileforge.core.display.slotTitle(it) }, !s.busy, { vm.filter(f.copy(slot = it)) })
    Spinner("Редкость", f.rarity, mapOf("" to "Все") + rarities.associateWith { com.sperance.exileforge.core.display.rarityTitle(it) }, !s.busy, { vm.filter(f.copy(rarity = it)) })
    OutlinedTextField(f.minLevel, { vm.filter(f.copy(minLevel = it)) }, label = { Text("Уровень от") }, singleLine = true)
    OutlinedTextField(f.maxLevel, { vm.filter(f.copy(maxLevel = it)) }, label = { Text("Уровень до") }, singleLine = true)
    Spinner("Свойство", f.stat, mapOf("" to "Любое", "damage_min" to "Минимальный урон", "damage_max" to "Максимальный урон", "defense" to "Защита", "attackSpeed" to "Скорость атаки"), !s.busy, { vm.filter(f.copy(stat = it, minStat = if(it.isBlank()) "" else f.minStat.ifBlank { "0" })) })
    if(f.stat.isNotBlank()) OutlinedTextField(f.minStat, { vm.filter(f.copy(minStat = it)) }, label = { Text("Минимальное значение") }, singleLine = true)
    OutlinedTextField(s.definitionQuery, vm::definitionQuery, label = { Text("Найти модификатор") }, singleLine = true)
    TextButton(enabled = !s.busy, onClick = { vm.loadDefinitions() }) { Text("Загрузить модификаторы") }
    Spinner("Модификатор", f.modifierId, mapOf("" to "Любой") + s.definitions.associate { it.text("id") to it.text("name") }, !s.busy, { vm.filter(f.copy(modifierId = it)) })
}
