package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketDue
import com.sperance.exileforge.core.market.due
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.AuctionRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.trade.BidEntry
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.*

/**
 * Вкладка «Аукцион»: торги под той же строкой поиска и фильтром, что витрина ([ShowcaseHeader]); строка торгов - [BidRow], касание
 * открывает лист ставки ([BidSheet]). Не загрузились - отказ и «Повторить», а не «ничего не найдено»; «Показать ещё» - следующая страница.
 */
@Composable internal fun ColumnScope.BidsTab(game: GameUi, market: Market, vm: MarketViewModel) {
    var openLot by remember { mutableStateOf<String?>(null) }
    val rules = game.index?.rules?.auction
    val page = market.auctions
    val now = rememberServerNow(LOT_TICK_MS)
    Box(Modifier.padding(top = 10.dp)) { CollapsibleHeader { ShowcaseHeader(game, market, vm) } }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
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
        items(page.items, key = { it.id }) { lot -> BidRow(lot, bidFacts(game.lang, lot), lotVisualKind(game, lot), rules, now, enabled = !game.busy, due = rules?.let { lot.due(game.heroId, it, now) }) { openLot = lot.id } }
        if (page.next != null) {
            item {
                ForgeButton(state = ButtonState.of(!game.busy && Reads.AUCTION !in game.loading), onClick = vm::moreAuctions, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                    Text(ui("auction.more", page.items.size, page.totalItems))
                }
            }
        }
    }
    page.items.firstOrNull { it.id == openLot }?.let { lot -> BidSheetOf(game, lot, now, vm) { openLot = null } }
}

/**
 * Лист ставки [BidSheet] торгов [lot] из среза игры: правила, герой, счёт золота и товар карточкой, медиана похожих сделок - тихим
 * чтением подсказки цены (одна на вещь за сеанс); команды - модели рынка [vm].
 */
@Composable internal fun BidSheetOf(game: GameUi, lot: AuctionLot, now: Long, vm: MarketViewModel, onDismiss: () -> Unit) {
    val index = game.index ?: return
    val rules = index.rules.auction
    val hint by produceState<PriceHint?>(null, lot.itemCode, lot.rarity, lot.itemLevel) { value = vm.priceHint(lot) }
    BidSheet(
        lot,
        rules,
        index,
        game.hero,
        enabled = !game.busy,
        now = now,
        hint = hint,
        goldBill = game::goldBill,
        goods = { BidGoods(game, lot) },
        onDismiss = onDismiss,
        onBid = { amount -> vm.bid(lot.id, amount) },
        onBuyout = { vm.buyout(lot.id) },
    )
}

/**
 * Строка торгов (вариант A), как [TradeRow], из готовых данных: плита товара - значок вида [visual], имя цветом редкости, факты [facts] и
 * пометки (ставок N, «вы лидер» / «ставку перебили»), срок к «сейчас» [now] (часы сервера); справа колонка - ставка крупно (без ставок -
 * первая ставка) и выкуп под ней; снизу полоса оставшегося срока, в последние часы хвоста ставки - красная. Лидер и перебитая ставка -
 * цветной кромкой слева: зелёной и красной. [due] - дело строки ([MarketDue]): жёлтая точка у пометки «ставку перебили» или «можно продать».
 */
