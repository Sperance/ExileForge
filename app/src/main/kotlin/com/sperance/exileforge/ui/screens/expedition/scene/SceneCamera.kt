package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.sperance.exileforge.core.campaign.run.IsoGrid
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * Камера сцены похода (0.14.4, #31/#18): одна изометрия карты на экран ([IsoGrid] ядра, 0.14.5, #49 - её же читает стик) - x клеток
 * бежит вниз-вправо, y вниз-влево, клетка вдвое шире, чем выше, - и место героя чуть ниже середины экрана. По ней рисует художник ([ScenePainter]), касание находит
 * клетку ([toWorld]), слои берут окрестность экрана ([window]), а мини-карта и полная карта кладут ту же изометрию своим
 * масштабом ([MapProjection]) - направление «вверх» на них то же, что на сцене.
 */
internal object SceneCamera {
    /** Доля высоты экрана, на которой стоит герой: впереди больше карты, чем позади (3.70.0). */
    const val HERO_DOWN = .55f

    /** Сдвиг точки карты ([x], [y]) по экрану вправо, в пикселях при полуширине клетки [unit]. */
    fun across(x: Double, y: Double, unit: Float): Float = (IsoGrid.across(x, y) * unit).toFloat()

    /** Сдвиг точки карты ([x], [y]) по экрану вниз, в пикселях при полуширине клетки [unit]. */
    fun down(x: Double, y: Double, unit: Float): Float = (IsoGrid.down(x, y) * unit).toFloat()

    /** Точка экрана [size], где стоит герой. */
    fun hero(size: Size, unit: Float): Offset = Offset(size.width / 2, size.height * HERO_DOWN + unit)

    /** Точка экрана [at] сцены [size] - точка карты (обратная камера), когда герой стоит в ([hx], [hy]). */
    fun toWorld(hx: Double, hy: Double, at: Offset, size: Size, unit: Float): Pair<Double, Double> {
        val hero = hero(size, unit)
        return inverse(at - hero, unit).let { (dx, dy) -> hx + dx to hy + dy }
    }

    /**
     * Клетки карты, что видит экран [size], сдвигами от клетки героя (0.14.4, #31): рамка под четырьмя углами экрана, сверху
     * шире на [rise] клеток - скала встаёт над своей клеткой, - и со всех сторон на [slack] клеток - герой ходит внутри клетки
     * до перезаписи слоёв. Прежний квадрат «половина длинной стороны» не накрывал углов экрана на высоких и широких телефонах.
     */
    fun window(size: Size, unit: Float, rise: Int, slack: Int): CellWindow {
        val hero = hero(size, unit)
        val corners = listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height))
            .map { inverse(it - hero, unit) }
        // Герой стоит где угодно внутри своей клетки: сдвиги от её угла - на клетку шире
        val xs = floor(corners.minOf { it.first }).toInt() - rise - slack..ceil(corners.maxOf { it.first }).toInt() + slack + 1
        val ys = floor(corners.minOf { it.second }).toInt() - rise - slack..ceil(corners.maxOf { it.second }).toInt() + slack + 1
        return CellWindow(xs, ys)
    }

    /** Сдвиг экрана [shift] в пикселях при полуширине клетки [unit] - сдвиг по карте ([IsoGrid.inverse]). */
    internal fun inverse(shift: Offset, unit: Float): Pair<Double, Double> = IsoGrid.inverse((shift.x / unit).toDouble(), (shift.y / unit).toDouble())
}

/** Окрестность героя, что видна на экране: сдвиги клеток по x и y от его клетки ([SceneCamera.window]). */
internal data class CellWindow(val xs: IntRange, val ys: IntRange) {
    /** Клетки карты [width]×[height] в этой окрестности героя на клетке ([hx], [hy]). */
    fun around(hx: Int, hy: Int, width: Int, height: Int): Pair<IntRange, IntRange> = (hx + xs.first).coerceAtLeast(0)..(hx + xs.last).coerceAtMost(width - 1) to (hy + ys.first).coerceAtLeast(0)..(hy + ys.last).coerceAtMost(height - 1)
}

/**
 * Изометрия карты на холст мини-карты и полной карты (0.14.4, #18): та же, что у сцены ([SceneCamera]), полуширина клетки
 * [unit] своя, начало карты - в точке холста [origin]. Клетка - ромб, метки - точки: «вверх» на карте - «вверх» на сцене.
 */
internal class MapProjection(private val origin: Offset, val unit: Float) {
    /** Точка карты ([x], [y]) на холсте. */
    fun at(x: Double, y: Double): Offset = Offset(origin.x + SceneCamera.across(x, y, unit), origin.y + SceneCamera.down(x, y, unit))

    /** Клетки карты [width]×[height], чьи ромбы задевают холст [size]: рамка под его углами с клеткой запаса. */
    fun cells(size: Size, width: Int, height: Int): Pair<IntRange, IntRange> {
        val corners = listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height))
            .map { SceneCamera.inverse(it - origin, unit) }
        val xs = (floor(corners.minOf { it.first }).toInt() - 1).coerceAtLeast(0)..(ceil(corners.maxOf { it.first }).toInt() + 1).coerceAtMost(width - 1)
        val ys = (floor(corners.minOf { it.second }).toInt() - 1).coerceAtLeast(0)..(ceil(corners.maxOf { it.second }).toInt() + 1).coerceAtMost(height - 1)
        return xs to ys
    }

    companion object {
        /** Герой в ([hx], [hy]) - в середине холста [size]. */
        fun centred(hx: Double, hy: Double, size: Size, unit: Float) = MapProjection(Offset(size.width / 2 - SceneCamera.across(hx, hy, unit), size.height / 2 - SceneCamera.down(hx, hy, unit)), unit)

        /** Вся карта [width]×[height] клеток вписана в холст [size]: ромб карты вдвое шире своей высоты. */
        fun fitted(width: Int, height: Int, size: Size): MapProjection {
            val span = (width + height).coerceAtLeast(1)
            val unit = min(size.width / span, size.height * 2 / span)
            return MapProjection(Offset((size.width - span * unit) / 2 + height * unit, (size.height - span * unit / 2) / 2), unit)
        }

        /** Сторона холста полной карты: ромб карты вдвое шире своей высоты. */
        const val FITTED_ASPECT = 2f
    }
}
