package com.sperance.exileforge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.ui.components.LocalEntityPageLoader
import com.sperance.exileforge.ui.screens.catalog.CatalogScreen
import com.sperance.exileforge.ui.screens.checks.ChecksScreen
import com.sperance.exileforge.ui.screens.editor.EditorScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
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
    CompositionLocalProvider(LocalEntityPageLoader provides vm::referencePage) {
    key(s.server) {
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
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Удалить запись?") },
        text = { Text("${s.original?.text("name")}\n${s.original?.entityId}\nУдаление на сервере необратимо.") },
        confirmButton = { TextButton(enabled = !s.busy, onClick = { confirmDelete = false; vm.delete() }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Закрыть редактор?") },
        text = { Text("Несохранённые изменения будут потеряны.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; vm.closeEditor() }) { Text("Закрыть") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Продолжить") } })
    }
    }
}
