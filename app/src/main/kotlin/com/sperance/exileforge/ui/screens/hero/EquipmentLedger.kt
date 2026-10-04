package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.icons.SlotIcon
import com.sperance.exileforge.ui.theme.*

/**
 * What the hero wears, one line per place on the body — read down, like the stash.
 *
 * A line is a way in rather than a control of its own: a worn item opens its sheet, where taking it
 * off is one of the actions, and an empty place opens the stash narrowed to what fits it. Each line
 * carries the item's name and the one property it is worn for, so the whole outfit reads without a
 * tap. [onPlace] gets the place and the worn item's id, or null.
 */
@Composable fun EquipmentLedger(game: GameUi, onPlace: (place: BodyPlace, itemId: String?) -> Unit) {
    rememberEquipment(game)?.let { EquipmentLedger(it, onPlace) }
}

/** The ledger over its own cut of the state (3.66.0): the purse moving does not redraw what is worn. */
@Composable fun EquipmentLedger(equipment: EquipmentState, onPlace: (place: BodyPlace, itemId: String?) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        equipment.places.forEach { line ->
            PlaceLine(line, equipment.lang, equipment.signedIn) { onPlace(line.place, line.wornId) }
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
 * so in red with the rules' first reason.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceLine(line: PlaceState, lang: Lang, signedIn: Boolean, onClick: () -> Unit) {
    val place = line.place
    val worn = line.worn
    val reasons = line.reasons
    val title = slotTitle(place.code, lang)
    val click = Modifier.fillMaxWidth().clickable(enabled = signedIn, role = Role.Button, onClickLabel = title, onClick = onClick)
    if (worn == null) {
        Row(click.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val frame = RoundedCornerShape(4.dp)
            Box(Modifier.size(22.dp).border(1.dp, PanelRaised, frame), contentAlignment = Alignment.Center) {
                SlotIcon(place.fits.first(), PanelRaised, Modifier.size(14.dp), tint = Muted.copy(alpha = .5f))
            }
            Text(title, color = Gold, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.width(96.dp))
            Text(
                ui(if (line.blocked) "hero.off_hand_taken" else "hero.empty_slot"),
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (line.spare > 0) {
                Text(
                    ui("hero.place_spare", line.spare),
                    color = GoldBright,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.background(Abyss, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        return
    }
    val color = rarityColor(worn.rarity.name)
    val frame = RoundedCornerShape(5.dp)
    val base = worn.base.flatMap { it.values }
    val rolled = worn.lines
    Row(click.padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).background(color.copy(alpha = .08f), frame).border(1.5.dp, color, frame), contentAlignment = Alignment.Center) {
            ItemIcon(worn, color, Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    worn.title,
                    color = color,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // An idle piece is a mark on the line (2.74.0); why is in its card, or behind the mark.
                if (reasons != null) {
                    Tipped({
                        Tip(ui("hero.inactive"), reasons.joinToString("\n") { requirementReason(it, lang) }, LifeRed)
                    }) { Icon(Icons.Outlined.Block, ui("hero.inactive"), tint = LifeRed, modifier = Modifier.size(14.dp)) }
                }
                Text(title, color = Gold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            if (base.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    base.forEach { BaseChip(it) }
                }
            }
            if (rolled.isNotEmpty()) TradeTable(rolled)
        }
    }
}

/**
 * What could go into one empty place: the stash, narrowed to the slots that fill it.
 *
 * Whether the hero can wear a line is read off the sheet added up here (2.46.0): a line out of
 * reach says what it needs and does not send the command.
 * The ring place goes with it, so a ring picked for the second line lands in the second ring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotPicker(game: GameUi, place: BodyPlace, onDismiss: () -> Unit, onEquip: (String) -> Unit) {
    val hero = game.hero ?: return
    // The dearest first by the merchant's price (3.81.0), what the hero cannot wear yet greyed at the foot.
    val fitting = hero.stash.filter { !it.socketed }.mapNotNull { game.view(it) }.filter { place.takes(it.slot) }
        .sortedWith(compareBy({ game.unmetFor(it.code).isNotEmpty() }, { -(game.sellPrice(it.item) ?: 0L) }))
    ForgeSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.8f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Engraved(ui("hero.slot_pick", slotTitle(place.code, game.lang))) }
            if (fitting.isEmpty()) item { InfoCard(ui("hero.slot_pick_empty"), ui("hero.slot_pick_hint")) }
            items(fitting, key = { it.id }) { piece ->
                val unmet = game.unmetFor(piece.code)
                ItemRow(
                    piece,
                    enabled = !game.busy && (game.ownsCharacter || game.isAdmin) && unmet.isEmpty(),
                    unwearable = unmet,
                    price = game.sellPrice(piece.item),
                ) {
                    onDismiss()
                    onEquip(piece.id)
                }
            }
        }
    }
}
