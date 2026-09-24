package com.sperance.exileforge.ui.screens.checks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
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
            ScreenHeader(ui("checks.title"), ui("checks.subtitle"), ForgeGlyphs.Scroll)
            CatalogSwitch(s, vm)
            if(s.admin.catalog == Catalog.CHARACTERS) Text(ui("checks.items_note"), color = Muted)
            InfoCard(ui("checks.crud"),
                ui("checks.crud_note", if (s.admin.catalog == Catalog.EQUIPMENT) ui("checks.modify_step") else ""))
        }
        item {
            ForgePanel {
                Button(enabled = !s.busy && s.isAdmin && s.admin.catalog != Catalog.CHARACTERS, onClick = { confirmRun = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PlayArrow, null); Text(ui("checks.run_check")) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !s.busy, onClick = vm::count) { Text(ui("checks.check_count")) }
                    Text(ui("catalog.count", s.admin.total), color = Muted, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = vm::clearLogs) { Text(ui("checks.clear_journal")) }
                }
            }
        }
        itemsIndexed(s.admin.checks) { _, check ->
            InfoCard((if (check.passed) "✓ " else "✕ ") + check.label, check.detail, failure = !check.passed)
        }
        item {
            Text(ui("account.journal").uppercase(), style = MaterialTheme.typography.titleLarge, color = Gold)
            OrnateDivider()
        }
        if (logs.isEmpty()) item { Text(ui("account.journal_empty"), color = Muted) }
        itemsIndexed(logs) { _, log -> LogCard(log) }
    }
    if (confirmRun) AlertDialog(onDismissRequest = { confirmRun = false }, containerColor = MaterialTheme.colorScheme.surface, titleContentColor = Gold,
        title = { Text(ui("checks.run_q")) },
        text = { Text(ui("checks.run_text", s.account.server, s.admin.catalog.path)) },
        confirmButton = { TextButton(onClick = { confirmRun = false; vm.runChecks() }) { Text(ui("checks.run_do")) } },
        dismissButton = { TextButton(onClick = { confirmRun = false }) { Text(ui("common.cancel")) } })
}
