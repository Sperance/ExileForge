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
import com.sperance.exileforge.core.display.itemRequirements
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.tr
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

/**
 * One lot as a line: everything a trader decides on without opening it.
 *
 * A stash line can be terse because its owner already knows what they own. A showcase line cannot:
 * lots differ by what they rolled, so the properties ride on the line, five at most, and the card
 * behind the tap carries the rest. The name is given in English too, because that is the language
 * of the wiki and of every trade site the lot will be compared against.
 *
 * The price sits on its own line at the bottom, where the eye lands last: it is what everything
 * above is being weighed against, not one more property among them.
 */
@Composable private fun LotRow(s: ForgeState, lot: AuctionLot, note: String?, onClick: () -> Unit) {
    val document = lotDocument(s, lot)
    val colour = lot.rarity?.let { rarityColor(it) } ?: Gold
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    val properties = lotProperties(s, document)
    Column(Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, colour.copy(alpha = .40f), shape)
        .clickable(enabled = !s.busy, onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // The icon always stands: it is how a row is recognised again after scrolling past it.
            ItemIcon(document, colour, Modifier.size(40.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(lot.title + lot.titleEn.let { if (it.isBlank()) "" else " · $it" }, color = colour,
                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(lotFacts(s, lot, document), color = Muted, style = MaterialTheme.typography.labelSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                // Two lines, so five properties can actually be read instead of being clipped at two.
                if (properties.isNotBlank()) Text(properties, color = Rune,
                    style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(orbPrice(s, lot), color = Gold, style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** How many properties fit on a line before it stops being a line and turns into a card. */
private const val LOT_PROPERTIES = 5

/**
 * The lot's properties, compressed: the base first, then what it rolled, five at most.
 *
 * The base lives in the template and the rolls on the instance, but a trader reads them as one
 * list, so they are printed as one. What does not fit is counted rather than quietly dropped —
 * "ещё 3" is the difference between a short item and a clipped one.
 */
private fun lotProperties(s: ForgeState, document: JsonObject): String {
    val all = ((document["baseParams"] as? JsonArray).orEmpty() + (document["params"] as? JsonArray).orEmpty())
        .mapNotNull { (it as? JsonObject)?.let { one -> modifierText(one, s.definitions) } }
    val shown = all.take(LOT_PROPERTIES).joinToString(" · ")
    val hidden = all.size - LOT_PROPERTIES
    return if (hidden > 0) shown + tr(" · ещё $hidden", " · $hidden more") else shown
}

/** What the lot is: its slot or kind, its rarity, its level, and what it asks of a character. */
private fun lotFacts(s: ForgeState, lot: AuctionLot, document: JsonObject): String {
    val kind = lot.slot?.let { slotTitle(it, s.lang) } ?: lotKindTitle(lot.kind, s.lang)
    val weapon = document.text("weaponType").takeIf { it.isNotBlank() }?.let { weaponTitle(it, s.lang) }
    val amount = if (lot.kind == AuctionLotKind.ITEM && lot.amount > 1) tr("${lot.amount} шт.", "${lot.amount} pcs") else null
    val level = if (lot.itemLevel > 0) tr("ур. ${lot.itemLevel}", "lvl ${lot.itemLevel}") else null
    // Requirements are printed, never enforced here: the server checks them and refuses in its own words.
    val needs = itemRequirements(document, s.lang).takeIf { it.isNotEmpty() }
        ?.let { tr("треб. ${it.joinToString(", ")}", "needs ${it.joinToString(", ")}") }
    return listOfNotNull(kind, weapon, amount, lot.rarity?.let { rarityTitle(it, s.lang) }, level, needs).joinToString(" · ")
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
