package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.itemIcon
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.DealEntry
import com.sperance.exileforge.core.market.DealLedger
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketChores
import com.sperance.exileforge.core.market.MyLotKind
import com.sperance.exileforge.core.market.MyLotSection
import com.sperance.exileforge.core.market.myLotSections
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.AuctionRules
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.trade.AuctionFees
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemEmblem
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.icons.SpriteIcon
import com.sperance.exileforge.ui.icons.orbArt
import com.sperance.exileforge.ui.screens.hero.StackIcon
import com.sperance.exileforge.ui.theme.*

/**
 * The hero's own lots that are still on sale, and those whose time ran out; withdrawn and sold lots leave this list.
 *
 * Места лотов и торгов общие, одинаковы у всех героев и не покупаются; сколько занято - числом у вкладки, сколько ставок лидирует - в
 * строке над списком. Сверху - свои торги на витрине ([BidRow]: ставок N), под ними - свои ставки в чужих торгах (лидирую / перебита),
 * ниже - лоты за цену и вернувшиеся; касание строки открывает страницу лота ([onOpen], [LotPage]). Строка лота - «ещё 14 ч 05 мин»
 * ([lotMarks]: в последний день - цветом [Caution], в окне продления - «· можно продлить»). Продление - в последний день, не больше `auction.extendMax` раз, каждое за плату выставления
 * против кошелька: на странице лота кнопка «Продлить на N дн.», рядом один чип платы и ⓘ с правилом. Истёкший лот возвращается сразу в тайник
 * (письмом - только когда места нет), строка ниже продающихся называет, куда он вернулся.
 *
 * Разделы - [myLotSections] ядра. Дела рынка - поток ядра ([MarketViewModel.chores]): строка, что ждёт действия, несёт жёлтую точку у
 * своей пометки, заголовок раздела - «● ждут: N». Вход с плитки Города - разовая просьба ([MarketViewModel.focus]): вкладка листает к
 * строке и обводит её, пока вкладка открыта и строка ещё ждёт действия.
 */
@Composable internal fun ColumnScope.MyLotsTab(game: GameUi, market: Market, vm: MarketViewModel, onOpen: (String) -> Unit) {
    val rules = game.index?.rules?.auction
    val now = rememberServerNow(LOT_TICK_MS)
    val chores by vm.chores.collectAsStateWithLifecycle()
    val focus by vm.focus.collectAsStateWithLifecycle()
    val sections = remember(market) { market.myLotSections }
    val rows = remember(market, sections, chores) { myLotsRows(market, sections, chores) }
    val list = rememberLazyListState()
    var spotlight by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focus, rows) {
        val asked = focus ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it.key == asked }
        if (index < 0) return@LaunchedEffect
        spotlight = asked
        vm.focused(asked)
        list.animateScrollToItem(index)
    }
    LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is MyLotsRow.Slots -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MutedText(ui("auction.my_fixed") + " " + ui("auction.slots_value", row.slots.used, row.slots.limit))
                    MutedText(ui("auction.my_bids") + " " + ui("auction.slots_value", row.slots.bids, row.slots.bidLimit))
                }

                MyLotsRow.Empty -> InfoCard(ui("auction.no_lots"), ui("auction.no_lots_hint"))

                is MyLotsRow.Section -> SectionTitle(sectionTitle(row.kind), row.count, row.due)

                is MyLotsRow.Lot -> Box(Modifier.spotlit(row.key == spotlight && chores[row.key] != null)) {
                    val lot = row.lot
                    when (row.kind) {
                        MyLotKind.AUCTIONS, MyLotKind.BIDS -> BidRow(lot, bidFacts(game.lang, lot), lotVisualKind(game, lot), rules, now, enabled = !game.busy, due = chores[lot.id]) { onOpen(lot.id) }
                        MyLotKind.FIXED -> LotRow(game, lot, marks = lotMarks(lot, rules, now), withSeller = false) { onOpen(lot.id) }
                    }
                }
            }
        }
    }
}

/** Имя раздела «Моих лотов» в словаре. */
private fun sectionTitle(kind: MyLotKind): String = when (kind) {
    MyLotKind.AUCTIONS -> ui("auction.my_auctions")
    MyLotKind.BIDS -> ui("auction.my_bids")
    MyLotKind.FIXED -> ui("auction.my_fixed")
}

/** Строка списка «Моих лотов»; [key] - ключ ленивого списка, у строки лота - id лота (по нему же подсветка входа с плитки). */
private sealed interface MyLotsRow {
    val key: String

    /** Места лотов и ставок над списком. */
    data class Slots(val slots: AuctionSlots) : MyLotsRow {
        override val key: String get() = "slots"
    }

