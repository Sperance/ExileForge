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
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item { header() }
        if (s.showcase.items.isEmpty()) item {
            InfoCard(tr("Ничего не найдено", "Nothing found"),
                tr("На витрине нет лотов по этому фильтру.", "No lot on the showcase matches this filter."))
        }
        items(s.showcase.items, key = { it.id }) { lot ->
            // A seller cannot buy their own lot, and the server says so; the sheet does not offer it.
            LotRow(s, lot, note = if (lot.belongsTo(s.characterId)) tr("Ваш лот", "Your lot") else null) { openLot = lot.id }
        }
        if (s.showcase.totalPages > 1) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(enabled = !s.busy && s.showcase.page > 0, onClick = { onPage(s.showcase.page - 1) }) { Text(tr("Назад", "Back")) }
                Text(tr("Страница ${s.showcase.page + 1} из ${s.showcase.totalPages}", "Page ${s.showcase.page + 1} of ${s.showcase.totalPages}"), color = Muted)
                OutlinedButton(enabled = !s.busy && s.showcase.page + 1 < s.showcase.totalPages, onClick = { onPage(s.showcase.page + 1) }) { Text(tr("Вперёд", "Next")) }
            }
        }
    }
    s.showcase.items.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = tr("Купить", "Buy"), enabled = !s.busy && !lot.belongsTo(s.characterId),
            note = if (lot.belongsTo(s.characterId)) tr("Свой лот купить нельзя", "You cannot buy your own lot") else null,
            loadBase = loadBase, onDismiss = { openLot = null }) { openLot = null; onBuy(lot.id) }
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
    val any = tr("Все", "All")
    ForgePanel {
        Engraved(tr("Поиск по витрине", "Search the showcase"))
        OutlinedTextField(f.title, { vm.auctionFilter(f.copy(title = it)) }, label = { Text(tr("Название", "Name")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spinner(tr("Что продаётся", "What is sold"), f.kind,
            mapOf("" to any) + AuctionLotKind.entries.associate { it.name to lotKindTitle(it, s.lang) }, !s.busy) { vm.auctionFilter(f.copy(kind = it)) }
        Spinner(tr("Цена в сфере", "Priced in"), f.priceOrbId,
            mapOf("" to any) + s.orbs.associate { it.id to it.title(s.lang) }, !s.busy) { vm.auctionFilter(f.copy(priceOrbId = it)) }
        OutlinedTextField(f.maxPrice, { vm.auctionFilter(f.copy(maxPrice = it)) }, label = { Text(tr("Цена не выше", "Price at most")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { more = !more }) { Text(tr("Ещё фильтры · ${if (more) "свернуть" else "показать"}", "More filters · ${if (more) "hide" else "show"}")) }
        if (more) {
            Spinner(tr("Слот", "Slot"), f.slot, mapOf("" to any) + slots.associateWith { slotTitle(it, s.lang) }, !s.busy) { vm.auctionFilter(f.copy(slot = it)) }
            Spinner(tr("Редкость", "Rarity"), f.rarity, mapOf("" to any) + rarities.associateWith { rarityTitle(it, s.lang) }, !s.busy) { vm.auctionFilter(f.copy(rarity = it)) }
            OutlinedTextField(f.minItemLevel, { vm.auctionFilter(f.copy(minItemLevel = it)) }, label = { Text(tr("Уровень предмета от", "Item level from")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(f.maxItemLevel, { vm.auctionFilter(f.copy(maxItemLevel = it)) }, label = { Text(tr("Уровень предмета до", "Item level to")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            EntitySpinner(tr("Продавец", "Seller"), f.sellerId, EntitySource.CHARACTER, !s.busy) { vm.auctionFilter(f.copy(sellerId = it)) }
        }
        // Own lots cannot be bought, so they are dropped unless a seller wants to compare prices.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Показывать свои лоты", "Show my own lots"), modifier = Modifier.weight(1f))
            Switch(checked = s.showOwnLots, enabled = !s.busy, onCheckedChange = { vm.showOwnLots(it); vm.loadShowcase(0) })
        }
        Button(enabled = !s.busy, onClick = { vm.loadShowcase(0) }, modifier = Modifier.fillMaxWidth()) { Text(tr("Искать", "Search")) }
        Text(tr("Фильтр считает сервер: все поля лежат снимком в самом лоте.",
                "The filter is the server's: every field is a snapshot the lot carries."),
            color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** The character's own lots, open and closed alike: the closed ones are their trading history. */
@Composable internal fun ColumnScope.MyLotsTab(s: ForgeState, vm: ForgeViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (s.myLots.isEmpty()) item {
            InfoCard(tr("Лотов нет", "No lots"), tr("Вы ещё ничего не выставляли.", "You have not listed anything yet."))
        }
        items(s.myLots, key = { it.id }) { lot ->
            LotRow(s, lot, note = if (lot.onSale) null else lotStatusTitle(lot.status, s.lang)) { openLot = lot.id }
        }
        item { OutlinedButton(enabled = !s.busy, onClick = vm::loadMyLots, modifier = Modifier.fillMaxWidth()) { Text(tr("Обновить", "Refresh")) } }
    }
    s.myLots.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(s, lot, action = tr("Снять с продажи", "Withdraw"), enabled = !s.busy && lot.onSale,
            note = if (lot.onSale) null else lotStatusTitle(lot.status, s.lang),
            loadBase = vm::equipmentBase, onDismiss = { openLot = null }) { openLot = null; vm.cancelLot(lot.id) }
    }
}

/** One lot as a line: what it is, what it costs, who is selling. The rest is one tap away. */
@Composable private fun LotRow(s: ForgeState, lot: AuctionLot, note: String?, onClick: () -> Unit) {
    val colour = lot.rarity?.let { rarityColor(it) } ?: Gold
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    Row(Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, colour.copy(alpha = .40f), shape)
        .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(lot.title, color = colour, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
            }
            Text(lotKindTitle(lot.kind, s.lang) + (lot.slot?.let { " · ${slotTitle(it, s.lang)}" } ?: "") +
                (lot.rarity?.let { " · ${rarityTitle(it, s.lang)}" } ?: "") +
                (if (lot.itemLevel > 0) tr(" · ур. ${lot.itemLevel}", " · lvl ${lot.itemLevel}") else ""),
                color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(orbPrice(s, lot) + " · " + lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" },
                color = Gold, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
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
                        actionLabel = tr("Лот", "Lot") + " · ${lot.id.takeLast(6)}")
                }
            }
            item {
                ForgePanel {
                    Engraved(tr("Лот", "Lot"))
                    if (lot.equipment == null) Text(lot.title, color = Gold, style = MaterialTheme.typography.titleMedium)
                    if (lot.kind == AuctionLotKind.ITEM) PropertyRow(tr("Количество", "Amount"), lot.amount.toString(), "item")
                    // The price is always counted in orbs; the catalogue the hero read gives the orb its name.
                    PropertyRow(tr("Цена", "Price"), orbPrice(s, lot), "price")
                    PropertyRow(tr("Продавец", "Seller"), lot.sellerName.ifBlank { "…${lot.sellerId.takeLast(6)}" }, "character")
                    note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium) }
                    Button(enabled = enabled, onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(action) }
                    Text(tr("Пока лот выставлен, предмет лежит в нём, а не у продавца. Сделку целиком проводит сервер.",
                            "While a lot is listed the goods live inside it, not with the seller. The server carries out the whole trade."),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** A lot's price, in the orb it was set in; an orb the catalogue misses keeps its tail as a name. */
private fun orbPrice(s: ForgeState, lot: AuctionLot): String = "${lot.price} × " +
    (s.orbs.firstOrNull { it.id == lot.priceOrbId }?.title(s.lang)
        ?: tr("сфера …${lot.priceOrbId.takeLast(6)}", "orb …${lot.priceOrbId.takeLast(6)}"))
