package com.sperance.exileforge.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * «Глубина» (3.88.7, выбор владельца из трёх макетов объёма): слои над тёмным фоном. Панель - градиент сверху светлее,
 * тонкая линия света по верхней кромке и мягкая тень под собой; главное действие - градиентная пилюля с зелёным ореолом;
 * поля и дорожки вкладок - вдавлены. Экраны берут эти модификаторы, а не рисуют объём сами.
 */

/** Верх и низ панели: светлее к кромке, как у стекла, подсвеченного сверху. */
val DepthTop = Color(0xEB222E38)
val DepthBottom = Color(0xEB121A21)

/** Линия света по верхней кромке и едва заметный контур. */
private val Rim = Color.White.copy(alpha = .10f)
private val Edge = Color.White.copy(alpha = .04f)

/** Градиент главной кнопки: светлая мята сверху, глубокий изумруд снизу. */
val PrimaryBrush = Brush.verticalGradient(listOf(Color(0xFF5BDB9B), Gold, Color(0xFF2E8F5E)))

/** Тень под слоем: чёрная - цветная тень на части прошивок выходит чёрной всё равно, ореол цвета рисует [glow]. */
private fun Modifier.lift(elevation: Dp, shape: Shape): Modifier = if (elevation > 0.dp) shadow(elevation, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black) else this

/** Линия света по верхней кромке формы - поверх содержимого, внутри скругления. */
private fun Modifier.topRim(color: Color = Rim): Modifier = drawWithContent {
    drawContent()
    val inset = 14.dp.toPx().coerceAtMost(size.width / 4)
    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, color, color, Color.Transparent), startX = 0f, endX = size.width), Offset(inset, .5.dp.toPx()), Offset(size.width - inset, .5.dp.toPx()), 1.dp.toPx())
}

/**
 * Панель «Глубины»: градиент [DepthTop] → [DepthBottom] (с лёгким оттенком [accent], если он задан), линия света сверху,
 * контур и тень [elevation].
 */
fun Modifier.depthPanel(shape: Shape = RoundedCornerShape(18.dp), accent: Color? = null, elevation: Dp = 10.dp): Modifier {
    val top = accent?.copy(alpha = .07f)?.compositeOver(DepthTop) ?: DepthTop
    return lift(elevation, shape).background(Brush.verticalGradient(listOf(top, DepthBottom)), shape).border(1.dp, Edge, shape).topRim()
}

/** Приподнятый мелкий слой - фишка, вторая кнопка, выбранная вкладка: светлое стекло с тенью. */
fun Modifier.depthRaised(shape: Shape = RoundedCornerShape(50), elevation: Dp = 4.dp): Modifier = lift(elevation, shape)
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .10f), Color.White.copy(alpha = .04f))), shape)
    .topRim(Color.White.copy(alpha = .15f))

/** Вдавленный слой - поле ввода, дорожка вкладок, шкала: темнее фона, тень внутрь. */
fun Modifier.depthInset(shape: Shape = RoundedCornerShape(50)): Modifier = background(Color.Black.copy(alpha = .35f), shape)
    .border(1.dp, Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.White.copy(alpha = .05f))), shape)

/** Главное действие: градиентная пилюля с зелёным ореолом; выключенное - плоская панель. */
fun Modifier.depthPrimary(shape: Shape = RoundedCornerShape(50), enabled: Boolean = true): Modifier = if (enabled) {
    glow(Gold, radius = 10.dp, shape = shape).background(PrimaryBrush, shape).topRim(Color.White.copy(alpha = .45f))
} else {
    background(Panel, shape)
}
