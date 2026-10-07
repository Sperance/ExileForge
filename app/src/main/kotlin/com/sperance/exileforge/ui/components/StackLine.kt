package com.sperance.exileforge.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Стопка сумки строкой списка (3.91.0, как добыча карт): значок, имя и сколько - «+12» добытого или «−3» потраченного в цвете
 * [tint]; нажатие открывает карточку предмета.
 */
@Composable fun StackLine(game: GameUi, code: String, figure: String, tint: Color, onClick: () -> Unit) {
    val kind = game.index?.item(ItemCode(code))?.let(::bagVisualKind) ?: ItemVisualKind.ITEM
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, Bronze.copy(alpha = .6f), shape).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BagIcon(code, Modifier.size(26.dp), kind = kind)
        Text(itemTitle(code), color = Parchment, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(figure, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
