package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.MapShape
import com.sperance.exileforge.rules.content.TerrainRule
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * A cell on the grid: rock, ground a hero can stand on, or a chasm - a pit, a crack or a sinkhole the hero sees across
 * but never steps into; и вода (реки и озёра, у иных биомов - лава, лёд, смола, тьма): через неё видно, пройти нельзя.
 */
enum class Tile { WALL, FLOOR, CHASM, WATER }

/** Чем полны низины биома: от этого - только вид воды на карте. */
enum class Liquid { WATER, SWAMP, LAVA, ICE, TAR, VOID }

data class Cell(val x: Int, val y: Int)

/** Узор стен (облик скалы): живая скала пещер или обточенный камень залов - формы комнат ([MapShape.ROOMS]). */
enum class MapStyle { CAVERN, HALLS }

/** How a biome's chasms look: round pits and sinkholes, or long narrow cracks. */
enum class ChasmShape { PIT, CRACK }

/**
 * A generated map: the grid, where the hero starts, where the exit is and where monsters stand.
 *
 * [decor] is a drawing on the ground and nothing else — a rock, a root, a crystal — so a map reads
 * as a place; it never blocks a step.
 */
class ExpeditionMap(
    val width: Int,
    val height: Int,
    private val tiles: Array<Tile>,
    val decor: IntArray,
    val start: Cell,
    val exit: Cell,
    val spawns: List<Cell>,
    /** Чем полна вода карты; null - воды нет. */
    val liquid: Liquid? = null,
    /**
     * Ворота арены кольца: клетки скалы между кольцом и ареной босса; пока доля стай не зачищена, они стоят
     * скалой, затем [openGate] делает их полом. Пусто - ворот нет.
     */
    gate: List<Cell> = emptyList(),
) {
    /** Клетки ворот, что ещё стоят скалой. */
    var gate: List<Cell> = gate
        private set

    /** Ворота открываются: их клетки становятся полом - правка карты, сцена перерисует скалу. */
    internal fun openGate() {
        gate.forEach { carve(it, Tile.FLOOR) }
        gate = emptyList()
    }

    fun tile(x: Int, y: Int): Tile = if (x in 0 until width && y in 0 until height) tiles[y * width + x] else Tile.WALL
    fun walkable(x: Int, y: Int) = tile(x, y) == Tile.FLOOR

    /** Whether sight passes over the cell: ground, chasms and water do, rock does not. */
    fun clear(x: Int, y: Int) = tile(x, y) != Tile.WALL
    fun decorAt(x: Int, y: Int): Int = if (x in 0 until width && y in 0 until height) decor[y * width + x] else 0
    val floor: Int get() = tiles.count { it == Tile.FLOOR }

    /** Правка карты: растёт с каждой вырезанной клеткой - по ней сцена пересчитывает то, что держит на карту. */
    var revision: Int = 0
        private set

    /** Клетка становится [tile]: объекты карты вырезают комнату за стеной, а открытая стена или дверь - пол. */
    internal fun carve(cell: Cell, tile: Tile) {
        if (cell.x in 0 until width && cell.y in 0 until height && tiles[cell.y * width + cell.x] != tile) {
            tiles[cell.y * width + cell.x] = tile
            revision++
        }
    }
}

/**
 * Every map of a run is new: carved from a seed, so the same seed is the same map.
 *
 * The biome decides the carving — a crypt, a temple, ruins, mines and a citadel are halls joined by
 * corridors, everything else a cavern dug by wandering walkers — and the map is then trimmed to
 * the one region the start can reach, so an exit or a monster is never sealed in rock. The exit is
 * the farthest reachable cell from the start; monsters stand away from both. Chasms are cut last, away from the start,
 * the exit and the monsters, and only where every piece of ground stays reachable.
 */
object MapGenerator {

    /** Узор стен формы [shape]: у комнат - обточенный камень, у прочих - живая скала. */
    fun styleOf(shape: MapShape): MapStyle = if (shape == MapShape.ROOMS) MapStyle.HALLS else MapStyle.CAVERN

    /** Biomes whose ground splits into cracks; the rest sink into pits. */
    private val cracked = setOf("CAVE", "MINES", "CANYON", "VOLCANO", "ASH", "STORMPEAK", "SKYREACH", "GLASSWASTE", "FROST", "CITADEL", "RUINS", "CRYPT", "TEMPLE", "GODHALL", "ASTRAL", "OBLIVION")

