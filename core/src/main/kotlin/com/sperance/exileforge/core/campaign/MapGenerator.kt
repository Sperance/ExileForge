package com.sperance.exileforge.core.campaign

import kotlin.math.abs
import kotlin.random.Random

/**
 * A cell on the grid: rock, ground a hero can stand on, or a chasm (3.91.0) - a pit, a crack or a sinkhole the hero sees across
 * but never steps into; с 3.95.0 и вода (реки и озёра, у иных биомов - лава, лёд, смола, тьма): через неё видно, пройти нельзя.
 */
enum class Tile { WALL, FLOOR, CHASM, WATER }

/** Чем полны низины биома (3.95.0): от этого - только вид воды на карте. */
enum class Liquid { WATER, SWAMP, LAVA, ICE, TAR, VOID }

data class Cell(val x: Int, val y: Int)

/** How the ground is carved: open caverns, or rooms joined by corridors. */
enum class MapStyle { CAVERN, HALLS }

/** How a biome's chasms look (3.91.0): round pits and sinkholes, or long narrow cracks. */
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
    /** Чем полна вода карты (3.95.0); null - воды нет. */
    val liquid: Liquid? = null,
) {
    fun tile(x: Int, y: Int): Tile = if (x in 0 until width && y in 0 until height) tiles[y * width + x] else Tile.WALL
    fun walkable(x: Int, y: Int) = tile(x, y) == Tile.FLOOR

    /** Whether sight passes over the cell (3.91.0): ground, chasms and water do, rock does not. */
    fun clear(x: Int, y: Int) = tile(x, y) != Tile.WALL
    fun decorAt(x: Int, y: Int): Int = if (x in 0 until width && y in 0 until height) decor[y * width + x] else 0
    val floor: Int get() = tiles.count { it == Tile.FLOOR }

    /** Клетка становится [tile] (3.90.0): объекты карты вырезают комнату за стеной, а открытая стена или дверь - пол. */
    internal fun carve(cell: Cell, tile: Tile) {
        if (cell.x in 0 until width && cell.y in 0 until height) tiles[cell.y * width + cell.x] = tile
    }
}

/**
 * Every map of a run is new: carved from a seed, so the same seed is the same map.
 *
 * The biome decides the carving — a crypt, a temple, ruins, mines and a citadel are halls joined by
 * corridors, everything else a cavern dug by wandering walkers — and the map is then trimmed to
 * the one region the start can reach, so an exit or a monster is never sealed in rock. The exit is
 * the farthest reachable cell from the start; monsters stand away from both. Chasms (3.91.0) are cut last, away from the start,
 * the exit and the monsters, and only where every piece of ground stays reachable.
 */
object MapGenerator {

    private val halls = setOf("RUINS", "CRYPT", "MINES", "TEMPLE", "CITADEL", VaalZones.BIOME)

    fun styleOf(biome: String): MapStyle = if (biome in halls) MapStyle.HALLS else MapStyle.CAVERN

    /** Biomes whose ground splits into cracks; the rest sink into pits. */
    private val cracked = setOf("CAVE", "MINES", "CANYON", "VOLCANO", "ASH", "STORMPEAK", "SKYREACH", "GLASSWASTE", "FROST", "CITADEL", "RUINS", "CRYPT", "TEMPLE", "GODHALL", "ASTRAL", "OBLIVION")

    fun chasmOf(biome: String): ChasmShape = if (biome in cracked) ChasmShape.CRACK else ChasmShape.PIT

    /** Вода биома (3.95.0): водные и болотные - чаще ям, у огня - лава, у холода - лёд, у бездны - тьма; прочие сухи. */
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

