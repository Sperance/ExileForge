package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
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
import com.sperance.exileforge.core.display.modifierText
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The showcase: the server's own search, so the page and the filter both belong to it. */
@Composable internal fun ColumnScope.ShowcaseTab(s: ForgeState, vm: ForgeViewModel) {
    ShowcaseList(s, header = { ShowcaseFilter(s, vm) }, onBuy = vm::buyLot, onPage = vm::loadShowcase, loadBase = vm::equipmentBase)
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
        if (s.showcase.items.isEmpty()) item {
            InfoCard(ui("tree.nothing_found"),
                ui("auction.showcase_empty"))
        }
        items(s.showcase.items, key = { it.id }) { lot ->
            // A seller cannot buy their own lot, and the server says so; the sheet does not offer it.
            LotRow(s, lot, note = if (lot.belongsTo(s.characterId)) ui("auction.your_lot") else null) { openLot = lot.id }
        }
        if (s.showcase.totalPages > 1) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(enabled = !s.busy && s.showcase.page > 0, onClick = { onPage(s.showcase.page - 1) }) { Text(ui("common.back")) }
                Text(ui("auction.page", s.showcase.page + 1, s.showcase.totalPages), color = Muted)
                OutlinedButton(enabled = !s.busy && s.showcase.page + 1 < s.showcase.totalPages, onClick = { onPage(s.showcase.page + 1) }) { Text(ui("auction.forward")) }
            }
        }
    }
    s.showcase.items.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = ui("auction.buy"), enabled = !s.busy && !lot.belongsTo(s.characterId),
            note = if (lot.belongsTo(s.characterId)) ui("auction.own_lot") else null,
            loadBase = loadBase, onDismiss = { openLot = null }) { openLot = null; confirmBuy = lot.id }
    }
    // A purchase cannot be undone, so it is asked about — and an item the character cannot wear
    // is said so in the same breath, because that is exactly the mistake worth catching.
    s.showcase.items.firstOrNull { it.id == confirmBuy }?.let { lot ->
        val blocked = s.hero?.sheet?.unwearableBy?.get(lot.equipment?.equipmentId.orEmpty()).orEmpty()
        ConfirmDialog(
            title = ui("auction.buy_q"),
            text = listOfNotNull(
                ui("auction.buy_text", lot.title, orbPrice(s, lot)),
                blocked.takeIf { it.isNotEmpty() }?.let {
                    ui("auction.unwearable", it.joinToString(", ") { r -> requirementReason(r, s.lang) })
                }
            ).joinToString("\n\n"),
            confirm = ui("auction.buy_do"),
            onDismiss = { confirmBuy = null }) { onBuy(lot.id) }
    }
}

/**
 * The filter.
 *
 * Name, kind and the price ceiling are always in reach because they are what a trader reaches for;
 * the rest of what the server understands sits one tap away.
 */
