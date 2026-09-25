package com.sperance.exileforge.ui.screens.auction

import com.sperance.exileforge.core.display.Glyph

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.rarities
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The showcase: the server's own search, so the page and the filter both belong to it. */
@Composable internal fun ColumnScope.ShowcaseTab(s: ForgeState, vm: ForgeViewModel) {
    ShowcaseList(s, header = { ShowcaseHeader(s, vm) }, onBuy = vm::buyLot, onPage = vm::loadShowcase, loadBase = vm::equipmentBase)
}

/**
 * The lots themselves, with the paging the server reported.
 *
 * The filter travels in as a header so the list can be driven without a view model: what is worth
 * checking here is which lot offers a Buy button and what the price says, not the form wiring.
 *
 * Buying happens in the lot's own sheet and nowhere else. A line in a list is not enough to decide
 * on — the rolls are what a trader is paying for — and a purchase is irreversible, so it is worth
 * the extra tap that guarantees the item was looked at.
 */
@Composable internal fun ColumnScope.ShowcaseList(s: ForgeState, header: @Composable () -> Unit = {},
    onBuy: (String) -> Unit, onPage: (Int) -> Unit, loadBase: suspend (String) -> JsonObject? = { null }) {
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
            LotRow(s, lot, note = if (lot.belongsTo(s.play.characterId)) ui("auction.your_lot") else null) { openLot = lot.id }
        }
        if (s.market.showcase.totalPages > 1) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(enabled = !s.busy && s.market.showcase.page > 0, onClick = { onPage(s.market.showcase.page - 1) }) { Text(ui("common.back")) }
                Text(ui("auction.page", s.market.showcase.page + 1, s.market.showcase.totalPages), color = Muted)
                OutlinedButton(enabled = !s.busy && s.market.showcase.page + 1 < s.market.showcase.totalPages, onClick = { onPage(s.market.showcase.page + 1) }) { Text(ui("auction.forward")) }
            }
        }
    }
    s.market.showcase.items.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = ui("auction.buy"), enabled = !s.busy && !lot.belongsTo(s.play.characterId),
            note = if (lot.belongsTo(s.play.characterId)) ui("auction.own_lot") else null,
            loadBase = loadBase, onDismiss = { openLot = null }) { openLot = null; confirmBuy = lot.id }
    }
    // A purchase cannot be undone, so it is asked about — and an item the character cannot wear
    // is said so in the same breath, because that is exactly the mistake worth catching.
    s.market.showcase.items.firstOrNull { it.id == confirmBuy }?.let { lot ->
        val blocked = s.play.hero?.sheet?.unwearableBy?.get(lot.equipment?.equipmentId.orEmpty()).orEmpty()
        val document = lotDocument(s, lot)
        val orb = orbTitle(s, lot)
        // What the bag keeps after paying: shown when the bag is known and can pay; when it cannot,
        // the sheet says so and the purchase is not sent (2.46.0).
        val have = s.bagAmount(lot.priceOrbId)
        ConfirmSheet(
            title = ui("auction.buy_q"), subtitle = lot.title,
            icon = { ItemIcon(document, rarityColor(document.text("rarity")), Modifier.size(44.dp)) },
            ledger = listOfNotNull(
                LedgerLine(ui("confirm.spend"), ui("confirm.minus", lot.price, orb), Tone.SPEND),
                have?.takeIf { it >= lot.price }?.let { LedgerLine(ui("confirm.left"), ui("confirm.amount", it - lot.price, orb)) },
                LedgerLine(ui("confirm.gain"), lot.title, Tone.GAIN),
                LedgerLine(ui("auction.seller"), lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }),
            ),
            note = ui("auction.buy_note"),
            warning = listOfNotNull(
                have?.takeIf { it < lot.price }?.let { ui("confirm.short", it) },
                blocked.takeIf { it.isNotEmpty() }?.let {
                    ui("auction.unwearable", it.joinToString(", ") { r -> requirementReason(r, s.lang) })
                },
            ).joinToString("\n").ifBlank { null },
            blocked = have != null && have < lot.price,
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
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ShowcaseHeader(s: ForgeState, vm: ForgeViewModel) {
    var sheet by remember { mutableStateOf(false) }
    val f = s.market.filter
    val count = f.active().size + if (s.market.showOwnLots) 1 else 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(f.title, { vm.auctionFilter(f.copy(title = it)) }, placeholder = { Text(ui("auction.name")) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.loadShowcase(0) }))
            OutlinedButton(onClick = { sheet = true }, enabled = !s.busy, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Outlined.FilterList, ui("auction.filters"), modifier = Modifier.size(18.dp))
                if (count > 0) { Spacer(Modifier.width(6.dp)); Text(count.toString()) }
            }
        }
        if (count > 0) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            f.active().forEach { field ->
                ActiveFilter(chipLabel(s, field, f.value(field))) { vm.auctionFilter(f.without(field)); vm.loadShowcase(0) }
            }
            if (s.market.showOwnLots) ActiveFilter(ui("auction.show_mine")) { vm.showOwnLots(false); vm.loadShowcase(0) }
        }
    }
    if (sheet) FilterSheet(s, onDismiss = { sheet = false }) { filter, mine ->
        sheet = false; vm.auctionFilter(filter); vm.showOwnLots(mine); vm.loadShowcase(0)
    }
}

