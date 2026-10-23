package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.GearLock
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fullIfShortened
import com.sperance.exileforge.core.display.requirementShort
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.WearBar
import com.sperance.exileforge.core.market.wearBar
import com.sperance.exileforge.core.market.wearable
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.core.model.trade.MerchantOffer
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.WearRequest
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.hero.WearChoice
import com.sperance.exileforge.ui.screens.hero.placeTitle
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
 * нигде не печатается - в строках только цена ([GoldCost] без «/ есть», красная при нехватке).
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
    val inspect = rememberInspect()
    val stock = game.hero?.merchant
    val money = game.hero?.money
    val offers = remember(stock?.offers, game.index, game.world) { stock?.offers.orEmpty().mapNotNull { offer -> game.view(offer.item)?.let { offer to it } } }
    val filterState = rememberItemFilter(ItemShelf.MERCHANT)
    var filtering by remember { mutableStateOf(false) }
    val prices = remember(offers) { offers.associate { (offer, view) -> view.id to offer.price } }
    val wares = remember(offers) { offers.map { it.second } }
    val shelf = rememberItemShelf(game, wares, filterState) { prices[it.id] }
    val byId = remember(offers) { offers.associateBy { it.first.item.id } }
    val visible = remember(shelf, byId) { shelf?.visible.orEmpty().mapNotNull { byId[it.id] } }
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
    PullToRefreshBox(isRefreshing = Reads.HERO in activity.loading, onRefresh = heroModel::load, modifier = Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            if (game.hero?.info?.trophies.orEmpty().isNotEmpty()) item(key = "trophies") { TrophyShelf(game, busy, market::tradeTrophy) }
            if (stock != null && offers.isEmpty()) item { InfoCard(ui("merchant.empty"), ui("merchant.empty_hint")) }
            if (offers.isNotEmpty()) item(key = "filter") { ItemSearchBar(filterState, ui("merchant.search"), game.lang, onFilter = { filtering = true }) }
            if (offers.isNotEmpty() && shelf != null && visible.isEmpty()) item { InfoCard(ui("tree.nothing_found"), ui("filter.empty")) }
            items(visible, key = { it.first.id }) { (offer, view) ->
                ItemTradeRow(view, enabled = !busy, unmet = game.unmetFor(offer.item), onClick = { chosen = offer }) { game.goldBill(offer.price)?.let { GoldCost(it, purse = false) } ?: GoldPrice(offer.price) }
            }
        }
    }
    if (filtering && shelf != null) ItemFilterSheet(filterState, shelf) { filtering = false }
    chosen?.let { offer ->
        OfferSheet(
            game,
            offer.item,
            offer.price,
            WearVenue.Town,
            onDismiss = { chosen = null },
            onBuyWorn = { wear ->
                chosen = null
                market.buyOfferWorn(offer.id, wear)
            },
        ) {
            chosen = null
            market.buyOffer(offer.id)
        }
    }
}

/**
 * Данные цепочки торговца: «8 450 золота · ↻ 2:14» - золото героя [money] и часы с минутами до нового товара [refreshAt] по часам
 * сервера, тикает живыми часами экрана; золото - короткой записью ([compactNumber]); касание - когда товар обновится и, если золото
 * сокращено, его полное число.
 */