@Composable private fun ShowcaseFilter(s: ForgeState, vm: ForgeViewModel) {
    var more by remember { mutableStateOf(false) }
    val f = s.auctionFilter
    val any = ui("common.all")
    ForgePanel {
        Engraved(ui("auction.search"))
        OutlinedTextField(f.title, { vm.auctionFilter(f.copy(title = it)) }, label = { Text(ui("auction.name")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spinner(ui("auction.what_sold"), f.kind,
            mapOf("" to any) + AuctionLotKind.entries.associate { it.name to lotKindTitle(it, s.lang) }, !s.busy) { vm.auctionFilter(f.copy(kind = it)) }
        Spinner(ui("auction.priced_in"), f.priceOrbId,
            mapOf("" to any) + s.orbs.associate { it.id to it.title(s.lang) }, !s.busy) { vm.auctionFilter(f.copy(priceOrbId = it)) }
        OutlinedTextField(f.maxPrice, { vm.auctionFilter(f.copy(maxPrice = it)) }, label = { Text(ui("auction.price_max")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { more = !more }) { Text(ui("auction.more_filters", if (more) ui("common.hide") else ui("common.show"))) }
        if (more) {
            Spinner(ui("common.slot"), f.slot, mapOf("" to any) + slots.associateWith { slotTitle(it, s.lang) }, !s.busy) { vm.auctionFilter(f.copy(slot = it)) }
            Spinner(ui("common.rarity"), f.rarity, mapOf("" to any) + rarities.associateWith { rarityTitle(it, s.lang) }, !s.busy) { vm.auctionFilter(f.copy(rarity = it)) }
            OutlinedTextField(f.minItemLevel, { vm.auctionFilter(f.copy(minItemLevel = it)) }, label = { Text(ui("auction.ilvl_from")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(f.maxItemLevel, { vm.auctionFilter(f.copy(maxItemLevel = it)) }, label = { Text(ui("auction.ilvl_to")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            EntitySpinner(ui("auction.seller"), f.sellerId, EntitySource.CHARACTER, !s.busy) { vm.auctionFilter(f.copy(sellerId = it)) }
        }
        // Own lots cannot be bought, so they are dropped unless a seller wants to compare prices.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(ui("auction.show_mine"), modifier = Modifier.weight(1f))
            Switch(checked = s.showOwnLots, enabled = !s.busy, onCheckedChange = { vm.showOwnLots(it); vm.loadShowcase(0) })
        }
        Button(enabled = !s.busy, onClick = { vm.loadShowcase(0) }, modifier = Modifier.fillMaxWidth()) { Text(ui("auction.do_search")) }
        Text(ui("auction.filter_note"),
            color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** The character's own lots, open and closed alike: the closed ones are their trading history. */
@Composable internal fun ColumnScope.MyLotsTab(s: ForgeState, vm: ForgeViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (s.myLots.isEmpty()) item {
            InfoCard(ui("auction.no_lots"), ui("auction.no_lots_hint"))
        }
        items(s.myLots, key = { it.id }) { lot ->
            LotRow(s, lot, note = if (lot.onSale) null else lotStatusTitle(lot.status, s.lang)) { openLot = lot.id }
        }
    }
    s.myLots.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = ui("auction.withdraw"), enabled = !s.busy && lot.onSale,
            note = if (lot.onSale) null else lotStatusTitle(lot.status, s.lang),
            loadBase = vm::equipmentBase, onDismiss = { openLot = null }) { openLot = null; vm.cancelLot(lot.id) }
    }
}

/**
 * One lot as a line: everything a trader decides on without opening it.
 *
 * It is the same [ItemRow] the stash draws, because a lot and a stash line are the same question
 * asked twice — what is it and what did it roll. What the auction adds is underneath:
 * the price on the left, where it is weighed, and the seller on the right.
 *
 * Rarity is not written anywhere: it is the colour of the frame and the name.
 */
@Composable private fun LotRow(s: ForgeState, lot: AuctionLot, note: String?, onClick: () -> Unit) {
    val document = lotDocument(s, lot)
    ItemRow(document, definitions = s.definitions, enabled = !s.busy, note = note, noteColor = Muted,
        facts = lotFacts(s, lot, document),
        footer = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(orbPrice(s, lot), color = Gold, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
    val base = instance?.let { s.inventoryBases[it.equipmentId] }
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
                    ItemCard(document, enabled = false, detailed = true, definitions = s.definitions,
                        actionLabel = ui("auction.lot") + " · ${lot.id.takeLast(6)}")
                }
            }
            item {
                ForgePanel {
                    Engraved(ui("auction.lot"))
                    if (lot.equipment == null) Text(lot.title, color = Gold, style = MaterialTheme.typography.titleMedium)
                    if (lot.kind == AuctionLotKind.ITEM) PropertyRow(ui("auction.amount"), lot.amount.toString(), "item")
                    // The price is always counted in orbs; the catalogue the hero read gives the orb its name.
                    PropertyRow(ui("card.price"), orbPrice(s, lot), "price")
                    PropertyRow(ui("auction.seller"), lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }, "character")
                    listedAt(lot.createdAt)?.let { PropertyRow(ui("auction.listed_at"), it, "level") }
                    note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium) }
                    Button(enabled = enabled, onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(action) }
                    Text(ui("auction.lot_note"),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
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
private fun orbPrice(s: ForgeState, lot: AuctionLot): String = "${lot.price} × " +
    (s.orbs.firstOrNull { it.id == lot.priceOrbId }?.title(s.lang)
        ?: ui("auction.orb_id", lot.priceOrbId.takeLast(6)))
