package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.AuctionRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.*

/**
 * Вкладка «Аукцион»: торги под той же строкой поиска и фильтром, что витрина ([ShowcaseHeader]); строка торгов - [BidRow], касание
 * открывает лист ставки ([BidSheet]). Не загрузились - отказ и «Повторить», а не «ничего не найдено»; листание до края дочитывает
 * следующую страницу ([PagedColumn]).
 */
@Composable internal fun ColumnScope.BidsTab(game: GameUi, market: Market, vm: MarketViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    val rules = game.index?.rules?.auction
    val page = market.auctions
    val now = rememberServerNow(LOT_TICK_MS)
    Box(Modifier.padding(top = 10.dp)) { CollapsibleHeader { ShowcaseHeader(game, market, vm) } }
    PagedColumn(page.items.size, page.next != null, Reads.AUCTION in game.loading, vm::moreAuctions, market.filter, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (page.items.isEmpty() && market.showcaseFailed) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCard(ui("auction.showcase_failed"), ui("auction.showcase_failed_hint"), failure = true)
                    ForgeButton(state = ButtonState.of(Reads.AUCTION !in game.loading), onClick = vm::loadShowcase, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                        Text(ui("auction.showcase_retry"))
                    }
                }
            }
        } else if (page.items.isEmpty()) {
            item { InfoCard(ui("tree.nothing_found"), ui("auction.bids_empty")) }
        }
        items(page.items, key = { it.id }) { lot -> BidRow(lot, bidFacts(game.lang, lot), lotVisualKind(game, lot), rules, now, enabled = !game.busy) { openLot = lot.id } }
    }
    page.items.firstOrNull { it.id == openLot }?.let { lot -> BidSheetOf(game, lot, now, vm) { openLot = null } }
}

/** Лист ставки [BidSheet] торгов [lot] из среза игры: правила, герой и товар карточкой; команды - модели рынка [vm]. */
@Composable internal fun BidSheetOf(game: GameUi, lot: AuctionLot, now: Long, vm: MarketViewModel, onDismiss: () -> Unit) {
    val index = game.index ?: return
    val rules = index.rules.auction
    BidSheet(lot, rules, index, game.hero, enabled = !game.busy, now = now, goods = { BidGoods(game, lot) }, onDismiss = onDismiss, onBid = { amount -> vm.bid(lot.id, amount) }, onBuyout = { vm.buyout(lot.id) })
}

/**
 * Строка торгов (вариант A), как [TradeRow], из готовых данных: плита товара - значок вида [visual], имя цветом редкости, факты [facts] и
 * пометки (ставок N, «вы лидер» / «ставку перебили»), срок к «сейчас» [now] (часы сервера); справа колонка - ставка крупно (без ставок -
 * первая ставка) и выкуп под ней; снизу полоса оставшегося срока, в последние часы хвоста ставки - красная. Лидер и перебитая ставка -
 * цветной кромкой слева: зелёной и красной.
 */
@Composable internal fun BidRow(lot: AuctionLot, facts: List<String>, visual: ItemVisualKind, rules: AuctionRules?, now: Long, enabled: Boolean, onClick: () -> Unit) {
    val card = RoundedCornerShape(16.dp)
    val frame = RoundedCornerShape(12.dp)
    val color = lotColor(lot)
    val edge = bidEdge(lot)
    val ending = rules != null && lot.bidEnding(rules, now)
    Column(
        Modifier.fillMaxWidth().depthPanel(card, elevation = 8.dp).clip(card).border(1.dp, (edge ?: color).copy(alpha = if (edge == null) .2f else .55f), card)
            .then(if (edge == null) Modifier else Modifier.drawBehind { drawRect(edge, size = Size(EDGE.toPx(), size.height)) })
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Abyss, frame).border(1.dp, color.copy(alpha = .55f), frame), contentAlignment = Alignment.Center) {
                LotIcon(lot, visual, Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(lot.title, color = color, style = itemName(15), maxLines = 2, overflow = TextOverflow.Ellipsis)
                TradeFacts(facts, bidMarks(lot))
                lotLeft(lot, now)?.let { Text(ui("auction.left", it), color = if (ending) LifeRed else Muted, style = MaterialTheme.typography.labelSmall) }
            }
            BidColumn(lot)
        }
        TimeBar(rules?.let { lot.timeShare(it, now) } ?: 0f, ending)
    }
}

