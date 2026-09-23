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
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted

/**
 * A picker: [glyph] says what is being picked, and [optionGlyph], when given, draws each option —
 * a list of characteristics, say, where the options are codes with meanings of their own.
 */
@Composable fun Spinner(label: String, value: String, options: Map<String, String>, enabled: Boolean = true,
    glyph: Glyph = Glyph.INFO, optionGlyph: ((String) -> Glyph)? = null, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    OutlinedButton(onClick = { search = ""; expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(glyph.vector, null, modifier = Modifier.size(20.dp), tint = Gold)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
            Text(options[value] ?: value.ifBlank { ui("common.choose") }, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Outlined.ExpandMore, null, tint = Gold)
    }
    if(expanded) ForgeDialog(label, onDismiss = { expanded = false }) {
        OutlinedTextField(search, { search = it }, label = { Text(ui("common.find_option")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
        val filtered = options.filter { (key, title) -> key.contains(search, true) || title.contains(search, true) }.toList()
        if(filtered.isEmpty()) Text(ui("common.no_options"), color = Muted)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
            items(filtered, key = { it.first }) { (key, title) ->
                TextButton(enabled = enabled, onClick = { expanded = false; onChange(key) }, modifier = Modifier.fillMaxWidth()) {
                    optionGlyph?.let { Icon(it(key).vector, null, tint = Gold, modifier = Modifier.size(22.dp)) }
                    Text(title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    if(key == value) Icon(Icons.Outlined.CheckCircle, ui("common.chosen"), tint = Gold)
                }
            }
        }
    }
}

/** Every picker opens in the same stone-framed dialog. */
@Composable internal fun ForgeDialog(title: String, onDismiss: () -> Unit, body: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = Gold, shape = MaterialTheme.shapes.medium,
        title = { Text(title.uppercase(), style = MaterialTheme.typography.titleMedium) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(ui("common.close")) } })
}
