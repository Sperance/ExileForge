package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.BodyPlace
import com.sperance.exileforge.core.contract.bodyPlaces
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.display.baseProperties
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.modifierText
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * What the character wears, one line per place on the body — read down, like the stash.
 *
 * A line is a way in rather than a control of its own: a worn item opens its sheet, where taking it
 * off is one of the actions, and an empty place opens the stash narrowed to what fits it. Each line
 * carries the item's name and the one property it is worn for, so the whole outfit reads without a
 * tap. [onPlace] gets the place and the worn instance, or null.
 */
@Composable fun EquipmentLedger(s: ForgeState, onPlace: (place: BodyPlace, instanceId: String?) -> Unit) {
    val hero = s.play.hero ?: return
    Column(Modifier.fillMaxWidth()) {
        bodyPlaces.forEach { place ->
            val instance = place.wornIn(hero.equipped)
            val document = instance?.let { inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
                ?: buildJsonObject { put("slot", place.fits.first()) }
            PlaceLine(s, place, document, worn = instance != null, blocked = place.blockedBy(hero.equipped),
                reasons = instance?.let { hero.inactive[it.id] }) { onPlace(place, instance?.id) }
            HorizontalDivider(color = PanelRaised)
        }
    }
}

/**
 * One place: the rarity spine, the item's icon, where it is worn, and what it is.
 *
 * An item whose requirements stopped being met keeps its place and stops counting; the line says
 * so in red with the server's first reason, instead of the property it no longer gives.
 */
@Composable private fun PlaceLine(s: ForgeState, place: BodyPlace, document: JsonObject, worn: Boolean, blocked: Boolean,
    reasons: List<String>?, onClick: () -> Unit) {
    val title = slotTitle(place.code, s.lang)
    val color = if (worn) rarityColor(document.text("rarity")) else PanelRaised
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        .clickable(enabled = s.account.signedIn, role = Role.Button, onClickLabel = title, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RaritySpine(color, 4.dp)
        ItemIcon(document, color, Modifier.size(30.dp), tint = Muted.takeIf { !worn })
        Text(title, color = Gold, style = MaterialTheme.typography.labelMedium, maxLines = 2, modifier = Modifier.width(84.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            when {
                worn -> {
                    Text(document.text("name"), color = Parchment, style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (reasons != null) {
                        Text(ui("hero.inactive"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
                        reasons.firstOrNull()?.let { Text(requirementReason(it, s.lang), color = LifeRed, style = MaterialTheme.typography.labelSmall) }
                    } else leadingProperty(document, s)?.let {
                        Text(it, color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                blocked -> Text(ui("hero.off_hand_taken"), color = Muted, style = MaterialTheme.typography.bodyMedium)
                else -> Text(ui("hero.empty_slot"), color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.padding(end = 6.dp).size(18.dp))
    }
}

/** The property an item is worn for: its base with its own local modifiers in it, else its first roll. */
private fun leadingProperty(document: JsonObject, s: ForgeState): String? =
    baseProperties(document, s.world.definitions).firstOrNull()?.line()
        ?: (document["params"] as? JsonArray)?.firstOrNull()?.let { it as? JsonObject }?.let { modifierText(it, s.world.definitions) }

/**
 * What could go into one empty place: the stash, narrowed to the slots that fill it.
 *
 * Whether the character can actually wear a line is the server's verdict, shown as the row shows it
 * everywhere else; the tap sends the command either way and the refusal, if any, is the server's.
 * The ring place goes with it, so a ring picked for the second line lands in the second ring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SlotPicker(s: ForgeState, place: BodyPlace, onDismiss: () -> Unit, onEquip: (String) -> Unit) {
    val hero = s.play.hero ?: return
    val fitting = hero.inventory.filter { !it.equipped && !it.socketed }
        .map { it to inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
        .filter { (_, document) -> document.text("slot") in place.fits }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.8f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("hero.slot_pick", slotTitle(place.code, s.lang))) }
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
