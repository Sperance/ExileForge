package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.rift.RiftNode
import com.sperance.exileforge.rules.rift.RiftPlan
import com.sperance.exileforge.ui.screens.expedition.scene.MapProjection
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

/**
 * Тропа карты Разлома от [from] к [to] (в dp холста): кривая, что выходит из узла вверх и входит в следующий снизу, - извилина острова.
 */
internal class IsleTrail(val from: Offset, val to: Offset) {
    private val c1 = Offset(from.x, from.y - (from.y - to.y) * BEND)
    private val c2 = Offset(to.x, to.y + (from.y - to.y) * BEND)

    /** Точка тропы на доле пути [t]. */
    fun at(t: Float): Offset {
        val u = 1 - t
        val a = u * u * u
        val b = 3 * u * u * t
        val c = 3 * u * t * t
        val d = t * t * t
        return Offset(a * from.x + b * c1.x + c * c2.x + d * to.x, a * from.y + b * c1.y + c * c2.y + d * to.y)
    }

    /** Тропа путём в пикселях при [scale] пикселей на dp. */
    fun path(scale: Float): Path = Path().apply {
        moveTo(from.x * scale, from.y * scale)
        cubicTo(c1.x * scale, c1.y * scale, c2.x * scale, c2.y * scale, to.x * scale, to.y * scale)
    }

    private companion object {
        const val BEND = .55f
    }
}

/**
 * Клетка земли острова: середина ромба [centre] (dp), ярус-акт [act], стенки обрыва слева [left] и справа [right] - там, где
 * соседа по изометрии нет, руна-искра [rune].
 */
internal class IsleTile(val centre: Offset, val act: Int, val left: Boolean, val right: Boolean, val rune: Boolean)

/**
 * Раскладка карты Разлома по схеме недели, в dp холста шириной [width] и высотой [height]: узлы - вдоль извилистой тропы снизу вверх
 * (глубина - ряд, место в ряду - вбок), вход [entry] под первым рядом; земля - клетки изометрии Похода ([MapProjection]) под тропами и
 * узлами ([tiles]), обрыв - стенки клеток без соседа. Считается раз на схему и ширину.
 */
internal class RiftIsle private constructor(
    val width: Float,
    val height: Float,
    private val points: Array<Offset>,
    val entry: Offset,
    val tiles: List<IsleTile>,
    val stars: List<Offset>,
) {
    /** Место узла [id] на земле; null - вход. */
    fun at(id: Int?): Offset = id?.let(points::get) ?: entry

    /** Тропа с места [from] (null - вход) к узлу [to]. */
    fun trail(from: Int?, to: Int): IsleTrail = IsleTrail(at(from), at(to))

    /** Высота земли на глубине [depth] (−1 - вход). */
    fun rowY(depth: Int): Float = entry.y - (depth + 1) * STEP

    companion object {
        /** Шаг ряда по высоте, dp. */
        const val STEP = 86f

        /** Полуширина клетки земли, dp. */
        const val UNIT = 14f

        /** Высота стенки обрыва, dp. */
        const val WALL = 16f

        /**
         * Раскладка схемы [plan] глубиной [depths] рядов ([depth] - ряд узла) на ширину [width] dp с запасом [top] dp сверху;
         * [lanes] - самый широкий ряд.
         */
        fun of(plan: RiftPlan, depth: (RiftNode) -> Int, depths: Int, lanes: Int, width: Float, top: Float): RiftIsle {
            val height = top + GATE_RISE + depths * STEP + ENTRY_PAD
            val entry = Offset(middle(width, -1), height - ENTRY_PAD)
            val pitch = min(PITCH, (width - 2 * (SIDE + WIGGLE)) / (lanes - 1).coerceAtLeast(1))
            val points = Array(plan.nodes.size) { i ->
                val node = plan.nodes[i]
                val row = depth(node)
                Offset(middle(width, row) + (node.lane - (node.width - 1) / 2f) * pitch, entry.y - (row + 1) * STEP)
            }

            fun at(id: Int?) = id?.let(points::get) ?: entry
            val discs = buildList {
                add(Disc(entry, NODE_LAND, 0))
                plan.nodes.forEach { node ->
                    add(Disc(points[node.id], if (node.kind == RiftNodeKind.GUARDIAN) GATE_LAND else NODE_LAND, node.act))
                    node.links.forEach { link -> trailDiscs(IsleTrail(points[node.id], points[link]), plan.nodes[link].act) }
                }
                plan.entries.forEach { trailDiscs(IsleTrail(entry, at(it)), 0) }
            }
            return RiftIsle(width, height, points, entry, land(discs, width, height), stars(plan.seed, width, height))
        }

        private fun MutableList<Disc>.trailDiscs(trail: IsleTrail, act: Int) {
            repeat(SAMPLES + 1) { k -> add(Disc(trail.at(k / SAMPLES.toFloat()), PATH_LAND, act)) }
        }

        private fun middle(width: Float, row: Int): Float = width / 2 + WIGGLE * sin(row * .62f + .4f)

        /**
         * Клетки изометрии, чья середина - на земле: под кругом узла или тропы (по высоте круг сжат - земля лежит плоско). Начало карты
         * выше холста на полширины - все клетки холста с неотрицательными номерами.
         */
        private fun land(discs: List<Disc>, width: Float, height: Float): List<IsleTile> {
            val bands = discs.groupBy { (it.at.y / BAND).toInt() }
            val projection = MapProjection(Offset(0f, -width / 2), UNIT)
            val span = ceil((width + height) / UNIT).toInt() + 2
            val (xs, ys) = projection.cells(Size(width, height), span, span)
            val acts = IntArray(span * span) { -1 }
            fun act(x: Int, y: Int): Int = if (x in 0 until span && y in 0 until span) acts[y * span + x] else -1
            for (x in xs) {
                for (y in ys) {
                    val c = projection.at(x + .5, y + .5)
                    if (c.x < 0 || c.x > width || c.y < 0 || c.y > height) continue
                    val band = (c.y / BAND).toInt()
                    val hit = (band - 1..band + 1).firstNotNullOfOrNull { b ->
                        bands[b]?.firstOrNull { d ->
                            val dx = c.x - d.at.x
                            val dy = (c.y - d.at.y) * FLAT
                            dx * dx + dy * dy <= d.r * d.r
                        }
                    }
                    if (hit != null) acts[y * span + x] = hit.act
                }
            }
            return buildList {
                for (x in xs) {
                    for (y in ys) {
                        val act = act(x, y)
                        if (act < 0) continue
                        add(IsleTile(projection.at(x + .5, y + .5), act, left = act(x, y + 1) < 0, right = act(x + 1, y) < 0, rune = (x * 73 + y * 37) % 29 == 0))
                    }
                }
            }
        }

        /** Звёзды изнанки по сиду недели: одни и те же у всех. */
        private fun stars(seed: Long, width: Float, height: Float): List<Offset> {
            val random = java.util.Random(seed)
            return List((height / STAR_GAP).toInt()) { Offset(random.nextFloat() * width, random.nextFloat() * height) }
        }

        /** Круг земли под узлом или точкой тропы: середина [at], радиус [r] (dp), акт [act]. */
        private class Disc(val at: Offset, val r: Float, val act: Int)

        private const val GATE_RISE = 96f
        private const val ENTRY_PAD = 56f
        private const val PITCH = 84f
        private const val SIDE = 30f
        private const val WIGGLE = 22f
        private const val NODE_LAND = 27f
        private const val GATE_LAND = 62f
        private const val PATH_LAND = 14f
        private const val SAMPLES = 10
        private const val BAND = 48f
        private const val FLAT = 1.6f
        private const val STAR_GAP = 9f
    }
}

