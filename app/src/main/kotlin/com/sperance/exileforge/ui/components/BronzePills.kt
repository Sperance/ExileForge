package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted

/**
 * Бронзовые плашки выбора раздела (4.4.x, Доска славы: регион и ступень раша, профессия): ряд с прокруткой вбок, выбранная -
 * залита бронзой [Brass] светлым текстом, остальные - бронзовая нить.
 */
@Composable fun BronzePills(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(50)
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label,
                color = if (on) GoldBright else Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.clip(pill).then(if (on) Modifier.background(Brass.copy(alpha = .28f), pill) else Modifier)
                    .border(1.dp, Brass.copy(alpha = if (on) .9f else .4f), pill)
                    .selectable(selected = on, role = Role.Tab) { if (!on) onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
