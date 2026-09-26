package com.sperance.exileforge.ui.screens.hero


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.core.model.campaign.MapRule
import com.sperance.exileforge.presentation.state.TAB_EXPEDITION
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/** What the action row opened on top of the sheet, if anything. */
private enum class ItemAction { AUCTION, SELL, WORN }

/**
 * One item of the stash: its card, and what can be done with it.
 *
 * The card scrolls; the actions do not — they sit in a row at the foot of the sheet, so the thing a
 * player came to do is never below the fold. Each one is a single tap: wearing and taking off at
 * once, an orb and the crafting bench in the forge, opened over this item, a listing through a small sheet of its own, and selling to
 * the merchant through the held confirmation, because that one cannot be taken back. Every rule behind them is the
 * server's, and it checks them again; since 2.46.0 the client adds the sheet up itself, so wearing
 * is also off for an item whose requirements it misses, and the card says what it would change.
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
    val price = s.sellPrice(instance)
    val reachable = s.unmetFor(instance.equipmentId).isEmpty()
    var open by remember(instanceId) { mutableStateOf<ItemAction?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ItemCard(document, enabled = false, detailed = true, definitions = s.world.definitions, price = price) }
                item { WearPreview(s, instance) }
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
                    // A map is not worn (2.37.0): it goes into its location's launch window, picked.
                    document.text("slot") == MapRule.SLOT -> Action(ForgeGlyphs.Portal, ui("hero.action_map"), can, GoldBright) {
                        onDismiss(); vm.tab(TAB_EXPEDITION); vm.selectZone(document.text("code").removePrefix("MAP_")); vm.pickMap(instance.id)
                    }
                    else -> Action(ForgeGlyphs.Helm, ui("hero.equip"), can && reachable, GoldBright) { onDismiss(); vm.equip(instance.id, null) }
                }
                // One way into the forge (2.51.0): its orbs and bench are its own tabs.
                Action(ForgeGlyphs.Anvil, ui("nav.forge"), can) { onDismiss(); vm.openForge(instance.id, ForgeSection.ORBS) }
                // A worn item cannot be listed or sold (AU_010, CH_014): the tap says so instead of doing nothing.
                Action(ForgeGlyphs.Scales, ui("hero.action_auction"), can) { open = if (loose) ItemAction.AUCTION else ItemAction.WORN }
                Action(ForgeGlyphs.Coins, ui("hero.action_sell"), can, LifeRed) { open = if (loose) ItemAction.SELL else ItemAction.WORN }
                if (s.adminTools) Action(Icons.Outlined.Edit, ui("hero.action_base"), !s.busy, Rune) { onDismiss(); vm.editInventoryBase(instance.equipmentId) }
            }
        }
    }
    when (open) {
        ItemAction.AUCTION -> ListingSheet(s, name, onDismiss = { open = null }) { orb, price, _ ->
            open = null; onDismiss(); vm.sellEquipment(instance.id, orb, price)
        }
        // Selling is final and takes the rolls with it, so it is asked about by name, with the sum
        // worked out here by the merchant's own rule.
        ItemAction.SELL -> ConfirmSheet(
            title = ui("hero.sell_q"), subtitle = name, danger = true,
            icon = { ItemIcon(document, rarityColor(document.text("rarity")), Modifier.size(44.dp)) },
            ledger = listOf(
                LedgerLine(ui("confirm.give"), name, Tone.SPEND),
                LedgerLine(ui("confirm.gain"), price?.let { ui("merchant.gold_amount", it) } ?: ui("confirm.gold_by_server"), Tone.GAIN),
            ),
            note = ui("hero.sell_confirm"),
            confirm = ui("hero.sell_do"),
            onDismiss = { open = null }) { onDismiss(); vm.sellForGold(instance.id) }
        ItemAction.WORN -> AlertDialog(onDismissRequest = { open = null }, containerColor = Panel,
            title = { Text(ui("hero.worn_title"), color = Gold) },
            text = { Text(ui("hero.worn_note"), color = Parchment) },
            confirmButton = { TextButton(enabled = can, onClick = {
                open = null; if (instance.socketed) vm.unsocketJewel(instance.id) else vm.unequip(instance.id)
            }) { Text(ui("hero.unequip")) } },
            dismissButton = { TextButton(onClick = { open = null }) { Text(ui("common.close")) } })
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