    fun chasmOf(biome: String): ChasmShape = if (biome in cracked) ChasmShape.CRACK else ChasmShape.PIT

    /** Вода биома: водные и болотные - чаще ям, у огня - лава, у холода - лёд, у бездны - тьма; прочие сухи. */
    private val liquids = mapOf(
        "SHORE" to Liquid.WATER, "FOREST" to Liquid.WATER, "CAVE" to Liquid.WATER, "CORAL" to Liquid.WATER, "SUNKEN" to Liquid.WATER,
        "TIDEVAULT" to Liquid.WATER, "DESERT" to Liquid.WATER, "CANYON" to Liquid.WATER,
        "MIRE" to Liquid.SWAMP, "JUNGLE" to Liquid.SWAMP,
        "VOLCANO" to Liquid.LAVA, "ASH" to Liquid.LAVA,
        "FROST" to Liquid.ICE, "STORMPEAK" to Liquid.ICE,
        "BLIGHT" to Liquid.TAR, "HIVE" to Liquid.TAR,
        "ABYSS" to Liquid.VOID, "OBLIVION" to Liquid.VOID,
    )

    /** Биомы, где воды больше, чем ям. */
    private val wet = setOf("SHORE", "MIRE", "JUNGLE", "CORAL", "SUNKEN", "TIDEVAULT", "FOREST")

    fun liquidOf(biome: String): Liquid? = liquids[biome]

    /**
     * Карта зоны; [terrain] - доли пропастей и воды из контента (`expedition.terrain`); [shape] -
     * форма по биому (`expedition.shapes`): пещера, комнаты и коридоры, кольцо вокруг арены, острова с мостами. Форма сама ставит вход
     * и место босса; каждая из [monsters] стай встаёт на карту - шаг между стаями сужается, пока не встанут все.
     *
     * Старт - у дальнего от середины края, путь ведёт через карту; что от старта недостижимо, засыпается. Выход - на открытом месте, где
     * его не закрывает скала перед ним по ходу камеры; у формы - её место босса. Вода - после ям и стай, их места она не сдвигает; ворота
     * арены встают скалой последними.
     */
    fun generate(
        seed: Long,
        biome: String,
        monsters: Int,
        terrain: TerrainRule,
        size: Int = 48,
        minChasm: Int = 1,
        minPool: Int = 1,
        shape: MapShape = MapShape.CAVERN,
    ): ExpeditionMap {
        val random = Random(seed)
        val grid = Array(size * size) { Tile.WALL }
        val plan = when (shape) {
            MapShape.CAVERN -> Plan().also { cavern(grid, size, random) }
            MapShape.ROOMS -> rooms(grid, size, random)
            MapShape.RING -> ring(grid, size, random)
            MapShape.ISLANDS -> islands(grid, size, random)
        }
        val start = plan.start?.takeIf { grid[it.y * size + it.x] == Tile.FLOOR } ?: (0 until size * size).filter { grid[it] == Tile.FLOOR }.let { floor ->
            val middle = distances(grid, size, Cell(size / 2, size / 2).let { nearest(grid, size, it) })
            floor.maxBy { middle[it] }.let { Cell(it % size, it / size) }
        }
        val fromStart = distances(grid, size, start)
        for (i in grid.indices) if (fromStart[i] < 0 && grid[i] == Tile.FLOOR) grid[i] = Tile.WALL
        fun roomy(i: Int) = grid[i] == Tile.FLOOR && i % size in 1 until size - 1 && i / size in 1 until size - 1 &&
            grid[i - 1] == Tile.FLOOR && grid[i + 1] == Tile.FLOOR && grid[i - size] == Tile.FLOOR && grid[i + size] == Tile.FLOOR
        fun rock(x: Int, y: Int) = x !in 0 until size || y !in 0 until size || grid[y * size + x] == Tile.WALL
        val roomyCells = grid.indices.filter(::roomy)
        val exitIndex = plan.exit?.let { it.y * size + it.x }?.takeIf(::roomy)
            ?: roomyCells.filter { SceneSight.inView(it % size, it / size, ::rock) }.ifEmpty { roomyCells }.ifEmpty { grid.indices.filter { grid[it] == Tile.FLOOR } }
                .maxBy { fromStart[it] }
        val exit = Cell(exitIndex % size, exitIndex / size)
        val fromExit = distances(grid, size, exit)
        val spawns = spawns(grid, size, random, monsters, fromStart, fromExit, plan.reserved)
        val decor = IntArray(size * size) { index ->
            if (grid[index] != Tile.FLOOR || fromStart[index] < 2 || index == exitIndex) {
                0
            } else if (random.nextDouble() < 0.07) {
                1 + random.nextInt(3)
            } else {
                0
            }
        }
        val keep = setOf(start, exit) + spawns + plan.gate
        val liquid = liquidOf(biome)
        chasms(grid, size, random, chasmOf(biome), keep, share = if (biome in wet) terrain.wetChasmShare else terrain.chasmShare, minimum = minChasm, swerve = terrain.crackSwerve)
        liquid?.let { waters(grid, size, random, keep, share = if (biome in wet) terrain.wetWaterShare else terrain.waterShare, terrain = terrain) }
        fillPuddles(grid, size, start, minPool)
        val gate = plan.gate.filter { grid[it.y * size + it.x] == Tile.FLOOR }.onEach { grid[it.y * size + it.x] = Tile.WALL }
        return ExpeditionMap(size, size, grid, decor, start, exit, spawns, liquid ?: Liquid.WATER.takeIf { Tile.WATER in grid }, gate)
    }

