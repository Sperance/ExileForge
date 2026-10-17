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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Узор контурной стены своего зерна скалы ([WallGrain], шум края - в поле стен ядра): метки куска - на гранях, на
 * крышке и у подножия. Узор лишь рисует в набросок ([WallSketch]); цвета даёт стиль карты ([WallTones]), так что один узор
 * в разных биомах - в своих красках. Всё детерминированно: от клетки, поля и соли карты.
 */
internal abstract class WallTexture {
    abstract fun WallSketch.face()
    abstract fun WallSketch.cap()
    abstract fun WallSketch.foot()

    /** Сколько целых шагов [step] (со сдвигом [phase]) лежит между экранными положениями [u0] и [u1]: доли пути по отрезку. */
    protected inline fun crossings(u0: Float, u1: Float, step: Float, phase: Float, at: (s: Float, index: Int) -> Unit) {
        val a = u0 / step + phase
        val b = u1 / step + phase
        if (a == b) return
        val lo = floor(minOf(a, b)).toInt() + 1
        val hi = floor(maxOf(a, b)).toInt()
        val order = if (a < b) lo..hi else hi downTo lo
        for (m in order) at((m - a) / (b - a), m)
    }

    /** Середина отрезка подножия, вынесенная наружу на [out] клетки: место для камешка или папоротника. */
    protected fun FrontEdge.outside(out: Float, along: Float = .5f) = floatArrayOf(ax + (bx - ax) * along + nx * out, ay + (by - ay) * along + ny * out)
}

/**
 * «Обточенный камень» - залы, крипты, руины, города: тёсаная кладка рядами вдоль грани со швами вразбежку, крышка - плиты со
 * швами, сколами и трещинами, светлая фаска по кромке, у подножия щебень. Край почти ровный.
 */
internal object Ashlar : WallTexture() {
    private const val COURSES = 4
    private const val JOINT = .7f
    private const val SLAB = .5f

    override fun WallSketch.face() {
        edges.forEach { edge ->
            for (r in 0 until COURSES) {
                val z0 = height * r / COURSES
                val z1 = height * (r + 1) / COURSES
                var from = 0f
                var block = floor(edge.u0 / JOINT + r * .5f).toInt()
                fun stone(to: Float) {
                    if (cellNoise(block, r, 211) < .3f) faceBand(WallInk.DIM, edge, from, to, z0, z0, z1, z1)
                }
                crossings(edge.u0, edge.u1, JOINT, r * .5f) { s, m ->
                    stone(s)
                    faceLine(WallInk.SHADOW, edge, s, z0 + unit * .02f, s, z1, unit * .03f)
                    from = s
                    block = if (edge.u1 > edge.u0) m else m - 1
                }
                stone(1f)
                if (r > 0) {
                    faceLine(WallInk.SHADOW, edge, 0f, z0, 1f, z0, unit * .035f)
                    faceLine(WallInk.SHEEN, edge, 0f, z0 + unit * .035f, 1f, z0 + unit * .035f, unit * .025f)
                }
            }
        }
    }

    override fun WallSketch.cap() {
        val step = 1f / 8
        for (r in 0 until 2) {
            val my = y + r * SLAB
            var mx = x.toFloat()
            while (mx < x + 1 - 1e-4f) {
                if (inside(mx, my, .1f) && inside(mx + step, my, .1f)) capLine(WallInk.SHADOW, mx, my, mx + step, my, unit * .03f)
                mx += step
            }
            val row = y * 2 + r
            val shift = (row % 2) * SLAB / 2
            for (k in 0 until 2) {
                val jx = x + shift + k * SLAB
                if (jx >= x + 1) continue
                if (inside(jx, my, .1f) && inside(jx, my + SLAB, .1f)) capLine(WallInk.SHADOW, jx, my, jx, my + SLAB, unit * .03f)
                val x1 = minOf(jx + SLAB, x + 1f)
                val tint = cellNoise(floor(jx * 2).toInt(), row, 223)
                val whole = inside(jx, my, .08f) && inside(x1, my, .08f) && inside(jx, my + SLAB, .08f) && inside(x1, my + SLAB, .08f)
                if (whole && (tint < .3f || tint > .8f)) slab(if (tint < .3f) WallInk.DIM else WallInk.SHEEN, jx + .03f, my + .03f, x1 - .03f, my + SLAB - .03f)
            }
        }
        repeat(2) { k ->
            val cx = x + .15f + hash(301, k) * .7f
            val cy = y + .15f + hash(302, k) * .7f
            if (!inside(cx, cy, .15f)) return@repeat
            val px = screenX(cx, cy)
            val py = screenY(cx, cy, height)
            val r = unit * (.06f + hash(303, k) * .05f)
            triangle(WallLayer.CAP, WallInk.LIGHT, px - r, py, px + r * .4f, py - r * .6f, px + r * .8f, py + r * .2f)
            triangle(WallLayer.CAP, WallInk.SHADOW, px - r, py, px + r * .8f, py + r * .2f, px, py + r * .5f)
        }
        if (hash(305) < .35f) crack(this, 306)
    }

