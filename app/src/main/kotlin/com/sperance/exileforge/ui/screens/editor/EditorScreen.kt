package com.sperance.exileforge.ui.screens.editor

import com.sperance.exileforge.core.display.Glyph

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.statNumber
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.editor.formSchema
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.forms.ObjectForm
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Composable internal fun EditorScreen(s: ForgeState, vm: ForgeViewModel, onDelete: () -> Unit, onClose: () -> Unit) {
    if (!s.admin.editorOpen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader(ui("editor.title_forge"), ui("editor.workshop_title"), ForgeGlyphs.Anvil)
            InfoCard(ui("editor.workshop"), ui("editor.workshop_note"))
            InfoCard(ui("editor.editor"), ui("editor.pick_note"))
            if (s.adminTools) CatalogSwitch(s, vm)
            ForgePanel {
                if (s.admin.catalog != Catalog.EQUIPMENT) Button(enabled = !s.busy && s.canEdit, onClick = { vm.create() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.admin.catalog == Catalog.CHARACTERS) ui("editor.create_character") else ui("editor.create_item"))
                }
                else EquipmentKind.entries.forEach { kind ->
                    OutlinedButton(enabled = !s.busy && s.canEdit, onClick = { vm.create(kind) }, modifier = Modifier.fillMaxWidth()) {
                        Text(when (kind) {
                            EquipmentKind.Weapon -> ui("editor.create_weapon")
                            EquipmentKind.Armor -> ui("editor.create_armour")
                            EquipmentKind.Accessory -> ui("editor.create_accessory")
                        })
                    }
                }
            }
        }
        return
    }
    key(s.admin.catalog, s.admin.original?.entityId) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ScreenHeader(if (s.admin.catalog == Catalog.CHARACTERS) ui("common.character") else ui("nav.forge"), icon = ForgeGlyphs.Anvil) }
                    IconButton(enabled = !s.busy, onClick = onClose) { Icon(Icons.Outlined.Close, ui("editor.close")) }
                }
                if (s.admin.catalog != Catalog.CHARACTERS) ItemCard(s.admin.draft, enabled = false, detailed = true, definitions = s.world.definitions, actionLabel = ui("editor.preview"))
                if (s.admin.catalog == Catalog.CHARACTERS && s.admin.original == null) {
                    Text(ui("editor.owner_note"), color = Muted)
                    // The class is the whole stat base and the way into the tree, and the server has
                    // no route to change it later: it is chosen here or nowhere.
                    Spinner(ui("common.class"), s.admin.draft.text("classId"),
                        s.world.classes.associate { it.id to it.title }, !s.busy, glyph = Glyph.CHARACTER) { chosen ->
                        vm.draftClass(chosen); vm.edit(JsonObject(s.admin.draft + ("classId" to JsonPrimitive(chosen))))
                    }
                    s.world.classes.firstOrNull { it.id == s.admin.draft.text("classId") }?.let { chosen ->
                        Text(chosen.details, color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text(ui("editor.level1_base") + chosen.baseStats.joinToString(" · ") { "${statTitle(it.stat, s.lang)} ${statNumber(it.stat, it.value)}" },
                            color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (s.world.classes.isEmpty()) Text(ui("editor.no_classes"), color = MaterialTheme.colorScheme.error)
                }
            }
            if (s.admin.catalog == Catalog.EQUIPMENT && s.adminTools) item {
                ForgePanel {
                    Engraved(ui("editor.modifiers"))
                    Text(ui("editor.definitions_loaded", s.world.definitions.size), color = Muted)
                    OutlinedButton(enabled = !s.busy, onClick = vm::loadDefinitions) { Text(ui("editor.refresh_modifiers")) }
                    Text(ui("editor.pool_note"),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                // A player may rename their own character; only an administrator touches its stats.
                val locked = if (s.admin.catalog == Catalog.CHARACTERS && !s.adminTools) setOf("professionSkills", "battleSkills", "boolSkills") else emptySet()
                ObjectForm(formSchema(s.admin.catalog), s.admin.draft, !s.busy && s.canEdit, locked = locked, onChange = vm::edit)
            }
            item { Button(enabled = !s.busy && s.canEdit, onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text(ui("editor.save")) } }
            if (s.admin.original != null) item { OutlinedButton(enabled = !s.busy, onClick = vm::reloadEditor, modifier = Modifier.fillMaxWidth()) { Text(ui("editor.reread")) } }
            if (s.admin.original != null && s.canEdit) item { TextButton(enabled = !s.busy, onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text(ui("common.delete"), color = MaterialTheme.colorScheme.error) } }
        }
    }
}