/** A filter that is set, named by its value, with a cross that drops it. */
@Composable private fun ActiveFilter(label: String, onClear: () -> Unit) {
    InputChip(selected = true, onClick = onClear, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Outlined.Close, ui("auction.remove_filter"), modifier = Modifier.size(16.dp)) },
        colors = InputChipDefaults.inputChipColors(selectedContainerColor = Gold, selectedLabelColor = Ink, selectedTrailingIconColor = Ink))
}

private fun chipLabel(s: ForgeState, field: FilterField, value: String): String = when (field) {
    FilterField.KIND -> runCatching { lotKindTitle(AuctionLotKind.valueOf(value), s.lang) }.getOrDefault(value)
    FilterField.SLOT -> slotTitle(value, s.lang)
    FilterField.RARITY -> rarityTitle(value, s.lang)
    FilterField.MIN_LEVEL -> ui("auction.chip_ilvl_from", value)
    FilterField.MAX_LEVEL -> ui("auction.chip_ilvl_to", value)
    FilterField.ORB -> ui("auction.chip_orb", s.world.orbs.firstOrNull { it.id == value }?.title(s.lang) ?: value.takeLast(6))
    FilterField.MAX_PRICE -> ui("auction.chip_max_price", value)
    FilterField.SELLER -> ui("auction.chip_seller", value.takeLast(6))
}

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
                mapOf("" to any) + AuctionLotKind.entries.associate { it.name to lotKindTitle(it, s.lang) }, true, glyph = Glyph.ITEM) { draft = draft.copy(kind = it) }
            Spinner(ui("common.slot"), draft.slot, mapOf("" to any) + slots.associateWith { slotTitle(it, s.lang) }, true, glyph = Glyph.ITEM) { draft = draft.copy(slot = it) }
            Spinner(ui("common.rarity"), draft.rarity, mapOf("" to any) + rarities.associateWith { rarityTitle(it, s.lang) }, true, glyph = Glyph.RARITY) { draft = draft.copy(rarity = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.minItemLevel, { draft = draft.copy(minItemLevel = it.filter(Char::isDigit)) }, label = { Text(ui("auction.ilvl_from")) },
                    singleLine = true, keyboardOptions = digits, modifier = Modifier.weight(1f))
                OutlinedTextField(draft.maxItemLevel, { draft = draft.copy(maxItemLevel = it.filter(Char::isDigit)) }, label = { Text(ui("auction.ilvl_to")) },
                    singleLine = true, keyboardOptions = digits, modifier = Modifier.weight(1f))
            }
            Spinner(ui("auction.priced_in"), draft.priceOrbId,
                mapOf("" to any) + s.world.orbs.associate { it.id to it.title(s.lang) }, true, glyph = Glyph.CURRENCY) { draft = draft.copy(priceOrbId = it) }
            OutlinedTextField(draft.maxPrice, { draft = draft.copy(maxPrice = it.filter(Char::isDigit)) }, label = { Text(ui("auction.price_max")) },
                singleLine = true, keyboardOptions = digits, modifier = Modifier.fillMaxWidth())
            EntitySpinner(ui("auction.seller"), draft.sellerId, EntitySource.CHARACTER, true) { draft = draft.copy(sellerId = it) }
            // Own lots cannot be bought, so they are dropped unless a seller wants to compare prices.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ui("auction.show_mine"), modifier = Modifier.weight(1f))
                Switch(checked = mine, onCheckedChange = { mine = it })
            }
            MutedText(ui("auction.filter_note"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { draft = draft.cleared(); mine = false }, modifier = Modifier.weight(1f)) { Text(ui("auction.reset")) }
                Button(enabled = !s.busy, onClick = { onApply(draft, mine) }, modifier = Modifier.weight(1f)) { Text(ui("auction.apply")) }
            }
        }
    }
}

