package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/**
 * «✦ +20%»: the copy's quality as a badge, nothing for none. A tap says what it boosts — the base's figures, or a
 * catalyst's kind of modifiers. [compact] is the list line's size, the card's otherwise.
 */
@Composable fun QualityBadge(item: ItemView, modifier: Modifier = Modifier, compact: Boolean = false) {
    val quality = item.quality.takeIf { it > 0 } ?: return
    val catalyst = item.catalyst
    val shape = RoundedCornerShape(50)
    Tipped({
        Tip(
            ui("card.quality", quality),
            catalyst?.let { ui("item.quality_catalyst_hint", ui("enum.catalyst.${it.name}")) } ?: ui("item.quality_hint", quality),
            tint = GoldBright,
        )
    }, modifier) {
        Text(
            ui("item.quality_badge", quality),
            color = GoldBright,
            fontWeight = FontWeight.Bold,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            modifier = Modifier.background(Gold.copy(alpha = .12f), shape).border(1.dp, Gold.copy(alpha = .5f), shape)
                .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = if (compact) 0.dp else 2.dp),
        )
    }
}
