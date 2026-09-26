package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max
import kotlin.math.sin

/** One tile as a style sees it: its cell, its centre in the pen's upward measure, and how many of its four sides are rock. */
internal class TileSpot(val x: Int, val y: Int, val cx: Float, val cy: Float, val walls: Int)

/** What every stroke of a frame shares: the pen, half a tile's width, and the scene's clock. */
internal class SceneFrame(val pen: Pen, val unit: Float, val time: Float)

/**
 * How a biome's ground and rock are drawn (2.64.0, the owner's map mockups I, II, III and V).
 *
 * The camera, the tile, the torch and the fog stay the scene's: a style only decides what a floor
 * tile and a block of rock look like, and what hangs in the air over the whole screen. Colours
 * still come from the biome's [Palette], so two biomes of one style differ at a glance.
 */
internal abstract class MapStyle {
    abstract fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float)
    abstract fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float)
    /** Drawn over the finished map, in screen space: drips, fog, embers, fireflies. */
    open fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {}

    /** A stable value in `[0, 1)` per cell and [salt]: the same tile always gets the same crack. */
    protected fun noise(x: Int, y: Int, salt: Int = 0): Float {
        var h = x * 374761393 + y * 668265263 + salt * 1442695041
        h = (h xor (h ushr 13)) * 1274126177
        return ((h xor (h ushr 16)) ushr 8) / 16777216f
    }

    protected fun Pen.diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) =
        quad(cx - halfWidth, cy, cx, cy - halfHeight, cx + halfWidth, cy, cx, cy + halfHeight)

    /** A block standing on the tile, [height] tall: the two faces the camera sees and the cap. */
    protected fun SceneFrame.block(spot: TileSpot, height: Float, left: Color, right: Color, cap: Color) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        val bottom = cy - u / 2
        pen.color = left; pen.quad(cx - u, cy, cx, bottom, cx, bottom + height, cx - u, cy + height)
        pen.color = right; pen.quad(cx, bottom, cx + u, cy, cx + u, cy + height, cx, bottom + height)
        pen.color = cap; pen.diamond(cx, cy + height, u, u / 2)
    }

    /** The cap's two edges that face the camera, where light and glow gather. */
    protected fun SceneFrame.frontEdges(spot: TileSpot, height: Float, color: Color, width: Float) {
        pen.color = color
        pen.polyline(spot.cx - unit, spot.cy + height, spot.cx, spot.cy - unit / 2 + height, spot.cx + unit, spot.cy + height, width = width)
    }

    /**
     * Which of the style's three wall textures a block wears (2.73.0): fixed per cell, so the rock
     * of one map is a mix of kindred faces rather than one repeated.
     */
    protected fun variant(spot: TileSpot) = (noise(spot.x, spot.y, 77) * WALL_VARIANTS).toInt().coerceAtMost(WALL_VARIANTS - 1)

    /** A point on the block's left face: [t] across it from the outer edge, [z] up from the ground. */
    protected fun SceneFrame.left(spot: TileSpot, t: Float, z: Float) = floatArrayOf(spot.cx - unit + t * unit, spot.cy - t * unit / 2 + z)

    /** A point on the block's right face: [t] across it from the front edge, [z] up from the ground. */
    protected fun SceneFrame.right(spot: TileSpot, t: Float, z: Float) = floatArrayOf(spot.cx + t * unit, spot.cy - unit / 2 + t * unit / 2 + z)

    /** A stroke along a face through its points, each a `t` and `z` pair. */
    protected fun SceneFrame.seam(face: (Float, Float) -> FloatArray, width: Float, vararg tz: Float) =
        pen.polyline(*tz.toList().chunked(2).flatMap { (t, z) -> face(t, z).toList() }.toFloatArray(), width = width)

    /** A value spread over the screen by [index] - particles that need no state of their own. */
    protected fun spread(index: Int, salt: Int) = noise(index, salt, 97)
}

/** How many kindred wall textures each style draws (2.73.0). */
private const val WALL_VARIANTS = 3

