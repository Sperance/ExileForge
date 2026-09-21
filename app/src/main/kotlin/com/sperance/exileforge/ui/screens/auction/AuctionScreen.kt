package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs

/**
 * The player auction.
 *
 * Three jobs live here and nowhere else: the showcase, the character's own lots and listing
 * something new. The Hero tab is left alone — an item is sold from here, not from the stash.
 *
 * The auction opens at a level the server keeps to itself, so the screen does not guard the gate:
 * it asks, and turns the refusal into an explanation.
 */
@Composable fun AuctionScreen(s: ForgeState, vm: ForgeViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(tr("Аукцион", "Auction"),
            tr("Лотов на витрине: ${s.showcase.totalItems}", "${s.showcase.totalItems} lots on the showcase"), ForgeGlyphs.Orb)
        // Opening the tab is what fills both lists; the character is the one from the menu.
        LaunchedEffect(s.characterId, s.sessionEpoch) { if (s.characterId.isNotBlank()) vm.loadAuction() }
        s.auctionLocked?.let { locked ->
            InfoCard(tr("Аукцион закрыт", "The auction is closed"), locked, failure = true)
            OutlinedButton(enabled = !s.busy, onClick = vm::loadAuction, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Проверить снова", "Check again"))
            }
            Text(tr("Порог называет сервер: клиент не хранит его копию и узнаёт только по отказу.",
                    "The threshold is the server's: the client keeps no copy and learns it from the refusal."),
                color = com.sperance.exileforge.ui.theme.Muted, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        val tabs = listOf(tr("Витрина", "Showcase"), tr("Мои лоты", "My lots"), tr("Выставить", "Sell"))
        TabRow(selectedTabIndex = s.auctionTab, containerColor = com.sperance.exileforge.ui.theme.Abyss) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = s.auctionTab == index, enabled = !s.busy, onClick = { vm.auctionTab(index) },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) })
            }
        }
        when (s.auctionTab) {
            0 -> ShowcaseTab(s, vm)
            1 -> MyLotsTab(s, vm)
            else -> SellTab(s, vm)
        }
    }
}
