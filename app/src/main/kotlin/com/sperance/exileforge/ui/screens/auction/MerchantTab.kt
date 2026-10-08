package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.core.model.trade.MerchantOrb
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.hero.StackInfoSheet
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * The merchant (since 2.36.0, server 0.34.0): a shelf of items the server rolled for this hero,
 * sold for gold, new every few hours and never sooner. The price is the server's — what the
 * merchant would pay for the item, several times over — and buying asks first, held.
 *
 * An offer is a copy like any in the stash (3.0.0): its view over the content draws it, and the
 * rules say whether this hero could wear it.
 *
 * Under the header, the orb shelf (3.15.0, server 1.13.0): lesser orbs for gold, each one of a kind
 * dearer than the last until the shelf renews with the wares.
 *
 * It left the auction's tabs in 3.22.0 for a building of the City of its own.
 */
@Composable fun MerchantScreen() {
    val game by koinViewModel<MarketViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val market = koinViewModel<MarketViewModel>()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Полка едет в снимке героя (3.94.1): вход лишь освежает героя, если он остыл.
        LaunchedEffect(game.heroId, game.sessionEpoch) {
            if (game.heroId.isNotBlank()) heroModel.ensure()
        }
        MerchantTab(game, market, heroModel)
    }
}

/**
 * «Шапка и вкладки» (variant A): the gold, the time to the next shelf and the loot filter in one strip; the notes behind
 * the header's (i); the wares and the orbs as two halves of one switch, so the first item is in sight at once.
 * С 3.90.3 под шапкой только кошелёк: время до нового товара - первой строкой списка, фильтр добычи - автопродажа тайника.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.MerchantTab(game: GameUi, market: MarketViewModel, heroModel: HeroViewModel) {
    val activity by market.activity.collectAsStateWithLifecycle()
    val busy = activity.busy
    var chosen by remember { mutableStateOf<MerchantOffer?>(null) }
    var notes by remember { mutableStateOf(false) }
    var orbsShelf by rememberSaveable { mutableStateOf(false) }
    // The orb a tap on its glass or name opened: what it is for, before it is bought.
    var info by remember { mutableStateOf<String?>(null) }
    val stock = game.hero?.merchant
    val money = game.hero?.money
    // A copy whose template the content does not hold cannot be drawn, and is not offered.
    val offers = remember(stock?.offers, game.index, game.world) { stock?.offers.orEmpty().mapNotNull { offer -> game.view(offer.item)?.let { offer to it } } }
    val orbs = stock?.orbs.orEmpty()
    // Шапка и полоса кошелька уходят при прокрутке вниз (3.88.9), вкладки остаются.
    CollapsibleHeader {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(24.dp))
                Text(ui("merchant.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { notes = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Info, ui("merchant.notes"), tint = Muted) }
            }
            money?.let { MerchantPurse(it) }
        }
    }
    if (orbs.isNotEmpty()) {
        PillTabs(
            listOf(ui("merchant.wares_n", offers.size), ui("merchant.orbs_n", orbs.size)),
            if (orbsShelf) 1 else 0,
            { orbsShelf = it == 1 },
            segmented = true,
        )
    }
    PullToRefreshBox(isRefreshing = Reads.HERO in activity.loading, onRefresh = heroModel::load, modifier = Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            if (orbsShelf && orbs.isNotEmpty()) {
                item {
                    ForgePanel {
                        orbs.forEach { orb ->
                            val price = orb.price
                            val short = game.shortfall(Cost.gold(price))?.text()
                            OrbRow(
                                orb,
                                price,
                                have = game.bagAmount(orb.code),
                                short = short.takeUnless { orb.soldOut },
                                enabled = !busy && !orb.soldOut && short == null,
                                onInfo = { info = orb.code },
                            ) { market.buyOrb(orb.code) }
                        }
                    }
                }
            } else {
                // Таймер обновления (3.90.3) - шапка списка товаров: тикает и уходит с прокруткой.
                stock?.refreshAt?.takeIf { it > 0 }?.let { at -> item(key = "renews") { RenewalRow(at) } }
                if (stock != null && offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
                items(offers, key = { it.first.id }) { (offer, view) ->
                    ItemTradeRow(view, enabled = !busy, unmet = game.unmetFor(offer.item.template), onClick = { chosen = offer }) { GoldPrice(offer.price) }
                }
            }
        }
    }
    if (notes) MerchantNotes { notes = false }
    info?.let { code -> StackInfoSheet(game, code) { info = null } }
    chosen?.let { offer ->
        OfferSheet(game, offer.item, offer.price, money, onDismiss = { chosen = null }) {
            chosen = null
            market.buyOffer(offer.id)
        }
    }
}

/** Кошелёк под шапкой (3.90.3): таймер товара ушёл шапкой списка, фильтр добычи - автопродажей тайника. */
@Composable private fun MerchantPurse(money: Long) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().depthPanel(shape).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        GoldPrice(money)
    }
}

