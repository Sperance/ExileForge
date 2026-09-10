package com.sperance.exileforge.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.editor.InputSpec
import kotlinx.serialization.json.*

@Composable internal fun NumberInput(label: String, spec: InputSpec.Number, value: JsonElement, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    var raw by remember { mutableStateOf((value as? JsonPrimitive)?.content.orEmpty()) }
    LaunchedEffect(value) { if(numericValue(raw, spec) != value) raw = (value as? JsonPrimitive)?.content.orEmpty() }
    OutlinedTextField(raw, { raw = it; onChange(numericValue(it, spec)) }, label = { Text(label) }, enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth())
}
