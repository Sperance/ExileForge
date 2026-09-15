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
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun ChecksScreen(s: ForgeState, vm: ForgeViewModel, logs: List<RequestLog>) {
    var confirmRun by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(tr("Испытания", "Trials"), tr("Сценарии контракта и журнал запросов", "Contract scenarios and the request journal"), ForgeGlyphs.Scroll)
            CatalogSwitch(s, vm)
            if(s.catalog == Catalog.CHARACTERS) Text(tr("Автосценарий доступен для предметов. Персонажа можно изменить через каталог и редактор.", "The scenario runs on items. Characters are edited through the catalogue and the editor."), color = Muted)
            InfoCard(tr("Полный цикл CRUD", "Full CRUD cycle"),
                tr("Создать → получить → изменить${if (s.catalog == Catalog.EQUIPMENT) " → модифицировать" else ""} → удалить. После записей выполняется проверочный GET.",
                   "Create → get → update${if (s.catalog == Catalog.EQUIPMENT) " → modify" else ""} → delete. Every write is verified with a GET."))
        }
        item {
            ForgePanel {
                Button(enabled = !s.busy && s.isAdmin && s.catalog != Catalog.CHARACTERS, onClick = { confirmRun = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PlayArrow, null); Text(tr("Запустить проверку", "Run the check")) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !s.busy, onClick = vm::count) { Text(tr("Проверить count", "Check count")) }
                    TextButton(onClick = vm::clearLogs) { Text(tr("Очистить журнал", "Clear the journal")) }
                }
            }
        }
        itemsIndexed(s.checks) { _, check ->
            InfoCard((if (check.passed) "✓ " else "✕ ") + check.label, check.detail, failure = !check.passed)
        }
        item {
            Text(tr("Журнал запросов", "Request journal").uppercase(), style = MaterialTheme.typography.titleLarge, color = Gold)
            OrnateDivider()
        }
        if (logs.isEmpty()) item { Text(tr("Здесь появятся запросы и ответы сервера.", "Requests and server responses will appear here."), color = Muted) }
        itemsIndexed(logs) { _, log -> LogCard(log) }
    }
    if (confirmRun) AlertDialog(onDismissRequest = { confirmRun = false }, containerColor = MaterialTheme.colorScheme.surface, titleContentColor = Gold,
        title = { Text(tr("Запустить CRUD-проверку?", "Run the CRUD check?")) },
        text = { Text(tr("На сервере ${s.server} будет создан и удалён один тестовый объект в ${s.catalog.path}. Используйте тестовую базу данных.",
                         "One test object will be created and deleted in ${s.catalog.path} on ${s.server}. Use a test database.")) },
        confirmButton = { TextButton(onClick = { confirmRun = false; vm.runChecks() }) { Text(tr("Запустить", "Run")) } },
        dismissButton = { TextButton(onClick = { confirmRun = false }) { Text(tr("Отмена", "Cancel")) } })
}
