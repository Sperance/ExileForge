package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.ExpeditionMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/**
 * Тела в скале карты (4.6.6): герой и монстр - круг, непроходимая клетка (скала, пропасть, вода) - квадрат, чьи выпуклые углы
 * скруглены на [corner] клетки, как их рисует сцена; без этого герой цеплялся за невидимый угол. Центр тела всегда стоит на
 * проходимой клетке - сетка путей, света и стай не меняется.
 */
internal class WallBody(private val map: ExpeditionMap, private val corner: Double) {
    /** Помещается ли круг радиуса [radius] с центром в ([x], [y]). */
    fun free(x: Double, y: Double, radius: Double): Boolean {
        if (!map.walkable(floor(x).toInt(), floor(y).toInt())) return false
        return nearest(x, y, radius)?.let { it.distance >= radius } ?: true
    }

    /**
     * Шаг ([dx], [dy]) из ([x], [y]): целиком, если свободно; иначе - вдоль стены (составляющая шага по касательной), иначе - по
     * осям порознь, как прежде. Так круг обтекает скруглённый угол, а не упирается в него.
     */
    fun slide(x: Double, y: Double, dx: Double, dy: Double, radius: Double): Pair<Double, Double> {
        if (free(x + dx, y + dy, radius)) return x + dx to y + dy
        nearest(x + dx, y + dy, radius)?.let { hit ->
            val along = dx * hit.nx + dy * hit.ny
            val tx = dx - along * hit.nx
            val ty = dy - along * hit.ny
            if (hypot(tx, ty) > EPSILON && free(x + tx, y + ty, radius)) return x + tx to y + ty
        }
        val nx = if (free(x + dx, y, radius)) x + dx else x
        val ny = if (free(nx, y + dy, radius)) y + dy else y
        return nx to ny
    }

    /** Ближайшая к точке преграда в пределах [reach]: расстояние до неё и нормаль от неё к точке. */
    private fun nearest(x: Double, y: Double, reach: Double): Hit? {
        val span = ceil(reach).toInt() + 1
        val cx = floor(x).toInt()
        val cy = floor(y).toInt()
        var best: Hit? = null
        for (j in cy - span..cy + span) {
            for (i in cx - span..cx + span) {
                if (map.walkable(i, j)) continue
                val hit = distance(x, y, i, j)
                if (best == null || hit.distance < best.distance) best = hit
            }
        }
        return best
    }

    /** Расстояние от точки до клетки [i], [j] со скруглёнными выпуклыми углами. */
    private fun distance(x: Double, y: Double, i: Int, j: Int): Hit {
        for (sx in 0..1) {
            for (sy in 0..1) {
                // Угол выпуклый, если обе соседние по сторонам клетки у него открыты
                val side = i + sx * 2 - 1
                val row = j + sy * 2 - 1
                if (corner <= 0 || !map.walkable(side, j) || !map.walkable(i, row)) continue
                val ox = i + sx + (1 - 2 * sx) * corner
                val oy = j + sy + (1 - 2 * sy) * corner
                val outX = if (sx == 0) x < ox else x > ox
                val outY = if (sy == 0) y < oy else y > oy
                if (outX && outY) return hit(x - ox, y - oy, -corner)
            }
        }
        val qx = x.coerceIn(i.toDouble(), i + 1.0)
        val qy = y.coerceIn(j.toDouble(), j + 1.0)
        return hit(x - qx, y - qy, 0.0)
    }

    private fun hit(dx: Double, dy: Double, shift: Double): Hit {
        val length = hypot(dx, dy)
        return if (length < EPSILON) Hit(0.0 + shift.coerceAtLeast(0.0), 0.0, 0.0) else Hit(length + shift, dx / length, dy / length)
    }

    /** Преграда: [distance] - до её края, ([nx], [ny]) - нормаль от неё к точке. */
    private class Hit(val distance: Double, val nx: Double, val ny: Double)

    private companion object {
        const val EPSILON = 1e-9
    }
}
