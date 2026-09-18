package com.sperance.exileforge.ui.forms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.editor.InputSpec
import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.ui.components.EntitySpinner
import com.sperance.exileforge.ui.components.Spinner
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Rune
import kotlinx.serialization.json.*

@Composable internal fun FormInput(label: String, spec: InputSpec, value: JsonElement, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    when (spec) {
        is InputSpec.Reference -> EntitySpinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.source, enabled) { onChange(JsonPrimitive(it)) }
        is InputSpec.Text -> {
            if (spec.suggestions.isNotEmpty()) Spinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.suggestions.associateWith { it }, enabled) { onChange(JsonPrimitive(it)) }
            TextFieldInput(if (spec.suggestions.isEmpty()) label else tr("Своё значение", "Custom value"), value, enabled, onChange)
        }
        is InputSpec.Number -> NumberInput(label, spec, value, enabled, onChange)
        InputSpec.Flag -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, modifier = Modifier.weight(1f)); Switch(checked = (value as? JsonPrimitive)?.booleanOrNull == true, enabled = enabled, onCheckedChange = { onChange(JsonPrimitive(it)) })
        }
        is InputSpec.Select -> Spinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.options.associateWith { it }, enabled) { onChange(JsonPrimitive(it)) }
        is InputSpec.Object -> {
            var expanded by remember { mutableStateOf(false) }
            OutlinedCard(border = BorderStroke(1.dp, Rune.copy(alpha = .3f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = !expanded }) { Text("$label · ${summary(value)} ${if (expanded) "▴" else "▾"}") }
                    if (expanded) ObjectForm(spec.schema, value as? JsonObject ?: defaultObject(spec.schema), enabled, onChange = onChange)
                }
            }
        }
        is InputSpec.ListOf -> {
            val values = value as? JsonArray ?: JsonArray(emptyList())
            var expanded by remember { mutableStateOf(false) }
            OutlinedCard(border = BorderStroke(1.dp, Gold.copy(alpha = .3f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = !expanded }) { Text("$label (${values.size}) ${if (expanded) "▴" else "▾"}") }
                    if (expanded) {
                        values.forEachIndexed { index, element ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FormInput("${index + 1}", spec.element, element, enabled) { replacement -> onChange(JsonArray(values.toMutableList().apply { set(index, replacement) })) }
                                TextButton(enabled = enabled, onClick = { onChange(JsonArray(values.filterIndexed { i, _ -> i != index })) }) { Text(tr("Удалить ${index + 1}", "Remove ${index + 1}")) }
                            }
                        }
                        OutlinedButton(enabled = enabled, onClick = { onChange(JsonArray(values + inputDefault(spec.element))) }) { Text(tr("Добавить", "Add")) }
                    }
                }
            }
        }
    }
}

internal fun numericValue(text: String, spec: InputSpec.Number): JsonElement =
    (if (spec.integer) text.toLongOrNull()?.let { JsonPrimitive(it) } else text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { JsonPrimitive(it) }) ?: JsonPrimitive(text)
