package com.sperance.exileforge.ui.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.sperance.exileforge.core.display.Portrait
import com.sperance.exileforge.core.display.PortraitPaint
import java.util.concurrent.ConcurrentHashMap

/** One shape turned into what a `DrawScope` paints: parsed once per portrait, not per frame. */
private class Prepared(val path: Path, val fill: Brush?, val fillAlpha: Float, val stroke: Brush?, val strokeAlpha: Float, val style: Stroke)

private val prepared = ConcurrentHashMap<Portrait, List<Prepared>>()

private fun brush(paint: PortraitPaint?): Brush? = when (paint) {
    null -> null
    is PortraitPaint.Solid -> SolidColor(Color(paint.argb))
    is PortraitPaint.Linear -> Brush.linearGradient(*paint.stops.map { it.offset to Color(it.argb) }.toTypedArray(),
        start = Offset(paint.x1, paint.y1), end = Offset(paint.x2, paint.y2))
    is PortraitPaint.Radial -> Brush.radialGradient(*paint.stops.map { it.offset to Color(it.argb) }.toTypedArray(),
        center = Offset(paint.cx, paint.cy), radius = paint.r.coerceAtLeast(.01f))
}

/** A shape whose outline will not parse costs itself alone; the rest of the portrait still draws. */
private fun prepare(portrait: Portrait): List<Prepared> = prepared.getOrPut(portrait) {
    portrait.shapes.mapNotNull { shape ->
        runCatching {
            Prepared(PathParser().parsePathString(shape.d).toPath(), brush(shape.fill), shape.fillAlpha, brush(shape.stroke), shape.strokeAlpha,
                Stroke(shape.strokeWidth, cap = if (shape.round) StrokeCap.Round else StrokeCap.Butt, join = if (shape.round) StrokeJoin.Round else StrokeJoin.Miter))
        }.getOrNull()
    }
}

/**
 * The server's portrait, filling this scope's box: scaled to cover it and centred, so a three-by-four
 * box shows all of it and any other box crops the edges rather than leaving bands.
 */
fun DrawScope.drawPortrait(portrait: Portrait, lift: Float = 0f) {
    val scale = maxOf(size.width / portrait.width, size.height / portrait.height)
    val dx = (size.width - portrait.width * scale) / 2
    val dy = (size.height - portrait.height * scale) / 2 + lift
    withTransform({ translate(dx, dy); scale(scale, scale, Offset.Zero) }) {
        prepare(portrait).forEach { shape ->
            shape.fill?.let { drawPath(shape.path, it, alpha = shape.fillAlpha) }
            shape.stroke?.let { drawPath(shape.path, it, alpha = shape.strokeAlpha, style = shape.style) }
        }
    }
}

/**
 * A round token cut from a portrait's face (since 2.31.0): the three-by-four box is placed so that
 * its face circle (`Portrait.TOKEN_*`) lands on [centre] with [radius], and everything outside it
 * is clipped. [draw] paints the box — the server's portrait or the client's own bust alike.
 */
fun DrawScope.drawToken(centre: Offset, radius: Float, ring: Color, draw: DrawScope.() -> Unit) {
    val width = radius / Portrait.TOKEN_R
    val left = centre.x - width * Portrait.TOKEN_X
    val top = centre.y - width * Portrait.TOKEN_Y
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(centre, radius)) }
    clipPath(circle) {
        drawCircle(Color(0xFF0B0E13), radius, centre)
        inset(left, top, size.width - left - width, size.height - top - width * 4f / 3f) { draw() }
    }
    drawCircle(ring, radius, centre, style = Stroke(radius * .12f))
    drawCircle(Color.Black.copy(alpha = .5f), radius * 1.06f, centre, style = Stroke(radius * .04f))
}
