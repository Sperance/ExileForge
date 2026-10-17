package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Shoreline
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.cellNoise

/**
 * Рельеф карты для сцены: что зависит только от клеток карты и потому считается раз на карту и её правку
 * ([ExpeditionMap.revision]), а не каждый кадр.
 *
 * - Массивы скалы: связные (по сторонам и диагоналям - контур стены сливает и их) клетки стены получают одну высоту [rise] -
 *   крышка массива ровная, без ступенек и швов.
 * - Контуры вод и пропастей ([outline]): соседние клетки - одна гладь со скруглёнными углами; контур строит
 *   [Shoreline] ядра (marching squares, сглаживание, отступ берега из правил) - одна линия для сцены и для тел.
 *
 * Массивы находятся обходом в ширину: номер массива - его первая клетка по порядку.
 */
internal class MapRelief private constructor(private val map: ExpeditionMap, private val shoreline: Shoreline) {
    private val revision = map.revision
    private val masses = IntArray(map.width * map.height) { -1 }
    private val rises = mutableListOf<Float>()
    private val paths = HashMap<Tile, Pair<Float, Path>>()

    init {
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

    /** Рельеф всё ещё этой карты в этой правке и с этим берегом. */
    fun fits(other: ExpeditionMap, shore: Shoreline) = other === map && other.revision == revision && shore === shoreline

    /** Высота массива скалы клетки в `[0, 1)`: одна на весь массив; за краем карты - нуль. */
    fun rise(x: Int, y: Int): Float = if (x in 0 until map.width && y in 0 until map.height) masses[y * map.width + x].takeIf { it >= 0 }?.let(rises::get) ?: 0f else 0f

    /**
     * Контур клеток [tile] в координатах холста сцены при полуширине клетки [unit] (до сдвига камеры): заливка - гладь, вне
     * её - берег. Петли [Shoreline] мира, те же, в которые упираются тела. Путь держится, пока не сменился [unit].
     */
    fun outline(tile: Tile, unit: Float): Path {
        paths[tile]?.takeIf { it.first == unit }?.let { return it.second }
        val path = Path().apply { fillType = PathFillType.EvenOdd }
        shoreline.loops(tile).forEach { loop -> trace(path, loop, unit) }
        paths[tile] = unit to path
        return path
    }

    private fun rock(x: Int, y: Int) = map.tile(x, y) == Tile.WALL

    /** Петля [loop] контура (клетки карты) в путь холста сцены: та же ломаная, в которую упираются тела. */
    private fun trace(path: Path, loop: DoubleArray, unit: Float) {
        val n = loop.size / 2
        fun sx(i: Int) = (loop[i * 2] - loop[i * 2 + 1]).toFloat() * unit
        fun sy(i: Int) = (loop[i * 2] + loop[i * 2 + 1]).toFloat() * unit / 2
        path.moveTo(sx(0), sy(0))
        for (i in 1 until n) path.lineTo(sx(i), sy(i))
        path.close()
    }

    companion object {
        /** Рельеф [map] с берегом [shoreline]: прежний [last], если карта та же и не правилась, иначе новый. */
        fun of(map: ExpeditionMap, shoreline: Shoreline, last: MapRelief?): MapRelief = last?.takeIf { it.fits(map, shoreline) } ?: MapRelief(map, shoreline)
    }
}

private const val MASS_SALT = 61

/** Восемь соседей клетки: массив скалы связен и по диагонали. */
private val AROUND = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to -1, 1 to -1, -1 to 1)
