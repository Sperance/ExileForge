package com.sperance.exileforge.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.weaponTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune

/**
 * Плитка вещи - сжатая карточка «Реликвария» в том же облике редкости ([relicGround], [RelicSocket], [RelicLook.nameStyle]):
 * гнездо с уровнем на углу ([LevelBadge]) и имя в шапке, под ним «что это · редкость», база одной строкой, затем каждая строка вещи мелким шрифтом в цвете её вида -
 * без значков и тиров. [footer] - что добавляет список под строками (вердикт, «Надеть»).
 * Строка тайника: [trailing] - напротив имени (цена, вердикт, отметки), [selected] - золотая рамка, [onClick] - вся плитка,
 * [onLongClick] - удержание плитки: в тайнике оно начинает выбор для продажи.
 *
 * Врождённые строки - над чертой, как в полной карточке.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemTile(
    item: ItemView,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable ColumnScope.() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val look = relicLook(item.rarity)
    Column(
        modifier.fillMaxWidth().relicGround(look, RoundedCornerShape(16.dp), selected)
            .then(if (onClick != null || onLongClick != null) Modifier.combinedClickable(onLongClick = onLongClick) { onClick?.invoke() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                RelicSocket(item, look, 40.dp)
                LevelBadge(item, look, Modifier.align(Alignment.BottomEnd).offset(x = 7.dp, y = 6.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = look.nameStyle(15), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(item.weaponType?.let { weaponTitle(it) } ?: slotTitle(item.slot), rarityTitle(item.rarity)).joinToString(" · "),
                    color = look.muted,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = .4.sp,
                    maxLines = 1,
                )
            }
            trailing?.let { Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp), content = it) }
        }
        if (item.base.isNotEmpty()) {
            Text(
                buildAnnotatedString {
                    item.base.forEachIndexed { i, property ->
                        if (i > 0) append(" · ")
                        append(basePropertyText(property, withBase = false))
                    }
                },
                color = Parchment,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        val lines = item.orderedLines
        if (lines.isNotEmpty()) {
            RelicDivider(look)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lines.forEachIndexed { i, line ->
                    Text(line.text, color = modColor(line.marks), style = MaterialTheme.typography.labelSmall)
                    if (implicitEnds(line, lines.getOrNull(i + 1))) HorizontalDivider(Modifier.padding(vertical = 2.dp), thickness = .5.dp, color = Color.White.copy(alpha = .08f))
                }
            }
        }
        footer()
    }
}

/**
 * Уровень вещи числом на углу гнезда: у карты без тира - уровень карты ([ItemView.mapLevel]) цветом `Rune`, у прочих - уровень
 * предмета в нити редкости; для чтения с экрана - подпись целиком. Уровня нет - значка нет.
 */
@Composable private fun LevelBadge(item: ItemView, look: RelicLook, modifier: Modifier) {
    val map = item.mapLevel
    val level = map ?: item.level.takeIf { it > 0 } ?: return
    val tint = if (map != null) Rune else look.rarity
    val shape = RoundedCornerShape(8.dp)
    val label = if (map != null) ui("card.map_level", map) else ui("row.level", level)
    Text(
        level.toString(),
        color = if (map != null) Rune else Parchment,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier.widthIn(min = 22.dp).heightIn(min = 16.dp).background(BadgeGround, shape).border(1.dp, tint.copy(alpha = .55f), shape)
            .padding(horizontal = 4.dp).clearAndSetSemantics { contentDescription = label },
    )
}

/** Подложка значка уровня - темнее гнезда, чтобы число читалось поверх рисунка. */
private val BadgeGround = Color(0xFF0B1013)
