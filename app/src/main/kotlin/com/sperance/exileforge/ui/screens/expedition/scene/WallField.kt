package com.sperance.exileforge.ui.screens.expedition.scene

import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import kotlin.math.exp
import kotlin.math.floor

/**
 * Шум края стены стиля: [amplitude] - насколько поле скалы гуляет вокруг порога, [scale] - сколько волн на клетку,
 * [rough] - доля второй, вдвое более частой октавы (рваный край у скалы, мягкие «кроны» у зарослей).
 */
internal class EdgeNoise(val amplitude: Float, val scale: Float, val rough: Float)

/**
 * Сглаженное поле скалы карты: сумма гауссиан («метаболы») по клеткам каймы - стенам, что касаются пола (8 соседей), - плюс
 * детерминированный шум края стиля [edge]. Поле считается раз на карту в узлах сетки по [STEPS] на клетку, так что каждый
 * квадрат marching squares лежит внутри одной клетки карты: изолинии режутся по клеткам без пересчёта.
 *
 * Изолиния поля на уровне [ROCK] - подножие массива; ниже - подложка земли ([PLATE]) и тень у подножия ([AO_FAR], [AO_NEAR]).
 */
internal class WallField(private val map: ExpeditionMap, private val edge: EdgeNoise, private val salt: Int) {
    val width = map.width
    val height = map.height
    private val cols = width * STEPS + 1
    private val rows = height * STEPS + 1
    private val rim = BooleanArray(width * height) { map.rimRock(it % width, it / width) }
    private val values = FloatArray(cols * rows)

    init {
        // Гауссиана разделима: вес клетки - произведение весов по осям, по доле шага внутри клетки и сдвигу до клетки
        val weights = Array(STEPS) { s -> FloatArray(REACH * 2 + 1) { k -> (s.toFloat() / STEPS - (k - REACH) - .5f).let { exp(-SPREAD * it * it) } } }
        for (j in 0 until rows) {
            val wy = weights[j % STEPS]
            val cy = j / STEPS
            for (i in 0 until cols) {
                val wx = weights[i % STEPS]
                val cx = i / STEPS
                var sum = 0f
                for (ky in 0..REACH * 2) {
                    val y = cy + ky - REACH
                    if (y !in 0 until height) continue
                    for (kx in 0..REACH * 2) {
                        val x = cx + kx - REACH
                        if (x in 0 until width && rim[y * width + x]) sum += wx[kx] * wy[ky]
                    }
                }
                values[j * cols + i] = sum + edge.amplitude * (fractal(i.toFloat() / STEPS, j.toFloat() / STEPS) - .5f) * 2f
            }
        }
    }

    /** Кайма ли клетка: из неё растёт стена. */
    fun rim(x: Int, y: Int) = x in 0 until width && y in 0 until height && rim[y * width + x]

    /** Значение поля в точке карты ([mx], [my] - в клетках): билинейно по узлам сетки. */
    fun value(mx: Float, my: Float): Float {
        val gx = (mx * STEPS).coerceIn(0f, cols - 1.001f)
        val gy = (my * STEPS).coerceIn(0f, rows - 1.001f)
        val i = gx.toInt()
        val j = gy.toInt()
        val fx = gx - i
        val fy = gy - j
        val top = at(i, j) + (at(i + 1, j) - at(i, j)) * fx
        val bottom = at(i, j + 1) + (at(i + 1, j + 1) - at(i, j + 1)) * fx
        return top + (bottom - top) * fy
    }

    /** Гладкий шум в `[0, 1)` в точке карты: значения по решётке, сглаженные кубически; [octave] сдвигает решётку. */
    fun smooth(mx: Float, my: Float, octave: Int = 0): Float {
        val x0 = floor(mx)
        val y0 = floor(my)
        val xi = x0.toInt()
        val yi = y0.toInt()
        val u = (mx - x0).let { it * it * (3 - 2 * it) }
        val v = (my - y0).let { it * it * (3 - 2 * it) }
        val s = salt + octave * OCTAVE_SALT
        val a = cellNoise(xi, yi, s)
        val b = cellNoise(xi + 1, yi, s)
        val c = cellNoise(xi, yi + 1, s)
        val d = cellNoise(xi + 1, yi + 1, s)
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v
    }

    private fun fractal(mx: Float, my: Float): Float = (1f - edge.rough) * smooth(mx * edge.scale, my * edge.scale) + edge.rough * smooth(mx * edge.scale * 2.3f, my * edge.scale * 2.3f, 1)

    private fun at(i: Int, j: Int): Float = values[j * cols + i]