    fun generate(seed: Long, biome: String, monsters: Int, size: Int = 48): ExpeditionMap {
        val random = Random(seed)
        val grid = Array(size * size) { Tile.WALL }
        when (styleOf(biome)) {
            MapStyle.CAVERN -> cavern(grid, size, random)
            MapStyle.HALLS -> halls(grid, size, random)
        }
        val start = (0 until size * size).filter { grid[it] == Tile.FLOOR }.let { floor ->
            // The start is the far end of the map from its middle: the walk leads across it.
            val middle = distances(grid, size, Cell(size / 2, size / 2).let { nearest(grid, size, it) })
            floor.maxBy { middle[it] }.let { Cell(it % size, it / size) }
        }
        val fromStart = distances(grid, size, start)
        // Whatever the start cannot reach is filled back in: nothing may stand where no one can walk.
        for (i in grid.indices) if (fromStart[i] < 0) grid[i] = Tile.WALL
        // The exit stands in the open (3.79.0): off the walls, where the perspective of the scene does not hide it.
        fun roomy(i: Int) = grid[i] == Tile.FLOOR && i % size in 1 until size - 1 && i / size in 1 until size - 1 &&
            grid[i - 1] == Tile.FLOOR && grid[i + 1] == Tile.FLOOR && grid[i - size] == Tile.FLOOR && grid[i + size] == Tile.FLOOR
        val exitIndex = (grid.indices.filter(::roomy).ifEmpty { grid.indices.filter { grid[it] == Tile.FLOOR } }).maxBy { fromStart[it] }
        val exit = Cell(exitIndex % size, exitIndex / size)
        val fromExit = distances(grid, size, exit)

        val candidates = grid.indices.filter { grid[it] == Tile.FLOOR && fromStart[it] >= 8 && fromExit[it] >= 3 }.shuffled(random)
        val spawns = mutableListOf<Cell>()
        for (index in candidates) {
            if (spawns.size >= monsters) break
            val cell = Cell(index % size, index / size)
            if (spawns.none { abs(it.x - cell.x) + abs(it.y - cell.y) < 5 }) spawns += cell
        }
        val decor = IntArray(size * size) { index ->
            if (grid[index] != Tile.FLOOR || fromStart[index] < 2 || index == exitIndex) {
                0
            } else if (random.nextDouble() < 0.07) {
                1 + random.nextInt(3)
            } else {
                0
            }
        }
        val keep = setOf(start, exit) + spawns
        val liquid = liquidOf(biome)
        chasms(grid, size, random, chasmOf(biome), keep, share = if (biome in wet) WET_CHASM_SHARE else CHASM_SHARE)
        // Вода (3.95.0) - после ям и стай: их места она не сдвигает
        liquid?.let { waters(grid, size, random, keep, share = if (biome in wet) WET_WATER_SHARE else WATER_SHARE) }
        return ExpeditionMap(size, size, grid, decor, start, exit, spawns, liquid)
    }

