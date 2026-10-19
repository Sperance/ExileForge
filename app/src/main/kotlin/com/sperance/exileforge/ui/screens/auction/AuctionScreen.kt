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
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The player auction.
 *
 * Three jobs live here and nowhere else: the showcase, the character's own lots and listing
 * something new. It is a building of the City, entered from its square. The Hero tab is left alone — an item is sold from here, not from the stash.
 *
 * The auction opens at a level the server keeps to itself, so the screen does not guard the gate:
 * it asks, and turns the refusal into an explanation.
 *
 * Без шапки: строка Города уже называет здание, вкладки несут «?». Открытие вкладки наполняет списки; лист героя нужен правилам, чтобы
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
            ForgeButton(enabled = !busy, onClick = model::loadAuction, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                Text(ui("auction.check_again"))
            }
            return@Column
        }
        val mine = market.ownLots.count { !it.expiredBack }
        val tabs = listOf(ui("auction.showcase"), if (mine > 0) ui("auction.my_lots_n", mine) else ui("auction.my_lots"), ui("auction.history"))
        val tab = market.tab.coerceIn(tabs.indices)
        PillTabs(tabs, tab, model::tab, enabled = !busy)
        PullToRefreshBox(isRefreshing = busy || Reads.AUCTION in activity.loading || Reads.LOTS in activity.loading, onRefresh = model::loadAuction, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (tab) {
                    0 -> ShowcaseTab(game, market, model)
                    1 -> MyLotsTab(game, market, model)
                    else -> HistoryTab(game, market)
                }
            }
        }
    }
}
