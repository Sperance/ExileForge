package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.icons.propertyIcon
import com.sperance.exileforge.ui.theme.Rune

@Composable fun PropertyRow(label: String, value: String, iconKey: String = label) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(propertyIcon(iconKey), null, tint = Rune, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.widthIn(max = 120.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}
