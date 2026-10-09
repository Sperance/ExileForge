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
import androidx.compose.ui.graphics.Color
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

/** Насколько тускло рисуется место, что уровень героя ещё не открыл (3.76.0). */
const val LOCKED_TILE_ALPHA = .45f

/**
 * Плитка хаба («Развитие», «Город» - 4.4.x): название, значок на квадрате цвета [accent], строка того, что внутри ([note]), и
 * строка того, что ждёт ([news], цветом [accent]); [badge] - сколько ждёт действия, [lockedUntil] - уровень открытия, пока герой
 * ниже него (плитка тусклая, с замком, вместо строк - «откроется с N»).
 */
@Immutable data class HubTile(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val note: String?,
    val news: String?,
    val badge: Int = 0,
    val lockedUntil: Int? = null,
    val onOpen: () -> Unit,
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

/** Одна плитка: значок на подкрашенном квадрате, имя и строки внизу; в углу - счётчик того, что ждёт, или замок. */
@Composable fun HubTileCard(tile: HubTile, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val locked = tile.lockedUntil
    val hot = tile.badge > 0 && locked == null
    Box(
        modifier.heightIn(min = HUB_TILE_MIN_HEIGHT).alpha(if (locked != null) LOCKED_TILE_ALPHA else 1f).clip(shape).depthPanel(shape)
            .border(1.dp, if (hot) tile.accent.copy(alpha = .55f) else PanelRaised, shape)
            .clickable(role = Role.Button, onClick = tile.onOpen).padding(12.dp),
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
                    tile.news?.let { Text(it, color = tile.accent, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        if (locked != null) Icon(Icons.Outlined.Lock, null, tint = Muted, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
        if (hot) {
            Box(Modifier.align(Alignment.TopEnd).background(tile.accent, CircleShape).padding(horizontal = 6.dp, vertical = 1.dp)) {
                Text(tile.badge.toString(), color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
