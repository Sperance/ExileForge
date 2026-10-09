package com.sperance.exileforge.ui.screens.expedition.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Узор контурной стены (утверждён владельцем по макетам «Стены v2»): шум края массива [edge] и метки куска - на гранях, на
 * крышке и у подножия. Узор лишь рисует в набросок ([WallSketch]); цвета даёт стиль карты ([WallTones]), так что один узор
 * в разных биомах - в своих красках. Всё детерминированно: от клетки, поля и соли карты.
 */
internal abstract class WallTexture(val edge: EdgeNoise) {
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
        // По ходу отрезка: доли растут
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
internal object Ashlar : WallTexture(EdgeNoise(amplitude = .06f, scale = 1.6f, rough = .2f)) {
    private const val COURSES = 4
    private const val JOINT = .7f
    private const val SLAB = .5f

    override fun WallSketch.face() {
        edges.forEach { edge ->
            for (r in 0 until COURSES) {
                val z0 = height * r / COURSES
                val z1 = height * (r + 1) / COURSES
                // Камни ряда: швы вразбежку по экрану, иной камень темнее
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
        // Плиты по полклетки рядами со сдвигом; шов рисуется, где обе его стороны - на крышке
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
                // Плита: темнее или светлее соседей, если целиком на крышке
                val x1 = minOf(jx + SLAB, x + 1f)
                val tint = cellNoise(floor(jx * 2).toInt(), row, 223)
                val whole = inside(jx, my, .08f) && inside(x1, my, .08f) && inside(jx, my + SLAB, .08f) && inside(x1, my + SLAB, .08f)
                if (whole && (tint < .3f || tint > .8f)) slab(if (tint < .3f) WallInk.DIM else WallInk.SHEEN, jx + .03f, my + .03f, x1 - .03f, my + SLAB - .03f)
            }
        }
        // Сколы у швов и трещина
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
internal object LiveRock : WallTexture(EdgeNoise(amplitude = .2f, scale = 2.4f, rough = .45f)) {
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
            // Прожилка: светлая жила наискось через пласты
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
internal object Thicket : WallTexture(EdgeNoise(amplitude = .17f, scale = 1.7f, rough = .15f)) {
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
        // Листва свисает с кромки над гранями
        edges.forEach { edge ->
            val mx = (edge.ax + edge.bx) / 2
            val my = (edge.ay + edge.by) / 2
            blob(WallLayer.CAP, WallInk.GROWTH, mx - edge.nx * .03f, my - edge.ny * .03f, .13f)
        }
        // Кроны: тень под листвой, лист, блик сверху
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
