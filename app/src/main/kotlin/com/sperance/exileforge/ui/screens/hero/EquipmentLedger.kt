package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.sellPrice
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Slot
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

/**
 * Снаряжение «Группами» (3.88.6, выбор владельца): места тела под заголовками - оружие, броня, украшения, прочее, фляги - и в
 * каждой группе по две карточки в ряд: место, имя, номер экземпляра и все строки вещи со значком тира. Пустое место говорит,
 * сколько подходящего лежит в тайнике. Своя нарезка состояния (3.66.0): движение кошелька не перерисовывает надетое.
 */
@Composable fun EquipmentLedger(equipment: EquipmentState, onPlace: (place: BodyPlace, itemId: String?) -> Unit) {
    val byCode = equipment.places.associateBy { it.place.code }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GEAR_GROUPS.forEach { (key, codes) ->
            val lines = codes.mapNotNull { byCode[it] }
            if (lines.isEmpty()) return@forEach
            Row(Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("gear.group.$key"), color = Color(0xFFB4C2CB), style = relicName(13).copy(letterSpacing = .8.sp))
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = .07f)))
            }
            lines.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { line ->
                        PlaceCard(line, equipment.lang, equipment.signedIn, Modifier.weight(1f).fillMaxHeight()) { onPlace(line.place, line.wornId) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Группы мест по порядку тела: ключ заголовка и коды мест. */
private val GEAR_GROUPS: List<Pair<String, List<String>>> = listOf(
    "weapons" to listOf(BodyPlace.MAIN_HAND, BodyPlace.OFF_HAND),
    "armour" to listOf(Slot.HELMET, Slot.BODY, Slot.GLOVES, Slot.BOOTS).map { it.name },
    "jewellery" to listOf(Slot.AMULET, Slot.RING, Slot.RING_2, Slot.BELT).map { it.name },
    "other" to listOf(Slot.WINGS, Slot.COLLAR).map { it.name },
    "flasks" to Slot.FLASKS.map { it.name },
)

/**
 * Одно место карточкой (3.88.6): гнездо с иконкой, место мелкими буквами, имя серифами в цвете редкости и строки вещи со
 * значком тира, каждая в одну линию. Вещь, что перестала действовать, несёт красный знак с причиной в подсказке;
 * пустое место - пунктир и что о нём сказать.
 */
@Composable
private fun PlaceCard(line: PlaceState, lang: Lang, signedIn: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val place = line.place
    val worn = line.worn
    val title = slotTitle(place.code, lang)
    val shape = RoundedCornerShape(16.dp)
    val look = worn?.let { relicLook(it.rarity) }
    val ground = when {
        look == null -> Modifier.border(1.dp, Color(0xFF26323B), shape)
        look.legend != null -> Modifier.background(Brush.verticalGradient(listOf(look.top, look.bottom)), shape).relicSky(look).border(1.dp, look.gold.copy(alpha = .55f), shape)
        else -> Modifier.depthPanel(shape, elevation = 6.dp).border(1.dp, look.rarity.copy(alpha = .2f), shape)
    }
    Column(
        modifier.clip(shape).then(ground).clickable(enabled = signedIn, role = Role.Button, onClickLabel = title, onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val socket = if (look?.legend == RelicLook.Legend.STARS) CircleShape else RoundedCornerShape(10.dp)
            Box(
                Modifier.size(34.dp).background(Brush.radialGradient(listOf(Color(0xFF222B30), Color(0xFF0B1013))), socket)
                    .border(1.dp, (look?.rarity ?: Color(0xFF3A4852)).copy(alpha = .55f), socket),
                contentAlignment = Alignment.Center,
            ) {
                if (worn != null) {
                    ItemIcon(worn, look!!.rarity, Modifier.size(20.dp))
                } else {
                    SlotIcon(place.fits.first(), PanelRaised, Modifier.size(16.dp), tint = Muted.copy(alpha = .5f))
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    // An idle piece is a mark on the line (2.74.0); why is in its card, or behind the mark.
                    line.reasons?.let { reasons ->
                        Tipped({
                            Tip(ui("hero.inactive"), reasons.joinToString("\n") { requirementReason(it, lang) }, LifeRed)
                        }) { Icon(Icons.Outlined.Block, ui("hero.inactive"), tint = LifeRed, modifier = Modifier.size(12.dp)) }
                    }
                }
                if (worn != null) {
                    Text(worn.title, color = look!!.name, style = relicName(12), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (worn.item.serial > 0) Text("◆ № ${worn.item.serial} ◆", color = Color(0xFFF0C76A), style = relicName(9))
                } else {
                    Text(ui(if (line.blocked) "hero.off_hand_taken" else "hero.empty_slot"), color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                }
            }
        }
        if (worn != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                worn.lines.forEach { mod ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TierHex(mod.marks, 12.dp)
                        Text(mod.text, color = modColor(mod.marks), fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else if (line.spare > 0) {
            Text(ui("hero.place_spare", line.spare), color = GoldBright, style = MaterialTheme.typography.labelSmall)
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
