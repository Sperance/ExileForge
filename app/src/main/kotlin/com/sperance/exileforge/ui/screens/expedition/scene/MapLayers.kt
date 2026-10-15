package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntSize
import com.sperance.exileforge.core.campaign.Liquid
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Земля и скала карты слоями: кадр не рисует их заново, а переносит готовые записи. Пол с подложкой скалы ([drawUnder]),
 * мелочь пола с тенью у подножия ([drawOver]) и скала кусками по глубине ([drawWalls]) записываются, когда герой сменил
 * клетку (свет считается по клеткам, без мерцания факела), когда сменились карта, её правка, стиль, масштаб или экран, а
 * скала - ещё и когда сменились «окна» вокруг монстров, что гонятся за героем. Узор пола, отлив фаски и мелочь пола в слое
 * стоят; гладь вод и пропастей течёт вживую по клеткам, что запомнил слой ([drawPools]).
 *
 * Скала нарезана слоями по целой части глубины кусков: слой встаёт в очередь стоящего глубиной первого куска, так что жетон
 * за скалой остаётся за ней, а жетон в той же полосе глубины ложится поверх. Слои берутся у окна ([graphics]) и отдаются
 * ему в [release].
 */
internal class MapLayers {
    /** Окно, у которого слои берутся: его даёт сцена. */
    var graphics: GraphicsContext? = null

    private var under: GraphicsLayer? = null
    private var over: GraphicsLayer? = null
    private val walls = ArrayList<GraphicsLayer>()
    private var depths = DoubleArray(WALL_LAYERS)

    /** Сколько слоёв скалы записано сейчас. */
    var wallCount = 0
        private set

    /** Левый верхний угол записи в мере холста сцены (до сдвига камеры). */
    private var left = 0f
    private var top = 0f

    /** Гладь вод и пропастей: клетки и их свет из последней записи. */
    private val pools = ArrayList<MapPool>()

    /** Перо записи: часы декора стоят на нуле - записанный узор не движется. */
    private var still: SceneFrame? = null

    private var cell = Long.MIN_VALUE
    private var relief: MapRelief? = null
    private var rock: WallRelief? = null
    private var style: MapStyle? = null
    private var palette: Palette? = null
    private var light = -1.0
    private var window: CellWindow? = null
    private var windows = 0L

    /** Запись устарела - пишется заново; кадр без перемен её только переносит. */
    fun refresh(painter: ScenePainter, scope: DrawScope, world: ExpeditionWorld, shape: MapRelief, rock: WallRelief, style: MapStyle, palette: Palette, biome: String, hx: Int, hy: Int) {
        val graphics = graphics ?: return
        val unit = painter.unit
        val width = scope.size.width
        // Окрестность - под углами экрана камеры (0.14.4, #31), с запасом на высоту скалы и шаг героя внутри клетки
        val seen = SceneCamera.window(scope.size, unit, RISE.toInt(), CELL_SLACK)
        // Камера ходит внутри клетки: ширина полосы - с запасом на клетку
        val across = (width / 2 / unit + 3).toDouble()
        val here = (hx.toLong() shl 32) or (hy.toLong() and 0xFFFFFFFFL)
        val ground = here != cell || shape !== relief || rock !== this.rock || style !== this.style || palette !== this.palette || world.lightRadius != light || seen != window
        val engaged = engaged(world)
        if (!ground && engaged == windows) return
        cell = here
        relief = shape
        this.rock = rock
        this.style = style
        this.palette = palette
        light = world.lightRadius
        window = seen
        windows = engaged
        val map = world.map
        val (xs, ys) = seen.around(hx, hy, map.width, map.height)
        if (xs.isEmpty() || ys.isEmpty()) return
        // Рамка записи: изометрия углов окрестности и запас на высоту скалы и мелочи
        left = ((xs.first - ys.last) * unit) - unit * MARGIN
        top = ((xs.first + ys.first) * unit / 2) - unit * (MARGIN + RISE)
        val right = ((xs.last + 1 - ys.first) * unit) + unit * MARGIN
        val bottom = ((xs.last + ys.last + 2) * unit / 2) + unit * MARGIN
        val size = IntSize(ceil(right - left).toInt(), ceil(bottom - top).toInt())
        val frame = still?.takeIf { it.pen === painter.pen } ?: SceneFrame(painter.pen, unit, 0f).also { still = it }
        frame.unit = unit
        frame.time = 0f
        val sight = Sight(world, rock, hx - hy, across)
        val pen = painter.pen
        val drawing = pen.scope
        val clock = painter.time
        // Записанное стоит: часы сцены на время записи - на нуле (как без `LocalMotion`)
        painter.time = 0f
        try {
            if (ground) {
                val below = under ?: graphics.createGraphicsLayer().also { under = it }
                below.record(scope, scope.layoutDirection, size) {
                    pen.scope = this
                    translate(-left, -top) {
                        // The ground first, all of it: nothing stands below the floor.
                        for (y in ys) for (x in xs) if (map.walkable(x, y) && sight.visible(x, y)) style.floor(frame, painter.spot(map, x, y), palette, painter.glow(x, y))
                        // Земля под скалой: подложка массива, где нет пола
                        for (y in ys) for (x in xs) rock.cell(x, y)?.takeIf { it.plate != null && sight.shown(x, y) }?.let { style.wallGround(frame, it, rock.tones, painter.rockGlow(rock, x, y)) }
                    }
                }
                pools(painter, world, xs, ys, sight)
                val above = over ?: graphics.createGraphicsLayer().also { over = it }
                above.record(scope, scope.layoutDirection, size) {
                    pen.scope = this
                    translate(-left, -top) {
                        for (y in ys) for (x in xs) if (map.walkable(x, y) && sight.visible(x, y)) painter.decor(map, x, y, palette, biome, painter.glow(x, y))
                        // Мягкая тень у подножия скалы ложится на пол и его мелочь
                        for (y in ys) for (x in xs) rock.cell(x, y)?.takeIf { sight.shown(x, y) }?.let { style.wallShadow(frame, it) }
                    }
                }
            }
            walls(painter, scope, graphics, world, rock, style, palette, xs, ys, sight, size, frame, hx, hy)
        } finally {
            painter.time = clock
            pen.scope = drawing
        }
    }

