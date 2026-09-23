package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.slots
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * What the character wears, one cell per body slot.
 *
 * A cell is a way in rather than a control of its own: a worn item opens its sheet, where taking it
 * off is one of the actions, and an empty slot opens the stash narrowed to what fits it, so putting
 * something on is two taps from here. [onSlot] gets the slot and the worn instance, or null.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun EquipmentGrid(s: ForgeState, onSlot: (slot: String, instanceId: String?) -> Unit) {
    val hero = s.play.hero ?: return
    // A jewel is worn in a socket on the tree, not on the body, so it has no cell here — the tree
    // draws it where it actually sits.
    val bodySlots = slots.filterNot { it == "JEWEL" }
    BoxWithConstraints {
        // The cells share the width evenly, so no gap is left on the right, and a wide screen fits
        // more of them in a row — a fixed width could do neither.
        val perRow = if (maxWidth >= 480.dp) 5 else 4
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = perRow) {
            bodySlots.forEach { slot ->
                val instance = hero.equipped[slot]
                val document = instance?.let { inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) } ?: buildJsonObject { put("slot", slot) }
                val reasons = instance?.let { hero.inactive[it.id] }
                val accent = when {
                    reasons != null -> LifeRed
                    instance != null -> rarityColor(document.text("rarity"))
                    else -> Bronze.copy(alpha = .4f)
                }
                val shape = CutCornerShape(6.dp)
                Column(Modifier.weight(1f)
                    .background(if (instance == null) SolidColor(Panel) else panelBrush(accent), shape)
                    .border(1.dp, if (instance == null) accent else accent.copy(alpha = .6f), shape)
                    .clickable(enabled = s.account.signedIn) { onSlot(slot, instance?.id) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(slotTitle(slot, s.lang), style = MaterialTheme.typography.labelSmall, color = Gold,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ItemIcon(document, GoldBright, Modifier.size(36.dp), tint = Muted.takeIf { instance == null })
                    Text(if (instance == null) ui("hero.empty_slot") else document.text("name"),
                        style = MaterialTheme.typography.labelSmall, color = if (instance == null) Muted else Parchment,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    // An item whose requirements stopped being met keeps its slot and stops counting.
                    reasons?.let {
                        Text(ui("hero.inactive"), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        it.forEach { reason -> Text(requirementReason(reason, s.lang), color = LifeRed, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
                    }
                }
            }
        }
    }
}

/**
 * What could go into one empty slot: the stash, narrowed to that slot.
 *
 * Whether the character can actually wear a line is the server's verdict, shown as the row shows it
 * everywhere else; the tap sends the command either way and the refusal, if any, is the server's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SlotPicker(s: ForgeState, slot: String, onDismiss: () -> Unit, onEquip: (String) -> Unit) {
    val hero = s.play.hero ?: return
    val fitting = hero.inventory.filter { !it.equipped && !it.socketed }
        .map { it to inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
        .filter { (_, document) -> document.text("slot") == slot }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.8f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("hero.slot_pick", slotTitle(slot, s.lang))) }
            if (fitting.isEmpty()) item { InfoCard(ui("hero.slot_pick_empty"), ui("hero.slot_pick_hint")) }
            items(fitting, key = { it.first.id }) { (instance, document) ->
                ItemRow(document, definitions = s.world.definitions, enabled = !s.busy && (s.ownsCharacter || s.isAdmin),
                    unwearable = s.play.hero?.sheet?.unwearableBy?.get(instance.equipmentId).orEmpty()) {
                    onDismiss(); onEquip(instance.id)
                }
            }
        }
    }
}
