package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Panel
import org.koin.compose.viewmodel.koinViewModel

/**
 * A piece the run dropped, opened where it dropped (3.24.0) — a chest's lid, a fight's spoils, the run's reports:
 * its card and what wearing it would change against what is worn. Снаряжение в заходе не меняется и не продаётся: надеть
 * или продать вещь можно только после захода. The drop is the server's roll and reaches the stash with the journal's
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
    markable: Boolean = true,
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

                // В заходе торговцу не продают (3.90.4; проверяет только клиент, 3.91.1): вещь помечают к продаже (3.91.0) - в итогах карты
                // она уже отмечена. В самих итогах ([markable] = false) отмечают их же выбором.
                LootPresence.HELD -> if (markable) {
                    val marked = item.id in expedition.saleMarks
                    ForgeOutlinedButton(onClick = { vm.toggleSaleMark(item.id) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(ForgeGlyphs.Coins, null, tint = if (marked) GoldBright else Gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(ui(if (marked) "sell.unmark" else "sell.mark"))
                    }
                    MutedText(ui("expedition.loot_sell_after"))
                } else {
                    MutedText(ui("expedition.loot_sell_after"))
                }
            }
            extra()
        }
    }
}
