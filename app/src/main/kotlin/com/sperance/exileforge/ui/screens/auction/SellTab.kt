package com.sperance.exileforge.ui.screens.auction

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
import com.sperance.exileforge.core.i18n.tr
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
    val hero = s.hero
    var kind by remember(s.characterId) { mutableStateOf(AuctionLotKind.EQUIPMENT) }
    var goods by remember(s.characterId, kind) { mutableStateOf("") }
    var amount by remember(s.characterId, kind) { mutableStateOf("1") }
    var orb by remember(s.characterId) { mutableStateOf(s.orbs.firstOrNull()?.id.orEmpty()) }
    var price by remember(s.characterId) { mutableStateOf("1") }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(2.dp))
        if (hero == null) {
            InfoCard(tr("Герой не загружен", "The hero is not loaded"),
                tr("Откройте вкладку «Герой» и обновите персонажа, чтобы увидеть, что можно продать.",
                   "Open the Hero tab and refresh the character to see what can be sold."))
            return@Column
        }
        // The stash only: an item in a slot has to come off before it can be listed.
        val stash = hero.inventory.filterNot { it.equipped }
        val equipment = stash.associate { instance ->
            val document = inventoryDocument(instance, s.inventoryBases[instance.equipmentId])
            instance.id to "${document.text("name")} · ${rarityTitle(instance.rarity, s.lang)}"
        }
        val bag = hero.bag.associate { item ->
            item.itemId to ((s.orbs.firstOrNull { it.id == item.itemId }?.title(s.lang)
                ?: (tr("Предмет", "Item") + " …${item.itemId.takeLast(6)}")) + " · ${item.amount}")
        }
        val owned = hero.bag.firstOrNull { it.itemId == goods }?.amount ?: 0L
        ForgePanel {
            Engraved(tr("Что продаём", "What is on sale"))
            Spinner(tr("Вид лота", "Lot kind"), kind.name,
                AuctionLotKind.entries.associate { it.name to lotKindTitle(it, s.lang) }, !s.busy) { kind = AuctionLotKind.valueOf(it) }
            if (kind == AuctionLotKind.EQUIPMENT) {
                // A jewel is worn too, but it comes out of a socket on the tree, not off a slot.
                if (equipment.isEmpty()) Text(tr("Снятой экипировки нет. Надетый предмет сервер выставить не даст — сначала снимите его во вкладке «Герой», а самоцвет выньте из гнезда во вкладке «Дерево».",
                        "No unequipped items. The server refuses to list a worn one — take it off on the Hero tab first, or a jewel out of its socket on the Tree tab."), color = Muted)
                else Spinner(tr("Предмет", "Item"), goods, equipment, !s.busy) { goods = it }
            } else {
                if (bag.isEmpty()) Text(tr("Сумка пуста", "The bag is empty"), color = Muted)
                else Spinner(tr("Предмет из сумки", "Bag item"), goods, bag, !s.busy) { goods = it }
                OutlinedTextField(amount, { amount = it }, label = { Text(tr("Количество, есть $owned", "Amount, $owned owned")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        ForgePanel {
            Engraved(tr("Цена", "Price"))
            Spinner(tr("Сфера", "Orb"), orb, s.orbs.associate { it.id to it.title(s.lang) }, !s.busy) { orb = it }
            OutlinedTextField(price, { price = it }, label = { Text(tr("Сколько сфер за весь лот", "Orbs for the whole lot")) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(tr("Цена назначается только в сферах: это единственная валюта, в которой сервер торгует.",
                    "The price is set in orbs alone: that is the only currency the server trades in."),
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        val count = amount.toLongOrNull() ?: 0L
        val cost = price.toLongOrNull() ?: 0L
        val ready = !s.busy && s.signedIn && (s.ownsCharacter || s.isAdmin) && goods.isNotBlank() && orb.isNotBlank() &&
            cost > 0 && (kind == AuctionLotKind.EQUIPMENT || count > 0)
        Button(enabled = ready, modifier = Modifier.fillMaxWidth(), onClick = {
            if (kind == AuctionLotKind.EQUIPMENT) vm.sellEquipment(goods, orb, cost) else vm.sellItem(goods, count, orb, cost)
        }) { Text(tr("Выставить на аукцион", "List on the auction")) }
        Text(tr("Пока лот на витрине, товар лежит в нём: надеть или продать его второй раз нельзя.",
                "While the lot is on the showcase the goods live inside it: it can be neither worn nor sold twice."),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
    }
}
