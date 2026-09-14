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
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.ui.components.LocalEntityPageLoader
import com.sperance.exileforge.ui.screens.catalog.CatalogScreen
import com.sperance.exileforge.ui.screens.checks.ChecksScreen
import com.sperance.exileforge.ui.screens.editor.EditorScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.inventory.InventoryForge
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
    key(s.server, s.sessionEpoch) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                val destinations = if(s.adminTools) listOf(0 to "Каталог", 1 to "Редактор", 2 to "Проверки", 4 to "Герой", 3 to "Аккаунт") else listOf(0 to "Персонажи", 4 to "Герой", 5 to "Кузница", 6 to "Поход", 3 to "Аккаунт")
                val icons = mapOf(0 to Icons.Outlined.Inventory2, 1 to Icons.Outlined.Build, 2 to Icons.AutoMirrored.Outlined.FactCheck, 3 to Icons.Outlined.Dns, 4 to Icons.Outlined.PersonOutline, 5 to Icons.Outlined.Build, 6 to Icons.Outlined.Shield)
                destinations.forEach { (index, label) ->
                    NavigationBarItem(selected = s.tab == index, onClick = { vm.tab(index) }, icon = { Icon(icons.getValue(index), null) }, label = { Text(label, fontSize = 11.sp) })
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
                    Text("АРСЕНАЛ ИЗГНАННИКА", fontSize = 9.sp, letterSpacing = 2.sp, color = Muted)
                }
                if(s.isAdmin) TextButton(enabled = !s.busy && !s.editorOpen, onClick = { vm.mode(if(s.adminTools) AppMode.PLAYER else AppMode.ADMIN) }) { Text(if(s.adminTools) "Админ" else "Игрок") }
            }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth()) else HorizontalDivider(color = Gold.copy(alpha = .25f))
            when (s.tab) {
                0 -> CatalogScreen(s, vm)
                1 -> EditorScreen(s, vm, onDelete = { confirmDelete = true }, onClose = { confirmDiscard = true })
                2 -> ChecksScreen(s, vm, logs)
                3 -> ServerScreen(s, vm)
                4 -> InventoryForge(s, vm)
                5 -> InventoryForge(s, vm, forgeOnly = true)
                6 -> com.sperance.exileforge.ui.screens.combat.CombatScreen(s, vm)
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Удалить запись?") },
        text = { Text("${s.original?.text("name")}\n${s.original?.entityId}\nЗапись будет скрыта на сервере.") },
        confirmButton = { TextButton(enabled = !s.busy, onClick = { confirmDelete = false; vm.delete() }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Закрыть редактор?") },
        text = { Text("Несохранённые изменения будут потеряны.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; vm.closeEditor() }) { Text("Закрыть") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Продолжить") } })
    }
    }
}
