package com.sperance.exileforge.ui.screens.checks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.network.RequestLog
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import kotlinx.serialization.json.*

@Composable internal fun LogCard(log: RequestLog) {
    var expanded by remember(log) { mutableStateOf(false) }
    OutlinedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${log.method}  ${log.status ?: "NETWORK"}  ·  ${log.elapsedMs} ms", color = if (log.ok) Gold else MaterialTheme.colorScheme.error)
            Text(log.path, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            if (expanded) SelectionContainer {
                Column {
                    if (log.request.isNotBlank()) Text("REQUEST\n${log.request}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("RESPONSE\n${log.response}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted)
                }
            }
        }
    }
}
