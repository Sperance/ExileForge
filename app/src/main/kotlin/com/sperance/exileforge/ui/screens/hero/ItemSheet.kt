package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/** What the action row opened on top of the sheet, if anything. */
private enum class ItemAction { ORB, AUCTION, SELL }

/**
 * One item of the stash: its card, and what can be done with it.
 *
 * The card scrolls; the actions do not — they sit in a row at the foot of the sheet, so the thing a
 * player came to do is never below the fold. Each one is a single tap: wearing and taking off at
 * once, an orb and a listing through a small sheet of their own, and selling to the merchant through
 * the held confirmation, because that one cannot be taken back. Every rule behind them is the
 * server's; a control is off only for what the client already knows as a fact — nobody signed in,
 * another command running, an item that is worn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ItemSheet(s: ForgeState, vm: ForgeViewModel, instanceId: String, onDismiss: () -> Unit) {
    val instance = s.play.hero?.inventory?.firstOrNull { it.id == instanceId }
    // The item can leave while its sheet is open — sold, listed, rolled into a copy — and then the
    // sheet has nothing left to be about.
    if (instance == null) { LaunchedEffect(instanceId) { onDismiss() }; return }
    val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
    val name = document.text("name")
    val can = !s.busy && s.account.signedIn && (s.ownsCharacter || s.isAdmin)
    val loose = !instance.equipped && !instance.socketed
    var open by remember(instanceId) { mutableStateOf<ItemAction?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ItemCard(document, enabled = false, detailed = true, definitions = s.world.definitions,
                    actionLabel = ui("hero.instance") + " · ${instance.id.takeLast(6)}") }
                // Worn but not counting: the server's reasons, as the slot cell prints them.
                s.play.hero?.inactive?.get(instance.id)?.let { reasons -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ui("hero.inactive"), color = LifeRed, style = MaterialTheme.typography.labelLarge)
                        reasons.forEach { Text(requirementReason(it, s.lang), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
                    }
                } }
            }
            OrnateDivider()
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
                when {
                    instance.socketed -> Action(ForgeGlyphs.Gem, ui("hero.unequip"), can) { onDismiss(); vm.unsocketJewel(instance.id) }
                    instance.equipped -> Action(ForgeGlyphs.Helm, ui("hero.unequip"), can) { onDismiss(); vm.unequip(instance.id) }
                    else -> Action(ForgeGlyphs.Helm, ui("hero.equip"), can, GoldBright) { onDismiss(); vm.equip(instance.id) }
                }
                Action(ForgeGlyphs.Orb, ui("hero.action_orb"), can) { open = ItemAction.ORB }
                Action(ForgeGlyphs.Scales, ui("hero.action_auction"), can && loose) { open = ItemAction.AUCTION }
                Action(ForgeGlyphs.Anvil, ui("hero.action_sell"), can && loose, LifeRed) { open = ItemAction.SELL }
                if (s.adminTools) Action(Icons.Outlined.Edit, ui("hero.action_base"), !s.busy, Rune) { onDismiss(); vm.editInventoryBase(instance.equipmentId) }
            }
        }
    }
    when (open) {
        ItemAction.ORB -> ModalBottomSheet(onDismissRequest = { open = null }, containerColor = Panel) {
            // The item sheet stays underneath: the card behind it is re-read after every orb, so the
            // result is visible the moment this one is put away.
            Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OrbPanel(s, instance.id, vm::selectOrb, vm::applyOrb)
            }
        }
        ItemAction.AUCTION -> ListingSheet(s, name, onDismiss = { open = null }) { orb, price ->
            open = null; onDismiss(); vm.sellEquipment(instance.id, orb, price)
        }
        // Selling is final and takes the rolls with it, so it is asked about by name. The price is
        // the merchant's: the sheet says gold is coming and leaves the sum to him.
        ItemAction.SELL -> ConfirmSheet(
            title = ui("hero.sell_q"), subtitle = name, danger = true,
            icon = { ItemIcon(document, rarityColor(document.text("rarity")), Modifier.size(44.dp)) },
            ledger = listOf(
                LedgerLine(ui("confirm.give"), name, Tone.SPEND),
                LedgerLine(ui("confirm.gain"), ui("confirm.gold_by_server"), Tone.GAIN),
            ),
            note = ui("hero.sell_confirm"),
            confirm = ui("hero.sell_do"),
            onDismiss = { open = null }) { onDismiss(); vm.sellForGold(instance.id) }
        null -> Unit
    }
}

/** One action of the row: a drawing over a word, the whole of it the tap target. */
@Composable private fun RowScope.Action(icon: ImageVector, label: String, enabled: Boolean, accent: Color = Gold, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
        .padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = if (enabled) accent else Muted.copy(alpha = .45f), modifier = Modifier.size(22.dp))
        Text(label, color = if (enabled) Parchment else Muted.copy(alpha = .6f), style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Listing one item on the auction, from the item itself.
 *
 * The price is counted in orbs alone (`AU_007`), so the orb is picked from the server's catalogue
 * and the amount typed; whether the listing stands is the server's to say.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ListingSheet(s: ForgeState, name: String, onDismiss: () -> Unit, onList: (orb: String, price: Long) -> Unit) {
    var orb by remember { mutableStateOf(s.world.orbs.firstOrNull()?.id.orEmpty()) }
    var price by remember { mutableStateOf("1") }
    val cost = price.toLongOrNull() ?: 0L
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("sell.list"))
            Text(name, color = Parchment, style = MaterialTheme.typography.titleMedium)
            if (s.world.orbs.isEmpty()) Text(ui("orb.none"), color = Muted)
            else Spinner(ui("orb.orb"), orb, s.world.orbs.associate { it.id to it.title(s.lang) }, !s.busy) { orb = it }
            OutlinedTextField(price, { value -> price = value.filter(Char::isDigit) }, label = { Text(ui("sell.price")) },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Text(ui("sell.price_note"), color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(ui("sell.note"), color = Muted, style = MaterialTheme.typography.bodySmall)
            Button(enabled = !s.busy && orb.isNotBlank() && cost > 0, onClick = { onList(orb, cost) }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("sell.list"))
            }
        }
    }
}
