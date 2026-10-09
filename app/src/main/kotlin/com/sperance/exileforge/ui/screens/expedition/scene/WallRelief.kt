package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import com.sperance.exileforge.core.campaign.MapStyle as Layout

/**
 * Скала карты контурами: каждый массив - одна изолиния сглаженного поля ([WallField]), выдавленная на высоту массива в
 * изометрии. Считается раз на карту, её правку, стиль, палитру и масштаб; кадр только заливает готовые пути.
 *
 * Для сортировки по глубине массив нарезан по клеткам карты ([WallPiece]): кусок клетки рисуется с глубиной клетки, как прежний
 * блок, так что жетоны героя и монстров встают перед скалой и за ней как раньше, а соседние куски сходятся в одну фигуру без
 * швов (края кусков на границе клетки заходят к соседу на [SEAL]).
 */
internal class WallRelief private constructor(
    private val map: ExpeditionMap,
    private val relief: MapRelief,
    private val style: MapStyle,
    private val palette: Palette,
    private val unit: Float,
    texture: WallTexture,
) {
    /** Тона слоёв стены стиля в палитре биома. */
    val tones: WallTones = style.wallTones(palette)
    private val field = WallField(map, texture.edge, saltOf(map))
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

    private fun fits(map: ExpeditionMap, relief: MapRelief, style: MapStyle, palette: Palette, unit: Float) = this.map === map && this.relief === relief && this.style === style && this.palette === palette && this.unit == unit

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
                // Отрезок края: внутри - справа по ходу, наружу - (dy, -dx); к камере смотрит то, что глядит в +x+y
                val dx = contour.xs[m] - contour.xs[k]
                val dy = contour.ys[m] - contour.ys[k]
                val length = hypot(dx, dy)
                if (length < 1e-4f || dy - dx <= length * FRONT) continue
                edges += FrontEdge(sealX(contour.xs[k]), sealY(contour.ys[k]), sealX(contour.xs[m]), sealY(contour.ys[m]), dy / length, -dx / length)
            }
            if (abs(twice) > 1e-6f) {
                area += twice / 2
                sumX += gx / 6
                sumY += gy / 6
            }
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
            return WallPiece(depth, cap, sketch.faces(), sketch.dusk(), sketch.rim(), sketch.marks(WallLayer.FACE), sketch.marks(WallLayer.CAP), sketch.marks(WallLayer.FOOT))
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
        /** Рельеф стен [map] стиля [style]: прежний [last], если всё то же, иначе новый. */
        fun of(map: ExpeditionMap, relief: MapRelief, style: MapStyle, layout: Layout, palette: Palette, unit: Float, last: WallRelief?): WallRelief = last?.takeIf { it.fits(map, relief, style, palette, unit) } ?: WallRelief(map, relief, style, palette, unit, style.texture(layout))

        /** Соль шума края - от карты: та же карта - тот же край. */
        private fun saltOf(map: ExpeditionMap) = map.start.x * 73 + map.start.y * 151 + map.exit.x * 199 + map.exit.y * 263 + map.width

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
 * - все грани, остальные - доли тени по повороту), [dusk] - сумрак к подножию, [rim] - фаска по кромке, метки текстуры слоями.
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

/**
 * Набросок куска массива для узора стиля ([WallTexture]): клетка, высота, видимые отрезки подножия и поле, а метки
 * ложатся в пути по слою и краске. Координаты - в клетках карты; высота [height] и толщины - в пикселях.
 */
