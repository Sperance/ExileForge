package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted

/**
 * A refusal, where the action was taken (2.46.0): nothing rises from the bottom of the screen any
 * more, and a success says nothing — what changed is already on screen. The line stays until it is
 * tapped away, the tab changes or the next command starts.
 */
@Composable fun RefusalLine(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val text = message ?: return
    Row(modifier.fillMaxWidth().background(LifeRed.copy(alpha = .14f)).clickable(onClickLabel = ui("common.close"), onClick = onDismiss)
        .padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = LifeRed, modifier = Modifier.size(16.dp))
        Text(text, color = LifeRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Close, null, tint = Muted, modifier = Modifier.size(14.dp))
    }
}
