package com.sperance.exileforge.ui.screens.craft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.sidesText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.presentation.forge.ForgeTarget
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.ItemSection
import com.sperance.exileforge.presentation.state.ItemType
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.ui.components.AffixBadge
import com.sperance.exileforge.ui.components.BaseChip
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.ItemFilterState
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.components.typeTitle
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.hero.PetCardSheet
import com.sperance.exileforge.ui.screens.hero.PetIcon
import com.sperance.exileforge.ui.screens.hero.PetLines
import com.sperance.exileforge.ui.screens.hero.petName
import com.sperance.exileforge.ui.screens.hero.petSubtitle
import com.sperance.exileforge.ui.theme.*

/** What lies in one round socket of the anvil: its drawing (null - the socket is empty), its name and its colour. */
internal class Socket(val label: String, val accent: Color, val glyph: (@Composable () -> Unit)?, val onClick: (() -> Unit)? = null)

/** Значок раздела на полке у наковальни. */
private val ItemSection.glyph: ImageVector get() = when (this) {
    ItemSection.GEAR -> ForgeGlyphs.Swords
    ItemSection.MAPS -> ForgeGlyphs.Atlas
    ItemSection.TOOLS -> Icons.Outlined.Build
    ItemSection.WORN -> Icons.Outlined.CheckCircle
}

/**
 * Полка у наковальни (макет B; с 4.2.0 - быстрый переключатель «Типа» общего фильтра [ItemShelf.FORGE], как рейка тайника):
 * «Все» и разделы списка рисунками; выбранный - в золоте. Нажатие ставит тип фильтра и открывает выбор цели на вещах - выбор на
 * полке и «Тип» в шторке одно и то же состояние. Под разделами вещей - питомцы ([pets]; нет питомцев - нет кнопки): тот же выбор
 * цели на разделе питомцев (4.4.x); [petOn] - под кузницей питомец.
 */
@Composable internal fun TargetRail(state: ItemFilterState, pets: Boolean, petOn: Boolean, onOpen: (PickerSection) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val entries = listOf<ItemType?>(null) + state.shelf.sections.map(ItemType::Section)
    Column(
        Modifier.width(52.dp).depthPanel(shape).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.forEach { type ->
            val title = type?.let { typeTitle(it, uiLanguage) } ?: ui("common.all")
            RailButton((type as? ItemType.Section)?.section?.glyph ?: Icons.Outlined.Search, title, on = !petOn && type == state.filter.type) {
                state.update(state.filter.copy(type = type))
                onOpen(PickerSection.ITEMS)
            }
        }
        if (pets) {
            HorizontalDivider(Modifier.width(28.dp), color = Bronze.copy(alpha = .5f))
            RailButton(Icons.Outlined.Pets, ui("forge.target_pet"), on = petOn) { onOpen(PickerSection.PETS) }
        }
    }
}

/** Кнопка полки: рисунок [glyph] с подписью [title] для чтеца, выбранная - в золоте. */
@Composable private fun RailButton(glyph: ImageVector, title: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp, 28.dp).clip(RoundedCornerShape(8.dp)).background(if (on) Gold.copy(alpha = .16f) else Color.Transparent)
            .clickable(role = Role.Tab, onClickLabel = title, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(glyph, title, tint = if (on) GoldBright else Muted, modifier = Modifier.size(16.dp))
    }
}

/**
 * The anvil (mockup B): the target's socket, then the [tool] laid on it and, for an orb, its [omen] — then the target itself as it
 * stands and the server's last word about it. С 4.4.x цель - [ForgeTarget]: вещь (база и строки со значками) или питомец (вид,
 * уровень и строки). Касание лежащей цели открывает её карточку, выбор другой - кнопкой карточки; пустая открывает выбор.
 */
