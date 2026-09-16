package com.sperance.exileforge.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.editor.defaultObject
import com.sperance.exileforge.core.i18n.tr
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
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) { Text(tr("Каталог: создать / изменить определение", "Catalogue: create / edit a definition")) }
    if(expanded) {
        Text(tr("Публикация доступна администратору. Изменение создаёт новую версию; старые предметы сохраняют прежнюю.", "Publishing is for administrators. An edit creates a new revision; existing items keep the old one."), color = Muted)
        Spinner(tr("Изменить пользовательское определение", "Edit a custom definition"), "", s.definitions.filter { it["poe"] == null || it["poe"] == JsonNull }.associate { definitionKey(it) to "${it.text("name")} · v${it.text("revision").ifBlank { "1" }}" }, !s.busy) { key ->
            draft = s.definitions.first { definitionKey(it) == key }
            revision = draft.text("revision").toIntOrNull() ?: 1
        }
        OutlinedButton(enabled = !s.busy, onClick = { draft = defaultObject("definition"); revision = 0 }) { Text(tr("Новое определение", "New definition")) }
        ObjectForm("definition", draft, s.definitions, !s.busy, locked = if(revision > 0) setOf("id") else emptySet(), onChange = { draft = it })
        Row {
            Text(tr("Доступен для новых предметов", "Available for new items"))
            Switch(checked = draft.text("enabled") != "false", enabled = !s.busy, onCheckedChange = { draft = JsonObject(draft + ("enabled" to JsonPrimitive(it))) })
        }
        Button(enabled = s.signedIn && !s.busy, onClick = { vm.publishDefinition(draft, revision) }) { Text(tr("Опубликовать версию ${revision + 1}", "Publish revision ${revision + 1}")) }
        Text(tr("Исходные правила PoE импортируются на сервере. Здесь редактируются пользовательские определения через формы.", "The original PoE rules are imported on the server. Custom definitions are edited here through forms."), color = Muted)
    }
}