    /**
     * Что форма карты решила сама: вход [start], место босса [exit], ворота арены [gate] и клетки, где стаи не встают
     * ([reserved]: арена и проход к ней). null - как у пещеры: вход - дальний край от середины, босс - дальше всего от входа.
     */
    private class Plan(val start: Cell? = null, val exit: Cell? = null, val gate: List<Cell> = emptyList(), val reserved: Set<Int> = emptySet())

    /**
     * Места стай: не ближе 8 шагов от входа и 3 от выхода, вне [reserved]; шаг между стаями - [SPAWN_GAP] клеток, а если все [count]
     * не встали - уже, до соседних клеток: каждая стая захода стоит на карте (иначе печать и ворота не снять).
     */
    private fun spawns(grid: Array<Tile>, size: Int, random: Random, count: Int, fromStart: IntArray, fromExit: IntArray, reserved: Set<Int>): List<Cell> {
        val open = grid.indices.filter { grid[it] == Tile.FLOOR && it !in reserved && fromExit[it] >= 3 }
        val candidates = open.filter { fromStart[it] >= 8 }.ifEmpty { open.filter { fromStart[it] >= 2 } }.shuffled(random)
        val spawns = mutableListOf<Cell>()
        for (gap in SPAWN_GAP downTo 1) {
            for (index in candidates) {
                if (spawns.size >= count) return spawns
                val cell = Cell(index % size, index / size)
                if (spawns.none { abs(it.x - cell.x) + abs(it.y - cell.y) < gap }) spawns += cell
            }
        }
        return spawns
    }

