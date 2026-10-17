package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.MapShape
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
 * Что форма карты решила сама: вход [start], место босса [exit], ворота арены [gate] и клетки, где стаи не встают
 * ([reserved]: арена и проход к ней). null - как у пещеры: вход - дальний край от середины, босс - дальше всего от входа.
 */
internal class MapPlan(val start: Cell? = null, val exit: Cell? = null, val gate: List<Cell> = emptyList(), val reserved: Set<Int> = emptySet())

/**
 * Форма карты (`expedition.shapes`): режет пол сетки стороной `size` из скалы и ставит вход, место босса и ворота ([MapPlan]).
 * Новая форма - новая реализация и её строка в [of].
 */
internal interface MapForm {
    /** Узор стен формы ([MapGenerator.styleOf]). */
    val style: MapStyle

    fun carve(grid: Array<Tile>, size: Int, random: Random): MapPlan

    companion object {
        /** Реализация формы [shape] контента. */
        fun of(shape: MapShape): MapForm = when (shape) {
            MapShape.CAVERN -> CavernForm
            MapShape.ROOMS -> RoomsForm
            MapShape.RING -> RingForm
            MapShape.ISLANDS -> IslandsForm
            MapShape.LABYRINTH -> LabyrinthForm
            MapShape.ARENAS -> ArenasForm
        }
    }
}

/**
 * Пещера: бродяги копают от середины, пока не откроют около двух пятых карты, затем края сглаживаются.
 *
 * Проход клеточного правила: стена, почти окружённая землёй, осыпается, и одиночные выступы не читаются шумом.
 */
internal object CavernForm : MapForm {
    override val style = MapStyle.CAVERN

    override fun carve(grid: Array<Tile>, size: Int, random: Random): MapPlan {
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
        return MapPlan()
    }
}

/**
 * A - комнаты и коридоры: комнаты по площади карты, коридоры шириной 2 - остов-дерево по ближним
 * комнатам и пара петель, чтобы не ходить назад; в больших залах - колонны. Вход - комната у нижнего края, босс - в самой дальней
 * по пути комнате.
 *
 * Остов-дерево: каждая новая комната - к ближайшей из связанных; тупик ведёт ещё и ко второй ближней комнате.
 */
internal object RoomsForm : MapForm {
    override val style = MapStyle.HALLS

    override fun carve(grid: Array<Tile>, size: Int, random: Random): MapPlan {
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
        if (rooms.isEmpty()) return CavernForm.carve(grid, size, random)
        rooms.forEach { it.open(grid, size) }
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
        val reach = MapGenerator.distances(grid, size, first.centre)
        val boss = rooms.filter { it !== first }.maxByOrNull { reach[it.centre.y * size + it.centre.x] } ?: first
        return MapPlan(first.centre, boss.centre)
    }

    /** Клеток карты на комнату, пределы числа комнат, стороны комнаты, попыток, зазор, петель и размер зала с колоннами. */
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
}

/**
 * B - кольцо вокруг арены босса: неровное кольцо, круглая арена в скальном поясе, проход к ней сверху - за воротами;
 * снаружи кольца - ниши-тупики для сундуков. Вход - снизу на кольце, до ворот - пол-кольца в любую сторону.
 *
 * Проход к арене - сверху, ворота - его средний ряд.
 */
internal object RingForm : MapForm {
    override val style = MapStyle.CAVERN

    override fun carve(grid: Array<Tile>, size: Int, random: Random): MapPlan {
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
        return MapPlan(start, Cell(cx, cx), gate, reserved)
    }

    /** На карте [RING_BASE] клеток: радиус арены, ниши снаружи кольца - углы, разброс угла, глубина и радиус. */
    private const val RING_BASE = 52.0
    private const val ARENA_RADIUS = 8.5
    private val NICHES = listOf(0.35, 2.6, 3.75, 5.75)
    private const val NICHE_JITTER = 0.3
    private const val NICHE_DEPTH = 2.5
    private const val NICHE_RADIUS = 2.6
}

/**
 * C - острова через воду: цепь островов от угла к углу и два боковых, мосты шириной 2 - пол поверх воды. Вход - на
 * первом острове, босс - на последнем, самом большом; цепь наугад отражена, чтобы карты не повторялись.
 */
internal object IslandsForm : MapForm {
    override val style = MapStyle.CAVERN

    override fun carve(grid: Array<Tile>, size: Int, random: Random): MapPlan {
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
        return MapPlan(first.cell, last.cell)
    }

    /** Боковой остров на сетке [ISLES_BASE]: угол разброса и остров цепи, к которому ведёт мост. */
    private class Side(val x: Double, val y: Double, val link: Int)