/** «Новый товар через 2:14:03» (3.90.3): первая строка списка товаров, тикает раз в секунду по часам устройства. */
@Composable private fun RenewalRow(at: Long) {
    val now by produceState(System.currentTimeMillis(), at) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    Row(
        Modifier.fillMaxWidth().depthInset(RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = Muted, modifier = Modifier.size(14.dp))
        Text(ui("merchant.renews", countdown(((at - now) / 1_000).coerceAtLeast(0))), color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Секунды как `h:mm:ss`. */
private fun countdown(seconds: Long): String = "%d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)

/** The merchant's two notes — how the shelf renews and is priced, how the orbs grow dearer — behind the header's (i). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantNotes(onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("merchant.title"))
            Text(ui("merchant.note"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            Engraved(ui("merchant.orbs"))
            Text(ui("merchant.orbs_note"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** One orb on the shelf: its glass and name (a tap on them opens the orb), how many the bag holds, and the button with the next price. */
@Composable private fun OrbRow(orb: MerchantOrb, price: Long, have: Long?, short: String?, enabled: Boolean, onInfo: () -> Unit, onBuy: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.weight(1f).clickable(onClickLabel = itemTitle(orb.code), onClick = onInfo),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OrbGlyph(Orb.of(orb.code), Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(itemTitle(orb.code), style = MaterialTheme.typography.bodyMedium)
                have?.let { MutedText(ui("merchant.orb_have", it)) }
                // The window's stock (3.24.0): how many more the shelf holds, in ember once it holds none.
                orb.left?.let { Text(ui("merchant.orb_left", it), color = if (it > 0) Muted else Ember, style = MaterialTheme.typography.labelSmall) }
                // Нехватка золота (3.89.0) - той же строкой, что на аукционе и в ремёслах.
                short?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.labelSmall) }
            }
        }
        ForgeOutlinedButton(enabled = enabled, onClick = onBuy) { GoldPrice(price) }
    }
}

/**
 * An offer's full card (since 2.40.0): the item as the stash would show it, scrolling, and under it
 * the one way to buy it — a button held for the price. Short of gold, it says so and the button
 * stays off (2.46.0); what wearing it would change is added up here, as on a stash card.
 *
 * Общая для любого товара за золото (4.2.0): лавка Города и странствующий торговец карты.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferSheet(game: GameUi, item: ItemInstance, price: Long, money: Long?, onDismiss: () -> Unit, onBuy: () -> Unit) {
    val view = game.view(item)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (view != null) item { ItemCard(view, enabled = false, detailed = true, totals = wearTotals(game, item, rememberWearChoice(game, item)), requirementsMet = game.unmetFor(item.template).isEmpty()) }
            }
            OrnateDivider()
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), Glyph.CURRENCY) }
                val short = game.shortfall(Cost.gold(price))?.text()
                short?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
                HoldButton(
                    ui("merchant.buy_for", number(price.toDouble())),
                    Gold,
                    Modifier.fillMaxWidth(),
                    enabled = !game.busy && short == null,
                    onHeld = onBuy,
                )
            }
        }
    }
}

/** How long until an epoch-millisecond moment, in hours and minutes by the device's clock. */
internal fun untilText(at: Long): String {
    val minutes = ((at - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
    return ui("merchant.time", minutes / 60, minutes % 60)
}
