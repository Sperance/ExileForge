package com.sperance.exileforge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.*
import com.sperance.exileforge.core.*
import kotlinx.serialization.json.*

@Composable fun ForgeApp(vm: ForgeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(s.message) {
        s.message?.let { snackbar.showSnackbar(it, withDismissAction = true); vm.dismissMessage() }
    }
    BackHandler(s.editorOpen && !s.busy) { confirmDiscard = true }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                val labels = listOf("Предметы", "Кузница", "Проверки", "Сервер")
                val icons = listOf(Icons.Outlined.Inventory2, Icons.Outlined.Build, Icons.AutoMirrored.Outlined.FactCheck, Icons.Outlined.Dns)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = s.tab == index, onClick = { vm.tab(index) },
                        icon = { Icon(icons[index], contentDescription = null) }, label = { Text(label, fontSize = 11.sp) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, Ink)))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Diamond, null, tint = Gold, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("EXILE FORGE", style = MaterialTheme.typography.titleLarge, color = Gold)
                    Text("МАСТЕРСКАЯ ПРЕДМЕТОВ", fontSize = 9.sp, letterSpacing = 2.sp, color = Muted)
                }
                Text("API v1", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth()) else HorizontalDivider(color = Gold.copy(alpha = .25f))
            when (s.tab) {
                0 -> CatalogScreen(s, vm)
                1 -> EditorScreen(s, vm, onDelete = { confirmDelete = true }, onClose = { confirmDiscard = true })
                2 -> ChecksScreen(s, vm, logs)
                3 -> ServerScreen(s, vm)
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Удалить предмет?") },
        text = { Text("${s.original?.text("name")}\n${s.original?.entityId}\nУдаление на сервере необратимо.") },
        confirmButton = { TextButton(enabled = !s.busy, onClick = { confirmDelete = false; vm.delete() }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Закрыть редактор?") },
        text = { Text("Несохранённые изменения будут потеряны.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; vm.closeEditor() }) { Text("Закрыть") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Продолжить") } })
}