/** Which style draws which biome: halls of stone, the dark of crypts, the burnt land, the living cave, the sands. */
internal object MapStyles {
    private val wet = WetStone()
    private val runes = RuneDark()
    private val ash = Ashen()
    private val moss = Overgrown()
    private val altar = BloodAltar()
    private val dunes = Dunes()

    fun of(biome: String): MapStyle = when (biome) {
        com.sperance.exileforge.core.campaign.VaalZones.BIOME -> altar
        "CRYPT", "TEMPLE", "ABYSS" -> runes
        "ASH", "VOLCANO" -> ash
        "FOREST", "MIRE", "JUNGLE", "HIVE", "BLIGHT" -> moss
        "DESERT", "CANYON" -> dunes
        else -> wet
    }
}

/** I · Wet stone: flagstones split by mortar, puddles holding the torch, walls laid in courses, drips falling. */
private class WetStone : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.95f + noise(spot.x, spot.y) * .3f) * light)
        pen.diamond(cx, cy, u, u / 2)
        pen.color = Color.Black.copy(alpha = .3f)
        pen.line(cx - u / 2, cy + u / 4, cx + u / 2, cy - u / 4, u * .03f)
        pen.color = Color.Black.copy(alpha = .45f)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .025f)
        if (spot.walls == 0 && noise(spot.x, spot.y, 3) < .12f) {
            val w = u * .55f * (.7f + noise(spot.x, spot.y, 4) * .5f)
            pen.color = tone(palette.accent, .3f * light, alpha = .7f)
            pen.ellipse(cx - w, cy - w / 2, w * 2, w)
            pen.color = Palettes.torch.copy(alpha = light * (.3f + .2f * sin(time * 3f + spot.x)))
            pen.ellipse(cx - w * .55f, cy, w * .7f, w * .2f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * 1.5f
        val grain = .9f + noise(spot.x, spot.y, 2) * .25f
        block(spot, h, tone(palette.wallSide, 1.15f * grain * light, alpha = alpha), tone(palette.wallSide, .75f * grain * light, alpha = alpha),
            tone(palette.wallTop, 1.1f * grain * light, alpha = alpha))
        pen.color = Color.Black.copy(alpha = .45f * alpha)
        val joint = unit * .03f
        when (variant(spot)) {
            // Courses laid level.
            0 -> for (i in 1..2) {
                val lift = h * i / 3f
                pen.polyline(spot.cx - unit, spot.cy + lift, spot.cx, spot.cy - unit / 2 + lift, spot.cx + unit, spot.cy + lift, width = joint)
            }
            // Ashlar: four courses with their joints staggered.
            1 -> for (i in 1..3) {
                val lift = h * i / 4f
                pen.polyline(spot.cx - unit, spot.cy + lift, spot.cx, spot.cy - unit / 2 + lift, spot.cx + unit, spot.cy + lift, width = joint)
                val shift = if (i % 2 == 0) .25f else .6f
                for (face in listOf<(Float, Float) -> FloatArray>({ t, z -> left(spot, t, z) }, { t, z -> right(spot, t, z) }))
                    seam(face, joint, shift, lift - h / 4f, shift, lift)
            }
            // Rubble: odd stones pressed into mortar, a green stain of damp at the foot.
            else -> {
                repeat(4) { k ->
                    val t = .15f + noise(spot.x, spot.y, 40 + k) * .7f
                    val z = h * (.2f + noise(spot.x, spot.y, 44 + k) * .6f)
                    val (x, y) = (if (k % 2 == 0) left(spot, t, z) else right(spot, t, z)).let { it[0] to it[1] }
                    pen.color = tone(palette.wallSide, (if (k % 2 == 0) .85f else .55f) * light, alpha = alpha)
                    pen.ellipse(x - unit * .16f, y - unit * .09f, unit * .32f, unit * .18f)
                    pen.color = Color.Black.copy(alpha = .4f * alpha)
                    pen.line(x - unit * .16f, y - unit * .09f, x + unit * .14f, y - unit * .09f, joint)
                }
                pen.color = Color(0xFF4F6B45).copy(alpha = .35f * light * alpha)
                seam({ t, z -> left(spot, t, z) }, unit * .09f, 0f, unit * .06f, .5f, unit * .1f, 1f, unit * .05f)
            }
        }
        // The wet shine along the cap.
        frontEdges(spot, h, Color(0xFFBED2E1).copy(alpha = .3f * light * alpha), unit * .04f)
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        val drop = h / 90f
        repeat(14) { i ->
            val x = spread(i, 1) * w
            val y = (time * h / 6f + spread(i, 2) * h * 3) % (h * 1.2f)
            scope.drawLine(palette.accent.copy(alpha = .3f), Offset(x, y), Offset(x, y + drop), strokeWidth = 1.5f)
        }
    }
}

