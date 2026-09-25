package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.BehaviourRule
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.MonsterEffect
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

/** What a monster is doing on the map, between fights. */
enum class AgentMode { IDLE, ASLEEP, LURKING, CHASING, HUNTING, RETURNING }

/**
 * A monster walking the map: where it lives, where it is going, and whether it is still there.
 *
 * [pack] is one or more (2.54.0): a jetton is usually a single foe, sometimes a pack of up to
 * three, fought all at once since 2.70.0. [monster] — the map token, its walking behaviour and its
 * portrait on the ground — is always the strongest of the pack; [fallen] are those already killed.
 */
class MonsterAgent(val id: Int, val pack: List<RolledMonster>, val homeX: Double, val homeY: Double) {
    val monster: RolledMonster = pack.maxBy { it.rarity.ordinal }
    val rule: BehaviourRule get() = monster.behaviour
    /** Members of [pack] already killed (2.70.0): a pack the hero walked away from keeps its dead dead. */
    val fallen = mutableSetOf<Int>()
    /** Members still standing, by their place in [pack]. */
    val standing: List<Int> get() = pack.indices.filterNot { it in fallen }
    var x = homeX
    var y = homeY
    var targetX = homeX
    var targetY = homeY
    var idle = 0.0
    /** Seconds it will neither chase nor fight: a monster the hero ran from does not pounce at once. */
    var calm = 0.0
    var alive = true
    var mode = when (monster.behaviour.type) {
        BehaviourRule.AMBUSH -> AgentMode.LURKING
        BehaviourRule.SLEEP -> AgentMode.ASLEEP
        else -> AgentMode.IDLE
    }
    /** Seconds since it last saw the hero it is hunting. */
    var unseen = 0.0
    /** Where it last saw the hero: a hunt goes there before it gives up. */
    var lastX = homeX
    var lastY = homeY
    /** The far end of a patrol, or null for a monster that does not patrol. */
    var patrol: Cell? = null
    var outbound = true
    internal var path: List<Cell> = emptyList()
    internal var pathTo: Cell? = null
    internal var repath = 0.0
    val chasing: Boolean get() = mode == AgentMode.CHASING || mode == AgentMode.HUNTING
}

/** A chest on the map (since 2.33.0): where it stands and whether the hero has opened it. */
class Chest(val id: Int, val cell: Cell) { var opened = false }

/** A fountain on the map (since 2.48.0): where it stands, how much life it gives back, and whether it was drunk dry. */
class Fountain(val id: Int, val cell: Cell, val heal: Double) { var used = false }

/** What a step of the world ran into. */
sealed interface WorldEvent {
    data class Encounter(val agent: MonsterAgent) : WorldEvent
    data class Opened(val chest: Chest) : WorldEvent
    data class Drank(val fountain: Fountain) : WorldEvent
    /** The hero reached the Vaal portal (since 2.65.0). */
    data object Portal : WorldEvent
    data object Exit : WorldEvent
}

/**
 * The map in motion — positions in tile units, stepped by the scene every frame.
 *
 * It is pure so a test can walk it: the scene reads positions from here and draws them, and the
 * stick's direction comes in already in world axes (see [screenToWorld]). A fight starts on
 * contact and the exit ends the map. None of this is a game rule the server knows — it is how a
 * run feels, and nothing of it travels except which monster was killed.
 *
 * Since 2.32.0 (server 0.30.0) the map is lit and remembered: the hero sees [lightRadius] cells —
 * the sheet's `STOCK_LIGHT_RADIUS` times the biome's `light` — along lines rock does not block
 * ([lit]); what was ever seen stays [explored], dim. Monsters walk by their [BehaviourRule]: they
 * notice a hero they can see, chase along a path round the rock, hunt the spot they lost them at
 * and go home after `giveUp` seconds; an ambusher waits until the hero is close, a sleeper wakes.
 */
