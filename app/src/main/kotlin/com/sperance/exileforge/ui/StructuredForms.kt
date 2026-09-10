package com.sperance.exileforge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.*
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
private fun JsonObject.changed(key: String, value: JsonElement) = JsonObject(this + (key to value))
private fun inputDefault(spec: InputSpec): JsonElement = when(spec) {
    is InputSpec.Object -> defaultObject(spec.schema)
    is InputSpec.Text -> JsonPrimitive(spec.suggestions.firstOrNull().orEmpty())
    is InputSpec.Number -> JsonPrimitive(0)
    InputSpec.Flag -> JsonPrimitive(false)
    is InputSpec.ListOf -> JsonArray(emptyList())
    is InputSpec.Select -> JsonPrimitive(spec.options.first())
    is InputSpec.Union -> JsonPrimitive(spec.variants.keys.first())
}
private fun summary(value: JsonElement): String = when(value) {
    is JsonObject -> listOf("name", "definitionId", "id", "stat", "type", "equipmentId", "itemId", "tier").firstNotNullOfOrNull { value.text(it).takeIf(String::isNotBlank) }.orEmpty()
    is JsonPrimitive -> value.content
    else -> ""
}

@Composable fun ObjectForm(schema: String, document: JsonObject, definitions: List<JsonObject>, enabled: Boolean, locked: Set<String> = emptySet(), onChange: (JsonObject) -> Unit) {
    val available = (definitionsOf(document) + definitions).distinctBy { it.text("id") + ":" + it.text("revision").ifBlank { "1" } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        schemaFields(schema, document).forEach { field ->
            key(field.key) {
                val fieldSpec = field.spec
                val value = document[field.key] ?: defaultValue(field.spec, field.default)
                val editable = enabled && field.key !in locked
                if(field.nullable) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(field.label, modifier = Modifier.weight(1f))
                        Switch(checked = value != JsonNull, enabled = editable, onCheckedChange = { onChange(document.changed(field.key, if(it) defaultValue(field.spec, if(field.default == JsonNull) inputDefault(field.spec) else field.default) else JsonNull)) })
                    }
                }
                if(value != JsonNull || !field.nullable) {
                    when {
                        (schema == "modifier" || schema == "reference") && field.key == "definitionId" -> {
                            Spinner(field.label, referenceKey(document, if(schema == "reference") "revision" else "definitionRevision"), available.associate { definitionKey(it) to "${it.text("name")} · v${it.text("revision").ifBlank { "1" }} (${it.text("id")})" }, editable) { selected ->
                                val definition = available.firstOrNull { definitionKey(it) == selected }
                                onChange(if(definition == null) document.changed(field.key, JsonPrimitive(selected)) else if(schema == "reference") definitionReference(definition) else JsonObject(document + modifierFromDefinition(definition)))
                            }
                            TextFieldInput("Свой идентификатор", value, editable) { onChange(document.changed(field.key, it)) }
                        }
                        schema == "modifier" && field.key == "tier" -> {
                            val definition = available.firstOrNull { definitionKey(it) == referenceKey(document, "definitionRevision") }
                            val tiers = (definition?.get("tiers") as? JsonArray).orEmpty().map { it.jsonObject.text("tier") }
                            if(tiers.isNotEmpty()) Spinner("Выбрать tier", document.text("tier"), tiers.associateWith { "T$it" }, editable) { tier ->
                                onChange(JsonObject(document + modifierFromDefinition(definition!!, tier.toInt())))
                            }
                            FormInput(field.label, field.spec, value, available, editable) { onChange(document.changed(field.key, it)) }
                        }
                        fieldSpec is InputSpec.Union -> Spinner(field.label, document.text("type"), fieldSpec.variants, editable) { onChange(defaultObject(schema, it)) }
                        else -> FormInput(field.label, field.spec, value, available, editable) { replacement ->
                            onChange(if(schema == "equipment" && field.key == "modifiers") attachSelectedDefinitions(document, replacement, available) else document.changed(field.key, replacement))
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun TextFieldInput(label: String, value: JsonElement, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    OutlinedTextField((value as? JsonPrimitive)?.content.orEmpty(), { onChange(JsonPrimitive(it)) }, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth(), singleLine = label != "Описание")
}

@Composable private fun FormInput(label: String, spec: InputSpec, value: JsonElement, definitions: List<JsonObject>, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    when(spec) {
        is InputSpec.Text -> {
            if(spec.suggestions.isNotEmpty()) Spinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.suggestions.associateWith { it }, enabled) { onChange(JsonPrimitive(it)) }
            TextFieldInput(if(spec.suggestions.isEmpty()) label else "Своё значение", value, enabled, onChange)
        }
        is InputSpec.Number -> NumberInput(label, spec, value, enabled, onChange)
        InputSpec.Flag -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, modifier = Modifier.weight(1f)); Switch(checked = (value as? JsonPrimitive)?.booleanOrNull == true, enabled = enabled, onCheckedChange = { onChange(JsonPrimitive(it)) })
        }
        is InputSpec.Select -> Spinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.options.associateWith { it }, enabled) { onChange(JsonPrimitive(it)) }
        is InputSpec.Union -> Spinner(label, (value as? JsonPrimitive)?.content.orEmpty(), spec.variants, enabled) { onChange(JsonPrimitive(it)) }
        is InputSpec.Object -> {
            var expanded by remember { mutableStateOf(false) }
            OutlinedCard(border = BorderStroke(1.dp, Rune.copy(alpha = .3f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = !expanded }) { Text("$label · ${summary(value)} ${if(expanded) "▴" else "▾"}") }
                    if(expanded) ObjectForm(spec.schema, value as? JsonObject ?: defaultObject(spec.schema), definitions, enabled, onChange = onChange)
                }
            }
        }
        is InputSpec.ListOf -> {
            val values = value as? JsonArray ?: JsonArray(emptyList())
            var expanded by remember { mutableStateOf(false) }
            OutlinedCard(border = BorderStroke(1.dp, Gold.copy(alpha = .3f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = !expanded }) { Text("$label (${values.size}) ${if(expanded) "▴" else "▾"}") }
                    if(expanded) {
                        values.forEachIndexed { index, element ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FormInput("${index + 1}", spec.element, element, definitions, enabled) { replacement -> onChange(JsonArray(values.toMutableList().apply { set(index, replacement) })) }
                                TextButton(enabled = enabled, onClick = { onChange(JsonArray(values.filterIndexed { i, _ -> i != index })) }) { Text("Удалить ${index + 1}") }
                            }
                        }
                        val objectSchema = (spec.element as? InputSpec.Object)?.schema
                        if(objectSchema == "reference" || objectSchema == "modifier") {
                            Spinner("Добавить из списка", "", definitions.filter { it.text("enabled") != "false" }.associate { definitionKey(it) to "${it.text("name")} · v${it.text("revision").ifBlank { "1" }} (${it.text("id")})" }, enabled) { id ->
                                val definition = definitions.first { definitionKey(it) == id }
                                val added = if(objectSchema == "reference") definitionReference(definition) else modifierFromDefinition(definition)
                                onChange(JsonArray(values + added))
                            }
                        }
                        OutlinedButton(enabled = enabled, onClick = {
                            val added = if(objectSchema == "definition") defaultObject("definition").changed("id", JsonPrimitive("custom_${newEntityId().take(8)}")) else inputDefault(spec.element)
                            onChange(JsonArray(values + added))
                        }) { Text(if(objectSchema == "definition") "Своё определение" else "Добавить") }
                    }
                }
            }
        }
    }
}

private fun numericValue(text: String, spec: InputSpec.Number): JsonElement =
    (if(spec.integer) text.toLongOrNull()?.let { JsonPrimitive(it) } else text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { JsonPrimitive(it) }) ?: JsonPrimitive(text)

@Composable private fun NumberInput(label: String, spec: InputSpec.Number, value: JsonElement, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    var raw by remember { mutableStateOf((value as? JsonPrimitive)?.content.orEmpty()) }
    LaunchedEffect(value) { if(numericValue(raw, spec) != value) raw = (value as? JsonPrimitive)?.content.orEmpty() }
    OutlinedTextField(raw, { raw = it; onChange(numericValue(it, spec)) }, label = { Text(label) }, enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth())
}
