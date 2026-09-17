package com.sperance.exileforge.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.forms.ObjectForm
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun EditorScreen(s: ForgeState, vm: ForgeViewModel, onDelete: () -> Unit, onClose: () -> Unit) {
    if (!s.editorOpen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader(tr("Кузница и персонажи", "Forge and characters"), tr("Мастерская изгнанника", "The exile's workshop"), ForgeGlyphs.Anvil)
            InfoCard(tr("Мастерская", "Workshop"), tr("Настройте свойства предмета или создайте новую базу. Экипировка персонажа и сферы доступны во вкладке «Герой».", "Tune an item's properties or create a new base. Gear and orbs live on the Hero tab."))
            InfoCard(tr("Редактор", "Editor"), tr("Выберите предмет или персонажа в каталоге. Характеристики, модификаторы и условия настраиваются через формы.", "Pick an item or a character in the catalogue. Stats, modifiers and conditions are edited through forms."))
            if(s.adminTools) CatalogSwitch(s, vm)
            ForgePanel {
                if (s.catalog != Catalog.EQUIPMENT) Button(enabled = !s.busy && s.canEdit && s.mergeReview == null, onClick = { vm.create() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if(s.catalog == Catalog.CHARACTERS) tr("Создать персонажа", "Create a character") else tr("Создать предмет", "Create an item"))
                }
                else EquipmentKind.entries.forEach { kind ->
                    OutlinedButton(enabled = !s.busy && s.canEdit && s.mergeReview == null, onClick = { vm.create(kind) }, modifier = Modifier.fillMaxWidth()) {
                        Text(when(kind) {
                            EquipmentKind.Weapon -> tr("Создать оружие", "Create a weapon")
                            EquipmentKind.Armor -> tr("Создать броню", "Create armour")
                            EquipmentKind.Accessory -> tr("Создать аксессуар", "Create an accessory")
                        })
                    }
                }
                OutlinedButton(enabled = !s.busy, onClick = vm::randomItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(ForgeGlyphs.Orb, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Получить случайный предмет", "Roll a random item"))
                }
            }
        }
        return
    }
    key(s.catalog, s.original?.entityId) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ScreenHeader(if(s.catalog == Catalog.CHARACTERS) tr("Персонаж", "Character") else tr("Кузница", "Forge"), icon = ForgeGlyphs.Anvil) }
                    IconButton(enabled = !s.busy, onClick = onClose) { Icon(Icons.Outlined.Close, tr("Закрыть редактор", "Close the editor")) }
                }
                if(s.catalog != Catalog.CHARACTERS) ItemCard(s.draft, enabled = false, detailed = true, definitions = s.definitions, actionLabel = tr("Предпросмотр", "Preview"))
                if(s.catalog == Catalog.CHARACTERS && s.original != null) OutlinedButton(enabled = !s.busy, onClick = { vm.showCharacterInventory(s.original.entityId) }) { Text(tr("Просмотреть экипировку", "View the equipment")) }
                if(s.catalog == Catalog.CHARACTERS && s.original == null) Text(tr("Владельцем станет текущий пользователь. Сервер проверит лимит персонажей.", "The current user becomes the owner. The server checks the character limit."), color = Muted)
            }
            if(s.catalog == Catalog.EQUIPMENT && s.adminTools) item {
                ForgePanel {
                    Engraved(tr("Модификаторы", "Modifiers"))
                    OutlinedTextField(s.definitionQuery, vm::definitionQuery, label = { Text(tr("Поиск модификаторов на сервере", "Search modifiers on the server")) }, enabled = !s.busy, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(enabled = !s.busy, onClick = { vm.loadDefinitions() }) { Text(tr("Найти модификаторы", "Find modifiers")) }
                    DefinitionPublisher(s, vm)
                    Text(tr("Найдено: ${s.definitionTotal} · страница ${s.definitionPage + 1}", "Found: ${s.definitionTotal} · page ${s.definitionPage + 1}"), color = Muted)
                    Row {
                        TextButton(enabled = !s.busy && s.definitionPage > 0, onClick = { vm.loadDefinitions(s.definitionPage - 1) }) { Text(tr("Назад", "Back")) }
                        TextButton(enabled = !s.busy && (s.definitionPage + 1) * 50 < s.definitionTotal, onClick = { vm.loadDefinitions(s.definitionPage + 1) }) { Text(tr("Далее", "Next")) }
                    }
                }
            }
            item {
                ObjectForm(formSchema(s.catalog), s.draft, s.definitions, !s.busy && s.canEdit && s.mergeReview == null,
                    locked = if(s.catalog == Catalog.CHARACTERS && s.original != null) setOf("userId") else emptySet(), onChange = vm::edit)
            }
            if(s.conflict) item { com.sperance.exileforge.ui.screens.editor.conflict.ConflictReview(s, vm) }
            item { Button(enabled = !s.busy && s.canEdit && !s.conflict, onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text(tr("Сохранить на сервере", "Save on the server")) } }
            if(s.original != null && s.canEdit) item { TextButton(enabled = !s.busy && !s.conflict, onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error) } }
        }
    }
}
