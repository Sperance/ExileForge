package com.sperance.exileforge.core.model.combat.world

import com.sperance.exileforge.core.i18n.tr
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** What one tile of a zone is made of. Only [blocking] decides where a body may walk. */
enum class Terrain(val blocking: Boolean) {
    FLOOR(false), RUBBLE(false), WATER(true), WALL(true), PILLAR(true)
}

/** Scenery standing on a walkable tile: light and leavings, never an obstacle. */
enum class DecorKind { BRAZIER, BONES, DRIFTWOOD }

data class Decor(val position: Vec2, val kind: DecorKind)

/** The shapes a zone's map can take. One zone always draws the same one. */
enum class MapKind { SHORE, VAULT, BASTION }

/**
 * One zone's ground, generated tile by tile and identical on every device.
 *
 * It decides where a walk can go and nothing else. No number here ever reaches the server, which has
 * no notion of where anyone stands, so a map can make a fight longer or shorter to reach but never
 * worth more or less. See [WorldSimulation].
 */
class Battlefield(
    val width: Int, val height: Int, val kind: MapKind, private val tiles: Array<Terrain>,
    val decor: List<Decor>, val spawn: Vec2, val camps: List<Vec2>, val altar: Vec2
) {
    init {
        require(width >= MIN_SIDE && height >= MIN_SIDE) {
            tr("Карта зоны не может быть меньше $MIN_SIDE клеток", "A zone map cannot be smaller than $MIN_SIDE tiles")
        }
        require(tiles.size == width * height) {
            tr("Сетка карты не совпадает с её размером", "The map grid does not match its size")
        }
    }

    val centre: Vec2 get() = Vec2(width / 2f, height / 2f)

    /** Outside the grid is solid rock, so nothing can ever walk off the edge of the world. */
    fun terrain(x: Int, y: Int): Terrain =
        if(x < 0 || y < 0 || x >= width || y >= height) Terrain.WALL else tiles[y * width + x]
    fun terrain(point: Vec2): Terrain = terrain(floor(point.x).toInt(), floor(point.y).toInt())
    fun tileCentre(index: Int) = Vec2(index % width + .5f, index / width + .5f)

    /** True when a body of [radius] standing at [point] would overlap something solid. */
    fun blocked(point: Vec2, radius: Float): Boolean {
        val left = floor(point.x - radius).toInt()
        val right = floor(point.x + radius).toInt()
        val top = floor(point.y - radius).toInt()
        val bottom = floor(point.y + radius).toInt()
        for(ty in top..bottom) for(tx in left..right) {
            if(!terrain(tx, ty).blocking) continue
            val near = Vec2(point.x.coerceIn(tx.toFloat(), tx + 1f), point.y.coerceIn(ty.toFloat(), ty + 1f))
            if(point.distanceTo(near) < radius) return true
        }
        return false
    }

    /**
     * Walks from [from] towards [to], sliding along whatever it cannot pass through.
     *
     * The two axes are tried separately, which is what turns a wall from a full stop into something a
     * thumb slides along. A body that somehow starts inside rock is let out rather than sealed in.
     */
    fun resolve(from: Vec2, to: Vec2, radius: Float): Vec2 {
        if(!blocked(to, radius)) return to
        if(blocked(from, radius)) return to
        var result = from
        Vec2(to.x, result.y).takeIf { !blocked(it, radius) }?.let { result = it }
        Vec2(result.x, to.y).takeIf { !blocked(it, radius) }?.let { result = it }
        return result
    }

    /** True when a body of [radius] can walk the straight line between two points. */
    fun clear(from: Vec2, to: Vec2, radius: Float): Boolean {
        val steps = max(1, (from.distanceTo(to) / SAMPLE).toInt())
        for(step in 1..steps) if(blocked(from.lerp(to, step.toFloat() / steps), radius)) return false
        return true
    }

    /** The nearest spot a body of [radius] can stand on, searched outwards from [point]. */
    fun settle(point: Vec2, radius: Float): Vec2 {
        if(!blocked(point, radius)) return point
        val cx = floor(point.x).toInt()
        val cy = floor(point.y).toInt()
        for(ring in 1..max(width, height)) for(dy in -ring..ring) for(dx in -ring..ring) {
            if(max(abs(dx), abs(dy)) != ring) continue
            val spot = Vec2(cx + dx + .5f, cy + dy + .5f)
            if(!blocked(spot, radius)) return spot
        }
        return centre
    }

    /** Tile indices a body of [radius] can reach from [start] on foot, in breadth-first order. */
    fun reachable(start: Vec2, radius: Float): List<Int> {
        val first = index(start)
        if(first < 0) return emptyList()
        val seen = BooleanArray(width * height)
        val queue = ArrayDeque<Int>()
        val found = ArrayList<Int>()
        seen[first] = true; queue += first; found += first
        while(queue.isNotEmpty()) {
            val at = queue.removeFirst()
            neighbours(at) { next ->
                if(!seen[next] && !blocked(tileCentre(next), radius)) { seen[next] = true; queue += next; found += next }
            }
        }
        return found
    }

    /**
     * A walk from [from] to [to] as tile centres, empty when there is no way through.
     *
     * Breadth-first over the grid rather than a straight line, because a corridor with a corner in it
     * is exactly where steering at a target walks into a wall and stays there.
     */
    fun path(from: Vec2, to: Vec2, radius: Float): List<Vec2> {
        val start = index(from)
        val goal = index(settle(to, radius))
        if(start < 0 || goal < 0) return emptyList()
        if(start == goal) return listOf(to)
        val previous = IntArray(width * height) { -1 }
        previous[start] = start
        val queue = ArrayDeque<Int>()
        queue += start
        while(queue.isNotEmpty()) {
            val at = queue.removeFirst()
            if(at == goal) break
            neighbours(at) { next ->
                if(previous[next] == -1 && !blocked(tileCentre(next), radius)) { previous[next] = at; queue += next }
            }
        }
        if(previous[goal] == -1) return emptyList()
        val walk = ArrayList<Vec2>()
        var at = goal
        while(at != start) { walk += tileCentre(at); at = previous[at] }
        walk.reverse()
        return walk
    }

    private fun index(point: Vec2): Int {
        val x = floor(point.x).toInt()
        val y = floor(point.y).toInt()
        return if(x < 0 || y < 0 || x >= width || y >= height) -1 else y * width + x
    }

    private inline fun neighbours(at: Int, visit: (Int) -> Unit) {
        val x = at % width
        val y = at / width
        if(x > 0) visit(at - 1)
        if(x < width - 1) visit(at + 1)
        if(y > 0) visit(at - width)
        if(y < height - 1) visit(at + width)
    }

    companion object {
        const val MIN_SIDE = 12
        /** The widest body the maps are checked against, so no corridor can swallow a boss. */
        const val CLEARANCE = .46f
        private const val SAMPLE = .25f
        fun of(zoneId: String, width: Int = 30, height: Int = 30): Battlefield =
            MapGenerator.generate(kindOf(zoneId), zoneId, width, height)
        /** One zone, one map. The three known zones get their own shape; anything else is seeded. */
        fun kindOf(zoneId: String): MapKind = when(zoneId) {
            "coast" -> MapKind.SHORE
            "crypt" -> MapKind.VAULT
            "citadel" -> MapKind.BASTION
            else -> MapKind.entries[((zoneId.hashCode() % MapKind.entries.size) + MapKind.entries.size) % MapKind.entries.size]
        }
    }
}