    override fun WallSketch.foot() {
        edges.forEachIndexed { i, edge ->
            if (hash(320, i) > .55f) return@forEachIndexed
            repeat(2) { k ->
                val (mx, my) = edge.outside(.08f + hash(321, i * 2 + k) * .14f, .2f + hash(322, i * 2 + k) * .6f).let { it[0] to it[1] }
                if (!ground(mx, my)) return@repeat
                val r = .04f + hash(323, i * 2 + k) * .05f
                blob(WallLayer.FOOT, WallInk.DIM, mx + .02f, my + .02f, r * 1.2f)
                blob(WallLayer.FOOT, WallInk.STONE, mx, my, r)
            }
        }
    }

    private fun WallSketch.slab(ink: WallInk, x0: Float, y0: Float, x1: Float, y1: Float) {
        triangle(WallLayer.CAP, ink, screenX(x0, y0), screenY(x0, y0, height), screenX(x1, y0), screenY(x1, y0, height), screenX(x1, y1), screenY(x1, y1, height))
        triangle(WallLayer.CAP, ink, screenX(x0, y0), screenY(x0, y0, height), screenX(x1, y1), screenY(x1, y1, height), screenX(x0, y1), screenY(x0, y1, height))
    }
}

/**
 * «Живая скала» - пещеры, горы, пустоши: рваный край, грани - пласты породы с прожилками, крышка - неровный камень с пятнами
 * мха и лишайника, вокруг мелкие камни.
 */
internal object LiveRock : WallTexture() {
    private const val STRATA = 5

    override fun WallSketch.face() {
        edges.forEachIndexed { i, edge ->
            fun level(k: Int, u: Float) = height * (k.toFloat() / STRATA + .05f * sin(u * 2.6f + k * 1.9f) + .06f * (smooth(u * 1.3f, k * 3.1f, 1) - .5f))
            for (k in 1 until STRATA) {
                val z0 = level(k, edge.u0)
                val z1 = level(k, edge.u1)
                if (k % 2 == 1) {
                    faceBand(WallInk.DIM, edge, 0f, 1f, level(k - 1, edge.u0).coerceAtLeast(0f), level(k - 1, edge.u1).coerceAtLeast(0f), z0, z1)
                    faceLine(WallInk.SHADOW, edge, 0f, z0, 1f, z1, unit * .03f)
                } else {
                    faceLine(WallInk.SHEEN, edge, 0f, z0, 1f, z1, unit * .02f)
                }
            }
            if (hash(401, i) < .2f) {
                val sway = hash(402, i)
                val points = floatArrayOf(.25f + sway * .3f, .9f, .6f, .66f, .35f + sway * .2f, .42f, .7f, .16f)
                for (p in 0 until points.size / 2 - 1) {
                    faceLine(WallInk.LIGHT, edge, points[p * 2], height * points[p * 2 + 1], points[p * 2 + 2], height * points[p * 2 + 3], unit * .02f)
                }
            }
        }
    }

    override fun WallSketch.cap() {
        for (a in 0 until 3) {
            for (b in 0 until 3) {
                val k = a * 3 + b
                val mx = x + (a + .2f + hash(410, k) * .6f) / 3
                val my = y + (b + .2f + hash(411, k) * .6f) / 3
                if (!inside(mx, my, .12f)) continue
                val moss = smooth(mx * 1.1f, my * 1.1f, 0)
                when {
                    moss > .56f -> {
                        blob(WallLayer.CAP, WallInk.GROWTH, mx, my, .1f + (moss - .56f) * .5f)
                        if (moss > .66f) blob(WallLayer.CAP, WallInk.BLOOM, mx - .03f, my - .03f, .05f)
                    }

                    hash(412, k) > .55f -> blob(WallLayer.CAP, WallInk.DIM, mx, my, .08f + hash(413, k) * .08f)

                    hash(414, k) < .2f -> blob(WallLayer.CAP, WallInk.LIGHT, mx, my, .03f + hash(415, k) * .02f)
                }
            }
        }
        if (hash(420) < .45f) crack(this, 421)
    }

    override fun WallSketch.foot() {
        edges.forEachIndexed { i, edge ->
            if (hash(430, i) > .6f) return@forEachIndexed
            val (mx, my) = edge.outside(.06f + hash(431, i) * .2f, .2f + hash(432, i) * .6f).let { it[0] to it[1] }
            if (!ground(mx, my)) return@forEachIndexed
            val px = screenX(mx, my)
            val py = screenY(mx, my, 0f)
            val r = unit * (.06f + hash(433, i) * .07f)
            blob(WallLayer.FOOT, WallInk.DIM, mx + .02f, my + .02f, r / unit * .9f)
            triangle(WallLayer.FOOT, WallInk.STONE, px - r, py + r * .25f, px + r * (.2f + hash(434, i) * .6f), py - r * .7f, px + r, py + r * .3f)
        }
    }
}

