package com.sperance.exileforge.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.editor.formSchema
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.CatalogSwitch
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.forms.ObjectForm
import com.sperance.exileforge.ui.screens.inventory.InventoryForge
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun EditorScreen(s: ForgeState, vm: ForgeViewModel, onDelete: () -> Unit, onClose: () -> Unit) {
    if (!s.editorOpen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Кузница и персонажи", style = MaterialTheme.typography.headlineLarge)
            InventoryForge(s, vm)
            InfoCard("Редактор", "Выберите предмет или персонажа в каталоге. Характеристики, модификаторы и условия настраиваются через формы.")
            CatalogSwitch(s, vm)
            if (s.catalog != Catalog.EQUIPMENT) Button(enabled = !s.busy, onClick = { vm.create() }) { Text(if(s.catalog == Catalog.CHARACTERS) "Создать персонажа" else "Создать предмет") }
            else EquipmentKind.entries.forEach { kind ->
                OutlinedButton(enabled = !s.busy, onClick = { vm.create(kind) }, modifier = Modifier.fillMaxWidth()) {
                    Text(when(kind) { EquipmentKind.Weapon -> "Создать оружие"; EquipmentKind.Armor -> "Создать броню"; EquipmentKind.Accessory -> "Создать аксессуар" })
                }
            }
            OutlinedButton(enabled = !s.busy, onClick = vm::randomItem) { Text("Получить случайный предмет") }
        }
        return
    }
    key(s.catalog, s.original?.entityId) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if(s.catalog == Catalog.CHARACTERS) "Персонаж" else "Кузница", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                    IconButton(enabled = !s.busy, onClick = onClose) { Icon(Icons.Outlined.Close, "Закрыть редактор") }
                }
                s.original?.let { SelectionContainer { Text(it.entityId, color = Muted) } }
                if(s.catalog == Catalog.CHARACTERS && s.original == null) Text("Выберите существующего пользователя из списка. Сервер проверит лимит персонажей.", color = Muted)
            }
            if(s.catalog != Catalog.ITEMS) item {
                OutlinedTextField(s.definitionQuery, vm::definitionQuery, label = { Text("Поиск модификаторов на сервере") }, enabled = !s.busy)
                OutlinedButton(enabled = !s.busy, onClick = { vm.loadDefinitions() }) { Text("Найти модификаторы") }
                DefinitionPublisher(s, vm)
                Text("Найдено: ${s.definitionTotal} · страница ${s.definitionPage + 1}")
                Row {
                    TextButton(enabled = !s.busy && s.definitionPage > 0, onClick = { vm.loadDefinitions(s.definitionPage - 1) }) { Text("Назад") }
                    TextButton(enabled = !s.busy && (s.definitionPage + 1) * 50 < s.definitionTotal, onClick = { vm.loadDefinitions(s.definitionPage + 1) }) { Text("Далее") }
                }
            }
            item {
                ObjectForm(formSchema(s.catalog), s.draft, s.definitions, !s.busy,
                    locked = if(s.catalog == Catalog.CHARACTERS && s.original != null) setOf("userId", "equipments", "items") else emptySet(), onChange = vm::edit)
            }
            item { Button(enabled = !s.busy, onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text("Сохранить на сервере") } }
            if(s.original != null) item { TextButton(enabled = !s.busy, onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Удалить", color = MaterialTheme.colorScheme.error) } }
        }
    }
}
