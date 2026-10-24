package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.AuctionRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.*

/**
 * Страница лота - уровень цепочки «Рынок › <вкладка> › Лот» вместо листа: один вид для всех лотов рынка (торги, лот за цену, свой лот,
 * сделка истории), одна прокрутка сверху вниз: статус и срок одной строкой над вещью ([LotStatusLine]), что
 * сказать о ставке героя или своём лоте ([LotNote]), карточка вещи со свойствами, свёрнутыми до двух ([LotGoods]), у торгов - панель
 * «Торги»: шкала цены и лента ставок ([TradePanel]), факты лота и продавец ([LotFacts]), в самом низу - чек: «Ваша ставка» у чужих
 * торгов ([YourBid]), «Покупка» у лота за цену ([PurchaseCheck]). Закреплены внизу только кнопки действия ([LotActions]). Что лот
 * разрешает герою - [stance] ядра; расчёты ставок - `:core` ([minBid], [bidCost], [buyoutAfterBid], [endsAfterBid]). [cut] - скидка
 * сбора героя от древа гильдии; команды - модели рынка [vm]; снятие и продажа ставке возвращают к списку ([onClose]).
 */
@Composable internal fun LotPage(game: GameUi, lot: AuctionLot, cut: Double, vm: MarketViewModel, onClose: () -> Unit) {
    val index = game.index ?: return
    val rules = index.rules.auction
    val now = rememberServerNow(LOT_TICK_MS)
    val stance = lot.stance(game.heroId)
    val hint by produceState<PriceHint?>(null, lot.id, stance) { value = if (lot.isBidding && stance != LotStance.CLOSED) vm.priceHint(lot) else null }
    val least = lot.minBid(rules)
    val draft = remember(lot.id, least) { mutableLongStateOf(least) }
    val bid = remember(lot, index, game, cut, now, draft) { BidDraft(lot, rules, index, game, cut, now, draft) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LotStatusLine(lot, stance, rules, now)
            LotNote(game, lot, index, cut, stance)
            LotGoods(game, lot)
            if (lot.isBidding) TradePanel(lot, rules, now, hint, bid.amount.takeIf { stance == LotStance.BID && bid.bidding })
            LotFacts(game, lot, stance, rules, now, vm)
            when (stance) {
                LotStance.BID -> YourBid(bid)
                LotStance.BUY -> PurchaseCheck(game, lot, cut)
                LotStance.OWN_FIXED, LotStance.OWN_BID, LotStance.CLOSED -> Unit
            }
        }
        LotActions(game, lot, stance, bid, now, vm, onClose)
    }
}

/**
 * Ставка героя в торгах [lot] к «сейчас» [now], пока страница открыта: сумма [amount] (шаги «−» / «+» - правила `:core`), что она
 * спишет и чего не хватает; [open] - торги идут и не своего аккаунта, [bidding] - ещё и своя ставка не лидирует. Сбор - со скидкой [cut].
 */
@Stable
internal class BidDraft(
    val lot: AuctionLot,
    val rules: AuctionRules,
    val index: ContentIndex,
    val game: GameUi,
    val cut: Double,
    val now: Long,
    private val state: MutableLongState,
) {
    val amount: Long get() = state.longValue
    val least: Long = lot.minBid(rules)
    val account: Boolean = lot.sameAccount(game.hero?.info?.userId)
    val open: Boolean = !game.busy && lot.onSaleAt(now) && !account
    val bidding: Boolean = open && !lot.leading

    /** Чего герою не хватает на ставку [amount]: сферы ставки и сбор; null - хватает. */
    val bidShort: String? get() = game.shortfall(lot.bidCost(index, amount, cut))?.text()

    /** Чего герою не хватает на выкуп. */
    val buyShort: String? = game.shortfall(lot.cost)?.text()

    fun less() {
        state.longValue = lot.bidDown(rules, amount)
    }

    fun more() {
        state.longValue = lot.bidUp(rules, amount)
    }
}

/**
 * Статус и срок одной строкой над вещью: метка - торги, за цену, свой лот, «вы лидер», «ставку перебили», закрыт; «через N» к «сейчас»
 * [now] (касание - точное время конца) и число ставок. Конец торгов близко - не только красным, но и словами «скоро конец»; у торгов
 * под строкой - полоса оставшегося срока.
 */
