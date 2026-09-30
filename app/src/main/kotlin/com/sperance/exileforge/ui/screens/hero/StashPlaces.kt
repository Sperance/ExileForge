package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/**
 * The stash's places (3.1.0, server 1.1.0): how many items it holds of how many, the next pack of places
 * for gold up to the rules' ceiling, and the overflow — what came when the stash was full, waiting to be
 * taken in or sold. Past the overflow the merchant buys a drop himself; the rules say how far each goes.
 * Only items count against the places: orbs and the rest of the bag stack without a limit.
 *
 * The count is a small button in the header, the pack and the ceiling a dialog behind it; the overflow
 * stays in the list, since it is items to act on.
 */
@Composable fun StashPlacesButton(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.hero ?: return
    val rules = s.index?.rules?.stash ?: return
    val capacity = rules.capacity(hero.info.stashSlots)
    val full = hero.items.size >= capacity
    val tint = if (full) MaterialTheme.colorScheme.error else Gold
    var open by remember { mutableStateOf(false) }
    val label = ui("stash.places", hero.items.size, capacity)
    Surface(onClick = { open = true }, shape = RoundedCornerShape(50), color = tint.copy(alpha = .14f),
        border = BorderStroke(1.dp, tint.copy(alpha = .6f)), modifier = Modifier.semantics { contentDescription = label }) {
        Row(Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(ForgeGlyphs.Stash, null, tint = tint, modifier = Modifier.size(14.dp))
            Text(ui("stash.places_chip", hero.items.size, capacity), color = GoldBright, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Outlined.ChevronRight, null, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
    if (open) ForgeDialog(ui("hero.section_stash"), onDismiss = { open = false }) {
        val price = rules.price(hero.info.stashSlots)
        Text(label, color = if (full) MaterialTheme.colorScheme.error else GoldBright, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall)
        MutedText(ui("stash.ceiling", rules.maxSlots))
        MutedText(ui("stash.places_note"))
        if (price > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeOutlinedButton(onClick = vm::expandStash, enabled = hero.money >= price && !s.busy) {
                Text(ui("stash.expand", minOf(rules.slotStep, rules.maxSlots - capacity)))
            }
            GoldPrice(price)
        }
        if (full) MutedText(ui("stash.full_hint", rules.overflowSlots))
    }
}

/** What came when the stash was full: each piece taken in once a place frees, or sold. */
@Composable fun StashOverflow(s: ForgeState, vm: ForgeViewModel) {
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
