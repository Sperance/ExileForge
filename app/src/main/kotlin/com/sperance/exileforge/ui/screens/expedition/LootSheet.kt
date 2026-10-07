package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.LootPresence
import com.sperance.exileforge.presentation.state.lootPresence
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.HoldButton
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Panel
import org.koin.compose.viewmodel.koinViewModel

/**
 * A piece the run dropped, opened where it dropped (3.24.0) — a chest's lid, a fight's spoils, the run's reports:
 * its card and what wearing it would change against what is worn. Снаряжение в заходе не меняется: надеть вещь можно
 * только в убежище, здесь её можно лишь продать. The drop is the server's roll and reaches the stash with the journal's
 * answer, so opening the sheet sends the journal at once; one sold for want of room says so.
 * [extra] is what the caller adds under the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LootSheet(
    game: GameUi,
    vm: ExpeditionViewModel,
    item: ItemView,
    onDismiss: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val expedition by vm.state.collectAsStateWithLifecycle()
    val stand = game.lootPresence(item.id, arriving = expedition.pending > 0)
    LaunchedEffect(item.id, stand) { if (stand == LootPresence.ARRIVING) vm.flushRun() }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemCard(item, enabled = false, detailed = true, price = game.sellPrice(item.item), totals = wearTotals(game, item.item, rememberWearChoice(game, item.item)), requirementsMet = game.unmetFor(item.code).isEmpty())
            when (stand) {
                LootPresence.WORN -> MutedText(ui("expedition.loot_worn"))
                LootPresence.GONE -> MutedText(ui("expedition.loot_gone"))
                LootPresence.ARRIVING -> MutedText(ui("expedition.loot_arriving"))
                LootPresence.HELD -> Unit
            }
            // Sold to the merchant right here (3.81.0), wherever the piece is opened on a run: a gilt ribbon with the coin and
            // the price in a chip, held as before. A locked piece (3.30.0) is not sold: the ribbon stays, dimmed, with the reason.
            if (stand == LootPresence.HELD) {
                val locked = game.hero?.item(item.id)?.locked == true
                HoldButton(
                    ui("expedition.loot_sell"),
                    Gold,
                    Modifier.fillMaxWidth(),
                    enabled = !game.busy && !locked,
                    icon = ForgeGlyphs.Coins,
                    figure = game.sellPrice(item.item)?.let { "+$it" },
                ) {
                    onDismiss()
                    vm.sellForGold(item.id)
                }
                if (locked) MutedText(ui("item.locked_hint"))
            }
            extra()
        }
    }
}
