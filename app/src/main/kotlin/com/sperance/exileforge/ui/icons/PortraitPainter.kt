package com.sperance.exileforge.ui.icons

import androidx.collection.MutableIntObjectMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import com.sperance.exileforge.core.display.Portrait
import com.sperance.exileforge.core.display.PortraitPaint
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

/** One shape turned into what a `DrawScope` paints: parsed once per portrait, not per frame. */
private class Prepared(val path: Path, val fill: Brush?, val fillAlpha: Float, val stroke: Brush?, val strokeAlpha: Float, val style: Stroke)

private val prepared = ConcurrentHashMap<Portrait, List<Prepared>>()

private fun brush(paint: PortraitPaint?): Brush? = when (paint) {
    null -> null

    is PortraitPaint.Solid -> SolidColor(Color(paint.argb))

    is PortraitPaint.Linear -> Brush.linearGradient(
        *paint.stops.map { it.offset to Color(it.argb) }.toTypedArray(),
        start = Offset(paint.x1, paint.y1),
        end = Offset(paint.x2, paint.y2),
    )

    is PortraitPaint.Radial -> Brush.radialGradient(
        *paint.stops.map { it.offset to Color(it.argb) }.toTypedArray(),
        center = Offset(paint.cx, paint.cy),
        radius = paint.r.coerceAtLeast(.01f),
    )
}

/** A shape whose outline will not parse costs itself alone; the rest of the portrait still draws. */
private fun prepare(portrait: Portrait): List<Prepared> = prepared.getOrPut(portrait) {
    portrait.shapes.mapNotNull { shape ->
        runCatching {
            Prepared(
                PathParser().parsePathString(shape.d).toPath(),
                brush(shape.fill),
                shape.fillAlpha,
                brush(shape.stroke),
                shape.strokeAlpha,
                Stroke(shape.strokeWidth, cap = if (shape.round) StrokeCap.Round else StrokeCap.Butt, join = if (shape.round) StrokeJoin.Round else StrokeJoin.Miter),
            )
        }.getOrNull()
    }
}

/**
 * The server's portrait, filling this scope's box: scaled to cover it and centred, so a three-by-four
 * box shows all of it and any other box crops the edges rather than leaving bands. Портрет рисуется растром из
 * [PortraitRasters] - фигуры сервера кладутся в растр раз на размер, кадр только переносит готовую картинку.
 */
fun DrawScope.drawPortrait(portrait: Portrait, lift: Float = 0f) {
    val scale = maxOf(size.width / portrait.width, size.height / portrait.height)
    val dx = (size.width - portrait.width * scale) / 2
    val dy = (size.height - portrait.height * scale) / 2 + lift
    val raster = PortraitRasters.of(this, portrait, scale)
    val fit = scale / raster.scale
    withTransform({
        translate(dx, dy)
        scale(fit, fit, Offset.Zero)
    }) { drawImage(raster.image) }
}

/** Фигуры портрета пером: в растр [PortraitRasters] и больше никуда. */
private fun DrawScope.drawShapes(portrait: Portrait) {
    prepare(portrait).forEach { shape ->
        shape.fill?.let { drawPath(shape.path, it, alpha = shape.fillAlpha) }
        shape.stroke?.let { drawPath(shape.path, it, alpha = shape.strokeAlpha, style = shape.style) }
    }
}

/** Растр портрета в масштабе [scale] (пикселей на единицу поля портрета); [used] - когда его брали в последний раз. */
private class Raster(val image: ImageBitmap, val scale: Float, var used: Long)

/**
 * Растры портретов сервера (кэш на портрет и ступень размера): ступень - корень четвёртой степени из двух, растр не меньше
 * нужного и уменьшается при рисовании не больше чем на 16%. Сверх [BUDGET] байт уходят давно не нужные. Только главный поток.
 */
private object PortraitRasters {
    private const val BUDGET = 24L * 1024 * 1024
    private const val STEPS_PER_OCTAVE = 4f

    private val rasters = IdentityHashMap<Portrait, MutableIntObjectMap<Raster>>()
    private val pen = CanvasDrawScope()
    private var tick = 0L
    private var bytes = 0L

    fun of(scope: DrawScope, portrait: Portrait, scale: Float): Raster {
        val step = ceil(log2(scale.coerceAtLeast(MIN_SCALE)) * STEPS_PER_OCTAVE).toInt()
        val sizes = rasters.getOrPut(portrait) { MutableIntObjectMap() }
        tick++
        sizes[step]?.let {
            it.used = tick
            return it
        }
        val fit = 2f.pow(step / STEPS_PER_OCTAVE)
        val width = ceil(portrait.width * fit).toInt().coerceAtLeast(1)
        val height = ceil(portrait.height * fit).toInt().coerceAtLeast(1)
        val image = ImageBitmap(width, height)
        pen.draw(Density(scope.density, scope.fontScale), scope.layoutDirection, Canvas(image), Size(width.toFloat(), height.toFloat())) {
            scale(fit, fit, Offset.Zero) { drawShapes(portrait) }
        }
        val raster = Raster(image, fit, tick)
        sizes[step] = raster
        bytes += width.toLong() * height * 4
        trim()
        return raster
    }

    /** Сверх бюджета - прочь самые давние растры, пока не влезет. */
    private fun trim() {
        while (bytes > BUDGET) {
            var oldest: Raster? = null
            var owner: MutableIntObjectMap<Raster>? = null
            var key = 0
            rasters.values.forEach { sizes ->
                sizes.forEach { step, raster ->
                    if (oldest == null || raster.used < oldest!!.used) {
                        oldest = raster
                        owner = sizes
                        key = step
                    }
                }
            }
            val gone = oldest ?: return
            if (gone.used == tick) return
            owner?.remove(key)
            bytes -= gone.image.width.toLong() * gone.image.height * 4
        }
    }

    private const val MIN_SCALE = .01f
}

/**
 * A round token cut from a portrait's face (since 2.31.0): the three-by-four box is placed so that
 * its face circle (`Portrait.TOKEN_*`) lands on [centre] with [radius], and everything outside it
 * is clipped. [draw] paints the box: the server's portrait.
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