    /**
     * Chasms (3.91.0): shapes of the biome are tried until about [CHASM_SHARE] of the ground is gone. A shape is cut only on
     * open ground two steps or more from [keep] and only if every remaining piece of ground is still reachable from the
     * first kept cell, so a map never splits; a shape that would split it is put back.
     */
    private fun chasms(grid: Array<Tile>, size: Int, random: Random, shape: ChasmShape, keep: Set<Cell>, share: Double) {
        val target = (grid.count { it == Tile.FLOOR } * share).toInt()
        val from = keep.first()
        var cut = 0
        repeat(CHASM_TRIES) {
            if (cut >= target) return
            val seed = Cell(random.nextInt(2, size - 2), random.nextInt(2, size - 2))
            val cells = when (shape) {
                ChasmShape.PIT -> pit(seed, random.nextInt(2, 7), random)
                ChasmShape.CRACK -> crack(seed, random.nextInt(4, 10), random)
            }.filter { it.x in 1 until size - 1 && it.y in 1 until size - 1 && grid[it.y * size + it.x] == Tile.FLOOR }
            if (cells.isEmpty() || cells.any { cell -> keep.any { abs(it.x - cell.x) + abs(it.y - cell.y) < 2 } }) return@repeat
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
     * Вода (3.95.0): реки и озёра, пока не займут около [share] пола. Озеро - пятно, вырезается целиком или никак, как яма; река -
     * извилистая полоса через полкарты, и каждая её клетка, что разорвала бы карту, остаётся сушей: так у реки появляются броды.
     * Ни вода, ни брод не встают ближе двух шагов к старту, выходу и стаям.
     */
    private fun waters(grid: Array<Tile>, size: Int, random: Random, keep: Set<Cell>, share: Double) {
        val target = (grid.count { it == Tile.FLOOR } * share).toInt()
        val from = keep.first()
        var cut = 0
        fun open(cell: Cell) = cell.x in 1 until size - 1 && cell.y in 1 until size - 1 && grid[cell.y * size + cell.x] == Tile.FLOOR &&
            keep.none { abs(it.x - cell.x) + abs(it.y - cell.y) < 2 }
        fun whole() = distances(grid, size, from).let { reach -> grid.indices.none { grid[it] == Tile.FLOOR && reach[it] < 0 } }
        repeat(WATER_TRIES) {
            if (cut >= target) return
            val seed = Cell(random.nextInt(2, size - 2), random.nextInt(2, size - 2))
            if (random.nextDouble() < RIVER_CHANCE) {
                river(seed, random.nextInt(size / 3, size * 2 / 3), random).filter(::open).forEach { cell ->
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

    /** Река: полоса шириной в клетку-две, что держит курс и петляет; [length] - шагов. */
    private fun river(seed: Cell, length: Int, random: Random): List<Cell> {
        val horizontal = random.nextBoolean()
        var x = seed.x
        var y = seed.y
        val cells = LinkedHashSet<Cell>()
        repeat(length) {
            cells += Cell(x, y)
            // Шире в половине мест: полоса читается рекой, а не трещиной
            if (random.nextBoolean()) cells += if (horizontal) Cell(x, y + 1) else Cell(x + 1, y)
            if (horizontal) x++ else y++
            if (random.nextDouble() < RIVER_BEND) if (horizontal) y += if (random.nextBoolean()) 1 else -1 else x += if (random.nextBoolean()) 1 else -1
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
    private fun crack(seed: Cell, length: Int, random: Random): List<Cell> {
        val horizontal = random.nextBoolean()
        var x = seed.x
        var y = seed.y
        return List(length) {
            Cell(x, y).also {
                if (horizontal) x++ else y++
                if (random.nextDouble() < CRACK_SWERVE) if (horizontal) y += if (random.nextBoolean()) 1 else -1 else x += if (random.nextBoolean()) 1 else -1
            }
        }
    }

    /** The share of the ground chasms take, how many shapes are tried for it, and how often a crack swerves. */
    private const val CHASM_SHARE = 0.04

    /** Доли пола под ямами и водой у водных биомов и под водой у прочих с водой (3.95.0); попытки и вид реки. */
    private const val WET_CHASM_SHARE = 0.015
    private const val WET_WATER_SHARE = 0.06
    private const val WATER_SHARE = 0.025
    private const val WATER_TRIES = 40
    private const val RIVER_CHANCE = 0.5
    private const val RIVER_BEND = 0.35
    private const val CHASM_TRIES = 60
    private const val CRACK_SWERVE = 0.3

    /** Walkers dig from the middle until about two fifths of the map is open, then the edges are smoothed. */
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
        // One pass of the cellular rule: a wall mostly surrounded by ground crumbles, and the
        // single-cell stubs a walker leaves behind stop reading as noise.
        val copy = grid.copyOf()
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val around = (-1..1).sumOf { dy -> (-1..1).count { dx -> copy[(y + dy) * size + x + dx] == Tile.FLOOR } }
                if (copy[y * size + x] == Tile.WALL && around >= 6) grid[y * size + x] = Tile.FLOOR
            }
        }
    }

    /** Rooms of a few sizes, each joined to the one before it by an L-shaped corridor. */
    private fun halls(grid: Array<Tile>, size: Int, random: Random) {
        val rooms = mutableListOf<Cell>()
        repeat(40) {
            if (rooms.size >= 11) return@repeat
            val w = random.nextInt(4, 9)
            val h = random.nextInt(4, 9)
            val x = random.nextInt(2, size - w - 2)
            val y = random.nextInt(2, size - h - 2)
            val centre = Cell(x + w / 2, y + h / 2)
            if (rooms.any { abs(it.x - centre.x) < 6 && abs(it.y - centre.y) < 6 }) return@repeat
            for (dy in 0 until h) for (dx in 0 until w) grid[(y + dy) * size + x + dx] = Tile.FLOOR
            rooms.lastOrNull()?.let { corridor(grid, size, it, centre, random) }
            rooms += centre
        }
    }

    private fun corridor(grid: Array<Tile>, size: Int, from: Cell, to: Cell, random: Random) {
        val horizontalFirst = random.nextBoolean()
        val corner = if (horizontalFirst) Cell(to.x, from.y) else Cell(from.x, to.y)
        fun dig(a: Cell, b: Cell) {
            var x = a.x
            var y = a.y
            while (true) {
                grid[y * size + x] = Tile.FLOOR
                if (x == b.x && y == b.y) break
                x += (b.x - x).coerceIn(-1, 1)
                y += (b.y - y).coerceIn(-1, 1)
            }
        }
        dig(from, corner)
        dig(corner, to)
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
