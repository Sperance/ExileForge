package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.theme.*

/** Витрина аукциона (3.80.24): список лотов, строка поиска и фильтры. */
/** The showcase: the server's own search, so the page and the filter both belong to it. */
@Composable internal fun ColumnScope.ShowcaseTab(s: ForgeState, market: Market, vm: MarketViewModel) {
    ShowcaseList(s, market, header = { ShowcaseHeader(s, market, vm) }, onBuy = vm::buy, onMore = vm::moreShowcase)
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
@Composable internal fun ColumnScope.ShowcaseList(
    s: ForgeState,
    market: Market,
    header: @Composable () -> Unit = {},
    onBuy: (String) -> Unit,
    onMore: () -> Unit,
) {
    var openLot by remember { mutableStateOf<String?>(null) }
    var confirmBuy by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item { header() }
        if (market.showcase.items.isEmpty()) {
            item {
                InfoCard(
                    ui("tree.nothing_found"),
                    ui("auction.showcase_empty"),
                )
            }
        }
        items(market.showcase.items, key = { it.id }) { lot ->
            // A seller cannot buy their own lot, and the server says so; the sheet does not offer it.
            LotRow(s, lot, mark = if (lot.belongsTo(s.play.heroId)) ui("auction.your_lot") else null) { openLot = lot.id }
        }
        val showcase = market.showcase
        if (showcase.next != null) {
            item {
                ForgeOutlinedButton(enabled = !s.busy && Reads.AUCTION !in s.loading, onClick = onMore, modifier = Modifier.fillMaxWidth()) {
                    Text(ui("auction.more", showcase.items.size, showcase.totalItems))
                }
            }
        }
    }
    market.showcase.items.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(
            s,
            lot,
            action = ui("auction.buy"),
            enabled = !s.busy && !lot.belongsTo(s.play.heroId),
            note = if (lot.belongsTo(s.play.heroId)) ui("auction.own_lot") else null,
            onDismiss = { openLot = null },
        ) {
            openLot = null
            confirmBuy = lot.id
        }
    }
    // A purchase cannot be undone, so it is asked about — and an item the hero cannot wear
    // is said so in the same breath, because that is exactly the mistake worth catching.
    market.showcase.items.firstOrNull { it.id == confirmBuy }?.let { lot ->
        val blocked = lotUnmet(s, lot)
        val orb = orbTitle(lot)
        // What the bag keeps after paying: shown when the bag is known and can pay; when it cannot,
        // the sheet says so and the purchase is not sent (2.46.0).
        val have = s.bagAmount(lot.priceOrb)
        val money = s.hero?.money
        val fee = lot.fee
        val poor = money != null && money < fee
        ConfirmSheet(
            title = ui("auction.buy_q"),
            subtitle = lot.title,
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
            onDismiss = { confirmBuy = null },
        ) { onBuy(lot.id) }
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
@Composable internal fun ShowcaseHeader(s: ForgeState, market: Market, vm: MarketViewModel) {
    var sheet by remember { mutableStateOf(false) }
    val f = market.filter
    val count = f.active().size + if (market.showOwnLots) 1 else 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                f.title,
                { vm.filter(f.copy(title = it.take(s.inputs.search))) },
                placeholder = { Text(ui("auction.name")) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.loadShowcase() }),
            )
            ForgeOutlinedButton(onClick = { sheet = true }, enabled = !s.busy, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Outlined.FilterList, ui("auction.filters"), modifier = Modifier.size(18.dp))
                if (count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(count.toString())
                }
            }
        }
        // The filters set are one line of chips that scrolls sideways, never a block growing down over the lots.
        if (count > 0) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                f.active().forEach { field ->
                    ActiveFilter(chipLabel(s, field, f.value(field))) {
                        vm.filter(f.without(field))
                        vm.loadShowcase()
                    }
                }
                if (market.showOwnLots) {
                    ActiveFilter(ui("auction.show_mine")) {
                        vm.showOwnLots(false)
                        vm.loadShowcase()
                    }
                }
            }
        }
    }
    if (sheet) {
        FilterSheet(s, market, onDismiss = { sheet = false }) { filter, mine ->
            sheet = false
            vm.filter(filter)
            vm.showOwnLots(mine)
            vm.loadShowcase()
        }
    }
}