@Composable private fun LotStatusLine(lot: AuctionLot, stance: LotStance, rules: AuctionRules, now: Long) {
    val (label, tint) = statusMark(lot, stance)
    val ending = lot.isBidding && lot.bidEnding(rules, now)
    val left = lotLeft(lot, now)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToneTag(label, tint, size = TagSize.Medium)
            left?.let {
                Tipped({ Tip(body = ui("auction.ends_exact", stampText(lot.expiresAt))) }, Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.CenterStart) {
                    Text(ui("auction.in_time", it), color = if (ending) LossText else Parchment, style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.Underline, maxLines = 1)
                }
            }
            if (ending) Text(ui("auction.lot_ending"), color = LossText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (lot.isBidding) MutedText(ui("auction.bids_count", lot.bidCount))
        }
        if (lot.isBidding && left != null) TimeBar(lot.timeShare(rules, now), ending)
    }
}

/** Метка статуса лота и её цвет: свой, ставка героя, вид лота или закрыт. */
private fun statusMark(lot: AuctionLot, stance: LotStance): Pair<String, Color> = when (stance) {
    LotStance.CLOSED -> ui("auction.lot_closed") to Muted

    LotStance.OWN_FIXED, LotStance.OWN_BID -> ui("auction.your_lot") to Caution

    LotStance.BID -> when {
        lot.leading -> ui("auction.you_lead") to Vital
        lot.outbid -> ui("auction.you_outbid") to LifeRed
        else -> ui("auction.lot_bidding") to Rune
    }

    LotStance.BUY -> ui("auction.lot_fixed") to Rune
}

/**
 * Что сказать под статусом: ставка героя лидирует - залог в торгах; перебита - что вернулось письмом; торги своего аккаунта - почему
 * ставить нельзя; свои торги со ставкой - что ставка вернётся покупателю при снятии и что значит «Продать»; свой вернувшийся лот -
 * куда; сделка истории - кто, кому и когда.
 */
@Composable private fun LotNote(game: GameUi, lot: AuctionLot, index: ContentIndex, cut: Double, stance: LotStance) {
    val orbs = { amount: Long -> "${number(amount.toDouble())} ${orbTitle(lot)}" }
    val lines: List<Pair<String, Color>> = when (stance) {
        LotStance.BID -> listOfNotNull(
            (ui("auction.status_held", orbs(lot.currentBid), number(lot.bidFee(index, lot.currentBid, cut).toDouble())) to Vital).takeIf { lot.leading },
            lot.lastOwnBid?.takeIf { lot.outbid }?.let { ui("auction.status_back", orbs(it), number(lot.bidFee(index, it, cut).toDouble())) to Muted },
            (ui("auction.own_account") to Caution).takeIf { lot.sameAccount(game.hero?.info?.userId) },
        )

        LotStance.OWN_BID -> lot.currentBid.takeIf { it > 0 }?.let { listOf(ui("auction.withdraw_bid_warn", orbs(it)) to Muted, ui("auction.accept_note") to Muted) }.orEmpty()

        LotStance.OWN_FIXED -> listOfNotNull(lotReturn(lot)?.let { it to Muted })

        LotStance.CLOSED -> listOfNotNull((if (lot.soldAt > 0) dealMark(lot, game.heroId) else lotReturn(lot))?.let { it to Muted })

        LotStance.BUY -> emptyList()
    }
    lines.forEach { (text, tint) -> Text(text, color = tint, style = MaterialTheme.typography.bodySmall) }
}

/**
 * Товар лота: копия - карточкой вещи, свойства свёрнуты до двух, «Все свойства» раскрывает полную карточку со сравнением с надетым и
 * требованиями; стопка - значком, именем и числом.
 */