    /** Ни лотов, ни ставок. */
    data object Empty : MyLotsRow {
        override val key: String get() = "empty"
    }

    /** Заголовок раздела [kind]: сколько строк и сколько из них ждут действия ([due]). */
    data class Section(val kind: MyLotKind, val count: Int, val due: Int) : MyLotsRow {
        override val key: String get() = "section-${kind.name}"
    }

    /** Лот раздела [kind]. */
    data class Lot(val lot: AuctionLot, val kind: MyLotKind) : MyLotsRow {
        override val key: String get() = lot.id
    }
}

/**
 * Строки «Моих лотов» по порядку: места, пусто, разделы [sections] с лотами; у заголовков - число дел [chores] раздела. Заголовок лотов за
 * цену - только когда выше есть другие разделы.
 */
private fun myLotsRows(market: Market, sections: List<MyLotSection>, chores: MarketChores): List<MyLotsRow> = buildList {
    market.slots?.takeIf { it.bidLimit > 0 }?.let { add(MyLotsRow.Slots(it)) }
    if (sections.isEmpty()) add(MyLotsRow.Empty)
    sections.forEach { (kind, lots) ->
        if (kind != MyLotKind.FIXED || sections.size > 1) add(MyLotsRow.Section(kind, lots.size, chores.count(lots)))
        lots.forEach { add(MyLotsRow.Lot(it, kind)) }
    }
}

/** Обводка строки, к которой привёл вход с плитки Города: жёлтая рамка по контуру плиты товара. */
private fun Modifier.spotlit(on: Boolean): Modifier = if (on) border(2.dp, Caution, RoundedCornerShape(16.dp)) else this

