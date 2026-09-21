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
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.statTitle
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Composable internal fun EditorScreen(s: ForgeState, vm: ForgeViewModel, onDelete: () -> Unit, onClose: () -> Unit) {
    if (!s.editorOpen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader(tr("Кузница и персонажи", "Forge and characters"), tr("Мастерская изгнанника", "The exile's workshop"), ForgeGlyphs.Anvil)
            InfoCard(tr("Мастерская", "Workshop"), tr("Настройте шаблон предмета или создайте новую базу. Экипировка персонажа — во вкладке «Герой».", "Tune an item template or create a new base. A character's gear lives on the Hero tab."))
            InfoCard(tr("Редактор", "Editor"), tr("Выберите предмет или персонажа в каталоге. Пул модификаторов задаёт, что сервер сможет выролить на экземпляре.", "Pick an item or a character in the catalogue. The modifier pool decides what the server may roll onto an instance."))
            if (s.adminTools) CatalogSwitch(s, vm)
            ForgePanel {
                if (s.catalog != Catalog.EQUIPMENT) Button(enabled = !s.busy && s.canEdit, onClick = { vm.create() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.catalog == Catalog.CHARACTERS) tr("Создать персонажа", "Create a character") else tr("Создать предмет", "Create an item"))
                }
                else EquipmentKind.entries.forEach { kind ->
                    OutlinedButton(enabled = !s.busy && s.canEdit, onClick = { vm.create(kind) }, modifier = Modifier.fillMaxWidth()) {
                        Text(when (kind) {
                            EquipmentKind.Weapon -> tr("Создать оружие", "Create a weapon")
                            EquipmentKind.Armor -> tr("Создать броню", "Create armour")
                            EquipmentKind.Accessory -> tr("Создать аксессуар", "Create an accessory")
                        })
                    }
                }
            }
        }
        return
    }
    key(s.catalog, s.original?.entityId) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ScreenHeader(if (s.catalog == Catalog.CHARACTERS) tr("Персонаж", "Character") else tr("Кузница", "Forge"), icon = ForgeGlyphs.Anvil) }
                    IconButton(enabled = !s.busy, onClick = onClose) { Icon(Icons.Outlined.Close, tr("Закрыть редактор", "Close the editor")) }
                }
                if (s.catalog != Catalog.CHARACTERS) ItemCard(s.draft, enabled = false, detailed = true, definitions = s.definitions, actionLabel = tr("Предпросмотр", "Preview"))
                if (s.catalog == Catalog.CHARACTERS && s.original == null) {
                    Text(tr("Владельцем станет текущий пользователь. Сервер проверит лимит персонажей.", "The current user becomes the owner. The server checks the character limit."), color = Muted)
                    // The class is the whole stat base and the way into the tree, and the server has
                    // no route to change it later: it is chosen here or nowhere.
                    Spinner(tr("Класс", "Class"), s.draft.text("classId"),
                        s.classes.associate { it.id to it.title }, !s.busy) { chosen ->
                        vm.draftClass(chosen); vm.edit(JsonObject(s.draft + ("classId" to JsonPrimitive(chosen))))
                    }
                    s.classes.firstOrNull { it.id == s.draft.text("classId") }?.let { chosen ->
                        Text(chosen.details, color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text(tr("База 1 уровня: ", "Level 1 base: ") + chosen.baseStats.joinToString(" · ") { "${statTitle(it.stat, s.lang)} ${it.value.toInt()}" },
                            color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (s.classes.isEmpty()) Text(tr("Сервер не вернул ни одного класса — персонажа создать нельзя.", "The server served no classes — a character cannot be created."), color = MaterialTheme.colorScheme.error)
                }
            }
            if (s.catalog == Catalog.EQUIPMENT && s.adminTools) item {
                ForgePanel {
                    Engraved(tr("Модификаторы", "Modifiers"))
                    Text(tr("Загружено описаний: ${s.definitions.size}", "Descriptions loaded: ${s.definitions.size}"), color = Muted)
                    OutlinedButton(enabled = !s.busy, onClick = vm::loadDefinitions) { Text(tr("Обновить каталог модификаторов", "Refresh the modifier catalogue")) }
                    Text(tr("В пул попадают ссылки на описания. Префиксы и суффиксы сервер роллит по редкости, остальные источники вешает всегда.",
                            "The pool holds references to descriptions. The server rolls prefixes and suffixes by rarity and applies the other sources to every copy."),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                // A player may rename their own character; only an administrator touches its stats.
                val locked = if (s.catalog == Catalog.CHARACTERS && !s.adminTools) setOf("professionSkills", "battleSkills", "boolSkills") else emptySet()
                ObjectForm(formSchema(s.catalog), s.draft, !s.busy && s.canEdit, locked = locked, onChange = vm::edit)
            }
            item { Button(enabled = !s.busy && s.canEdit, onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text(tr("Сохранить на сервере", "Save on the server")) } }
            if (s.original != null) item { OutlinedButton(enabled = !s.busy, onClick = vm::reloadEditor, modifier = Modifier.fillMaxWidth()) { Text(tr("Перечитать запись с сервера", "Re-read the record from the server")) } }
            if (s.original != null && s.canEdit) item { TextButton(enabled = !s.busy, onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error) } }
        }
    }
}
