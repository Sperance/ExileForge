package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InventoryForge(s: ForgeState, vm: ForgeViewModel) {
    var detailId by remember(s.characterId) { mutableStateOf<String?>(null) }
    var confirmCurrency by remember { mutableStateOf(false) }
    var query by remember(s.characterId) { mutableStateOf("") }
    var slot by remember(s.characterId) { mutableStateOf("") }
    val documents = s.inventory.associate { it.text("uuid") to inventoryDocument(it, s.inventoryBases[it.text("equipmentId")]) }
    val slots = documents.values.map { it.text("slot") }.filter(String::isNotBlank).distinct()
    LazyVerticalGrid(columns = GridCells.Adaptive(300.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Арсенал героя", style = MaterialTheme.typography.headlineLarge)
                Text("${s.inventory.size} предметов · снаряжение в инвентаре", color = Muted)
                EntitySpinner("Персонаж", s.characterId, EntitySource.CHARACTER, !s.busy && s.pending == null, vm::characterId)
                if(!s.signedIn) InfoCard("Войдите в аккаунт", "Во вкладке «Сервер» войдите, чтобы посмотреть снаряжение своего персонажа и применять сферы.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !s.busy && s.signedIn && s.characterId.isNotBlank(), onClick = vm::loadInventory) { Icon(Icons.Outlined.Refresh, null); Text("Обновить") }
                    Button(enabled = !s.busy && s.signedIn && s.inventoryVersion != null && s.pending == null, onClick = { vm.inventoryAction("drop") }) { Icon(Icons.Outlined.AutoAwesome, null); Text("Новый дроп") }
                }
                Text("Тестовый дроп доступен администратору для своего персонажа.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(query, { query = it }, label = { Text("Найти предмет в арсенале") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = slot.isBlank(), onClick = { slot = "" }, label = { Text("Все") }) }
                    items(slots) { key -> FilterChip(selected = slot == key, onClick = { slot = key }, label = { Text(slotTitle(key)) }) }
                }
                if(s.pending != null) InfoCard("Ожидает подтверждения", "Повторите исходный запрос, чтобы узнать результат без повторного списания.")
                if(s.pending != null) OutlinedButton(enabled = !s.busy && s.signedIn, onClick = vm::retryInventoryAction) { Text("Подтвердить результат") }
            }
        }
        val visible = s.inventory.filter { instance -> documents[instance.text("uuid")]?.let { (slot.isBlank() || it.text("slot") == slot) && it.text("name").contains(query, true) } == true }
        if(visible.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { InfoCard(if(s.inventoryVersion == null) "Арсенал ещё не загружен" else "Ничего не найдено", "Выберите персонажа, загрузите экипировку или измените фильтры.") }
        items(visible, key = { it.text("uuid") }) { instance ->
            val id = instance.text("uuid")
            ItemCard(documents.getValue(id), enabled = !s.busy, selected = id == s.selectedEquipment,
                definitions = s.inventoryDefinitions, actionLabel = "Свойства и крафт") { detailId = id; vm.selectEquipment(id) }
        }
    }
    val instance = s.inventory.firstOrNull { it.text("uuid") == detailId }
    if(instance != null) ModalBottomSheet(onDismissRequest = { detailId = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ItemCard(documents.getValue(instance.text("uuid")), enabled = false, detailed = true, definitions = s.inventoryDefinitions, actionLabel = "Экземпляр · ${instance.text("uuid").takeLast(6)}") }
            item {
                Text("Крафт экземпляра", style = MaterialTheme.typography.titleLarge, color = Gold)
                Spinner("Сфера", s.selectedCurrency, s.currencies.associate { it.text("id") to it.text("name") }, !s.busy, vm::selectCurrency)
                Button(modifier = Modifier.fillMaxWidth(), enabled = !s.busy && s.signedIn && instance["poe"] is JsonObject && s.selectedCurrency.isNotBlank() && s.inventoryVersion != null && s.pending == null,
                    onClick = { confirmCurrency = true }) { Icon(Icons.Outlined.Build, null); Text("Применить сферу") }
                if(instance["poe"] !is JsonObject) Text("Этот экземпляр ещё не переведён в формат PoE.", color = Muted)
            }
            item {
                HorizontalDivider()
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !s.busy, onClick = { vm.editInventoryBase(instance.text("equipmentId")); detailId = null }) { Icon(Icons.Outlined.Edit, null); Text("Редактировать базу предмета") }
                Text("База — общий шаблон. Выпавшие значения этого экземпляра изменяются через крафт.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if(confirmCurrency) AlertDialog(onDismissRequest = { confirmCurrency = false }, title = { Text("Применить сферу?") },
        text = { Text("${s.currencies.firstOrNull { it.text("id") == s.selectedCurrency }?.text("name") ?: s.selectedCurrency}\nБудет потрачена одна сфера. Свойства выбранного экземпляра могут измениться или удалиться.") },
        confirmButton = { TextButton(onClick = { confirmCurrency = false; vm.inventoryAction("craft") }) { Text("Применить") } },
        dismissButton = { TextButton(onClick = { confirmCurrency = false }) { Text("Отмена") } })
}