    /**
     * A - комнаты и коридоры: комнаты по площади карты, коридоры шириной 2 - остов-дерево по ближним
     * комнатам и пара петель, чтобы не ходить назад; в больших залах - колонны. Вход - комната у нижнего края, босс - в самой дальней
     * по пути комнате.
     *
     * Остов-дерево: каждая новая комната - к ближайшей из связанных; тупик ведёт ещё и ко второй ближней комнате.
     */
    private fun rooms(grid: Array<Tile>, size: Int, random: Random): Plan {
        val target = (size * size / ROOM_AREA).coerceIn(ROOMS_MIN, ROOMS_MAX)
        val widest = (size / 4).coerceIn(ROOM_MIN + 1, ROOM_MAX)
        val rooms = mutableListOf<Room>()
        repeat(ROOM_TRIES) {
            if (rooms.size >= target) return@repeat
            val w = random.nextInt(ROOM_MIN, widest + 1)
            val h = random.nextInt(ROOM_MIN - 1, widest)
            if (size - w - 4 <= 2 || size - h - 4 <= 2) return@repeat
            val room = Room(random.nextInt(2, size - w - 2), random.nextInt(2, size - h - 2), w, h)
            if (rooms.all { it.apart(room, ROOM_MARGIN) }) rooms += room
        }
        if (rooms.isEmpty()) return Plan().also { cavern(grid, size, random) }
        rooms.forEach { room -> for (y in room.y until room.y + room.h) for (x in room.x until room.x + room.w) grid[y * size + x] = Tile.FLOOR }
        val degree = IntArray(rooms.size)
        fun join(a: Int, b: Int) {
            wideCorridor(grid, size, rooms[a].centre, rooms[b].centre, random)
            degree[a]++
            degree[b]++
        }
        val joined = mutableListOf(0)
        val waiting = (1 until rooms.size).toMutableList()
        while (waiting.isNotEmpty()) {
            val (a, b) = joined.flatMap { a -> waiting.map { b -> a to b } }.minBy { (a, b) -> rooms[a].distance(rooms[b]) }
            join(a, b)
            joined += b
            waiting.remove(b)
        }
        rooms.indices.filter { degree[it] == 1 }.shuffled(random).take(LOOPS).forEach { a ->
            rooms.indices.filter { it != a }.sortedBy { rooms[a].distance(rooms[it]) }.getOrNull(1)?.let { join(a, it) }
        }
        rooms.filter { it.w >= PILLAR_W && it.h >= PILLAR_H }.forEach { room ->
            listOf(room.x + 2 to room.y + 2, room.x + room.w - 3 to room.y + 2, room.x + 2 to room.y + room.h - 3, room.x + room.w - 3 to room.y + room.h - 3)
                .forEach { (x, y) -> grid[y * size + x] = Tile.WALL }
        }
        val first = rooms.minBy { it.centre.x + (size - it.centre.y) }
        val reach = distances(grid, size, first.centre)
        val boss = rooms.filter { it !== first }.maxByOrNull { reach[it.centre.y * size + it.centre.x] } ?: first
        return Plan(first.centre, boss.centre)
    }

    /** Комната формы A: угол, ширина и высота. */
    private class Room(val x: Int, val y: Int, val w: Int, val h: Int) {
        val centre = Cell(x + w / 2, y + h / 2)
        fun apart(other: Room, margin: Int) = x + w + margin < other.x || other.x + other.w + margin < x || y + h + margin < other.y || other.y + other.h + margin < y
        fun distance(other: Room) = hypot((centre.x - other.centre.x).toDouble(), (centre.y - other.centre.y).toDouble())
    }

    /** Коридор шириной 2 буквой Г от [from] к [to]. */
    private fun wideCorridor(grid: Array<Tile>, size: Int, from: Cell, to: Cell, random: Random) {
        fun open(x: Int, y: Int) {
            if (x in 1 until size - 1 && y in 1 until size - 1) grid[y * size + x] = Tile.FLOOR
        }
        val corner = if (random.nextBoolean()) Cell(to.x, from.y) else Cell(from.x, to.y)
        listOf(from to corner, corner to to).forEach { (a, b) ->
            for (x in minOf(a.x, b.x)..maxOf(a.x, b.x)) {
                for (y in minOf(a.y, b.y)..maxOf(a.y, b.y)) {
                    open(x, y)
                    open(x + 1, y)
                    open(x, y + 1)
                    open(x + 1, y + 1)
                }
            }
        }
    }

