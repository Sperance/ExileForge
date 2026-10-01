package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.itemIcon
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.theme.*

/** The showcase: the server's own search, so the page and the filter both belong to it. */
@Composable internal fun ColumnScope.ShowcaseTab(s: ForgeState, vm: ForgeViewModel) {
    ShowcaseList(s, header = { ShowcaseHeader(s, vm) }, onBuy = vm::buyLot, onMore = vm::moreShowcase)
}

/**
 * The lots themselves, the next page added under them by «Показать ещё» while the server has one.
 *
 * The filter travels in as a header so the list can be driven without a view model: what is worth
 * checking here is which lot offers a Buy button and what the price says, not the form wiring.
 *
 * Buying happens in the lot's own sheet and nowhere else. A line in a list is not enough to decide
 * on — the rolls are what a trader is paying for — and a purchase is irreversible, so it is worth
 * the extra tap that guarantees the item was looked at.
 */
@Composable internal fun ColumnScope.ShowcaseList(s: ForgeState, header: @Composable () -> Unit = {},
    onBuy: (String) -> Unit, onMore: () -> Unit) {
    var openLot by remember { mutableStateOf<String?>(null) }
    var confirmBuy by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item { header() }
        if (s.market.showcase.items.isEmpty()) item {
            InfoCard(ui("tree.nothing_found"),
                ui("auction.showcase_empty"))
        }
        items(s.market.showcase.items, key = { it.id }) { lot ->
            // A seller cannot buy their own lot, and the server says so; the sheet does not offer it.
            LotRow(s, lot, mark = if (lot.belongsTo(s.play.heroId)) ui("auction.your_lot") else null) { openLot = lot.id }
        }
        val showcase = s.market.showcase
        if (showcase.next != null) item {
            ForgeOutlinedButton(enabled = !s.busy, onClick = onMore, modifier = Modifier.fillMaxWidth()) {
                Text(ui("auction.more", showcase.items.size, showcase.totalItems))
            }
        }
    }
    s.market.showcase.items.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = ui("auction.buy"), enabled = !s.busy && !lot.belongsTo(s.play.heroId),
            note = if (lot.belongsTo(s.play.heroId)) ui("auction.own_lot") else null,
            onDismiss = { openLot = null }) { openLot = null; confirmBuy = lot.id }
    }
    // A purchase cannot be undone, so it is asked about — and an item the hero cannot wear
    // is said so in the same breath, because that is exactly the mistake worth catching.
    s.market.showcase.items.firstOrNull { it.id == confirmBuy }?.let { lot ->
        val blocked = lotUnmet(s, lot)
        val orb = orbTitle(lot)
        // What the bag keeps after paying: shown when the bag is known and can pay; when it cannot,
        // the sheet says so and the purchase is not sent (2.46.0).
        val have = s.bagAmount(lot.priceOrb)
        val money = s.hero?.money
        val fee = lot.fee
        val poor = money != null && money < fee
        ConfirmSheet(
            title = ui("auction.buy_q"), subtitle = lot.title,
            icon = { LotIcon(s, lot, Modifier.size(44.dp)) },
            ledger = listOfNotNull(
                LedgerLine(ui("confirm.spend"), ui("confirm.minus", lot.price, orb), Tone.SPEND),
                fee.takeIf { it > 0 }?.let { LedgerLine(ui("auction.fee"), ui("merchant.gold_amount", it), Tone.SPEND) },
                have?.takeIf { it >= lot.price }?.let { LedgerLine(ui("confirm.left"), ui("confirm.amount", it - lot.price, orb)) },
                LedgerLine(ui("confirm.gain"), lot.title, Tone.GAIN),
                LedgerLine(ui("auction.seller"), sellerName(lot)),
            ),
            warning = listOfNotNull(
                have?.takeIf { it < lot.price }?.let { ui("confirm.short", it) },
                ui("auction.fee_short", fee).takeIf { poor },
                blocked.takeIf { it.isNotEmpty() }?.let {
                    ui("auction.unwearable", it.joinToString(", ") { r -> requirementReason(r, s.lang) })
                },
            ).joinToString("\n").ifBlank { null },
            blocked = (have != null && have < lot.price) || poor,
            confirm = ui("auction.buy_do"),
            onDismiss = { confirmBuy = null }) { onBuy(lot.id) }
    }
}

/**
 * The showcase's head: the name to search for, the button to every other filter, and the filters
 * already set as chips.
 *
 * Nothing here asks the server on its own: the search key of the keyboard, «Показать» in the sheet
 * and a chip's cross do. A chip is one filter the server understands, named as the player set it,
 * and its cross drops that filter and asks again — the quickest way back from "nothing found".
 */
