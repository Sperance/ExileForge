package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GearSheet(s: ForgeState, vm: ForgeViewModel, onDismiss: () -> Unit) {
    var place by remember { mutableStateOf<BodyPlace?>(null) }
    var worn by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f).navigationBarsPadding(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("expedition.gear")) }
            item { Text(ui("expedition.gear_hint"), color = Muted, style = MaterialTheme.typography.bodySmall) }
            item { EquipmentLedger(s) { p, w -> place = p; worn = w } }
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
