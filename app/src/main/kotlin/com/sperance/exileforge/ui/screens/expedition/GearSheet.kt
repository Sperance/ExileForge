package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.LootEntry
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.screens.hero.SlotPicker
import com.sperance.exileforge.ui.screens.hero.WearPreview
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * «Новый лут» (2.45.0): the gear this run brought, maps aside, while it is still loose — a hero read
 * after a piece landed says whether it was worn or sold since; before that it is taken as loose. Each
 * piece is its view over the content (3.0.0): the hero's copy of it when the hero holds it, the roll's otherwise.
 */
fun newLoot(game: GameUi, runLoot: List<LootEntry>): List<ItemView> = runLoot.mapNotNull { entry ->
    val held = game.hero?.item(entry.item.id)
    val loose = if (held != null) !held.equipped && !held.socketed else game.holding.seenAt < entry.at
    game.view(held ?: entry.item)?.takeIf { loose && it.slot != Slot.MAP }
}

/**
 * The gear on the map (since 2.40.0): the body's ledger as the Equipment section draws it, over the
 * walking map. An empty place opens the stash narrowed to what fits it; a worn one shows its card
 * with «Снять» and «Заменить». The server decides, the hero is re-read, and the run takes the new
 * sheet before its next fight — life and mana keep their share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearSheet(game: GameUi, vm: ExpeditionViewModel, onDismiss: () -> Unit) {
    var place by remember { mutableStateOf<BodyPlace?>(null) }
    var worn by remember { mutableStateOf<String?>(null) }
    var lootTab by remember { mutableStateOf(false) }
    var looked by remember { mutableStateOf<String?>(null) }
    val expedition by vm.state.collectAsStateWithLifecycle()
    val loot = newLoot(game, expedition.runLoot)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!lootTab) {
                    item { Engraved(ui("expedition.gear")) }
                    item { MutedText(ui("expedition.gear_hint")) }
                    item {
                        EquipmentLedger(game) { p, w ->
                            place = p
                            worn = w
                        }
                    }
                } else {
                    item { Engraved(ui("expedition.loot_tab")) }
                    if (loot.isEmpty()) item { MutedText(ui("expedition.loot_empty")) }
                    items(loot, key = { it.id }) { item ->
                        ItemRow(item, enabled = !game.busy, unwearable = game.unmetFor(item.code), price = game.sellPrice(item.item)) { looked = item.id }
                    }
                }
            }
            // The tabs sit at the foot (2.45.0): the body's ledger, and what this run brought.
            TabRow(selectedTabIndex = if (lootTab) 1 else 0, containerColor = Abyss, contentColor = Gold) {
                Tab(selected = !lootTab, onClick = { lootTab = false }, text = { Text(ui("expedition.gear")) })
                Tab(selected = lootTab, onClick = { lootTab = true }, text = { Text(ui("expedition.loot_tab_count", loot.size)) })
            }
        }
    }
    loot.firstOrNull { it.id == looked }?.let { item ->
        LootSheet(game, vm, item, onDismiss = { looked = null }) {
            // A gilt ribbon with the coin and the price in a chip (2.73.0), held as before.
            val price = game.sellPrice(item.item)
            // A locked piece (3.30.0) is not sold: the ribbon stays, dimmed, with the reason under it.
            val locked = game.hero?.item(item.id)?.locked == true
            HoldButton(
                ui("expedition.loot_sell"),
                Gold,
                Modifier.fillMaxWidth(),
                enabled = !game.busy && !locked,
                icon = ForgeGlyphs.Coins,
                figure = price?.let { "+$it" },
            ) {
                looked = null
                vm.sellForGold(item.id)
            }
            if (locked) MutedText(ui("item.locked_hint"))
        }
    }
    val chosen = place
    val instance = worn?.let { id -> game.hero?.item(id) }
    if (chosen != null && instance != null) {
        ForgeSheet(onDismissRequest = {
            place = null
            worn = null
        }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                game.view(instance)?.let { ItemCard(it, enabled = false, detailed = true, price = game.sellPrice(instance)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForgeOutlinedButton(enabled = !game.busy, onClick = {
                        vm.unequip(instance.id)
                        place = null
                        worn = null
                    }, modifier = Modifier.weight(1f)) {
                        Text(ui("hero.unequip"))
                    }
                    ForgeButton(enabled = !game.busy, onClick = { worn = null }, modifier = Modifier.weight(1f)) { Text(ui("expedition.gear_replace")) }
                }
            }
        }
    } else if (chosen != null && worn == null) {
        SlotPicker(game, chosen, onDismiss = { place = null }, onEquip = { id -> vm.equip(id, chosen.place) })
    }
}
