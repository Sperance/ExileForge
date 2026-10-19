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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.toolPlaces
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
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
 * Снаряжение «Полными карточками» (выбор владельца): места тела под заголовками - оружие, броня, украшения, прочее, фляги - и
 * инструменты ремёсел со счётом надетых. Надетая вещь - карточка во всю ширину: имя с номером экземпляра, метка роллов и все
 * строки вещи целиком, с переносом; пустое место - строка с кнопкой выбора из тайника. Своя нарезка состояния: движение
 * кошелька не перерисовывает надетое.
 */
@Composable fun EquipmentLedger(equipment: EquipmentState, onPlace: (place: BodyPlace, itemId: String?) -> Unit) {
    val byCode = equipment.places.associateBy { it.place.code }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GEAR_GROUPS.forEach { group ->
            val lines = group.codes.mapNotNull { byCode[it] }
            if (lines.isEmpty()) return@forEach
            GroupHeader(group, lines)
            lines.forEach { line ->
                val open = { onPlace(line.place, line.wornId) }
                if (line.worn != null) {
                    WornCard(line, line.worn, equipment.lang, equipment.signedIn, group.compact, open)
                } else {
                    EmptyPlace(line, equipment.lang, equipment.signedIn, open)
                }
            }
        }
    }
}

/** Группа мест по порядку тела: ключ заголовка, коды мест, считать ли надетое в заголовке и плотнее ли карточки. */
private class GearGroup(val key: String, val codes: List<String>, val counted: Boolean = false, val compact: Boolean = false)

/** Группы мест по порядку тела, инструменты - последними. */
private val GEAR_GROUPS: List<GearGroup> = listOf(
    GearGroup("weapons", listOf(BodyPlace.MAIN_HAND, BodyPlace.OFF_HAND)),
    GearGroup("armour", listOf(Slot.HELMET, Slot.BODY, Slot.GLOVES, Slot.BOOTS).map { it.name }),
    GearGroup("jewellery", listOf(Slot.AMULET, Slot.RING, Slot.RING_2, Slot.BELT).map { it.name }),
    GearGroup("other", listOf(Slot.WINGS.name)),
    GearGroup("flasks", Slot.FLASKS.map { it.name }),
    GearGroup("tools", toolPlaces.map { it.code }, counted = true, compact = true),
)

/** Заголовок группы серифами с чертой до края; у счётной группы - «5 из 7» надетого. */
@Composable
private fun GroupHeader(group: GearGroup, lines: List<PlaceState>) {
    Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("gear.group.${group.key}"), color = Color(0xFFB4C2CB), style = relicName(13).copy(letterSpacing = .8.sp))
        if (group.counted) Text(ui("gear.group_count", lines.count { it.worn != null }, lines.size), color = Muted, fontSize = 13.sp)
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = .07f)))
    }
}

/**
 * Надетая вещь карточкой во всю ширину: гнездо с иконкой (у колец и фляг - цифра места), имя в цвете редкости с «№ 7» в
 * конце, знак бездействия и метка роллов справа, ниже все строки вещи 12sp со значком тира - с переносом, без обрезки.
 * Место не подписано: его называют группа и иконка. Почему вещь не работает - в подсказке по касанию знака.
 */
