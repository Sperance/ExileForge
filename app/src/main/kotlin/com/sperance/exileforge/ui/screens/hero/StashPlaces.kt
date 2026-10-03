package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/**
 * The stash's places (3.1.0, server 1.1.0): how many items it holds of how many, the next pack of places
 * for gold up to the rules' ceiling, and the overflow — what came when the stash was full, waiting to be
 * taken in or sold. Past the overflow the merchant buys a drop himself; the rules say how far each goes.
 * Only items count against the places: orbs and the rest of the bag stack without a limit.
 *
 * The fill is a thin bar under the shelf's switch (3.69.0), across the width, in the error colour once full; its «+»
 * asks for the next pack with its price. The overflow stays in the list, since it is items to act on.
 */
@Composable fun StashFill(s: ForgeState, vm: HeroViewModel, modifier: Modifier = Modifier) {
    val hero = s.hero ?: return
    val rules = s.index?.rules?.stash ?: return
    val capacity = rules.capacity(hero.info.stashSlots)
    val held = hero.items.size
    val full = held >= capacity
    val tint = if (full) MaterialTheme.colorScheme.error else Gold
    val price = rules.price(hero.info.stashSlots)
    val label = ui("stash.places", held, capacity)
    var buying by remember { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = label },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.weight(1f).height(4.dp).background(PanelRaised, RoundedCornerShape(2.dp))) {
                Box(
                    Modifier.fillMaxWidth(if (capacity > 0) (held.toFloat() / capacity).coerceIn(0f, 1f) else 1f).fillMaxHeight()
                        .background(tint, RoundedCornerShape(2.dp)),
                )
            }
            Text(
                ui("stash.places_chip", held, capacity),
                color = if (full) tint else GoldBright,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (price > 0) {
            IconButton(onClick = { buying = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Add, ui("stash.expand", minOf(rules.slotStep, rules.maxSlots - capacity)), tint = tint, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (buying) {
        val short = hero.money < price
        ConfirmSheet(
            title = ui("stash.expand", minOf(rules.slotStep, rules.maxSlots - capacity)),
            confirm = ui("stash.buy"),
            subtitle = "$label · ${ui("stash.ceiling", rules.maxSlots)}",
            onDismiss = { buying = false },
            ledger = listOf(LedgerLine(ui("confirm.spend"), ui("merchant.gold_amount", price), Tone.SPEND)),
            note = listOfNotNull(ui("stash.places_note"), ui("stash.full_hint", rules.overflowSlots).takeIf { full }).joinToString("\n"),
            blocked = short || s.busy,
            warning = ui("stash.no_gold").takeIf { short },
        ) {
            buying = false
            vm.expandStash()
        }
    }
}

/** What came when the stash was full: each piece taken in once a place frees, or sold. */
@Composable fun StashOverflow(s: ForgeState, vm: HeroViewModel) {
    val hero = s.hero ?: return
    val rules = s.index?.rules?.stash ?: return
    if (hero.overflow.isEmpty()) return
    val full = hero.items.size >= rules.capacity(hero.info.stashSlots)
    ForgePanel(accent = Ember) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Engraved(ui("stash.overflow", hero.overflow.size, rules.overflowSlots), accent = Ember)
            Spacer(Modifier.weight(1f))
            ForgeTextButton(onClick = { vm.claimOverflow() }, enabled = !full && !s.busy) { Text(ui("stash.claim_all")) }
        }
        MutedText(ui("stash.overflow_hint"))
        hero.overflow.forEach { item ->
            s.view(item)?.let { piece ->
                ItemRow(piece, price = s.sellPrice(item), footer = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForgeTextButton(onClick = { vm.claimOverflow(item.id) }, enabled = !full && !s.busy) { Text(ui("stash.claim")) }
                        // A locked piece (3.30.0) waits in the overflow: it is never sold, by hand or by the server.
                        ForgeTextButton(onClick = { vm.sellOverflow(item.id) }, enabled = !s.busy && !item.locked) { Text(ui("hero.sell_do")) }
                    }
                }) { }
            }
        }
    }
}
