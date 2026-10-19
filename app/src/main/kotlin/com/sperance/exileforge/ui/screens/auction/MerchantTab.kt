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
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.filter.itemShelf
import com.sperance.exileforge.core.filter.railGroups
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.core.model.trade.MerchantOrb
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.OrbGlyph
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * The merchant: a shelf of items the server rolled for this hero,
 * sold for gold, new every few hours and never sooner. The price is the server's — what the
 * merchant would pay for the item, several times over — and buying asks first, held.
 *
 * An offer is a copy like any in the stash: its view over the content draws it, and the
 * rules say whether this hero could wear it.
 *
 * Under the header, the orb shelf: lesser orbs for gold, each one of a kind
 * dearer than the last until the shelf renews with the wares.
 *
 * It has a building of the City of its own, apart from the auction's tabs.
 *
 * Полка едет в снимке героя: вход лишь освежает остывшего героя.
 */
@Composable fun MerchantScreen() {
    val game by koinViewModel<MarketViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val market = koinViewModel<MarketViewModel>()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LaunchedEffect(game.heroId, game.sessionEpoch) {
            if (game.heroId.isNotBlank()) heroModel.ensure()
        }
        MerchantTab(game, market, heroModel)
    }
}

/**
 * «Шапка и вкладки» (variant A): the gold, the time to the next shelf and the loot filter in one strip; the notes behind
 * the header's (i); the wares and the orbs as two halves of one switch, so the first item is in sight at once.
 * Под шапкой только кошелёк: время до нового товара - первой строкой списка, фильтр добычи - автопродажа тайника.
 *
 * Копия без шаблона в контенте не показывается. Товар - под общим фильтром предметов. Таймер на нуле - витрина и полка сфер перечитываются,
 * карточка ушедшего товара закрывается. Шапка и полоса кошелька уходят при прокрутке, вкладки остаются; трофеи боссов - над товаром.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.MerchantTab(game: GameUi, market: MarketViewModel, heroModel: HeroViewModel) {
    val activity by market.activity.collectAsStateWithLifecycle()
    val busy = activity.busy
    var chosen by remember { mutableStateOf<MerchantOffer?>(null) }
    var notes by remember { mutableStateOf(false) }
    var orbsShelf by rememberSaveable { mutableStateOf(false) }
    val inspect = rememberInspect()
    val stock = game.hero?.merchant
    val money = game.hero?.money
    val offers = remember(stock?.offers, game.index, game.world) { stock?.offers.orEmpty().mapNotNull { offer -> game.view(offer.item)?.let { offer to it } } }
    val orbs = stock?.orbs.orEmpty()
    val filterState = rememberItemFilter(ItemShelf.MERCHANT)
    var filtering by remember { mutableStateOf(false) }
    val prices = remember(offers) { offers.associate { (offer, view) -> view.id to offer.price } }
    val wares = remember(offers) { offers.map { it.second } }
    val shelf = { draft: ItemFilter -> game.itemShelf(wares, ItemShelf.MERCHANT, draft) { prices[it.id] } }
    val visible = remember(offers, filterState.filter, game.hero) { shelf(filterState.filter).map { it.id } }
    val byId = remember(offers) { offers.associateBy { it.first.item.id } }
    stock?.refreshAt?.takeIf { it > 0 }?.let { at ->
        LaunchedEffect(at) {
            delay((at - ServerClock.now()).coerceAtLeast(0) + RENEW_SLACK_MS)
            heroModel.load()
        }
    }
    LaunchedEffect(stock?.offers) {
        if (chosen != null && stock?.offers.orEmpty().none { it.id == chosen?.id }) chosen = null
    }
    CollapsibleHeader {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(24.dp))
                Text(ui("merchant.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                ForgeIconButton(Icons.Outlined.Info, ui("merchant.notes"), onClick = { notes = true }, size = ButtonSize.Compact)
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
                            val bill = game.goldBill(orb.price)
                            OrbRow(
                                orb,
                                bill,
                                have = game.bagAmount(orb.code),
                                enabled = !busy && !orb.soldOut && bill?.affordable == true,
                                onInfo = { inspect(Inspect.Stack(orb.code)) },
                            ) { market.buyOrb(orb.code) }
                        }
                    }
                }
            } else {
                stock?.refreshAt?.takeIf { it > 0 }?.let { at -> item(key = "renews") { RenewalRow(at) } }
                if (game.hero?.info?.trophies.orEmpty().isNotEmpty()) item(key = "trophies") { TrophyShelf(game, busy, market::tradeTrophy) }
                if (stock != null && offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
                if (offers.isNotEmpty()) item(key = "filter") { ItemFilterBar(filterState, visible.size, onOpen = { filtering = true }) }
                if (offers.isNotEmpty() && visible.isEmpty()) item { InfoCard(ui("tree.nothing_found"), ui("filter.empty")) }
                items(visible.mapNotNull(byId::get), key = { it.first.id }) { (offer, view) ->
                    ItemTradeRow(view, enabled = !busy, unmet = game.unmetFor(offer.item.template), onClick = { chosen = offer }) { game.goldBill(offer.price)?.let { GoldCost(it) } ?: GoldPrice(offer.price) }
                }
            }
        }
    }
    if (notes) MerchantNotes { notes = false }
    if (filtering) {
        ItemFilterSheet(
            filterState,
            railGroups(wares).map { it.first },
            wares.map { it.rarity }.distinct().sortedByDescending { it.ordinal },
            game.lang,
            count = { shelf(it).size },
        ) { filtering = false }
    }
    chosen?.let { offer ->
        OfferSheet(game, offer.item, offer.price, onDismiss = { chosen = null }) {
            chosen = null
            market.buyOffer(offer.id)
        }
    }
}

/** Кошелёк под шапкой: таймер товара ушёл шапкой списка, фильтр добычи - автопродажей тайника. */
@Composable private fun MerchantPurse(money: Long) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().depthPanel(shape).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        GoldPrice(money)
    }
}

