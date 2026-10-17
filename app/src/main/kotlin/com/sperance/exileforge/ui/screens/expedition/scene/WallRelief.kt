package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.WallField
import com.sperance.exileforge.core.campaign.WallGrain
import com.sperance.exileforge.core.campaign.cellNoise
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Скала карты контурами: каждый массив - одна изолиния сглаженного поля ([WallField]), выдавленная на высоту массива в
 * изометрии. Считается раз на карту, её правку, стиль, палитру и масштаб; кадр только заливает готовые пути.
 *
 * Для сортировки по глубине массив нарезан по клеткам карты ([WallPiece]): кусок клетки рисуется с глубиной клетки, как
 * блок, так что жетоны героя и монстров встают перед скалой и за ней верно, а соседние куски сходятся в одну фигуру без
 * швов (края кусков на границе клетки заходят к соседу на [SEAL]).
 */
internal class WallRelief private constructor(
    private val map: ExpeditionMap,
    private val relief: MapRelief,
    private val style: MapStyle,
    private val palette: Palette,
    private val unit: Float,
    private val field: WallField,
    texture: WallTexture,
) {
    /** Тона слоёв стены стиля в палитре биома. */
    val tones: WallTones = style.wallTones(palette)
    private val cells = arrayOfNulls<WallCell>(map.width * map.height)

    init {
        val drafts = arrayOfNulls<Draft>(map.width * map.height)
        fun draft(x: Int, y: Int) = drafts[y * map.width + x] ?: Draft(x, y, unit * style.wallHeight(riseNear(x, y))).also { drafts[y * map.width + x] = it }
        field.trace(WallField.ROCK) { draft(it.cellX, it.cellY).rock(it) }
        field.trace(WallField.PLATE) { if (map.tile(it.cellX, it.cellY) == Tile.WALL) draft(it.cellX, it.cellY).plate(it) }
        field.trace(WallField.AO_FAR, shift = 1) { draft(it.cellX, it.cellY).shadow(it, near = false) }
        field.trace(WallField.AO_NEAR, shift = 1) { draft(it.cellX, it.cellY).shadow(it, near = true) }
        drafts.forEachIndexed { index, draft -> cells[index] = draft?.finish(texture) }
    }

    /** Что лежит в клетке: подложка, тень, кусок массива; `null` - ничего. */
    fun cell(x: Int, y: Int): WallCell? = if (x in 0 until map.width && y in 0 until map.height) cells[y * map.width + x] else null

    /**
     * Глубокая скала (стена не каймы): её клетку игрок обычно не видел, но подложка и край массива каймы заходят в неё - они
     * видны вместе с соседом.
     */
    fun deep(x: Int, y: Int) = map.tile(x, y) == Tile.WALL && !field.rim(x, y)

    /** Высота массива у клетки [x], [y] в клетках сцены: дверь и рычаг встают в рост своей стены. */
    fun height(x: Int, y: Int): Float = style.wallHeight(riseNear(x, y))

    private fun fits(map: ExpeditionMap, field: WallField, relief: MapRelief, style: MapStyle, palette: Palette, unit: Float) = this.map === map && this.field === field && this.relief === relief && this.style === style && this.palette === palette && this.unit == unit

    /** Высота массива клетки: своя у стены, у выступа на соседней клетке - ближайшей стены каймы (массивы 8-связны). */
    private fun riseNear(x: Int, y: Int): Float {
        if (map.tile(x, y) == Tile.WALL) return relief.rise(x, y)
        return NEAR.firstOrNull { (dx, dy) -> field.rim(x + dx, y + dy) }?.let { (dx, dy) -> relief.rise(x + dx, y + dy) } ?: 0f
    }

    /** Заготовка клетки: пути копятся по квадратам сетки, кусок массива собирается в конце. */
    private inner class Draft(val x: Int, val y: Int, val height: Float) {
        var plate: Path? = null
        var far: Path? = null
        var near: Path? = null
        val cap = Path()
        val edges = ArrayList<FrontEdge>()
        var slope: Path? = null
        var area = 0f
        var sumX = 0f
        var sumY = 0f

        fun rock(contour: WallField.Contour) {
            polygon(cap, contour, height, seal = true)
            val n = contour.size
            var twice = 0f
            var gx = 0f
            var gy = 0f
            for (k in 0 until n) {
                val m = (k + 1) % n
                val cross = contour.xs[k] * contour.ys[m] - contour.xs[m] * contour.ys[k]
                twice += cross
                gx += (contour.xs[k] + contour.xs[m]) * cross
                gy += (contour.ys[k] + contour.ys[m]) * cross
                if (!contour.crossing[k] || !contour.crossing[m]) continue
                val dx = contour.xs[m] - contour.xs[k]
                val dy = contour.ys[m] - contour.ys[k]
                val length = hypot(dx, dy)
                if (length < 1e-4f) continue
                if (dy - dx < -length * FRONT) {
                    back(sealX(contour.xs[k]), sealY(contour.ys[k]), sealX(contour.xs[m]), sealY(contour.ys[m]), dy / length, -dx / length)
                    continue
                }
                if (dy - dx <= length * FRONT) continue
                edges += FrontEdge(sealX(contour.xs[k]), sealY(contour.ys[k]), sealX(contour.xs[m]), sealY(contour.ys[m]), dy / length, -dx / length)
            }
            if (abs(twice) > 1e-6f) {
                area += twice / 2
                sumX += gx / 6
                sumY += gy / 6
            }
        }

        /**
         * Дальняя от камеры кромка массива - скат: полоса крышки от кромки вглубь на [SLOPE_DEPTH] клетки, у кромки
         * ниже на долю высоты [SLOPE_DROP] - верх массива скругляется вниз, а не обрывается плоским краем; кадр кладёт её сумраком.
         */
        private fun back(ax: Float, ay: Float, bx: Float, by: Float, nx: Float, ny: Float) {
            val path = slope ?: Path().also { slope = it }
            val low = height * (1 - SLOPE_DROP)
            fun px(mx: Float, my: Float) = (mx - my) * unit
            fun py(mx: Float, my: Float, z: Float) = (mx + my) * unit / 2 - z
            path.moveTo(px(ax, ay), py(ax, ay, low))
            path.lineTo(px(bx, by), py(bx, by, low))
            path.lineTo(px(bx - nx * SLOPE_DEPTH, by - ny * SLOPE_DEPTH), py(bx - nx * SLOPE_DEPTH, by - ny * SLOPE_DEPTH, height))
            path.lineTo(px(ax - nx * SLOPE_DEPTH, ay - ny * SLOPE_DEPTH), py(ax - nx * SLOPE_DEPTH, ay - ny * SLOPE_DEPTH, height))
            path.close()
        }

        fun plate(contour: WallField.Contour) = polygon(plate ?: Path().also { plate = it }, contour, 0f, seal = true)

        fun shadow(contour: WallField.Contour, near: Boolean) {
            val path = if (near) this.near ?: Path().also { this.near = it } else far ?: Path().also { far = it }
            polygon(path, contour, 0f, seal = false)
        }

        fun finish(texture: WallTexture): WallCell? {
            val piece = if (abs(area) > MIN_AREA) piece(texture) else null
            if (piece == null && plate == null && far == null && near == null) return null
            return WallCell(plate, far, near, piece)
        }

        private fun piece(texture: WallTexture): WallPiece {
            val depth = if (map.tile(x, y) == Tile.WALL) x + y + 1.0 else (sumX + sumY).toDouble() / area
            val sketch = WallSketch(x, y, height, unit, edges, field, map)
            with(texture) {
                sketch.face()
                sketch.cap()
                sketch.foot()
            }
            return WallPiece(depth, cap, sketch.faces(), sketch.dusk(), sketch.rim(), sketch.marks(WallLayer.FACE), sketch.marks(WallLayer.CAP), sketch.marks(WallLayer.FOOT), slope)
        }

        /** Многоугольник квадрата в холст на высоте [z]; [seal] выносит вершины на границе клетки к соседу. */
        private fun polygon(path: Path, contour: WallField.Contour, z: Float, seal: Boolean) {
            for (k in 0 until contour.size) {
                val mx = if (seal) sealX(contour.xs[k]) else contour.xs[k]
                val my = if (seal) sealY(contour.ys[k]) else contour.ys[k]
                val px = (mx - my) * unit
                val py = (mx + my) * unit / 2 - z
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }

        private fun sealX(mx: Float) = seal(mx, x)
        private fun sealY(my: Float) = seal(my, y)
    }

    companion object {
        /**
         * Рельеф стен [map] стиля [style] по полю скалы ядра [field] (то же, во что упираются тела) узором зерна [grain]: прежний
         * [last], если всё то же, иначе новый.
         */
        fun of(map: ExpeditionMap, field: WallField, grain: WallGrain, relief: MapRelief, style: MapStyle, palette: Palette, unit: Float, last: WallRelief?): WallRelief = last?.takeIf { it.fits(map, field, relief, style, palette, unit) } ?: WallRelief(map, relief, style, palette, unit, field, grain.texture)

        private fun seal(value: Float, cell: Int) = when {
            abs(value - cell) < 1e-4f -> value - SEAL
            abs(value - cell - 1) < 1e-4f -> value + SEAL
            else -> value
        }

        /** Соседи клетки выступа: сперва по сторонам, затем по диагонали. */
        private val NEAR = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to -1, 1 to -1, -1 to 1)
    }
}

