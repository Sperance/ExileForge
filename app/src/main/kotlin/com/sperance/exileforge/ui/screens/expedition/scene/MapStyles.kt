package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.sperance.exileforge.core.campaign.Liquid
import kotlin.math.sin
import com.sperance.exileforge.core.campaign.MapStyle as Layout

/** One tile as a style sees it: its cell, its centre in the pen's upward measure, and how many of its four sides are rock. */
internal class TileSpot(val x: Int, val y: Int, val cx: Float, val cy: Float, val walls: Int)

/** What every stroke of a frame shares: the pen, half a tile's width, and the scene's clock (декора: стоит без `LocalMotion`). */
internal class SceneFrame(val pen: Pen, var unit: Float, var time: Float)

/** Клетка воды или пропасти, как её видит слой глади (4.2.0): место и свет. */
internal class PoolCell(val spot: TileSpot, val light: Float)

/**
 * How a biome's ground and rock are drawn (2.64.0, the owner's map mockups I, II, III and V).
 *
 * The camera, the tile, the torch and the fog stay the scene's: a style only decides what a floor
 * tile and the rock look like, and what hangs in the air over the whole screen. Colours
 * still come from the biome's [Palette], so two biomes of one style differ at a glance. Скала - контурами массивов
 * ([WallRelief]): стиль даёт им высоту, узор ([texture]), тона и отлив фаски.
 */
internal abstract class MapStyle {
    abstract fun floor(frame: SceneFrame, spot: TileSpot, palette: Palette, light: Float)

    /**
     * Высота массива скалы в полуширинах клетки при его доле [rise] в `[0, 1)` - одна на массив.
     */
    abstract fun wallHeight(rise: Float): Float

    /**
     * Узор контурной стены по раскладке карты (утверждено владельцем, макеты «Стены v2»): залы - «Обточенный камень»,
     * пещеры и пустоши - «Живая скала»; стиль может выбрать свой.
     */
    open fun texture(layout: Layout): WallTexture = if (layout == Layout.HALLS) Ashlar else LiveRock

    /** Тона слоёв стены в палитре биома. */
    open fun wallTones(palette: Palette): WallTones = WallTones.of(palette)

    /**
     * Цвет фаски по кромке куска клетки ([x], [y]) при свете [light]: у стиля - свой отлив (мокрый блеск, тлеющие угли, руны);
     * [time] - часы декора, без `LocalMotion` стоят.
     */
    protected open fun rim(tones: WallTones, palette: Palette, x: Int, y: Int, time: Float, light: Float): Color = tone(tones.rim, light)

    /** Земля под скалой клетки, где не лежит пол: подложка массива. */
    fun wallGround(frame: SceneFrame, cell: WallCell, tones: WallTones, light: Float) {
        cell.plate?.let { frame.pen.scope.drawPath(it, tone(tones.ground, light)) }
    }

    /** Мягкая тень у подножия массива, вперёд к камере: дальний край и ближний, плотнее. */
    fun wallShadow(frame: SceneFrame, cell: WallCell) {
        cell.far?.let { frame.pen.scope.drawPath(it, Color.Black.copy(alpha = SHADOW_FAR)) }
        cell.near?.let { frame.pen.scope.drawPath(it, Color.Black.copy(alpha = SHADOW_NEAR)) }
    }

    /**
     * Кусок массива клетки ([x], [y]) слоями: грани (светлее, что смотрят в `+y`, темнее - в `+x`, сумрак к подножию) и их узор,
     * крышка, фаска, узор крышки, мелочь у подножия. [alpha] - прозрачность «окна» у героя, [light] - свет клетки.
     */
    fun wall(frame: SceneFrame, piece: WallPiece, x: Int, y: Int, tones: WallTones, palette: Palette, alpha: Float, light: Float) {
        val scope = frame.pen.scope
        piece.faces[0]?.let { face ->
            scope.drawPath(face, tone(tones.face, light, alpha = alpha))
            for (k in 1 until piece.faces.size) piece.faces[k]?.let { scope.drawPath(it, Color.Black.copy(alpha = FACE_SHADE * k / (piece.faces.size - 1) * alpha)) }
            piece.dusk?.let { scope.drawPath(face, it, alpha = alpha) }
        }
        marks(frame, piece.faceMarks, tones, alpha, light)
        scope.drawPath(piece.cap, tone(tones.cap, light, alpha = alpha))
        piece.rim?.let { scope.drawPath(it, rim(tones, palette, x, y, frame.time, light).let { c -> c.copy(alpha = c.alpha * alpha) }) }
        marks(frame, piece.capMarks, tones, alpha, light)
        marks(frame, piece.footMarks, tones, alpha, light)
    }

