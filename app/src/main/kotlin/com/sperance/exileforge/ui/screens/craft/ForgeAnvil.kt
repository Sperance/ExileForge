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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.sidesText
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.filter.ItemSection
import com.sperance.exileforge.core.filter.ItemType
import com.sperance.exileforge.core.filter.typeTitle
import com.sperance.exileforge.core.forge.ForgeTarget
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.ui.components.AffixBadge
import com.sperance.exileforge.ui.components.BaseChip
import com.sperance.exileforge.ui.components.ItemFilterState
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.theme.*

/** Что лежит в круглом гнезде наковальни: рисунок (null - гнездо пусто) и цвет; имя лежащего называет рецепт полосы ([Recipe]). */
internal class Socket(val accent: Color, val glyph: (@Composable () -> Unit)?, val onClick: (() -> Unit)? = null)

/** Значок раздела на полке у наковальни. */
private val ItemSection.glyph: ImageVector get() = when (this) {
    ItemSection.GEAR -> ForgeGlyphs.Swords
    ItemSection.MAPS -> ForgeGlyphs.Atlas
    ItemSection.TOOLS -> Icons.Outlined.Build
    ItemSection.WORN -> ForgeGlyphs.Helm
}

/**
 * Полка у наковальни (быстрый переключатель «Типа» общего фильтра [ItemShelf.FORGE], как рейка тайника):
 * «Все» и разделы списка рисунками; выбранный - в золоте. Раздел по месту вещи ([ItemSection.apart], надетое) - под чертой отдельной
 * кнопкой в золотой рамке с подписью. Нажатие ставит тип фильтра и открывает выбор цели на вещах - выбор на полке и «Тип» в шторке одно
 * и то же состояние.
 */
@Composable internal fun TargetRail(state: ItemFilterState, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val (apart, kinds) = state.shelf.sections.partition { it.apart }
    val pick = { type: ItemType? ->
        state.update(state.filter.copy(type = type))
        onOpen()
    }
    Column(
        Modifier.width(52.dp).depthPanel(shape).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (listOf<ItemType?>(null) + kinds.map(ItemType::Section)).forEach { type ->
            val title = type?.let { typeTitle(it, uiLanguage) } ?: ui("common.all")
            RailButton((type as? ItemType.Section)?.section?.glyph ?: Icons.Outlined.Search, title, on = type == state.filter.type) { pick(type) }
        }
        if (apart.isNotEmpty()) Box(Modifier.width(34.dp).height(1.dp).background(PanelRaised))
        apart.forEach { section ->
            val type = ItemType.Section(section)
            ApartButton(section.glyph, typeTitle(type, uiLanguage), on = type == state.filter.type) { pick(type) }
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

/** Отдельная кнопка полки под чертой: рисунок [glyph] и подпись [title] в золотой рамке, выбранная - ярче. */
@Composable private fun ApartButton(glyph: ImageVector, title: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.size(46.dp).clip(shape).background(Gold.copy(alpha = if (on) .22f else .10f), shape).border(1.dp, Gold.copy(alpha = if (on) .9f else .55f), shape)
            .clickable(role = Role.Tab, onClickLabel = title, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(glyph, null, tint = GoldBright, modifier = Modifier.size(16.dp))
        Text(title, color = Gold, fontSize = 8.5.sp, lineHeight = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

/**
 * The anvil: the target's socket, then the [tool] laid on it and, for an orb, its [omen]; под гнёздами - сама цель как есть и последнее
 * слово сервера о ней. Связку и итог называет рецепт полосы ([ForgeBar]), наковальня отдаёт место предмету. Цель - [ForgeTarget]: вещь
 * (база и строки со значками). Касание лежащей цели открывает её карточку ([ForgeCards]): выбор другой - «Сменить», рядом - надеть или
 * снять, в «⋯» - аукцион; пустая открывает выбор.
 */
@Composable internal fun Anvil(
    game: GameUi,
    cards: ForgeCards,
    target: ForgeTarget?,
    tool: Socket,
    omen: Socket?,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val inspect = rememberInspect()
    val open = {
        when (target) {
            null -> onPick()
            is ForgeTarget.Gear -> inspect(cards.of(target.view.item, ui(target.changeKey), onPick))
        }
    }
    Column(
        modifier.background(Brush.verticalGradient(listOf(Gold.copy(alpha = .14f).compositeOver(Panel), Panel)), shape)
            .border(1.dp, Bronze, shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Top) {
            TargetSocket(target, open)
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
        }
        game.holding.forgeLine.takeIf { it.isNotBlank() }?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Vital, modifier = Modifier.size(14.dp))
                Text(it, color = Vital, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** The item as the anvil holds it: the name in its rarity's colour, what it is and its affix sides, its base and its lines. */
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
                item.sides?.let { sidesText(it, item.influenceSlots) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
        )
        val base = item.base.flatMap { it.values }
        if (base.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                base.forEach { BaseChip(it) }
            }
        }
        item.orderedLines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AffixBadge(line.marks)
                Text(line.text, color = ModBlue, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun Plus() = Text("+", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 18.dp))

/** The target's square socket, framed in its rarity's colour: the item's icon. */
@Composable private fun TargetSocket(target: ForgeTarget?, onClick: () -> Unit) {
    val color = when (target) {
        null -> Bronze
        is ForgeTarget.Gear -> rarityColor(target.view.rarity.name)
    }
    val frame = RoundedCornerShape(14.dp)
    Box(Modifier.width(62.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(60.dp).clip(frame).background(PanelRaised, frame).border(2.dp, color, frame).clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when (target) {
                null -> Icon(ForgeGlyphs.Plus, null, tint = Muted, modifier = Modifier.size(22.dp))
                is ForgeTarget.Gear -> ItemIcon(target.view, color, Modifier.size(34.dp))
            }
        }
    }
}

/** A round socket: the orb, essence or bench line laid on the item, or the omen paired with the orb; dim while empty. */
@Composable private fun RoundSocket(socket: Socket) {
    val filled = socket.glyph != null
    Box(Modifier.width(62.dp), contentAlignment = Alignment.Center) {
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
