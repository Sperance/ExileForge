package com.sperance.exileforge.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.*
import com.sperance.exileforge.core.*
import kotlinx.serialization.json.*

@Composable fun LoginForm(s: ForgeState, vm: ForgeViewModel) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    if(s.signedIn) {
        Text("Сессия активна")
        OutlinedButton(enabled = !s.busy, onClick = vm::logout) { Text("Выйти") }
    } else {
        OutlinedTextField(login, { login = it }, enabled = !s.busy, label = { Text("Логин") }, singleLine = true)
        OutlinedTextField(password, { password = it }, enabled = !s.busy, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Button(enabled = !s.busy && login.isNotBlank() && password.isNotEmpty(), onClick = { vm.login(login, password); password = "" }) { Text("Войти") }
    }
}

@Composable fun InventoryForge(s: ForgeState, vm: ForgeViewModel) {
    var confirmCurrency by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Экипировка персонажа", style = MaterialTheme.typography.titleLarge)
            if(!s.signedIn) Text("Для выпадения и крафта войдите во вкладке «Сервер».")
            OutlinedTextField(s.characterId, vm::characterId, enabled = !s.busy && s.pending == null, label = { Text("ID своего персонажа") }, singleLine = true)
            OutlinedButton(enabled = !s.busy && s.signedIn, onClick = vm::loadInventory) { Text("Загрузить инвентарь") }
            Button(enabled = !s.busy && s.signedIn && s.inventoryVersion != null && s.pending == null, onClick = { vm.inventoryAction("drop") }) { Text("Получить случайный предмет") }
            Text("Тестовое выпадение доступно администратору для собственного персонажа.", color = Muted)
            Spinner("Экземпляр", s.selectedEquipment, s.inventory.associate { item ->
                val poe = item["poe"] as? JsonObject
                item.text("uuid") to "${poe?.text("rarity").orEmpty()} ${poe?.text("baseId") ?: item.text("equipmentId")} · ${item.text("uuid")}" 
            }, !s.busy, vm::selectEquipment)
            val item = s.inventory.firstOrNull { it.text("uuid") == s.selectedEquipment }
            val poe = item?.get("poe") as? JsonObject
            if(poe != null) {
                Text("Уровень ${poe.text("itemLevel")} · качество ${poe.text("quality")}")
                listOf("implicits", "explicits").forEach { key ->
                    (poe[key] as? JsonArray).orEmpty().forEach { raw ->
                        val roll = raw.jsonObject
                        Text("${roll.text("id")} · v${roll.text("revision").ifBlank { "1" }}: ${(roll["values"] as? JsonArray).orEmpty().joinToString { it.jsonPrimitive.content }}${if(roll.text("fractured") == "true") " · fractured" else ""}", color = Rune)
                    }
                }
            } else if(item != null) Text("Старый экземпляр без состояния PoE: крафт сфер недоступен.")
            Spinner("Сфера", s.selectedCurrency, s.currencies.associate { it.text("id") to it.text("name") }, !s.busy, vm::selectCurrency)
            Button(enabled = !s.busy && s.signedIn && poe != null && s.selectedCurrency.isNotBlank() && s.inventoryVersion != null && s.pending == null, onClick = { confirmCurrency = true }) { Text("Применить сферу") }
            if(s.pending != null) {
                Text("Результат запроса не подтверждён. Повторите тот же запрос; сервер защитит от повторного списания.")
                OutlinedButton(enabled = !s.busy && s.signedIn, onClick = vm::retryInventoryAction) { Text("Повторить запрос") }
            }
        }
    }
    if(confirmCurrency) AlertDialog(onDismissRequest = { confirmCurrency = false }, title = { Text("Применить сферу?") }, text = { Text("Будет потрачена одна ${s.selectedCurrency} на выбранный экземпляр. Модификаторы могут измениться или удалиться.") }, confirmButton = { TextButton(onClick = { confirmCurrency = false; vm.inventoryAction("craft") }) { Text("Применить") } }, dismissButton = { TextButton(onClick = { confirmCurrency = false }) { Text("Отмена") } })
}

@Composable fun DefinitionPublisher(s: ForgeState, vm: ForgeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(defaultObject("definition")) }
    var revision by remember { mutableStateOf(0) }
    OutlinedButton(onClick = { expanded = !expanded }) { Text("Каталог: создать / изменить определение") }
    if(expanded) {
        Text("Публикация доступна администратору. Изменение создаёт новую версию; старые предметы сохраняют прежнюю.")
        Spinner("Изменить пользовательское определение", "", s.definitions.filter { it["poe"] == null || it["poe"] == JsonNull }.associate { definitionKey(it) to "${it.text("name")} · v${it.text("revision").ifBlank { "1" }}" }, !s.busy) { key ->
            draft = s.definitions.first { definitionKey(it) == key }
            revision = draft.text("revision").toIntOrNull() ?: 1
        }
        OutlinedButton(enabled = !s.busy, onClick = { draft = defaultObject("definition"); revision = 0 }) { Text("Новое определение") }
        ObjectForm("definition", draft, s.definitions, !s.busy, locked = if(revision > 0) setOf("id") else emptySet(), onChange = { draft = it })
        Row {
            Text("Доступен для новых предметов")
            Switch(checked = draft.text("enabled") != "false", enabled = !s.busy, onCheckedChange = { draft = JsonObject(draft + ("enabled" to JsonPrimitive(it))) })
        }
        Button(enabled = s.signedIn && !s.busy, onClick = { vm.publishDefinition(draft, revision) }) { Text("Опубликовать версию ${revision + 1}") }
        Text("Исходные правила PoE импортируются на сервере. Здесь редактируются пользовательские определения через формы.", color = Muted)
    }
}
