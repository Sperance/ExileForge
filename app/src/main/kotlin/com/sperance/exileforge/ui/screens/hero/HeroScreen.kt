package com.sperance.exileforge.ui.screens.hero

import com.sperance.exileforge.presentation.state.sellPrice
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.sperance.exileforge.core.display.ItemSearch
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.BodyPlace
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_TREE
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size

/** The Hero tab's four sections, in the order a player reaches for them. */
private enum class HeroSection(val title: String, val icon: ImageVector) {
    CHARACTER("hero.section_character", ForgeGlyphs.Exile), EQUIPMENT("hero.section_equipment", ForgeGlyphs.Helm),
    STASH("hero.section_stash", ForgeGlyphs.Stash), BAG("hero.section_bag", ForgeGlyphs.Orb)
}

/**
 * The hero: who they are, what they wear, and what they own.
 *
 * It used to be one long scroll with the stash — the list a player opens this tab for — at the very
 * bottom, under a character sheet and eleven slots that were mostly empty. Each is its own section
 * now, one tap apart, under a header that says who the character is — the one part they all share.
 * Every item, wherever it is shown, opens the same [ItemSheet].
 *
 * Since 2.22.0 the stash holds only what lies loose — what is worn or socketed is the Equipment
 * section's — and the bag is a section of its own, a list rather than a strip of chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HeroScreen(s: ForgeState, vm: ForgeViewModel) {
    var section by rememberSaveable(s.play.characterId) { mutableStateOf(HeroSection.CHARACTER) }
    var detailId by remember(s.play.characterId) { mutableStateOf<String?>(null) }
    var pickPlace by remember(s.play.characterId) { mutableStateOf<BodyPlace?>(null) }
    var query by remember(s.play.characterId) { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var slot by remember(s.play.characterId) { mutableStateOf("") }
    var stackId by remember(s.play.characterId) { mutableStateOf<String?>(null) }
    var listStack by remember(s.play.characterId) { mutableStateOf<String?>(null) }
    // The stash is two shelves since 2.56.1: gear, and the professions' tools apart from it.
    var tools by rememberSaveable(s.play.characterId) { mutableStateOf(false) }
    // Opening the tab is what refreshes the hero, and only when the last reading has gone cold.
    // Nothing here asks the player to press anything: the pull below is for when they disagree.
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero() }
    val hero = s.play.hero
    // The stash holds everything (2.51.0): what is worn or socketed too, with a gold frame and a badge.
    val stash = hero?.inventory.orEmpty()
    val documents = stash.associate { it.id to inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
    // How many loose items each slot holds (2.47.0): a chip says it, and a slot with none has no chip.
    val shelf = stash.filter { documents.getValue(it.id).text("slot").startsWith(TOOL_SLOT) == tools }
    val slotCounts = shelf.map { documents.getValue(it.id).text("slot") }.filter(String::isNotBlank).groupingBy { it }.eachCount()
    val slots = slotCounts.keys.toList()
    val visible = shelf.filter { instance -> documents[instance.id]?.let { (slot.isBlank() || it.text("slot") == slot) && ItemSearch.matches(it, query) } == true }
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.HERO), onRefresh = vm::loadHero, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // Who the character is heads every section; until the hero arrives the tab says what it is.
                if (hero != null) HeroHeader(s, onTree = { vm.tab(TAB_TREE) }) { vm.tab(TAB_CRAFT) }
                else ScreenHeader(ui("hero.title"), ui("hero.inventory_count", stash.size), ForgeGlyphs.Stash)
            }
            item { SectionBar(section) { section = it } }
            if (hero == null) item { InfoCard(ui("common.loading"), ui("hero.stash_empty_hint")) }
            else when (section) {
                HeroSection.CHARACTER -> item { HeroSummary(s) }
                HeroSection.EQUIPMENT -> {
                    item { HeroVitals(s) }
                    item { EquipmentLedger(s) { place, worn -> if (worn != null) detailId = worn else pickPlace = place } }
                }
                HeroSection.BAG -> {
                    val sections = bagSections(s)
                    if (sections.isEmpty()) item { InfoCard(ui("hero.bag_empty"), ui("bag.empty_hint")) }
                    // A table since 2.75.0: icon and count per cell, everything else behind the tap.
                    else item(key = "bag") { BagGrid(s, sections) { stackId = it } }
                }
                HeroSection.STASH -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(selected = !tools, onClick = { tools = false; slot = "" }, label = { Text(ui("hero.stash_gear")) },
                                    leadingIcon = { Icon(ForgeGlyphs.Helm, null, modifier = Modifier.size(16.dp)) })
                                FilterChip(selected = tools, onClick = { tools = true; slot = "" }, label = { Text(ui("hero.stash_tools")) },
                                    leadingIcon = { Icon(ForgeGlyphs.Anvil, null, modifier = Modifier.size(16.dp)) })
                                Spacer(Modifier.weight(1f))
                                // The search is a glyph at the side since 2.75.0, the field behind it in a dialog.
                                SearchGlyph(active = query.isNotBlank()) { searching = true }
                            }
                            if (query.isNotBlank()) InputChip(selected = true, onClick = { searching = true }, label = { Text(ui("hero.search_chip", query.trim())) },
                                trailingIcon = { Icon(Icons.Outlined.Close, ui("hero.search_clear"), Modifier.size(16.dp).clickable { query = "" }) })
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { FilterChip(selected = slot.isBlank(), onClick = { slot = "" }, label = { Text(ui("hero.slot_count", ui("common.all"), shelf.size)) }) }
                                items(slots) { key -> FilterChip(selected = slot == key, onClick = { slot = key },
                                    label = { Text(ui("hero.slot_count", slotTitle(key, s.lang), slotCounts[key] ?: 0)) }) }
                            }
                        }
                    }
                    if (visible.isEmpty()) item { InfoCard(ui("tree.nothing_found"), ui("hero.stash_empty_hint")) }
                    // A line, not a card: a stash is read down, and the card is one tap behind each line.
                    items(visible, key = { it.id }) { instance ->
                        val worn = instance.equipped || instance.socketed
                        ItemRow(documents.getValue(instance.id), definitions = s.world.definitions,
                            selected = instance.id == s.play.selectedEquipment, worn = worn,
                            // The sheet added up here (2.46.0) says what the template needs, and the merchant's rule what it fetches.
                            // No rarity in words, a map's included (2.73.0): the row's frame already wears it.
                            unwearable = hero.sheet.unwearableBy[instance.equipmentId].orEmpty(), price = s.sellPrice(instance).takeUnless { worn }) {
                            detailId = instance.id; vm.selectEquipment(instance.id)
                        }
                    }
                }
            }
        }
    }
    detailId?.let { id -> ItemSheet(s, vm, id) { detailId = null } }
    if (searching) SearchDialog(query, onDismiss = { searching = false }) { query = it; searching = false }
    stackId?.let { id -> s.play.hero?.bag?.firstOrNull { it.itemId == id } }?.let { stack ->
        BagSheet(s, stack, onDismiss = { stackId = null },
            onForge = { id -> stackId = null; vm.selectOrb(id); vm.openForge(null, ForgeSection.ORBS) },
            onAuction = { id -> stackId = null; listStack = id })
    }
    listStack?.let { id ->
        ListingSheet(s, bagTitle(s, id), owned = s.bagAmount(id) ?: 0L, onDismiss = { listStack = null }) { orb, price, amount ->
            listStack = null; vm.sellItem(id, amount, orb, price)
        }
    }
    pickPlace?.let { place -> SlotPicker(s, place, onDismiss = { pickPlace = null }, onEquip = { instanceId -> vm.equip(instanceId, place.ring) }) }
}

/**
 * The four sections as glyphs over short labels.
 *
 * Four words side by side do not fit a phone's width in both languages, and a row that scrolls
 * hides the section a player is looking for; a drawing over a small label fits and is found first.
 */
