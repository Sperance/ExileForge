package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** The Hero tab's three sections, in the order a player reaches for them. */
private enum class HeroSection(val title: String) {
    CHARACTER("hero.section_character"), EQUIPMENT("hero.section_equipment"), STASH("hero.section_stash")
}

/**
 * The hero: who they are, what they wear, and what they own.
 *
 * It used to be one long scroll with the stash — the list a player opens this tab for — at the very
 * bottom, under a character sheet and eleven slots that were mostly empty. Each is its own section
 * now, one tap apart. The forge sits in the header because it is reached from here but is not a
 * part of the hero. Every item, wherever it is shown, opens the same [ItemSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HeroScreen(s: ForgeState, vm: ForgeViewModel) {
    var section by rememberSaveable(s.play.characterId) { mutableStateOf(HeroSection.CHARACTER) }
    var detailId by remember(s.play.characterId) { mutableStateOf<String?>(null) }
    var pickSlot by remember(s.play.characterId) { mutableStateOf<String?>(null) }
    var query by remember(s.play.characterId) { mutableStateOf("") }
    var slot by remember(s.play.characterId) { mutableStateOf("") }
    // Opening the tab is what refreshes the hero, and only when the last reading has gone cold.
    // Nothing here asks the player to press anything: the pull below is for when they disagree.
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero() }
    val hero = s.play.hero
    val stash = hero?.inventory.orEmpty()
    val documents = stash.associate { it.id to inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
    val slots = documents.values.map { it.text("slot") }.filter(String::isNotBlank).distinct()
    val visible = stash.filter { instance -> documents[instance.id]?.let { (slot.isBlank() || it.text("slot") == slot) && it.text("name").contains(query, true) } == true }
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.HERO), onRefresh = vm::loadHero, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) { ScreenHeader(ui("hero.title"), ui("hero.inventory_count", stash.size), ForgeGlyphs.Stash) }
                    IconButton(enabled = !s.busy, onClick = { vm.tab(TAB_CRAFT) }) {
                        Icon(ForgeGlyphs.Tome, ui("nav.craft"), tint = Gold, modifier = Modifier.size(24.dp))
                    }
                }
            }
            item {
                TabRow(selectedTabIndex = section.ordinal, containerColor = Abyss) {
                    HeroSection.entries.forEach { entry ->
                        Tab(selected = section == entry, onClick = { section = entry },
                            text = { Text(ui(entry.title), style = MaterialTheme.typography.labelLarge) })
                    }
                }
            }
            if (hero == null) item { InfoCard(ui("common.loading"), ui("hero.stash_empty_hint")) }
            else when (section) {
                HeroSection.CHARACTER -> item { HeroSummary(s) }
                HeroSection.EQUIPMENT -> item {
                    EquipmentGrid(s) { bodySlot, worn -> if (worn != null) detailId = worn else pickSlot = bodySlot }
                }
                HeroSection.STASH -> {
                    item { BagPanel(s) }
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
                    if (visible.isEmpty()) item { InfoCard(ui("tree.nothing_found"), ui("hero.stash_empty_hint")) }
                    // A line, not a card: a stash is read down, and the card is one tap behind each line.
                    items(visible, key = { it.id }) { instance ->
                        val inactive = hero.inactive[instance.id] != null
                        ItemRow(documents.getValue(instance.id), definitions = s.world.definitions,
                            selected = instance.id == s.play.selectedEquipment,
                            // The server's verdict on the template, not a requirement worked out here.
                            // Something already worn is judged by `inactive` instead: it is in a slot,
                            // and "cannot be worn" would be an odd thing to say about it.
                            unwearable = if (instance.equipped) emptyList() else hero.sheet.unwearableBy[instance.equipmentId].orEmpty(),
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
    }
    detailId?.let { id -> ItemSheet(s, vm, id) { detailId = null } }
    pickSlot?.let { bodySlot -> SlotPicker(s, bodySlot, onDismiss = { pickSlot = null }, onEquip = { instanceId -> vm.equip(instanceId, bodySlot) }) }
}
