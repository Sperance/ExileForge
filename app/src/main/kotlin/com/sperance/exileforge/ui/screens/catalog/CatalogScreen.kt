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
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.CatalogSwitch
import com.sperance.exileforge.ui.components.EntitySpinner
import com.sperance.exileforge.ui.components.InfoCard
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun CatalogScreen(s: ForgeState, vm: ForgeViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(if(s.catalog == Catalog.CHARACTERS) "Персонажи" else "Хранилище", style = MaterialTheme.typography.headlineLarge)
            Text("${s.total} записей · страница ${s.page + 1} / ${maxOf(1, s.totalPages)}", color = Muted)
            CatalogSwitch(s, vm)
            if (s.editorOpen) Text("В кузнице открыт предмет. Закройте редактор для смены каталога.", color = Muted, fontSize = 12.sp)
        }
        item {
            OutlinedTextField(s.query, vm::query, label = { Text("Поиск на текущей странице") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            EntitySpinner("Открыть запись", "", when(s.catalog) {
                Catalog.CHARACTERS -> EntitySource.CHARACTER
                Catalog.EQUIPMENT -> EntitySource.EQUIPMENT
                Catalog.ITEMS -> EntitySource.ITEM
            }, !s.busy && !s.editorOpen, vm::open)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !s.busy && !s.editorOpen, onClick = { vm.create() }) { Icon(Icons.Outlined.Add, null); Text("Создать") }
                OutlinedButton(enabled = !s.busy, onClick = { vm.refresh() }) { Text("Обновить") }
            }
            if(s.catalog == Catalog.EQUIPMENT) Row {
                OutlinedButton(enabled = !s.busy && !s.editorOpen, onClick = vm::randomItem) { Text("Получить случайный предмет") }
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