@Composable private fun ShowcaseHeader(s: ForgeState, vm: ForgeViewModel) {
    var sheet by remember { mutableStateOf(false) }
    val f = s.market.filter
    val count = f.active().size + if (s.market.showOwnLots) 1 else 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(f.title, { vm.auctionFilter(f.copy(title = it)) }, placeholder = { Text(ui("auction.name")) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.loadShowcase() }))
            ForgeOutlinedButton(onClick = { sheet = true }, enabled = !s.busy, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Outlined.FilterList, ui("auction.filters"), modifier = Modifier.size(18.dp))
                if (count > 0) { Spacer(Modifier.width(6.dp)); Text(count.toString()) }
            }
        }
        // The filters set are one line of chips that scrolls sideways, never a block growing down over the lots.
        if (count > 0) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            f.active().forEach { field ->
                ActiveFilter(chipLabel(s, field, f.value(field))) { vm.auctionFilter(f.without(field)); vm.loadShowcase() }
            }
            if (s.market.showOwnLots) ActiveFilter(ui("auction.show_mine")) { vm.showOwnLots(false); vm.loadShowcase() }
        }
    }
    if (sheet) FilterSheet(s, onDismiss = { sheet = false }) { filter, mine ->
        sheet = false; vm.auctionFilter(filter); vm.showOwnLots(mine); vm.loadShowcase()
    }
}

/** A filter that is set, named by its value, with a cross that drops it. */
@Composable private fun ActiveFilter(label: String, onClear: () -> Unit) {
    InputChip(selected = true, onClick = onClear, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Outlined.Close, ui("auction.remove_filter"), modifier = Modifier.size(16.dp)) },
        colors = InputChipDefaults.inputChipColors(selectedContainerColor = Gold, selectedLabelColor = Ink, selectedTrailingIconColor = Ink))
}

private fun chipLabel(s: ForgeState, field: FilterField, value: String): String = when (field) {
    FilterField.KIND -> LotKind.entries.firstOrNull { it.name == value }?.let { lotKindTitle(it, s.lang) } ?: value
    FilterField.SLOT -> slotTitle(value, s.lang)
    FilterField.RARITY -> rarityTitle(value, s.lang)
    FilterField.MIN_LEVEL -> ui("auction.chip_ilvl_from", value)
    FilterField.MAX_LEVEL -> ui("auction.chip_ilvl_to", value)
    FilterField.ORB -> ui("auction.chip_orb", itemTitle(value))
    FilterField.MAX_PRICE -> ui("auction.chip_max_price", value)
    FilterField.SELLER -> ui("auction.chip_seller", value.takeLast(6))
}

/** The slots a template may have: the second ring and the belt's other flasks are places of the worn, never of a lot. */
private val templateSlots: List<Slot> = Slot.entries.filter { it != Slot.RING_2 && it != Slot.FLASK_2 && it != Slot.FLASK_3 }