/** Заголовок раздела своих лотов: имя, сколько в нём строк и справа - сколько ждут действия ([due], ноль не печатается). */
@Composable private fun SectionTitle(title: String, count: Int, due: Int = 0) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Parchment, style = MaterialTheme.typography.titleSmall)
        Text(count.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
        if (due > 0) {
            Spacer(Modifier.weight(1f))
            ChoreDot()
            Text(ui("auction.due_count", due), color = Caution, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Пометки строки своего лота к «сейчас» [now] (часы сервера): куда вернулся истёкший ([lotReturn]), иначе «ещё N» - в окне продления
 * (последний день, [rules]) цветом [Caution] - и «можно продлить» с точкой дела, пока продления не кончились; срока нет - пометок нет.
 */
private fun lotMarks(lot: AuctionLot, rules: AuctionRules?, now: Long): List<TradeMark> {
    lotReturn(lot)?.let { return listOf(TradeMark(it)) }
    val left = lotLeft(lot, now) ?: return emptyList()
    val ending = rules != null && lot.extendable(rules.extendWindowMillis, now)
    return listOfNotNull(
        TradeMark(ui("auction.left", left), if (ending) Caution else Rune),
        TradeMark(ui("auction.extend_now"), Caution, due = true).takeIf { rules != null && lot.extendable(rules, now) },
    )
}

/**
 * Продление своего лота в листе: «Продлить на N дн.» (не хватает золота - недостача, кнопка погашена), рядом один чип платы [fee]
 * против кошелька и ⓘ - когда и сколько раз продлевают ([rules]).
 */
@Composable internal fun ExtendRow(game: GameUi, fee: Long, rules: AuctionRules, onExtend: () -> Unit) {
    val bill = game.goldBill(fee)
    val label = ui("auction.extend_days", rules.lotDays)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeButton(state = ButtonState.of(!game.busy && bill?.affordable == true), onClick = onExtend, modifier = Modifier.weight(1f), kind = ButtonKind.Secondary) {
            Text(bill?.payLabel(label) ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        bill?.let { GoldCost(it) }
        InfoMark(ui("auction.extend_about"), Rune) { Text(ui("auction.extend_tip", rules.extendMax), color = Parchment, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Which deals the history shows. */
internal enum class DealFilter { ALL, SOLD, BOUGHT }

/**
 * The hero's deals of the last days: what they sold and to whom, what they bought and from whom, the copy as it
 * changed hands a tap away; on top - подпись, что здесь только сделки между игроками (торговец сюда не входит), и две свёрнутые таблички
 * «Получено» и «Потрачено» ([DealLedger]), сколько хранятся сделки - в ⓘ у отбора и в подсказке пустой истории (`auction.historyDays`).
 *
 * Стопку и сферу открывает общая карточка, товар - страница лота ([onOpen]).
 */
@Composable internal fun ColumnScope.HistoryTab(game: GameUi, market: Market, onOpen: (String) -> Unit) {
    var filter by remember { mutableStateOf(DealFilter.ALL) }
    val heroId = game.heroId
    val deals = market.history.map { it.deal }
    val ledger = remember(deals, heroId) { DealLedger.of(deals, heroId) }
    val inspect = rememberInspect()
    val open = { entry: DealEntry ->
        when (entry) {
            is DealEntry.Stack -> inspect(Inspect.Stack(entry.code))
            is DealEntry.Goods -> onOpen(entry.lot.id)
            is DealEntry.Gold -> Unit
        }
    }
    val shown = deals.filter { deal ->
        when (filter) {
            DealFilter.ALL -> true
            DealFilter.SOLD -> deal.belongsTo(heroId)
            DealFilter.BOUGHT -> !deal.belongsTo(heroId)
        }
    }
    val rules = game.index?.rules?.auction
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                MutedText(ui("auction.history_scope"))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChoiceChipRow(DealFilter.entries.map { ChipChoice(it, ui("auction.history_${it.name.lowercase()}")) }, { it == filter }, { filter = it }, Modifier.weight(1f), track = Panel)
                    rules?.let { InfoMark(ui("auction.history_about"), Rune) { Text(ui("auction.history_note", it.historyDays), color = Parchment, style = MaterialTheme.typography.bodySmall) } }
                }
                DealTable(game, ui("auction.history_earned"), ledger.received, open)
                DealTable(game, ui("auction.history_spent"), ledger.spent, open)
            }
        }
        if (shown.isEmpty() && rules != null) item { InfoCard(ui("auction.history_empty"), ui("auction.history_empty_hint", rules.historyDays)) }
        items(shown, key = { it.id }) { deal ->
            LotRow(game, deal, marks = listOf(TradeMark(dealMark(deal, heroId))), withSeller = false) { onOpen(deal.id) }
        }
    }
}

/**
 * Табличка итога истории: «Получено» или «Потрачено», свёрнутая, в заголовке - сколько строк. Строка - значок, название и
 * количество (стопки и валюта сложены); нажатие на стопку, сферу или вещь - [onOpen]. Пустой таблички нет.
 */
@Composable private fun DealTable(game: GameUi, title: String, entries: List<DealEntry>, onOpen: (DealEntry) -> Unit) {
    if (entries.isEmpty()) return
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().foldToggle(open, title) { open = !open }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = Parchment, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(entries.size.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
            FoldChevron(open, Gold)
        }
        FoldBody(open, Arrangement.spacedBy(4.dp)) { entries.forEach { DealLine(game, it, onOpen) } }
    }
}

/** Строка таблички: значок, название и количество; золото сборов не открывается. */
@Composable private fun DealLine(game: GameUi, entry: DealEntry, onOpen: (DealEntry) -> Unit) {
    val (title, amount) = when (entry) {
        is DealEntry.Stack -> stackTitle(game, entry.code) to entry.amount
        is DealEntry.Gold -> ui("auction.history_fee_gold") to entry.amount
        is DealEntry.Goods -> entry.lot.title to entry.lot.amount
    }
    Row(
        Modifier.fillMaxWidth().then(if (entry is DealEntry.Gold) Modifier else Modifier.clickable(onClickLabel = title) { onOpen(entry) }).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (entry) {
            is DealEntry.Stack -> Orb.of(entry.code)?.let { OrbGlyph(it, Modifier.size(20.dp)) } ?: StackIcon(game, entry.code, 20)
            is DealEntry.Gold -> Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(20.dp))
            is DealEntry.Goods -> LotIcon(game, entry.lot, 20.dp)
        }
        Text(
            title,
            color = (entry as? DealEntry.Goods)?.let { lotColor(it.lot) } ?: Parchment,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(number(amount.toDouble()), color = GoldBright, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Имя стопки или сферы цены; сфера, которой правила не знают, - хвостом своего кода. */
private fun stackTitle(game: GameUi, code: String): String = if (Orb.of(code) != null || game.index?.item(ItemCode(code)) != null) itemTitle(code) else ui("auction.orb_id", code.takeLast(6))

/** «Продано: Имя · 12.10 14:30» or «Куплено у Имя · …». */
internal fun dealMark(deal: AuctionLot, heroId: String): String {
    val at = stampText(deal.soldAt)
    return if (deal.belongsTo(heroId)) {
        ui("auction.history_sold_to", deal.buyerName.ifBlank { "…" }, at)
    } else {
        ui("auction.history_bought_from", sellerName(deal), at)
    }
}

/** Момент [at] (мс эпохи) днём и временем часов устройства: «12.10 14:30» - сделки истории и конец торгов. */
internal fun stampText(at: Long): String = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.ROOT).format(java.util.Date(at))
