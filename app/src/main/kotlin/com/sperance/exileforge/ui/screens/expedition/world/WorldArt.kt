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
import com.sperance.exileforge.rules.content.CampaignFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The parchment the world map is drawn on (2.76.0, the owner's pick «Пергамент»): a dark chart with
 * a sea along the west, the land each zone stands on sketched in its biome — hills and snow over the
 * mines and passes, trees in the forests, reeds in the mire, columns over ruins, and since 2.77.0
 * dunes, mesas, hives, towers, chasms and spore caps over the lands up to level 70 — the dotted borders
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
            val lit = Path().apply {
                moveTo(centre.x, centre.y)
                at(angle - PI / 2, half).let { lineTo(it.x, it.y) }
                lineTo(tip.x, tip.y)
                close()
            }
            val dark = Path().apply {
                moveTo(centre.x, centre.y)
                at(angle + PI / 2, half).let { lineTo(it.x, it.y) }
                lineTo(tip.x, tip.y)
                close()
            }
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

        /** The one world's art, built once per process (3.75.0): a return to the tab finds it ready. */
        @Volatile private var cache: Pair<CampaignFile, WorldArt>? = null
        private val building = Mutex()

        /** The art if it is built already, for the first frame. */
        fun cached(campaign: CampaignFile): WorldArt? = cache?.takeIf { it.first === campaign }?.second

        /**
         * The art, built away from the main thread (3.75.0): hundreds of sketches and grains took the tab a second to open.
         * It is warmed as soon as the campaign arrives, so the map seldom waits for it at all.
         */
        suspend fun of(campaign: CampaignFile): WorldArt = cached(campaign) ?: building.withLock {
            cached(campaign) ?: withContext(Dispatchers.Default) { build(campaign) }.also { cache = campaign to it }
        }

        private fun build(campaign: CampaignFile): WorldArt {
            val width = campaign.world.width.toFloat()
            val height = campaign.world.height.toFloat()
            val random = Random(campaign.regions.sumOf { it.code.hashCode() } xor campaign.world.height)
            val zones = campaign.zones
            val tokens = zones.map { Offset(it.x.toFloat(), height - it.y) }
            val labels = campaign.regions.map { Rect(Offset(it.label.x.toFloat(), height - it.label.y), 190f) }
            // The stains and the grain were measured for the first chart of 1100 × 2000; a taller world gets as many per stretch.
            val area = width * height / (1100f * 2000f)

            // A sketch keeps off the tokens, off the names hanging under them, off the regions' names and out of the sea.
            fun free(p: Offset, room: Float): Boolean = p.x > coastAt(p.y) + 14 && p.x < width - 8 && p.y > 8 && p.y < height - 8 &&
                tokens.none { t -> hypot(t.x - p.x, t.y - p.y) < room || (kotlin.math.abs(t.x - p.x) < 72 && p.y > t.y && p.y < t.y + 56) } &&
                labels.none { it.copy(top = it.center.y - 26, bottom = it.center.y + 26).contains(p) }

            val pools = campaign.regions.filter { it.zones.isNotEmpty() }.map { region ->
                val centre = Offset(region.zones.map { it.x }.average().toFloat(), height - region.zones.map { it.y }.average().toFloat())
                Glow(centre, 520f, poolColor(region.zones.groupingBy { it.biome }.eachCount().maxByOrNull { it.value }?.key.orEmpty()))
            } + List((12 * area).toInt()) { Glow(Offset(random.nextFloat() * width, random.nextFloat() * height), 80f + random.nextFloat() * 170f, Color.Black.copy(alpha = .18f)) }

            val shore = (0..height.toInt() step 5).map { y -> Offset(coastAt(y.toFloat()), y.toFloat()) }
            val sea = Path().apply {
                moveTo(0f, 0f)
                shore.forEach { lineTo(it.x, it.y) }
                lineTo(0f, height)
                close()
            }
            fun line(points: List<Offset>) = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            val coast = line(shore)
            val lining = (1..4).map { k -> line(shore.map { Offset(maxOf(0f, it.x - k * 7), it.y) }) }
            val waves = Path().apply {
                repeat((height / 45).toInt()) {
                    val y = random.nextFloat() * height
                    val room = coastAt(y) - 34
                    if (room > 10) {
                        val x = 6 + random.nextFloat() * room
                        moveTo(x, y)
                        quadraticTo(x + 5, y - 4, x + 10, y)
                        quadraticTo(x + 15, y - 4, x + 20, y)
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
                Grain(List((1400 * area).toInt()) { Offset(random.nextFloat() * width, random.nextFloat() * height) }, Color(0xFFE4DCCF).copy(alpha = alpha))
            }
            val borders = WorldMap.borders(campaign).map { y ->
                val down = height - y
                line((coastAt(down).toInt() + 4..width.toInt() step 4).map { x -> Offset(x.toFloat(), down + (14 * sin(x / 90.0)).toFloat()) })
            }
            return WorldArt(width, height, pools, sea, coast, lining, waves, sketch.build(), grain, borders)
        }

        private fun poolColor(biome: String): Color = when (biome) {
            "MINES", "FROST" -> Color(0xFF7FA9C8).copy(alpha = .13f)
            "ASH", "VOLCANO", "CITADEL" -> Color(0xFFD9642E).copy(alpha = .1f)
            "TEMPLE", "VAAL" -> Color(0xFF3A8290).copy(alpha = .1f)
            "CRYPT", "RUINS" -> Color(0xFF8A7AA0).copy(alpha = .08f)
            "DESERT", "CANYON" -> Color(0xFFD8B070).copy(alpha = .1f)
            "JUNGLE", "HIVE" -> Color(0xFF3A8A3E).copy(alpha = .12f)
            "ABYSS", "BLIGHT" -> Color(0xFF6A3AA0).copy(alpha = .12f)
            "SKYREACH", "GLASSWASTE" -> Color(0xFF9FD8E8).copy(alpha = .12f)
            "STORMPEAK", "ASTRAL" -> Color(0xFF5A62E0).copy(alpha = .12f)
            "SUNKEN", "TIDEVAULT" -> Color(0xFF38B8B0).copy(alpha = .12f)
            "CORAL" -> Color(0xFFE86A7A).copy(alpha = .1f)
            "GODHALL" -> Color(0xFFE8C860).copy(alpha = .1f)
            "OBLIVION" -> Color(0xFF6A6A78).copy(alpha = .12f)
            else -> Color(0xFF805E38).copy(alpha = .2f)
        }

        private fun sketchOf(biome: String): SketchKind? = when (biome) {
            "MINES" -> SketchKind.CRYSTAL_HILL

            "FROST" -> SketchKind.SNOW_HILL

            "ASH", "VOLCANO" -> SketchKind.CINDER_HILL

            "FOREST", "JUNGLE" -> SketchKind.TREE

            "MIRE" -> SketchKind.REED

            "RUINS", "TEMPLE" -> SketchKind.COLUMN

            "CRYPT" -> SketchKind.TOMB

            "CAVE", "SHORE" -> SketchKind.ROCK

            "DESERT" -> SketchKind.DUNE

            "CANYON" -> SketchKind.MESA

            "HIVE" -> SketchKind.MOUND

            "CITADEL" -> SketchKind.TOWER

            "ABYSS" -> SketchKind.CHASM

            "BLIGHT" -> SketchKind.SPORE

            // The lands 71–100 (3.43.0): floating isles, glass spires, storm peaks, drowned arches, coral, the vaults'
            // columns, golden shrines, stars and the chasm of oblivion.
            "SKYREACH" -> SketchKind.ISLE

            "GLASSWASTE" -> SketchKind.SPIRE

            "STORMPEAK" -> SketchKind.STORM

            "SUNKEN" -> SketchKind.ARCH

            "CORAL" -> SketchKind.CORAL

            "TIDEVAULT" -> SketchKind.COLUMN

            "GODHALL" -> SketchKind.SHRINE

            "ASTRAL" -> SketchKind.STAR

            "OBLIVION" -> SketchKind.CHASM

            else -> null
        }
    }
}
