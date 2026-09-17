package com.sperance.exileforge.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.*
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.command.EquipmentSlot
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ForgeIcon
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InventoryForge(s: ForgeState, vm: ForgeViewModel, forgeOnly: Boolean = false) {
    var detailId by remember(s.characterId, forgeOnly) { mutableStateOf<String?>(if(forgeOnly) s.selectedEquipment.takeIf { it.isNotBlank() } else null) }
    var confirmCurrency by remember { mutableStateOf(false) }
    var query by remember(s.characterId) { mutableStateOf("") }
    var slot by remember(s.characterId) { mutableStateOf("") }
    val documents = s.inventory.associate { it.text("uuid") to inventoryDocument(it, s.inventoryBases[it.text("equipmentId")]) }
    val slots = documents.values.map { it.text("slot") }.filter(String::isNotBlank).distinct()
    LazyVerticalGrid(columns = GridCells.Adaptive(300.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScreenHeader(if(forgeOnly) tr("Кузница", "Forge") else tr("Арсенал героя", "Hero's arsenal"),
                    tr("Загружено ${s.inventory.size} из ${s.inventoryTotal} · снаряжение в инвентаре", "${s.inventory.size} of ${s.inventoryTotal} loaded from the stash"),
                    if(forgeOnly) ForgeGlyphs.Anvil else ForgeGlyphs.Stash)
                EntitySpinner(tr("Персонаж", "Character"), s.characterId, EntitySource.CHARACTER, !s.busy && s.pending == null, vm::characterId)
                if(!s.signedIn) InfoCard(tr("Войдите в аккаунт", "Sign in"), tr("Во вкладке «Сервер» войдите, чтобы посмотреть снаряжение своего персонажа и применять сферы.", "Sign in on the Account tab to see your character's gear and use orbs."))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !s.busy && s.signedIn && s.characterId.isNotBlank(), onClick = vm::loadInventory) { Icon(Icons.Outlined.Refresh, null); Text(tr("Обновить", "Refresh")) }
                    Button(enabled = !s.busy && s.adminTools && s.ownsCharacter && s.inventoryVersion != null && s.pending == null, onClick = { vm.inventoryAction("drop") }) { Icon(Icons.Outlined.AutoAwesome, null); Text(tr("Новый дроп", "New drop")) }
                }
                Text(tr("Тестовый дроп доступен администратору для своего персонажа.", "Test drops are available to an administrator on their own character."), color = Muted, style = MaterialTheme.typography.bodySmall)
                if(!forgeOnly) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !s.busy && s.signedIn, onClick = { vm.tab(7) }) { Icon(ForgeGlyphs.Constellation, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Древо навыков", "Passive tree")) }
                    OutlinedButton(enabled = !s.busy && s.signedIn, onClick = { vm.tab(6) }) { Icon(ForgeGlyphs.Swords, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(tr("В поход", "March out")) }
                }
                if(!forgeOnly) { CharacterEquipmentPanel(s, vm::unequip); InventoryCommandsPanel(s, vm) }
                OutlinedTextField(query, { query = it }, label = { Text(tr("Найти предмет в арсенале", "Find an item in the stash")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = slot.isBlank(), onClick = { slot = "" }, label = { Text(tr("Все", "All")) }) }
                    items(slots) { key -> FilterChip(selected = slot == key, onClick = { slot = key }, label = { Text(slotTitle(key, s.lang)) }) }
                }
                // The stash is unbounded, so the server hands it out one cursor page at a time.
                if(s.inventoryNext != null) OutlinedButton(enabled = !s.busy && s.signedIn, onClick = vm::loadMoreInventory, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.ExpandMore, null); Text(tr("Загрузить ещё предметы", "Load more items"))
                }
                if(s.pending != null) InfoCard(tr("Ожидает подтверждения", "Awaiting confirmation"), tr("Повторите исходный запрос, чтобы узнать результат без повторного списания.", "Repeat the original request to learn the result without spending twice."))
                if(s.pending != null) OutlinedButton(enabled = !s.busy && s.signedIn, onClick = vm::retryInventoryAction) { Text(tr("Подтвердить результат", "Confirm the result")) }
            }
        }
        val visible = s.inventory.filter { instance -> documents[instance.text("uuid")]?.let { (slot.isBlank() || it.text("slot") == slot) && it.text("name").contains(query, true) } == true }
        if(visible.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            InfoCard(if(s.inventoryVersion == null) tr("Арсенал ещё не загружен", "The stash is not loaded yet") else tr("Ничего не найдено", "Nothing found"),
                if(s.inventoryNext != null) tr("Поиск идёт по загруженным страницам. Догрузите инвентарь или измените фильтры.", "The search covers the loaded pages. Load more of the stash or change the filters.")
                else tr("Выберите персонажа, загрузите экипировку или измените фильтры.", "Choose a character, load the equipment or change the filters."))
        }
        items(visible, key = { it.text("uuid") }) { instance ->
            val id = instance.text("uuid")
            ItemCard(documents.getValue(id), enabled = !s.busy, selected = id == s.selectedEquipment,
                definitions = s.inventoryDefinitions, actionLabel = if(id in s.equipmentView?.equipped.orEmpty().values) tr("Надето · свойства", "Equipped · properties") else tr("Надеть · свойства", "Equip · properties")) { detailId = id; vm.selectEquipment(id) }
        }
    }
    // Mirror returns a new UUID. Keep the visible detail and the next craft target identical.
    LaunchedEffect(s.selectedEquipment) {
        if(detailId != null && s.inventory.any { it.text("uuid") == s.selectedEquipment }) {
            detailId = s.selectedEquipment
        }
    }
    val instance = s.inventory.firstOrNull { it.text("uuid") == detailId }
    if(instance != null) ModalBottomSheet(onDismissRequest = { detailId = null }, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ItemCard(documents.getValue(instance.text("uuid")), enabled = false, detailed = true, definitions = s.inventoryDefinitions, actionLabel = tr("Экземпляр", "Instance") + " · ${instance.text("uuid").takeLast(6)}") }
            item {
                val available = EquipmentSlot.forItem(documents.getValue(instance.text("uuid")).text("slot"))
                var chosen by remember(instance.text("uuid")) { mutableStateOf(available.firstOrNull()?.name.orEmpty()) }
                ForgePanel {
                    Engraved(tr("Надеть", "Equip"))
                    Spinner(tr("Надеть в слот", "Equip into slot"), chosen, available.associate { it.name to it.title(s.lang) }, !s.busy, { chosen = it })
                    Button(enabled = !s.busy && s.signedIn && s.pending == null && s.inventoryVersion != null && chosen.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                        onClick = { detailId = null; vm.compareEquipment(instance.text("uuid"), EquipmentSlot.valueOf(chosen)) }) { Icon(ForgeGlyphs.Scales, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Сравнить и надеть", "Compare and equip")) }
                    Text(tr("Требования уровня, характеристик и совместимость рук проверит сервер.", "Level, stat and hand requirements are checked by the server."), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                CraftDetails(s)
                ForgePanel {
                    Engraved(tr("Крафт экземпляра", "Craft this instance"))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // The orb's own icon: the currency list carries it, the tables answer for older servers.
                        val orb = s.currencies.firstOrNull { it.text("id") == s.selectedCurrency }
                        ForgeIcon(orb?.text("icon")?.ifBlank { null } ?: LocalForgeIcons.current.forCurrency(s.selectedCurrency),
                            Modifier.size(44.dp), description = orb?.text("name"))
                        Box(Modifier.weight(1f)) { Spinner(tr("Сфера", "Orb"), s.selectedCurrency, s.currencies.associate { it.text("id") to it.text("name") }, !s.busy, vm::selectCurrency) }
                    }
                    Button(modifier = Modifier.fillMaxWidth(), enabled = !s.busy && s.ownsCharacter && instance["poe"] is JsonObject && s.selectedCurrency.isNotBlank() && s.inventoryVersion != null && s.pending == null && s.craftOptions?.let { options -> options.characterVersion == s.inventoryVersion && options.options.any { it.currency == s.selectedCurrency && it.available } } == true,
                        onClick = { confirmCurrency = true }) { Icon(ForgeGlyphs.Orb, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Применить сферу", "Use the orb")) }
                    if(instance["poe"] !is JsonObject) Text(tr("Этот экземпляр ещё не переведён в формат PoE.", "This instance has not been converted to the PoE format yet."), color = Muted)
                }
            }
            item {
                OrnateDivider()
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !s.busy && s.adminTools, onClick = { vm.editInventoryBase(instance.text("equipmentId")); detailId = null }) { Icon(Icons.Outlined.Edit, null); Text(tr("Редактировать базу предмета", "Edit the item base")) }
                Text(tr("База — общий шаблон. Выпавшие значения этого экземпляра изменяются через крафт.", "The base is a shared template. Rolled values of this instance change only through crafting."), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    EquipmentComparisonSheet(s, vm)
    if(confirmCurrency) AlertDialog(onDismissRequest = { confirmCurrency = false }, containerColor = Panel, titleContentColor = Gold,
        title = { Text(tr("Применить сферу?", "Use the orb?")) },
        text = { Text("${s.currencies.firstOrNull { it.text("id") == s.selectedCurrency }?.text("name") ?: s.selectedCurrency}\n" + tr("Будет потрачена одна сфера. Свойства выбранного экземпляра могут измениться или удалиться.", "One orb will be spent. The properties of the chosen instance may change or disappear.")) },
        confirmButton = { TextButton(onClick = { confirmCurrency = false; vm.inventoryAction("craft") }) { Text(tr("Применить", "Use")) } },
        dismissButton = { TextButton(onClick = { confirmCurrency = false }) { Text(tr("Отмена", "Cancel")) } })
}
