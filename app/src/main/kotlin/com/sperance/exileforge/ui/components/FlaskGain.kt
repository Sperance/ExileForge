package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.ui.theme.Vital

/**
 * Пополнение фляги на глазах (0.14.2, #21): заряды выросли - над кнопкой [content] всплывает и гаснет «+N», ободок коротко
 * вспыхивает цветом фляги [tint]. Источник роста не важен (убийство, сила, фонтан): виден любой прирост [charges], расход -
 * нет. Новый прирост во время всплытия начинает его заново со своим числом.
 */
@Composable fun FlaskGain(charges: Int, tint: Color, content: @Composable () -> Unit) {
    var last by remember { mutableIntStateOf(charges) }
    var gain by remember { mutableIntStateOf(0) }
    val rise = remember { Animatable(1f) }
    LaunchedEffect(charges) {
        val added = charges - last
        last = charges
        // Перезапуск отменил прежнее всплытие: расход его гасит, а не оставляет застывшим
        if (added <= 0) return@LaunchedEffect rise.snapTo(1f)
        gain = added
        rise.snapTo(0f)
        rise.animateTo(1f, tween(GAIN_MS, easing = LinearOutSlowInEasing))
    }
    Box(contentAlignment = Alignment.Center) {
        content()
        val t = rise.value
        if (t < 1f) {
            Box(Modifier.matchParentSize().border(2.dp, tint.copy(alpha = 1f - t), CircleShape))
            Text(
                "+$gain",
                color = Vital.copy(alpha = 1f - t),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).offset { IntOffset(0, -(RISE_FROM + RISE_BY * t).dp.roundToPx()) },
            )
        }
    }
}

/** Сколько длится всплытие, мс, и как высоко над кнопкой оно начинается и сколько поднимается, dp. */
private const val GAIN_MS = 900
private const val RISE_FROM = 10f
private const val RISE_BY = 14f
