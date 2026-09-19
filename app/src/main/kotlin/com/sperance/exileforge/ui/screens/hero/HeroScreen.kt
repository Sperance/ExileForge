package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HeroScreen(s: ForgeState, vm: ForgeViewModel) {
    var detailId by remember(s.characterId) { mutableStateOf<String?>(null) }
    var query by remember(s.characterId) { mutableStateOf("") }
    var slot by remember(s.characterId) { mutableStateOf("") }
    val stash = s.hero?.inventory.orEmpty()
    val documents = stash.associate { it.id to inventoryDocument(it, s.inventoryBases[it.equipmentId]) }
    val slots = documents.values.map { it.text("slot") }.filter(String::isNotBlank).distinct()
    LazyVerticalGrid(columns = GridCells.Adaptive(300.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScreenHeader(tr("Арсенал героя", "Hero's arsenal"),
                    tr("Предметов в инвентаре: ${stash.size}", "${stash.size} items in the inventory"), ForgeGlyphs.Stash)
                EntitySpinner(tr("Персонаж", "Character"), s.characterId, EntitySource.CHARACTER, !s.busy, vm::characterId)
                if (!s.signedIn) InfoCard(tr("Войдите в аккаунт", "Sign in"), tr("Во вкладке «Аккаунт» войдите, чтобы посмотреть снаряжение своего персонажа.", "Sign in on the Account tab to see your character's gear."))
                OutlinedButton(enabled = !s.busy && s.signedIn && s.characterId.isNotBlank(), onClick = vm::loadHero, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(8.dp)); Text(tr("Обновить героя", "Refresh the hero"))
                }
                HeroEquipmentPanel(s, vm::unequip)
                AdminGrantPanel(s, vm)
                BagPanel(s, vm)
                OutlinedTextField(query, { query = it }, label = { Text(tr("Найти предмет в арсенале", "Find an item in the stash")) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = slot.isBlank(), onClick = { slot = "" }, label = { Text(tr("Все", "All")) }) }
                    items(slots) { key -> FilterChip(selected = slot == key, onClick = { slot = key }, label = { Text(slotTitle(key, s.lang)) }) }
                }
            }
        }
        val visible = stash.filter { instance -> documents[instance.id]?.let { (slot.isBlank() || it.text("slot") == slot) && it.text("name").contains(query, true) } == true }
        if (visible.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            InfoCard(if (s.hero == null) tr("Арсенал ещё не загружен", "The stash is not loaded yet") else tr("Ничего не найдено", "Nothing found"),
                tr("Выберите персонажа, обновите героя или измените фильтры.", "Choose a character, refresh the hero or change the filters."))
        }
        items(visible, key = { it.id }) { instance ->
            ItemCard(documents.getValue(instance.id), enabled = !s.busy, selected = instance.id == s.selectedEquipment, definitions = s.definitions,
                actionLabel = if (instance.equipped) tr("Надето · свойства", "Equipped · properties") else tr("Надеть · свойства", "Equip · properties")) {
                detailId = instance.id; vm.selectEquipment(instance.id)
            }
        }
    }
    val instance = stash.firstOrNull { it.id == detailId }
    if (instance != null) ModalBottomSheet(onDismissRequest = { detailId = null }, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val document = documents.getValue(instance.id)
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ItemCard(document, enabled = false, detailed = true, definitions = s.definitions, actionLabel = tr("Экземпляр", "Instance") + " · ${instance.id.takeLast(6)}") }
            item {
                ForgePanel {
                    Engraved(if (instance.equipped) tr("Снять", "Unequip") else tr("Надеть", "Equip"))
                    Text(tr("Слот определяет шаблон предмета: ${slotTitle(document.text("slot"), s.lang)}.", "The template decides the slot: ${slotTitle(document.text("slot"), s.lang)}."), color = Muted, style = MaterialTheme.typography.bodySmall)
                    Button(enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin), modifier = Modifier.fillMaxWidth(), onClick = {
                        detailId = null
                        if (instance.equipped) vm.unequip(instance.id) else vm.equip(instance.id)
                    }) {
                        Icon(ForgeGlyphs.Helm, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (instance.equipped) tr("Снять предмет", "Take the item off") else tr("Надеть предмет", "Put the item on"))
                    }
                    Text(tr("Предмет, уже занимающий слот, сервер снимет сам.", "The server takes off whatever already occupies the slot."), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                OrnateDivider()
                ForgePanel { OrbPanel(s, instance.id, vm::selectOrb, vm::applyOrb) }
            }
            item {
                OrnateDivider()
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !s.busy && s.adminTools,
                    onClick = { vm.editInventoryBase(instance.equipmentId); detailId = null }) { Icon(Icons.Outlined.Edit, null); Text(tr("Редактировать базу предмета", "Edit the item base")) }
                Text(tr("База — общий шаблон. Выпавшие значения этого экземпляра принадлежат ему одному.", "The base is a shared template. The rolled values belong to this copy alone."), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