    /** На карте [ISLES_BASE] клеток: цепь островов от угла к углу, боковые острова, радиусы островов. */
    private const val ISLES_BASE = 60.0
    private val CHAIN = listOf(9.0 to 50.0, 22.0 to 40.0, 30.0 to 27.0, 42.0 to 33.0, 50.0 to 12.0)
    private val SIDES = listOf(Side(12.0, 24.0, 1), Side(46.0, 49.0, 3))
    private const val ISLE_MIN = 5.5
    private const val ISLE_SPREAD = 1.8
    private const val BOSS_ISLE = 8.5
    private const val SIDE_ISLE = 4.6
}

/**
 * Сетка залов: сторона карты делится на [PITCH]-клеточные ячейки (на картах 52-60 - 4×4), в каждой - свой зал со
 * случайными сторонами и сдвигом. Соседние залы связывает остов - обход в глубину от входа в случайном порядке, так путь к
 * боссу петляет через залы и оставляет тупики; поверх - петли из тупиков к соседям, но только те, что не срезают путь от
 * входа до босса по залам. Двери шириной [DOOR] - напротив друг друга, где стороны залов перекрываются. Вход - угловой зал
 * нижнего ряда, босс - противоположный угол.
 */
internal object LabyrinthForm : MapForm {
    override val style = MapStyle.HALLS

    override fun carve(grid: Array<Tile>, size: Int, random: Random): MapPlan {
        val side = (size / PITCH.toDouble()).roundToInt().coerceAtLeast(SIDE_MIN)
        val pitch = (size - 2 * EDGE) / side
        val widest = pitch - INSET_MIN
        val narrowest = (pitch - INSET_MAX).coerceIn(HALL_MIN, widest)
        val halls = List(side * side) { i ->
            val w = random.nextInt(narrowest, widest + 1)
            val h = random.nextInt(narrowest, widest + 1)
            Room(EDGE + i % side * pitch + random.nextInt(1, pitch - w), EDGE + i / side * pitch + random.nextInt(1, pitch - h), w, h)
        }
        val corner = if (random.nextBoolean()) 0 else side - 1
        val entry = (side - 1) * side + corner
        val boss = side - 1 - corner
        fun neighbours(i: Int) = listOfNotNull(
            (i - 1).takeIf { i % side > 0 },
            (i + 1).takeIf { i % side < side - 1 },
            (i - side).takeIf { it >= 0 },
            (i + side).takeIf { it < side * side },
        )
        val links = HashSet<Pair<Int, Int>>()
        fun link(a: Int, b: Int) = minOf(a, b) to maxOf(a, b)
        val seen = BooleanArray(side * side).also { it[entry] = true }
        val path = ArrayDeque(listOf(entry))
        while (path.isNotEmpty()) {
            val next = neighbours(path.last()).filterNot { seen[it] }.randomOrNull(random)
            if (next == null) {
                path.removeLast()
                continue
            }
            seen[next] = true
            links += link(path.last(), next)
            path.addLast(next)
        }
        fun degree(i: Int) = links.count { i == it.first || i == it.second }
        fun hops(): Int {
            val steps = IntArray(side * side) { -1 }.also { it[entry] = 0 }
            val queue = ArrayDeque(listOf(entry))
            while (queue.isNotEmpty()) {
                val at = queue.removeFirst()
                neighbours(at).filter { steps[it] < 0 && link(at, it) in links }.forEach {
                    steps[it] = steps[at] + 1
                    queue.addLast(it)
                }
            }
            return steps[boss]
        }
        val route = hops()
        val loops = side * side / LOOP_HALLS
        var added = 0
        val spare = halls.indices.flatMap { a -> neighbours(a).filter { it > a }.map { a to it } }.filter { it !in links }.shuffled(random)
        for ((a, b) in spare) {
            if (added >= loops) break
            if (listOf(a, b).none { it != entry && it != boss && degree(it) == 1 }) continue
            links += a to b
            if (hops() < route) links -= a to b else added++
        }
        halls.forEach { it.open(grid, size) }
        links.forEach { (a, b) -> door(grid, size, halls[a], halls[b], beside = b == a + 1, random) }
        return MapPlan(halls[entry].centre, halls[boss].centre)
    }

    /**
     * Дверь шириной [DOOR] между залом [a] и его соседом [b] справа ([beside]) или снизу: прямо через скалу там, где их
     * стороны перекрываются; не перекрываются - коридор буквой Г между серединами.
     */
    private fun door(grid: Array<Tile>, size: Int, a: Room, b: Room, beside: Boolean, random: Random) {
        val low = if (beside) maxOf(a.y, b.y) else maxOf(a.x, b.x)
        val high = (if (beside) minOf(a.y + a.h, b.y + b.h) else minOf(a.x + a.w, b.x + b.w)) - DOOR
        if (high < low) return wideCorridor(grid, size, a.centre, b.centre, random)
        val at = random.nextInt(low, high + 1)
        val through = if (beside) a.x + a.w until b.x else a.y + a.h until b.y
        for (along in through) {
            for (k in 0 until DOOR) {
                val cell = if (beside) Cell(along, at + k) else Cell(at + k, along)
                grid[cell.y * size + cell.x] = Tile.FLOOR
            }
        }
    }

