package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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
 * Без шапки: строка Города уже называет здание; места лотов - числом «занято/всего» у вкладки «Мои лоты» ([slotsBadge]). Открытие вкладки наполняет списки; лист героя нужен правилам, чтобы
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
        val tabs = MarketTab.entries.map(::tabTitle)
        val tab = market.tab
        PillTabs(tabs, tab.ordinal, { model.tab(MarketTab.entries[it]) }, enabled = !busy, badges = MarketTab.entries.map { if (it == MarketTab.LOTS) slotsBadge(market) else null })
        PullToRefreshBox(isRefreshing = busy || Reads.AUCTION in activity.loading || Reads.LOTS in activity.loading, onRefresh = model::loadAuction, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (tab) {
                    MarketTab.SHOWCASE -> ShowcaseTab(game, market, model)
                    MarketTab.AUCTION -> BidsTab(game, market, model)
                    MarketTab.LOTS -> MyLotsTab(game, market, model)
                    MarketTab.HISTORY -> HistoryTab(game, market)
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

/** Места лотов у вкладки «Мои лоты»: «3/5», все заняты - красным; пока мест не знаем - число своих лотов на продаже. */
private fun slotsBadge(market: Market): TabBadge? = market.slots?.let { TabBadge(ui("auction.slots_value", it.used, it.limit), alert = it.full) }
    ?: market.ownLots.count { !it.expiredBack }.takeIf { it > 0 }?.let { TabBadge(it.toString()) }
