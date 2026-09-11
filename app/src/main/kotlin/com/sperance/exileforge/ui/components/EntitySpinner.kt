package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.ui.theme.Gold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.network.ItemPage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

val LocalEntityPageLoader = staticCompositionLocalOf<suspend (EntitySource, Int) -> ItemPage> {
    { _, _ -> error("Entity page loader is not provided") }
}

/** Reusable paged selector. Only the selected identifier leaves this component. */
@Composable fun EntitySpinner(label: String, value: String, source: EntitySource, enabled: Boolean = true, onChange: (String) -> Unit) {
    val loader = LocalEntityPageLoader.current
    var expanded by remember(source) { mutableStateOf(false) }
    var records by remember(source) { mutableStateOf(emptyList<JsonObject>()) }
    var page by remember(source) { mutableIntStateOf(0) }
    var totalPages by remember(source) { mutableIntStateOf(1) }
    var query by remember(source) { mutableStateOf("") }
    var loading by remember(source) { mutableStateOf(false) }
    var failure by remember(source) { mutableStateOf<String?>(null) }
    var retry by remember(source) { mutableIntStateOf(0) }
    fun title(record: JsonObject): String = listOf("name", "login", "code").firstNotNullOfOrNull { record.text(it).takeIf(String::isNotBlank) }
        ?: "Запись"
    val selected = records.firstOrNull { it.entityId == value }
    OutlinedButton(enabled = enabled, onClick = { records = emptyList(); page = 0; totalPages = 1; query = ""; expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${selected?.let(::title) ?: if(value.isBlank()) "Выбрать" else "Выбрано · ${value.takeLast(6)}"} ▾")
    }
    LaunchedEffect(expanded, page, retry, source) {
        if(!expanded) return@LaunchedEffect
        loading = true; failure = null
        try {
            val result = loader(source, page)
            records = (if(page == 0) result.items else records + result.items).distinctBy { it.entityId }
            totalPages = result.totalPages
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { failure = e.message ?: "Не удалось загрузить список" }
        finally { loading = false }
    }
    if(expanded) AlertDialog(onDismissRequest = { expanded = false }, title = { Text(label) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("Поиск по загруженным записям") }, singleLine = true)
            if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                items(records.filter { title(it).contains(query, true) || it.entityId.contains(query, true) }, key = { it.entityId }) { record ->
                    TextButton(enabled = enabled, onClick = { onChange(record.entityId); expanded = false }, modifier = Modifier.fillMaxWidth()) {
                        ItemEmblem(itemVisualKind(record), Gold, Modifier.size(48.dp))
                        Text("${title(record)} · ${record.entityId.takeLast(6)}", modifier = Modifier.weight(1f).padding(start = 12.dp))
                    }
                }
            }
            if(!loading && failure == null && records.isEmpty()) Text("Записей пока нет")
            if(failure != null) TextButton(onClick = { retry++ }, enabled = !loading) { Text("Повторить") }
            else if(page + 1 < totalPages) TextButton(onClick = { loading = true; page++ }, enabled = !loading) { Text("Загрузить ещё") }
        }
    }, confirmButton = { TextButton(onClick = { expanded = false }) { Text("Закрыть") } })
}
