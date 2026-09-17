package com.sperance.exileforge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.ui.components.LocalEntityPageLoader
import com.sperance.exileforge.ui.components.OrnateDivider
import com.sperance.exileforge.ui.components.voidBackdrop
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.screens.catalog.CatalogScreen
import com.sperance.exileforge.ui.screens.checks.ChecksScreen
import com.sperance.exileforge.ui.screens.editor.EditorScreen
import com.sperance.exileforge.ui.screens.server.ServerScreen
import com.sperance.exileforge.ui.screens.inventory.InventoryForge
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*

@Composable fun ForgeApp(vm: ForgeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(s.message) {
        s.message?.let { snackbar.showSnackbar(it, withDismissAction = true); vm.dismissMessage() }
    }
    BackHandler(s.editorOpen && !s.busy) { confirmDiscard = true }
    CompositionLocalProvider(LocalEntityPageLoader provides vm::referencePage, LocalForgeIcons provides s.icons) {
    // Language is part of the key: every cached label is rebuilt in the chosen tongue.
    key(s.server, s.sessionEpoch, s.lang) {
    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbar) { data -> Snackbar(data, containerColor = PanelRaised, contentColor = Parchment, actionColor = Gold, shape = MaterialTheme.shapes.small) } },
        bottomBar = {
            NavigationBar(containerColor = Abyss, tonalElevation = 0.dp,
                modifier = Modifier.drawBehind { drawLine(Gold.copy(alpha = .35f), Offset(0f, 0f), Offset(size.width, 0f), 2f) }) {
                val destinations = if(s.adminTools)
                    listOf(0 to tr("Каталог", "Catalogue"), 1 to tr("Редактор", "Editor"), 2 to tr("Проверки", "Checks"), 4 to tr("Герой", "Hero"), 3 to tr("Аккаунт", "Account"))
                else listOf(0 to tr("Персонажи", "Characters"), 4 to tr("Герой", "Hero"), 5 to tr("Кузница", "Forge"), 6 to tr("Поход", "Expedition"), 3 to tr("Аккаунт", "Account"))
                val icons = mapOf<Int, ImageVector>(0 to ForgeGlyphs.Stash, 1 to ForgeGlyphs.Tome, 2 to ForgeGlyphs.Scroll,
                    3 to ForgeGlyphs.Portal, 4 to ForgeGlyphs.Helm, 5 to ForgeGlyphs.Anvil, 6 to ForgeGlyphs.Swords)
                destinations.forEach { (index, label) ->
                    NavigationBarItem(selected = s.tab == index, onClick = { vm.tab(index) },
                        icon = { Icon(icons.getValue(index), null, modifier = Modifier.size(22.dp)) }, label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = GoldBright, selectedTextColor = Gold,
                            indicatorColor = Gold.copy(alpha = .16f), unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().voidBackdrop()) {
            ForgeBanner(s.isAdmin, s.adminTools, s.lang, !s.busy && !s.editorOpen, onMode = { vm.mode(if(s.adminTools) AppMode.PLAYER else AppMode.ADMIN) }, onLanguage = vm::language)
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised) else OrnateDivider(Gold)
            when (s.tab) {
                0 -> CatalogScreen(s, vm)
                1 -> EditorScreen(s, vm, onDelete = { confirmDelete = true }, onClose = { confirmDiscard = true })
                2 -> ChecksScreen(s, vm, logs)
                3 -> ServerScreen(s, vm)
                4 -> InventoryForge(s, vm)
                5 -> InventoryForge(s, vm, forgeOnly = true)
                // The expedition draws a floor under a camera, so it takes the height the column has left.
                6 -> Box(Modifier.weight(1f)) { com.sperance.exileforge.ui.screens.combat.CombatScreen(s, vm) }
                7 -> com.sperance.exileforge.ui.screens.passives.PassiveScreen(s, vm)
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, containerColor = Panel, titleContentColor = Gold,
        title = { Text(tr("Удалить запись?", "Delete the record?")) },
        text = { Text("${s.original?.text("name")}\n${s.original?.entityId}\n" + tr("Запись будет скрыта на сервере.", "The record will be hidden on the server.")) },
        confirmButton = { TextButton(enabled = !s.busy, onClick = { confirmDelete = false; vm.delete() }) { Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Отмена", "Cancel")) } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, containerColor = Panel, titleContentColor = Gold,
        title = { Text(tr("Закрыть редактор?", "Close the editor?")) },
        text = { Text(tr("Несохранённые изменения будут потеряны.", "Unsaved changes will be lost.")) },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; vm.closeEditor() }) { Text(tr("Закрыть", "Close")) } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(tr("Продолжить", "Keep editing")) } })
    }
    }
}

/** Title banner: the sigil, the league name, and the two switches an exile keeps at hand. */
@Composable private fun ForgeBanner(isAdmin: Boolean, adminTools: Boolean, lang: Lang, enabled: Boolean, onMode: () -> Unit, onLanguage: (Lang) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Gold.copy(alpha = .10f), Color.Transparent, Gold.copy(alpha = .06f))))
        .padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).border(1.dp, Gold.copy(alpha = .5f), CutCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("EXILE FORGE", style = MaterialTheme.typography.titleLarge, color = GoldBright)
            Text(tr("АРСЕНАЛ ИЗГНАННИКА", "THE EXILE'S ARSENAL"), style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        LanguageSwitch(lang, onLanguage)
        if(isAdmin) TextButton(enabled = enabled, onClick = onMode) { Text(if(adminTools) tr("Админ", "Admin") else tr("Игрок", "Player")) }
    }
}

/** Two runes carved side by side: the tongue every label speaks. */
@Composable private fun LanguageSwitch(lang: Lang, onLanguage: (Lang) -> Unit) {
    Row(Modifier.border(1.dp, Gold.copy(alpha = .35f), CutCornerShape(6.dp)), verticalAlignment = Alignment.CenterVertically) {
        Lang.entries.forEach { option ->
            val active = option == lang
            Text(option.short, color = if(active) Ink else Muted, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.background(if(active) Gold else Color.Transparent)
                    .clickable(enabled = !active) { onLanguage(option) }
                    .padding(horizontal = 9.dp, vertical = 6.dp))
        }
    }
}
