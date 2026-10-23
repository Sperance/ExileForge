package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketTab
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Рынок игроков - здание Города: витрина лотов за цену, торги со ставками («Аукцион»), свои лоты и ставки, история сделок.
 * The Hero tab is left alone — an item is sold from here, not from the stash.
 *
 * The market opens at a level the server keeps to itself, so the screen does not guard the gate:
 * it asks, and turns the refusal into an explanation.
 *
 * Лот открывается страницей - уровнем цепочки «Рынок › <вкладка> › Лот» вместо вкладок ([LotPage]); лота больше нигде нет - страница
 * закрывается сама. Без шапки: строка Города уже называет здание; места лотов - числом «занято/всего» у вкладки «Мои лоты» ([slotsBadge]). Открытие вкладки наполняет списки; лист героя нужен правилам, чтобы
 * пометить лот, который не надеть. Вещь выставляют из её карточки, стопку - из сумки. Вкладки перечитываются потягиванием.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen() {
    val game by koinViewModel<MarketViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val model = koinViewModel<MarketViewModel>()
    val market by model.market.collectAsStateWithLifecycle()
    val activity by model.activity.collectAsStateWithLifecycle()
    val busy = activity.busy
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LaunchedEffect(game.heroId, game.sessionEpoch) {
            if (game.heroId.isNotBlank()) {
                heroModel.ensure()
                model.openAuction()
            }
        }
        market.locked?.let { locked ->
            InfoCard(ui("auction.closed"), locked, failure = true)
            ForgeButton(state = ButtonState.of(!busy), onClick = model::loadAuction, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                Text(ui("auction.check_again"))
            }
            return@Column
        }
        val tab = market.tab
        var opened by rememberSaveable { mutableStateOf<String?>(null) }
        val close = { opened = null }
        val open = { id: String -> opened = id }
        opened?.let { id ->
            val lot = market.lot(id)
            LaunchedEffect(lot == null) { if (lot == null) close() }
            lot ?: return@Column
            TrailLevel(tabTitle(tab), onBack = close) {
                TrailLevel(ui("auction.lot_title"), onBack = close) { LotPage(game, lot, market.feeCut, model, onClose = close) }
            }
            return@Column
        }
        val chores by model.chores.collectAsStateWithLifecycle()
        MarketTabs(market, due = chores.total > 0, enabled = !busy) { model.tab(it) }
        PullToRefreshBox(isRefreshing = busy || Reads.AUCTION in activity.loading || Reads.LOTS in activity.loading, onRefresh = model::loadAuction, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (tab) {
                    MarketTab.SHOWCASE -> ShowcaseTab(game, market, model, open)
                    MarketTab.AUCTION -> BidsTab(game, market, model, open)
                    MarketTab.LOTS -> MyLotsTab(game, market, model, open)
                    MarketTab.HISTORY -> HistoryTab(game, market, open)
                }
            }
        }
    }
}

/** Подпись вкладки рынка: Витрина · Аукцион · Мои лоты · История. */
private fun tabTitle(tab: MarketTab): String = when (tab) {
    MarketTab.SHOWCASE -> ui("auction.showcase")
    MarketTab.AUCTION -> ui("auction.tab_auction")
    MarketTab.LOTS -> ui("auction.my_lots")
    MarketTab.HISTORY -> ui("auction.history")
}

/** Вкладки рынка; у «Моих лотов» - места ([slotsBadge]) и жёлтая точка, пока там есть дело рынка ([due]). */
@Composable private fun MarketTabs(market: Market, due: Boolean, enabled: Boolean, onSelect: (MarketTab) -> Unit) {
    val badges = MarketTab.entries.map { if (it == MarketTab.LOTS) slotsBadge(market, due) else null }
    PillTabs(MarketTab.entries.map(::tabTitle), market.tab.ordinal, { onSelect(MarketTab.entries[it]) }, enabled = enabled, badges = badges)
}

/**
 * Места лотов у вкладки «Мои лоты»: «3/5», все заняты - красным; пока мест не знаем - число своих лотов на продаже; [due] - точка
 * дела.
 */
private fun slotsBadge(market: Market, due: Boolean): TabBadge? = market.slots?.let { TabBadge(ui("auction.slots_value", it.used, it.limit), alert = it.full, due = due) }
    ?: market.ownLots.count { !it.expiredBack }.takeIf { it > 0 || due }?.let { TabBadge(if (it > 0) it.toString() else "", due = due) }
