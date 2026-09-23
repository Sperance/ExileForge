package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import com.sperance.exileforge.presentation.state.Reads
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
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HeroScreen(s: ForgeState, vm: ForgeViewModel) {
    var sellId by remember { mutableStateOf<String?>(null) }
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
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.HERO), onRefresh = vm::loadHero, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ScreenHeader(ui("hero.title"),
                    ui("hero.inventory_count", stash.size), ForgeGlyphs.Stash)
            }
            // The character is the one chosen in the menu; no screen below the gate picks another.
            item { HeroEquipmentPanel(s, vm::unequip) }
            item {
                OutlinedButton(enabled = !s.busy, onClick = { vm.tab(TAB_CRAFT) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(ForgeGlyphs.Tome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(ui("nav.craft"))
                }
            }
            item { BagPanel(s) }
            item { SectionHeader(ui("hero.stash"), stash.size, stashOpen) { stashOpen = !stashOpen } }
            if (stashOpen) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(query, { query = it }, label = { Text(ui("hero.find_item")) },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = slot.isBlank(), onClick = { slot = "" }, label = { Text(ui("common.all")) }) }
                            items(slots) { key -> FilterChip(selected = slot == key, onClick = { slot = key }, label = { Text(slotTitle(key, s.lang)) }) }
                        }
                    }
                }
                if (visible.isEmpty()) item {
                    InfoCard(if (s.hero == null) ui("hero.stash_empty") else ui("tree.nothing_found"),
                        ui("hero.stash_empty_hint"))
                }
                // A line, not a card: a stash is read down, and the card is one tap behind each line.
                items(visible, key = { it.id }) { instance ->
                    val inactive = s.hero?.inactive?.get(instance.id) != null
                    ItemRow(documents.getValue(instance.id), definitions = s.definitions, enabled = !s.busy,
                        selected = instance.id == s.selectedEquipment,
                        // The server's verdict on the template, not a requirement worked out here.
                        // Something already worn is judged by `inactive` instead: it is in a slot,
                        // and "cannot be worn" would be an odd thing to say about it.
                        unwearable = if (instance.equipped) emptyList()
                            else s.hero?.sheet?.unwearableBy?.get(instance.equipmentId).orEmpty(),
                        note = when {
                            inactive -> ui("hero.inactive")
                            // A jewel is worn too, but not anywhere a player can point at on the
                            // body — saying "equipped" would send them looking through the slots.
                            instance.socketed -> ui("hero.in_socket")
                            instance.equipped -> ui("hero.equipped")
                            else -> null
                        },
                        noteColor = if (inactive) LifeRed else Gold) {
                        detailId = instance.id; vm.selectEquipment(instance.id)
                    }
                }
            }
        }
    }
    // Selling is final and takes the rolls with it, so it is asked about by name.
    sellId?.let { id ->
        val document = documents[id]
        val name = document?.text("name").orEmpty()
        // The price is the merchant's: the sheet says gold is coming and leaves the sum to him.
        ConfirmSheet(
            title = ui("hero.sell_q"), subtitle = name, danger = true,
            icon = if (document == null) null else ({ ItemIcon(document, rarityColor(document.text("rarity")), Modifier.size(44.dp)) }),
            ledger = listOf(
                LedgerLine(ui("confirm.give"), name, Tone.SPEND),
                LedgerLine(ui("confirm.gain"), ui("confirm.gold_by_server"), Tone.GAIN),
            ),
            note = ui("hero.sell_confirm"),
            confirm = ui("hero.sell_do"),
            onDismiss = { sellId = null }) { detailId = null; vm.sellForGold(id) }
    }
    val instance = stash.firstOrNull { it.id == detailId }
    if (instance != null) ModalBottomSheet(onDismissRequest = { detailId = null }, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val document = documents.getValue(instance.id)
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ItemCard(document, enabled = false, detailed = true, definitions = s.definitions, actionLabel = ui("hero.instance") + " · ${instance.id.takeLast(6)}") }
            item {
                ForgePanel {
                    Engraved(if (instance.equipped) ui("hero.unequip") else ui("hero.equip"))
                    Text(ui("hero.slot_note", slotTitle(document.text("slot"), s.lang)), color = Muted, style = MaterialTheme.typography.bodySmall)
                    Button(enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin), modifier = Modifier.fillMaxWidth(), onClick = {
                        detailId = null
                        if (instance.equipped) vm.unequip(instance.id) else vm.equip(instance.id)
                    }) {
                        Icon(ForgeGlyphs.Helm, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (instance.equipped) ui("hero.take_off") else ui("hero.put_on"))
                    }
                    Text(ui("hero.equip_note"),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                OrnateDivider()
                ForgePanel {
                    Engraved(ui("hero.sell_section"))
                    Text(ui("hero.sell_note"),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                    // Selling destroys the copy and its rolls, which is why it is asked about.
                    OutlinedButton(modifier = Modifier.fillMaxWidth(),
                        enabled = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin) && !instance.equipped && !instance.socketed,
                        onClick = { sellId = instance.id }) {
                        Icon(ForgeGlyphs.Orb, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(ui("hero.sell_for_gold"))
                    }
                }
            }
            item {
                OrnateDivider()
                ForgePanel { OrbPanel(s, instance.id, vm::selectOrb, vm::applyOrb) }
            }
            item {
                OrnateDivider()
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !s.busy && s.adminTools,
                    onClick = { vm.editInventoryBase(instance.equipmentId); detailId = null }) { Icon(Icons.Outlined.Edit, null); Text(ui("hero.edit_base")) }
                Text(ui("hero.base_note"), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
