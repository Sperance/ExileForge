package com.sperance.exileforge.core.campaign

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Береговые контуры карты - один источник и для сцены, и для тел: соседние клетки пропасти или воды - одна гладь со
 * скруглёнными углами. Marching squares по центрам клеток (седло - вода по диагонали - связно), контур отступает от суши на
 * [shore] клетки (`expedition.shore` правил), затем сглаживание квадратичными кривыми через середины соседних точек, разбитыми
 * на [STEPS] отрезков. Сцена заливает эти петли, тела упираются в них ([nearest], [inside]) - как нарисовано, так и упирается.
 * Строится раз на карту и её правку ([ExpeditionMap.revision]), в кадре - только чтение.
 */
class Shoreline private constructor(private val map: ExpeditionMap, private val shore: Double) {
    private val revision = map.revision

    /** Гладкие петли по виду клеток: точки (x, y) подряд в клетках карты, петля замкнута. */
    private val loops: Map<Tile, List<DoubleArray>> = LIQUIDS.associateWith { tile -> trace(tile).map(::smooth) }

    /** Петли с рамками - для тел: петля, чья рамка далеко от точки, не перебирается. */
    private val bounded: List<Bounded> = loops.values.flatten().map(::Bounded)

    /** Петли клеток [tile] в клетках карты: (x, y) подряд, замкнуты; пусто - таких клеток нет. */
    fun loops(tile: Tile): List<DoubleArray> = loops[tile].orEmpty()

    /** Контур всё ещё этой карты в этой правке и при том же отступе. */
    fun fits(other: ExpeditionMap, shore: Double) = other === map && other.revision == revision && shore == this.shore

    /** Точка ([x], [y]) внутри глади (чёт-нечет по всем петлям). */
    fun inside(x: Double, y: Double): Boolean = bounded.count { it.contains(x, y) && it.crossings(x, y) % 2 == 1 } % 2 == 1

    /** Ближайший к точке край глади в пределах [reach]: расстояние до него и нормаль от края к точке; null - края рядом нет. */
    fun nearest(x: Double, y: Double, reach: Double): Edge? {
        var best: Edge? = null
        for (loop in bounded) {
            if (!loop.near(x, y, reach)) continue
            val points = loop.points
            val n = points.size / 2
            for (i in 0 until n) {
                val ax = points[i * 2]
                val ay = points[i * 2 + 1]
                val bx = points[(i + 1) % n * 2]
                val by = points[(i + 1) % n * 2 + 1]
                val dx = bx - ax
                val dy = by - ay
                val length = dx * dx + dy * dy
                val t = if (length <= 0) 0.0 else (((x - ax) * dx + (y - ay) * dy) / length).coerceIn(0.0, 1.0)
                val ox = x - (ax + dx * t)
                val oy = y - (ay + dy * t)
                val distance = hypot(ox, oy)
                if (best != null && distance >= best.distance) continue
                best = if (distance < EPSILON) Edge(0.0, 0.0, 0.0) else Edge(distance, ox / distance, oy / distance)
            }
        }
        return best
    }

    /**
     * Петли границы клеток [tile] в клетках карты (пары x, y). Квадрат marching squares - между центрами четырёх соседних клеток;
     * его отрезки границы ориентированы одинаково (внутри - слева), так что петли собираются по концам отрезков.
     *
     * Отрезок идёт из точки на ребре в точку на ребре; ребро - пара соседних центров, ключ - удвоенная середина. Многоугольник квадрата -
     * по ходу углов: угол внутри - сам угол, смена на ребре - точка ребра на [shore] от центра клетки внутри к центру снаружи; соседние в
     * кольце точки рёбер - отрезок границы.
     */
    private fun trace(tile: Tile): List<DoubleArray> {
        fun inside(x: Int, y: Int) = x in 0 until map.width && y in 0 until map.height && map.tile(x, y) == tile
        val next = HashMap<Long, Long>()
        val points = HashMap<Long, Pair<Double, Double>>()
        for (j in -1 until map.height) {
            for (i in -1 until map.width) {
                val corners = listOf(i to j, i + 1 to j, i + 1 to j + 1, i to j + 1)
                val ins = corners.map { (x, y) -> inside(x, y) }
                if (ins.none { it } || ins.all { it }) continue
                val ring = mutableListOf<Long?>()
                for (k in 0 until 4) {
                    if (ins[k]) ring += null
                    val n = (k + 1) % 4
                    if (ins[k] != ins[n]) {
                        val (a, b) = if (ins[k]) corners[k] to corners[n] else corners[n] to corners[k]
                        val key = edgeKey(a, b)
                        points[key] = (a.first + .5 + (b.first - a.first) * shore) to (a.second + .5 + (b.second - a.second) * shore)
                        ring += key
                    }
                }
                for (k in ring.indices) {
                    val from = ring[k] ?: continue
                    val to = ring[(k + 1) % ring.size] ?: continue
                    next[from] = to
                }
            }
        }
        val loops = mutableListOf<DoubleArray>()
        val left = HashSet(next.keys)
        while (left.isNotEmpty()) {
            val first = left.first()
            val loop = mutableListOf<Double>()
            var at: Long? = first
            while (at != null && left.remove(at)) {
                val (x, y) = points.getValue(at)
                loop += x
                loop += y
                at = next[at]
            }
            if (loop.size >= 6) loops += loop.toDoubleArray()
        }
        return loops
    }