    private fun marks(frame: SceneFrame, marks: Array<WallMark>, tones: WallTones, alpha: Float, light: Float) {
        for (mark in marks) {
            val color = when (mark.ink) {
                WallInk.DIM -> Color.Black.copy(alpha = .2f * alpha)
                WallInk.SHADOW -> Color.Black.copy(alpha = .42f * alpha)
                WallInk.SHEEN -> tone(tones.light, light, alpha = .22f * alpha)
                WallInk.ROOT -> tone(tones.root, light, alpha = alpha)
                WallInk.STONE -> tone(tones.stone, light, alpha = alpha)
                WallInk.GROWTH -> tone(tones.growth, light, alpha = alpha)
                WallInk.LIGHT -> tone(tones.light, light, alpha = .85f * alpha)
                WallInk.BLOOM -> tone(tones.bloom, light, alpha = .9f * alpha)
            }
            frame.pen.scope.drawPath(mark.path, color)
        }
    }

    /**
     * A chasm (3.91.0): the ground's rim around a sunken dark, the biome's accent glimmering at the bottom - a pit, a crack, a
     * sinkhole, the same hole in every biome's colours. Seen across, never walked. С 4.2.0 соседние клетки - одна пропасть по
     * контуру [outline]; край - только снаружи.
     */
    open fun chasm(frame: SceneFrame, cells: List<PoolCell>, shade: List<PoolCell>, outline: Path, palette: Palette): Unit = with(frame) {
        // Глубина (4.7.0): дальняя стенка провала - тёмный камень, сама тьма сдвинута к зрителю, по краю - светлая кромка; яма
        // читается и на тёмном полу
        val face = tone(palette.floor, .42f)
        pool(cells, shade, outline, { tone(palette.floor, .9f * it) }, face, CHASM_LIP, depth = {
            pen.scope.translate(0f, unit * CHASM_DEPTH) { drawPath(outline, tone(palette.void, .95f)) }
        }) { cell ->
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
    protected fun SceneFrame.pool(
        cells: List<PoolCell>,
        shade: List<PoolCell>,
        outline: Path,
        rim: (Float) -> Color,
        fill: Color,
        edge: Color,
        depth: (() -> Unit)? = null,
        inside: (PoolCell) -> Unit,
    ) {
        cells.forEach { cell ->
            pen.color = rim(cell.light)
            pen.diamond(cell.spot.cx, cell.spot.cy, unit, unit / 2)
        }
        pen.scope.drawPath(outline, fill)
        pen.scope.clipPath(outline) {
            depth?.invoke()
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

    /**
     * Мягкое пятно кистью [brush] единичного круга (`unitGlow`) в ([x], [y]) радиуса [r]: кисть одна на стиль, кадр её только
     * растягивает переносом.
     */
    protected fun DrawScope.blot(brush: Brush, x: Float, y: Float, r: Float) = withTransform({
        translate(x, y)
        scale(r, r, Offset.Zero)
    }) { drawCircle(brush, 1f, Offset.Zero) }

    /**
     * Полоса кисти [brush] единичной высоты (`unitFade`) от [startY] до [endY] во всю ширину экрана: до и после - крайние цвета,
     * как у вертикального градиента.
     */
    protected fun DrawScope.band(brush: Brush, startY: Float, endY: Float) {
        val span = (endY - startY).takeIf { it != 0f } ?: return
        withTransform({
            translate(0f, startY)
            scale(1f, span, Offset.Zero)
        }) { drawRect(brush, Offset(0f, -startY / span), Size(size.width, size.height / span)) }
    }

    /** A stable value in `[0, 1)` per cell and [salt]: the same tile always gets the same crack. */
    protected fun noise(x: Int, y: Int, salt: Int = 0): Float = cellNoise(x, y, salt)

    protected fun Pen.diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) = quad(cx - halfWidth, cy, cx, cy - halfHeight, cx + halfWidth, cy, cx, cy + halfHeight)

    /** A value spread over the screen by [index] - particles that need no state of their own. */
    protected fun spread(index: Int, salt: Int) = noise(index, salt, 97)
}

/** Кисть пятна: [inner] в центре круга радиуса 1, [outer] к краю - для [MapStyle.blot]. */
internal fun unitGlow(inner: Color, outer: Color = Color.Transparent): Brush = Brush.radialGradient(listOf(inner, outer), Offset.Zero, 1f)

/** Кисть полосы: [top] сверху, [bottom] снизу на единичной высоте - для [MapStyle.band]. */
internal fun unitFade(top: Color, bottom: Color): Brush = Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = 1f)

/** Кисть, что зависит от палитры биома: собирается заново лишь со сменой палитры. */
internal class PaletteBrush(private val make: (Palette) -> Brush) {
    private var palette: Palette? = null
    private var brush: Brush? = null

    operator fun invoke(of: Palette): Brush {
        if (of !== palette) {
            palette = of
            brush = make(of)
        }
        return brush!!
    }
}

/** Тень грани, что смотрит в `+x`, против освещённой, что смотрит в `+y`. */
private const val FACE_SHADE = .36f

/** Глубина пропасти (4.7.0): на сколько клетки тьма сдвинута к зрителю - видна дальняя стенка. */
private const val CHASM_DEPTH = .24f

/** Светлая кромка края пропасти (4.7.0). */
private val CHASM_LIP = Color(0xFFA89878).copy(alpha = .6f)

/** Мягкая тень у подножия: дальний край и ближний. */
private const val SHADOW_FAR = .18f
private const val SHADOW_NEAR = .24f

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
