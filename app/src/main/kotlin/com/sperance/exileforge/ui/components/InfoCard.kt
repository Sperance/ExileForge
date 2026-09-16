package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.icons.propertyIcon
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted

/** Parchment note pinned to the stash wall; turns blood-red when it reports a failure. */
@Composable internal fun InfoCard(title: String, body: String, failure: Boolean = false) {
    val accent = if (failure) MaterialTheme.colorScheme.error else Gold
    ForgePanel(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(propertyIcon(title), null, tint = accent, modifier = Modifier.size(18.dp))
            Text(title, color = accent, style = MaterialTheme.typography.titleMedium)
        }
        SelectionContainer { Text(body, color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}
