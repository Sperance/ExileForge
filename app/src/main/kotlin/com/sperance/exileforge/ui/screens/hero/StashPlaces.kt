package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 */
@Composable fun StashPlaces(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.hero ?: return
    val rules = s.index?.rules?.stash ?: return
    val capacity = rules.capacity(hero.info.stashSlots)
    val price = rules.price(hero.info.stashSlots)
    val full = hero.items.size >= capacity
    ForgePanel(accent = if (full) MaterialTheme.colorScheme.error else Gold) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ForgeGlyphs.Stash, null, tint = if (full) MaterialTheme.colorScheme.error else Gold, modifier = Modifier.size(18.dp))
            Text(ui("stash.places", hero.items.size, capacity), color = GoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            MutedText(ui("stash.ceiling", rules.maxSlots))
        }
        if (price > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeOutlinedButton(onClick = vm::expandStash, enabled = hero.money >= price && !s.busy) {
                Text(ui("stash.expand", minOf(rules.slotStep, rules.maxSlots - capacity)))
            }
            GoldPrice(price)
        }
        if (full) MutedText(ui("stash.full_hint", rules.overflowSlots))
    }
    if (hero.overflow.isEmpty()) return
    Spacer(Modifier.height(8.dp))
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
                        ForgeTextButton(onClick = { vm.sellOverflow(item.id) }, enabled = !s.busy) { Text(ui("hero.sell_do")) }
                    }
                }) { }
            }
        }
    }
}