/** Заход края куска за границу клетки к соседу, в клетках: соседние куски перекрываются и не светят щелью сглаживания. */
private const val SEAL = .012f

/** Грань смотрит к камере, если её нормаль хоть немного глядит в `+x+y` (доля длины отрезка). */
private const val FRONT = .02f

/** Скат дальней кромки: насколько вглубь массива, в клетках, и на какую долю высоты ниже у самой кромки. */
private const val SLOPE_DEPTH = .35f
private const val SLOPE_DROP = .35f

/** Кусок меньше этой площади (в клетках) - пыль сглаживания, не рисуется. */
private const val MIN_AREA = .002f

/** Отрезок подножия, видимый камере, в клетках карты: от ([ax], [ay]) к ([bx], [by]), нормаль наружу ([nx], [ny]). */
internal class FrontEdge(val ax: Float, val ay: Float, val bx: Float, val by: Float, val nx: Float, val ny: Float) {
    /** Тень грани по её повороту: `0` - смотрит в `+y` (освещённая левая), `1` - в `+x` (теневая правая). */
    val shade: Float get() = (.5f + .5f * (nx - ny) / (abs(nx) + abs(ny))).coerceIn(0f, 1f)

    /** Положение на экране поперёк взгляда (`x - y`) в начале и конце: по нему швы и корни идут ровно по экрану. */
    val u0: Float get() = ax - ay
    val u1: Float get() = bx - by
}

