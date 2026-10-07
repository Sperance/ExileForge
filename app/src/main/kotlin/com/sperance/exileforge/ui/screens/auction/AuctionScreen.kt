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
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The player auction.
 *
 * Three jobs live here and nowhere else: the showcase, the character's own lots and listing
 * something new. Since 3.22.0 it is a building of the City, entered from its square. The Hero tab is left alone — an item is sold from here, not from the stash.
 *
 * The auction opens at a level the server keeps to itself, so the screen does not guard the gate:
 * it asks, and turns the refusal into an explanation.
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
        // No header (variant A): the City's row above already names the building, and the tabs carry its «?».
        // Opening the tab is what fills both lists; the hero is the one from the menu.
        // The hero comes too, and not for the bag: the sheet is what the rules read to say which
        // templates this hero can wear, and that is what marks an unwearable lot.
        LaunchedEffect(game.heroId, game.sessionEpoch) {
            if (game.heroId.isNotBlank()) {
                heroModel.ensure()
                model.openAuction()
            }
        }
        market.locked?.let { locked ->
            InfoCard(ui("auction.closed"), locked, failure = true)
            ForgeOutlinedButton(enabled = !busy, onClick = model::loadAuction, modifier = Modifier.fillMaxWidth()) {
                Text(ui("auction.check_again"))
            }
            return@Column
        }
        val mine = market.ownLots.size
        // «Выставить» left in 2.48.0: an item is listed from its own card, a stack from the bag.
        // The merchant moved out in 3.22.0, to a building of the City of its own.
        val tabs = listOf(ui("auction.showcase"), if (mine > 0) ui("auction.my_lots_n", mine) else ui("auction.my_lots"), ui("auction.history"))
        val tab = market.tab.coerceIn(tabs.indices)
        PillTabs(tabs, tab, model::tab, enabled = !busy)
        // Every tab is refreshed the same way the hero is: by pulling it. A button competing with
        // the content was one more thing to find, and the gesture is already the habit here.
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
