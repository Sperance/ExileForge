package com.sperance.exileforge.ui.screens.auction

import com.sperance.exileforge.core.display.Glyph

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.auction.AuctionLotKind
import com.sperance.exileforge.core.model.auction.lotKindTitle
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.Muted

/**
 * Listing something for sale.
 *
 * Only what can actually go on the showcase is offered: an equipped item lives in a slot and the
 * server refuses it (`AU_010`), so the picker shows the stash rather than the whole inventory.
 * That is a reading of `equippedSlot`, which the server itself wrote — not a rule kept here.
 *
 * The price is always counted in orbs, because that is the only currency the server prices in.
 */
@Composable internal fun ColumnScope.SellTab(s: ForgeState, vm: ForgeViewModel) {
    val hero = s.play.hero
    var kind by remember(s.play.characterId) { mutableStateOf(AuctionLotKind.EQUIPMENT) }
    var goods by remember(s.play.characterId, kind) { mutableStateOf("") }
    var amount by remember(s.play.characterId, kind) { mutableStateOf("1") }
    var orb by remember(s.play.characterId) { mutableStateOf(s.world.orbs.firstOrNull()?.id.orEmpty()) }
    var price by remember(s.play.characterId) { mutableStateOf("1") }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(2.dp))
        if (hero == null) {
            InfoCard(ui("tree.no_hero"),
                ui("sell.hero_first"))
            return@Column
        }
        // The stash only: an item in a slot has to come off before it can be listed.
        val stash = hero.inventory.filterNot { it.equipped }
        val equipment = stash.associate { instance ->
            val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
            instance.id to "${document.text("name")} · ${rarityTitle(instance.rarity, s.lang)}"
        }
        val bag = hero.bag.associate { item ->
            item.itemId to ((s.world.orbs.firstOrNull { it.id == item.itemId }?.title(s.lang)
                ?: (ui("common.item") + " …${item.itemId.takeLast(6)}")) + " · ${item.amount}")
        }
        val owned = hero.bag.firstOrNull { it.itemId == goods }?.amount ?: 0L
        ForgePanel {
            Engraved(ui("sell.what"))
            Spinner(ui("sell.lot_kind"), kind.name,
                AuctionLotKind.entries.associate { it.name to lotKindTitle(it, s.lang) }, !s.busy, glyph = Glyph.ITEM) { kind = AuctionLotKind.valueOf(it) }
            if (kind == AuctionLotKind.EQUIPMENT) {
                // A jewel is worn too, but it comes out of a socket on the tree, not off a slot.
                if (equipment.isEmpty()) Text(ui("sell.no_equipment"), color = Muted)
                else Spinner(ui("common.item"), goods, equipment, !s.busy, glyph = Glyph.ITEM) { goods = it }
            } else {
                if (bag.isEmpty()) Text(ui("hero.bag_empty"), color = Muted)
                else Spinner(ui("sell.bag_item"), goods, bag, !s.busy, glyph = Glyph.CURRENCY) { goods = it }
                OutlinedTextField(amount, { amount = it }, label = { Text(ui("sell.amount_owned", owned)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        ForgePanel {
            Engraved(ui("card.price"))
            Spinner(ui("orb.orb"), orb, s.world.orbs.associate { it.id to it.title(s.lang) }, !s.busy, glyph = Glyph.CURRENCY) { orb = it }
            OutlinedTextField(price, { price = it }, label = { Text(ui("sell.price")) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(ui("sell.price_note"),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        val count = amount.toLongOrNull() ?: 0L
        val cost = price.toLongOrNull() ?: 0L
        val ready = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin) && goods.isNotBlank() && orb.isNotBlank() &&
            cost > 0 && (kind == AuctionLotKind.EQUIPMENT || count > 0)
        Button(enabled = ready, modifier = Modifier.fillMaxWidth(), onClick = {
            if (kind == AuctionLotKind.EQUIPMENT) vm.sellEquipment(goods, orb, cost) else vm.sellItem(goods, count, orb, cost)
        }) { Text(ui("sell.list")) }
        Text(ui("sell.note"),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
    }
}