/** The character's own lots that are still on sale; withdrawn and sold lots leave this list. */
@Composable internal fun ColumnScope.MyLotsTab(s: ForgeState, vm: ForgeViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    var buyingSlot by remember { mutableStateOf(false) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        // Lot places (0.34.0): five to start with, one more at a time for gold, up to the server's ceiling.
        s.market.slots?.let { slots ->
            item {
                ForgePanel {
                    PropertyRow(ui("auction.slots"), ui("auction.slots_value", slots.used, slots.limit, slots.max), Glyph.ITEM)
                    if (slots.full) Text(ui("auction.slots_full"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
                    if (slots.price > 0) OutlinedButton(enabled = !s.busy, onClick = { buyingSlot = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(ui("auction.slot_buy", slots.price))
                    }
                }
            }
        }
        if (s.ownLots.isEmpty()) item {
            InfoCard(ui("auction.no_lots"), ui("auction.no_lots_hint"))
        }
        items(s.ownLots, key = { it.id }) { lot ->
            LotRow(s, lot, note = null) { openLot = lot.id }
        }
    }
    val slots = s.market.slots
    if (buyingSlot && slots != null) {
        val money = s.play.hero?.character?.money
        ConfirmSheet(title = ui("auction.slot_q"), confirm = ui("auction.slot_confirm"), onDismiss = { buyingSlot = false },
            ledger = listOfNotNull(LedgerLine(ui("confirm.spend"), ui("merchant.gold_amount", slots.price), Tone.SPEND),
                LedgerLine(ui("confirm.gain"), ui("auction.slots_value", slots.used, slots.limit + 1, slots.max), Tone.GAIN),
                money?.let { it - slots.price }?.takeIf { it >= 0 }?.let { LedgerLine(ui("confirm.left"), ui("merchant.gold_amount", it)) }),
            warning = money?.takeIf { it < slots.price }?.let { ui("merchant.short") }, blocked = money != null && money < slots.price) {
            buyingSlot = false
            vm.buyLotSlot()
        }
    }
    s.ownLots.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = ui("auction.withdraw"), enabled = !s.busy,
            note = null,
            loadBase = vm::equipmentBase, onDismiss = { openLot = null }) { openLot = null; vm.cancelLot(lot.id) }
    }
}

/**
 * One lot as a line: everything a trader decides on without opening it.
 *
 * It is the same [ItemRow] the stash draws, because a lot and a stash line are the same question
 * asked twice — what is it and what did it roll. What the auction adds is underneath:
 * the price opposite the name, where it is weighed with it, and the seller at the bottom.
 *
 * Rarity is not written anywhere: it is the frame of the icon and the colour of the name.
 */
@Composable private fun LotRow(s: ForgeState, lot: AuctionLot, note: String?, onClick: () -> Unit) {
    val document = lotDocument(s, lot)
    ItemRow(document, definitions = s.world.definitions, enabled = !s.busy,
        facts = lotFacts(s, lot, document),
        // The server's verdict on the template, as the stash marks it: a lot the buyer cannot wear yet.
        unwearable = s.play.hero?.sheet?.unwearableBy?.get(lot.equipment?.equipmentId.orEmpty()).orEmpty(),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(15.dp))
                Text(orbPrice(s, lot), color = GoldBright, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        },
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically) {
                note?.let { Text(it, color = Rune, style = MaterialTheme.typography.labelSmall) }
                Text(lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }, color = Muted,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        onClick = onClick)
}

/**
 * What the row adds about a lot, after what it already says itself.
 *
 * [ItemRow] prints the slot and the item level off the document, so repeating them here would
 * read "Шлем · ур. 30 · Шлем". The kind is only worth a word when there is no slot to print —
 * a stack lot, which has none.
 */