/** II · Rune dark: near-black ground where runes breathe, obsidian obelisks with glowing seams, crawling fog. */
private class RuneDark : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.7f + noise(spot.x, spot.y) * .3f) * light)
        pen.diamond(cx, cy, u, u / 2)
        pen.color = palette.accent.copy(alpha = .08f * light)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .02f)
        if (noise(spot.x, spot.y, 5) >= .1f) return
        val a = (.35f + .35f * sin(time * 2f + noise(spot.x, spot.y, 6) * 6f)) * max(.3f, light)
        for ((glow, stroke) in listOf(.3f to .12f, 1f to .04f)) {
            pen.color = palette.accent.copy(alpha = a * glow)
            pen.ring(cx - u * .42f, cy - u * .21f, u * .84f, u * .42f, u * stroke)
            val w = u * stroke
            when ((noise(spot.x, spot.y, 7) * 3).toInt()) {
                0 -> { pen.line(cx, cy - u * .15f, cx, cy + u * .15f, w); pen.line(cx - u * .2f, cy - u * .05f, cx + u * .2f, cy + u * .05f, w) }
                1 -> pen.polyline(cx - u * .25f, cy - u * .1f, cx, cy + u * .15f, cx + u * .25f, cy - u * .1f, width = w)
                else -> { pen.line(cx - u * .2f, cy - u * .1f, cx + u * .2f, cy + u * .1f, w); pen.line(cx + u * .2f, cy - u * .1f, cx - u * .2f, cy + u * .1f, w) }
            }
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.7f + noise(spot.x, spot.y, 8) * .9f)
        block(spot, h, tone(palette.wallSide, .8f * light, alpha = alpha), tone(palette.wallSide, .55f * light, alpha = alpha),
            tone(palette.wallTop, .9f * light, alpha = alpha))
        val a = (.3f + .3f * sin(time * 1.6f + spot.x + spot.y)) * max(.3f, light) * alpha
        when (variant(spot)) {
            // An obelisk whose edge seam glows.
            0 -> if (noise(spot.x, spot.y, 9) < .3f) for ((glow, stroke) in listOf(.3f to .14f, 1f to .04f)) {
                pen.color = palette.accent.copy(alpha = a * glow)
                pen.line(spot.cx, spot.cy - unit / 2 + unit * .1f, spot.cx, spot.cy - unit / 2 + h - unit * .1f, unit * stroke)
            }
            // A carved panel: a rune cut in the right face, breathing.
            1 -> for ((glow, stroke) in listOf(.3f to .12f, 1f to .035f)) {
                pen.color = palette.accent.copy(alpha = a * glow * .8f)
                val right = { t: Float, z: Float -> right(spot, t, z) }
                seam(right, unit * stroke, .5f, h * .25f, .5f, h * .75f)
                seam(right, unit * stroke, .3f, h * .6f, .5f, h * .45f, .7f, h * .6f)
            }
            // Cracked obsidian: a split down the left face, a sliver of light in it.
            else -> {
                pen.color = Color.Black.copy(alpha = .6f * alpha)
                val left = { t: Float, z: Float -> left(spot, t, z) }
                seam(left, unit * .05f, .55f, h * .95f, .4f, h * .65f, .6f, h * .4f, .45f, h * .1f)
                pen.color = palette.accent.copy(alpha = a * .5f)
                seam(left, unit * .015f, .55f, h * .95f, .4f, h * .65f, .6f, h * .4f, .45f, h * .1f)
            }
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        repeat(6) { i ->
            val x = (spread(i, 3) * w * 1.6f + time * w * (.02f + i * .008f)) % (w * 1.6f) - w * .3f
            val y = h * (.25f + spread(i, 4) * .6f)
            val r = w * (.25f + spread(i, 5) * .2f)
            scope.drawCircle(Brush.radialGradient(listOf(Color(0xFF78A0B4).copy(alpha = .1f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
    }
}

/** III · Ash and embers: burnt ground with lava pulsing in its cracks, charred rock with a smouldering rim, sparks rising. */
private class Ashen : MapStyle() {
    private val lava = Color(0xFFFF6A20)
    private val core = Color(0xFFFF8A3C)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.85f + noise(spot.x, spot.y) * .35f) * light)
        pen.diamond(cx, cy, u, u / 2)
        if (noise(spot.x, spot.y, 10) >= .35f) return
        val a = (.45f + .35f * sin(time * 1.3f + noise(spot.x, spot.y, 11) * 9f)) * max(.35f, light)
        val crack = floatArrayOf(cx - u * .6f, cy + (noise(spot.x, spot.y, 12) - .5f) * u * .15f, cx - u * .1f, cy + u * .06f,
            cx + u * .15f, cy - u * .05f, cx + u * .6f, cy + (noise(spot.x, spot.y, 13) - .3f) * u * .15f)
        pen.color = lava.copy(alpha = a * .3f); pen.polyline(*crack, width = u * .12f)
        pen.color = core.copy(alpha = a); pen.polyline(*crack, width = u * .035f)
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.2f + noise(spot.x, spot.y, 14) * .6f)
        block(spot, h, tone(palette.wallSide, 1.1f * light, alpha = alpha), tone(palette.wallSide, .75f * light, alpha = alpha),
            tone(palette.wallTop, 1.05f * light, alpha = alpha))
        val ember = ((.35f + .3f * sin(time * 2f + spot.x * 3)) * light * alpha).coerceIn(0f, 1f)
        frontEdges(spot, h, core.copy(alpha = ember), unit * .05f)
        when (variant(spot)) {
            // Plain charred rock.
            0 -> Unit
            // Basalt: columns split top to bottom.
            1 -> {
                pen.color = Color.Black.copy(alpha = .45f * alpha)
                for (t in listOf(.33f, .66f)) {
                    seam({ a, z -> left(spot, a, z) }, unit * .03f, t, 0f, t, h)
                    seam({ a, z -> right(spot, a, z) }, unit * .03f, t, 0f, t, h)
                }
            }
            // Burning through: a glowing fissure down the left face.
            else -> {
                val left = { t: Float, z: Float -> left(spot, t, z) }
                pen.color = lava.copy(alpha = ember * .35f); seam(left, unit * .12f, .3f, h * .9f, .55f, h * .55f, .4f, h * .15f)
                pen.color = core.copy(alpha = ember); seam(left, unit * .035f, .3f, h * .9f, .55f, h * .55f, .4f, h * .15f)
            }
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        scope.drawRect(Brush.verticalGradient(listOf(Color.Transparent, lava.copy(alpha = .1f)), startY = h * .5f, endY = h))
        val spark = w / 180f
        repeat(40) { i ->
            val life = (time * .25f + spread(i, 6)) % 1f
            val x = spread(i, 7) * w + sin(time + i) * spark * 6
            scope.drawCircle(Color(1f, (120 + 80 * spread(i, 8)) / 255f, 60 / 255f, (1 - life) * .8f), spark, Offset(x, h * (1.05f - life * 1.1f)))
        }
    }
}

/** V · Moss and roots: a living floor with grass swaying, rounded boulders with roots and moss, soft shade by the rock, fireflies. */
private class Overgrown : MapStyle() {
    private val grass = Color(0xFF6F9E4C)
    private val root = Color(0xFF4A3624)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.9f + noise(spot.x, spot.y) * .3f) * light)
        pen.diamond(cx, cy, u, u / 2)
        if (spot.walls > 0) { pen.color = Color.Black.copy(alpha = .1f * spot.walls); pen.diamond(cx, cy, u, u / 2) }
        if (noise(spot.x, spot.y, 16) >= .35f) return
        pen.color = tone(grass, 1.1f * light)
        repeat(4) { k ->
            val bx = cx + (noise(spot.x, spot.y, 17 + k) - .5f) * u
            val by = cy + (noise(spot.x, spot.y, 21 + k) - .5f) * u * .4f
            val sway = sin(time * 2f + bx * .05f) * u * .06f
            pen.polyline(bx, by, bx + sway * .5f, by + u * .2f, bx + sway, by + u * (.35f + .2f * noise(spot.x, spot.y, 25 + k)), width = u * .04f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val (cx, cy) = spot.cx to spot.cy
        val r = unit * (.95f + noise(spot.x, spot.y, 31) * .3f)
        val h = unit * (.9f + noise(spot.x, spot.y, 32) * .5f)
        val half = h * .75f + r * .2f
        pen.color = Color.Black.copy(alpha = .35f * alpha); pen.ellipse(cx - r * 1.05f, cy - r * .5f, r * 2.1f, r)
        pen.color = tone(palette.wallSide, light, alpha = alpha); pen.ellipse(cx - r, cy + h * .45f - half, r * 2, half * 2)
        pen.color = tone(palette.wallTop, 1.2f * light, alpha = alpha * .8f); pen.ellipse(cx - r * .75f, cy + h * .7f, r * 1.1f, h * .6f)
        pen.color = tone(palette.decor, .95f * light, alpha = alpha); pen.ellipse(cx - r * .8f, cy + h * .95f, r * 1.4f, r * .5f)
        when (variant(spot)) {
            // A boulder, now and then with a root over it.
            0 -> if (noise(spot.x, spot.y, 33) < .4f) {
                pen.color = tone(root, light, alpha = alpha)
                pen.polyline(cx + r * .3f, cy + h * .7f, cx + r * .6f, cy + h * .3f, cx + r * .4f, cy + h * .05f, cx + r * .7f, cy - unit * .1f, width = unit * .05f)
            }
            // A second, smaller stone settled on the first, its own moss cap.
            1 -> {
                val small = r * .55f
                pen.color = tone(palette.wallSide, .9f * light, alpha = alpha); pen.ellipse(cx - small * .7f, cy + h * .95f, small * 2, small * 1.1f)
                pen.color = tone(palette.decor, 1.05f * light, alpha = alpha); pen.ellipse(cx - small * .5f, cy + h * .95f + small * .6f, small * 1.4f, small * .45f)
            }
            // Ferns spilling from the moss.
            else -> {
                pen.color = tone(grass, light, alpha = alpha)
                repeat(3) { k ->
                    val bx = cx + (k - 1) * r * .35f
                    val by = cy + h * 1.1f
                    val lean = (k - 1) * r * .25f + sin(time * 1.5f + k) * unit * .03f
                    pen.polyline(bx, by, bx + lean * .5f, by + unit * .25f, bx + lean, by + unit * .4f, width = unit * .05f)
                }
            }
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        val fly = Color(0xFFD2FF8C)
        val dot = w / 220f
        repeat(18) { i ->
            val at = Offset(w * spread(i, 40) + sin(time * .6f + i) * dot * 18, h * spread(i, 41) + sin(time * .5f + i * 2 + 1.6f) * dot * 12)
            val a = .4f + .4f * sin(time * 3f + i)
            scope.drawCircle(fly.copy(alpha = a * .2f), dot * 4, at)
            scope.drawCircle(fly.copy(alpha = a), dot, at)
        }
    }
}

/**
 * VI · Blood altar, the Vaal zones' own (2.65.0, the owner's mockup I): black obsidian where scarlet
 * veins pulse in a wave, pools of blood in the hollows, stepped blocks weeping blood, red haze.
 */
private class BloodAltar : MapStyle() {
    private val vein = Color(0xFFFF3C28)
    private val blood = Color(0xFF5A0406)
    private val weep = Color(0xFFAA0C0C)

    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.85f + noise(spot.x, spot.y) * .35f) * light)
        pen.diamond(cx, cy, u, u / 2)
        pen.color = Color.Black.copy(alpha = .5f)
        pen.polyline(cx - u, cy, cx, cy + u / 2, cx + u, cy, width = u * .025f)
        if (noise(spot.x, spot.y, 3) < .38f) {
            // The veins pulse as one wave rolling across the floor, not each on its own.
            val a = (.4f + .4f * sin(time * 2.2f - (spot.x + spot.y) * .6f)) * max(.4f, light)
            val bend = (noise(spot.x, spot.y, 5) - .5f) * u * .3f
            val across = noise(spot.x, spot.y, 4) < .5f
            val line = if (across) floatArrayOf(cx - u / 2, cy + u / 4, cx + bend, cy, cx + u / 2, cy - u / 4)
                else floatArrayOf(cx - u / 2, cy - u / 4, cx, cy + bend * .6f, cx + u / 2, cy + u / 4)
            pen.color = vein.copy(alpha = a * .3f); pen.polyline(*line, width = u * .12f)
            pen.color = vein.copy(alpha = a); pen.polyline(*line, width = u * .035f)
        }
        if (spot.walls == 0 && noise(spot.x, spot.y, 6) < .07f) {
            val w = u * .5f
            pen.color = tone(blood, light.coerceAtLeast(.5f), alpha = .9f); pen.ellipse(cx - w, cy - w / 2, w * 2, w)
            pen.color = Color(0xFFFF5A46).copy(alpha = (.3f + .15f * sin(time * 3f + spot.x)) * light)
            pen.ellipse(cx - w * .6f, cy, w * .6f, w * .2f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1.3f + noise(spot.x, spot.y, 8) * .5f)
        block(spot, h, tone(palette.wallSide, light, alpha = alpha), tone(palette.wallSide, .65f * light, alpha = alpha), tone(palette.wallTop, light, alpha = alpha))
        val kind = variant(spot)
        // Its teeth: a crown of spikes instead of a step.
        if (kind == 2) {
            pen.color = tone(palette.wallTop, 1.2f * light, alpha = alpha)
            for (t in listOf(.25f, .75f)) {
                val (lx, ly) = left(spot, t, h).let { it[0] to it[1] }
                pen.triangle(lx - unit * .14f, ly, lx, ly + unit * .45f, lx + unit * .14f, ly)
                val (rx, ry) = right(spot, t, h).let { it[0] to it[1] }
                pen.triangle(rx - unit * .14f, ry, rx, ry + unit * .45f, rx + unit * .14f, ry)
            }
            return@with
        }
        // A band of carved glyphs round the block, lit by the veins.
        if (kind == 1) {
            pen.color = vein.copy(alpha = ((.35f + .25f * sin(time * 2.2f - (spot.x + spot.y) * .6f)) * light * alpha).coerceIn(0f, 1f))
            for (face in listOf<(Float, Float) -> FloatArray>({ t, z -> left(spot, t, z) }, { t, z -> right(spot, t, z) }))
                seam(face, unit * .03f, 0f, h * .55f, .2f, h * .65f, .4f, h * .55f, .6f, h * .65f, .8f, h * .55f, 1f, h * .65f)
        }
        // A second, narrower step on top: the altar's terraces.
        val step = TileSpot(spot.x, spot.y, spot.cx, spot.cy + h, spot.walls)
        val inset = unit * .3f
        pen.color = tone(palette.wallSide, 1.1f * light, alpha = alpha)
        pen.quad(step.cx - unit + inset, step.cy, step.cx, step.cy - unit / 2 + inset / 2, step.cx, step.cy - unit / 2 + inset / 2 + unit * .35f, step.cx - unit + inset, step.cy + unit * .35f)
        pen.color = tone(palette.wallSide, .7f * light, alpha = alpha)
        pen.quad(step.cx, step.cy - unit / 2 + inset / 2, step.cx + unit - inset, step.cy, step.cx + unit - inset, step.cy + unit * .35f, step.cx, step.cy - unit / 2 + inset / 2 + unit * .35f)
        pen.color = tone(palette.wallTop, 1.25f * light, alpha = alpha)
        pen.diamond(step.cx, step.cy + unit * .35f, unit - inset, (unit - inset) / 2)
        if (noise(spot.x, spot.y, 9) < .35f) {
            pen.color = weep.copy(alpha = ((.5f + .3f * sin(time * 1.5f + spot.x)) * light * alpha).coerceIn(0f, 1f))
            val x = spot.cx + unit * (.2f + noise(spot.x, spot.y, 10) * .6f)
            val top = spot.cy - unit / 2 + h - unit * .1f
            pen.line(x, top, x, top - h * (.4f + noise(spot.x, spot.y, 11) * .4f), unit * .06f)
        }
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        val centre = Offset(w / 2, h * .55f)
        scope.drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFF500000).copy(alpha = .55f)), centre, max(w, h) * .75f))
        repeat(5) { i ->
            val x = (spread(i, 3) * w * 1.6f + time * w * (.015f + i * .005f)) % (w * 1.6f) - w * .3f
            val y = h * (.2f + spread(i, 4) * .7f)
            val r = w * .3f
            scope.drawCircle(Brush.radialGradient(listOf(Color(0xFFC81414).copy(alpha = .09f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
    }
}

/** VI · Dunes (2.77.0): sand rippled by the wind, sandstone laid in bands, grains blowing across the screen. */
private class Dunes : MapStyle() {
    override fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float): Unit = with(frame) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        pen.color = tone(palette.floor, (.95f + noise(spot.x, spot.y) * .25f) * light)
        pen.diamond(cx, cy, u, u / 2)
        // The wind's hand: two or three soft ripples across the tile.
        pen.color = tone(palette.decor, .9f * light, alpha = .35f)
        val ripples = 2 + (noise(spot.x, spot.y, 40) * 2).toInt()
        repeat(ripples) { k ->
            val y = cy + ((k + 1f) / (ripples + 1) - .5f) * u * .8f + (noise(spot.x, spot.y, 41 + k) - .5f) * u * .1f
            pen.polyline(cx - u * .55f, y, cx, y + u * .08f, cx + u * .55f, y, width = u * .03f)
        }
    }

    override fun wall(frame: SceneFrame, spot: TileSpot, palette: Palette, alpha: Float, light: Float): Unit = with(frame) {
        val h = unit * (1f + noise(spot.x, spot.y, 42) * .7f)
        block(spot, h, tone(palette.wallSide, 1.15f * light, alpha = alpha), tone(palette.wallSide, .8f * light, alpha = alpha),
            tone(palette.wallTop, 1.05f * light, alpha = alpha))
        // Strata: the bands the sandstone was laid in, running across both faces.
        pen.color = tone(palette.wallTop, .75f * light, alpha = .5f * alpha)
        val bands = if (variant(spot) == 0) listOf(.3f, .6f) else listOf(.25f, .5f, .75f)
        bands.forEach { z ->
            seam({ t, lift -> left(spot, t, lift) }, unit * .04f, 0f, h * z, 1f, h * z)
            seam({ t, lift -> right(spot, t, lift) }, unit * .04f, 0f, h * z, 1f, h * z)
        }
        // A sunlit crest on some of the rock.
        if (variant(spot) == 2) frontEdges(spot, h, tone(palette.accent, light, alpha = .35f * alpha), unit * .04f)
    }

    override fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {
        val (w, h) = scope.size.width to scope.size.height
        scope.drawRect(Brush.verticalGradient(listOf(palette.accent.copy(alpha = .06f), Color.Transparent), startY = 0f, endY = h * .6f))
        val grain = w / 260f
        repeat(60) { i ->
            val drift = (time * (.08f + spread(i, 21) * .08f) + spread(i, 22)) % 1f
            val y = spread(i, 23) * h + sin(time * 1.5f + i) * grain * 4
            scope.drawCircle(palette.accent.copy(alpha = .25f + .25f * spread(i, 24)), grain, Offset(drift * w * 1.2f - w * .1f, y))
        }
    }
}
