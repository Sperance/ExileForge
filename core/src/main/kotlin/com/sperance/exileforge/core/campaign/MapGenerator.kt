package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.MapShape
import com.sperance.exileforge.rules.content.TerrainRule
import kotlin.math.abs
import kotlin.random.Random

/**
 * A cell on the grid: rock, ground a hero can stand on, or a chasm - a pit, a crack or a sinkhole the hero sees across
 * but never steps into; и вода (реки и озёра, у иных биомов - лава, лёд, смола, тьма): через неё видно, пройти нельзя.
 */
enum class Tile { WALL, FLOOR, CHASM, WATER }

/** Чем полны низины биома: от этого - только вид воды на карте. */
enum class Liquid { WATER, SWAMP, LAVA, ICE, TAR, VOID }

data class Cell(val x: Int, val y: Int)

/** Узор стен (облик скалы): живая скала пещер или обточенный камень залов - формы комнат и сетки залов ([MapForm.style]). */
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
     * Ворота арены босса (форма с воротами): клетки скалы прохода к арене; пока доля стай не зачищена, они стоят
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
 * Пол режет форма биома ([MapForm]), затем карта обрезается до области, куда дойти от входа: выход и стаи не замурованы в
 * скале. Выход - место босса формы или дальняя от входа клетка; стаи - поодаль от обоих. Пропасти режутся последними, вдали от
 * входа, выхода и стай, и только там, где вся земля остаётся достижимой.
 */
object MapGenerator {

    /** Узор стен формы [shape]: у залов (комнаты, сетка залов) - обточенный камень, у прочих - живая скала. */
    fun styleOf(shape: MapShape): MapStyle = MapForm.of(shape).style

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
     * форма по биому (`expedition.shapes`, реализация - [MapForm]): пещера, комнаты и коридоры, кольцо вокруг арены, острова с
     * мостами, сетка залов, кольцо арен. Форма сама ставит вход и место босса; каждая из [monsters] стай встаёт на карту - шаг между стаями сужается, пока не встанут все.
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
        val plan = MapForm.of(shape).carve(grid, size, random)
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

    /** Шаг между стаями, клеток (сужается, пока все стаи не встанут). */
    private const val SPAWN_GAP = 5

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
