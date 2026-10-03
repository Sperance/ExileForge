package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.*

/**
 * A screen's few tabs as pills: the open one filled in gold. [segmented] lays them as halves of one track instead —
 * the open one raised — for two views of the same shelf; [trailing] closes the row (a screen's «?»).
 */
@Composable fun PillTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    segmented: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val pill = RoundedCornerShape(if (segmented) 9.dp else 16.dp)
    val track = if (segmented) {
        Modifier.background(Panel, RoundedCornerShape(12.dp)).border(1.dp, PanelRaised, RoundedCornerShape(12.dp)).padding(3.dp)
    } else {
        Modifier
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(if (segmented) Modifier.weight(1f).then(track) else Modifier, horizontalArrangement = Arrangement.spacedBy(if (segmented) 0.dp else 6.dp)) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                val fill = when {
                    !on -> if (segmented) Color.Transparent else Panel
                    segmented -> PanelRaised
                    else -> Gold
                }
                Text(
                    label,
                    color = when {
                        !on -> Muted
                        segmented -> GoldBright
                        else -> Ink
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = (if (segmented) Modifier.weight(1f) else Modifier).clip(pill).background(fill, pill)
                        .selectable(selected = on, enabled = enabled, role = Role.Tab) { if (!on) onSelect(index) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        if (!segmented) Spacer(Modifier.weight(1f))
        trailing()
    }
}