    fun drawUnder(scope: DrawScope) {
        under?.let { scope.translate(left, top) { drawLayer(it) } }
    }

    fun drawOver(scope: DrawScope) {
        over?.let { scope.translate(left, top) { drawLayer(it) } }
    }

    /** Слой скалы [i] в очереди стоящего. */
    fun drawWalls(scope: DrawScope, i: Int) {
        scope.translate(left, top) { drawLayer(walls[i]) }
    }

    /** Глубина слоя скалы [i]: его первого куска. */
    fun wallDepth(i: Int): Double = depths[i]

    /**
     * Гладь (3.91.0, 3.95.0; с 4.2.0 - одна по контуру карты): пропасти уходят в землю, воды - вровень с полом; неувиденное под
     * гладью закрыто тьмой. Клетки и свет - из записи, течение - вживую на часах декора [frame].
     */
    fun drawPools(painter: ScenePainter, frame: SceneFrame, shape: MapRelief, style: MapStyle, palette: Palette, liquid: Liquid?) {
        val unit = painter.unit
        for (i in pools.indices) {
            val pool = pools[i]
            val outline = shape.outline(pool.tile, unit)
            if (pool.tile == Tile.WATER) {
                style.water(frame, pool.cells, pool.shade, outline, palette, liquid ?: continue)
            } else {
                style.chasm(frame, pool.cells, pool.shade, outline, palette)
            }
            painter.pen.color = palette.void
            for (k in 0 until pool.hidden step 2) painter.diamond(pool.hide[k], pool.hide[k + 1], unit, unit / 2)
        }
    }

    fun release() {
        val graphics = graphics ?: return
        under?.let(graphics::releaseGraphicsLayer)
        over?.let(graphics::releaseGraphicsLayer)
        walls.forEach(graphics::releaseGraphicsLayer)
        under = null
        over = null
        walls.clear()
        wallCount = 0
        cell = Long.MIN_VALUE
    }

    private fun pools(painter: ScenePainter, world: ExpeditionWorld, xs: IntRange, ys: IntRange, sight: Sight) {
        pools.clear()
        val map = world.map
        fun cells(tile: Tile, near: Boolean) = buildList {
            for (y in ys) {
                for (x in xs) {
                    if (!sight.visible(x, y)) continue
                    val hit = map.tile(x, y) == tile || near && nearby { dx, dy -> map.tile(x + dx, y + dy) == tile }
                    if (hit) add(PoolCell(painter.spot(map, x, y), painter.glow(x, y)))
                }
            }
        }
        for (tile in POOLS) {
            if (tile == Tile.WATER && map.liquid == null) continue
            val cells = cells(tile, near = false)
            if (cells.isEmpty()) continue
            val hide = ArrayList<Float>()
            for (y in ys) {
                for (x in xs) {
                    if (world.explored(x, y) || !nearby { dx, dy -> map.tile(x + dx, y + dy) == tile }) continue
                    hide += painter.isoX(x + .5, y + .5)
                    hide += painter.isoY(x + .5, y + .5)
                }
            }
            pools += MapPool(tile, cells, cells(tile, near = true), hide.toFloatArray())
        }
    }

