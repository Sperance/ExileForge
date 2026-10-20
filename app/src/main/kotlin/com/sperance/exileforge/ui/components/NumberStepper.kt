package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
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

/**
 * Степпер числа «−» N «+» с шагом 1 в границах [range]: на краю кнопка погашена, удержание листает с ускорением. [less] и [more] -
 * имена кнопок для чтения с экрана.
 */
@Composable fun NumberStepper(value: Int, range: IntRange, enabled: Boolean, less: String, more: String, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    val well = RoundedCornerShape(10.dp)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        ForgeIconButton(
            Icons.Outlined.Remove,
            less,
            { onChange((value - 1).coerceIn(range)) },
            kind = ButtonKind.Secondary,
            state = ButtonState.of(enabled && value > range.first),
            repeatOnHold = true,
        )
        Box(
            Modifier.widthIn(min = 52.dp).height(ButtonSize.Wide.height).background(Abyss, well).border(1.dp, Bronze, well).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(value.toString(), color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        ForgeIconButton(
            Icons.Outlined.Add,
            more,
            { onChange((value + 1).coerceIn(range)) },
            kind = ButtonKind.Secondary,
            state = ButtonState.of(enabled && value < range.last),
            repeatOnHold = true,
        )
    }
}