/**
 * «Заросли» - лес, болото, джунгли: мягкий «кроновый» край, грани - переплетение корней в тени, крышка - листва слоями с
 * бликами, листва свисает с кромки, у подножия папоротник.
 */
internal object Thicket : WallTexture() {
    private const val ROOTS = .45f

    override fun WallSketch.face() {
        edges.forEach { edge ->
            faceBand(WallInk.DIM, edge, 0f, 1f, 0f, 0f, height * .4f, height * .4f)
            crossings(edge.u0, edge.u1, ROOTS, cellNoise(x, y, 501)) { s, m ->
                val drop = height * (.2f + cellNoise(m, 0, 502) * .5f)
                val lean = (cellNoise(m, 1, 503) - .5f) * .3f
                faceLine(WallInk.ROOT, edge, s, height, (s + lean * .5f).coerceIn(0f, 1f), height - drop * .5f, unit * .05f)
                faceLine(WallInk.ROOT, edge, (s + lean * .5f).coerceIn(0f, 1f), height - drop * .5f, (s - lean * .3f).coerceIn(0f, 1f), height - drop, unit * .035f)
            }
        }
    }

    override fun WallSketch.cap() {
        edges.forEach { edge ->
            val mx = (edge.ax + edge.bx) / 2
            val my = (edge.ay + edge.by) / 2
            blob(WallLayer.CAP, WallInk.GROWTH, mx - edge.nx * .03f, my - edge.ny * .03f, .13f)
        }
        for (a in 0 until 3) {
            for (b in 0 until 3) {
                val k = a * 3 + b
                val mx = x + (a + .15f + hash(510, k) * .7f) / 3
                val my = y + (b + .15f + hash(511, k) * .7f) / 3
                if (!inside(mx, my, .02f)) continue
                val r = .14f + hash(512, k) * .08f
                blob(WallLayer.CAP, WallInk.DIM, mx + .05f, my + .05f, r)
                blob(WallLayer.CAP, WallInk.GROWTH, mx, my, r)
                if (hash(513, k) > .35f) blob(WallLayer.CAP, WallInk.BLOOM, mx - r * .3f, my - r * .3f, r * .45f)
                if (hash(514, k) > .85f) blob(WallLayer.CAP, WallInk.LIGHT, mx - r * .45f, my - r * .2f, r * .15f)
            }
        }
    }

    override fun WallSketch.foot() {
        edges.forEachIndexed { i, edge ->
            if (hash(520, i) > .5f) return@forEachIndexed
            val (mx, my) = edge.outside(.06f + hash(521, i) * .1f, .2f + hash(522, i) * .6f).let { it[0] to it[1] }
            if (!ground(mx, my)) return@forEachIndexed
            val px = screenX(mx, my)
            val py = screenY(mx, my, 0f)
            val length = unit * (.3f + hash(523, i) * .25f)
            for (blade in 0 until 5) {
                val angle = (PI / 2 + (blade - 2) * .42f).toFloat()
                val tx = px + cos(angle) * length
                val ty = py - sin(angle) * length * (.7f + .1f * (blade % 2))
                line(WallLayer.FOOT, WallInk.GROWTH, px, py, (px + tx) / 2 + (tx - px) * .1f, (py + ty) / 2 - length * .12f, unit * .045f)
                line(WallLayer.FOOT, WallInk.GROWTH, (px + tx) / 2 + (tx - px) * .1f, (py + ty) / 2 - length * .12f, tx, ty, unit * .035f)
                if (blade % 2 == 0) dot(WallLayer.FOOT, WallInk.BLOOM, tx, ty, unit * .05f)
            }
        }
    }
}

/** Трещина по крышке: три излома, все - на крышке. */
private fun crack(sketch: WallSketch, salt: Int) = with(sketch) {
    var mx = x + .25f + hash(salt) * .5f
    var my = y + .25f + hash(salt + 1) * .5f
    var angle = hash(salt + 2) * 6.28f
    repeat(3) { k ->
        val nx = mx + cos(angle) * .14f
        val ny = my + sin(angle) * .14f
        if (!inside(mx, my, .12f) || !inside(nx, ny, .12f)) return@with
        capLine(WallInk.SHADOW, mx, my, nx, ny, unit * (.025f - k * .005f))
        mx = nx
        my = ny
        angle += (hash(salt + 3 + k) - .5f) * 1.6f
    }
}

/** Узор зерна скалы: тёсаный камень, живая скала или заросли. */
internal val WallGrain.texture: WallTexture
    get() = when (this) {
        WallGrain.ASHLAR -> Ashlar
        WallGrain.LIVE_ROCK -> LiveRock
        WallGrain.THICKET -> Thicket
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