/** Что лежит в клетке карты: подложка земли под скалой, мягкая тень у подножия (дальняя и ближняя), кусок массива. */
internal class WallCell(val plate: Path?, val far: Path?, val near: Path?, val piece: WallPiece?)

/**
 * Кусок массива в клетке: [depth] - глубина для сортировки с жетонами, [cap] - крышка, [faces] - видимые грани (нулевой путь
 * - все грани, остальные - доли тени по повороту), [dusk] - сумрак к подножию, [rim] - фаска по кромке, метки текстуры слоями,
 * [slope] - скат дальней кромки.
 */
internal class WallPiece(
    val depth: Double,
    val cap: Path,
    val faces: Array<Path?>,
    val dusk: Brush?,
    val rim: Path?,
    val faceMarks: Array<WallMark>,
    val capMarks: Array<WallMark>,
    val footMarks: Array<WallMark>,
    /** Скат дальней кромки: сумрак поверх крышки у края, что смотрит от камеры; нет такого края - null. */
    val slope: Path? = null,
)

/** Метка текстуры: путь одной краской. */
internal class WallMark(val ink: WallInk, val path: Path)

/** Слой метки: на гранях (до крышки), на крышке (после неё и фаски), у подножия (поверх всего куска). */
internal enum class WallLayer { FACE, CAP, FOOT }

/** Краска метки; порядок - порядок заливки внутри слоя: сперва тени, затем камень, поросль и блики. */
internal enum class WallInk { DIM, SHADOW, SHEEN, ROOT, STONE, GROWTH, LIGHT, BLOOM }

/**
 * Тона слоёв стены: [face] - освещённая грань, [cap] - крышка, [rim] - фаска, [ground] - земля под скалой, [light] - сколы и
 * прожилки, [growth] и [bloom] - мох или листва и их блики, [root] - корни, [stone] - камни у подножия.
 */
internal class WallTones(
    val face: Color,
    val cap: Color,
    val rim: Color,
    val ground: Color,
    val light: Color,
    val growth: Color,
    val bloom: Color,
    val root: Color,
    val stone: Color,
) {
    companion object {
        private val earth = Color(0xFF4A3624)

        /** Тона из палитры биома: грань и камни - бока скалы, крышка и фаска - её верх, поросль - цвет декора. */
        fun of(palette: Palette) = WallTones(
            face = tone(palette.wallSide, 1.2f),
            cap = tone(palette.wallTop, 1.05f),
            rim = tone(palette.wallTop, 1.6f),
            ground = tone(palette.floor, .8f),
            light = tone(palette.wallTop, 1.45f),
            growth = tone(palette.decor, .75f),
            bloom = tone(palette.decor, 1.25f),
            root = earth,
            stone = tone(palette.wallSide, 1.5f),
        )
    }
}
