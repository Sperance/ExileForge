package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.icons.propertyIcon
import com.sperance.exileforge.ui.theme.*

/** One rolled property: socketed rune, engraved label, value struck in gold. */
@Composable fun PropertyRow(label: String, value: String, iconKey: String = label) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(26.dp).background(Rune.copy(alpha = .10f), CutCornerShape(5.dp)), contentAlignment = Alignment.Center) {
            Icon(propertyIcon(iconKey), null, tint = Rune, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Parchment, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.widthIn(max = 130.dp)
            .drawBehind { drawLine(Gold.copy(alpha = .25f), Offset(0f, size.height), Offset(size.width, size.height), 1f) },
            color = GoldBright, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
    }
}
