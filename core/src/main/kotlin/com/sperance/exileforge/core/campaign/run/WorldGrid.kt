package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.rules.content.BehaviourRule
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

// ==================== The grid ====================

/**
 * The shortest way from [from] to [to] over floor, eight ways, never cutting a corner of rock;
 * empty when there is none. Both ends are included.
 */
fun ExpeditionWorld.path(from: Cell, to: Cell): List<Cell> {
    if (!map.walkable(to.x, to.y) || !map.walkable(from.x, from.y)) return emptyList()
    if (from == to) return listOf(from)
    val previous = IntArray(map.width * map.height) { -1 }
    val start = from.y * map.width + from.x
    val goal = to.y * map.width + to.x
    previous[start] = start
    val queue = ArrayDeque<Int>().apply { add(start) }
    while (queue.isNotEmpty()) {
        val index = queue.removeFirst()
        if (index == goal) break
        val x = index % map.width
        val y = index / map.width
        for ((dx, dy) in ExpeditionWorld.STEPS) {
            val nx = x + dx
            val ny = y + dy
            if (!map.walkable(nx, ny) || (dx != 0 && dy != 0 && !(map.walkable(x + dx, y) && map.walkable(x, y + dy)))) continue
            val next = ny * map.width + nx
            if (previous[next] >= 0) continue
            previous[next] = index
            queue.add(next)
        }
    }
    if (previous[goal] < 0) return emptyList()
    val cells = ArrayList<Cell>()
    var index = goal
    while (index != start) {
        cells += Cell(index % map.width, index / map.width)
        index = previous[index]
    }
    cells += from
    return cells.reversed()
}

/** Steps from [from] to every floor cell within [limit] steps, four ways. */
internal fun ExpeditionWorld.distances(from: Cell, limit: Int): Map<Cell, Int> {
    val seen = mutableMapOf(from to 0)
    val queue = ArrayDeque<Cell>().apply { add(from) }
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        val d = seen.getValue(cell)
        if (d == limit) continue
        for ((dx, dy) in ExpeditionWorld.STEPS.take(4)) {
            val next = Cell(cell.x + dx, cell.y + dy)
            if (map.walkable(next.x, next.y) && next !in seen) {
                seen[next] = d + 1
                queue.add(next)
            }
        }
    }
    return seen
}

/** Whether a straight line from one point to another crosses no rock. */
fun ExpeditionWorld.sight(ax: Double, ay: Double, bx: Double, by: Double): Boolean {
    val distance = hypot(bx - ax, by - ay)
    val steps = ceil(distance / ExpeditionWorld.SIGHT_STEP).toInt()
    for (i in 1 until steps) {
        val t = i.toDouble() / steps
        if (!map.walkable(floor(ax + (bx - ax) * t).toInt(), floor(ay + (by - ay) * t).toInt())) return false
    }
    return true
}

/** What the hero sees from the cell they stand in, worked out again only when they leave it. */
internal fun ExpeditionWorld.light() {
    val here = Cell(floor(heroX).toInt(), floor(heroY).toInt())
    if (here == litFrom) return
    litFrom = here
    lit.fill(false)
    val reach = ceil(lightRadius).toInt()
    for (y in here.y - reach..here.y + reach) {
        for (x in here.x - reach..here.x + reach) {
            if (x !in 0 until map.width || y !in 0 until map.height) continue
            if (hypot(x - here.x.toDouble(), y - here.y.toDouble()) > lightRadius) continue
            // A rock face is seen when the line reaches it; what is behind it is not.
            val cx = x + 0.5
            val cy = y + 0.5
            val toward = hypot(cx - heroX, cy - heroY).coerceAtLeast(1e-6)
            val near = (toward - 0.75).coerceAtLeast(0.0) / toward
            if (!sight(heroX, heroY, heroX + (cx - heroX) * near, heroY + (cy - heroY) * near)) continue
            lit[y * map.width + x] = true
            explored[y * map.width + x] = true
        }
    }
}

/** A move that slides along walls: each axis is tried on its own, so a diagonal into a wall still glides. */
internal fun ExpeditionWorld.slide(x: Double, y: Double, dx: Double, dy: Double, radius: Double): Pair<Double, Double> {
    val nx = if (free(x + dx, y, radius)) x + dx else x
    val ny = if (free(nx, y + dy, radius)) y + dy else y
    return nx to ny
}

internal fun ExpeditionWorld.free(x: Double, y: Double, radius: Double): Boolean = listOf(-radius to -radius, radius to -radius, -radius to radius, radius to radius)
    .all { (ox, oy) -> map.walkable(floor(x + ox).toInt(), floor(y + oy).toInt()) }

/**
 * The packs a fight with [agent] draws in (3.26.0): it and every ordinary pack still standing within
 * [rules.gatherRadius] of it — since 3.28.0 in the order they are fought, [agent] first, then the rest nearest first.
 * The boss, a crystal's guardian and anything not of the map fight alone.
 */
fun ExpeditionWorld.gathered(agent: MonsterAgent): List<MonsterAgent> {
    if (agent === boss || agent.crystal != null || agents.none { it === agent }) return listOf(agent)
    fun distance(other: MonsterAgent) = hypot(other.x - agent.x, other.y - agent.y)
    return listOf(agent) + agents.filter {
        it !== agent && it !== boss && it.alive && it.crystal == null && it.standing.isNotEmpty() && distance(it) <= rules.gatherRadius
    }.sortedBy(::distance)
}

/**
 * The gathered packs as the stages they are fought in (3.70.0): packs in a row share a stage while the stage holds
 * no more than [rules.stageMonsters] monsters — 1+1 and 1+2 fight together, 2+2 stay two stages; a pack too big stands alone.
 */
fun ExpeditionWorld.stages(packs: List<MonsterAgent>): List<List<MonsterAgent>> = packs.fold(mutableListOf<MutableList<MonsterAgent>>()) { stages, pack ->
    val last = stages.lastOrNull()
    if (last != null && last.sumOf { it.standing.size } + pack.standing.size <= rules.stageMonsters) last += pack else stages += mutableListOf(pack)
    stages
}
