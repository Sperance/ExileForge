package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.icons.propertyIcon
import com.sperance.exileforge.ui.theme.Gold

@Composable fun Spinner(label: String, value: String, options: Map<String, String>, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    OutlinedButton(onClick = { search = ""; expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(propertyIcon(label), null, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(options[value] ?: value.ifBlank { "Выбрать" }, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Outlined.ExpandMore, null)
    }
    if(expanded) AlertDialog(onDismissRequest = { expanded = false }, title = { Text(label) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(search, { search = it }, label = { Text("Найти вариант") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
            val filtered = options.filter { (key, title) -> key.contains(search, true) || title.contains(search, true) }.toList()
            if(filtered.isEmpty()) Text("Нет подходящих вариантов")
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(filtered, key = { it.first }) { (key, title) ->
                    TextButton(enabled = enabled, onClick = { expanded = false; onChange(key) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(propertyIcon(key), null, tint = Gold, modifier = Modifier.size(22.dp))
                        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                        if(key == value) Icon(Icons.Outlined.CheckCircle, "Выбрано", tint = Gold)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { expanded = false }) { Text("Закрыть") } })
}
