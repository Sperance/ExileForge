package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.BodyPlace
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.hero.EquipmentLedger
import com.sperance.exileforge.ui.screens.hero.SlotPicker
import com.sperance.exileforge.ui.theme.*

/**
 * The gear on the map (since 2.40.0): the body's ledger as the Equipment section draws it, over the
 * walking map. An empty place opens the stash narrowed to what fits it; a worn one shows its card
 * with «Снять» and «Заменить». The server decides, the hero is re-read, and the run takes the new
 * sheet before its next fight — life and mana keep their share.
 */
/**
 * «Новый лут» (2.45.0): the gear this run brought, maps aside, while it is still loose — a hero read
 * after a piece landed says whether it was worn or sold since; before that it is taken as loose.
 */
fun newLoot(s: ForgeState): List<com.sperance.exileforge.core.model.hero.EquipmentInstance> {
    val hero = s.play.hero
    return s.play.runLoot.filter { entry ->
        val slot = s.world.inventoryBases[entry.item.equipmentId]?.let { com.sperance.exileforge.core.display.inventoryDocument(entry.item, it) }
            ?.let { (it["slot"] as? kotlinx.serialization.json.JsonPrimitive)?.content }
        val now = hero?.inventory?.firstOrNull { it.id == entry.item.id }
        slot != com.sperance.exileforge.core.model.campaign.MapRule.SLOT && when {
            now != null -> !now.equipped && !now.socketed
            else -> s.play.heroSeenAt < entry.at
        }
    }.map { it.item }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GearSheet(s: ForgeState, vm: ForgeViewModel, onDismiss: () -> Unit) {
    var place by remember { mutableStateOf<BodyPlace?>(null) }
    var worn by remember { mutableStateOf<String?>(null) }
    var lootTab by remember { mutableStateOf(false) }
    var looked by remember { mutableStateOf<String?>(null) }
    val loot = newLoot(s)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!lootTab) {
                    item { Engraved(ui("expedition.gear")) }
                    item { Text(ui("expedition.gear_hint"), color = Muted, style = MaterialTheme.typography.bodySmall) }
                    item { EquipmentLedger(s) { p, w -> place = p; worn = w } }
                } else {
                    item { Engraved(ui("expedition.loot_tab")) }
                    if (loot.isEmpty()) item { Text(ui("expedition.loot_empty"), color = Muted, style = MaterialTheme.typography.bodySmall) }
                    items(loot, key = { it.id }) { item ->
                        ItemRow(inventoryDocument(item, s.world.inventoryBases[item.equipmentId]), definitions = s.world.definitions, enabled = !s.busy,
                            unwearable = s.play.hero?.sheet?.unwearableBy?.get(item.equipmentId).orEmpty()) { looked = item.id }
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
        ModalBottomSheet(onDismissRequest = { looked = null }, containerColor = Panel) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemCard(inventoryDocument(item, s.world.inventoryBases[item.equipmentId]), enabled = false, detailed = true, definitions = s.world.definitions)
                Button(enabled = !s.busy, onClick = { looked = null; vm.equip(item.id) }, modifier = Modifier.fillMaxWidth()) { Text(ui("hero.equip")) }
                HoldButton(ui("expedition.loot_sell"), LifeRed, Modifier.fillMaxWidth(), enabled = !s.busy) { looked = null; vm.sellForGold(item.id) }
            }
        }
    }
    val chosen = place
    val instance = worn?.let { id -> s.play.hero?.inventory?.firstOrNull { it.id == id } }
    if (chosen != null && instance != null) {
        ModalBottomSheet(onDismissRequest = { place = null; worn = null }, containerColor = Panel) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemCard(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), enabled = false, detailed = true, definitions = s.world.definitions)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(enabled = !s.busy, onClick = { vm.unequip(instance.id); place = null; worn = null }, modifier = Modifier.weight(1f)) {
                        Text(ui("hero.unequip"))
                    }
                    Button(enabled = !s.busy, onClick = { worn = null }, modifier = Modifier.weight(1f)) { Text(ui("expedition.gear_replace")) }
                }
            }
        }
    } else if (chosen != null && worn == null) {
        SlotPicker(s, chosen, onDismiss = { place = null }, onEquip = { id -> vm.equip(id, chosen.ring) })
    }
}
