package com.sperance.exileforge.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Muted

@Composable internal fun CatalogScreen(s: ForgeState, vm: ForgeViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(if (s.catalog == Catalog.CHARACTERS) tr("Персонажи", "Characters") else tr("Хранилище", "Stash"),
                tr("${s.total} записей · страница ${s.page + 1} / ${maxOf(1, s.totalPages)}", "${s.total} records · page ${s.page + 1} / ${maxOf(1, s.totalPages)}"),
                if (s.catalog == Catalog.CHARACTERS) ForgeGlyphs.Exile else ForgeGlyphs.Stash)
            if (s.adminTools) CatalogSwitch(s, vm)
            if (s.editorOpen) Text(tr("В кузнице открыт предмет. Закройте редактор для смены каталога.", "An item is open in the forge. Close the editor to switch catalogues."), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            ForgePanel {
                Engraved(tr("Поиск", "Search"))
                OutlinedTextField(s.query, vm::query, label = { Text(tr("Поиск по всему каталогу", "Search the whole catalogue")) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (s.catalog == Catalog.EQUIPMENT) CatalogFilters(s, vm)
                Button(enabled = !s.busy && s.signedIn, onClick = vm::applyFilters, modifier = Modifier.fillMaxWidth()) { Text(tr("Найти", "Search")) }
                EntitySpinner(tr("Открыть запись", "Open a record"), "", when (s.catalog) {
                    Catalog.CHARACTERS -> EntitySource.CHARACTER
                    Catalog.EQUIPMENT -> EntitySource.EQUIPMENT
                    Catalog.ITEMS -> EntitySource.ITEM
                }, !s.busy && !s.editorOpen && s.signedIn, vm::open)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !s.busy && !s.editorOpen && s.canEdit, onClick = { vm.create() }) { Icon(Icons.Outlined.Add, null); Text(tr("Создать", "Create")) }
                    OutlinedButton(enabled = !s.busy, onClick = { vm.refresh() }) { Text(tr("Обновить", "Refresh")) }
                }
            }
        }
        val visible = s.items
        if (visible.isEmpty()) item { InfoCard(if (s.busy) tr("Загрузка…", "Loading…") else tr("Предметов нет", "No items"), tr("Обновите список, измените поиск или создайте новый предмет.", "Refresh the list, change the search or create a new item.")) }
        items(visible, key = { it.entityId }) { doc ->
            ItemCard(doc, enabled = !s.busy && !s.editorOpen, definitions = s.definitions) { vm.open(doc.entityId) }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(enabled = !s.busy && s.page > 0, onClick = { vm.refresh(s.page - 1) }) { Text(tr("Назад", "Back")) }
                OutlinedButton(enabled = !s.busy && s.page + 1 < s.totalPages, onClick = { vm.refresh(s.page + 1) }) { Text(tr("Далее", "Next")) }
            }
        }
    }
}