    /**
     * Область поля выше [level] по клеткам карты (marching squares с линейной интерполяцией): каждый квадрат сетки отдаёт
     * многоугольник [Contour] своей клетки. Седло решается средним по квадрату. [shift] сдвигает поле на столько узлов к камере
     * (по `+x` и `+y`) - тень у подножия ложится вперёд.
     */
    fun trace(level: Float, shift: Int = 0, sink: (Contour) -> Unit) {
        val contour = Contour()
        val v = FloatArray(4)
        for (j in 0 until rows - 1) {
            for (i in 0 until cols - 1) {
                for (k in 0 until 4) {
                    val ci = i + CORNER_DX[k] - shift
                    val cj = j + CORNER_DY[k] - shift
                    v[k] = if (ci in 0 until cols && cj in 0 until rows) at(ci, cj) else 0f
                }
                val inside = (0 until 4).count { v[it] > level }
                if (inside == 0) continue
                contour.cellX = i / STEPS
                contour.cellY = j / STEPS
                val saddle = inside == 2 && (v[0] > level) == (v[2] > level)
                if (saddle && (v[0] + v[1] + v[2] + v[3]) / 4 <= level) {
                    // Две разные лапы по диагонали: каждый угол - свой треугольник
                    for (k in 0 until 4) {
                        if (v[k] <= level) continue
                        contour.clear()
                        contour.corner(i, j, k)
                        contour.cross(i, j, k, (k + 1) % 4, v, level)
                        contour.cross(i, j, (k + 3) % 4, k, v, level)
                        sink(contour)
                    }
                    continue
                }
                contour.clear()
                for (k in 0 until 4) {
                    val n = (k + 1) % 4
                    if (v[k] > level) contour.corner(i, j, k)
                    if ((v[k] > level) != (v[n] > level)) contour.cross(i, j, k, n, v, level)
                }
                sink(contour)
            }
        }
    }

    companion object {
        /** Узлов поля на клетку: квадрат сетки - четверть клетки. */
        const val STEPS = 4

        /** Порог подножия скалы: на прямом крае - чуть внутри клетки стены, одиночная клетка - столб в треть клетки. */
        const val ROCK = .8f

        /** Порог подложки земли под скалой: накрывает клетку каймы до её краёв и за массивом - плавной кромкой. */
        const val PLATE = .3f

        /** Пороги мягкой тени у подножия: дальний край и ближний, плотнее. */
        const val AO_FAR = .34f
        const val AO_NEAR = .58f

        private const val REACH = 2
        private const val SPREAD = 2.2f
        private const val OCTAVE_SALT = 7919

        /** Углы квадрата по ходу: левый верхний, правый верхний, правый нижний, левый нижний (y карты растёт вниз). */
        private val CORNER_DX = intArrayOf(0, 1, 1, 0)
        private val CORNER_DY = intArrayOf(0, 0, 1, 1)
    }

    /**
     * Многоугольник одного квадрата сетки в клетках карты: обход по часовой (внутри - справа по ходу), у каждой вершины - лежит
     * ли она на изолинии ([crossing]). Подряд идущие вершины изолинии - отрезок края с нормалью наружу `(dy, -dx)`.
     */
    class Contour {
        var cellX = 0
        var cellY = 0
        var size = 0
            private set
        val xs = FloatArray(MAX)
        val ys = FloatArray(MAX)
        val crossing = BooleanArray(MAX)

        internal fun clear() {
            size = 0
        }

        internal fun corner(i: Int, j: Int, k: Int) = add((i + CORNER_DX[k]).toFloat() / STEPS, (j + CORNER_DY[k]).toFloat() / STEPS, false)

        internal fun cross(i: Int, j: Int, a: Int, b: Int, v: FloatArray, level: Float) {
            val t = ((level - v[a]) / (v[b] - v[a])).coerceIn(0f, 1f)
            val ax = (i + CORNER_DX[a]).toFloat()
            val ay = (j + CORNER_DY[a]).toFloat()
            add((ax + (i + CORNER_DX[b] - ax) * t) / STEPS, (ay + (j + CORNER_DY[b] - ay) * t) / STEPS, true)
        }

        private fun add(x: Float, y: Float, edge: Boolean) {
            xs[size] = x
            ys[size] = y
            crossing[size] = edge
            size++
        }

        private companion object {
            const val MAX = 8
        }
    }
}

/** Кайма скалы: стена, у которой среди 8 соседей есть пол, - только она и видна на карте. */
internal fun ExpeditionMap.rimRock(x: Int, y: Int) = tile(x, y) == Tile.WALL && (-1..1).any { dy -> (-1..1).any { dx -> walkable(x + dx, y + dy) } }