/** Кромка строки торгов: зелёная - ставка героя лидирует, красная - перебита; null - он не ставил. */
private fun bidEdge(lot: AuctionLot): Color? = when {
    lot.leading -> Vital
    lot.outbid -> LifeRed
    else -> null
}

/** Ширина цветной кромки строки торгов. */
private val EDGE = 3.dp

/** Факты товара торгов на языке [lang]: у вещи - уровень и место, у стопки - сколько штук. */
internal fun bidFacts(lang: Lang, lot: AuctionLot): List<String> = if (lot.equipment != null) {
    listOfNotNull(ui("common.level_short", lot.itemLevel), lot.slot?.let { slotTitle(it.name, lang) })
} else {
    stackFacts(lang, lot)
}

/** Пометки торгов: число ставок (нет ставок - приглушённо) и где ставка героя. */
internal fun bidMarks(lot: AuctionLot): List<TradeMark> = listOfNotNull(
    if (lot.bidCount > 0) TradeMark(ui("auction.bids_count", lot.bidCount), Quiet) else TradeMark(ui("auction.no_bids"), Muted),
    TradeMark(ui("auction.you_lead"), Vital).takeIf { lot.leading },
    TradeMark(ui("auction.you_outbid"), LifeRed).takeIf { lot.outbid },
)

/** Колонка цены торгов: подпись, ставка крупно (без ставок - первая ставка), выкуп под ней. */
@Composable private fun BidColumn(lot: AuctionLot) {
    val bid = lot.currentBid
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(ui(if (bid > 0) "auction.bid_label" else "auction.first_bid"), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        OrbAmount(lot.priceOrb, if (bid > 0) bid else lot.bidding?.minPrice ?: 0, big = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui("auction.buyout_label"), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            OrbAmount(lot.priceOrb, lot.price)
        }
    }
}

/** Число сфер [code] со знаком сферы: крупно [big] - ставка, мелко - выкуп и запасы. */
@Composable private fun OrbAmount(code: String, amount: Long, big: Boolean = false, tint: Color = GoldBright) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            number(amount.toDouble()),
            color = tint,
            style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
            fontWeight = if (big) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
        )
        Orb.of(code)?.let { OrbGlyph(it, Modifier.size(if (big) 18.dp else 13.dp)) } ?: Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(if (big) 16.dp else 12.dp))
    }
}

/** Полоса оставшегося срока торгов: доля [share] от полного срока лота; [ending] - последние часы, красная. */
@Composable private fun TimeBar(share: Float, ending: Boolean) {
    Box(Modifier.fillMaxWidth().height(3.dp).background(Abyss)) {
        Box(Modifier.fillMaxWidth(share).fillMaxHeight().background(if (ending) LifeRed else Gold.copy(alpha = .7f)))
    }
}

/**
 * Лист ставки: сверху товар [goods] (карточка вещи, у стопки - значок и имя) и факты торгов - текущая или первая ставка с числом ставок,
 * выкуп, срок к «сейчас» [now] (часы сервера), продавец (касание - карточка игрока) и правило торгов. Внизу закреплены: где ставка героя,
 * шаги «−» / «+» и «Мин. ставка», запас сфер и недостача, кнопки «Поставить N» и «Выкупить за M» - удержанием. Шаг, наименьшая ставка и
 * залог (сферы и сбор) - правила `:core` ([minBid], [bidCost]) против сумки и кошелька героя [hero]; своя лидирующая ставка не
 * перебивается, свой лот - только смотреть, лот другого героя своего аккаунта - кнопки погашены и сказано почему ([sameAccount]).
 */
