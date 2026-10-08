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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.ItemSection
import com.sperance.exileforge.presentation.state.ItemType
import com.sperance.exileforge.ui.components.AffixBadge
import com.sperance.exileforge.ui.components.BaseChip
import com.sperance.exileforge.ui.components.ItemFilterState
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.typeTitle
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
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
 * «Все» и разделы списка рисунками; выбранный - в золоте. Нажатие ставит тип фильтра и открывает выбор вещи - выбор на полке и
 * «Тип» в шторке одно и то же состояние.
 */
@Composable internal fun TargetRail(state: ItemFilterState, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val entries = listOf<ItemType?>(null) + state.shelf.sections.map(ItemType::Section)
    Column(
        Modifier.width(52.dp).depthPanel(shape).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.forEach { type ->
            val on = type == state.filter.type
            val title = type?.let { typeTitle(it, uiLanguage) } ?: ui("common.all")
            Box(
                Modifier.size(40.dp, 28.dp).clip(RoundedCornerShape(8.dp)).background(if (on) Gold.copy(alpha = .16f) else Color.Transparent)
                    .clickable(role = Role.Tab, onClickLabel = title) {
                        state.update(state.filter.copy(type = type))
                        onOpen()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon((type as? ItemType.Section)?.section?.glyph ?: Icons.Outlined.Search, title, tint = if (on) GoldBright else Muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * The anvil (mockup B): the item's socket, then the [tool] laid on it and, for an orb, its [omen] — then the item itself as it
 * stands, its base and every line with its badge, and the server's last word about it. A tap on the item opens the picker.
 */
@Composable internal fun Anvil(game: GameUi, item: ItemView?, tool: Socket, omen: Socket?, onPick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.background(Brush.verticalGradient(listOf(Gold.copy(alpha = .14f).compositeOver(Panel), Panel)), shape)
            .border(1.dp, Bronze, shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Top) {
            ItemSocket(item, onPick)
            Plus()
            RoundSocket(tool)
            omen?.let {
                Plus()
                RoundSocket(it)
            }
        }
        if (item == null) {
            Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onPick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(ui("forge.pick_item"), color = GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("forge.pick_item_hint"))
            }
        } else {
            AnvilItem(item)
        }
        game.holding.forgeLine.takeIf { it.isNotBlank() }?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Vital, modifier = Modifier.size(14.dp))
                Text(it, color = Vital, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** The item as the anvil holds it: the name in its rarity's colour, what it is and its affix sides (4.2.0), its base and its lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnvilItem(item: ItemView) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

/** The item's square socket, framed in its rarity's colour. */
@Composable private fun ItemSocket(item: ItemView?, onPick: () -> Unit) {
    val color = item?.let { rarityColor(it.rarity.name) } ?: Bronze
    val frame = RoundedCornerShape(14.dp)
    SocketColumn(ui("forge.socket_item")) {
        Box(
            Modifier.size(60.dp).clip(frame).background(PanelRaised, frame).border(2.dp, color, frame).clickable(role = Role.Button, onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            if (item != null) {
                ItemIcon(item, color, Modifier.size(34.dp))
            } else {
                Icon(ForgeGlyphs.Plus, null, tint = Muted, modifier = Modifier.size(22.dp))
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