@Composable private fun MerchantTrail(money: Long?, refreshAt: Long?) {
    val leftMs = rememberServerLeft(refreshAt ?: 0, TRAIL_TICK_MS)
    val left = refreshAt?.takeIf { it > 0 }?.let { countdown(leftMs / 1_000) }
    val data = listOfNotNull(money?.let { ui("merchant.gold_amount", compactNumber(it)) }, left?.let { ui("merchant.trail_renews", it) }).joinToString(" · ")
    val full = money?.let(::fullIfShortened)?.let { ui("gold.have") to it }
    val tip = if (left != null || full != null) Tip(left?.let { ui("merchant.renews", it) }, facts = listOfNotNull(full)) else null
    if (data.isNotEmpty()) TrailData(data, tip)
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

/** Где покупают с надеванием: торговец Города или бродячий торговец в походе по карте с замком набора [lock] (null - сменить можно). */
internal sealed interface WearVenue {
    /** Торговец Города: надеть - по правилу убежища. */
    data object Town : WearVenue

    /** Бродячий торговец карты: надеть - по правилу смены набора похода, сейчас мешает [lock]. */
    data class MapRun(val lock: GearLock?) : WearVenue
}

/**
 * An offer's full card: the item as the stash would show it, scrolling, and under it
 * the one way to buy it — a button held for the price. Short of gold, it says so and the button
 * stays off; what wearing it would change is added up here, as on a stash card.
 *
 * Общая для любого товара за золото: лавка Города и странствующий торговец карты ([venue]).
 *
 * Цена против кошелька - одним чипом рядом с кнопкой удержания «Купить» (у карточки плашки цены нет); не хватает - кнопка пишет
 * недостачу и гаснет. Под ней у вещи, что надевается, - «Купить и надеть» удержанием ([onBuyWorn]): мест несколько (кольца, фляги) -
 * сперва лист «Куда надеть» ([WearPlaceSheet]); нельзя надеть ([wearBar], замок похода) - кнопка погашена, причина под ней.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferSheet(game: GameUi, item: ItemInstance, price: Long, venue: WearVenue, onDismiss: () -> Unit, onBuyWorn: (WearRequest) -> Unit, onBuy: () -> Unit) {
    val view = game.view(item)
    val choice = rememberWearChoice(game, item)
    var picking by remember(item.id) { mutableStateOf(false) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (view != null) item { ItemCard(view, enabled = false, detailed = true, totals = wearTotals(game, item, choice), unmet = game.unmetNeeds(item), seals = game.itemSeals(item)) }
            }
            OrnateDivider()
            val bill = game.goldBill(price)
            val affordable = !game.busy && bill?.affordable == true
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    bill?.let { GoldCost(it) } ?: GoldPrice(price)
                    ForgeButton(onClick = onBuy, modifier = Modifier.weight(1f), state = ButtonState.of(affordable), hold = Hold()) {
                        Text(bill?.payLabel(ui("auction.buy")) ?: ui("auction.buy"))
                    }
                }
                if (game.wearable(item)) {
                    val place = choice.target
                    val reason = remember(item, game.hero, game.index, place, venue) { wearReason(game, item, place, venue) }
                    ForgeButton(
                        onClick = { if (choice.choosable) picking = true else onBuyWorn(WearRequest(null)) },
                        modifier = Modifier.fillMaxWidth(),
                        kind = ButtonKind.Secondary,
                        state = ButtonState.of(affordable && reason == null),
                        hold = if (choice.choosable) null else Hold(),
                    ) { Text(ui("merchant.buy_wear"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    reason?.let { Text(it, color = LossText, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
    if (picking) {
        WearPlaceSheet(game, choice, onDismiss = { picking = false }) { slot ->
            picking = false
            onBuyWorn(WearRequest(slot))
        }
    }
}

/**
 * Почему вещь [item] нельзя купить и сразу надеть на место [place] у торговца [venue]: замок набора похода, требования («Нужно: Сила 48»),
 * поход по карте, забег Разлома, полный тайник, вторая копия уникалки; null - можно.
 */
private fun wearReason(game: GameUi, item: ItemInstance, place: Slot?, venue: WearVenue): String? {
    if (venue is WearVenue.MapRun) venue.lock?.let { return ui(it.key) }
    return when (game.wearBar(item, place, onMap = venue is WearVenue.MapRun)) {
        WearBar.NEEDS -> ui("merchant.wear_needs", game.unmetFor(item).joinToString(", ") { requirementShort(it, game.lang) })
        WearBar.MAP_RUN -> ui("merchant.wear_map_run")
        WearBar.RIFT -> ui("merchant.wear_rift")
        WearBar.STASH_FULL -> ui("merchant.wear_stash_full")
        WearBar.TWIN -> ui("hero.unique_worn")
        null -> null
    }
}

/**
 * Лист «Куда надеть» вещи слота с несколькими местами (кольца, фляги): строка на место - его имя и что там надето (свободно - так и
 * сказано); касание удержанием покупает и надевает туда ([onPick]).
 */
@Composable private fun WearPlaceSheet(game: GameUi, choice: WearChoice, onDismiss: () -> Unit, onPick: (Slot) -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss, body = SheetBody.Scroll()) {
        Text(ui("merchant.wear_where"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
        choice.places.forEach { place ->
            ForgeButton(onClick = { onPick(place.slot) }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, state = ButtonState.of(!game.busy), hold = Hold()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(placeTitle(place.slot), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (place.free) ui("wear.place_free") else ui("wear.instead", place.removed.joinToString { equipmentTitle(it.template) }),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