/**
 * Every filter the server understands, on a draft: nothing reaches the showcase until «Показать»,
 * so trying a combination costs no request, and «Сбросить» clears all but the typed name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FilterSheet(s: ForgeState, onDismiss: () -> Unit, onApply: (AuctionFilter, Boolean) -> Unit) {
    var draft by remember { mutableStateOf(s.market.filter) }
    var mine by remember { mutableStateOf(s.market.showOwnLots) }
    val any = ui("common.all")
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("auction.filters"))
            Spinner(ui("auction.what_sold"), draft.kind,
                mapOf("" to any) + LotKind.entries.associate { it.name to lotKindTitle(it, s.lang) }, true, glyph = Glyph.ITEM) { draft = draft.copy(kind = it) }
            Spinner(ui("common.slot"), draft.slot, mapOf("" to any) + templateSlots.associate { it.name to slotTitle(it, s.lang) }, true, glyph = Glyph.ITEM) { draft = draft.copy(slot = it) }
            Spinner(ui("common.rarity"), draft.rarity, mapOf("" to any) + Rarity.entries.associate { it.name to rarityTitle(it, s.lang) }, true, glyph = Glyph.RARITY) { draft = draft.copy(rarity = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.minItemLevel, { draft = draft.copy(minItemLevel = it.filter(Char::isDigit)) }, label = { Text(ui("auction.ilvl_from")) },
                    singleLine = true, keyboardOptions = digits, modifier = Modifier.weight(1f))
                OutlinedTextField(draft.maxItemLevel, { draft = draft.copy(maxItemLevel = it.filter(Char::isDigit)) }, label = { Text(ui("auction.ilvl_to")) },
                    singleLine = true, keyboardOptions = digits, modifier = Modifier.weight(1f))
            }
            // The price's orb is an item code (3.0.0): the auction's currencies, in the order of their price.
            Spinner(ui("auction.priced_in"), draft.priceOrb,
                mapOf("" to any) + orbOptions(s), true, glyph = Glyph.CURRENCY, optionArt = orbArt(s.currencies)) { draft = draft.copy(priceOrb = it) }
            OutlinedTextField(draft.maxPrice, { draft = draft.copy(maxPrice = it.filter(Char::isDigit)) }, label = { Text(ui("auction.price_max")) },
                singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
            // A seller is named by the hero's id: there is no catalogue of heroes to pick one from.
            OutlinedTextField(draft.sellerId, { draft = draft.copy(sellerId = it.trim()) }, label = { Text(ui("auction.seller")) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            // Own lots cannot be bought, so they are dropped unless a seller wants to compare prices.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ui("auction.show_mine"), modifier = Modifier.weight(1f))
                Switch(checked = mine, onCheckedChange = { mine = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeOutlinedButton(onClick = { draft = draft.cleared(); mine = false }, modifier = Modifier.weight(1f)) { Text(ui("auction.reset")) }
                ForgeButton(enabled = !s.busy, onClick = { onApply(draft, mine) }, modifier = Modifier.weight(1f)) { Text(ui("auction.apply")) }
            }
        }
    }
}

/** The hero's own lots that are still on sale; withdrawn and sold lots leave this list. */
@Composable internal fun ColumnScope.MyLotsTab(s: ForgeState, vm: ForgeViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        // Lot places (3.47.0): the same for every hero, never bought.
        s.market.slots?.let { slots ->
            item {
                ForgePanel {
                    PropertyRow(ui("auction.slots"), ui("auction.slots_value", slots.used, slots.limit), Glyph.ITEM)
                    if (slots.full) Text(ui("auction.slots_full"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (s.ownLots.isEmpty()) item {
            InfoCard(ui("auction.no_lots"), ui("auction.no_lots_hint"))
        }
        items(s.ownLots, key = { it.id }) { lot ->
            LotRow(s, lot, mark = lotExpiry(lot), withSeller = false) { openLot = lot.id }
        }
    }
    s.ownLots.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = ui("auction.withdraw"), enabled = !s.busy,
            note = lotExpiry(lot), onDismiss = { openLot = null }) { openLot = null; vm.cancelLot(lot.id) }
    }
}

/**
 * One lot as a line (variant A): everything a trader decides on without opening it — what it is, every line it
 * rolled, who sells it, and the price opposite. A copy is read through its view as the stash reads it; a stack has
 * no rolls, and its line is the lot's own: the item's icon, its name and how many.
 *
 * Rarity is not written anywhere: it is the frame of the icon and the colour of the name.
 */
@Composable private fun LotRow(s: ForgeState, lot: AuctionLot, mark: String?, withSeller: Boolean = true, onClick: () -> Unit) {
    var orbInfo by remember { mutableStateOf(false) }
    if (orbInfo) StackInfoSheet(s, lot.priceOrb) { orbInfo = false }
    val price: @Composable ColumnScope.() -> Unit = { LotPrice(lot) { orbInfo = true } }
    val seller = listOfNotNull(sellerName(lot).takeIf { withSeller })
    val view = lot.equipment?.let { s.view(it) }
    // The rules' verdict on the template, as the stash marks it: a lot the buyer cannot wear yet says why instead of its facts.
    if (view != null) ItemTradeRow(view, enabled = !s.busy, unmet = lotUnmet(s, lot), extra = seller, mark = mark, onClick = onClick, price = price)
    else TradeRow(lot.title, lotColor(lot), stackFacts(s, lot) + seller, emptyList(), enabled = !s.busy, mark = mark, onClick = onClick,
        icon = { LotIcon(s, lot, Modifier.size(28.dp)) }, price = price)
}

/**
 * The price opposite the name: the count and the orb in its own glass (2.69.0) — the plain glyph for one the client has no art
 * for — and the orb's name under them, since the glasses of the lesser orbs look alike; a tap on a known orb's glass opens it.
 */
@Composable private fun LotPrice(lot: AuctionLot, onOrb: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(lot.price.toString(), color = Vital, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Orb.of(lot.priceOrb)?.let { OrbGlyph(it, Modifier.clickable(onClickLabel = orbTitle(lot), onClick = onOrb).padding(2.dp).size(16.dp)) }
            ?: Icon(ForgeGlyphs.Orb, orbTitle(lot), tint = Gold, modifier = Modifier.size(15.dp))
    }
    Text(orbTitle(lot), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 96.dp))
}

/** How long an own lot still stands (server 1.30.0): days and hours, hours and minutes on its last day; null when it names no end. */
private fun lotExpiry(lot: AuctionLot): String? {
    val minutes = ((lot.timeLeft() ?: return null) + 59_999) / 60_000
    return if (minutes >= MINUTES_A_DAY) ui("auction.left_days", minutes / MINUTES_A_DAY, minutes % MINUTES_A_DAY / 60)
    else ui("auction.left_hours", minutes / 60, minutes % 60)
}

private const val MINUTES_A_DAY = 1_440L

/**
 * The lot's drawing: the server's sprite for its code, the bundled emblem of its kind otherwise — a
 * copy by its template, a stack by the item of the bag it is a stack of.
 */
@Composable private fun LotIcon(s: ForgeState, lot: AuctionLot, modifier: Modifier) {
    val sprite = if (lot.kind == LotKind.EQUIPMENT) equipmentIcon(lot.itemCode) else itemIcon(lot.itemCode)
    if (!SpriteIcon(sprite, lotColor(lot), modifier, halo = lot.kind == LotKind.EQUIPMENT)) ItemEmblem(lotVisualKind(s, lot), lotColor(lot), modifier)
}

private fun lotVisualKind(s: ForgeState, lot: AuctionLot): ItemVisualKind = when (lot.kind) {
    LotKind.EQUIPMENT -> s.index?.template(lot.itemCode)?.let(::itemVisualKind) ?: ItemVisualKind.ITEM
    LotKind.ITEM -> s.index?.item(lot.itemCode)?.let(::bagVisualKind) ?: ItemVisualKind.ITEM
}

/** The rarity's colour for a copy; a stack has none and keeps bone white. */
private fun lotColor(lot: AuctionLot) = rarityColor(lot.rarity?.name.orEmpty())

private fun sellerName(lot: AuctionLot): String = lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }

/** The requirements the lot's template misses against the hero's sheet; empty for a stack, and for a wearable copy. */
private fun lotUnmet(s: ForgeState, lot: AuctionLot): List<String> = lot.equipment?.let { s.unmetFor(it.template) }.orEmpty()

/** A stack lot's facts: how many, or what kind of goods when it is one. */
private fun stackFacts(s: ForgeState, lot: AuctionLot): List<String> =
    listOf(if (lot.amount > 1) ui("auction.pieces", lot.amount) else lotKindTitle(lot.kind, s.lang))

/**
 * One lot in full, with the goods drawn as the stash draws them.
 *
 * The copy travels inside the lot, rolls and all, and its template is in the content every hero
 * reads once: looking at a lot costs no request at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LotSheet(s: ForgeState, lot: AuctionLot, action: String, enabled: Boolean, note: String?,
    onDismiss: () -> Unit, onAction: () -> Unit) {
    val view = lot.equipment?.let { s.view(it) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (view != null) item {
                ItemCard(view, enabled = false, detailed = true, actionLabel = ui("auction.lot") + " · ${lot.id.takeLast(6)}")
            }
            item {
                ForgePanel {
                    Engraved(ui("auction.lot"))
                    if (view == null) Text(lot.title, color = Gold, style = MaterialTheme.typography.titleMedium)
                    if (lot.kind == LotKind.ITEM) PropertyRow(ui("auction.amount"), lot.amount.toString(), Glyph.ITEM)
                    // The price is always counted in orbs; the content gives the orb its name.
                    PropertyRow(ui("card.price"), orbPrice(lot), Glyph.CURRENCY)
                    PropertyRow(ui("auction.seller"), sellerName(lot), Glyph.CHARACTER)
                    listedAt(lot.createdAt)?.let { PropertyRow(ui("auction.listed_at"), it, Glyph.LEVEL) }
                    note?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
                    ForgeButton(enabled = enabled, onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(action) }
                }
            }
        }
    }
}

/**
 * When the lot was listed, in the zone the device is standing in.
 *
 * The server writes the stamp in UTC — its `LocalDateTime.now()` is built from
 * `TimeZone.UTC` — so it carries no zone of its own and this is the one place that gives it one.
 * A stamp that will not parse is simply not shown: a wrong time is worse than no time.
 */
private fun listedAt(stamp: String): String? {
    if (stamp.isBlank()) return null
    return try {
        java.time.LocalDateTime.parse(stamp)
            .atZone(java.time.ZoneOffset.UTC)
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: Exception) { null }
}

/** A lot's price, in the orb it was set in; an orb the rules do not know keeps its tail as a name. */
private fun orbPrice(lot: AuctionLot): String = ui("confirm.amount", lot.price, orbTitle(lot))

private fun orbTitle(lot: AuctionLot): String =
    if (Orb.of(lot.priceOrb) != null) itemTitle(lot.priceOrb) else ui("auction.orb_id", lot.priceOrb.takeLast(6))