/** A seeded generator: a zone's ground must come out the same on every device. */
internal class Lcg(seed: Long) {
    private var state = (seed shl 1) or 1L
    fun next(): Float {
        state = state * 6364136223846793005L + 1442695040888963407L
        return ((state ushr 40).toInt() and 0xFFFFFF) / 16_777_215f
    }
    fun int(bound: Int): Int = if(bound <= 1) 0 else (next() * bound).toInt().coerceIn(0, bound - 1)
    fun range(from: Int, to: Int): Int = if(to <= from) from else from + int(to - from + 1)
    fun chance(odds: Float): Boolean = next() < odds
}

/** A grid under construction; the generators carve into it and [MapGenerator] finishes the job. */
private class Canvas(val width: Int, val height: Int, fill: Terrain) {
    val tiles = Array(width * height) { fill }
    fun inside(x: Int, y: Int) = x in 0 until width && y in 0 until height
    operator fun get(x: Int, y: Int): Terrain = if(inside(x, y)) tiles[y * width + x] else Terrain.WALL
    operator fun set(x: Int, y: Int, value: Terrain) { if(inside(x, y)) tiles[y * width + x] = value }
    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, value: Terrain) {
        for(y in max(0, y0)..min(height - 1, y1)) for(x in max(0, x0)..min(width - 1, x1)) set(x, y, value)
    }
    fun frame(thickness: Int, value: Terrain) {
        for(y in 0 until height) for(x in 0 until width)
            if(x < thickness || y < thickness || x >= width - thickness || y >= height - thickness) set(x, y, value)
    }
    fun blob(cx: Int, cy: Int, radius: Int, value: Terrain) {
        for(y in cy - radius..cy + radius) for(x in cx - radius..cx + radius)
            if((x - cx) * (x - cx) + (y - cy) * (y - cy) <= radius * radius) set(x, y, value)
    }
}