    /**
     * B - кольцо вокруг арены босса: неровное кольцо, круглая арена в скальном поясе, проход к ней сверху - за воротами;
     * снаружи кольца - ниши-тупики для сундуков. Вход - снизу на кольце, до ворот - пол-кольца в любую сторону.
     *
     * Проход к арене - сверху, ворота - его средний ряд.
     */
    private fun ring(grid: Array<Tile>, size: Int, random: Random): Plan {
        val scale = size / RING_BASE
        val c = (size - 1) / 2.0
        val phases = DoubleArray(4) { random.nextDouble() * 2 * PI }
        fun inner(a: Double) = (14.2 + 1.3 * sin(3 * a + phases[0]) + 0.8 * sin(5 * a + phases[1])) * scale
        fun outer(a: Double) = minOf((21.5 + 1.6 * sin(2 * a + phases[2]) + 1.0 * sin(7 * a + phases[3])) * scale, c - 1)
        val arena = ARENA_RADIUS * scale
        val reserved = HashSet<Int>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val r = hypot(x - c, y - c)
                val a = atan2(y - c, x - c)
                if (r < arena || (r > inner(a) && r < outer(a))) grid[y * size + x] = Tile.FLOOR
                if (r <= inner(a)) reserved += y * size + x
            }
        }
        val cx = c.roundToInt()
        val top = (c - inner(-PI / 2)).roundToInt() - 1
        val bottom = (c - arena).roundToInt() + 1
        for (y in top..bottom) for (x in cx - 1..cx + 1) grid[y * size + x] = Tile.FLOOR
        val gateRow = (top + bottom) / 2
        val gate = (cx - 1..cx + 1).map { Cell(it, gateRow) }
        NICHES.forEach { base ->
            val a = base + random.nextDouble() * NICHE_JITTER
            val edge = outer(a)
            val far = edge + NICHE_DEPTH * scale
            val nx = c + cos(a) * far
            val ny = c + sin(a) * far
            for (y in 1 until size - 1) for (x in 1 until size - 1) if (hypot(x - nx, y - ny) < NICHE_RADIUS * scale) grid[y * size + x] = Tile.FLOOR
            val steps = ceil(far - edge + 1).toInt() * 2
            for (t in 0..steps) {
                val r = edge - 1 + (far - edge + 1) * t / steps
                val x = (c + cos(a) * r).roundToInt()
                val y = (c + sin(a) * r).roundToInt()
                if (x in 1 until size - 2 && y in 1 until size - 1) {
                    grid[y * size + x] = Tile.FLOOR
                    grid[y * size + x + 1] = Tile.FLOOR
                }
            }
        }
        val south = PI / 2
        val start = Cell(cx, (c + (inner(south) + outer(south)) / 2).roundToInt())
        grid[start.y * size + start.x] = Tile.FLOOR
        return Plan(start, Cell(cx, cx), gate, reserved)
    }

    /**
     * C - острова через воду: цепь островов от угла к углу и два боковых, мосты шириной 2 - пол поверх воды. Вход - на
     * первом острове, босс - на последнем, самом большом; цепь наугад отражена, чтобы карты не повторялись.
     */
    private fun islands(grid: Array<Tile>, size: Int, random: Random): Plan {
        val scale = size / ISLES_BASE
        for (i in grid.indices) grid[i] = if (i % size in 2 until size - 2 && i / size in 2 until size - 2) Tile.WATER else Tile.WALL
        val flipX = random.nextBoolean()
        val flipY = random.nextBoolean()
        fun at(x: Double, y: Double) = Pair((if (flipX) ISLES_BASE - x else x) * scale, (if (flipY) ISLES_BASE - y else y) * scale)
        val chain = CHAIN.mapIndexed { i, (x, y) ->
            val (px, py) = at(x + random.nextInt(-2, 3), y + random.nextInt(-2, 3))
            Isle(px, py, (if (i == CHAIN.lastIndex) BOSS_ISLE else ISLE_MIN + random.nextDouble() * ISLE_SPREAD) * scale)
        }
        val sides = SIDES.map { side ->
            val (px, py) = at(side.x + random.nextInt(0, 4), side.y + random.nextInt(0, 4))
            Isle(px, py, SIDE_ISLE * scale)
        }
        (chain + sides).forEach { isle ->
            val p0 = random.nextDouble() * 2 * PI
            val p1 = random.nextDouble() * 2 * PI
            for (y in 2 until size - 2) {
                for (x in 2 until size - 2) {
                    val a = atan2(y - isle.y, x - isle.x)
                    if (hypot(x - isle.x, y - isle.y) < isle.r * (1 + 0.18 * sin(3 * a + p0) + 0.1 * sin(5 * a + p1))) grid[y * size + x] = Tile.FLOOR
                }
            }
        }
        fun bridge(a: Isle, b: Isle) {
            val steps = (hypot(b.x - a.x, b.y - a.y) * 2).toInt().coerceAtLeast(1)
            for (t in 0..steps) {
                val x = (a.x + (b.x - a.x) * t / steps).roundToInt()
                val y = (a.y + (b.y - a.y) * t / steps).roundToInt()
                listOf(x to y, x + 1 to y, x to y + 1).forEach { (bx, by) -> if (bx in 2 until size - 2 && by in 2 until size - 2) grid[by * size + bx] = Tile.FLOOR }
            }
        }
        chain.zipWithNext().forEach { (a, b) -> bridge(a, b) }
        SIDES.forEachIndexed { i, side -> bridge(chain[side.link], sides[i]) }
        val first = chain.first()
        val last = chain.last()
        return Plan(Cell(first.x.roundToInt(), first.y.roundToInt()), Cell(last.x.roundToInt(), last.y.roundToInt()))
    }

    /** Остров формы C: середина и радиус. */
    private class Isle(val x: Double, val y: Double, val r: Double)

    /** Боковой остров формы C на сетке [ISLES_BASE]: угол разброса и остров цепи, к которому ведёт мост. */
    private class Side(val x: Double, val y: Double, val link: Int)

    /** Шаг между стаями, клеток (сужается, пока все стаи не встанут). */
    private const val SPAWN_GAP = 5

    /** Форма A: клеток карты на комнату, пределы числа комнат, стороны комнаты, попыток, зазор, петель и размер зала с колоннами. */
    private const val ROOM_AREA = 330
    private const val ROOMS_MIN = 4
    private const val ROOMS_MAX = 10
    private const val ROOM_MIN = 6
    private const val ROOM_MAX = 11
    private const val ROOM_TRIES = 400
    private const val ROOM_MARGIN = 3
    private const val LOOPS = 2
    private const val PILLAR_W = 9
    private const val PILLAR_H = 7

    /** Форма B на карте [RING_BASE] клеток: радиус арены, ниши снаружи кольца - углы, разброс угла, глубина и радиус. */
    private const val RING_BASE = 52.0
    private const val ARENA_RADIUS = 8.5
    private val NICHES = listOf(0.35, 2.6, 3.75, 5.75)
    private const val NICHE_JITTER = 0.3
    private const val NICHE_DEPTH = 2.5
    private const val NICHE_RADIUS = 2.6

    /** Форма C на карте [ISLES_BASE] клеток: цепь островов от угла к углу, боковые острова, радиусы островов. */
    private const val ISLES_BASE = 60.0
    private val CHAIN = listOf(9.0 to 50.0, 22.0 to 40.0, 30.0 to 27.0, 42.0 to 33.0, 50.0 to 12.0)
    private val SIDES = listOf(Side(12.0, 24.0, 1), Side(46.0, 49.0, 3))
    private const val ISLE_MIN = 5.5
    private const val ISLE_SPREAD = 1.8
    private const val BOSS_ISLE = 8.5
    private const val SIDE_ISLE = 4.6

    /**
     * Chasms: shapes of the biome are tried until about [share] of the ground is gone (трещина виляет с шансом [swerve]); a lone shape smaller than
     * [minimum] cells is not cut. A shape is cut only on
     * open ground two steps or more from [keep] and only if every remaining piece of ground is still reachable from the
     * first kept cell, so a map never splits; a shape that would split it is put back.
     *
     * Меньше [minimum] клеток - не пропасть, а пятно: режется, только если прирастает к уже вырезанной.
     */
    private fun chasms(grid: Array<Tile>, size: Int, random: Random, shape: ChasmShape, keep: Set<Cell>, share: Double, minimum: Int, swerve: Double) {
        val target = (grid.count { it == Tile.FLOOR } * share).toInt()
        val from = keep.first()
        var cut = 0
        repeat(CHASM_TRIES) {
            if (cut >= target) return
            val seed = Cell(random.nextInt(2, size - 2), random.nextInt(2, size - 2))
            val cells = when (shape) {
                ChasmShape.PIT -> pit(seed, random.nextInt(2, 7), random)
                ChasmShape.CRACK -> crack(seed, random.nextInt(4, 10), random, swerve)
            }.filter { it.x in 1 until size - 1 && it.y in 1 until size - 1 && grid[it.y * size + it.x] == Tile.FLOOR }
            if (cells.isEmpty() || cells.any { cell -> keep.any { abs(it.x - cell.x) + abs(it.y - cell.y) < 2 } }) return@repeat
            if (cells.size < minimum && cells.none { cell -> STEPS.any { (dx, dy) -> grid.getOrNull((cell.y + dy) * size + cell.x + dx) == Tile.CHASM } }) return@repeat
            cells.forEach { grid[it.y * size + it.x] = Tile.CHASM }
            val reach = distances(grid, size, from)
            if (grid.indices.any { grid[it] == Tile.FLOOR && reach[it] < 0 }) {
                cells.forEach { grid[it.y * size + it.x] = Tile.FLOOR }
            } else {
                cut += cells.size
            }
        }
    }

    /**
     * Вода: реки и озёра, пока не займут около [share] пола. Озеро - пятно, вырезается целиком или никак, как яма; река -
     * извилистая полоса через полкарты, и каждая её клетка, что разорвала бы карту, остаётся сушей: так у реки появляются броды.
     * Ни вода, ни брод не встают ближе двух шагов к старту, выходу и стаям.
     */
    private fun waters(grid: Array<Tile>, size: Int, random: Random, keep: Set<Cell>, share: Double, terrain: TerrainRule) {
        val target = (grid.count { it == Tile.FLOOR } * share).toInt()
        val from = keep.first()
        var cut = 0
        fun open(cell: Cell) = cell.x in 1 until size - 1 && cell.y in 1 until size - 1 && grid[cell.y * size + cell.x] == Tile.FLOOR &&
            keep.none { abs(it.x - cell.x) + abs(it.y - cell.y) < 2 }
        fun whole() = distances(grid, size, from).let { reach -> grid.indices.none { grid[it] == Tile.FLOOR && reach[it] < 0 } }
        repeat(WATER_TRIES) {
            if (cut >= target) return
            val seed = Cell(random.nextInt(2, size - 2), random.nextInt(2, size - 2))
            if (random.nextDouble() < terrain.riverChance) {
                river(seed, random.nextInt(size / 3, size * 2 / 3), random, terrain.riverBend).filter(::open).forEach { cell ->
                    grid[cell.y * size + cell.x] = Tile.WATER
                    if (whole()) cut++ else grid[cell.y * size + cell.x] = Tile.FLOOR
                }
            } else {
                val cells = pit(seed, random.nextInt(5, 14), random).filter(::open)
                if (cells.isEmpty()) return@repeat
                cells.forEach { grid[it.y * size + it.x] = Tile.WATER }
                if (whole()) cut += cells.size else cells.forEach { grid[it.y * size + it.x] = Tile.FLOOR }
            }
        }
    }

    /**
     * Последний проход: пятно пропасти или воды (связное по сторонам и диагоналям) меньше [minimum] клеток засыпается -
     * одиночных луж и дыр нет. Засыпанное становится полом там, куда дойдёт герой от [start], иначе скалой: ничто не стоит там,
     * куда не дойти.
     */
    private fun fillPuddles(grid: Array<Tile>, size: Int, start: Cell, minimum: Int) {
        if (minimum <= 1) return
        val seen = BooleanArray(grid.size)
        val filled = mutableListOf<Int>()
        for (first in grid.indices) {
            val tile = grid[first]
            if (seen[first] || (tile != Tile.CHASM && tile != Tile.WATER)) continue
            val spot = mutableListOf(first)
            seen[first] = true
            var k = 0
            while (k < spot.size) {
                val at = spot[k++]
                for ((dx, dy) in AROUND) {
                    val x = at % size + dx
                    val y = at / size + dy
                    if (x !in 0 until size || y !in 0 until size) continue
                    val next = y * size + x
                    if (seen[next] || grid[next] != tile) continue
                    seen[next] = true
                    spot += next
                }
            }
            if (spot.size < minimum) filled += spot
        }
        if (filled.isEmpty()) return
        filled.forEach { grid[it] = Tile.FLOOR }
        val reach = distances(grid, size, start)
        filled.forEach { if (reach[it] < 0) grid[it] = Tile.WALL }
    }

    /**
     * Река: полоса шириной в клетку-две, что держит курс и петляет; [length] - шагов.
     *
     * В половине мест полоса шире: читается рекой, а не трещиной.
     */
    private fun river(seed: Cell, length: Int, random: Random, bend: Double): List<Cell> {
        val horizontal = random.nextBoolean()
        var x = seed.x
        var y = seed.y
        val cells = LinkedHashSet<Cell>()
        repeat(length) {
            cells += Cell(x, y)
            if (random.nextBoolean()) cells += if (horizontal) Cell(x, y + 1) else Cell(x + 1, y)
            if (horizontal) x++ else y++
            if (random.nextDouble() < bend) if (horizontal) y += if (random.nextBoolean()) 1 else -1 else x += if (random.nextBoolean()) 1 else -1
        }
        return cells.toList()
    }

    /** A pit: [count] cells grown from [seed] to its neighbours. */
    private fun pit(seed: Cell, count: Int, random: Random): List<Cell> {
        val cells = linkedSetOf(seed)
        while (cells.size < count) {
            val (x, y) = cells.elementAt(random.nextInt(cells.size))
            cells += listOf(Cell(x + 1, y), Cell(x - 1, y), Cell(x, y + 1), Cell(x, y - 1))[random.nextInt(4)]
        }
        return cells.toList()
    }

    /** A crack: a line of [length] cells from [seed], keeping its course and swerving now and then. */
    private fun crack(seed: Cell, length: Int, random: Random, swerve: Double): List<Cell> {
        val horizontal = random.nextBoolean()
        var x = seed.x
        var y = seed.y
        return List(length) {
            Cell(x, y).also {
                if (horizontal) x++ else y++
                if (random.nextDouble() < swerve) if (horizontal) y += if (random.nextBoolean()) 1 else -1 else x += if (random.nextBoolean()) 1 else -1
            }
        }
    }

    /** Сколько пятен воды и пропастей пробуется за карту (доли и вид реки - контент, [TerrainRule]). */
    private const val WATER_TRIES = 40
    private const val CHASM_TRIES = 60

    /** Восемь соседей клетки: пятно глади связно и по диагонали - так его рисует контур. */
    private val AROUND = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to -1, 1 to -1, -1 to 1)

    /** Четыре соседа клетки: пропасть прирастает к пропасти по стороне. */
    private val STEPS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

    /**
     * Walkers dig from the middle until about two fifths of the map is open, then the edges are smoothed.
     *
     * Проход клеточного правила: стена, почти окружённая землёй, осыпается, и одиночные выступы не читаются шумом.
     */
    private fun cavern(grid: Array<Tile>, size: Int, random: Random) {
        val target = size * size * 2 / 5
        var open = 0
        repeat(5) {
            var x = size / 2
            var y = size / 2
            repeat(size * size) {
                if (open >= target) return@repeat
                val index = y * size + x
                if (grid[index] == Tile.WALL) {
                    grid[index] = Tile.FLOOR
                    open++
                }
                when (random.nextInt(4)) {
                    0 -> x = (x + 1).coerceIn(2, size - 3)
                    1 -> x = (x - 1).coerceIn(2, size - 3)
                    2 -> y = (y + 1).coerceIn(2, size - 3)
                    else -> y = (y - 1).coerceIn(2, size - 3)
                }
            }
        }
        val copy = grid.copyOf()
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val around = (-1..1).sumOf { dy -> (-1..1).count { dx -> copy[(y + dy) * size + x + dx] == Tile.FLOOR } }
                if (copy[y * size + x] == Tile.WALL && around >= 6) grid[y * size + x] = Tile.FLOOR
            }
        }
    }

    private fun nearest(grid: Array<Tile>, size: Int, cell: Cell): Cell = grid.indices.filter { grid[it] == Tile.FLOOR }.minBy { abs(it % size - cell.x) + abs(it / size - cell.y) }.let { Cell(it % size, it / size) }

    /** Steps from [from] to every cell, or -1 where it cannot be reached. */
    fun distances(grid: Array<Tile>, size: Int, from: Cell): IntArray {
        val result = IntArray(size * size) { -1 }
        val queue = ArrayDeque<Int>()
        val first = from.y * size + from.x
        result[first] = 0
        queue.add(first)
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val x = index % size
            val y = index / size
            for ((nx, ny) in listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
                if (nx !in 0 until size || ny !in 0 until size) continue
                val next = ny * size + nx
                if (grid[next] != Tile.FLOOR || result[next] >= 0) continue
                result[next] = result[index] + 1
                queue.add(next)
            }
        }
        return result
    }
}
