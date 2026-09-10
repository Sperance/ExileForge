package com.sperance.exileforge.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.referenceKey
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.editor.InputSpec
import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.core.editor.defaultValue
import com.sperance.exileforge.core.editor.schemaFields
import com.sperance.exileforge.core.generation.attachSelectedDefinitions
import com.sperance.exileforge.core.generation.definitionReference
import com.sperance.exileforge.core.generation.definitionsOf
import com.sperance.exileforge.core.generation.modifierFromDefinition
import com.sperance.exileforge.ui.components.Spinner
import kotlinx.serialization.json.*

@Composable fun ObjectForm(schema: String, document: JsonObject, definitions: List<JsonObject>, enabled: Boolean, locked: Set<String> = emptySet(), onChange: (JsonObject) -> Unit) {
    val available = (definitionsOf(document) + definitions).distinctBy { it.text("id") + ":" + it.text("revision").ifBlank { "1" } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        schemaFields(schema, document).forEach { field ->
            key(field.key) {
                val fieldSpec = field.spec
                val value = document[field.key] ?: defaultValue(field.spec, field.default)
                val editable = enabled && field.key !in locked && field.key != "uuid"
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