/**
 * Земля острова в пикселях ([scale] пикселей на dp), собранная раз на раскладку: изнанка со звёздами, стенки обрыва, ромбы клеток в
 * два тона по ярусам-актам, кромка и руны-искры. Рисуется без часов - слой стоит.
 */
internal class IsleArt(isle: RiftIsle, private val scale: Float) {
    private val tops = List(2) { Path() }
    private val walls = Path()
    private val shades = Path()
    private val rims = Path()
    private val runes = isle.tiles.filter { it.rune }.map { it.centre * scale }
    private val stars = isle.stars.map { it * scale }
    private val void = Brush.verticalGradient(listOf(RiftColors.Ground, RiftColors.Ground2, RiftColors.Ground))

    init {
        val u = RiftIsle.UNIT * scale
        val h = u / 2
        val wall = RiftIsle.WALL * scale
        isle.tiles.forEach { tile ->
            val c = tile.centre * scale
            if (tile.left) {
                quad(walls, Offset(c.x - u, c.y), Offset(c.x, c.y + h), Offset(c.x, c.y + h + wall), Offset(c.x - u, c.y + wall))
                rims.moveTo(c.x - u, c.y + wall)
                rims.lineTo(c.x, c.y + h + wall)
            }
            if (tile.right) {
                quad(shades, Offset(c.x, c.y + h), Offset(c.x + u, c.y), Offset(c.x + u, c.y + wall), Offset(c.x, c.y + h + wall))
                rims.moveTo(c.x, c.y + h + wall)
                rims.lineTo(c.x + u, c.y + wall)
            }
        }
        isle.tiles.forEach { tile ->
            val c = tile.centre * scale
            quad(tops[tile.act % 2], Offset(c.x - u, c.y), Offset(c.x, c.y - h), Offset(c.x + u, c.y), Offset(c.x, c.y + h))
        }
    }

    fun draw(scope: DrawScope) = with(scope) {
        drawRect(void)
        stars.forEachIndexed { k, at -> drawCircle(if (k % 3 == 0) RiftColors.Rift else RiftColors.Soft, (.4f + k % 4 * .3f) * scale, at, alpha = .12f + k % 5 * .08f) }
        drawPath(walls, IsleWall)
        drawPath(shades, IsleShade)
        drawPath(rims, RiftColors.Rift.copy(alpha = .35f), style = Stroke(1.2f * scale))
        drawPath(tops[0], IsleTop)
        drawPath(tops[1], IsleTopAlt)
        drawPath(tops[0], RiftColors.Line, style = Stroke(.6f * scale))
        drawPath(tops[1], RiftColors.Line, style = Stroke(.6f * scale))
        runes.forEach { drawCircle(RiftColors.Rift, 1.3f * scale, it, alpha = .5f) }
    }

    private fun quad(path: Path, a: Offset, b: Offset, c: Offset, d: Offset) {
        path.moveTo(a.x, a.y)
        path.lineTo(b.x, b.y)
        path.lineTo(c.x, c.y)
        path.lineTo(d.x, d.y)
        path.close()
    }
}

private val IsleTop = Color(0xFF0C1A12)
private val IsleTopAlt = Color(0xFF0A1510)
private val IsleWall = Color(0xFF07110B)
private val IsleShade = Color(0xFF040A06)
