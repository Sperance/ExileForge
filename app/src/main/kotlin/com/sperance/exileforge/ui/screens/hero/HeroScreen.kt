package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HeroScreen(s: ForgeState, vm: ForgeViewModel) {
    var detailId by remember(s.characterId) { mutableStateOf<String?>(null) }
    var query by remember(s.characterId) { mutableStateOf("") }
    var slot by remember(s.characterId) { mutableStateOf("") }
    var stashOpen by remember(s.characterId) { mutableStateOf(true) }
    // Opening the tab is what refreshes the hero, and only when the last reading has gone cold.
    // Nothing here asks the player to press anything: the pull below is for when they disagree.
    LaunchedEffect(s.characterId, s.sessionEpoch) { vm.ensureHero() }
    val stash = s.hero?.inventory.orEmpty()
    val documents = stash.associate { it.id to inventoryDocument(it, s.inventoryBases[it.equipmentId]) }
    val slots = documents.values.map { it.text("slot") }.filter(String::isNotBlank).distinct()
    val visible = stash.filter { instance -> documents[instance.id]?.let { (slot.isBlank() || it.text("slot") == slot) && it.text("name").contains(query, true) } == true }
    PullToRefreshBox(isRefreshing = s.busy, onRefresh = vm::loadHero, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ScreenHeader(tr("Арсенал героя", "Hero's arsenal"),
                    tr("Предметов в инвентаре: ${stash.size}", "${stash.size} items in the inventory"), ForgeGlyphs.Stash)
            }
            // The character is the one chosen in the menu; no screen below the gate picks another.
            item { HeroEquipmentPanel(s, vm::unequip) }
            item {
                OutlinedButton(enabled = !s.busy, onClick = { vm.tab(TAB_CRAFT) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(ForgeGlyphs.Tome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(tr("Крафт", "Crafting"))
                }
            }
            item { AdminGrantPanel(s, vm) }
            item { BagPanel(s) }
            item { SectionHeader(tr("Арсенал", "Stash"), stash.size, stashOpen) { stashOpen = !stashOpen } }
            if (stashOpen) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(query, { query = it }, label = { Text(tr("Найти предмет в арсенале", "Find an item in the stash")) },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = slot.isBlank(), onClick = { slot = "" }, label = { Text(tr("Все", "All")) }) }
                            items(slots) { key -> FilterChip(selected = slot == key, onClick = { slot = key }, label = { Text(slotTitle(key, s.lang)) }) }
                        }
                    }
                }
                if (visible.isEmpty()) item {
                    InfoCard(if (s.hero == null) tr("Арсенал ещё не загружен", "The stash is not loaded yet") else tr("Ничего не найдено", "Nothing found"),
                        tr("Потяните список вниз или измените фильтры.", "Pull the list down or change the filters."))
                }
                // A line, not a card: a stash is read down, and the card is one tap behind each line.
                items(visible, key = { it.id }) { instance ->
                    val inactive = s.hero?.inactive?.get(instance.id) != null
                    ItemRow(documents.getValue(instance.id), definitions = s.definitions, enabled = !s.busy,
                        selected = instance.id == s.selectedEquipment,
                        note = when {
                            inactive -> tr("Не работает", "Not working")
                            instance.equipped -> tr("Надето", "Equipped")
                            else -> null
                        },
                        noteColor = if (inactive) LifeRed else Gold) {
                        detailId = instance.id; vm.selectEquipment(instance.id)
                    }
                }
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
                    Text(tr("Предмет, уже занимающий слот, сервер снимет сам. Если требования предмета не выполнены, надеть его сервер не даст.",
                            "The server takes off whatever already occupies the slot. If the item's requirements are not met, the server refuses to put it on."),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
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