@Composable private fun CatalogSwitch(s: ForgeState, vm: ForgeViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Catalog.entries.forEach { catalog -> FilterChip(selected = s.catalog == catalog,
            enabled = !s.busy && !s.editorOpen, onClick = { vm.catalog(catalog) }, label = { Text(catalog.title) }) }
    }
}
@Composable private fun CatalogScreen(s: ForgeState, vm: ForgeViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Хранилище", style = MaterialTheme.typography.headlineLarge)
            Text("${s.total} предметов · страница ${s.page + 1} / ${maxOf(1, s.totalPages)}", color = Muted)
            CatalogSwitch(s, vm)
            if (s.editorOpen) Text("В кузнице открыт предмет. Закройте редактор для смены каталога.", color = Muted, fontSize = 12.sp)
        }
        item {
            OutlinedTextField(s.query, vm::query, label = { Text("Поиск на текущей странице") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(s.lookupId, vm::lookup, label = { Text("Получить по ID") }, modifier = Modifier.weight(1f), singleLine = true)
                IconButton(enabled = !s.busy && !s.editorOpen, onClick = { vm.open(s.lookupId.trim()) }) { Icon(Icons.Outlined.TravelExplore, "Получить по ID", tint = Gold) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !s.busy && !s.editorOpen, onClick = { vm.create() }) { Icon(Icons.Outlined.Add, null); Text("Создать") }
                OutlinedButton(enabled = !s.busy, onClick = { vm.refresh() }) { Text("Обновить") }
            }
        }
        val visible = s.items.filter { it.text("name").contains(s.query, true) || it.entityId.contains(s.query, true) }
        if (visible.isEmpty()) item { InfoCard(if (s.busy) "Загрузка…" else "Предметов нет", "Обновите список, измените поиск или создайте новый предмет.") }
        items(visible, key = { it.entityId }) { doc -> ItemCard(doc, enabled = !s.busy && !s.editorOpen) { vm.open(doc.entityId) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(enabled = !s.busy && s.page > 0, onClick = { vm.refresh(s.page - 1) }) { Text("Назад") }
                OutlinedButton(enabled = !s.busy && s.page + 1 < s.totalPages, onClick = { vm.refresh(s.page + 1) }) { Text("Далее") }
            }
        }
    }
}
@Composable fun ItemCard(doc: JsonObject, enabled: Boolean = true, onClick: () -> Unit = {}) {
    val color = rarityColor(doc.text("rarity"))
    OutlinedCard(onClick = onClick, enabled = enabled, border = BorderStroke(1.dp, color.copy(alpha = .55f)),
        shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = .08f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, color.copy(alpha = .3f))) {
                    Icon(if (doc["type"] == null) Icons.Outlined.Diamond else Icons.Outlined.Shield, null, tint = color, modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(doc.text("name"), style = MaterialTheme.typography.titleMedium, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOf(doc.text("rarity"), doc.text("slot"), doc.text("category")).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 11.sp)
                }
            }
            HorizontalDivider(color = color.copy(alpha = .2f))
            if (doc["itemLevel"] != null) Text("Уровень ${doc.text("itemLevel")}  ·  Цена ${doc.text("price")}", fontSize = 12.sp)
            else Text("Цена ${doc.text("price")}", fontSize = 12.sp)
            (doc["modifiers"] as? JsonArray)?.take(6)?.forEach { raw ->
                val mod = raw.jsonObject
                val values = (mod["values"] as? JsonArray).orEmpty().joinToString(" / ") { (it as? JsonObject)?.text("value").orEmpty() }
                Text("$values · ${mod.text("definitionId")} · ${mod.text("source")} [T${mod.text("tier")}]", color = Rune, fontSize = 12.sp)
            }
            if (doc.text("description").isNotBlank()) Text(doc.text("description"), color = Muted, fontFamily = FontFamily.Serif, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(doc.entityId, color = Muted.copy(alpha = .65f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}
@Composable private fun EditorScreen(s: ForgeState, vm: ForgeViewModel, onDelete: () -> Unit, onClose: () -> Unit) {
    if (!s.editorOpen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Кузница", style = MaterialTheme.typography.headlineLarge)
            InfoCard("Создайте своё наследие", "Выберите предмет в хранилище или создайте новый. Здесь можно изменять свойства и модификаторы.")
            CatalogSwitch(s, vm)
            if (s.catalog == Catalog.ITEMS) Button(enabled = !s.busy, onClick = { vm.create() }) { Text("Создать предмет") }
            else EquipmentKind.entries.forEach { kind ->
                OutlinedButton(enabled = !s.busy, onClick = { vm.create(kind) }, modifier = Modifier.fillMaxWidth()) {
                    Text(when(kind) { EquipmentKind.Weapon -> "Создать оружие"; EquipmentKind.Armor -> "Создать броню"; EquipmentKind.Accessory -> "Создать аксессуар" })
                }
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (s.original == null) "Новый предмет" else "Кузница", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                IconButton(enabled = !s.busy, onClick = onClose) { Icon(Icons.Outlined.Close, "Закрыть редактор") }
            }
            s.original?.let { SelectionContainer { Text(it.entityId, color = Muted, fontFamily = FontFamily.Monospace) } }
        }
        items(s.fields.keys.filter { it != "type" }.toList(), key = { it }) { key ->
            val options = when(key) { "rarity" -> rarities; "slot" -> slots; "weaponType" -> weapons; else -> null }
            if (options != null) Choice(fieldLabel(key), s.fields[key].orEmpty(), options, !s.busy) { vm.field(key, it) }
            else OutlinedTextField(s.fields[key].orEmpty(), { vm.field(key, it) }, label = { Text(fieldLabel(key)) }, enabled = !s.busy,
                modifier = Modifier.fillMaxWidth(), minLines = if (key == "description") 2 else 1,
                singleLine = key !in listOf("description", "modifierDefinitions", "modifierDefinitionsStock"))
        }
        if (s.catalog == Catalog.EQUIPMENT) {
            item {
                HorizontalDivider(color = Gold.copy(alpha = .3f))
                Text("Модификаторы", style = MaterialTheme.typography.titleLarge, color = Gold)
                Text("Изменения сохраняются вместе с предметом.", color = Muted, fontSize = 12.sp)
            }
            itemsIndexed(s.modifiers) { index, mod ->
                OutlinedCard(border = BorderStroke(1.dp, Rune.copy(alpha = .35f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Модификатор ${index + 1}", color = Rune)
                        OutlinedTextField(mod.json, { vm.modifier(index, mod.copy(json = it)) },
                            label = { Text("Модификатор (JSON)") }, enabled = !s.busy,
                            supportingText = { Text("definitionId, values: [{value: число}], tier, source, tags. Можно задать несколько значений и любые теги.") },
                            modifier = Modifier.fillMaxWidth(), minLines = 6)
                        IconButton(enabled = !s.busy, onClick = { vm.removeModifier(index) }) {
                            Icon(Icons.Outlined.DeleteOutline, "Удалить модификатор")
                        }
                    }
                }
            }
            item { OutlinedButton(enabled = !s.busy, onClick = vm::addModifier, modifier = Modifier.fillMaxWidth()) { Text("Добавить модификатор") } }
        }
        item { Button(enabled = !s.busy, onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(8.dp)); Text("Сохранить на сервере") } }
        if (s.original != null) item { TextButton(enabled = !s.busy, onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Удалить предмет", color = MaterialTheme.colorScheme.error) } }
    }
}
@Composable private fun ChecksScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>) {
    var confirmRun by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Испытания", style = MaterialTheme.typography.headlineLarge)
            CatalogSwitch(s, vm)
            InfoCard("Полный цикл CRUD", "Создать → получить → изменить${if (s.catalog == Catalog.EQUIPMENT) " → модифицировать" else ""} → удалить. После записей выполняется проверочный GET.")
        }
        item {
            Button(enabled = !s.busy, onClick = { confirmRun = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PlayArrow, null); Text("Запустить проверку") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !s.busy, onClick = vm::count) { Text("Проверить count") }
                TextButton(onClick = vm::clearLogs) { Text("Очистить журнал") }
            }
        }
        itemsIndexed(s.checks) { _, check ->
            InfoCard((if (check.passed) "✓ " else "✕ ") + check.label, check.detail, failure = !check.passed)
        }
        item { Text("Журнал запросов", style = MaterialTheme.typography.titleLarge, color = Gold) }
        if (logs.isEmpty()) item { Text("Здесь появятся запросы и ответы сервера.", color = Muted) }
        itemsIndexed(logs) { _, log -> LogCard(log) }
    }
    if (confirmRun) AlertDialog(onDismissRequest = { confirmRun = false }, title = { Text("Запустить CRUD-проверку?") },
        text = { Text("На сервере ${s.server} будет создан и удалён один тестовый объект в ${s.catalog.path}. Используйте тестовую базу данных.") },
        confirmButton = { TextButton(onClick = { confirmRun = false; vm.runChecks() }) { Text("Запустить") } },
        dismissButton = { TextButton(onClick = { confirmRun = false }) { Text("Отмена") } })
}
@Composable private fun LogCard(log: RequestLog) {
    var expanded by remember(log) { mutableStateOf(false) }
    OutlinedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${log.method}  ${log.status ?: "NETWORK"}  ·  ${log.elapsedMs} ms", color = if (log.ok) Gold else MaterialTheme.colorScheme.error)
            Text(log.path, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            if (expanded) SelectionContainer {
                Column {
                    if (log.request.isNotBlank()) Text("REQUEST\n${log.request}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("RESPONSE\n${log.response}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
                }
            }
        }
    }
}
@Composable private fun ServerScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Врата мира", style = MaterialTheme.typography.headlineLarge)
        Text("Подключение к ktor-bestgame", color = Muted)
        OutlinedTextField(s.serverDraft, vm::serverDraft, enabled = !s.busy, label = { Text("Адрес сервера") }, supportingText = { Text("Без /api/v1: https://example.com/") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(enabled = !s.busy && !s.editorOpen, onClick = vm::connect, modifier = Modifier.fillMaxWidth()) { Text("Сохранить и подключиться") }
        if (s.editorOpen) Text("Перед сменой сервера закройте редактор в кузнице.", color = Muted)
        OutlinedButton(enabled = !s.busy, onClick = vm::health, modifier = Modifier.fillMaxWidth()) { Text("Проверить /system/health") }
        InfoCard("Состояние сервера", s.health)
        InfoCard("Локальная разработка", "Эмулятор: http://10.0.2.2:8080/\nТелефон: IP компьютера в вашей Wi-Fi сети. HTTP разрешён в debug-сборке; release использует HTTPS.")
        InfoCard("Контракт сервера", "master · ${SERVER_COMMIT.take(12)}\nПредметы и экипировка. Модификаторы сохраняются через PUT; серверного маршрута случайного крафта пока нет.")
    }
}
@Composable private fun Choice(label: String, value: String, options: List<String>, enabled: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp)) {
            Column(Modifier.weight(1f)) { Text(label, fontSize = 11.sp, color = Muted); Text(value, fontSize = 12.sp) }
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option, fontSize = 12.sp) }, onClick = { expanded = false; onChange(option) }) }
        }
    }
}
@Composable private fun InfoCard(title: String, body: String, failure: Boolean = false) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, if (failure) MaterialTheme.colorScheme.error else Gold.copy(alpha = .3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = if (failure) MaterialTheme.colorScheme.error else Gold, style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Text(body, color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
private fun fieldLabel(key: String) = when(key) {
    "name" -> "Название"; "description" -> "Описание"; "category" -> "Категория"; "subCategory" -> "Подкатегория"
    "price" -> "Цена"; "rarity" -> "Редкость"; "itemLevel" -> "Уровень предмета"; "slot" -> "Слот"
    "damage_min" -> "Минимальный урон"; "damage_max" -> "Максимальный урон"; "attackSpeed" -> "Скорость атаки"
    "durability" -> "Прочность"; "defense" -> "Защита"; "weaponType" -> "Тип оружия"; "image" -> "URL изображения"
    "modifierDefinitions" -> "Доступные модификаторы (JSON-массив)"
    "modifierDefinitionsStock" -> "Встроенные модификаторы (JSON-массив)"
    else -> key
}