/** A filter that is set, named by its value, with a cross that drops it. */
@Composable internal fun ActiveFilter(label: String, onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Outlined.Close, ui("auction.remove_filter"), modifier = Modifier.size(16.dp)) },
        colors = InputChipDefaults.inputChipColors(selectedContainerColor = Gold, selectedLabelColor = Ink, selectedTrailingIconColor = Ink),
    )
}

internal fun chipLabel(s: ForgeState, field: FilterField, value: String): String = when (field) {
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
internal val templateSlots: List<Slot> = Slot.entries.filter { it != Slot.RING_2 && it != Slot.FLASK_2 && it != Slot.FLASK_3 }

/**
 * Every filter the server understands, on a draft: nothing reaches the showcase until «Показать»,
 * so trying a combination costs no request, and «Сбросить» clears all but the typed name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(s: ForgeState, market: Market, onDismiss: () -> Unit, onApply: (AuctionFilter, Boolean) -> Unit) {
    var draft by remember { mutableStateOf(market.filter) }
    var mine by remember { mutableStateOf(market.showOwnLots) }
    val any = ui("common.all")
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Engraved(ui("auction.filters"))
            Spinner(
                ui("auction.what_sold"),
                draft.kind,
                mapOf("" to any) + LotKind.entries.associate { it.name to lotKindTitle(it, s.lang) },
                true,
                glyph = Glyph.ITEM,
            ) { draft = draft.copy(kind = it) }
            Spinner(ui("common.slot"), draft.slot, mapOf("" to any) + templateSlots.associate { it.name to slotTitle(it, s.lang) }, true, glyph = Glyph.ITEM) { draft = draft.copy(slot = it) }
            Spinner(ui("common.rarity"), draft.rarity, mapOf("" to any) + Rarity.entries.associate { it.name to rarityTitle(it, s.lang) }, true, glyph = Glyph.RARITY) { draft = draft.copy(rarity = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    draft.minItemLevel,
                    { draft = draft.copy(minItemLevel = it.filter(Char::isDigit).take(3)) },
                    label = { Text(ui("auction.ilvl_from")) },
                    singleLine = true,
                    keyboardOptions = digits,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    draft.maxItemLevel,
                    { draft = draft.copy(maxItemLevel = it.filter(Char::isDigit).take(3)) },
                    label = { Text(ui("auction.ilvl_to")) },
                    singleLine = true,
                    keyboardOptions = digits,
                    modifier = Modifier.weight(1f),
                )
            }
            // The price's orb is an item code (3.0.0): the auction's currencies, in the order of their price.
            Spinner(
                ui("auction.priced_in"),
                draft.priceOrb,
                mapOf("" to any) + orbOptions(s),
                true,
                glyph = Glyph.CURRENCY,
                optionArt = orbArt(s.currencies),
            ) { draft = draft.copy(priceOrb = it) }
            OutlinedTextField(
                draft.maxPrice,
                { draft = draft.copy(maxPrice = it.filter(Char::isDigit).take(s.inputs.number)) },
                label = { Text(ui("auction.price_max")) },
                singleLine = true,
                keyboardOptions = digits,
                modifier = Modifier.fillMaxWidth(),
            )
            // A seller is named by the hero's id: there is no catalogue of heroes to pick one from.
            OutlinedTextField(
                draft.sellerId,
                { draft = draft.copy(sellerId = it.trim().take(s.inputs.code)) },
                label = { Text(ui("auction.seller")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // Own lots cannot be bought, so they are dropped unless a seller wants to compare prices.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ui("auction.show_mine"), modifier = Modifier.weight(1f))
                Switch(checked = mine, onCheckedChange = { mine = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeOutlinedButton(onClick = {
                    draft = draft.cleared()
                    mine = false
                }, modifier = Modifier.weight(1f)) { Text(ui("auction.reset")) }
                ForgeButton(enabled = !s.busy, onClick = { onApply(draft, mine) }, modifier = Modifier.weight(1f)) { Text(ui("auction.apply")) }
            }
        }
    }
}
