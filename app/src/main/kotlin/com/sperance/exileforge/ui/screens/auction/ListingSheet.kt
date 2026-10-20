package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.rules.trade.AuctionFees
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Listing something on the auction: one item from its card or the sell tab, or part of a stack.
 *
 * The price is counted in any orb (`AU_007`), so the orb is picked from the auction's currencies
 * by the orb grid ([OrbGrid]) and the amount typed; whether the listing stands is the server's to say. [owned] is given for a
 * stack, and then the sheet also asks how many of them — how many there are is said, not enforced.
 *
 * Заголовка нет: лист начинают значок [icon] и имя [name], имя выбранной сферы - в подписи поля цены, под сеткой его нет. Медиана
 * недавних сделок - только при достаточном их числе, касание берёт её. Плата за место - правилом `AuctionFees`, та же, что спишет сервер;
 * что она не возвращается и что товар лежит в лоте - в ⓘ у платы. Не хватает золота - кнопка гаснет с недостачей; все места лотов
 * заняты - гаснет «Места заняты» (мест ещё не знаем - лист их спрашивает). Аукцион ещё закрыт - кнопка неактивна, рядом сказано, когда
 * откроется.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingSheet(
    game: GameUi,
    name: String,
    owned: Long? = null,
    onDismiss: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    hint: (suspend () -> PriceHint?)? = null,
    onList: (orb: String, price: Long, amount: Long) -> Unit,
) {
    val model = koinViewModel<MarketViewModel>()
    val market by model.market.collectAsStateWithLifecycle()
    var orb by remember { mutableStateOf(game.currencies.firstOrNull()?.code?.value.orEmpty()) }
    var price by remember { mutableStateOf("1") }
    var amount by remember { mutableStateOf("1") }
    val cost = price.toLongOrNull() ?: 0L
    val count = if (owned == null) 1L else amount.toLongOrNull() ?: 0L
    val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
    val recent by produceState<PriceHint?>(null, hint) { value = hint?.invoke() }
    val fee = game.index?.takeIf { orb.isNotBlank() && cost > 0 }?.let { AuctionFees.listing(it, game.heroLevel, orb, cost) } ?: 0L
    val bill = game.goldBill(fee).takeIf { fee > 0 }
    val closed = auctionClosed(game)
    val full = market.slots?.full == true
    LaunchedEffect(closed == null) { if (closed == null && market.slots == null) model.glanceLots() }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = {
            val ready = closed == null && !full && bill?.affordable != false && !game.busy && orb.isNotBlank() && cost > 0 && count > 0
            ForgeButton(state = ButtonState.of(ready), onClick = { onList(orb, cost, count) }, modifier = Modifier.weight(1f)) {
                Text(if (full) ui("auction.slots_full") else bill?.payLabel(ui("sell.list")) ?: ui("sell.list"))
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon?.invoke()
            Text(name, color = Parchment, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        if (owned != null) {
            OutlinedTextField(
                amount,
                { value -> amount = value.filter(Char::isDigit).take(game.inputs.number) },
                label = { Text(ui("sell.amount_owned", owned)) },
                singleLine = true,
                keyboardOptions = digits,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (game.currencies.isEmpty()) {
            Text(ui("orb.none"), color = Muted)
        } else {
            OrbGrid(null, game.currencies, orb, { orb = it }, enabled = !game.busy, named = false)
        }
        OutlinedTextField(
            price,
            { value -> price = value.filter(Char::isDigit).take(game.inputs.number) },
            label = { Text(if (orb.isBlank()) ui("sell.price") else ui("sell.price_in", itemTitle(orb))) },
            singleLine = true,
            keyboardOptions = digits,
            modifier = Modifier.fillMaxWidth(),
        )
        recent?.let { h ->
            ForgeButton(onClick = {
                orb = h.priceOrb
                price = (h.price * count.coerceAtLeast(1)).toString()
            }, kind = ButtonKind.Text, contentPadding = PaddingValues(0.dp)) {
                Text(ui("sell.price_hint", h.price, itemTitle(h.priceOrb), h.sales), style = MaterialTheme.typography.bodySmall)
            }
        }
        bill?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MutedText(ui("sell.listing_fee"), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                GoldCost(it)
                InfoMark(ui("sell.listing_fee"), Rune) { Text(ui("sell.note"), color = Parchment, style = MaterialTheme.typography.bodySmall) }
            }
        }
        closed?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Почему выставить лот нельзя: аукцион откроется с уровня из правил; null - можно. */
fun auctionClosed(game: GameUi): String? = Feature.AUCTION.takeUnless { game.unlocked(it) }?.let { ui("auction.closed_until", it.level(game.index?.rules), game.heroLevel) }