@Composable internal fun Anvil(game: GameUi, target: ForgeTarget?, tool: Socket, omen: Socket?, onPick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val inspect = rememberInspect()
    var petCard by remember { mutableStateOf<ForgeTarget.Beast?>(null) }
    val open = {
        when (target) {
            null -> onPick()
            is ForgeTarget.Gear -> inspect(Inspect.Copy(target.view.item, InspectAction(ui(target.changeKey), run = onPick)))
            is ForgeTarget.Beast -> petCard = target
        }
    }
    Column(
        modifier.background(Brush.verticalGradient(listOf(Gold.copy(alpha = .14f).compositeOver(Panel), Panel)), shape)
            .border(1.dp, Bronze, shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Top) {
            TargetSocket(game, target, open)
            Plus()
            RoundSocket(tool)
            omen?.let {
                Plus()
                RoundSocket(it)
            }
        }
        when (target) {
            null -> Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onPick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(ui("forge.pick_item"), color = GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("forge.pick_item_hint"))
            }

            is ForgeTarget.Gear -> AnvilItem(target.view, open)

            is ForgeTarget.Beast -> AnvilPet(game, target.pet, open)
        }
        game.holding.forgeLine.takeIf { it.isNotBlank() }?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Vital, modifier = Modifier.size(14.dp))
                Text(it, color = Vital, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    petCard?.let { beast -> PetCardSheet(game, beast.pet, InspectAction(ui(beast.changeKey), run = onPick)) { petCard = null } }
}

/** Питомец, как его держит наковальня (4.4.x): вид в цвете редкости, кто он и уровень, его строки. */
@Composable private fun AnvilPet(game: GameUi, pet: Pet, onClick: () -> Unit) {
    val index = game.index ?: return
    val menagerie = remember(index) { Menagerie(index) }
    val kind = menagerie.species(pet.species) ?: return
    Column(Modifier.clickable(role = Role.Button, onClick = onClick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(petName(pet.species), color = rarityColor(pet.rarity.name), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        MutedText(listOf(petSubtitle(kind, pet), ui("pets.level", pet.level)).joinToString(" · "), style = MaterialTheme.typography.labelSmall)
        PetLines(index, menagerie, pet)
        if (pet.corrupted) Text(ui("pets.corrupted"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
    }
}

/** The item as the anvil holds it: the name in its rarity's colour, what it is and its affix sides (4.2.0), its base and its lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnvilItem(item: ItemView, onClick: () -> Unit) {
    Column(Modifier.clickable(role = Role.Button, onClick = onClick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            item.title,
            color = rarityColor(item.rarity.name),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MutedText(
            listOfNotNull(
                slotTitle(item.slot),
                ui("row.level", item.level),
                item.sides?.let(::sidesText),
                item.baseQuality?.let { ui("card.base_quality", it) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
        )
        val base = item.base.flatMap { it.values }
        if (base.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                base.forEach { BaseChip(it) }
            }
        }
        item.lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AffixBadge(line.marks)
                Text(line.text, color = ModBlue, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (item.corrupted) Text(ui("orb.corrupted"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun Plus() = Text("+", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 18.dp))

/** The target's square socket, framed in its rarity's colour: the item's icon or the pet's sprite. */
@Composable private fun TargetSocket(game: GameUi, target: ForgeTarget?, onClick: () -> Unit) {
    val color = when (target) {
        null -> Bronze
        is ForgeTarget.Gear -> rarityColor(target.view.rarity.name)
        is ForgeTarget.Beast -> rarityColor(target.pet.rarity.name)
    }
    val frame = RoundedCornerShape(14.dp)
    SocketColumn(ui(target?.socketKey ?: "forge.socket_item")) {
        Box(
            Modifier.size(60.dp).clip(frame).background(PanelRaised, frame).border(2.dp, color, frame).clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when (target) {
                null -> Icon(ForgeGlyphs.Plus, null, tint = Muted, modifier = Modifier.size(22.dp))
                is ForgeTarget.Gear -> ItemIcon(target.view, color, Modifier.size(34.dp))
                is ForgeTarget.Beast -> PetIcon(game, target.pet.species, 34)
            }
        }
    }
}

/** A round socket: the orb, essence or bench line laid on the item, or the omen paired with the orb; dim while empty. */
@Composable private fun RoundSocket(socket: Socket) {
    val filled = socket.glyph != null
    SocketColumn(socket.label) {
        Box(
            Modifier.padding(top = 6.dp).size(48.dp).clip(CircleShape)
                .background(if (filled) socket.accent.copy(alpha = .12f).compositeOver(PanelRaised) else PanelRaised, CircleShape)
                .border(2.dp, if (filled) socket.accent else Bronze, CircleShape)
                .let { base -> socket.onClick?.let { click -> base.clickable(role = Role.Button, onClick = click) } ?: base },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { socket.glyph?.invoke() }
        }
    }
}

@Composable private fun SocketColumn(label: String, content: @Composable () -> Unit) {
    Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        content()
        Text(label, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
