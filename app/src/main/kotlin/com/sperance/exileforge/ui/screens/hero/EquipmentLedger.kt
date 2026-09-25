package com.sperance.exileforge.ui.screens.hero

import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.sellPrice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
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
import com.sperance.exileforge.core.display.shownLines
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
    // How many loose items of each slot lie in the stash (2.48.0): each place says what it could take.
    val loose = hero.inventory.filter { !it.equipped && !it.socketed }.mapNotNull { s.world.inventoryBases[it.equipmentId]?.text("slot") }
        .groupingBy { it }.eachCount()
    Column(Modifier.fillMaxWidth()) {
        bodyPlaces.forEach { place ->
            val instance = place.wornIn(hero.equipped)
            val document = instance?.let { inventoryDocument(it, s.world.inventoryBases[it.equipmentId]) }
                ?: buildJsonObject { put("slot", place.fits.first()) }
            PlaceLine(s, place, document, worn = instance != null, blocked = place.blockedBy(hero.equipped),
                reasons = instance?.let { hero.inactive[it.id] }, spare = place.fits.sumOf { loose[it] ?: 0 }) { onPlace(place, instance?.id) }
            HorizontalDivider(color = PanelRaised)
        }
    }
}

/**
 * One place, «Гроссбух» (2.50.0, the owner's pick of five mockups): a worn item is its icon in a
 * rarity frame, the name and the place on one line, the base as chips and every roll under a rhombus
 * with its tier — the stash's own row, tighter. An empty place is one thin line with what the stash
 * holds for it.
 *
 * An item whose requirements stopped being met keeps its place and stops counting; the line says
 * so in red with the server's first reason.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PlaceLine(s: ForgeState, place: BodyPlace, document: JsonObject, worn: Boolean, blocked: Boolean,
    reasons: List<String>?, spare: Int = 0, onClick: () -> Unit) {
    val title = slotTitle(place.code, s.lang)
    val click = Modifier.fillMaxWidth().clickable(enabled = s.account.signedIn, role = Role.Button, onClickLabel = title, onClick = onClick)
    if (!worn) {
        Row(click.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val frame = RoundedCornerShape(4.dp)
            Box(Modifier.size(22.dp).border(1.dp, PanelRaised, frame), contentAlignment = Alignment.Center) {
                ItemIcon(document, PanelRaised, Modifier.size(14.dp), tint = Muted.copy(alpha = .5f))
            }
            Text(title, color = Gold, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.width(96.dp))
            Text(ui(if (blocked) "hero.off_hand_taken" else "hero.empty_slot"), color = Muted, style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (spare > 0) Text(ui("hero.place_spare", spare), color = GoldBright, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.background(Abyss, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
        return
    }
    val color = rarityColor(document.text("rarity"))
    val frame = RoundedCornerShape(5.dp)
    val base = baseProperties(document, s.world.definitions).flatMap { it.values }
    val rolled = shownLines((document["params"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }, s.world.definitions)
    Row(click.padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).background(color.copy(alpha = .08f), frame).border(1.5.dp, color, frame), contentAlignment = Alignment.Center) {
            ItemIcon(document, color, Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(document.text("name"), color = color, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                // An idle piece is a mark on the line (2.74.0); why is in its card, or behind the mark.
                if (reasons != null) com.sperance.exileforge.ui.components.Tipped({
                    com.sperance.exileforge.ui.components.Tip(ui("hero.inactive"), reasons.joinToString("\n") { requirementReason(it, s.lang) }, LifeRed)
                }) { Icon(Icons.Outlined.Block, ui("hero.inactive"), tint = LifeRed, modifier = Modifier.size(14.dp)) }
                Text(title, color = Gold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            if (base.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                base.forEach { BaseChip(it) }
            }
            if (rolled.isNotEmpty()) TradeTable(rolled, s.world.definitions)
        }
    }
}

/**
 * What could go into one empty place: the stash, narrowed to the slots that fill it.
 *
 * Whether the character can wear a line is read off the sheet added up here (2.46.0): a line out of
 * reach says what it needs and does not send the command.
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
                val unmet = s.unmetFor(instance.equipmentId)
                ItemRow(document, definitions = s.world.definitions, enabled = !s.busy && (s.ownsCharacter || s.isAdmin) && unmet.isEmpty(),
                    unwearable = unmet, price = s.sellPrice(instance)) {
                    onDismiss(); onEquip(instance.id)
                }
            }
        }
    }
}
