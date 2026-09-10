package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import kotlinx.serialization.json.*

@Composable fun Spinner(label: String, value: String, options: Map<String, String>, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { search = ""; expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${options[value] ?: value.ifBlank { "Выбрать" }}  ▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            if(options.size > 8) OutlinedTextField(search, { search = it }, label = { Text("Поиск") }, singleLine = true, modifier = Modifier.padding(8.dp))
            options.filter { (key, label) -> key.contains(search, true) || label.contains(search, true) }.forEach { (key, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = { expanded = false; onChange(key) })
            }
        }
    }
}
