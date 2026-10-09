package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted

/** Вкладка-плита: подпись, значок режима и его цвет механики ([com.sperance.exileforge.ui.theme.ModeHue]). */
@Immutable data class StoneTab(val label: String, val icon: ImageVector, val tint: Color)

/**
 * Каменные вкладки (4.4.x, из полосы «Похода» - макет «Скрижали»): тёмная дорожка, открытая вкладка - приподнятая плита в
 * золотой нити. У каждой над подписью значок режима в цвете его механики (у закрытых - приглушён). [scrollable] - плиты своей
 * ширины с прокруткой вбок (Доска славы); иначе делят ширину поровну («Поход»).
 */
@Composable fun StoneTabs(tabs: List<StoneTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, scrollable: Boolean = true) {
    val track = RoundedCornerShape(10.dp)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clip(track)
            .background(Brush.verticalGradient(listOf(TrackTop, TrackBottom))).border(1.dp, Bronze, track)
            .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            StoneTile(tab, i == selected, if (scrollable) Modifier.widthIn(min = TILE_MIN_WIDTH) else Modifier.weight(1f)) { onSelect(i) }
        }
    }
}

/** Одна плита: значок режима над подписью; открытая приподнята и в золотой нити. */
@Composable private fun StoneTile(tab: StoneTab, on: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Column(
        modifier.clip(shape)
            .then(if (on) Modifier.background(Brush.verticalGradient(listOf(StoneTop, StoneBottom))).border(1.dp, Gold.copy(alpha = .35f), shape) else Modifier)
            .selectable(selected = on, role = Role.Tab) { if (!on) onSelect() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(tab.icon, null, tint = if (on) tab.tint else tab.tint.copy(alpha = DIM), modifier = Modifier.size(16.dp))
        Text(tab.label, color = if (on) GoldBright else Muted, style = relicName(12), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val TrackTop = Color(0xFF0B1116)
private val TrackBottom = Color(0xFF0A0F13)
private val StoneTop = Color(0xFF1C2A33)
private val StoneBottom = Color(0xFF121B22)
private val TILE_MIN_WIDTH = 72.dp

/** Насколько приглушён значок закрытой вкладки. */
private const val DIM = .55f
