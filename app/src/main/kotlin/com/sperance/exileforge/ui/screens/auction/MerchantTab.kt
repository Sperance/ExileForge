package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.filter.itemShelf
import com.sperance.exileforge.core.filter.railGroups
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
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
 * Своей шапки нет: золото героя и время до нового товара - справа в цепочке «Город › Торговец» ([MerchantTrail]), кошелёк больше
 * нигде не печатается - в строках только цена ([GoldCost] без «/ есть», красная при нехватке). ⓘ с правилами торговца - справа над товаром.
 *
 * Копия без шаблона в контенте не показывается. Товар - под общим фильтром предметов. Таймер на нуле - витрина перечитывается,
 * карточка ушедшего товара закрывается. Трофеи боссов - над товаром.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.MerchantTab(game: GameUi, market: MarketViewModel, heroModel: HeroViewModel) {
    val activity by market.activity.collectAsStateWithLifecycle()
    val busy = activity.busy
    var chosen by remember { mutableStateOf<MerchantOffer?>(null) }
    var notes by remember { mutableStateOf(false) }
    val inspect = rememberInspect()
    val stock = game.hero?.merchant
    val money = game.hero?.money
    val offers = remember(stock?.offers, game.index, game.world) { stock?.offers.orEmpty().mapNotNull { offer -> game.view(offer.item)?.let { offer to it } } }
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
    MerchantTrail(money, stock?.refreshAt)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ForgeIconButton(Icons.Outlined.Info, ui("merchant.notes"), onClick = { notes = true }, size = ButtonSize.Compact)
    }
    PullToRefreshBox(isRefreshing = Reads.HERO in activity.loading, onRefresh = heroModel::load, modifier = Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            if (game.hero?.info?.trophies.orEmpty().isNotEmpty()) item(key = "trophies") { TrophyShelf(game, busy, market::tradeTrophy) }
            if (stock != null && offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
            if (offers.isNotEmpty()) item(key = "filter") { ItemFilterBar(filterState, visible.size, onOpen = { filtering = true }) }
            if (offers.isNotEmpty() && visible.isEmpty()) item { InfoCard(ui("tree.nothing_found"), ui("filter.empty")) }
            items(visible.mapNotNull(byId::get), key = { it.first.id }) { (offer, view) ->
                ItemTradeRow(view, enabled = !busy, unmet = game.unmetFor(offer.item.template), onClick = { chosen = offer }) { game.goldBill(offer.price)?.let { GoldCost(it, purse = false) } ?: GoldPrice(offer.price) }
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
            game.heroLevel,
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

/**
 * Данные цепочки торговца: «8 450 золота · ↻ 2:14» - золото героя [money] и часы с минутами до нового товара [refreshAt] по часам
 * сервера, тикает живыми часами экрана; касание - когда товар обновится.
 */
@Composable private fun MerchantTrail(money: Long?, refreshAt: Long?) {
    val leftMs = rememberServerLeft(refreshAt ?: 0, TRAIL_TICK_MS)
    val left = refreshAt?.takeIf { it > 0 }?.let { countdown(leftMs / 1_000) }
    val data = listOfNotNull(money?.let { ui("merchant.gold_amount", number(it.toDouble())) }, left?.let { ui("merchant.trail_renews", it) }).joinToString(" · ")
    if (data.isNotEmpty()) TrailData(data, left?.let { Tip(ui("merchant.renews", it)) })
}

/** Такт часов цепочки торговца. */
private const val TRAIL_TICK_MS = 1_000L

/** Насколько позже нуля таймера витрина перечитывается: окно полки сервера уже сменилось. */
private const val RENEW_SLACK_MS = 1_500L

/** Секунды как `h:mm`, минуты - с округлением вверх: последняя минута не показывает «0:00». */
private fun countdown(seconds: Long): String {
    val minutes = (seconds + 59) / 60
    return "%d:%02d".format(minutes / 60, minutes % 60)
}

/** Правила торговца - как витрина обновляется и как назначена цена - за ⓘ над товаром. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantNotes(onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("merchant.title"))
            Text(ui("merchant.note"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * An offer's full card: the item as the stash would show it, scrolling, and under it
 * the one way to buy it — a button held for the price. Short of gold, it says so and the button
 * stays off; what wearing it would change is added up here, as on a stash card.
 *
 * Общая для любого товара за золото: лавка Города и странствующий торговец карты.
 *
 * Цена против кошелька - одним чипом рядом с кнопкой удержания «Купить» (у карточки плашки цены нет); не хватает - кнопка пишет
 * недостачу и гаснет.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferSheet(game: GameUi, item: ItemInstance, price: Long, onDismiss: () -> Unit, onBuy: () -> Unit) {
    val view = game.view(item)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (view != null) item { ItemCard(view, enabled = false, detailed = true, priced = false, totals = wearTotals(game, item, rememberWearChoice(game, item)), unmet = game.unmetNeeds(item.template), seals = game.itemSeals(item)) }
            }
            OrnateDivider()
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val bill = game.goldBill(price)
                bill?.let { GoldCost(it) } ?: GoldPrice(price)
                ForgeButton(onClick = onBuy, modifier = Modifier.weight(1f), state = ButtonState.of(!game.busy && bill?.affordable == true), hold = Hold()) {
                    Text(bill?.payLabel(ui("auction.buy")) ?: ui("auction.buy"))
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
