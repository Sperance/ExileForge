package com.sperance.exileforge.ui.forms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.editor.InputSpec
import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.EntitySpinner
import com.sperance.exileforge.ui.components.Spinner
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Rune
import kotlinx.serialization.json.*
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgeTextButton

@Composable internal fun FormInput(label: String, spec: InputSpec, value: JsonElement, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    when (spec) {
        is InputSpec.Reference -> EntitySpinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.source, enabled) { onChange(JsonPrimitive(it)) }
        is InputSpec.Text -> {
            if (spec.suggestions.isNotEmpty()) Spinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.suggestions.associateWith { it }, enabled) { onChange(JsonPrimitive(it)) }
            TextFieldInput(if (spec.suggestions.isEmpty()) label else ui("form.custom_value"), value, enabled, onChange)
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
                    ForgeTextButton(onClick = { expanded = !expanded }) { Text("$label · ${summary(value)} ${if (expanded) "▴" else "▾"}") }
                    if (expanded) ObjectForm(spec.schema, value as? JsonObject ?: defaultObject(spec.schema), enabled, onChange = onChange)
                }
            }
        }
        InputSpec.Weights -> WeightsInput(label, value as? JsonObject ?: JsonObject(emptyMap()), enabled, onChange)
        is InputSpec.ListOf -> {
            val values = value as? JsonArray ?: JsonArray(emptyList())
            var expanded by remember { mutableStateOf(false) }
            OutlinedCard(border = BorderStroke(1.dp, Gold.copy(alpha = .3f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForgeTextButton(onClick = { expanded = !expanded }) { Text("$label (${values.size}) ${if (expanded) "▴" else "▾"}") }
                    if (expanded) {
                        values.forEachIndexed { index, element ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FormInput("${index + 1}", spec.element, element, enabled) { replacement -> onChange(JsonArray(values.toMutableList().apply { set(index, replacement) })) }
                                ForgeTextButton(enabled = enabled, onClick = { onChange(JsonArray(values.filterIndexed { i, _ -> i != index })) }) { Text(ui("form.remove", index + 1)) }
                            }
                        }
                        ForgeOutlinedButton(enabled = enabled, onClick = { onChange(JsonArray(values + inputDefault(spec.element))) }) { Text(ui("form.add")) }
                    }
                }
            }
        }
    }
}

/** Pools a record sits in: one tag and its weight per line; a renamed tag keeps its place. */
@Composable private fun WeightsInput(label: String, pools: JsonObject, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val entries = pools.entries.toList()
    fun write(lines: List<Pair<String, JsonElement>>) = onChange(JsonObject(lines.toMap(LinkedHashMap())))
    OutlinedCard(border = BorderStroke(1.dp, Gold.copy(alpha = .3f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeTextButton(onClick = { expanded = !expanded }) { Text("$label (${entries.size}) ${if (expanded) "▴" else "▾"}") }
            if (expanded) {
                entries.forEachIndexed { index, (tag, weight) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextFieldInput(ui("form.pool_tag"), JsonPrimitive(tag), enabled) { renamed ->
                            val name = (renamed as? JsonPrimitive)?.content.orEmpty()
                            if (name == tag || name !in pools) write(entries.mapIndexed { i, (k, v) -> if (i == index) name to v else k to v })
                        }
                        NumberInput(ui("form.pool_weight"), InputSpec.Number(integer = true, min = 0.0), weight, enabled) { changed ->
                            write(entries.mapIndexed { i, (k, v) -> if (i == index) k to changed else k to v })
                        }
                        ForgeTextButton(enabled = enabled, onClick = { write(entries.filterIndexed { i, _ -> i != index }.map { it.key to it.value }) }) { Text(ui("form.remove", index + 1)) }
                    }
                }
                ForgeOutlinedButton(enabled = enabled && "" !in pools, onClick = { write(entries.map { it.key to it.value } + ("" to JsonPrimitive(DEFAULT_POOL_WEIGHT))) }) { Text(ui("form.add")) }
            }
        }
    }
}

private const val DEFAULT_POOL_WEIGHT = 100

internal fun numericValue(text: String, spec: InputSpec.Number): JsonElement =
    (if (spec.integer) text.toLongOrNull()?.let { JsonPrimitive(it) } else text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { JsonPrimitive(it) }) ?: JsonPrimitive(text)