/**
 * The three map shapes, carved tile by tile from the zone's own seed.
 *
 * Each shape is a different problem to walk through — an open shore, a warren of rooms, a walled
 * courtyard — and each is finished the same way: the spawn is settled on solid ground, everything
 * reachable from it is flood-filled, and the altar, the camps and the scenery are only ever placed on
 * tiles that flood fill found. That is what stops a zone from ever generating a monster, a reward or a
 * boss behind a wall with no way round.
 */
internal object MapGenerator {
    private const val CAMPS = 7
    private const val CAMP_SPACING = 4.5f
    private const val CAMP_FROM_SPAWN = 6f
    private const val ALTAR_FROM_SPAWN = 9f
    private const val DECOR = 26

    fun generate(kind: MapKind, zoneId: String, width: Int, height: Int): Battlefield {
        val roll = Lcg(zoneId.hashCode().toLong())
        val canvas = Canvas(width, height, if(kind == MapKind.VAULT) Terrain.WALL else Terrain.FLOOR)
        val hints = when(kind) {
            MapKind.SHORE -> shore(canvas, roll)
            MapKind.VAULT -> vault(canvas, roll)
            MapKind.BASTION -> bastion(canvas, roll)
        }
        return assemble(kind, canvas, roll, hints)
    }

    /** Where a shape wants the exile to appear and where it wants the boss called. */
    private class Hints(val spawn: Vec2, val altar: Vec2)

    /** An open beach: a wavy tideline on one side, boulder fields, and drifts of shingle. */
    private fun shore(canvas: Canvas, roll: Lcg): Hints {
        canvas.frame(1, Terrain.WALL)
        // The tide is a walk of one column at a time, so the waterline bends instead of stepping.
        var depth = 4
        for(x in 0 until canvas.width) {
            depth = (depth + roll.range(-1, 1)).coerceIn(3, 8)
            for(y in canvas.height - depth until canvas.height) canvas[x, y] = Terrain.WATER
        }
        repeat(16) {
            val cx = roll.range(3, canvas.width - 4)
            val cy = roll.range(3, canvas.height - 10)
            if(canvas[cx, cy] == Terrain.FLOOR) canvas.blob(cx, cy, roll.range(1, 2), Terrain.WALL)
        }
        repeat(10) {
            val cx = roll.range(2, canvas.width - 3)
            val cy = roll.range(2, canvas.height - 6)
            for(y in cy - 1..cy + 1) for(x in cx - 2..cx + 2)
                if(canvas[x, y] == Terrain.FLOOR) canvas[x, y] = Terrain.RUBBLE
        }
        repeat(5) {
            val cx = roll.range(4, canvas.width - 5)
            val cy = roll.range(4, canvas.height - 9)
            if(canvas[cx, cy] != Terrain.WATER) canvas[cx, cy] = Terrain.PILLAR
        }
        return Hints(Vec2(3.5f, 3.5f), Vec2(canvas.width - 5.5f, canvas.height * .38f))
    }