class ExpeditionWorld(
    val map: ExpeditionMap,
    /** One entry per spawn, usually of one monster — a pack (since 2.54.0) is more than one. */
    packs: List<List<RolledMonster>>,
    heroSpeed: Double,
    private val seed: Long,
    lightRadius: Double = DEFAULT_LIGHT,
    bossMonster: RolledMonster? = null,
    hasPortal: Boolean = false,
) {
    /** The hero's pace and sight; both follow the gear when it is changed on the map (since 2.40.0). */
    private var heroSpeed = heroSpeed
    var lightRadius = lightRadius
        private set

    fun regear(speed: Double, light: Double) { heroSpeed = speed; lightRadius = light }

    private val random = Random(seed)
    /** The map's boss (since 2.34.0): the guardian of the exit, standing beside it; none from an older server. */
    val boss: MonsterAgent? = bossMonster?.let { monster -> guardPost()?.let { cell -> MonsterAgent(packs.size, listOf(monster), cell.x + 0.5, cell.y + 0.5) } }
    /**
     * The Vaal portal (since 2.65.0), if this run rolled one — away from the everyday spawns and the
     * exit. Touching it opens the gate; it is gone once the zone was entered or refused.
     */
    var portal: Cell? = if (hasPortal) portalPost() else null
        private set

    /** The portal is spent: entered, refused, or its zone closed. */
    fun closePortal() { portal = null }
    /** Stepping off the portal a refused gate left the hero on: it does not open again underfoot. */
    private var portalArmed = true
    val agents: List<MonsterAgent> = packs.zip(map.spawns).mapIndexed { index, (pack, cell) ->
        MonsterAgent(index, pack, cell.x + 0.5, cell.y + 0.5).also { agent ->
            if (agent.monster.behaviour.type == BehaviourRule.PATROL) agent.patrol = patrolEnd(cell, agent.monster.behaviour.wanderRadius)
        }
    } + listOfNotNull(boss)

    /** The exit does not open while its guardian lives. */
    val sealed: Boolean get() = boss?.alive == true

    /** The server says the boss was slain within the hour: it is not on the map this run. */
    fun bossAbsent() { boss?.alive = false }

    /** Where the guardian stands: the floor nearest the exit, a step or two from it. */
    private fun guardPost(): Cell? {
        val near = distances(map.exit, 3)
        return near.entries.filter { it.value in 1..2 && it.key !in map.spawns }.maxByOrNull { it.value }?.key
    }

    /** Where the Vaal portal stands: far from the start, off the exit and the spawns, by the seed. */
    private fun portalPost(): Cell? {
        val placing = Random(seed * 15485863 + 53)
        val taken = map.spawns.toSet() + map.exit + map.start
        return distances(map.start, Int.MAX_VALUE).filter { (cell, steps) -> steps >= CHEST_STEPS && cell !in taken }.keys.shuffled(placing).firstOrNull()
    }
    var heroX = map.start.x + 0.5
    var heroY = map.start.y + 0.5
    /** The last direction the hero moved in, for the drawing to face. */
    var facingX = 1.0
    var facingY = 0.0
    var moving = false

    /** Every cell the hero has ever seen, floor and rock alike. */
    val explored = BooleanArray(map.width * map.height)
    /** The cells the hero sees right now. */
    val lit = BooleanArray(map.width * map.height)
    private var litFrom: Cell? = null

    /** The chests the server says stand on this map, placed by [placeChests]. */
    val chests = mutableListOf<Chest>()
    /** The fountains, placed by [placeFountains]. */
    val fountains = mutableListOf<Fountain>()

    init { light() }

    /**
     * Puts [count] chests on the map, once: far from the start, off the exit and the monsters'
     * places, a few steps apart, nooks first — a chest is found by walking, not by standing still.
     * Where they stand is the seed's; how many, the server's.
     */
    fun placeChests(count: Int) {
        if (count <= 0 || chests.isNotEmpty()) return
        val placing = Random(seed * 7919 + 17)
        val taken = map.spawns.toSet() + map.exit + map.start + fountains.map { it.cell }
        fun nook(cell: Cell) = STEPS.count { (dx, dy) -> !map.walkable(cell.x + dx, cell.y + dy) }
        val candidates = distances(map.start, Int.MAX_VALUE).filter { (cell, steps) -> steps >= CHEST_STEPS && cell !in taken }.keys
            .shuffled(placing).sortedByDescending(::nook)
        for (cell in candidates) {
            if (chests.size >= count) break
            if (chests.all { hypot((it.cell.x - cell.x).toDouble(), (it.cell.y - cell.y).toDouble()) >= CHEST_SPACING }) chests += Chest(chests.size, cell)
        }
    }

    /**
     * Puts the map's fountains down, once (since 2.48.0): how many, between the rule's low and high,
     * and where, both by the seed — away from the start, the exit, the monsters' places and each other.
     * Each gives back [heal] percent of life, once.
     */
    fun placeFountains(low: Int, high: Int, heal: Double) {
        if (high <= 0 || fountains.isNotEmpty()) return
        val placing = Random(seed * 104729 + 31)
        val count = low + placing.nextInt(high - low + 1)
        if (count <= 0) return
        val taken = map.spawns.toSet() + map.exit + map.start + chests.map { it.cell }
        val candidates = distances(map.start, Int.MAX_VALUE).filter { (cell, steps) -> steps >= FOUNTAIN_STEPS && cell !in taken }.keys.shuffled(placing)
        for (cell in candidates) {
            if (fountains.size >= count) break
            if (fountains.all { hypot((it.cell.x - cell.x).toDouble(), (it.cell.y - cell.y).toDouble()) >= FOUNTAIN_SPACING }) fountains += Fountain(fountains.size, cell, heal)
        }
    }

    fun explored(x: Int, y: Int) = x in 0 until map.width && y in 0 until map.height && explored[y * map.width + x]
    fun lit(x: Int, y: Int) = x in 0 until map.width && y in 0 until map.height && lit[y * map.width + x]

    fun step(dt: Double, stickX: Double, stickY: Double): WorldEvent? {
        val length = hypot(stickX, stickY)
        moving = length > 0.1
        if (moving) {
            val speed = heroSpeed * length.coerceAtMost(1.0)
            facingX = stickX / length
            facingY = stickY / length
            val (nx, ny) = slide(heroX, heroY, facingX * speed * dt, facingY * speed * dt, HERO_RADIUS)
            heroX = nx
            heroY = ny
        }
        light()
        chests.firstOrNull { !it.opened && hypot(it.cell.x + 0.5 - heroX, it.cell.y + 0.5 - heroY) < CHEST_REACH }?.let { chest ->
            chest.opened = true
            return WorldEvent.Opened(chest)
        }
        fountains.firstOrNull { !it.used && hypot(it.cell.x + 0.5 - heroX, it.cell.y + 0.5 - heroY) < CHEST_REACH }?.let { fountain ->
            fountain.used = true
            return WorldEvent.Drank(fountain)
        }
        portal?.let { cell ->
            val near = hypot(cell.x + 0.5 - heroX, cell.y + 0.5 - heroY) < CHEST_REACH
            if (near && portalArmed) { portalArmed = false; return WorldEvent.Portal }
            if (!near) portalArmed = true
        }
        agents.filter { it.alive }.forEach { agent ->
            agent.calm = (agent.calm - dt).coerceAtLeast(0.0)
            val toHero = hypot(heroX - agent.x, heroY - agent.y)
            if (agent.calm <= 0 && toHero < CONTACT) return WorldEvent.Encounter(agent)
            think(agent, toHero, dt)
        }
        if (!sealed && hypot(heroX - (map.exit.x + 0.5), heroY - (map.exit.y + 0.5)) < EXIT_REACH) return WorldEvent.Exit
        return null
    }

    /** Monsters still standing, the boss apart: it is counted as the exit's seal, not as one of them. */
    val alive: Int get() = agents.count { it.alive && it !== boss }
    val total: Int get() = agents.count { it !== boss }

    // ==================== Monsters ====================

    private fun think(agent: MonsterAgent, toHero: Double, dt: Double) {
        val rule = agent.rule
        val sees = agent.calm <= 0 && toHero <= rule.sight && sight(agent.x, agent.y, heroX, heroY)
        when (agent.mode) {
            // A sleeper and an ambusher only stir when the hero is right there.
            AgentMode.ASLEEP, AgentMode.LURKING -> if (sees && toHero <= rule.wake) agent.mode = AgentMode.CHASING
            else -> if (sees) agent.mode = AgentMode.CHASING
        }
        when (agent.mode) {
            AgentMode.ASLEEP, AgentMode.LURKING -> Unit
            AgentMode.CHASING -> {
                agent.lastX = heroX
                agent.lastY = heroY
                agent.unseen = 0.0
                if (!sees) agent.mode = AgentMode.HUNTING
                go(agent, heroX, heroY, rule.chaseSpeed, dt)
            }
            AgentMode.HUNTING -> {
                agent.unseen += dt
                if (agent.unseen >= rule.giveUp) agent.mode = AgentMode.RETURNING
                else go(agent, agent.lastX, agent.lastY, rule.chaseSpeed, dt)
            }
            AgentMode.RETURNING -> if (!go(agent, agent.homeX, agent.homeY, rule.wanderSpeed.coerceAtLeast(MIN_WALK), dt)) {
                agent.mode = if (rule.type == BehaviourRule.AMBUSH) AgentMode.LURKING else AgentMode.IDLE
            }
            AgentMode.IDLE -> roam(agent, rule, dt)
        }
    }

    /** Wandering round home, or walking a patrol: the monster's own business while nobody is near. */
    private fun roam(agent: MonsterAgent, rule: BehaviourRule, dt: Double) {
        if (rule.wanderSpeed <= 0 || rule.type == BehaviourRule.AMBUSH) return
        if (agent.idle > 0) { agent.idle -= dt; return }
        val patrol = agent.patrol
        if (patrol != null) {
            val (tx, ty) = if (agent.outbound) patrol.x + 0.5 to patrol.y + 0.5 else agent.homeX to agent.homeY
            if (!go(agent, tx, ty, rule.wanderSpeed, dt)) { agent.outbound = !agent.outbound; agent.idle = 0.8 + random.nextDouble() }
            return
        }
        if (!walk(agent, rule.wanderSpeed, dt)) {
            agent.idle = 1 + random.nextDouble() * 2.5
            pickWanderTarget(agent, rule.wanderRadius)
        }
    }

    /**
     * Heads for ([tx], [ty]): straight when nothing is in the way, along a path round the rock
     * otherwise. False once it has arrived or has no way there.
     */
    private fun go(agent: MonsterAgent, tx: Double, ty: Double, speed: Double, dt: Double): Boolean {
        if (hypot(tx - agent.x, ty - agent.y) < 0.1) return false
        if (sight(agent.x, agent.y, tx, ty)) {
            agent.targetX = tx
            agent.targetY = ty
            // A corner can still catch a body wider than the line: then the path takes over.
            if (walk(agent, speed, dt)) { agent.path = emptyList(); return true }
        }
        val goal = Cell(floor(tx).toInt(), floor(ty).toInt())
        agent.repath -= dt
        if (agent.pathTo != goal || agent.repath <= 0 || agent.path.isEmpty()) {
            agent.path = path(Cell(floor(agent.x).toInt(), floor(agent.y).toInt()), goal).drop(1)
            agent.pathTo = goal
            agent.repath = REPATH
        }
        val next = agent.path.firstOrNull() ?: return false
        agent.targetX = next.x + 0.5
        agent.targetY = next.y + 0.5
        if (hypot(agent.targetX - agent.x, agent.targetY - agent.y) < 0.2) agent.path = agent.path.drop(1)
        walk(agent, speed, dt)
        return true
    }

    /** Moves toward the target; false once it has arrived or a wall stopped it. */
    private fun walk(agent: MonsterAgent, speed: Double, dt: Double): Boolean {
        val dx = agent.targetX - agent.x
        val dy = agent.targetY - agent.y
        val distance = hypot(dx, dy)
        if (distance < 0.05) return false
        val stepLength = (speed * dt).coerceAtMost(distance)
        val (nx, ny) = slide(agent.x, agent.y, dx / distance * stepLength, dy / distance * stepLength, MONSTER_RADIUS)
        val moved = hypot(nx - agent.x, ny - agent.y) > stepLength * 0.2
        agent.x = nx
        agent.y = ny
        return moved
    }

    private fun pickWanderTarget(agent: MonsterAgent, radius: Double) {
        repeat(8) {
            val tx = agent.homeX + (random.nextDouble() * 2 - 1) * radius
            val ty = agent.homeY + (random.nextDouble() * 2 - 1) * radius
            if (map.walkable(floor(tx).toInt(), floor(ty).toInt())) { agent.targetX = tx; agent.targetY = ty; return }
        }
    }

    /** The far end of a patrol: the reachable floor furthest from home within [radius] steps. */
    private fun patrolEnd(home: Cell, radius: Double): Cell? {
        val limit = ceil(radius).toInt().coerceAtLeast(1)
        val distance = distances(home, limit)
        return distance.entries.filter { it.value == limit }.map { it.key }.let { far -> if (far.isEmpty()) null else far[random.nextInt(far.size)] }
    }

    // ==================== The grid ====================

    /**
     * The shortest way from [from] to [to] over floor, eight ways, never cutting a corner of rock;
     * empty when there is none. Both ends are included.
     */
    fun path(from: Cell, to: Cell): List<Cell> {
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
            for ((dx, dy) in STEPS) {
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
        while (index != start) { cells += Cell(index % map.width, index / map.width); index = previous[index] }
        cells += from
        return cells.reversed()
    }

    /** Steps from [from] to every floor cell within [limit] steps, four ways. */
    private fun distances(from: Cell, limit: Int): Map<Cell, Int> {
        val seen = mutableMapOf(from to 0)
        val queue = ArrayDeque<Cell>().apply { add(from) }
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val d = seen.getValue(cell)
            if (d == limit) continue
            for ((dx, dy) in STEPS.take(4)) {
                val next = Cell(cell.x + dx, cell.y + dy)
                if (map.walkable(next.x, next.y) && next !in seen) { seen[next] = d + 1; queue.add(next) }
            }
        }
        return seen
    }

    /** Whether a straight line from one point to another crosses no rock. */
    fun sight(ax: Double, ay: Double, bx: Double, by: Double): Boolean {
        val distance = hypot(bx - ax, by - ay)
        val steps = ceil(distance / SIGHT_STEP).toInt()
        for (i in 1 until steps) {
            val t = i.toDouble() / steps
            if (!map.walkable(floor(ax + (bx - ax) * t).toInt(), floor(ay + (by - ay) * t).toInt())) return false
        }
        return true
    }

    /** What the hero sees from the cell they stand in, worked out again only when they leave it. */
    private fun light() {
        val here = Cell(floor(heroX).toInt(), floor(heroY).toInt())
        if (here == litFrom) return
        litFrom = here
        lit.fill(false)
        val reach = ceil(lightRadius).toInt()
        for (y in here.y - reach..here.y + reach) for (x in here.x - reach..here.x + reach) {
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

    /** A move that slides along walls: each axis is tried on its own, so a diagonal into a wall still glides. */
    private fun slide(x: Double, y: Double, dx: Double, dy: Double, radius: Double): Pair<Double, Double> {
        val nx = if (free(x + dx, y, radius)) x + dx else x
        val ny = if (free(nx, y + dy, radius)) y + dy else y
        return nx to ny
    }

    private fun free(x: Double, y: Double, radius: Double): Boolean =
        listOf(-radius to -radius, radius to -radius, -radius to radius, radius to radius)
            .all { (ox, oy) -> map.walkable(floor(x + ox).toInt(), floor(y + oy).toInt()) }

    /** The hero stepped back from a fight nobody won: the monster lets them go for a while, and goes home. */
    fun retreatFrom(agent: MonsterAgent) {
        agent.calm = CALM_AFTER_RETREAT
        agent.mode = AgentMode.RETURNING
    }

    companion object {
        const val HERO_SPEED = 3.2
        const val HERO_RADIUS = 0.28
        const val MONSTER_RADIUS = 0.3
        const val CONTACT = 0.8
        const val EXIT_REACH = 0.7
        const val CALM_AFTER_RETREAT = 4.0
        const val CHEST_REACH = 0.7
        /** A chest stands at least this many steps from the start and this far from another chest. */
        const val CHEST_STEPS = 8
        const val CHEST_SPACING = 5.0
        /** A fountain stands at least this many steps from the start and this far from another one. */
        const val FOUNTAIN_STEPS = 6
        const val FOUNTAIN_SPACING = 8.0
        /** What a hero sees by when the server has not said: the level-1 base since server 0.30.0. */
        const val DEFAULT_LIGHT = 5.0
        private const val MIN_WALK = 0.8
        private const val REPATH = 0.4
        private const val SIGHT_STEP = 0.25
        private val STEPS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)

        /**
         * The stick's direction on screen, turned into world axes.
         *
         * The map is drawn as a diamond grid — a tile twice as wide as it is tall, `x` running down to
         * the right and `y` down to the left — so a push straight up the screen is a step toward the
         * top corner, which is minus both. [screenY] grows downward.
         */
        fun screenToWorld(screenX: Double, screenY: Double): Pair<Double, Double> {
            val wx = screenX + 2 * screenY
            val wy = 2 * screenY - screenX
            val length = hypot(wx, wy)
            if (length < 1e-9) return 0.0 to 0.0
            val push = hypot(screenX, screenY).coerceAtMost(1.0)
            return wx / length * push to wy / length * push
        }

        /**
         * A new run of [map]: how many monsters, the ground they stand on and what each one rolled,
         * all from one seed.
         */
        fun create(map: CampaignMap, rarities: List<CampaignRarity>, heroStats: Map<String, Double>, seed: Long,
                   mapBuffs: List<MonsterEffect> = emptyList(), portalChance: Double = 0.0): ExpeditionWorld {
            val random = Random(seed)
            val (low, high) = map.monsterCount.let { (it.getOrNull(0) ?: 10) to (it.getOrNull(1) ?: 14) }
            // The map's size is the server's since 0.40.0; room for the pack a map's modifier asks for comes with it.
            val layout = MapGenerator.generate(seed, map.biome, random.nextInt(low, high + 1), map.size)
            // A pack's own stream (2.54.0), so whether a spawn is one or several never shifts the
            // ordinary roll that follows it — the same seed still hands out the same single monsters.
            val packRandom = Random(seed * 32452843 + 71)
            val packs = List(layout.spawns.size) { MonsterRoller.rollPack(map, rarities, random, packRandom).map { it.copy(mapBuffs = mapBuffs) } }
            return ExpeditionWorld(layout, packs, heroSpeed(heroStats), seed, lightRadius(heroStats, map.light),
                MonsterRoller.boss(map, rarities)?.copy(mapBuffs = mapBuffs),
                MonsterRoller.portal(map, portalChance, random))
        }

        /** The hero's pace, sped up by movement speed from the sheet. */
        fun heroSpeed(stats: Map<String, Double>): Double = HERO_SPEED * (1 + (stats["STOCK_MOVEMENT_SPEED"] ?: 0.0) / 100).coerceIn(0.5, 2.5)

        /** How far the hero sees: the sheet's light radius — the base, if the server sent none — times the biome's light. */
        fun lightRadius(stats: Map<String, Double>, biomeLight: Double): Double =
            ((stats["STOCK_LIGHT_RADIUS"]?.takeIf { it > 0 } ?: DEFAULT_LIGHT) * biomeLight).coerceIn(MIN_LIGHT, MAX_LIGHT)

        const val MIN_LIGHT = 2.0
        const val MAX_LIGHT = 14.0
    }
}