    /** Ключ ребра между центрами клеток [a] и [b]: удвоенная середина, одна на ребро с обеих сторон. */
    private fun edgeKey(a: Pair<Int, Int>, b: Pair<Int, Int>): Long {
        val x = a.first + b.first + 2L
        val y = a.second + b.second + 2L
        return (x shl 32) or y
    }

    /** Петля [loop] гладкой кривой: квадратичные кривые через середины соседних точек (точки - опорные), по [STEPS] отрезков. */
    private fun smooth(loop: DoubleArray): DoubleArray {
        val n = loop.size / 2
        fun px(i: Int) = loop[(i % n) * 2]
        fun py(i: Int) = loop[(i % n) * 2 + 1]
        val out = DoubleArray(n * STEPS * 2)
        var k = 0
        for (i in 1..n) {
            val sx = (px(i - 1) + px(i)) / 2
            val sy = (py(i - 1) + py(i)) / 2
            val ex = (px(i) + px(i + 1)) / 2
            val ey = (py(i) + py(i + 1)) / 2
            for (s in 0 until STEPS) {
                val t = s.toDouble() / STEPS
                val u = 1 - t
                out[k++] = u * u * sx + 2 * u * t * px(i) + t * t * ex
                out[k++] = u * u * sy + 2 * u * t * py(i) + t * t * ey
            }
        }
        return out
    }

    /** Край глади: [distance] - до него, ([nx], [ny]) - нормаль от него к точке. */
    class Edge(val distance: Double, val nx: Double, val ny: Double)

    /** Петля с рамкой. */
    private class Bounded(val points: DoubleArray) {
        private var left = Double.MAX_VALUE
        private var top = Double.MAX_VALUE
        private var right = -Double.MAX_VALUE
        private var bottom = -Double.MAX_VALUE

        init {
            for (i in points.indices step 2) {
                left = min(left, points[i])
                right = max(right, points[i])
                top = min(top, points[i + 1])
                bottom = max(bottom, points[i + 1])
            }
        }

        fun near(x: Double, y: Double, reach: Double) = x >= left - reach && x <= right + reach && y >= top - reach && y <= bottom + reach

        fun contains(x: Double, y: Double) = near(x, y, 0.0)

        /** Сколько рёбер петли пересекает луч из точки вправо. */
        fun crossings(x: Double, y: Double): Int {
            val n = points.size / 2
            var count = 0
            for (i in 0 until n) {
                val ax = points[i * 2]
                val ay = points[i * 2 + 1]
                val bx = points[(i + 1) % n * 2]
                val by = points[(i + 1) % n * 2 + 1]
                if ((ay > y) != (by > y) && x < ax + (y - ay) / (by - ay) * (bx - ax)) count++
            }
            return count
        }
    }

    companion object {
        /** Клетки, у которых гладь: пропасть и вода. */
        val LIQUIDS = listOf(Tile.CHASM, Tile.WATER)

        /** Отрезков на кривую сглаживания: глазу кривая, телу - ломаная, и это одна линия. */
        private const val STEPS = 4

        private const val EPSILON = 1e-9

        /** Контур [map] с отступом [shore]: прежний [last], если карта та же и не правилась, иначе новый. */
        fun of(map: ExpeditionMap, shore: Double, last: Shoreline?): Shoreline = last?.takeIf { it.fits(map, shore) } ?: Shoreline(map, shore)
    }
}
