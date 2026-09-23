package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

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

    /** A pie slice, [start] and [degrees] counter-clockwise as the pen measures them. */
    fun arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float) =
        scope.drawArc(color, -(start + degrees), degrees, true, Offset(x - radius, -y - radius), Size(radius * 2, radius * 2))
}