    /** A flooded crypt: chambers cut out of the rock and joined by corridors two tiles wide. */
    private fun vault(canvas: Canvas, roll: Lcg): Hints {
        val rooms = ArrayList<IntArray>()
        var attempts = 0
        while(rooms.size < 8 && attempts < 120) {
            attempts++
            val w = roll.range(5, 8)
            val h = roll.range(4, 7)
            val x = roll.range(2, canvas.width - w - 3)
            val y = roll.range(2, canvas.height - h - 3)
            val room = intArrayOf(x, y, x + w, y + h)
            if(rooms.any { overlaps(it, room, 2) }) continue
            rooms += room
            canvas.rect(x, y, x + w, y + h, Terrain.FLOOR)
        }
        // Every room is joined to the one before it, so the whole crypt is one connected walk.
        for(index in 1 until rooms.size) corridor(canvas, centreOf(rooms[index - 1]), centreOf(rooms[index]), roll)
        rooms.forEach { room ->
            if(room[2] - room[0] >= 6 && room[3] - room[1] >= 5 && roll.chance(.7f)) {
                canvas[room[0] + 2, room[1] + 2] = Terrain.PILLAR
                canvas[room[2] - 2, room[3] - 2] = Terrain.PILLAR
            }
            if(roll.chance(.35f)) canvas.blob(centreOf(room).x.toInt(), centreOf(room).y.toInt(), 1, Terrain.WATER)
        }
        canvas.frame(1, Terrain.WALL)
        val first = rooms.firstOrNull() ?: intArrayOf(2, 2, 6, 6)
        val last = rooms.lastOrNull() ?: first
        return Hints(centreOf(first), centreOf(last))
    }

    /** A citadel: an outer yard, a walled ring with four gates, and a courtyard for the summoning. */
    private fun bastion(canvas: Canvas, roll: Lcg): Hints {
        canvas.frame(2, Terrain.WALL)
        val inset = 7
        for(x in inset until canvas.width - inset) {
            canvas[x, inset] = Terrain.WALL
            canvas[x, canvas.height - inset - 1] = Terrain.WALL
        }
        for(y in inset until canvas.height - inset) {
            canvas[inset, y] = Terrain.WALL
            canvas[canvas.width - inset - 1, y] = Terrain.WALL
        }
        // Four gates, so the courtyard is never sealed whatever else the seed does.
        val midX = canvas.width / 2
        val midY = canvas.height / 2
        for(step in -1..1) {
            canvas[midX + step, inset] = Terrain.FLOOR
            canvas[midX + step, canvas.height - inset - 1] = Terrain.FLOOR
            canvas[inset, midY + step] = Terrain.FLOOR
            canvas[canvas.width - inset - 1, midY + step] = Terrain.FLOOR
        }
        listOf(inset + 2 to inset + 2, canvas.width - inset - 3 to inset + 2,
            inset + 2 to canvas.height - inset - 3, canvas.width - inset - 3 to canvas.height - inset - 3)
            .forEach { (x, y) -> canvas[x, y] = Terrain.PILLAR }
        repeat(14) {
            val cx = roll.range(3, canvas.width - 4)
            val cy = roll.range(3, canvas.height - 4)
            // Rubble only in the outer yard: the courtyard is kept clear for the fight it holds.
            if(cx in inset..canvas.width - inset || cy in inset..canvas.height - inset) return@repeat
            for(y in cy - 1..cy + 1) for(x in cx - 1..cx + 1)
                if(canvas[x, y] == Terrain.FLOOR) canvas[x, y] = Terrain.RUBBLE
        }
        repeat(8) {
            val cx = roll.range(3, canvas.width - 4)
            val cy = roll.range(3, canvas.height - 4)
            if(cx in inset - 1..canvas.width - inset || cy in inset - 1..canvas.height - inset) return@repeat
            canvas.blob(cx, cy, 1, Terrain.WALL)
        }
        return Hints(Vec2(4.5f, 4.5f), Vec2(midX + .5f, midY + .5f))
    }

    /** The biggest patch of ground a body can walk without ever passing through rock. */
    private fun largest(map: Battlefield): List<Int> {
        val seen = BooleanArray(map.width * map.height)
        var best = emptyList<Int>()
        for(index in seen.indices) {
            if(seen[index] || map.blocked(map.tileCentre(index), Battlefield.CLEARANCE)) continue
            val region = map.reachable(map.tileCentre(index), Battlefield.CLEARANCE)
            region.forEach { seen[it] = true }
            if(region.size > best.size) best = region
        }
        return best
    }

