package com.sperance.exileforge.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.editor.defaultValue
import com.sperance.exileforge.core.editor.schemaFields
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.ui.icons.vector
import com.sperance.exileforge.ui.theme.Gold
import kotlinx.serialization.json.*

@Composable fun ObjectForm(schema: String, document: JsonObject, enabled: Boolean, locked: Set<String> = emptySet(), onChange: (JsonObject) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        schemaFields(schema, document).forEach { field ->
            key(field.key) {
                val value = document[field.key] ?: defaultValue(field.spec, field.default)
                val editable = enabled && field.key !in locked
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Glyph.ofField(field.key).vector, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Text(field.label, style = MaterialTheme.typography.labelLarge)
                }
                if (field.nullable) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(field.label, modifier = Modifier.weight(1f))
                    Switch(checked = value != JsonNull, enabled = editable, onCheckedChange = {
                        onChange(document.changed(field.key, if (it) defaultValue(field.spec, if (field.default == JsonNull) inputDefault(field.spec) else field.default) else JsonNull))
                    })
                }
                if (value != JsonNull || !field.nullable) FormInput(field.label, field.spec, value, editable) { onChange(document.changed(field.key, it)) }
            }
        }
    }
}