@Composable internal fun BidSheet(
    lot: AuctionLot,
    rules: AuctionRules,
    index: ContentIndex,
    hero: HeroView?,
    enabled: Boolean,
    now: Long,
    goods: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onBid: (Long) -> Unit,
    onBuyout: () -> Unit,
) {
    val own = hero != null && lot.belongsTo(hero.id)
    val account = lot.sameAccount(hero?.info?.userId)
    val least = lot.minBid(rules)
    var amount by remember(lot.id, least) { mutableLongStateOf(least) }
    val bidCost = lot.bidCost(index, amount)
    val bidShort = hero?.shortfall(bidCost)?.text()
    val buyShort = hero?.shortfall(lot.cost)?.text()
    val open = enabled && lot.onSaleAt(now) && !own && !account
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = if (own) {
            null
        } else {
            {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    standingText(lot)?.let { (text, tint) -> Text(text, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
                    if (account) Text(ui("auction.own_account"), color = Caution, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ForgeIconButton(Icons.Outlined.Remove, ui("auction.bid_less"), { amount = lot.bidDown(rules, amount) }, kind = ButtonKind.Secondary, state = ButtonState.of(open && amount > least), repeatOnHold = true)
                        Box(
                            Modifier.weight(1f).height(ButtonSize.Wide.height).background(Abyss, RoundedCornerShape(10.dp)).border(1.dp, if (bidShort != null) LifeRed else Bronze, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { OrbAmount(lot.priceOrb, amount, big = true, tint = if (bidShort != null) LossText else GoldBright) }
                        ForgeIconButton(Icons.Outlined.Add, ui("auction.bid_more"), { amount = lot.bidUp(rules, amount) }, kind = ButtonKind.Secondary, state = ButtonState.of(open && amount < lot.price), repeatOnHold = true)
                        ForgeButton(onClick = { amount = least }, kind = ButtonKind.Secondary, state = ButtonState.of(open && amount != least)) { Text(ui("auction.min_bid"), maxLines = 1) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MutedText(ui("auction.bid_have", number((hero?.count(lot.priceOrb) ?: 0L).toDouble())), Modifier.weight(1f))
                        Text(bidShort ?: ui("auction.bid_step", percent(rules.bidStep), rules.bidStepMin), color = if (bidShort != null) LossText else Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForgeButton(
                            onClick = { onBid(amount) },
                            modifier = Modifier.weight(1f),
                            kind = ButtonKind.Secondary,
                            state = ButtonState.of(open && bidShort == null && !lot.leading),
                            hold = Hold(),
                        ) { Text(ui("auction.bid_do", number(amount.toDouble())), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        ForgeButton(
                            onClick = onBuyout,
                            modifier = Modifier.weight(1f),
                            state = ButtonState.of(open && buyShort == null),
                            hold = Hold(),
                        ) { Text(ui("auction.buyout_do", number(lot.price.toDouble())), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    buyShort?.takeIf { bidShort == null }?.let { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
    ) {
        goods()
        ForgePanel {
            val bid = lot.currentBid
            if (bid > 0) {
                PropertyRow(ui("auction.current_bid"), "${number(bid.toDouble())} ${orbTitle(lot)} · ${ui("auction.bids_count", lot.bidCount)}", Glyph.ITEM)
            } else {
                PropertyRow(ui("auction.first_bid_title"), "${number((lot.bidding?.minPrice ?: 0L).toDouble())} ${orbTitle(lot)} · ${ui("auction.no_bids")}", Glyph.ITEM)
            }
            PropertyRow(ui("auction.buyout"), "${number(lot.price.toDouble())} ${orbTitle(lot)}", Glyph.ITEM)
            lotLeft(lot, now)?.let { PropertyRow(ui("auction.expires_in"), it, Glyph.STATE) }
            if (!own) Box(Modifier.opensPlayer(lot.sellerId)) { PropertyRow(ui("auction.seller"), sellerName(lot), Glyph.CHARACTER) }
            if (own) MutedText(ui("auction.own_lot"), style = MaterialTheme.typography.labelMedium)
            MutedText(ui("auction.bid_rules", percent(rules.bidStep), rules.bidTailHours, rules.lotDays))
            MutedText(ui("auction.bid_fee", percent(rules.buyerFee)))
        }
    }
}

/** Где ставка героя словами и цветом: лидирует - зелёным, перебита - красным; не ставил - null. */
internal fun standingText(lot: AuctionLot): Pair<String, Color>? = when {
    lot.leading -> ui("auction.bid_leading") to Vital
    lot.outbid -> ui("auction.bid_outbid") to LifeRed
    else -> null
}

/** Доля правил в процентах без лишнего «.0». */
internal fun percent(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/** Товар торгов в листе: копия - карточкой вещи со сравнением с надетым, стопка - значком, именем и числом. */
@Composable internal fun BidGoods(game: GameUi, lot: AuctionLot) {
    val copy = lot.equipment
    val view = copy?.let { game.view(it) }
    if (copy != null && view != null) {
        ItemCard(
            view,
            enabled = false,
            detailed = true,
            priced = false,
            totals = wearTotals(game, copy, rememberWearChoice(game, copy)),
            unmet = game.unmetNeeds(copy.template),
            seals = game.itemSeals(copy),
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LotIcon(game, lot, Modifier.size(28.dp))
            Text(lot.title, color = lotColor(lot), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (lot.amount > 1) Text(ui("auction.pieces", lot.amount), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}