    /** Open on every side: somewhere a fight has room rather than a corner to be pinned in. */
    private fun roomy(map: Battlefield, at: Vec2): Boolean =
        listOf(Vec2(1f, 0f), Vec2(-1f, 0f), Vec2(0f, 1f), Vec2(0f, -1f))
            .none { map.blocked(at + it, Battlefield.CLEARANCE) }

    private fun overlaps(a: IntArray, b: IntArray, gap: Int) =
        a[0] - gap <= b[2] && b[0] - gap <= a[2] && a[1] - gap <= b[3] && b[1] - gap <= a[3]

    private fun centreOf(room: IntArray) = Vec2((room[0] + room[2]) / 2f + .5f, (room[1] + room[3]) / 2f + .5f)

    /** An L of two straight runs, two tiles wide so nothing can wedge itself in a doorway. */
    private fun corridor(canvas: Canvas, from: Vec2, to: Vec2, roll: Lcg) {
        val x0 = from.x.toInt(); val y0 = from.y.toInt()
        val x1 = to.x.toInt(); val y1 = to.y.toInt()
        val horizontalFirst = roll.chance(.5f)
        val bend = if(horizontalFirst) x1 to y0 else x0 to y1
        canvas.rect(min(x0, bend.first), min(y0, bend.second), max(x0, bend.first), max(y0, bend.second) + 1, Terrain.FLOOR)
        canvas.rect(min(bend.first, x1), min(bend.second, y1), max(bend.first, x1) + 1, max(bend.second, y1), Terrain.FLOOR)
    }

    /**
     * Turns a carved grid into a battlefield: find the open ground, then place everything on it.
     *
     * The spawn is chosen inside the *largest* region a body can walk, never merely near the shape's
     * hint — a seed that happens to drop a boulder on the hint would otherwise wall the exile into a
     * cranny with nowhere to go and nothing to fight. Camps, the altar and the scenery all come out of
     * that same region, which is what makes every one of them reachable by construction.
     */
    private fun assemble(kind: MapKind, canvas: Canvas, roll: Lcg, hints: Hints): Battlefield {
        val bare = Battlefield(canvas.width, canvas.height, kind, canvas.tiles, emptyList(),
            Vec2(1.5f, 1.5f), emptyList(), Vec2(1.5f, 1.5f))
        val walkable = largest(bare).map(bare::tileCentre)
        val spawn = walkable.filter { roomy(bare, it) }.minByOrNull { it.distanceTo(hints.spawn) }
            ?: walkable.minByOrNull { it.distanceTo(hints.spawn) } ?: bare.centre
        val altar = walkable.filter { it.distanceTo(spawn) >= ALTAR_FROM_SPAWN }
            .minByOrNull { it.distanceTo(hints.altar) }
            ?: walkable.maxByOrNull { it.distanceTo(spawn) } ?: spawn
        val camps = ArrayList<Vec2>()
        walkable.filter { it.distanceTo(spawn) >= CAMP_FROM_SPAWN }.forEach { spot ->
            if(camps.size < CAMPS && camps.none { it.distanceTo(spot) < CAMP_SPACING }) camps += spot
        }
        if(camps.isEmpty()) camps += walkable.lastOrNull() ?: spawn
        val decor = ArrayList<Decor>()
        val plain = walkable.filter { bare.terrain(it) == Terrain.FLOOR || bare.terrain(it) == Terrain.RUBBLE }
        repeat(DECOR) {
            val spot = plain.getOrNull(roll.int(plain.size)) ?: return@repeat
            if(spot.distanceTo(spawn) < 2f || decor.any { it.position.distanceTo(spot) < 1.5f }) return@repeat
            decor += Decor(spot, when(kind) {
                MapKind.SHORE -> if(roll.chance(.7f)) DecorKind.DRIFTWOOD else DecorKind.BRAZIER
                MapKind.VAULT -> if(roll.chance(.6f)) DecorKind.BONES else DecorKind.BRAZIER
                MapKind.BASTION -> if(roll.chance(.55f)) DecorKind.BRAZIER else DecorKind.BONES
            })
        }
        return Battlefield(canvas.width, canvas.height, kind, canvas.tiles, decor, spawn, camps, altar)
    }
}
