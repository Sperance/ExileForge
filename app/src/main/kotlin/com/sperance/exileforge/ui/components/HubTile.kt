package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/** Плиток в ряду хаба. */
private const val HUB_COLUMNS = 2

/** Самая низкая плитка хаба: ряд держит высоту самой высокой из двух. */
private val HUB_TILE_MIN_HEIGHT = 120.dp

/** Насколько тускло рисуется место, что уровень героя ещё не открыл. */
const val LOCKED_TILE_ALPHA = .45f

/**
 * Плитка хаба («Развитие», «Город»): название, значок на квадрате цвета [accent], строка того, что внутри ([note]), и
 * строка того, что ждёт ([news], цветом [accent]); [badge] - сколько ждёт действия, [alert] - что-то ждёт взгляда без числа (точка в
 * углу; есть число - точка ждёт), [lockedUntil] - уровень открытия, пока герой ниже него (плитка тусклая, с замком, вместо строк -
 * «откроется с N»). [chores] - строки дел, что ждут действия («Можно продлить: 1»), каждая с жёлтой точкой дела ([ChoreDot]), на месте
 * [news]. [badgeTip] - что значит счётчик: касание счётчика (зона 44 dp в углу) открывает его окном. Без [onOpen] - заглушка будущего
 * места: пунктирная рамка, тускло, не нажимается.
 */
@Immutable data class HubTile(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val note: String?,
    val news: String?,
    val badge: Int = 0,
    val lockedUntil: Int? = null,
    val chores: List<String> = emptyList(),
    val alert: Boolean = false,
    val badgeTip: String? = null,
    val onOpen: (() -> Unit)?,
)

/** Хаб плитками по две в ряд; нечётная последняя - во всю ширину. */
@Composable fun HubGrid(tiles: List<HubTile>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(HUB_COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { HubTileCard(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

/** Одна плитка: значок на подкрашенном квадрате, имя и строки внизу; в углу - счётчик того, что ждёт, точка или замок. */
@Composable fun HubTileCard(tile: HubTile, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val locked = tile.lockedUntil
    val hot = (tile.badge > 0 || tile.alert) && locked == null
    val open = tile.onOpen
    val framed = modifier.heightIn(min = HUB_TILE_MIN_HEIGHT).alpha(if (locked != null || open == null) LOCKED_TILE_ALPHA else 1f).clip(shape)
    Box(
        if (open == null) {
            framed.dashedBorder(Bronze, shape).padding(12.dp)
        } else {
            framed.depthPanel(shape).border(1.dp, if (hot) tile.accent.copy(alpha = .55f) else PanelRaised, shape)
                .clickable(role = Role.Button, onClick = open).padding(12.dp)
        },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(40.dp).background(tile.accent.copy(alpha = .14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(tile.icon, null, tint = tile.accent, modifier = Modifier.size(24.dp))
            }
            Text(tile.title, color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (locked != null) {
                    MutedText(ui("unlock.from", locked))
                } else {
                    tile.note?.let { MutedText(it) }
                    if (tile.chores.isNotEmpty()) {
                        tile.chores.forEach { ChoreLine(it) }
                    } else {
                        tile.news?.let { Text(it, color = tile.accent, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }
        if (locked != null) Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
        when {
            !hot -> Unit

            tile.badge > 0 -> Tipped(tile.badgeTip?.let { tip -> { Tip(body = tip, tint = tile.accent) } }, Modifier.align(Alignment.TopEnd).sizeIn(minWidth = BADGE_ZONE, minHeight = BADGE_ZONE), Alignment.TopEnd) {
                Box(Modifier.background(tile.accent, CircleShape).padding(horizontal = 6.dp, vertical = 1.dp)) {
                    Text(tile.badge.toString(), color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            else -> Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(8.dp).background(GoldBright, CircleShape))
        }
    }
}

/** Зона касания счётчика плитки: квадрат в её углу, счётчик - в его углу. */
private val BADGE_ZONE = 44.dp

/** Строка дела плитки: жёлтая точка и текст. */
@Composable private fun ChoreLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ChoreDot()
        Text(text, color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Точка дела, что ждёт действия: одна на плитке, вкладке, разделе и строке - цвета [Caution]. */
@Composable fun ChoreDot(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(Caution, CircleShape))
}

/** Пунктирная рамка заглушки: контур [shape] штрихами цвета [color]. */
internal fun Modifier.dashedBorder(color: Color, shape: Shape): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val dash = 6.dp.toPx()
    drawOutline(shape.createOutline(size, layoutDirection, this), color, style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))))
}
