package com.sperance.exileforge.core.editor

import kotlinx.serialization.json.*

data class FormField(val key: String, val label: String, val spec: InputSpec, val default: JsonElement, val nullable: Boolean = false)
