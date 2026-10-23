package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted

/** Флажок выбора нескольких: отмеченный - золотая заливка с галочкой, нет - пустой квадрат в окантовке; нажатие - у строки, что его держит. */
@Composable fun CheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier.size(22.dp).clip(shape).background(if (checked) Gold else Color.Transparent).border(1.5.dp, if (checked) Gold else Muted, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Outlined.Check, null, tint = Ink, modifier = Modifier.size(16.dp))
    }
}
