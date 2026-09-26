package com.sperance.exileforge.ui.screens.expedition.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.model.campaign.CampaignView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The parchment the world map is drawn on (2.76.0, the owner's pick «Пергамент»): a dark chart with
 * a sea along the west, the land each zone stands on sketched in its biome — hills and snow over the
 * mines and passes, trees in the forests, reeds in the mire, columns over ruins — the dotted borders
 * of the regions and a compass rose. It is the world's alone, not the hero's: built once per world,
 * laid out «down» (`height - y`, as the screen goes) in world units, and every sketch of one kind is
 * one path, so a frame is a few dozen draws however far the map is zoomed.
 */
class WorldArt private constructor(
    private val width: Float,
    private val height: Float,
    private val pools: List<Glow>,
    private val sea: Path,
    private val coast: Path,
    private val lining: List<Path>,
    private val waves: Path,
    private val sketches: List<Sketch>,
    private val grain: List<Grain>,
    private val borders: List<Path>,
) {
    private class Glow(val centre: Offset, val radius: Float, val color: Color)
    private class Sketch(val path: Path, val color: Color, val fill: Boolean, val width: Float = 1f)
    private class Grain(val points: List<Offset>, val color: Color)

    fun draw(scope: DrawScope) = with(scope) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF14100B), Color(0xFF1B150D), Color(0xFF1F180F)), 0f, height), size = Size(width, height))
        pools.forEach { drawCircle(Brush.radialGradient(listOf(it.color, it.color.copy(alpha = 0f)), it.centre, it.radius), it.radius, it.centre) }
        drawPath(sea, Brush.horizontalGradient(listOf(Color(0xFF071418), Color(0xFF0D2229)), 0f, SEA_WIDTH))
        lining.forEachIndexed { i, path -> drawPath(path, Rune.copy(alpha = .24f - (i + 1) * .05f), style = Stroke(1f)) }
        drawPath(coast, Gold.copy(alpha = .6f), style = Stroke(1.8f))
        drawPath(waves, Rune.copy(alpha = .28f), style = Stroke(1f))
        sketches.forEach { if (it.fill) drawPath(it.path, it.color) else drawPath(it.path, it.color, style = Stroke(it.width, cap = StrokeCap.Round)) }
        grain.forEach { drawPoints(it.points, PointMode.Points, it.color, strokeWidth = 1f) }
        val dots = PathEffect.dashPathEffect(floatArrayOf(2f, 7f))
        borders.forEach { drawPath(it, Gold.copy(alpha = .45f), style = Stroke(1.6f, pathEffect = dots, cap = StrokeCap.Round)) }
        drawCompass(Offset(width - COMPASS_INSET, height - COMPASS_INSET))
    }

    /** A compass rose in the south-east corner: eight points, the long four lit on one side. */
    private fun DrawScope.drawCompass(centre: Offset) {
        drawCircle(Gold.copy(alpha = .5f), 29f, centre, style = Stroke(1f))
        repeat(8) { i ->
            val angle = i * PI / 4 - PI / 2
            val long = i % 2 == 0
            val length = if (long) 40f else 22f
            val half = if (long) 6.5f else 4f
            fun at(a: Double, r: Float) = Offset(centre.x + (cos(a) * r).toFloat(), centre.y + (sin(a) * r).toFloat())
            val tip = at(angle, length)
            val lit = Path().apply { moveTo(centre.x, centre.y); at(angle - PI / 2, half).let { lineTo(it.x, it.y) }; lineTo(tip.x, tip.y); close() }
            val dark = Path().apply { moveTo(centre.x, centre.y); at(angle + PI / 2, half).let { lineTo(it.x, it.y) }; lineTo(tip.x, tip.y); close() }
            drawPath(lit, Gold.copy(alpha = if (long) .8f else .5f))
            drawPath(dark, Color(0xF21E180F))
            drawPath(dark, Gold.copy(alpha = .5f), style = Stroke(1f))
        }
    }

    companion object {
        private val Gold = Color(0xFFC8AA6E)
        private val Rune = Color(0xFF7FA9C8)
        private const val SEA_WIDTH = 150f
        private const val COMPASS_INSET = 70f

        /** Where the sea meets the land at [y] (down): a slow swell over a quick ripple. */
        fun coastAt(y: Float): Float = (95 + 26 * sin(y / 70.0) + 12 * sin(y / 29.0 + 1)).toFloat()

        fun of(view: CampaignView): WorldArt {
            val width = view.world.width.toFloat()
            val height = view.world.height.toFloat()
            val random = Random(view.regions.sumOf { it.code.hashCode() } xor view.world.height)
            val zones = view.zones
            val tokens = zones.map { Offset(it.x.toFloat(), height - it.y) }
            val labels = view.regions.map { Rect(Offset(it.label.x.toFloat(), height - it.label.y), 190f) }
            // A sketch keeps off the tokens, off the names hanging under them, off the regions' names and out of the sea.
            fun free(p: Offset, room: Float): Boolean =
                p.x > coastAt(p.y) + 14 && p.x < width - 8 && p.y > 8 && p.y < height - 8 &&
                    tokens.none { t -> hypot(t.x - p.x, t.y - p.y) < room || (kotlin.math.abs(t.x - p.x) < 72 && p.y > t.y && p.y < t.y + 56) } &&
                    labels.none { it.copy(top = it.center.y - 26, bottom = it.center.y + 26).contains(p) }

            val pools = view.regions.filter { it.zones.isNotEmpty() }.map { region ->
                val centre = Offset(region.zones.map { it.x }.average().toFloat(), height - region.zones.map { it.y }.average().toFloat())
                Glow(centre, 520f, poolColor(region.zones.groupingBy { it.biome }.eachCount().maxByOrNull { it.value }?.key.orEmpty()))
            } + List(12) { Glow(Offset(random.nextFloat() * width, random.nextFloat() * height), 80f + random.nextFloat() * 170f, Color.Black.copy(alpha = .18f)) }

            val shore = (0..height.toInt() step 5).map { y -> Offset(coastAt(y.toFloat()), y.toFloat()) }
            val sea = Path().apply { moveTo(0f, 0f); shore.forEach { lineTo(it.x, it.y) }; lineTo(0f, height); close() }
            fun line(points: List<Offset>) = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            val coast = line(shore)
            val lining = (1..4).map { k -> line(shore.map { Offset(maxOf(0f, it.x - k * 7), it.y) }) }
            val waves = Path().apply {
                repeat((height / 45).toInt()) {
                    val y = random.nextFloat() * height
                    val room = coastAt(y) - 34
                    if (room > 10) {
                        val x = 6 + random.nextFloat() * room
                        moveTo(x, y); quadraticTo(x + 5, y - 4, x + 10, y); quadraticTo(x + 15, y - 4, x + 20, y)
                    }
                }
            }

            val sketch = Sketches()
            zones.forEach { zone ->
                val centre = Offset(zone.x.toFloat(), height - zone.y)
                val kind = sketchOf(zone.biome) ?: return@forEach
                var placed = 0
                var tries = 0
                while (placed < kind.count && tries++ < 400) {
                    val angle = random.nextDouble() * 2 * PI
                    val distance = 48 + random.nextFloat() * 110
                    val p = Offset(centre.x + (cos(angle) * distance).toFloat(), centre.y + (sin(angle) * distance * .8).toFloat())
                    if (!free(p, kind.room) || sketch.crowded(p, kind.room * .6f)) continue
                    sketch.add(kind, p, 1 + random.nextFloat() * .7f)
                    placed++
                }
            }
            val grain = listOf(.03f, .05f, .08f).map { alpha ->
                Grain(List(1400) { Offset(random.nextFloat() * width, random.nextFloat() * height) }, Color(0xFFE4DCCF).copy(alpha = alpha))
            }
            val borders = WorldMap.borders(view).map { y ->
                val down = height - y
                line((coastAt(down).toInt() + 4..width.toInt() step 4).map { x -> Offset(x.toFloat(), down + (14 * sin(x / 90.0)).toFloat()) })
            }
            return WorldArt(width, height, pools, sea, coast, lining, waves, sketch.build(), grain, borders)
        }

        private fun poolColor(biome: String): Color = when (biome) {
            "MINES", "FROST" -> Color(0xFF7FA9C8).copy(alpha = .13f)
            "ASH" -> Color(0xFFD9642E).copy(alpha = .1f)
            "TEMPLE", "VAAL" -> Color(0xFF3A8290).copy(alpha = .1f)
            "CRYPT", "RUINS" -> Color(0xFF8A7AA0).copy(alpha = .08f)
            else -> Color(0xFF805E38).copy(alpha = .2f)
        }

        private fun sketchOf(biome: String): SketchKind? = when (biome) {
            "MINES" -> SketchKind.CRYSTAL_HILL
            "FROST" -> SketchKind.SNOW_HILL
            "ASH" -> SketchKind.CINDER_HILL
            "FOREST" -> SketchKind.TREE
            "MIRE" -> SketchKind.REED
            "RUINS", "TEMPLE" -> SketchKind.COLUMN
            "CRYPT" -> SketchKind.TOMB
            "CAVE", "SHORE" -> SketchKind.ROCK
            else -> null
        }
    }

    /** What a biome's land is sketched with, how many a zone gets and how much room each wants. */
    private enum class SketchKind(val count: Int, val room: Float) {
        CRYSTAL_HILL(6, 52f), SNOW_HILL(6, 52f), CINDER_HILL(6, 52f), TREE(10, 30f), REED(10, 28f), COLUMN(7, 26f), TOMB(7, 26f), ROCK(7, 28f)
    }

    /** The sketches gathered into one path per ink, so the whole land is drawn in a handful of calls. */
    private class Sketches {
        private val spots = mutableListOf<Offset>()
        private val hillBody = Path()
        private val hillEdge = Path()
        private val hillHatch = Path()
        private val snow = Path()
        private val embers = Path()
        private val crystals = Path()
        private val canopies = Path()
        private val canopyEdge = Path()
        private val trunks = Path()
        private val reeds = Path()
        private val stones = Path()
        private val stoneEdge = Path()

        fun crowded(p: Offset, room: Float) = spots.any { hypot(it.x - p.x, it.y - p.y) < room }

        fun add(kind: SketchKind, p: Offset, size: Float) {
            spots += p
            when (kind) {
                SketchKind.CRYSTAL_HILL, SketchKind.SNOW_HILL, SketchKind.CINDER_HILL -> hill(kind, p, 15 * size)
                SketchKind.TREE -> {
                    trunks.moveTo(p.x, p.y); trunks.lineTo(p.x, p.y + 6)
                    canopies.addOval(Rect(Offset(p.x, p.y - 2), 6f)); canopyEdge.addOval(Rect(Offset(p.x, p.y - 2), 6f))
                }
                SketchKind.REED -> {
                    reeds.moveTo(p.x - 5, p.y); reeds.lineTo(p.x + 5, p.y)
                    reeds.moveTo(p.x, p.y); reeds.lineTo(p.x, p.y - 6)
                    reeds.moveTo(p.x, p.y); reeds.lineTo(p.x - 3, p.y - 5)
                    reeds.moveTo(p.x, p.y); reeds.lineTo(p.x + 3, p.y - 5)
                }
                SketchKind.COLUMN -> {
                    val h = 7 + 5 * size
                    stones.addRect(Rect(p.x, p.y - h, p.x + 4, p.y))
                    stones.addRect(Rect(p.x + 7, p.y - h * .55f, p.x + 11, p.y))
                }
                SketchKind.TOMB -> {
                    val arch = Path().apply { moveTo(p.x - 5, p.y); lineTo(p.x - 5, p.y - 6); quadraticTo(p.x, p.y - 13, p.x + 5, p.y - 6); lineTo(p.x + 5, p.y); close() }
                    stones.addPath(arch); stoneEdge.addPath(arch)
                    stoneEdge.moveTo(p.x, p.y - 8); stoneEdge.lineTo(p.x, p.y - 3); stoneEdge.moveTo(p.x - 2, p.y - 6); stoneEdge.lineTo(p.x + 2, p.y - 6)
                }
                SketchKind.ROCK -> {
                    val rock = Rect(p.x - 6 * size, p.y - 4 * size, p.x + 6 * size, p.y)
                    stones.addOval(rock); stoneEdge.addOval(rock)
                }
            }
        }

        /** A hill as the old charts drew them: a dark cone, an inked edge, hatching on the shaded side, and its cap. */
        private fun hill(kind: SketchKind, p: Offset, s: Float) {
            val peak = Offset(p.x - s * .12f, p.y - s * 1.05f)
            val left = p.x - s
            val right = p.x + s * .9f
            hillBody.moveTo(left, p.y); hillBody.lineTo(peak.x, peak.y); hillBody.lineTo(right, p.y); hillBody.close()
            hillEdge.moveTo(left, p.y); hillEdge.lineTo(peak.x, peak.y); hillEdge.lineTo(right, p.y)
            for (i in 1..4) {
                val t = i / 5f
                val hx = peak.x + (right - peak.x) * t
                val hy = peak.y + (p.y - peak.y) * t
                hillHatch.moveTo(hx, hy); hillHatch.lineTo(hx - s * .22f, p.y)
            }
            val cap = Path().apply {
                moveTo(peak.x, peak.y); lineTo(peak.x - s * .28f, peak.y + s * .34f); lineTo(peak.x - s * .05f, peak.y + s * .26f)
                lineTo(peak.x + s * .12f, peak.y + s * .38f); lineTo(peak.x + s * .3f, peak.y + s * .3f); close()
            }
            when (kind) {
                SketchKind.CINDER_HILL -> embers.addPath(cap)
                SketchKind.CRYSTAL_HILL -> {
                    snow.addPath(cap)
                    val c = Offset(p.x + s * .9f, p.y - 3)
                    crystals.moveTo(c.x, c.y - 9); crystals.lineTo(c.x + 3.5f, c.y); crystals.lineTo(c.x, c.y + 3); crystals.lineTo(c.x - 3.5f, c.y); crystals.close()
                }
                else -> snow.addPath(cap)
            }
        }

        fun build(): List<Sketch> {
            val gold = Color(0xFFC8AA6E)
            return listOf(
                Sketch(hillBody, Color(0xEB10141A), fill = true),
                Sketch(hillEdge, gold.copy(alpha = .55f), fill = false, width = 1.2f),
                Sketch(hillHatch, gold.copy(alpha = .25f), fill = false),
                Sketch(snow, Color(0xFFCFE3F2).copy(alpha = .55f), fill = true),
                Sketch(embers, Color(0xFFD9642E).copy(alpha = .7f), fill = true),
                Sketch(crystals, Color(0xFF8EC5FF).copy(alpha = .5f), fill = true),
                Sketch(trunks, gold.copy(alpha = .35f), fill = false),
                Sketch(canopies, Color(0xF2223420), fill = true),
                Sketch(canopyEdge, Color(0xFFA0B478).copy(alpha = .45f), fill = false),
                Sketch(reeds, Color(0xFF8CA578).copy(alpha = .5f), fill = false),
                Sketch(stones, Color(0xFF3A3326).copy(alpha = .9f), fill = true),
                Sketch(stoneEdge, gold.copy(alpha = .35f), fill = false),
            )
        }
    }
}