internal class WallSketch(
    val x: Int,
    val y: Int,
    val height: Float,
    val unit: Float,
    val edges: List<FrontEdge>,
    private val field: WallField,
    private val map: ExpeditionMap,
) {
    private val paths = HashMap<Int, Path>()

    /** Внутри крышки с запасом [margin] (в долях поля): метка крышки не вылезет за край. */
    fun inside(mx: Float, my: Float, margin: Float = 0f) = field.value(mx, my) > WallField.ROCK + margin

    /** Точка у подножия на полу: не в скале и на проходимой клетке. */
    fun ground(mx: Float, my: Float) = field.value(mx, my) < WallField.ROCK && map.walkable(floor(mx).toInt(), floor(my).toInt())

    /** Стабильное значение в `[0, 1)` для клетки и [salt]. */
    fun hash(salt: Int, extra: Int = 0) = cellNoise(x * 31 + extra, y, salt)

    /** Гладкий шум карты: пятна мха, волна пластов. */
    fun smooth(mx: Float, my: Float, octave: Int) = field.smooth(mx, my, octave + 3)

    /** Точка грани над отрезком [edge]: [s] - доля пути вдоль него, [z] - высота над землёй в пикселях. */
    fun faceX(edge: FrontEdge, s: Float) = screenX(edge.ax + (edge.bx - edge.ax) * s, edge.ay + (edge.by - edge.ay) * s)
    fun faceY(edge: FrontEdge, s: Float, z: Float) = screenY(edge.ax + (edge.bx - edge.ax) * s, edge.ay + (edge.by - edge.ay) * s, z)

    fun screenX(mx: Float, my: Float) = (mx - my) * unit
    fun screenY(mx: Float, my: Float, z: Float) = (mx + my) * unit / 2 - z

    /** Черта по грани от (`s0`, `z0`) к (`s1`, `z1`) толщиной [width]. */
    fun faceLine(ink: WallInk, edge: FrontEdge, s0: Float, z0: Float, s1: Float, z1: Float, width: Float) = line(WallLayer.FACE, ink, faceX(edge, s0), faceY(edge, s0, z0), faceX(edge, s1), faceY(edge, s1, z1), width)

    /** Полоса грани по всей ширине отрезка: снизу - [low0] и [low1] на концах, сверху - [high0] и [high1]. */
    fun faceBand(ink: WallInk, edge: FrontEdge, s0: Float, s1: Float, low0: Float, low1: Float, high0: Float, high1: Float) {
        val path = path(WallLayer.FACE, ink)
        path.moveTo(faceX(edge, s0), faceY(edge, s0, low0))
        path.lineTo(faceX(edge, s1), faceY(edge, s1, low1))
        path.lineTo(faceX(edge, s1), faceY(edge, s1, high1))
        path.lineTo(faceX(edge, s0), faceY(edge, s0, high0))
        path.close()
    }

    /** Черта по крышке между точками карты. */
    fun capLine(ink: WallInk, x0: Float, y0: Float, x1: Float, y1: Float, width: Float) = line(WallLayer.CAP, ink, screenX(x0, y0), screenY(x0, y0, height), screenX(x1, y1), screenY(x1, y1, height), width)

    /** Пятно радиусом [r] клетки, лежащее в плоскости слоя [layer]: на крышке или на земле у подножия. */
    fun blob(layer: WallLayer, ink: WallInk, mx: Float, my: Float, r: Float) {
        val z = if (layer == WallLayer.FOOT) 0f else height
        val cx = screenX(mx, my)
        val cy = screenY(mx, my, z)
        val rx = r * unit * ISO_WIDE
        val ry = r * unit * ISO_FLAT
        path(layer, ink).addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
    }

    /** Точка в холсте (пиксели, `y` вниз) радиусом [r]: кончик листа. */
    fun dot(layer: WallLayer, ink: WallInk, px: Float, py: Float, r: Float) = path(layer, ink).addOval(Rect(px - r, py - r, px + r, py + r))

    /** Черта в холсте (пиксели, `y` вниз) - травинка, корень. */
    fun line(layer: WallLayer, ink: WallInk, x0: Float, y0: Float, x1: Float, y1: Float, width: Float) {
        val length = hypot(x1 - x0, y1 - y0)
        if (length < 1e-3f) return
        val ox = -(y1 - y0) / length * width / 2
        val oy = (x1 - x0) / length * width / 2
        val path = path(layer, ink)
        path.moveTo(x0 + ox, y0 + oy)
        path.lineTo(x1 + ox, y1 + oy)
        path.lineTo(x1 - ox, y1 - oy)
        path.lineTo(x0 - ox, y0 - oy)
        path.close()
    }

    /** Треугольник в холсте: скол, камешек. */
    fun triangle(layer: WallLayer, ink: WallInk, x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = path(layer, ink)
        path.moveTo(x0, y0)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.close()
    }

    /** Грани куска: все разом и доли тени по повороту ([SHADES] ступеней). */
    internal fun faces(): Array<Path?> {
        val faces = arrayOfNulls<Path>(SHADES + 1)
        edges.forEach { edge ->
            val bin = (edge.shade * SHADES + .5f).toInt().coerceIn(0, SHADES)
            val targets = if (bin == 0) listOf(0) else listOf(0, bin)
            targets.forEach { k ->
                val path = faces[k] ?: Path().also { faces[k] = it }
                path.moveTo(faceX(edge, 0f), faceY(edge, 0f, 0f))
                path.lineTo(faceX(edge, 1f), faceY(edge, 1f, 0f))
                path.lineTo(faceX(edge, 1f), faceY(edge, 1f, height))
                path.lineTo(faceX(edge, 0f), faceY(edge, 0f, height))
                path.close()
            }
        }
        return faces
    }

    /** Сумрак граней к подножию: вертикальный градиент по высоте куска. */
    internal fun dusk(): Brush? {
        if (edges.isEmpty()) return null
        var top = Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        edges.forEach { edge ->
            top = min(top, min(faceY(edge, 0f, height), faceY(edge, 1f, height)))
            bottom = max(bottom, max(faceY(edge, 0f, 0f), faceY(edge, 1f, 0f)))
        }
        return Brush.verticalGradient(0f to Color.Transparent, .45f to Color.Transparent, 1f to Color.Black.copy(alpha = DUSK), startY = top, endY = bottom)
    }

    /** Фаска: светлая полоса по кромке крышки над видимыми гранями. */
    internal fun rim(): Path? {
        if (edges.isEmpty()) return null
        val path = Path()
        val width = unit * RIM
        edges.forEach { edge ->
            path.moveTo(faceX(edge, 0f), faceY(edge, 0f, height))
            path.lineTo(faceX(edge, 1f), faceY(edge, 1f, height))
            path.lineTo(faceX(edge, 1f), faceY(edge, 1f, height) + width)
            path.lineTo(faceX(edge, 0f), faceY(edge, 0f, height) + width)
            path.close()
        }
        return path
    }

    internal fun marks(layer: WallLayer): Array<WallMark> = WallInk.entries.mapNotNull { ink -> paths[key(layer, ink)]?.let { WallMark(ink, it) } }.toTypedArray()

    private fun path(layer: WallLayer, ink: WallInk) = paths.getOrPut(key(layer, ink)) { Path() }

    private fun key(layer: WallLayer, ink: WallInk) = layer.ordinal * WallInk.entries.size + ink.ordinal

    private companion object {
        /** Ступени тени граней по повороту. */
        const val SHADES = 3

        /** Сумрак у самого подножия. */
        const val DUSK = .5f

        /** Толщина фаски в полуширинах клетки. */
        const val RIM = .07f

        /** Круг на плоскости карты в изометрии: полуоси эллипса на клетку радиуса, в полуширинах клетки. */
        const val ISO_WIDE = 1.414f
        const val ISO_FLAT = .707f
    }
}
