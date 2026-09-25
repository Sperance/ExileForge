package com.sperance.exileforge.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.sperance.exileforge.ui.theme.Muted

/** Secondary text — a hint, a caption, a count: muted, and small unless [style] says otherwise. */
@Composable internal fun MutedText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) =
    Text(text, modifier, color = Muted, style = style)
