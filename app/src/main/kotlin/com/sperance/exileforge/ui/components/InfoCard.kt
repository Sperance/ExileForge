package com.sperance.exileforge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun InfoCard(title: String, body: String, failure: Boolean = false) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, if (failure) MaterialTheme.colorScheme.error else Gold.copy(alpha = .3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = if (failure) MaterialTheme.colorScheme.error else Gold, style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Text(body, color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
