package com.sperance.exileforge.ui.screens.checks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun LogCard(log: RequestLog) {
    var expanded by remember(log) { mutableStateOf(false) }
    val accent = if (log.ok) Gold else MaterialTheme.colorScheme.error
    ForgePanel(Modifier.clickable { expanded = !expanded }, accent = accent) {
        Text("${log.method}  ${log.status ?: "NETWORK"}  ·  ${log.elapsedMs} ms", color = accent, style = MaterialTheme.typography.labelLarge)
        Text(log.path, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
        if (expanded) SelectionContainer {
            Column {
                if (log.request.isNotBlank()) Text("REQUEST\n${log.request}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("RESPONSE\n${log.response}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
            }
        }
    }
}
