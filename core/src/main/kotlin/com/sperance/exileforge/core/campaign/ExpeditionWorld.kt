package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

/** A monster walking the map: where it lives, where it is going, and whether it is still there. */
class MonsterAgent(val id: Int, val monster: RolledMonster, val homeX: Double, val homeY: Double) {
    var x = homeX
    var y = homeY
    var targetX = homeX
    var targetY = homeY
    var idle = 0.0
    /** Seconds it will neither chase nor fight: a monster the hero ran from does not pounce at once. */
    var calm = 0.0
    var alive = true
    var chasing = false
}

/** What a step of the world ran into. */
sealed interface WorldEvent {
    data class Encounter(val agent: MonsterAgent) : WorldEvent
    data object Exit : WorldEvent
}

/**
 * The map in motion — positions in tile units, stepped by the scene every frame.
 *
 * It is pure so a test can walk it: the scene reads positions from here and draws them, and the
 * stick's direction comes in already in world axes (see [screenToWorld]). Monsters wander around
 * where they stood, chase a hero who comes close and start a fight on contact; the exit ends the
 * map. None of this is a game rule the server knows — it is how a run feels, and nothing of it
 * travels except which monster was killed.
 */
class ExpeditionWorld(val map: ExpeditionMap, monsters: List<RolledMonster>, private val heroSpeed: Double, seed: Long) {
    private val random = Random(seed)
    val agents: List<MonsterAgent> = monsters.zip(map.spawns).mapIndexed { index, (monster, cell) ->
        MonsterAgent(index, monster, cell.x + 0.5, cell.y + 0.5)
    }
    var heroX = map.start.x + 0.5
    var heroY = map.start.y + 0.5
    /** The last direction the hero moved in, for the drawing to face. */
    var facingX = 1.0
    var facingY = 0.0
    var moving = false

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
        agents.filter { it.alive }.forEach { agent ->
            agent.calm = (agent.calm - dt).coerceAtLeast(0.0)
            val toHero = hypot(heroX - agent.x, heroY - agent.y)
            if (agent.calm <= 0 && toHero < CONTACT) return WorldEvent.Encounter(agent)
            agent.chasing = agent.calm <= 0 && toHero < AGGRO
            if (agent.chasing) {
                agent.targetX = heroX
                agent.targetY = heroY
                walk(agent, CHASE_SPEED, dt)
            } else if (agent.idle > 0) {
                agent.idle -= dt
            } else if (!walk(agent, WANDER_SPEED, dt)) {
                agent.idle = 1 + random.nextDouble() * 2.5
                pickWanderTarget(agent)
            }
        }
        if (hypot(heroX - (map.exit.x + 0.5), heroY - (map.exit.y + 0.5)) < EXIT_REACH) return WorldEvent.Exit
        return null
    }

    val alive: Int get() = agents.count { it.alive }

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

    private fun pickWanderTarget(agent: MonsterAgent) {
        repeat(8) {
            val tx = agent.homeX + (random.nextDouble() * 2 - 1) * WANDER_RADIUS
            val ty = agent.homeY + (random.nextDouble() * 2 - 1) * WANDER_RADIUS
            if (map.walkable(floor(tx).toInt(), floor(ty).toInt())) { agent.targetX = tx; agent.targetY = ty; return }
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

    /** The hero stepped back from a fight nobody won: the monster lets them go for a while. */
    fun retreatFrom(agent: MonsterAgent) { agent.calm = CALM_AFTER_RETREAT }

    companion object {
        const val HERO_SPEED = 3.2
        const val HERO_RADIUS = 0.28
        const val MONSTER_RADIUS = 0.3
        const val WANDER_SPEED = 1.1
        const val CHASE_SPEED = 2.2
        const val WANDER_RADIUS = 3.0
        const val AGGRO = 3.5
        const val CONTACT = 0.8
        const val EXIT_REACH = 0.7
        const val CALM_AFTER_RETREAT = 4.0

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
        fun create(map: CampaignMap, rarities: List<CampaignRarity>, heroStats: Map<String, Double>, seed: Long): ExpeditionWorld {
            val random = Random(seed)
            val (low, high) = map.monsterCount.let { (it.getOrNull(0) ?: 10) to (it.getOrNull(1) ?: 14) }
            val layout = MapGenerator.generate(seed, map.biome, random.nextInt(low, high + 1))
            val monsters = List(layout.spawns.size) { MonsterRoller.roll(map, rarities, random) }
            return ExpeditionWorld(layout, monsters, heroSpeed(heroStats), seed)
        }

        /** The hero's pace, sped up by movement speed from the sheet. */
        fun heroSpeed(stats: Map<String, Double>): Double = HERO_SPEED * (1 + (stats["STOCK_MOVEMENT_SPEED"] ?: 0.0) / 100).coerceIn(0.5, 2.5)
    }
}
