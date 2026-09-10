package com.sperance.exileforge.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.Spinner
import com.sperance.exileforge.ui.forms.ObjectForm
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable fun DefinitionPublisher(s: ForgeState, vm: ForgeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(defaultObject("definition")) }
    var revision by remember { mutableStateOf(0) }
    OutlinedButton(onClick = { expanded = !expanded }) { Text("Каталог: создать / изменить определение") }
    if(expanded) {
        Text("Публикация доступна администратору. Изменение создаёт новую версию; старые предметы сохраняют прежнюю.")
        Spinner("Изменить пользовательское определение", "", s.definitions.filter { it["poe"] == null || it["poe"] == JsonNull }.associate { definitionKey(it) to "${it.text("name")} · v${it.text("revision").ifBlank { "1" }}" }, !s.busy) { key ->
            draft = s.definitions.first { definitionKey(it) == key }
            revision = draft.text("revision").toIntOrNull() ?: 1
        }
        OutlinedButton(enabled = !s.busy, onClick = { draft = defaultObject("definition"); revision = 0 }) { Text("Новое определение") }
        ObjectForm("definition", draft, s.definitions, !s.busy, locked = if(revision > 0) setOf("id") else emptySet(), onChange = { draft = it })
        Row {
            Text("Доступен для новых предметов")
            Switch(checked = draft.text("enabled") != "false", enabled = !s.busy, onCheckedChange = { draft = JsonObject(draft + ("enabled" to JsonPrimitive(it))) })
        }
        Button(enabled = s.signedIn && !s.busy, onClick = { vm.publishDefinition(draft, revision) }) { Text("Опубликовать версию ${revision + 1}") }
        Text("Исходные правила PoE импортируются на сервере. Здесь редактируются пользовательские определения через формы.", color = Muted)
    }
}
