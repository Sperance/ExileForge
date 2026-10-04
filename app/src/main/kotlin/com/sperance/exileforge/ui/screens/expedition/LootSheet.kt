package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.HoldButton
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.WearPreview
import com.sperance.exileforge.ui.screens.hero.wearable
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Panel
import org.koin.compose.viewmodel.koinViewModel

/** Where a dropped piece stands for «Надеть»: the server holds it loose, wears it already, has it on the way, or never got it. */
private enum class LootStand { LOOSE, WORN, ARRIVING, GONE }

private fun lootStand(game: GameUi, item: ItemView, pending: Int): LootStand {
    val held = game.hero?.item(item.id)
    return when {
        held == null -> if (pending > 0) LootStand.ARRIVING else LootStand.GONE
        held.equipped || held.socketed -> LootStand.WORN
        else -> LootStand.LOOSE
    }
}

/**
 * A piece the run dropped, opened where it dropped (3.24.0) — a chest's lid, a fight's spoils, the gear
 * sheet's loot: its card, what wearing it would change against what is worn, and «Надеть» right there.
 * The drop is the server's roll and reaches the stash with the journal's answer, so opening the sheet sends the
 * journal at once, and the button waits until the server holds the piece; one sold for want of room says so.
 * [extra] is what the caller adds under the button.
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
    val stand = lootStand(game, item, expedition.pending)
    LaunchedEffect(item.id, stand) { if (stand == LootStand.ARRIVING) vm.flushRun() }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemCard(item, enabled = false, detailed = true, price = game.sellPrice(item.item))
            WearPreview(game, item.item)
            when {
                stand == LootStand.WORN -> MutedText(ui("expedition.loot_worn"))

                stand == LootStand.GONE -> MutedText(ui("expedition.loot_gone"))

                // A map or a jewel is not worn (3.73.0): no «Надеть» under it.
                item.slot.isJewelLike -> Unit

                else -> ForgeButton(
                    enabled = stand == LootStand.LOOSE && !game.busy && game.unmetFor(item.code).isEmpty(),
                    onClick = {
                        onDismiss()
                        vm.equip(item.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(ui(if (stand == LootStand.ARRIVING) "expedition.loot_arriving" else "hero.equip"))
                }
            }
            // Sold to the merchant right here (3.81.0), wherever the piece is opened on a run: a gilt ribbon with the coin and
            // the price in a chip, held as before. A locked piece (3.30.0) is not sold: the ribbon stays, dimmed, with the reason.
            if (stand == LootStand.LOOSE) {
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

/** A dropped piece, whole: only what goes on the body offers the comparison with what is worn — a map or a jewel has nothing to compare with. */
@Composable
internal fun LootCard(game: GameUi, item: ItemView, onCompare: ((ItemView) -> Unit)?) {
    val price = game.sellPrice(item.item)
    if (onCompare == null || !wearable(game, item.item)) {
        ItemCard(item, enabled = false, detailed = true, price = price)
    } else {
        ItemCard(item, detailed = true, actionLabel = ui("expedition.loot_compare"), action = true, price = price) { onCompare(item) }
    }
}
