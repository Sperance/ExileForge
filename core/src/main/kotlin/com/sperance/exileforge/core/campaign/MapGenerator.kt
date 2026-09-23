package com.sperance.exileforge.core.campaign

import kotlin.math.abs
import kotlin.random.Random

/** A cell on the grid: rock, or ground a hero can stand on. */
enum class Tile { WALL, FLOOR }

data class Cell(val x: Int, val y: Int)

/** How the ground is carved: open caverns, or rooms joined by corridors. */
enum class MapStyle { CAVERN, HALLS }

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
) {
    fun tile(x: Int, y: Int): Tile = if (x in 0 until width && y in 0 until height) tiles[y * width + x] else Tile.WALL
    fun walkable(x: Int, y: Int) = tile(x, y) == Tile.FLOOR
    fun decorAt(x: Int, y: Int): Int = if (x in 0 until width && y in 0 until height) decor[y * width + x] else 0
    val floor: Int get() = tiles.count { it == Tile.FLOOR }
}

/**
 * Every map of a run is new: carved from a seed, so the same seed is the same map.
 *
 * The biome decides the carving — a crypt, a temple, ruins and mines are halls joined by
 * corridors, everything else a cavern dug by wandering walkers — and the map is then trimmed to
 * the one region the start can reach, so an exit or a monster is never sealed in rock. The exit is
 * the farthest reachable cell from the start; monsters stand away from both.
 */
object MapGenerator {

    private val halls = setOf("RUINS", "CRYPT", "MINES", "TEMPLE")

    fun styleOf(biome: String): MapStyle = if (biome in halls) MapStyle.HALLS else MapStyle.CAVERN

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
        val exitIndex = grid.indices.filter { grid[it] == Tile.FLOOR }.maxBy { fromStart[it] }
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
            if (grid[index] != Tile.FLOOR || fromStart[index] < 2 || index == exitIndex) 0
            else if (random.nextDouble() < 0.07) 1 + random.nextInt(3) else 0
        }
        return ExpeditionMap(size, size, grid, decor, start, exit, spawns)
    }

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
                if (grid[index] == Tile.WALL) { grid[index] = Tile.FLOOR; open++ }
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
        for (y in 1 until size - 1) for (x in 1 until size - 1) {
            val around = (-1..1).sumOf { dy -> (-1..1).count { dx -> copy[(y + dy) * size + x + dx] == Tile.FLOOR } }
            if (copy[y * size + x] == Tile.WALL && around >= 6) grid[y * size + x] = Tile.FLOOR
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

    private fun nearest(grid: Array<Tile>, size: Int, cell: Cell): Cell =
        grid.indices.filter { grid[it] == Tile.FLOOR }.minBy { abs(it % size - cell.x) + abs(it / size - cell.y) }.let { Cell(it % size, it / size) }

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
