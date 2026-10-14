package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.sperance.exileforge.ui.icons.Strokes

/**
 * A pen over a [DrawScope] that measures up rather than down.
 *
 * Every drawing in the scene is built from feet upward — a figure stands on a point and grows
 * toward the top of the screen — so the pen takes `y` growing up and turns it over itself, and the
 * scene moves the origin where it wants it with a translation. One reusable path, because the map
 * is a few hundred diamonds a frame.
 */
internal class Pen {
    lateinit var scope: DrawScope
    var color: Color = Color.White
    private val path = Path()
    private val outline = Path()

    fun rect(x: Float, y: Float, width: Float, height: Float) = scope.drawRect(color, Offset(x, -(y + height)), Size(width, height))
    fun ellipse(x: Float, y: Float, width: Float, height: Float) = scope.drawOval(color, Offset(x, -(y + height)), Size(width, height))
    fun circle(x: Float, y: Float, radius: Float) = scope.drawCircle(color, radius, Offset(x, -y))
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float) = scope.drawLine(color, Offset(x1, -y1), Offset(x2, -y2), width)

    /** Four corners in order, as one filled shape: a tile, a wall face. */
    fun quad(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float) {
        path.reset()
        path.moveTo(x1, -y1)
        path.lineTo(x2, -y2)
        path.lineTo(x3, -y3)
        path.lineTo(x4, -y4)
        path.close()
        scope.drawPath(path, color)
    }

    fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        path.reset()
        path.moveTo(x1, -y1)
        path.lineTo(x2, -y2)
        path.lineTo(x3, -y3)
        path.close()
        scope.drawPath(path, color)
    }

    /** An ellipse's outline only: a rune's ring, a portal's rim. */
    fun ring(x: Float, y: Float, width: Float, height: Float, stroke: Float) = scope.drawOval(color, Offset(x, -(y + height)), Size(width, height), style = Strokes.of(stroke))

    /** A broken line through [points] (x, y pairs): a crack, a root, a blade of grass. */
    fun polyline(points: FloatArray, width: Float) {
        for (i in 0 until points.size / 2 - 1) line(points[i * 2], points[i * 2 + 1], points[i * 2 + 2], points[i * 2 + 3], width)
    }

    /** Ломаная из трёх точек: без массива на кадр. */
    fun polyline(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, width: Float) {
        line(x1, y1, x2, y2, width)
        line(x2, y2, x3, y3, width)
    }

    /** Ломаная из четырёх точек. */
    fun polyline(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, width: Float) {
        polyline(x1, y1, x2, y2, x3, y3, width)
        line(x3, y3, x4, y4, width)
    }

    /** Ломаная из пяти точек. */
    fun polyline(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, x5: Float, y5: Float, width: Float) {
        polyline(x1, y1, x2, y2, x3, y3, x4, y4, width)
        line(x4, y4, x5, y5, width)
    }

    /** Многоугольник из первых [count] точек [points] (x, y парами) - арка на грани стены (0.14.0, ASANA-N10). */
    fun polygon(points: FloatArray, count: Int) {
        trace(path, points, count)
        scope.drawPath(path, color)
    }

    /** Рисунок [block] только внутри многоугольника [points] (0.14.0): доски и полосы створок не выходят за арку. */
    fun clipped(points: FloatArray, count: Int, block: () -> Unit) {
        trace(outline, points, count)
        scope.clipPath(outline) { block() }
    }

    private fun trace(into: Path, points: FloatArray, count: Int) {
        into.reset()
        for (i in 0 until count) if (i == 0) into.moveTo(points[0], -points[1]) else into.lineTo(points[i * 2], -points[i * 2 + 1])
        into.close()
    }

    /** A pie slice, [start] and [degrees] counter-clockwise as the pen measures them. */
    fun arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float) = scope.drawArc(color, -(start + degrees), degrees, true, Offset(x - radius, -y - radius), Size(radius * 2, radius * 2))
}
