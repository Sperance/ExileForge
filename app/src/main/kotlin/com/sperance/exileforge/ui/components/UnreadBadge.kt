package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Ink

/*
 * Метка «непрочитано» (0.14.5, #37, макет «Метка почты», вариант B «Изумрудная искра») - одна семья на шапку и ящик, язык
 * утверждённой метки вкладки: искра [UnreadDot] без числа - на значке, число [UnreadCount] - только внутри ящика.
 */

/**
 * Искра «есть непрочитанное»: точка 8dp цвета [Gold] с мягким ореолом и вырезом [Ink] вокруг - отделяет её от
 * контура значка. Без числа: сколько - скажет ящик.
 */
@Composable fun UnreadDot(modifier: Modifier = Modifier) {
    Box(
        modifier.size(DOT + CUT * 2).drawBehind {
            val centre = Offset(size.width / 2, size.height / 2)
            val dot = DOT.toPx() / 2
            drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = HALO), Gold.copy(alpha = 0f)), centre, dot * 3), dot * 3, centre)
            drawCircle(Ink, dot + CUT.toPx(), centre)
            drawCircle(Gold, dot, centre)
        },
    )
}

/** Значок [content] с искрой в правом верхнем углу, пока [unread]. */
@Composable fun UnreadBadge(unread: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier) {
        content()
        if (unread) UnreadDot(Modifier.align(Alignment.TopEnd).offset(x = CUT + 2.dp, y = -(CUT + 2.dp)))
    }
}

/** Счёт непрочитанного внутри ящика (вкладки): число [Gold] полужирным, без плашки; ноль - ничего. */
@Composable fun UnreadCount(count: Int, modifier: Modifier = Modifier, size: TextUnit = 12.sp) {
    if (count > 0) Text(count.toString(), modifier, color = Gold, fontSize = size, fontWeight = FontWeight.Bold, maxLines = 1)
}

/** Сторона искры, вырез вокруг неё и сила ореола. */
private val DOT: Dp = 8.dp
private val CUT: Dp = 2.dp
private const val HALO = .55f