    private fun walls(
        painter: ScenePainter,
        scope: DrawScope,
        graphics: GraphicsContext,
        world: ExpeditionWorld,
        rock: WallRelief,
        style: MapStyle,
        palette: Palette,
        xs: IntRange,
        ys: IntRange,
        sight: Sight,
        size: IntSize,
        frame: SceneFrame,
        hx: Int,
        hy: Int,
    ) {
        // Rock between the hero and the player is see-through, or a corridor would hide them. С 4.2.0 - круглое «окно» вокруг
        // героя, монстров, что гонятся за ним, и закрытых скалой ориентиров (3.95.3): скала перед ними прозрачна к центру; с
        // записью слоями - вокруг середины их клеток
        val windows = buildList {
            add(painter.Window(hx + .5, hy + .5))
            world.agents.filter { it.alive && it.mode in ENGAGED && world.lit(floor(it.x).toInt(), floor(it.y).toInt()) }
                .forEach { add(painter.Window(floor(it.x) + .5, floor(it.y) + .5)) }
            world.screened().forEach { add(painter.Window(it.x + .5, it.y + .5)) }
        }
        // Скала - кусками массива по клеткам (контур один на массив), по глубине; слой - куски одной целой части глубины
        val pieces = buildList {
            for (y in ys) {
                for (x in xs) {
                    val piece = rock.cell(x, y)?.piece ?: continue
                    if (sight.shown(x, y)) add(Placed(piece, x, y))
                }
            }
        }.sortedBy { it.piece.depth }
        wallCount = 0
        var from = 0
        while (from < pieces.size) {
            val band = floor(pieces[from].piece.depth)
            var to = from
            while (to < pieces.size && floor(pieces[to].piece.depth) == band) to++
            val layer = walls.getOrNull(wallCount) ?: graphics.createGraphicsLayer().also { walls += it }
            if (wallCount == depths.size) depths = depths.copyOf(depths.size * 2)
            depths[wallCount] = pieces[from].piece.depth
            val first = from
            layer.record(scope, scope.layoutDirection, size) {
                painter.pen.scope = this
                translate(-left, -top) {
                    for (k in first until to) {
                        val (piece, x, y) = pieces[k]
                        val alpha = windows.minOf { it.alpha(x, y) }
                        style.wall(frame, piece, x, y, rock.tones, palette, alpha, painter.rockGlow(rock, x, y))
                    }
                }
            }
            wallCount++
            from = to
        }
    }

    /** Ключ «окон» вокруг монстров, что гонятся за героем: по клетке каждого, без порядка и без выделений. */
    private fun engaged(world: ExpeditionWorld): Long {
        var key = 0L
        for (agent in world.agents) {
            if (!agent.alive || agent.mode !in ENGAGED) continue
            val x = floor(agent.x).toInt()
            val y = floor(agent.y).toInt()
            if (!world.lit(x, y)) continue
            key += (((x.toLong() shl 32) xor (y.toLong() and 0xFFFFFFFFL)) + 1) * MIX
        }
        return key
    }

    /** Что видно с клетки героя: полоса экрана и увиденное; глубокая скала - вместе с видимым соседом. */
    private class Sight(private val world: ExpeditionWorld, private val rock: WallRelief, private val diagonal: Int, private val across: Double) {
        fun visible(x: Int, y: Int) = world.explored(x, y) && abs((x - y) - diagonal) < across

        fun shown(x: Int, y: Int) = visible(x, y) || rock.deep(x, y) && nearby { dx, dy -> visible(x + dx, y + dy) }
    }

    /** Кусок скалы клетки ([x], [y]) в записи. */
    private data class Placed(val piece: WallPiece, val x: Int, val y: Int)

    /** Гладь [tile]: клетки, тень света рядом и середины неувиденных клеток у берега, что закрывает тьма. */
    private class MapPool(val tile: Tile, val cells: List<PoolCell>, val shade: List<PoolCell>, val hide: FloatArray) {
        val hidden: Int get() = hide.size
    }

    private companion object {
        /** Запас рамки записи по краям и над скалой, в полуширинах клетки. */
        const val MARGIN = 3f
        const val RISE = 4f

        /** Запас окрестности в клетках (#31): шаг героя внутри клетки до перезаписи и округление углов экрана. */
        const val CELL_SLACK = 2
        const val WALL_LAYERS = 64
        const val MIX = -7046029254386353131L
        val POOLS = listOf(Tile.CHASM, Tile.WATER)
    }
}
