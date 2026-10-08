package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import com.sperance.exileforge.core.campaign.Liquid
import kotlin.math.sin

/**
 * One tile as a style sees it: its cell, its centre in the pen's upward measure, and how many of its four sides are rock. У скалы
 * (4.2.0) ещё [faces] - какие грани видны камере (сосед там не скала) - и [rise], высота её массива в `[0, 1)`, одна на массив.
 */
internal class TileSpot(val x: Int, val y: Int, val cx: Float, val cy: Float, val walls: Int, val faces: Int = Face.ALL, val rise: Float = 0f) {
    fun shows(face: Face) = faces and face.bit != 0
}

/**
 * Грань блока скалы, обращённая к камере (4.2.0): левая смотрит на клетку `y + 1`, правая - на `x + 1`. Грань рисуется, только
 * где сосед не скала - внутри массива граней и швов нет.
 */
internal enum class Face(val bit: Int, val dx: Int, val dy: Int) {
    LEFT(1, 0, 1),
    RIGHT(2, 1, 0),
    ;

    companion object {
        const val ALL = 3
    }
}

/** What every stroke of a frame shares: the pen, half a tile's width, and the scene's clock (декора: стоит без `LocalMotion`). */
internal class SceneFrame(val pen: Pen, val unit: Float, val time: Float)

/** Клетка воды или пропасти, как её видит слой глади (4.2.0): место и свет. */
internal class PoolCell(val spot: TileSpot, val light: Float)

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

    /**
     * A chasm (3.91.0): the ground's rim around a sunken dark, the biome's accent glimmering at the bottom - a pit, a crack, a
     * sinkhole, the same hole in every biome's colours. Seen across, never walked. С 4.2.0 соседние клетки - одна пропасть по
     * контуру [outline]; край - только снаружи.
     */
    open fun chasm(frame: SceneFrame, cells: List<PoolCell>, shade: List<PoolCell>, outline: Path, palette: Palette): Unit = with(frame) {
        pool(cells, shade, outline, { tone(palette.floor, .8f * it) }, tone(palette.void, .9f), Color.Black.copy(alpha = .5f)) { cell ->
            val (cx, cy, u) = Triple(cell.spot.cx, cell.spot.cy, unit)
            pen.color = tone(palette.accent, .35f * cell.light, alpha = .35f + .1f * sin(time * 2f + cell.spot.x + cell.spot.y))
            pen.diamond(cx, cy - u * .1f, u * .3f, u * .12f)
        }
    }

    /**
     * Вода (3.95.0): берег цвета пола, гладь цвета воды биома и блики, бегущие по течению; у лавы и тьмы - своё свечение. С 4.2.0
     * соседние клетки - одна гладь по контуру [outline] со скруглёнными углами: берег только по внешнему краю, блики по всей
     * площади, светлая кромка у берега.
     */
    open fun water(frame: SceneFrame, cells: List<PoolCell>, shade: List<PoolCell>, outline: Path, palette: Palette, liquid: Liquid): Unit = with(frame) {
        val look = LiquidLook.of(liquid)
        // Гладь светлее у берега: кромка по всему контуру изнутри
        pool(cells, shade, outline, { tone(palette.floor, .75f * it) }, lerp(look.deep, look.surface, .45f), look.surface.copy(alpha = .5f)) { cell ->
            // Блик течения: по диагонали клетки, со сдвигом по времени и месту - соседние клетки текут одной волной
            val (cx, cy, u) = Triple(cell.spot.cx, cell.spot.cy, unit)
            val wave = ((time * look.flow + (cell.spot.x + cell.spot.y) * .37f) % 1f)
            pen.color = look.glint.copy(alpha = look.glintAlpha * sin(wave * Math.PI.toFloat()))
            pen.diamond(cx - u * .5f + u * wave, cy - u * .03f, u * .18f, u * .05f)
        }
    }

    /**
     * Слой глади (4.2.0): край [rim] по клеткам [cells] - он остаётся виден лишь снаружи контура, - заливка [fill] по контуру, затем
     * внутри контура - кромка [edge] вдоль берега, [inside] каждой клетки и тень света клеток [shade] (клетки глади и суши рядом,
     * куда заходит скруглённый угол).
     */
    protected fun SceneFrame.pool(cells: List<PoolCell>, shade: List<PoolCell>, outline: Path, rim: (Float) -> Color, fill: Color, edge: Color, inside: (PoolCell) -> Unit) {
        cells.forEach { cell ->
            pen.color = rim(cell.light)
            pen.diamond(cell.spot.cx, cell.spot.cy, unit, unit / 2)
        }
        pen.scope.drawPath(outline, fill)
        pen.scope.clipPath(outline) {
            drawPath(outline, edge, style = Stroke(unit * .16f, join = StrokeJoin.Round))
            cells.forEach(inside)
            shade.forEach { cell ->
                pen.color = Color.Black.copy(alpha = (1f - cell.light).coerceIn(0f, 1f))
                pen.diamond(cell.spot.cx, cell.spot.cy, unit, unit / 2)
            }
        }
    }

    /** Drawn over the finished map, in screen space: fog, embers, fireflies; [time] - часы декора, без `LocalMotion` стоят. */
    open fun atmosphere(scope: DrawScope, palette: Palette, time: Float) {}

    /** A stable value in `[0, 1)` per cell and [salt]: the same tile always gets the same crack. */
    protected fun noise(x: Int, y: Int, salt: Int = 0): Float = cellNoise(x, y, salt)

    protected fun Pen.diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) = quad(cx - halfWidth, cy, cx, cy - halfHeight, cx + halfWidth, cy, cx, cy + halfHeight)

    /**
     * A block standing on the tile, [height] tall: the faces the camera sees and the cap. Автотайлинг (4.2.0): грань - только
     * где сосед не скала ([TileSpot.faces]), высота - одна на массив, так что крышки соседних блоков сходятся без швов. Крышка
     * чуть шире клетки - сглаживание краёв не оставляет щелей между крышками.
     */
    protected fun SceneFrame.block(spot: TileSpot, height: Float, left: Color, right: Color, cap: Color) {
        val (cx, cy, u) = Triple(spot.cx, spot.cy, unit)
        val bottom = cy - u / 2
        if (spot.shows(Face.LEFT)) {
            pen.color = left
            pen.quad(cx - u, cy, cx, bottom, cx, bottom + height, cx - u, cy + height)
        }
        if (spot.shows(Face.RIGHT)) {
            pen.color = right
            pen.quad(cx, bottom, cx + u, cy, cx + u, cy + height, cx, bottom + height)
        }
        pen.color = cap
        val seal = if (cap.alpha >= 1f) CAP_SEAL else 0f
        pen.diamond(cx, cy + height, u + seal, u / 2 + seal / 2)
    }

    /** The cap's edges that face the camera, where light and glow gather - только над видимыми гранями (4.2.0). */
    protected fun SceneFrame.frontEdges(spot: TileSpot, height: Float, color: Color, width: Float) {
        pen.color = color
        if (spot.shows(Face.LEFT)) pen.line(spot.cx - unit, spot.cy + height, spot.cx, spot.cy - unit / 2 + height, width)
        if (spot.shows(Face.RIGHT)) pen.line(spot.cx, spot.cy - unit / 2 + height, spot.cx + unit, spot.cy + height, width)
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

    /** Точка на грани [face] блока: [t] поперёк грани, [z] вверх от земли. */
    protected fun SceneFrame.on(face: Face, spot: TileSpot, t: Float, z: Float) = if (face == Face.LEFT) left(spot, t, z) else right(spot, t, z)

    /**
     * A stroke along a face through its points, each a `t` and `z` pair. Скрытая грань (4.2.0) - внутри массива - не несёт ни
     * швов, ни узора.
     */
    protected fun SceneFrame.seam(spot: TileSpot, face: Face, width: Float, vararg tz: Float) {
        if (!spot.shows(face)) return
        pen.polyline(*tz.toList().chunked(2).flatMap { (t, z) -> on(face, spot, t, z).toList() }.toFloatArray(), width = width)
    }

    /** A value spread over the screen by [index] - particles that need no state of their own. */
    protected fun spread(index: Int, salt: Int) = noise(index, salt, 97)
}

