package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.features.WarmStep
import com.sperance.exileforge.presentation.features.Warmup
import com.sperance.exileforge.ui.theme.*

/** The loading screen (3.54.0): the bar of the warm-up and each of its steps, ticked as it is done. */
@Composable fun WarmupScreen(warmup: Warmup) {
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("warmup.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(progress = { warmup.progress }, modifier = Modifier.fillMaxWidth(), color = Gold, trackColor = Panel)
            WarmStep.entries.forEach { step ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step in warmup.done) {
                        Text("✓", color = Vital, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(16.dp))
                    } else {
                        CircularProgressIndicator(Modifier.size(14.dp), color = Muted, strokeWidth = 2.dp)
                    }
                    Text(ui("warmup.step.${step.name}"), color = if (step in warmup.done) Parchment else Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
