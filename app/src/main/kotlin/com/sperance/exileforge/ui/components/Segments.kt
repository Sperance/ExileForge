package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Vital

/**
 * Сегменты: одна капсула 32dp, выбранный сегмент залит бронзой, у сегмента -
 * число [badge] плашкой (0 - без плашки). Плотная замена вкладок со значком над словом.
 */
@Composable fun <T> Segments(
    options: List<T>,
    chosen: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    badge: (T) -> Int = { 0 },
    onPick: (T) -> Unit,
) {
    val outer = RoundedCornerShape(10.dp)
    val inner = RoundedCornerShape(7.dp)
    Row(
        modifier.fillMaxWidth().height(32.dp).background(Abyss, outer).border(1.dp, Bronze.copy(alpha = .5f), outer).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val on = option == chosen
            Box(
                Modifier.weight(1f).fillMaxHeight().then(if (on) Modifier.background(Bronze.copy(alpha = .55f), inner) else Modifier)
                    .clickable { onPick(option) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(label(option), color = if (on) GoldBright else Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    badge(option).takeIf { it > 0 }?.let { count ->
                        Text(
                            count.toString(),
                            color = Ink,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Vital, RoundedCornerShape(50)).padding(horizontal = 5.dp),
                        )
                    }
                }
            }
        }
    }
}
