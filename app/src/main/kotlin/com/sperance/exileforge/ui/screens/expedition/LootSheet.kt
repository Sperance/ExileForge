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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.GearLock
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.expedition.LootPresence
import com.sperance.exileforge.core.expedition.lootPresence
import com.sperance.exileforge.core.expedition.saleMarkable
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.unmetNeeds
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.InspectButton
import com.sperance.exileforge.ui.components.InspectChoice
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SheetFooter
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.OwnItemActions
import com.sperance.exileforge.ui.screens.hero.rememberWearChoice
import com.sperance.exileforge.ui.screens.hero.wearTotals
import com.sperance.exileforge.ui.screens.hero.wearable
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Panel
import org.koin.compose.viewmodel.koinViewModel

/**
 * A piece the run dropped, opened where it dropped — a chest's lid, a fight's spoils, the run's reports:
 * its card and what wearing it would change against what is worn. The drop is the server's roll and reaches the stash with the
 * journal's answer, so opening the sheet sends the journal at once; one sold for want of room says so. [extra] is what the caller adds
 * under the card.
 *
 * Карточка прокручивается, действия - в закреплённом подвале ([SheetFooter]). В походе по карте находку захода можно надеть между
 * боями - широкой «Надеть» с тем, что изменится, над ней ([runWearAction]); в бою она погашена с причиной; испытание и итоги закрытого
 * захода её не показывают. Копия героя берётся по id: выданная сервером несёт номер экземпляра. Легшую в тайник вещь можно пометить к
 * продаже (в итогах карты она уже отмечена) или продать торговцу сразу - удержанием в ряду своей вещи ([OwnItemActions]).
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
    val choice = rememberWearChoice(game, item.item)
    val wearing = rememberRunWearing(vm)?.takeIf { it.lock != GearLock.OVER && wearable(game, item.item) }
    LaunchedEffect(item.id, stand) { if (stand == LootPresence.ARRIVING) vm.flushRun() }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val shown = remember(item, game.hero) { game.hero?.item(item.id)?.takeIf { it.serial != item.item.serial }?.let { ItemView(it, item.template, item.index) } ?: item }
                ItemCard(shown, enabled = false, detailed = true, price = game.sellPrice(item.item), totals = wearTotals(game, item.item, choice), unmet = game.unmetNeeds(item.code), seals = game.itemSeals(item.item))
                when (stand) {
                    LootPresence.WORN -> MutedText(ui("expedition.loot_worn"))
                    LootPresence.GONE -> MutedText(ui("expedition.loot_gone"))
                    LootPresence.ARRIVING -> MutedText(ui("expedition.loot_arriving"))
                    LootPresence.HELD -> Unit
                }
                extra()
            }
            val held = game.hero?.item(item.id)?.takeIf { stand == LootPresence.HELD }
            if (held != null) {
                SheetFooter {
                    wearing?.let {
                        val wear = runWearAction(game, vm, held, null, it)
                        wear.notes?.invoke(this)
                        InspectButton(wear, InspectChoice(choice.target), onDismiss)
                    }
                    if (markable && (item.id in expedition.saleMarks || game.saleMarkable(item.id))) {
                        val marked = item.id in expedition.saleMarks
                        ForgeButton(onClick = { vm.toggleSaleMark(item.id) }, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) {
                            Icon(ForgeGlyphs.Coins, null, tint = if (marked) GoldBright else Gold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ui(if (marked) "sell.unmark" else "sell.mark"))
                        }
                    }
                    OwnItemActions(game, held, onDone = onDismiss)
                }
            }
        }
    }
}