private fun lotFacts(s: ForgeState, lot: AuctionLot, document: JsonObject): List<String> {
    val kind = if (lot.slot == null) lotKindTitle(lot.kind, s.lang) else null
    val weapon = document.text("weaponType").takeIf { it.isNotBlank() }?.let { weaponTitle(it, s.lang) }
    val amount = if (lot.kind == AuctionLotKind.ITEM && lot.amount > 1) ui("auction.pieces", lot.amount) else null
    return listOfNotNull(kind, weapon, amount)
}

/**
 * A lot as the display helpers read it: a document.
 *
 * An equipment lot already carries its instance, and the template comes from the catalogue the
 * session read whole. A stack lot has no instance at all, so it gets the little the lot itself
 * knows — the name and the kind.
 */
private fun lotDocument(s: ForgeState, lot: AuctionLot): JsonObject {
    val instance = lot.equipment
    val base = instance?.let { s.world.inventoryBases[it.equipmentId] }
    val own = buildJsonObject {
        put("name", lot.title)
        // A stack lot carries no instance, so without this it would have neither an icon nor an
        // English name: both are found by the code, and only the lot itself knows it.
        if (lot.itemCode.isNotBlank()) put("code", lot.itemCode)
        lot.slot?.let { put("slot", it) }
        lot.rarity?.let { put("rarity", it) }
        if (lot.itemLevel > 0) put("itemLevel", lot.itemLevel)
    }
    return JsonObject((instance?.let { inventoryDocument(it, base) } ?: JsonObject(emptyMap())) + own)
}

/**
 * One lot in full, with the goods drawn as the stash draws them.
 *
 * The instance travels inside the lot, rolls and all, but its template does not — and armour,
 * damage and every requirement live in the template. So the sheet reads one, through the same
 * cache the stash fills: looking at a lot of something you already own costs no request at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LotSheet(s: ForgeState, lot: AuctionLot, action: String, enabled: Boolean, note: String?,
    loadBase: suspend (String) -> JsonObject?, onDismiss: () -> Unit, onAction: () -> Unit) {
    var base by remember(lot.id) { mutableStateOf<JsonObject?>(null) }
    var loading by remember(lot.id) { mutableStateOf(false) }
    LaunchedEffect(lot.id) {
        val templateId = lot.equipment?.equipmentId.orEmpty()
        if (templateId.isNotBlank()) {
            loading = true
            // A template that will not load costs the base half of the card, never the card: the
            // rolls came with the lot and are what the price is actually being paid for.
            try { base = loadBase(templateId) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
            finally { loading = false }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold) }
            lot.equipment?.let { instance ->
                item {
                    // The lot names the goods; the template would call an unread base "an item".
                    val document = JsonObject(inventoryDocument(instance, base) + ("name" to JsonPrimitive(lot.title)))
                    ItemCard(document, enabled = false, detailed = true, definitions = s.world.definitions,
                        actionLabel = ui("auction.lot") + " · ${lot.id.takeLast(6)}")
                }
            }
            item {
                ForgePanel {
                    Engraved(ui("auction.lot"))
                    if (lot.equipment == null) Text(lot.title, color = Gold, style = MaterialTheme.typography.titleMedium)
                    if (lot.kind == AuctionLotKind.ITEM) PropertyRow(ui("auction.amount"), lot.amount.toString(), Glyph.ITEM)
                    // The price is always counted in orbs; the catalogue the hero read gives the orb its name.
                    PropertyRow(ui("card.price"), orbPrice(s, lot), Glyph.CURRENCY)
                    PropertyRow(ui("auction.seller"), lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }, Glyph.CHARACTER)
                    listedAt(lot.createdAt)?.let { PropertyRow(ui("auction.listed_at"), it, Glyph.LEVEL) }
                    note?.let { MutedText(it, style = MaterialTheme.typography.labelMedium) }
                    Button(enabled = enabled, onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(action) }
                    MutedText(ui("auction.lot_note"))
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

/** A lot's price, in the orb it was set in; an orb the catalogue misses keeps its tail as a name. */
private fun orbPrice(s: ForgeState, lot: AuctionLot): String = ui("confirm.amount", lot.price, orbTitle(s, lot))

private fun orbTitle(s: ForgeState, lot: AuctionLot): String =
    s.world.orbs.firstOrNull { it.id == lot.priceOrbId }?.title(s.lang) ?: ui("auction.orb_id", lot.priceOrbId.takeLast(6))
