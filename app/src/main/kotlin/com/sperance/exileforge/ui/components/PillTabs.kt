package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
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
 * Вкладки экрана таблетками (3.88.3, «Мягкий»): открытая - на приподнятом фоне светлым текстом, зелёный остаётся главной
 * кнопке. [segmented] кладёт их долями одной капсулы-дорожки - два вида одной полки; [trailing] замыкает ряд («?» экрана).
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
    val pill = RoundedCornerShape(50)
    // «Глубина» (3.88.7): дорожка вдавлена, открытая вкладка приподнята стеклом; отдельные таблетки - стекло, открытая - зелёная.
    val track = if (segmented) {
        Modifier.depthInset(pill).padding(4.dp)
    } else {
        Modifier
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(if (segmented) Modifier.weight(1f).then(track) else Modifier, horizontalArrangement = Arrangement.spacedBy(if (segmented) 0.dp else 6.dp)) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                val fill = when {
                    on && segmented -> Modifier.depthRaised(pill)
                    on -> Modifier.depthPrimary(pill)
                    segmented -> Modifier
                    else -> Modifier.depthRaised(pill, elevation = 2.dp)
                }
                Text(
                    label,
                    color = if (on && !segmented) {
                        Ink
                    } else if (on) {
                        GoldBright
                    } else {
                        Muted
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = (if (segmented) Modifier.weight(1f) else Modifier).then(fill).clip(pill)
                        .selectable(selected = on, enabled = enabled, role = Role.Tab) { if (!on) onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        if (!segmented) Spacer(Modifier.weight(1f))
        trailing()
    }
}