@Composable
private fun WornCard(line: PlaceState, worn: ItemView, lang: Lang, signedIn: Boolean, compact: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val look = relicLook(worn.rarity)
    val ground = if (look.legend != null) {
        Modifier.background(Brush.verticalGradient(listOf(look.top, look.bottom)), shape).border(1.dp, look.gold.copy(alpha = .55f), shape)
    } else {
        Modifier.depthPanel(shape, elevation = 6.dp).border(1.dp, look.rarity.copy(alpha = .2f), shape)
    }
    Column(
        Modifier.fillMaxWidth().clip(shape).then(ground)
            .clickable(enabled = signedIn, role = Role.Button, onClickLabel = slotTitle(line.place.code, lang), onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val socket = if (look.legend == RelicLook.Legend.STARS) CircleShape else RoundedCornerShape(10.dp)
            Box {
                Box(
                    Modifier.size(if (compact) 32.dp else 36.dp).background(Brush.radialGradient(listOf(Color(0xFF222B30), Color(0xFF0B1013))), socket)
                        .border(1.dp, look.rarity.copy(alpha = .55f), socket),
                    contentAlignment = Alignment.Center,
                ) { ItemIcon(worn, look.rarity, Modifier.size(if (compact) 18.dp else 20.dp)) }
                placeNumber(line.place)?.let { PlaceNumber(it, Modifier.align(Alignment.BottomEnd).offset(5.dp, 5.dp)) }
            }
            Text(nameWithSerial(worn), color = look.name, style = itemName(15), modifier = Modifier.weight(1f))
            line.reasons?.let { reasons ->
                Tipped({ Tip(ui("hero.inactive"), reasons.joinToString("\n") { requirementReason(it, lang) }, LifeRed) }, Modifier.size(28.dp), Alignment.Center) {
                    Icon(Icons.Outlined.Block, ui("hero.inactive"), tint = LifeRed, modifier = Modifier.size(16.dp))
                }
            }
            worn.summary.quality?.let { RollPill(it) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            worn.orderedLines.forEach { mod ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.padding(top = .5.dp)) { TierHex(mod.marks, 16.dp) }
                    Text(mod.text, color = modColor(mod.marks), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

/** Имя вещи с номером экземпляра в конце («… № 7»), номер - золотом мельче и неразрывно. */
private fun nameWithSerial(worn: ItemView): AnnotatedString = buildAnnotatedString {
    append(worn.title)
    if (worn.item.serial > 0) {
        append(' ')
        withStyle(SpanStyle(color = Color(0xFFF0C76A), fontSize = 12.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold)) {
            append("№\u00A0${worn.item.serial}")
        }
    }
}

/** Цифра места у парных мест - первое и второе кольцо, первая и вторая фляга; у прочих - null. */
private fun placeNumber(place: BodyPlace): Int? = when (place.place) {
    Slot.RING, Slot.FLASK -> 1
    Slot.RING_2, Slot.FLASK_2 -> 2
    else -> null
}

/** Цифра места кружком в углу гнезда. */
@Composable
private fun PlaceNumber(number: Int, modifier: Modifier) {
    Box(
        modifier.defaultMinSize(16.dp, 16.dp).background(Color(0xFF16212A), CircleShape).border(1.dp, Color(0xFF3A4852), CircleShape).padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center,
    ) { Text(number.toString(), color = Muted, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold) }
}

/**
 * Пустое место строкой в пунктире: гнездо с силуэтом слота, имя места и справа - кнопка «Выбрать из N», когда в тайнике
 * есть подходящее, иначе «Нечего надеть»; вторая рука под двуручным оружием - сплошная рамка и «Занята двуручным оружием».
 */
@Composable
private fun EmptyPlace(line: PlaceState, lang: Lang, signedIn: Boolean, onPick: () -> Unit) {
    val place = line.place
    val shape = RoundedCornerShape(14.dp)
    val frame = if (line.blocked) Modifier.border(1.dp, Color(0xFF26323B), shape) else Modifier.dashedBorder(Color(0xFF2B3944), shape)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).then(frame).padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(32.dp).background(Color(0xFF0B1013), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF3A4852).copy(alpha = .55f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { SlotIcon(place.fits.first(), PanelRaised, Modifier.size(16.dp), tint = Muted.copy(alpha = .5f)) }
        Text(slotTitle(place.code, lang), color = Parchment, fontSize = 14.sp, lineHeight = 17.5.sp, modifier = Modifier.weight(1f))
        when {
            line.blocked -> EmptyNote(ui("hero.off_hand_taken"))
            line.spare > 0 -> PickButton(line.spare, signedIn, onPick)
            else -> EmptyNote(ui("gear.nothing_to_wear"))
        }
    }
}

/** «Выбрать из N»: открывает тайник, суженный до того, что подходит месту. */
@Composable
private fun PickButton(spare: Int, signedIn: Boolean, onPick: () -> Unit) {
    ForgeButton(onClick = onPick, kind = ButtonKind.Secondary, size = ButtonSize.Small, state = if (signedIn) ButtonState.Enabled else ButtonState.Disabled) {
        Text(ui("gear.pick_from", spare), maxLines = 1)
    }
}

/** Что сказать о пустом месте без кнопки: мелко, приглушённо, справа. */
@Composable
private fun EmptyNote(text: String) {
    Text(text, color = Muted, fontSize = 12.sp, lineHeight = 15.6.sp, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 130.dp))
}

/**
 * What could go into one empty place: the stash, narrowed to the slots that fill it.
 *
 * Whether the hero can wear a line is read off the sheet added up here: a line out of
 * reach says what it needs and does not send the command.
 * The ring place goes with it, so a ring picked for the second line lands in the second ring.
 *
 * Дороже - выше по цене торговца, что не надеть - серым внизу. Вердикт - на это самое место (второе кольцо - со вторым кольцом). Вторую
 * копию уникалки не надеть.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotPicker(game: GameUi, place: BodyPlace, onDismiss: () -> Unit, onEquip: (String) -> Unit) {
    val hero = game.hero ?: return
    val inspect = rememberInspect()
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
                    unwearable = unmet,
                    price = game.sellPrice(piece.item),
                    verdict = rememberGearVerdict(game, piece.item, place.place),
                ) {
                    val twin = game.index?.let { hero.uniqueWorn(it, piece.item, place.place) } == true
                    val equip = InspectAction(
                        ui("hero.equip"),
                        enabled = !game.busy && (game.ownsCharacter || game.isAdmin) && unmet.isEmpty() && !twin,
                        reason = ui("hero.unique_worn").takeIf { twin },
                    ) {
                        onDismiss()
                        onEquip(piece.id)
                    }
                    inspect(Inspect.Copy(piece.item, equip))
                }
            }
        }
    }
}
