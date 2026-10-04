package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.SlotGroup
import com.sperance.exileforge.presentation.state.StashFilter
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_SKILLS
import com.sperance.exileforge.presentation.state.stashShelf
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** The Hero tab's sections, in the order a player reaches for them; the menagerie (3.5.0) moved to «Развитие». */
private enum class HeroSection(val title: String, val icon: ImageVector) {
    CHARACTER("hero.section_character", ForgeGlyphs.Exile),
    EQUIPMENT("hero.section_equipment", ForgeGlyphs.Helm),
    STASH("hero.section_stash", ForgeGlyphs.Stash),
    BAG("hero.section_bag", ForgeGlyphs.Orb),
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
 * section's — and the bag is a section of its own, a list rather than a strip of chips. Since 3.0.0
 * every copy is read through its view over the content on the device, and the bag is keyed by item code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroScreen() {
    val game by koinViewModel<HeroViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val model = koinViewModel<HeroViewModel>()
    val smithy = koinViewModel<SmithyViewModel>()
    val heroId = game.heroId
    var section by rememberSaveable(heroId) { mutableStateOf(HeroSection.CHARACTER) }
    var detailId by remember(heroId) { mutableStateOf<String?>(null) }
    var pickPlace by remember(heroId) { mutableStateOf<BodyPlace?>(null) }
    // The filters (3.30.0) are the screen's own and go with it; the order is kept on the device.
    var filter by remember(heroId) { mutableStateOf(StashFilter()) }
    var filtering by remember { mutableStateOf(false) }
    var stackCode by remember(heroId) { mutableStateOf<String?>(null) }
    var listStack by remember(heroId) { mutableStateOf<String?>(null) }
    // The stash is two shelves since 2.56.1: gear, and the professions' tools apart from it.
    var tools by rememberSaveable(heroId) { mutableStateOf(false) }
    // Opening the tab is what refreshes the hero, and only when the last reading has gone cold.
    // Nothing here asks the player to press anything: the pull below is for when they disagree.
    LaunchedEffect(heroId, game.sessionEpoch) { model.ensure() }
    val hero = game.hero
    // The stash holds everything (2.51.0): what is worn or socketed too, with a gold frame and a badge.
    // A copy whose template the content does not hold is left out rather than drawn blank.
    // Remembered (3.55.0): a thousand views, the filter and the sort by price were rebuilt on every tick of the state.
    // Keyed by the copies rather than the hero (3.66.0): a view rebuilt for a changed purse would redraw every line.
    val stash = remember(hero?.items, game.index, game.world) { hero?.items.orEmpty().mapNotNull { game.view(it) } }
    // How many items each slot group holds (2.47.0, grouped since 3.30.0): a chip says it, and a group with none has no chip.
    val shelf = remember(stash, tools) { stash.filter { it.slot.isTool == tools } }
    val groupCounts = remember(shelf) { shelf.groupingBy { SlotGroup.of(it.slot) }.eachCount() }
    val rarities = remember(shelf) { shelf.map { it.rarity }.distinct().sortedByDescending { it.ordinal } }
    // The shelf reads the sheet (what can be worn, what the merchant pays), not the rest of the hero.
    // «Hide equipped» (3.69.0) is the gear shelf's: a tool shelf shows everything it holds.
    val hideWorn = game.stashHideWorn && !tools
    val visible = remember(shelf, filter, game.stashSort, hideWorn, hero?.level, hero?.stats, game.world) { game.stashShelf(shelf, filter, hideWorn) }
    val tweaks = stashTweaks(filter, game.stashSort, showsWorn = !game.stashHideWorn && !tools)
    // Each part is handed its own cut (3.66.0): a changed purse redraws the header, not the ledger or the stash.
    val header = rememberHeroHeader(game)
    val equipment = rememberEquipment(game)
    val lines = rememberStashLines(game, visible)
    val selected = game.holding.selectedEquipment
    PullToRefreshBox(isRefreshing = game.refreshing(Reads.HERO), onRefresh = model::load, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // Who the character is heads every section; until the hero arrives the tab says what it is.
                if (header != null) {
                    HeroHeader(header)
                } else {
                    ScreenHeader(ui("hero.title"), ui("hero.inventory_count", stash.size), ForgeGlyphs.Stash, guide = Guide.HERO)
                }
            }
            item { SectionBar(section) { section = it } }
            if (hero == null) {
                item { InfoCard(ui("common.loading"), ui("hero.stash_empty_hint")) }
            } else {
                when (section) {
                    HeroSection.CHARACTER -> {
                        item { HeroSummary(game) }
                    }

                    HeroSection.EQUIPMENT -> {
                        item { equipment?.let { EquipmentLedger(it) { place, worn -> if (worn != null) detailId = worn else pickPlace = place } } }
                    }

                    HeroSection.BAG -> {
                        val sections = bagSections(game)
                        if (sections.isEmpty()) {
                            item { InfoCard(ui("hero.bag_empty"), ui("bag.empty_hint")) }
                        } // A table since 2.75.0: icon and count per cell, everything else behind the tap.
                        else {
                            item(key = "bag") { BagGrid(game, sections) { stackCode = it } }
                        }
                    }

                    HeroSection.STASH -> {
                        if (hero.overflow.isNotEmpty()) item(key = "overflow") { StashOverflow(game, model) }
                        // Two rows since 3.69.0: the count beside the switch squeezed the filter glyph off its shape.
                        // The switch takes what is left after the glyph, never the other way round.
                        item(key = "shelf") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(
                                        selected = !tools,
                                        onClick = {
                                            tools = false
                                            filter = filter.copy(groups = emptySet())
                                        },
                                        label = { Text(ui("hero.stash_gear"), maxLines = 1) },
                                        leadingIcon = { Icon(ForgeGlyphs.Helm, null, modifier = Modifier.size(16.dp)) },
                                    )
                                    FilterChip(
                                        selected = tools,
                                        onClick = {
                                            tools = true
                                            filter = filter.copy(groups = emptySet())
                                        },
                                        label = { Text(ui("hero.stash_tools"), maxLines = 1) },
                                        leadingIcon = { Icon(ForgeGlyphs.Anvil, null, modifier = Modifier.size(16.dp)) },
                                    )
                                }
                                // Search, order and filters live behind one glyph since 3.67.0: the shelf keeps the screen.
                                StashFilterButton(tweaks) { filtering = true }
                            }
                        }
                        // The places held of how many across the whole width, and a «+» for the next pack.
                        item(key = "fill") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                StashFill(game, model, Modifier.weight(1f))
                                if (!tools) HideWornChip(game.stashHideWorn, shell::stashHideWorn)
                            }
                        }
                        if (visible.isEmpty()) item { InfoCard(ui("tree.nothing_found"), if (filter.active || hideWorn) ui("stash.filter_empty") else ui("hero.stash_empty_hint")) }
                        // A line, not a card: a stash is read down, and the card is one tap behind each line.
                        items(lines, key = { it.piece.id }) { line ->
                            val piece = line.piece
                            // No rarity in words, a map's included (2.73.0): the row's frame already wears it.
                            ItemRow(
                                piece,
                                selected = piece.id == selected,
                                worn = line.worn,
                                unwearable = line.unwearable,
                                price = line.price,
                                waiting = line.waiting,
                            ) {
                                detailId = piece.id
                                model.selectEquipment(piece.id)
                            }
                        }
                    }
                }
            }
        }
    }
    detailId?.let { id -> ItemSheet(game, model, id) { detailId = null } }
    if (filtering) {
        StashFilterSheet(
            filter, game.stashSort, game.lang, shelf.size, groupCounts, rarities, onFilter = { filter = it },
            onSort = shell::stashSort, hideWorn = game.stashHideWorn.takeUnless { tools }, onHideWorn = shell::stashHideWorn, onDismiss = { filtering = false },
        )
    }
    // The sheet is about a stack the bag still holds: listed or read away, it closes with it.
    stackCode?.let { code ->
        hero?.bag?.get(code)?.takeIf { it > 0 }?.let { amount ->
            BagSheet(
                game,
                BagStack(code, amount),
                onDismiss = { stackCode = null },
                onForge = { orb ->
                    stackCode = null
                    smithy.selectOrb(orb)
                    smithy.open(null, ForgeSection.ORBS)
                    shell.tab(TAB_CRAFT)
                },
                onAuction = { stack ->
                    stackCode = null
                    listStack = stack
                },
                // A book is read where it lies, and its page opens in the grimoire (2.78.0); an essence goes to the forge.
                onRead = { skill ->
                    stackCode = null
                    model.learnSkill(skill)
                    shell.tab(TAB_SKILLS)
                },
                onEssence = { essence ->
                    stackCode = null
                    smithy.selectEssence(essence)
                    smithy.open(null, ForgeSection.ESSENCES)
                    shell.tab(TAB_CRAFT)
                },
                onOpenChest = { chest ->
                    stackCode = null
                    model.openChest(chest)
                },
            )
        }
    }
    game.holding.chest?.let { opening -> ChestOpenedSheet(game, opening, model::dismissChest) }
    listStack?.let { code ->
        ListingSheet(game, itemTitle(code), owned = game.bagAmount(code) ?: 0L, onDismiss = { listStack = null }, hint = { model.priceHint(code, null, 0) }) { orb, price, amount ->
            listStack = null
            model.sellItem(code, amount, orb, price)
        }
    }
    // The place goes with the pick: a ring chosen for the second line lands in the second ring, a flask in its own bay.
    pickPlace?.let { place -> SlotPicker(game, place, onDismiss = { pickPlace = null }, onEquip = { itemId -> model.equip(itemId, place.place) }) }
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
                Column(
                    Modifier.weight(1f).selectable(selected = on, role = Role.Tab, onClick = { onSelect(entry) }).padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(entry.icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(20.dp))
                    Text(
                        ui(entry.title),
                        color = if (on) GoldBright else Muted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) Gold else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = PanelRaised)
    }
}
