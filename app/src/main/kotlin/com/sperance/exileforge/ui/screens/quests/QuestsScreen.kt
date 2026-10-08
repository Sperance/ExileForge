package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.quests.QuestTab
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The quest board of the City (3.23.0): three sections — dailies, weeklies and the contract board (the story is the
 * expedition's own screen since 4.2.0) — each a ledger of rows. The server rolls and counts; a finished quest is claimed here, a daily replaced, a notice taken off
 * the board or given up. Pulling down reads the board again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen() {
    val game by koinViewModel<QuestViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<QuestViewModel>()
    val quests by vm.quests.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val current by vm.tab.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId, game.sessionEpoch) { if (game.heroId.isNotBlank()) vm.open() }
    val board = quests.board
    val busy = activity.busy
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            CollapsibleHeader { ScreenHeader(ui("quest.title"), ui("quest.subtitle"), ForgeGlyphs.Scroll) }
        }
        TabRow(selectedTabIndex = current.ordinal, containerColor = Abyss) {
            QuestTab.entries.forEach { tab ->
                val ready = readyCount(tab, board)
                // A glyph over a short word, the ready count a badge on the glyph: four long titles did not fit a row.
                Tab(
                    selected = tab == current,
                    onClick = { vm.tab(tab) },
                    icon = {
                        BadgedBox(badge = { if (ready > 0) Badge(containerColor = Vital, contentColor = Ink) { Text(ready.toString(), fontSize = 9.sp) } }) {
                            Icon(tab.glyph, null, modifier = Modifier.size(20.dp))
                        }
                    },
                    text = { Text(ui("quest.tab.${tab.name.lowercase()}"), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                )
            }
        }
        PullToRefreshBox(isRefreshing = Reads.QUESTS in activity.loading, onRefresh = vm::load, modifier = Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (board == null) {
                    item { MutedText(ui("quest.loading")) }
                    return@LazyColumn
                }
                when (current) {
                    QuestTab.DAILY -> daily(vm, busy, board)
                    QuestTab.WEEKLY -> weekly(vm, busy, board)
                    QuestTab.CONTRACTS -> contracts(vm, busy, board)
                }
            }
        }
    }
}

/** How many of a section's quests wait to be claimed. */
private fun readyCount(tab: QuestTab, board: QuestBoard?): Int = when (tab) {
    QuestTab.DAILY -> board?.daily
    QuestTab.WEEKLY -> board?.weekly
    QuestTab.CONTRACTS -> board?.contracts
}.orEmpty().count { it.done && !it.claimed }

private val QuestTab.glyph: ImageVector get() = when (this) {
    QuestTab.DAILY -> ForgeGlyphs.Target
    QuestTab.WEEKLY -> ForgeGlyphs.Banner
    QuestTab.CONTRACTS -> ForgeGlyphs.Scroll
}

private fun LazyListScope.daily(vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    // Dailies are not swapped for gold since 3.24.0 (server 1.22.0): what the day rolled is the day's.
    item { MutedText(ui("quest.resets", untilText(board.dayEndsAt))) }
    if (board.daily.isEmpty()) item { MutedText(ui("quest.none")) }
    items(board.daily, key = { it.id }) { quest -> QuestRow(quest) { ClaimButton(vm, busy, quest) } }
}

private fun LazyListScope.weekly(vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    item { MutedText(ui("quest.resets", weekText(board.weekEndsAt))) }
    if (board.weekly.isEmpty()) item { MutedText(ui("quest.none")) }
    items(board.weekly, key = { it.id }) { quest -> QuestRow(quest) { ClaimButton(vm, busy, quest) } }
}

private fun LazyListScope.contracts(vm: QuestViewModel, busy: Boolean, board: QuestBoard) {
    item { Engraved(ui("quest.contracts_taken", board.contracts.size, board.activeLimit)) }
    if (board.contracts.isEmpty()) item { MutedText(ui("quest.contracts_none")) }
    items(board.contracts, key = { it.id }) { quest ->
        QuestRow(quest) {
            if (!quest.done) ForgeTextButton({ vm.abandon(quest.id) }, enabled = !busy) { Text(ui("quest.abandon"), color = LifeRed) }
            ClaimButton(vm, busy, quest)
        }
    }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Engraved(ui("quest.board"), modifier = Modifier.weight(1f))
            if (board.nextOfferAt > 0) MutedText(ui("quest.next_offer", untilText(board.nextOfferAt)))
        }
    }
    if (board.offers.isEmpty()) item { MutedText(ui("quest.board_empty")) }
    val full = board.contracts.size >= board.activeLimit
    items(board.offers, key = { it.id }) { offer ->
        QuestRow(offer) {
            ForgeButton({ vm.take(offer.id) }, enabled = !busy && !full) { Text(ui("quest.take")) }
        }
    }
}

@Composable internal fun ClaimButton(vm: QuestViewModel, busy: Boolean, quest: Quest) {
    if (quest.done && !quest.claimed) ForgeButton({ vm.claim(quest.id) }, enabled = !busy) { Text(ui("quest.claim")) }
}

/** Days and hours to a week's end — a week does not fit in the merchant's hours and minutes. */
internal fun weekText(at: Long): String {
    val hours = ((at - System.currentTimeMillis()) / 3_600_000).coerceAtLeast(0)
    return ui("quest.days_hours", hours / 24, hours % 24)
}
