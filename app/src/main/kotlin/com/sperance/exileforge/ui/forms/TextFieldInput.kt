package com.sperance.exileforge.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.i18n.ui
import kotlinx.serialization.json.*

@Composable internal fun TextFieldInput(label: String, value: JsonElement, enabled: Boolean, onChange: (JsonElement) -> Unit) {
    OutlinedTextField((value as? JsonPrimitive)?.content.orEmpty(), { onChange(JsonPrimitive(it)) }, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth(), singleLine = label != ui("form.description"))
}