    /**
     * Шаг сетки залов, клеток; залов на сторону не меньше [SIDE_MIN]; отступ сетки от края [EDGE]; сторона зала - шаг минус
     * от [INSET_MIN] до [INSET_MAX], но не меньше [HALL_MIN]; ширина двери; одна петля на [LOOP_HALLS] залов.
     */
    private const val PITCH = 14
    private const val SIDE_MIN = 3
    private const val EDGE = 2
    private const val INSET_MIN = 4
    private const val INSET_MAX = 6
    private const val HALL_MIN = 3
    private const val DOOR = 2
    private const val LOOP_HALLS = 5
}

/**
 * Кольцо арен: неровный кольцевой коридор шириной около двух клеток, на нём четыре круглые арены - снизу (вход), сверху и по
 * бокам (боковые сдвинуты наугад по кольцу); в середине - арена босса в скальном поясе, проход к ней - от верхней арены, за
 * воротами (средний ряд прохода). До ворот - пол-кольца в любую сторону, через боковые арены.
 */
internal object ArenasForm : MapForm {
    override val style = MapStyle.CAVERN

    override fun carve(grid: Array<Tile>, size: Int, random: Random): MapPlan {
        val scale = size / ARENAS_BASE
        val c = (size - 1) / 2.0
        val wave = random.nextDouble() * 2 * PI
        fun road(a: Double) = (LOOP_RADIUS + LOOP_WAVE * sin(3 * a + wave)) * scale
        val angles = listOf(PI / 2, -PI / 2) + listOf(0.0, PI).map { it + (random.nextDouble() * 2 - 1) * ARENA_JITTER }
        val arenas = angles.map { a -> Isle(c + cos(a) * road(a), c + sin(a) * road(a), ARENA_RADIUS * scale) }
        val phases = arenas.map { random.nextDouble() * 2 * PI }
        val boss = BOSS_RADIUS * scale
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val r = hypot(x - c, y - c)
                val onRoad = abs(r - road(atan2(y - c, x - c))) < LOOP_HALF
                val inArena = arenas.indices.any { i ->
                    val isle = arenas[i]
                    hypot(x - isle.x, y - isle.y) < isle.r * (1 + ARENA_WAVE * sin(3 * atan2(y - isle.y, x - isle.x) + phases[i]))
                }
                if (r < boss || onRoad || inArena) grid[y * size + x] = Tile.FLOOR
            }
        }
        val cx = c.roundToInt()
        val north = arenas[1]
        val top = north.y.roundToInt()
        val bottom = (c - boss).roundToInt() + 1
        for (y in top..bottom) for (x in cx - 1..cx + 1) grid[y * size + x] = Tile.FLOOR
        val gateRow = ((north.y + north.r * (1 + ARENA_WAVE) + c - boss) / 2).roundToInt()
        val gate = (cx - 1..cx + 1).map { Cell(it, gateRow) }
        val behind = c - gateRow + 1
        val reserved = grid.indices.filterTo(HashSet()) { hypot(it % size - c, it / size - c) < behind }
        return MapPlan(arenas[0].cell, Cell(cx, cx), gate, reserved)
    }

    /**
     * На карте [ARENAS_BASE] клеток: радиус кольца и его волна, полуширина коридора (не растёт с картой), радиус арены, её волна
     * долей радиуса и разброс угла боковых арен, радиус арены босса.
     */
    private const val ARENAS_BASE = 56.0
    private const val LOOP_RADIUS = 18.0
    private const val LOOP_WAVE = 0.8
    private const val LOOP_HALF = 1.2
    private const val ARENA_RADIUS = 6.5
    private const val ARENA_WAVE = 0.1
    private const val ARENA_JITTER = 0.25
    private const val BOSS_RADIUS = 6.0
}

/** Прямоугольный зал: угол, ширина и высота. */
private class Room(val x: Int, val y: Int, val w: Int, val h: Int) {
    val centre = Cell(x + w / 2, y + h / 2)
    fun apart(other: Room, margin: Int) = x + w + margin < other.x || other.x + other.w + margin < x || y + h + margin < other.y || other.y + other.h + margin < y
    fun distance(other: Room) = hypot((centre.x - other.centre.x).toDouble(), (centre.y - other.centre.y).toDouble())

    /** Зал становится полом сетки. */
    fun open(grid: Array<Tile>, size: Int) {
        for (cy in y until y + h) for (cx in x until x + w) grid[cy * size + cx] = Tile.FLOOR
    }
}

/** Круглое место: середина и радиус - остров, арена. */
private class Isle(val x: Double, val y: Double, val r: Double) {
    val cell get() = Cell(x.roundToInt(), y.roundToInt())
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
