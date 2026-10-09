package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.rules.content.BehaviourRule
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sign
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

/**
 * Whether a straight line from one point to another crosses no rock (3.89.0; chasms are seen across, 3.91.0): an exact walk over every cell
 * the line enters (Amanatides–Woo), not samples along it - a sample could step over the tip of a rock corner.
 * The cells of both ends are not checked: a rock face is seen, what is behind it is not.
 * A line squeezing through the corner between two rocks touching by corners is blocked; past a single rock corner it goes on.
 */
fun ExpeditionWorld.sight(ax: Double, ay: Double, bx: Double, by: Double): Boolean = line(ax, ay, bx, by) { cx, cy -> map.clear(cx, cy) }

/**
 * Прямая проходима ногами (3.95.0): та же проверка клеток, что у [sight], но пропасть и вода её рвут - монстр, видящий
 * героя через яму, идёт в обход, а не скользит по её краю.
 */
fun ExpeditionWorld.passable(ax: Double, ay: Double, bx: Double, by: Double): Boolean = line(ax, ay, bx, by) { cx, cy -> map.walkable(cx, cy) }

/** Обход клеток прямой (Amanatides–Woo): [open] - пропускает ли клетка; на углу двух закрытых клеток прямая рвётся. */
private inline fun line(ax: Double, ay: Double, bx: Double, by: Double, open: (Int, Int) -> Boolean): Boolean {
    val x = GridAxis(ax, bx)
    val y = GridAxis(ay, by)
    var remaining = x.cells + y.cells
    while (remaining > 0) {
        val gap = x.next - y.next
        when {
            abs(gap) < CORNER -> {
                if (!open(x.cell + x.step, y.cell) && !open(x.cell, y.cell + y.step)) return false
                x.advance()
                y.advance()
                remaining -= 2
            }

            gap < 0 -> {
                x.advance()
                remaining--
            }

            else -> {
                y.advance()
                remaining--
            }
        }
        if (remaining > 0 && !open(x.cell, y.cell)) return false
    }
    return true
}

/**
 * One axis of the walk in [sight]: the cell the line is in, which way it goes, how many cell borders it has
 * left to cross, and at which share of the line it crosses the next one.
 */
private class GridAxis(from: Double, to: Double) {
    var cell = floor(from).toInt()
        private set
    val step = sign(to - from).toInt()
    val cells = abs(floor(to).toInt() - cell)
    private val span = if (step == 0) Double.POSITIVE_INFINITY else 1.0 / abs(to - from)
    var next = when {
        step > 0 -> (cell + 1 - from) * span
        step < 0 -> (from - cell) * span
        else -> Double.POSITIVE_INFINITY
    }
        private set

    fun advance() {
        cell += step
        next += span
    }
}

/** Shares of a line closer than this cross both borders at once: the line goes through a corner. */
private const val CORNER = 1e-9

/**
 * What the hero sees from the cell they stand in, worked out again only when they leave it. Сначала открытые клетки по прямой
 * [sight]; затем скала (4.3.1): видна, если до её центра дошла прямая или рядом (8 соседей) лежит видимая открытая клетка - так
 * в туннеле в одну клетку стены видны вдоль всего освещённого пола, а не лишь у ног: прямая к центру дальней боковой стены
 * режет соседнюю скалу под скользящим углом.
 */
internal fun ExpeditionWorld.light() {
    val here = Cell(floor(heroX).toInt(), floor(heroY).toInt())
    if (here == litFrom) return
    litFrom = here
    lit.fill(false)
    val reach = ceil(lightRadius).toInt()
    val rocks = ArrayList<Cell>()
    for (y in here.y - reach..here.y + reach) {
        for (x in here.x - reach..here.x + reach) {
            if (x !in 0 until map.width || y !in 0 until map.height) continue
            if (hypot(x - here.x.toDouble(), y - here.y.toDouble()) > lightRadius) continue
            if (!map.clear(x, y)) {
                rocks += Cell(x, y)
                continue
            }
            if (sight(heroX, heroY, x + 0.5, y + 0.5)) see(x, y)
        }
    }
    // A rock face is seen when the line reaches it or it borders lit open ground; what is behind it is not.
    rocks.forEach { (x, y) -> if (bordersLit(x, y) || sight(heroX, heroY, x + 0.5, y + 0.5)) see(x, y) }
}

private fun ExpeditionWorld.see(x: Int, y: Int) {
    lit[y * map.width + x] = true
    explored[y * map.width + x] = true
}

/** Лежит ли рядом со скалой (8 соседей) видимая открытая клетка. */
private fun ExpeditionWorld.bordersLit(x: Int, y: Int): Boolean = ExpeditionWorld.STEPS.any { (dx, dy) -> map.clear(x + dx, y + dy) && lit(x + dx, y + dy) }

/** A move that slides along walls: each axis is tried on its own, so a diagonal into a wall still glides. */
internal fun ExpeditionWorld.slide(x: Double, y: Double, dx: Double, dy: Double, radius: Double): Pair<Double, Double> {
    val nx = if (free(x + dx, y, radius)) x + dx else x
    val ny = if (free(nx, y + dy, radius)) y + dy else y
    return nx to ny
}

internal fun ExpeditionWorld.free(x: Double, y: Double, radius: Double): Boolean = listOf(-radius to -radius, radius to -radius, -radius to radius, radius to radius)
    .all { (ox, oy) -> map.walkable(floor(x + ox).toInt(), floor(y + oy).toInt()) }