@Composable internal fun BidRow(lot: AuctionLot, facts: List<String>, visual: ItemVisualKind, rules: AuctionRules?, now: Long, enabled: Boolean, due: MarketDue? = null, onClick: () -> Unit) {
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
                TradeFacts(facts, bidMarks(lot, due))
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

/** Пометки торгов: число ставок (нет ставок - приглушённо), где ставка героя и дело строки [due] - с жёлтой точкой. */
private fun bidMarks(lot: AuctionLot, due: MarketDue?): List<TradeMark> = listOfNotNull(
    if (lot.bidCount > 0) TradeMark(ui("auction.bids_count", lot.bidCount), Quiet) else TradeMark(ui("auction.no_bids"), Muted),
    TradeMark(ui("auction.you_lead"), Vital).takeIf { lot.leading },
    TradeMark(ui("auction.you_outbid"), LifeRed, due = due == MarketDue.OUTBID).takeIf { lot.outbid },
    TradeMark(ui("auction.sell_now"), Caution, due = true).takeIf { due == MarketDue.SELL },
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

/** Число сфер [code] со знаком сферы: крупно [big] - ставка, мелко - выкуп, лента и запасы. */
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
 * Лист ставки сверху вниз: товар [goods] (карточка вещи, у стопки - значок и имя), где ставка героя ([BidStatus]: перебита - что
 * вернулось письмом, лидирует - залог в торгах), срок «через N» к «сейчас» [now] (часы сервера; касание - точное время) с полосой,
 * шкала цены от первой ставки до выкупа с текущей, своей суммой и медианой похожих сделок [hint] ([PriceScale]), правила торгов - в ⓘ,
 * лента ставок без имён ([BidFeed]) и продавец (касание - карточка игрока). Внизу закреплены: когда кончатся торги, если поставить сейчас
 * ([BidTailLine]), шаги «−» / «+» с наименьшей ставкой и шагом, залог «Спишется сейчас» - сферы и сбор против сумки и кошелька
 * ([BidPledge], золото - счёт [goldBill]), сбор выкупа и кнопки «Поставить N» и «Выкупить за M» - удержанием. Шаг, наименьшая ставка,
 * залог и срок - правила `:core` ([minBid], [bidCost], [endsAfterBid]); своя лидирующая ставка не перебивается, свой лот - только
 * смотреть, лот другого героя своего аккаунта - кнопки погашены и сказано почему ([sameAccount]).
 */
@Composable internal fun BidSheet(
    lot: AuctionLot,
    rules: AuctionRules,
    index: ContentIndex,
    hero: HeroView?,
    enabled: Boolean,
    now: Long,
    hint: PriceHint?,
    goldBill: (Long) -> GoldBill?,
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
    val bidding = open && !lot.leading
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = if (own) {
            null
        } else {
            {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (account) Text(ui("auction.own_account"), color = Caution, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    BidTailLine(lot, rules, now)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ForgeIconButton(Icons.Outlined.Remove, ui("auction.bid_less"), { amount = lot.bidDown(rules, amount) }, kind = ButtonKind.Secondary, state = ButtonState.of(bidding && amount > least), repeatOnHold = true)
                        Box(
                            Modifier.weight(1f).height(ButtonSize.Wide.height).background(Abyss, RoundedCornerShape(10.dp)).border(1.dp, if (bidShort != null && bidding) LifeRed else Bronze, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { OrbAmount(lot.priceOrb, amount, big = true, tint = if (!bidding) Muted else if (bidShort != null) LossText else GoldBright) }
                        ForgeIconButton(Icons.Outlined.Add, ui("auction.bid_more"), { amount = lot.bidUp(rules, amount) }, kind = ButtonKind.Secondary, state = ButtonState.of(bidding && amount < lot.price), repeatOnHold = true)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MutedText(ui("auction.bid_min_hint", number(least.toDouble())))
                        MutedText(ui("auction.bid_step", percent(rules.bidStep), rules.bidStepMin))
                    }
                    BidPledge(lot, index, hero, if (lot.leading) null else amount, goldBill)
                    bidShort?.takeIf { bidding }?.let { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
                    goldBill(lot.fee)?.let { bill ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MutedText(ui("auction.buyout_fee"), Modifier.weight(1f))
                            GoldCost(bill)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForgeButton(
                            onClick = { onBid(amount) },
                            modifier = Modifier.weight(1f),
                            kind = ButtonKind.Secondary,
                            state = ButtonState.of(bidding && bidShort == null),
                            hold = Hold(),
                        ) { Text(ui(if (lot.leading) "auction.you_lead_button" else "auction.bid_do", number(amount.toDouble())), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        ForgeButton(
                            onClick = onBuyout,
                            modifier = Modifier.weight(1f),
                            state = ButtonState.of(open && buyShort == null),
                            hold = Hold(),
                        ) { Text(ui("auction.buyout_do", number(lot.price.toDouble())), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    buyShort?.takeIf { open && (bidShort == null || !bidding) }?.let { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
    ) {
        goods()
        BidStatus(lot, index, own)
        BidTerm(lot, rules, now)
        ForgePanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val bid = lot.currentBid
                Text(ui(if (bid > 0) "auction.current_bid" else "auction.first_bid_title"), color = Parchment, style = MaterialTheme.typography.titleSmall)
                OrbAmount(lot.priceOrb, if (bid > 0) bid else lot.bidding?.minPrice ?: 0)
                Spacer(Modifier.weight(1f))
                InfoMark(ui("auction.rules_about"), Rune) {
                    Text(ui("auction.rules_about"), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(ui("auction.bid_rules", percent(rules.bidStep), rules.bidTailHours, rules.lotDays), color = Parchment, style = MaterialTheme.typography.bodySmall)
                    Text(ui("auction.bid_fee", percent(rules.buyerFee)), color = Parchment, style = MaterialTheme.typography.bodySmall)
                }
            }
            PriceScale(lot, if (lot.leading || !open) null else amount, lot.median(hint), hint?.sales ?: 0)
        }
        ForgePanel { BidFeed(lot, now, if (bidding) amount else null) }
        if (!own) Box(Modifier.opensPlayer(lot.sellerId)) { PropertyRow(ui("auction.seller"), sellerName(lot), Glyph.CHARACTER) }
    }
}

/**
 * Где ставка героя: перебита - красной плашкой, что его последняя ставка ленты и её сбор вернулись письмом; лидирует - зелёной, что
 * залог (ставка и сбор) лежит в торгах; свой лот - что купить его нельзя. Не ставил - ничего.
 */
@Composable private fun BidStatus(lot: AuctionLot, index: ContentIndex, own: Boolean) {
    val (title, tint, detail) = when {
        own -> Triple(ui("auction.own_lot"), Caution, null)
        lot.leading -> Triple(ui("auction.bid_leading"), Vital, ui("auction.status_held", "${number(lot.currentBid.toDouble())} ${orbTitle(lot)}", number(lot.bidFee(index, lot.currentBid).toDouble())))
        lot.outbid -> Triple(ui("auction.bid_outbid"), LifeRed, lot.lastOwnBid?.let { ui("auction.status_back", "${number(it.toDouble())} ${orbTitle(lot)}", number(lot.bidFee(index, it).toDouble())) })
        else -> return
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().background(tint.copy(alpha = .1f), shape).border(1.dp, tint.copy(alpha = .45f), shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            detail?.let { MutedText(it) }
        }
    }
}

/** Срок торгов: «Срок · через N» к «сейчас» [now] (касание - точное время конца, если ставок больше не будет) и полоса срока. */
@Composable private fun BidTerm(lot: AuctionLot, rules: AuctionRules, now: Long) {
    val left = lotLeft(lot, now) ?: return
    val ending = lot.bidEnding(rules, now)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(ui("auction.term"), color = Quiet, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Tipped({ Tip(body = ui("auction.ends_exact", stampText(lot.expiresAt))) }, Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.CenterEnd) {
                Text(ui("auction.in_time", left), color = if (ending) LossText else Parchment, style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.Underline)
            }
        }
        TimeBar(lot.timeShare(rules, now), ending)
    }
}

/**
 * Шкала цены торгов от первой ставки до выкупа: залита до текущей ставки, отметки - текущая, своя сумма [mine] (null - не ставит:
 * лидирует или торги закрыты ему) и медиана похожих сделок [median] из [sales]; под ней края и подписи отметок цветами отметок.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PriceScale(lot: AuctionLot, mine: Long?, median: Long?, sales: Int) {
    val bid = lot.currentBid
    val low = lot.bidding?.minPrice ?: 0L
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().height(14.dp).drawBehind {
                val track = 6.dp.toPx()
                val top = (size.height - track) / 2
                drawRoundRect(Abyss, Offset(0f, top), Size(size.width, track), CornerRadius(track / 2))
                if (bid > 0) drawRoundRect(Gold.copy(alpha = .6f), Offset(0f, top), Size(size.width * lot.priceShare(bid), track), CornerRadius(track / 2))
                fun mark(amount: Long, color: Color) {
                    val x = (size.width * lot.priceShare(amount)).coerceIn(1.5.dp.toPx(), size.width - 1.5.dp.toPx())
                    drawLine(color, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx(), StrokeCap.Round)
                }
                median?.let { mark(it, Rune) }
                if (bid > 0) mark(bid, GoldBright)
                mine?.let { mark(it, Caution) }
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MutedText(ui("auction.scale_first", number(low.toDouble())))
            MutedText(ui("auction.scale_buyout", number(lot.price.toDouble())))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (bid > 0) ScaleKey(GoldBright, ui(if (lot.leading) "auction.scale_now_mine" else "auction.scale_now", number(bid.toDouble())))
            mine?.let { ScaleKey(Caution, ui("auction.scale_mine", number(it.toDouble()))) }
            median?.let { ScaleKey(Rune, ui("auction.scale_median", number(it.toDouble()), sales)) } ?: MutedText(ui("auction.scale_no_median"))
        }
    }
}

/** Подпись отметки шкалы: знак её цвета [color] и текст [text]. */
@Composable private fun ScaleKey(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(width = 3.dp, height = 10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(text, color = Quiet, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Лента ставок без имён, новые сверху: сумма, «вы» у своих (у лидирующей своей - зелёным), «лидирует» у верхней и давность к «сейчас»
 * [now]; над ней пунктиром - своя сумма [pending], пока ставка не сделана. Видны [FEED_SHOWN] верхних, остальные свёрнуты «Ещё N».
 * Ставок нет - что своя будет первой и наименьшая.
 */
@Composable private fun BidFeed(lot: AuctionLot, now: Long, pending: Long?) {
    val feed = lot.feed
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("auction.feed"), color = Parchment, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (lot.bidCount > 0) MutedText(ui("auction.feed_meta", lot.bidCount))
    }
    pending?.let { PendingBid(lot, it) }
    if (feed.isEmpty()) {
        MutedText(if (lot.bidCount > 0) ui("auction.bids_count", lot.bidCount) else ui("auction.feed_empty", "${number((lot.bidding?.minPrice ?: 0L).toDouble())} ${orbTitle(lot)}"))
        return
    }
    feed.take(FEED_SHOWN).forEachIndexed { at, entry -> FeedRow(lot, entry, top = at == 0, now) }
    val rest = feed.drop(FEED_SHOWN)
    if (rest.isEmpty()) return
    var open by remember(lot.id) { mutableStateOf(false) }
    val label = ui("auction.feed_more", rest.size)
    Row(Modifier.fillMaxWidth().foldToggle(open, label) { open = !open }.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        FoldChevron(open)
    }
    Column { FoldBody(open) { rest.forEach { FeedRow(lot, it, top = false, now) } } }
}

/** Строка ленты: сумма со знаком сферы, «вы» у своей, «лидирует» у верхней [top] и давность. */
@Composable private fun FeedRow(lot: AuctionLot, entry: BidEntry, top: Boolean, now: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OrbAmount(lot.priceOrb, entry.orbs, tint = if (top) GoldBright else Quiet)
        if (entry.own) Text(ui("auction.feed_you"), color = if (top) Vital else Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(if (top) ui("auction.feed_leads") else "", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(agoText(now - entry.at), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Своя сумма [amount] над лентой пунктиром: ставка ещё не сделана. */
@Composable private fun PendingBid(lot: AuctionLot, amount: Long) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().dashedBorder(Caution.copy(alpha = .7f), shape).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OrbAmount(lot.priceOrb, amount, tint = Caution)
        Text(ui("auction.feed_pending"), color = Caution, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
    }
}

/** Сколько верхних ставок ленты видно без раскрытия. */
private const val FEED_SHOWN = 4

/**
 * Когда кончатся торги, если поставить сейчас ([BidTail]): не раньше такого-то времени через хвост ставки, раньше нынешнего конца -
 * предупреждение, предел срока лота - что дальше не продлят; своя лидирует - что перебитая ставка вернётся и торги продлятся.
 */
@Composable private fun BidTailLine(lot: AuctionLot, rules: AuctionRules, now: Long) {
    val at = stampText(lot.endsAfterBid(rules, now))
    val (text, tint) = when {
        lot.leading -> ui("auction.tail_leading", rules.bidTailHours) to Muted
        else -> when (lot.bidTail(rules, now)) {
            BidTail.EXTENDS -> ui("auction.tail_extends", at, rules.bidTailHours) to Muted
            BidTail.SHORTENS -> ui("auction.tail_shortens", at, rules.bidTailHours, lotLeft(lot, now).orEmpty()) to Caution
            BidTail.CAPPED -> ui("auction.tail_capped", at) to Muted
        }
    }
    Text(text, color = tint, style = MaterialTheme.typography.bodySmall)
}

/**
 * Залог ставки: «Спишется сейчас» - сферы ставки [amount] против сумки и сбор золотом против кошелька ([goldBill]); своя лидирует
 * ([amount] null) - «Ваш залог в торгах»: что уже лежит в лоте.
 */
@Composable private fun BidPledge(lot: AuctionLot, index: ContentIndex, hero: HeroView?, amount: Long?, goldBill: (Long) -> GoldBill?) {
    val orbs = amount ?: lot.currentBid
    val fee = lot.bidFee(index, orbs)
    val have = hero?.count(lot.priceOrb) ?: 0L
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MutedText(ui(if (amount == null) "auction.pledge_held" else "auction.pledge_now"), Modifier.weight(1f))
        OrbAmount(lot.priceOrb, orbs, tint = if (amount != null && have < orbs) LossText else GoldBright)
        if (amount != null) MutedText("/ ${compactNumber(have.toDouble())}")
        MutedText(ui("auction.pledge_fee"))
        if (amount == null) GoldPrice(fee) else goldBill(fee)?.let { GoldCost(it) } ?: GoldPrice(fee)
    }
}

/** Давность ставки [ago] мс: «только что», минуты, часы, дни. */
private fun agoText(ago: Long): String {
    val minutes = ago.coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> ui("auction.ago_now")
        minutes < 60 -> ui("auction.ago_minutes", minutes)
        minutes < MINUTES_A_DAY -> ui("auction.ago_hours", minutes / 60)
        else -> ui("auction.ago_days", minutes / MINUTES_A_DAY)
    }
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
