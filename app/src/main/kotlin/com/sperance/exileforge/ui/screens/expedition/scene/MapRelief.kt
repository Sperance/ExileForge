package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile

/**
 * Рельеф карты для сцены (4.2.0): что зависит только от клеток карты и потому считается раз на карту и её правку
 * ([ExpeditionMap.revision]), а не каждый кадр.
 *
 * - Массивы скалы: связные (по сторонам и диагоналям - контур стены сливает и их) клетки стены получают одну высоту [rise] -
 *   крышка массива ровная, без ступенек и швов.
 * - Контуры вод и пропастей ([outline]): соседние клетки - одна гладь со скруглёнными углами (marching squares по центрам клеток,
 *   затем сглаживание квадратичными кривыми); контур отступает от суши на [SHORE] клетки - там виден берег, и только снаружи.
 */
internal class MapRelief private constructor(private val map: ExpeditionMap) {
    private val revision = map.revision
    private val masses = IntArray(map.width * map.height) { -1 }
    private val rises = mutableListOf<Float>()
    private val loops = HashMap<Tile, List<FloatArray>>()
    private val paths = HashMap<Tile, Pair<Float, Path>>()

    init {
        // Массивы обходом в ширину: номер массива - его первая клетка по порядку, от неё и высота
        val queue = ArrayDeque<Int>()
        for (start in masses.indices) {
            if (masses[start] >= 0 || !rock(start % map.width, start / map.width)) continue
            val id = rises.size
            rises += cellNoise(start % map.width, start / map.width, MASS_SALT)
            masses[start] = id
            queue += start
            while (queue.isNotEmpty()) {
                val at = queue.removeFirst()
                val x = at % map.width
                val y = at / map.width
                for ((dx, dy) in AROUND) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until map.width || ny !in 0 until map.height || !rock(nx, ny)) continue
                    val next = ny * map.width + nx
                    if (masses[next] < 0) {
                        masses[next] = id
                        queue += next
                    }
                }
            }
        }
    }

    /** Рельеф всё ещё этой карты в этой правке. */
    fun fits(other: ExpeditionMap) = other === map && other.revision == revision

    /** Высота массива скалы клетки в `[0, 1)`: одна на весь массив; за краем карты - нуль. */
    fun rise(x: Int, y: Int): Float = if (x in 0 until map.width && y in 0 until map.height) masses[y * map.width + x].takeIf { it >= 0 }?.let(rises::get) ?: 0f else 0f

    /**
     * Контур клеток [tile] в координатах холста сцены при полуширине клетки [unit] (до сдвига камеры): заливка - гладь, вне
     * её - берег. Путь держится, пока не сменился [unit].
     */
    fun outline(tile: Tile, unit: Float): Path {
        paths[tile]?.takeIf { it.first == unit }?.let { return it.second }
        val path = Path().apply { fillType = PathFillType.EvenOdd }
        loops.getOrPut(tile) { trace(tile) }.forEach { loop -> smooth(path, loop, unit) }
        paths[tile] = unit to path
        return path
    }

    private fun rock(x: Int, y: Int) = map.tile(x, y) == Tile.WALL

    /**
     * Петли границы клеток [tile] в клетках карты (пары x, y). Квадрат marching squares - между центрами четырёх соседних клеток;
     * его отрезки границы ориентированы одинаково (внутри - слева), так что петли собираются по концам отрезков. Седло
     * (вода по диагонали) считается связным - диагональные клетки сливаются в одну гладь.
     */
    private fun trace(tile: Tile): List<FloatArray> {
        fun inside(x: Int, y: Int) = map.tile(x, y) == tile && x in 0 until map.width && y in 0 until map.height
        // Отрезок: из точки на ребре в точку на ребре. Ребро - пара соседних центров, ключ - удвоенная середина
        val next = HashMap<Long, Long>()
        val points = HashMap<Long, Pair<Float, Float>>()
        for (j in -1 until map.height) {
            for (i in -1 until map.width) {
                val corners = listOf(i to j, i + 1 to j, i + 1 to j + 1, i to j + 1)
                val ins = corners.map { (x, y) -> inside(x, y) }
                if (ins.none { it } || ins.all { it }) continue
                // Многоугольник квадрата по ходу углов: угол внутри - сам угол, смена на ребре - точка ребра
                val ring = mutableListOf<Long?>()
                for (k in 0 until 4) {
                    if (ins[k]) ring += null
                    val n = (k + 1) % 4
                    if (ins[k] != ins[n]) {
                        val (a, b) = if (ins[k]) corners[k] to corners[n] else corners[n] to corners[k]
                        val key = edgeKey(a, b)
                        // Точка ребра - на [SHORE] от центра клетки внутри к центру клетки снаружи
                        points[key] = (a.first + .5f + (b.first - a.first) * SHORE) to (a.second + .5f + (b.second - a.second) * SHORE)
                        ring += key
                    }
                }
                // Соседние в кольце точки рёбер - отрезок границы
                for (k in ring.indices) {
                    val from = ring[k] ?: continue
                    val to = ring[(k + 1) % ring.size] ?: continue
                    next[from] = to
                }
            }
        }
        val loops = mutableListOf<FloatArray>()
        val left = HashSet(next.keys)
        while (left.isNotEmpty()) {
            val first = left.first()
            val loop = mutableListOf<Float>()
            var at: Long? = first
            while (at != null && left.remove(at)) {
                val (x, y) = points.getValue(at)
                loop += x
                loop += y
                at = next[at]
            }
            if (loop.size >= 6) loops += loop.toFloatArray()
        }
        return loops
    }

    /** Ключ ребра между центрами клеток [a] и [b]: удвоенная середина, одна на ребро с обеих сторон. */
    private fun edgeKey(a: Pair<Int, Int>, b: Pair<Int, Int>): Long {
        val x = a.first + b.first + 2L
        val y = a.second + b.second + 2L
        return (x shl 32) or y
    }

    /** Петля [loop] гладкой кривой: через середины соседних точек, точки - опорные; клетка карты переводится в холст сцены. */
    private fun smooth(path: Path, loop: FloatArray, unit: Float) {
        val n = loop.size / 2
        fun sx(i: Int) = (loop[(i % n) * 2] - loop[(i % n) * 2 + 1]) * unit
        fun sy(i: Int) = (loop[(i % n) * 2] + loop[(i % n) * 2 + 1]) * unit / 2
        path.moveTo((sx(0) + sx(1)) / 2, (sy(0) + sy(1)) / 2)
        for (i in 1..n) path.quadraticTo(sx(i), sy(i), (sx(i) + sx(i + 1)) / 2, (sy(i) + sy(i + 1)) / 2)
        path.close()
    }

    companion object {
        /** Рельеф [map]: прежний [last], если карта та же и не правилась, иначе новый. */
        fun of(map: ExpeditionMap, last: MapRelief?): MapRelief = last?.takeIf { it.fits(map) } ?: MapRelief(map)
    }
}

/** Отступ глади от суши (4.2.0): доля клетки от центра клетки воды к соседу-суше, где проходит контур; берег - остальное до края. */
private const val SHORE = .32f

private const val MASS_SALT = 61

/** Восемь соседей клетки: массив скалы связен и по диагонали. */
private val AROUND = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to -1, 1 to -1, -1 to 1)

/** Стабильное значение в `[0, 1)` на клетку и [salt]. */
internal fun cellNoise(x: Int, y: Int, salt: Int = 0): Float {
    var h = x * 374761393 + y * 668265263 + salt * 1442695041
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) ushr 8) / 16777216f
}