@Composable private fun SectionBar(selected: HeroSection, onSelect: (HeroSection) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().background(Abyss)) {
            HeroSection.entries.forEach { entry ->
                val on = entry == selected
                Column(Modifier.weight(1f).selectable(selected = on, role = Role.Tab, onClick = { onSelect(entry) }).padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(entry.icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(20.dp))
                    Text(ui(entry.title), color = if (on) GoldBright else Muted, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, softWrap = false)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) Gold else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = PanelRaised)
    }
}

/** The slots a profession's tool goes in all begin so; the stash shelves them apart. */
private const val TOOL_SLOT = "TOOL_"

/** The stash's search glyph (2.75.0): a lens in a small ring, lit while a query filters the shelf. */
@Composable private fun SearchGlyph(active: Boolean, onClick: () -> Unit) {
    val tint = if (active) GoldBright else Muted
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp).border(1.dp, tint.copy(alpha = .6f), CircleShape)) {
        Icon(Icons.Outlined.Search, ui("hero.find_item"), tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** The field behind the glyph: typed, then found, or cleared; the query is kept until it is. */
@Composable private fun SearchDialog(query: String, onDismiss: () -> Unit, onFind: (String) -> Unit) {
    var text by remember { mutableStateOf(query) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Panel,
        title = { Text(ui("hero.find_item"), color = GoldBright) },
        text = {
            OutlinedTextField(text, { text = it }, placeholder = { Text(ui("hero.search_hint")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onFind(text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus))
        },
        confirmButton = { TextButton(onClick = { onFind(text) }) { Text(ui("hero.search_find")) } },
        dismissButton = { TextButton(onClick = { onFind("") }) { Text(ui("hero.search_clear")) } })
}
