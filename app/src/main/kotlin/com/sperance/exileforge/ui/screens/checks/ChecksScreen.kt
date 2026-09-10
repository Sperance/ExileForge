package com.sperance.exileforge.ui.screens.checks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.CatalogSwitch
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun ChecksScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>) {
    var confirmRun by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Испытания", style = MaterialTheme.typography.headlineLarge)
            CatalogSwitch(s, vm)
            if(s.catalog == Catalog.CHARACTERS) Text("Автосценарий доступен для предметов. Персонажа можно изменить через каталог и редактор.", color = Muted)
            InfoCard("Полный цикл CRUD", "Создать → получить → изменить${if (s.catalog == Catalog.EQUIPMENT) " → модифицировать" else ""} → удалить. После записей выполняется проверочный GET.")
        }
        item {
            Button(enabled = !s.busy && s.catalog != Catalog.CHARACTERS, onClick = { confirmRun = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PlayArrow, null); Text("Запустить проверку") }
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