/** How many kindred wall textures each style draws (2.73.0). */
private const val WALL_VARIANTS = 3

/** Запас крышки за край клетки в пикселях (4.2.0): соседние крышки перекрываются и не светят щелью сглаживания. */
private const val CAP_SEAL = .6f

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

/** Цвета воды биома (3.95.0): глубина, гладь, блик и как быстро бежит течение. */
internal class LiquidLook(val deep: Color, val surface: Color, val glint: Color, val glintAlpha: Float, val flow: Float) {
    companion object {
        private val looks = mapOf(
            Liquid.WATER to LiquidLook(Color(0xFF0B2A3C), Color(0xFF2F6F8F), Color(0xFFBFEAFF), .7f, .35f),
            Liquid.SWAMP to LiquidLook(Color(0xFF1A2414), Color(0xFF3F5A2A), Color(0xFFC8E8A0), .45f, .12f),
            Liquid.LAVA to LiquidLook(Color(0xFF4A1206), Color(0xFFD9541C), Color(0xFFFFD27A), .9f, .2f),
            Liquid.ICE to LiquidLook(Color(0xFF2A4A60), Color(0xFF9CC8E0), Color(0xFFFFFFFF), .6f, .05f),
            Liquid.TAR to LiquidLook(Color(0xFF14110A), Color(0xFF3A3220), Color(0xFFB0A060), .35f, .08f),
            Liquid.VOID to LiquidLook(Color(0xFF0A0614), Color(0xFF2A1848), Color(0xFFC88AF0), .6f, .15f),
        )

        fun of(liquid: Liquid): LiquidLook = looks.getValue(liquid)
    }
}

/** Цвет воды на миникарте (3.95.0). */
internal fun liquidTint(liquid: Liquid): Color = LiquidLook.of(liquid).surface