@Composable private fun LotGoods(game: GameUi, lot: AuctionLot) {
    val copy = lot.equipment
    val view = copy?.let { game.view(it) }
    if (copy == null || view == null) {
        ForgePanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LotIcon(game, lot, 28.dp)
                Text(lot.title, color = lotColor(lot), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (lot.amount > 1) Text(ui("auction.pieces", lot.amount), color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
        return
    }
    var all by remember(lot.id) { mutableStateOf(false) }
    val unmet = game.unmetNeeds(copy)
    if (all) {
        ItemCard(view, enabled = false, detailed = true, totals = wearTotals(game, copy, rememberWearChoice(game, copy)), unmet = unmet, seals = game.itemSeals(copy))
    } else {
        ItemCard(view, enabled = false, action = false, unmet = unmet, shortLines = FOLDED_LINES)
    }
    ForgeButton(onClick = { all = !all }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Text) {
        Text(ui(if (all) "auction.lot_fold" else "auction.lot_all_props"), maxLines = 1)
    }
}

/** Сколько свойств вещи видно на странице лота до «Все свойства». */
private const val FOLDED_LINES = 2

/**
 * Панель «Торги»: текущая ставка (без ставок - первая) и выкуп, правила торгов - в ⓘ; шкала цены от первой ставки до выкупа с текущей,
 * своей суммой [mine] и медианой похожих сделок [hint] ([PriceScale]); ниже - лента ставок, имя - только у лидера ([BidFeed]).
 */
@Composable private fun TradePanel(lot: AuctionLot, rules: AuctionRules, now: Long, hint: PriceHint?, mine: Long?) {
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val bid = lot.currentBid
            Text(ui("auction.lot_trade"), color = Parchment, style = MaterialTheme.typography.titleSmall)
            OrbAmount(lot.priceOrb, if (bid > 0) bid else lot.bidding?.minPrice ?: 0)
            MutedText(ui("auction.lot_buyout", number(lot.price.toDouble())))
            Spacer(Modifier.weight(1f))
            InfoMark(ui("auction.rules_about"), Rune) {
                Text(ui("auction.rules_about"), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(ui("auction.bid_rules", percent(rules.bidStep), rules.bidTailHours, rules.lotDays), color = Parchment, style = MaterialTheme.typography.bodySmall)
                Text(ui("auction.bid_fee", percent(rules.buyerFee)), color = Parchment, style = MaterialTheme.typography.bodySmall)
                Text(ui("auction.buyout_grows", percent(rules.bidStep)), color = Parchment, style = MaterialTheme.typography.bodySmall)
            }
        }
        PriceScale(lot, mine, lot.median(hint), hint?.sales ?: 0)
        HorizontalDivider(thickness = 1.dp, color = Gold.copy(alpha = .15f))
        BidFeed(lot, now, mine)
    }
}

/**
 * Факты лота: продавец (касание - карточка игрока; у своего лота его нет), лидер открытых торгов - ник, класс и уровень (касание -
 * карточка игрока; не видно - строки нет), когда выставлен; свой лот за цену в окне продления -
 * «Продлить на N дн.» с платой ([ExtendRow]).
 */
@Composable private fun LotFacts(game: GameUi, lot: AuctionLot, stance: LotStance, rules: AuctionRules, now: Long, vm: MarketViewModel) {
    val own = stance == LotStance.OWN_FIXED || stance == LotStance.OWN_BID || lot.belongsTo(game.heroId)
    ForgePanel {
        if (!own) Box(Modifier.opensPlayer(lot.sellerId)) { PropertyRow(ui("auction.seller"), sellerName(lot), Glyph.CHARACTER) }
        lot.leader?.let { leader -> Box(Modifier.opensPlayer(leader.hero)) { PropertyRow(ui("auction.leader"), leaderLine(leader), Glyph.CHARACTER) } }
        listedAt(lot.createdAt)?.let { PropertyRow(ui("auction.listed_at"), it, Glyph.LEVEL) }
        if (stance == LotStance.OWN_FIXED && lot.extendable(rules, now)) {
            val fee = game.index?.let { com.sperance.exileforge.rules.trade.AuctionFees.listing(it, game.heroLevel, lot.priceOrb, lot.price) } ?: 0L
            ExtendRow(game, fee, rules) { vm.extend(lot.id) }
        }
    }
}

/**
 * Чек «Ваша ставка» внизу страницы чужих торгов: сумма шагами «−» / «+», наименьшая ставка и шаг, сколько сфер в сумке, сбор ставки,
 * итог «Спишется сейчас» - сферы и сбор (своя лидирует - «Ваш залог в торгах»), «После ставки N выкуп станет M» - от набранной суммы
 * ([buyoutAfterBid]), когда кончатся торги, если поставить ([BidTailLine]), и сбор выкупа. Недостача и запрет своего аккаунта -
 * текстом [LossText] под чеком.
 */
@Composable private fun YourBid(bid: BidDraft) {
    val lot = bid.lot
    val game = bid.game
    val amount = bid.amount
    val leading = lot.leading
    val orbs = if (leading) lot.currentBid else amount
    val fee = lot.bidFee(bid.index, orbs, bid.cut)
    val have = game.hero?.count(lot.priceOrb) ?: 0L
    val short = bid.bidShort.takeIf { bid.bidding }
    Text(ui("auction.your_bid"), color = Parchment, style = MaterialTheme.typography.titleMedium)
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MutedText(ui("auction.your_bid_amount"), Modifier.weight(1f))
            ForgeIconButton(Icons.Outlined.Remove, ui("auction.bid_less"), bid::less, kind = ButtonKind.Secondary, state = ButtonState.of(bid.bidding && amount > bid.least), repeatOnHold = true)
            Box(Modifier.widthIn(min = 72.dp), contentAlignment = Alignment.Center) {
                OrbAmount(
                    lot.priceOrb,
                    orbs,
                    big = true,
                    tint = if (!bid.bidding) {
                        Muted
                    } else if (short != null) {
                        LossText
                    } else {
                        GoldBright
                    },
                )
            }
            ForgeIconButton(Icons.Outlined.Add, ui("auction.bid_more"), bid::more, kind = ButtonKind.Secondary, state = ButtonState.of(bid.bidding && amount < lot.price), repeatOnHold = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MutedText(ui("auction.bid_min_hint", number(bid.least.toDouble())) + " · " + ui("auction.bid_step", percent(bid.rules.bidStep), bid.rules.bidStepMin))
            MutedText(ui("auction.your_bid_have", compactNumber(have.toDouble())), style = MaterialTheme.typography.labelSmall)
        }
        CheckRow(ui("auction.your_bid_fee")) { game.goldBill(fee)?.let { GoldCost(it) } ?: GoldPrice(fee) }
        HorizontalDivider(thickness = 1.dp, color = Gold.copy(alpha = .15f))
        CheckRow(ui(if (leading) "auction.pledge_held" else "auction.pledge_now"), bold = true) {
            OrbAmount(lot.priceOrb, orbs, tint = if (!leading && have < orbs) LossText else GoldBright)
            MutedText(ui("auction.pledge_fee"))
            GoldPrice(fee)
        }
        if (bid.bidding && !lot.buysOut(amount)) {
            val after = lot.buyoutAfterBid(bid.rules, amount)
            CheckRow(ui("auction.your_bid_buyout", compactNumber(amount))) {
                FullNumberTipped(after) { Text(compactNumber(after), color = Parchment, style = MaterialTheme.typography.bodySmall) }
            }
        }
        BidTailLine(lot, bid.rules, bid.now)
        game.goldBill(lot.fee)?.let { bill -> CheckRow(ui("auction.buyout_fee")) { GoldCost(bill) } }
    }
    if (bid.account) Text(ui("auction.own_account"), color = Caution, style = MaterialTheme.typography.bodySmall)
    short?.let { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
    bid.buyShort?.takeIf { bid.open && short == null }?.let { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
}

/**
 * Чек «Покупка» внизу страницы лота за цену: цена в сферах, сбор покупателя (доля цены, со скидкой грамоты гильдии [cut]) против
 * кошелька и итог «Спишется»; недостача - текстом [LossText], вещь, которую не надеть, - чем она не подходит.
 */
@Composable private fun PurchaseCheck(game: GameUi, lot: AuctionLot, cut: Double) {
    val rate = game.index?.rules?.auction?.buyerFee?.let(::percent)
    val blocked = lotUnmet(game, lot)
    Text(ui("auction.lot_purchase"), color = Parchment, style = MaterialTheme.typography.titleMedium)
    ForgePanel {
        CheckRow(ui("auction.lot_price")) { OrbAmount(lot.priceOrb, lot.price) }
        if (lot.fee > 0) CheckRow(feeLabel(rate, cut)) { game.goldBill(lot.fee)?.let { GoldCost(it) } ?: GoldPrice(lot.fee) }
        HorizontalDivider(thickness = 1.dp, color = Gold.copy(alpha = .15f))
        CheckRow(ui("auction.lot_spend"), bold = true) {
            OrbAmount(lot.priceOrb, lot.price)
            if (lot.fee > 0) {
                MutedText(ui("auction.pledge_fee"))
                GoldPrice(lot.fee)
            }
        }
    }
    game.shortfall(lot.cost)?.text()?.let { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
    if (blocked.isNotEmpty()) Text(ui("auction.unwearable", blocked.joinToString(", ") { requirementReason(it, game.lang) }), color = Caution, style = MaterialTheme.typography.bodySmall)
}

/** Строка чека: подпись [label] слева (итог - [bold]) и значение [value] справа. */
@Composable private fun CheckRow(label: String, bold: Boolean = false, value: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            color = if (bold) Parchment else Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}

/** Подпись сбора в чеке покупки: доля цены лота [rate] (правила), со скидкой грамоты гильдии [cut] - и она; без доли - просто «Сбор». */
private fun feeLabel(rate: String?, cut: Double): String = when {
    rate == null -> ui("auction.fee")
    cut > 0 -> ui("auction.fee_rate_cut", rate, number(cut))
    else -> ui("auction.fee_rate", rate)
}

/**
 * Закреплённые кнопки страницы - только действия, удержанием: у чужих торгов широкая «Поставить N» (своя ставка лидирует - погашенная
 * «Вы лидер») и узкая «Выкупить за N»; у лота за цену - «Купить за N»; у своих торгов - «Снять» и при ставке «Продать за N»; у своего
 * лота за цену - «Снять с продажи». Закрытый лот кнопок не имеет.
 */
@Composable private fun LotActions(game: GameUi, lot: AuctionLot, stance: LotStance, bid: BidDraft, now: Long, vm: MarketViewModel, onClose: () -> Unit) {
    if (stance == LotStance.CLOSED) return
    OrnateDivider()
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (stance) {
            LotStance.BID -> {
                ForgeButton(onClick = { vm.bid(lot.id, bid.amount) }, modifier = Modifier.weight(1f), state = ButtonState.of(bid.bidding && bid.bidShort == null), hold = Hold()) {
                    Text(if (lot.leading) ui("auction.you_lead_button") else ui("auction.bid_do", number(bid.amount.toDouble())), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ForgeButton(onClick = { vm.buyout(lot.id) }, modifier = Modifier.weight(.6f), kind = ButtonKind.Secondary, state = ButtonState.of(bid.open && bid.buyShort == null), hold = Hold()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ui("auction.lot_buyout_for"), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Text(compactNumber(lot.price), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            LotStance.BUY -> {
                val short = game.shortfall(lot.cost)?.text()
                ForgeButton(onClick = { vm.buy(lot.id) }, modifier = Modifier.weight(1f), state = ButtonState.of(!game.busy && lot.onSaleAt(now) && short == null), hold = Hold()) {
                    Text(ui("auction.lot_buy_for", number(lot.price.toDouble())), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            LotStance.OWN_BID -> {
                val acceptable = lot.acceptableAt(now)
                ForgeButton(
                    onClick = {
                        onClose()
                        vm.cancel(lot.id)
                    },
                    modifier = Modifier.weight(if (acceptable) .6f else 1f),
                    kind = ButtonKind.Secondary,
                    state = ButtonState.of(!game.busy && lot.onSaleAt(now)),
                    hold = Hold(),
                ) { Text(ui("auction.lot_withdraw_short"), maxLines = 1) }
                if (acceptable) {
                    ForgeButton(
                        onClick = {
                            onClose()
                            vm.accept(lot.id)
                        },
                        modifier = Modifier.weight(1f),
                        state = ButtonState.of(!game.busy),
                        hold = Hold(),
                    ) { Text(ui("auction.accept_do", number(lot.currentBid.toDouble())), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }

            LotStance.OWN_FIXED -> ForgeButton(
                onClick = {
                    onClose()
                    vm.cancel(lot.id)
                },
                modifier = Modifier.weight(1f),
                kind = ButtonKind.Secondary,
                state = ButtonState.of(!game.busy && lot.onSaleAt(now)),
                hold = Hold(),
            ) { Text(ui("auction.withdraw"), maxLines = 1) }

            LotStance.CLOSED -> Unit
        }
    }
}
