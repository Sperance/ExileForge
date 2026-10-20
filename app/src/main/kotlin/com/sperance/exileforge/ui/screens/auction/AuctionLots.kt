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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
 * Места лотов одинаковы у всех героев и не покупаются; сколько занято - числом у вкладки, не в списке. Строка лота - «ещё 14 ч 05 мин»
 * ([lotMarks]: в последний день - цветом [Caution], в окне продления - «· можно продлить»). Продление - в последний день, не больше `auction.extendMax` раз, каждое за плату выставления
 * против кошелька: в листе лота кнопка «Продлить на N дн.», рядом один чип платы и ⓘ с правилом. Истёкший лот возвращается сразу в тайник
 * (письмом - только когда места нет), строка ниже продающихся называет, куда он вернулся.
 */
@Composable internal fun ColumnScope.MyLotsTab(game: GameUi, market: Market, vm: MarketViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    val rules = game.index?.rules?.auction
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        if (market.ownLots.isEmpty()) {
            item {
                InfoCard(ui("auction.no_lots"), ui("auction.no_lots_hint"))
            }
        }
        items(market.ownLots, key = { it.id }) { lot ->
            LotRow(game, lot, marks = lotMarks(lot, rules), withSeller = false) { openLot = lot.id }
        }
    }
    market.ownLots.firstOrNull { it.id == openLot }?.let { lot ->
        LotSheet(
            game,
            lot,
            action = ui("auction.withdraw"),
            enabled = !game.busy && lot.onSale,
            note = lotReturn(lot),
            onDismiss = { openLot = null },
            left = lotLeft(lot),
            extra = if (rules != null && lot.extendable(rules)) {
                {
                    ExtendRow(game, game.index?.let { AuctionFees.listing(it, game.heroLevel, lot.priceOrb, lot.price) } ?: 0L, rules) {
                        openLot = null
                        vm.extend(lot.id)
                    }
                }
            } else {
                null
            },
        ) {
            openLot = null
            vm.cancel(lot.id)
        }
    }
}

/**
 * Пометки строки своего лота: куда вернулся истёкший ([lotReturn]), иначе «ещё N» - в окне продления (последний день, [rules]) цветом
 * [Caution] - и «можно продлить», пока продления не кончились; срока нет - пометок нет.
 */
private fun lotMarks(lot: AuctionLot, rules: AuctionRules?): List<TradeMark> {
    lotReturn(lot)?.let { return listOf(TradeMark(it)) }
    val left = lotLeft(lot) ?: return emptyList()
    val ending = rules != null && lot.extendable(rules.extendWindowMillis)
    return listOfNotNull(
        TradeMark(ui("auction.left", left), if (ending) Caution else Rune),
        TradeMark(ui("auction.extend_now")).takeIf { rules != null && lot.extendable(rules) },
    )
}

/**
 * Продление своего лота в листе: «Продлить на N дн.» (не хватает золота - недостача, кнопка погашена), рядом один чип платы [fee]
 * против кошелька и ⓘ - когда и сколько раз продлевают ([rules]).
 */
@Composable private fun ExtendRow(game: GameUi, fee: Long, rules: AuctionRules, onExtend: () -> Unit) {
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
 * changed hands a tap away; on top - две свёрнутые таблички «Получено» и «Потрачено» ([DealLedger]), сколько хранятся сделки - в ⓘ
 * у отбора.
 *
 * Стопку и сферу открывает общая карточка, товар - карточка лота.
 */
@Composable internal fun ColumnScope.HistoryTab(game: GameUi, market: Market) {
    var filter by remember { mutableStateOf(DealFilter.ALL) }
    var openLot by remember { mutableStateOf<String?>(null) }
    val heroId = game.heroId
    val deals = market.history.map { it.deal }
    val ledger = remember(deals, heroId) { DealLedger.of(deals, heroId) }
    var opened by remember { mutableStateOf<DealEntry?>(null) }
    val inspect = rememberInspect()
    val open = { entry: DealEntry ->
        if (entry is DealEntry.Stack) {
            inspect(Inspect.Stack(entry.code))
        } else {
            opened = entry
        }
    }
    val shown = deals.filter { deal ->
        when (filter) {
            DealFilter.ALL -> true
            DealFilter.SOLD -> deal.belongsTo(heroId)
            DealFilter.BOUGHT -> !deal.belongsTo(heroId)
        }
    }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            ForgePanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DealFilter.entries.forEach { f ->
                        ForgeChip(ui("auction.history_${f.name.lowercase()}"), filter == f, { filter = f }, mode = ChipMode.Choice)
                    }
                    Spacer(Modifier.weight(1f))
                    game.index?.rules?.auction?.let { rules ->
                        InfoMark(ui("auction.history_about"), Rune) { Text(ui("auction.history_note", rules.historyDays), color = Parchment, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                DealTable(game, ui("auction.history_earned"), ledger.received, open)
                DealTable(game, ui("auction.history_spent"), ledger.spent, open)
            }
        }
        if (shown.isEmpty()) item { InfoCard(ui("auction.history_empty"), ui("auction.history_empty_hint")) }
        items(shown, key = { it.id }) { deal ->
            LotRow(game, deal, marks = listOf(TradeMark(dealMark(deal, heroId))), withSeller = false) { openLot = deal.id }
        }
    }
    shown.firstOrNull { it.id == openLot }?.let { deal ->
        LotSheet(game, deal, action = null, enabled = false, note = dealMark(deal, heroId), onDismiss = { openLot = null })
    }
    when (val entry = opened) {
        is DealEntry.Goods -> LotSheet(game, entry.lot, action = null, enabled = false, note = dealMark(entry.lot, heroId), onDismiss = { opened = null })
        is DealEntry.Stack, is DealEntry.Gold, null -> Unit
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
            is DealEntry.Goods -> LotIcon(game, entry.lot, Modifier.size(20.dp))
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
    val at = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.ROOT).format(java.util.Date(deal.soldAt))
    return if (deal.belongsTo(heroId)) {
        ui("auction.history_sold_to", deal.buyerName.ifBlank { "…" }, at)
    } else {
        ui("auction.history_bought_from", sellerName(deal), at)
    }
}
