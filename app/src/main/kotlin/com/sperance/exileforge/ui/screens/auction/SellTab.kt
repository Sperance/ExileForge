package com.sperance.exileforge.ui.screens.auction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** Listing something for sale: the stash and the bag, each thing one tap from its price. */
@Composable internal fun ColumnScope.SellTab(s: ForgeState, vm: ForgeViewModel) {
    SellList(s, onEquipment = vm::sellEquipment, onItem = vm::sellItem)
}

/**
 * What the character can put up, read down: the stash as the showcase draws its lots, then the bag.
 *
 * A tap opens the listing sheet — the one an item's own card opens — with the price and, for a
 * stack, how many. Only what can actually go on the showcase is listed: an item in a slot or a
 * socket is refused by the server (`AU_010`), so it is left out rather than offered to fail. That
 * is a reading of `equippedSlot` and `socketCode`, which the server itself wrote.
 */
@Composable internal fun ColumnScope.SellList(s: ForgeState, onEquipment: (String, String, Long) -> Unit,
    onItem: (String, Long, String, Long) -> Unit) {
    val hero = s.play.hero
    var pickedItem by remember { mutableStateOf<String?>(null) }
    var pickedStack by remember { mutableStateOf<String?>(null) }
    if (hero == null) { InfoCard(ui("tree.no_hero"), ui("sell.hero_first")); return }
    val stash = hero.inventory.filterNot { it.equipped || it.socketed }
    val names = stash.associate { it.id to inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item { Engraved(ui("sell.stash")) }
        if (stash.isEmpty()) item { Text(ui("sell.no_equipment"), color = Muted, style = MaterialTheme.typography.bodySmall) }
        items(stash, key = { it.id }) { instance ->
            ItemRow(names.getValue(instance.id), s.world.definitions, enabled = !s.busy) { pickedItem = instance.id }
        }
        item { Spacer(Modifier.height(4.dp)); Engraved(ui("sell.bag")) }
        if (hero.bag.isEmpty()) item { Text(ui("hero.bag_empty"), color = Muted, style = MaterialTheme.typography.bodySmall) }
        items(hero.bag, key = { "bag-" + it.itemId }) { stack ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(enabled = !s.busy, role = Role.Button) { pickedStack = stack.itemId },
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                RaritySpine(Gold.copy(alpha = .35f), 3.dp)
                Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(24.dp))
                Text(stackTitle(s, stack.itemId), color = Parchment, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                Text(stack.amount.toString(), color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp))
            }
            HorizontalDivider(color = PanelRaised)
        }
        item { Text(ui("sell.note"), color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
    pickedItem?.let { id ->
        ListingSheet(s, names[id]?.text("name").orEmpty(), onDismiss = { pickedItem = null }) { orb, price, _ ->
            pickedItem = null; onEquipment(id, orb, price)
        }
    }
    pickedStack?.let { id ->
        ListingSheet(s, stackTitle(s, id), owned = s.bagAmount(id) ?: 0L, onDismiss = { pickedStack = null }) { orb, price, amount ->
            pickedStack = null; onItem(id, amount, orb, price)
        }
    }
}

/** A bag stack's name: the orb's own, or the tail of an id the catalogue does not name. */
private fun stackTitle(s: ForgeState, itemId: String): String =
    s.world.orbs.firstOrNull { it.id == itemId }?.title(s.lang) ?: (ui("common.item") + " …${itemId.takeLast(6)}")
