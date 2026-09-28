package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import com.sperance.exileforge.presentation.state.Reads
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs

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
@Composable fun AuctionScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(ui("nav.auction"),
            ui("auction.showcase_count", s.market.showcase.totalItems), ForgeGlyphs.Orb, guide = Guide.AUCTION)
        // Opening the tab is what fills both lists; the hero is the one from the menu.
        // The hero comes too, and not for the bag: the sheet is what the rules read to say which
        // templates this hero can wear, and that is what marks an unwearable lot.
        LaunchedEffect(s.play.heroId, s.account.sessionEpoch) {
            if (s.play.heroId.isNotBlank()) { vm.ensureHero(); vm.loadAuction() }
        }
        s.market.locked?.let { locked ->
            InfoCard(ui("auction.closed"), locked, failure = true)
            ForgeOutlinedButton(enabled = !s.busy, onClick = vm::loadAuction, modifier = Modifier.fillMaxWidth()) {
                Text(ui("auction.check_again"))
            }
            return@Column
        }
        val mine = s.ownLots.size
        // «Выставить» left in 2.48.0: an item is listed from its own card, a stack from the bag.
        // The merchant moved out in 3.22.0, to a building of the City of its own.
        val tabs = listOf(ui("auction.showcase"), if (mine > 0) ui("auction.my_lots_n", mine) else ui("auction.my_lots"))
        val tab = s.market.tab.coerceIn(tabs.indices)
        TabRow(selectedTabIndex = tab, containerColor = com.sperance.exileforge.ui.theme.Abyss) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tab == index, enabled = !s.busy, onClick = { vm.auctionTab(index) },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) })
            }
        }
        // Every tab is refreshed the same way the hero is: by pulling it. A button competing with
        // the content was one more thing to find, and the gesture is already the habit here.
        PullToRefreshBox(isRefreshing = s.refreshing(Reads.AUCTION) || Reads.LOTS in s.loading, onRefresh = vm::loadAuction, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (tab == 0) ShowcaseTab(s, vm) else MyLotsTab(s, vm)
            }
        }
    }
}
