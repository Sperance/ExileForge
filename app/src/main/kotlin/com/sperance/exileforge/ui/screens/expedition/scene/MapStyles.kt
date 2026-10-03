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

    protected fun Pen.diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) = quad(cx - halfWidth, cy, cx, cy - halfHeight, cx + halfWidth, cy, cx, cy + halfHeight)

    /** A block standing on the tile, [height] tall: the two faces the camera sees and the cap. */
    protected fun SceneFrame.block(spot: TileSpot, height: Float, left: Color, right: Color, cap: Color) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        val bottom = cy - u / 2
        pen.color = left
        pen.quad(cx - u, cy, cx, bottom, cx, bottom + height, cx - u, cy + height)
        pen.color = right
        pen.quad(cx, bottom, cx + u, cy, cx + u, cy + height, cx, bottom + height)
        pen.color = cap
        pen.diamond(cx, cy + height, u, u / 2)
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
    protected fun SceneFrame.seam(face: (Float, Float) -> FloatArray, width: Float, vararg tz: Float) = pen.polyline(*tz.toList().chunked(2).flatMap { (t, z) -> face(t, z).toList() }.toFloatArray(), width = width)

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
    private val sky = SkyGlass()
    private val drowned = Drowned()
    private val divine = Divine()

    fun of(biome: String): MapStyle = when (biome) {
        com.sperance.exileforge.core.campaign.VaalZones.BIOME -> altar

        "CRYPT", "TEMPLE", "ABYSS" -> runes

        "ASH", "VOLCANO" -> ash

        "FOREST", "MIRE", "JUNGLE", "HIVE", "BLIGHT" -> moss

        "DESERT", "CANYON" -> dunes

        // The lands 71–100 (3.43.0): the broken sky, the drowned empire, the halls of the gods.
        "SKYREACH", "GLASSWASTE", "STORMPEAK" -> sky

        "SUNKEN", "CORAL", "TIDEVAULT" -> drowned

        "GODHALL", "ASTRAL", "OBLIVION" -> divine

        else -> wet
    }
}