/** «Новый товар через 2:14:03»: первая строка списка товаров, тикает раз в секунду по часам устройства. */
@Composable private fun RenewalRow(at: Long) {
    val now by produceState(ServerClock.now(), at) {
        while (true) {
            value = ServerClock.now()
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

/** Насколько позже нуля таймера витрина перечитывается: окно полки сервера уже сменилось. */
private const val RENEW_SLACK_MS = 1_500L

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

/**
 * One orb on the shelf: its glass and name (a tap on them opens the orb), how many the bag holds, and the button with the next price.
 * Цена - чип «цена / есть» на кнопке ([GoldCost]); не хватает - цена красная, кнопка погашена.
 *
 * Запас окна - сколько ещё на полке, угольным, когда пусто; нехватка золота - недостачей счёта.
 */
@Composable private fun OrbRow(orb: MerchantOrb, bill: GoldBill?, have: Long?, enabled: Boolean, onInfo: () -> Unit, onBuy: () -> Unit) {
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
                orb.left?.let { Text(ui("merchant.orb_left", it), color = if (it > 0) Muted else Ember, style = MaterialTheme.typography.labelSmall) }
                bill?.takeUnless { it.affordable || orb.soldOut }?.let { Text(it.shortText(), color = LifeRed, style = MaterialTheme.typography.labelSmall) }
            }
        }
        ForgeButton(state = ButtonState.of(enabled), onClick = onBuy, kind = ButtonKind.Secondary) { bill?.let { GoldCost(it) } ?: GoldPrice(orb.price) }
    }
}

/**
 * An offer's full card: the item as the stash would show it, scrolling, and under it
 * the one way to buy it — a button held for the price. Short of gold, it says so and the button
 * stays off; what wearing it would change is added up here, as on a stash card.
 *
 * Общая для любого товара за золото: лавка Города и странствующий торговец карты.
 *
 * Цена против кошелька одним чипом; не хватает - кнопка пишет недостачу и гаснет.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferSheet(game: GameUi, item: ItemInstance, price: Long, onDismiss: () -> Unit, onBuy: () -> Unit) {
    val view = game.view(item)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (view != null) item { ItemCard(view, enabled = false, detailed = true, totals = wearTotals(game, item, rememberWearChoice(game, item)), unmet = game.unmetNeeds(item.template), seals = game.itemSeals(item)) }
            }
            OrnateDivider()
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val bill = game.goldBill(price)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MutedText(ui("merchant.gold"), Modifier.weight(1f))
                    bill?.let { GoldCost(it) } ?: GoldPrice(price)
                }
                ForgeButton(onClick = onBuy, modifier = Modifier.fillMaxWidth(), state = ButtonState.of(!game.busy && bill?.affordable == true), hold = Hold()) {
                    Text(bill?.payLabel(ui("merchant.buy_for", number(price.toDouble()))) ?: ui("merchant.buy_for", number(price.toDouble())))
                }
            }
        }
    }
}

/** How long until an epoch-millisecond moment, in hours and minutes by the device's clock. */
internal fun untilText(at: Long): String {
    val minutes = ((at - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
    return ui("merchant.time", minutes / 60, minutes % 60)
}
