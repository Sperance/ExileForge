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

// ==================== Monsters ====================

internal fun ExpeditionWorld.think(agent: MonsterAgent, toHero: Double, dt: Double) {
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
            if (agent.unseen >= rule.giveUp) {
                agent.mode = AgentMode.RETURNING
            } else {
                go(agent, agent.lastX, agent.lastY, rule.chaseSpeed, dt)
            }
        }

        AgentMode.RETURNING -> if (!go(agent, agent.homeX, agent.homeY, rule.wanderSpeed.coerceAtLeast(ExpeditionWorld.MIN_WALK), dt)) {
            agent.mode = if (rule.type == Behaviours.AMBUSH) AgentMode.LURKING else AgentMode.IDLE
        }

        AgentMode.IDLE -> roam(agent, rule, dt)
    }
}

/** Wandering round home, or walking a patrol: the monster's own business while nobody is near. */
internal fun ExpeditionWorld.roam(agent: MonsterAgent, rule: BehaviourRule, dt: Double) {
    if (rule.wanderSpeed <= 0 || rule.type == Behaviours.AMBUSH) return
    if (agent.idle > 0) {
        agent.idle -= dt
        return
    }
    val patrol = agent.patrol
    if (patrol != null) {
        val (tx, ty) = if (agent.outbound) patrol.x + 0.5 to patrol.y + 0.5 else agent.homeX to agent.homeY
        if (!go(agent, tx, ty, rule.wanderSpeed, dt)) {
            agent.outbound = !agent.outbound
            agent.idle = 0.8 + random.nextDouble()
        }
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
internal fun ExpeditionWorld.go(agent: MonsterAgent, tx: Double, ty: Double, speed: Double, dt: Double): Boolean {
    if (hypot(tx - agent.x, ty - agent.y) < 0.1) return false
    if (sight(agent.x, agent.y, tx, ty)) {
        agent.targetX = tx
        agent.targetY = ty
        // A corner can still catch a body wider than the line: then the path takes over.
        if (walk(agent, speed, dt)) {
            agent.path = emptyList()
            return true
        }
    }
    val goal = Cell(floor(tx).toInt(), floor(ty).toInt())
    agent.repath -= dt
    if (agent.pathTo != goal || agent.repath <= 0 || agent.path.isEmpty()) {
        agent.path = path(Cell(floor(agent.x).toInt(), floor(agent.y).toInt()), goal).drop(1)
        agent.pathTo = goal
        agent.repath = ExpeditionWorld.REPATH
    }
    val next = agent.path.firstOrNull() ?: return false
    agent.targetX = next.x + 0.5
    agent.targetY = next.y + 0.5
    if (hypot(agent.targetX - agent.x, agent.targetY - agent.y) < 0.2) agent.path = agent.path.drop(1)
    walk(agent, speed, dt)
    return true
}

/** Moves toward the target; false once it has arrived or a wall stopped it. */
internal fun ExpeditionWorld.walk(agent: MonsterAgent, speed: Double, dt: Double): Boolean {
    val dx = agent.targetX - agent.x
    val dy = agent.targetY - agent.y
    val distance = hypot(dx, dy)
    if (distance < 0.05) return false
    val stepLength = (speed * dt).coerceAtMost(distance)
    val (nx, ny) = slide(agent.x, agent.y, dx / distance * stepLength, dy / distance * stepLength, rules.monsterRadius)
    val moved = hypot(nx - agent.x, ny - agent.y) > stepLength * 0.2
    agent.x = nx
    agent.y = ny
    return moved
}

internal fun ExpeditionWorld.pickWanderTarget(agent: MonsterAgent, radius: Double) {
    repeat(8) {
        val tx = agent.homeX + (random.nextDouble() * 2 - 1) * radius
        val ty = agent.homeY + (random.nextDouble() * 2 - 1) * radius
        if (map.walkable(floor(tx).toInt(), floor(ty).toInt())) {
            agent.targetX = tx
            agent.targetY = ty
            return
        }
    }
}

/** The far end of a patrol: the reachable floor furthest from home within [radius] steps. */
internal fun ExpeditionWorld.patrolEnd(home: Cell, radius: Double): Cell? {
    val limit = ceil(radius).toInt().coerceAtLeast(1)
    val distance = distances(home, limit)
    return distance.entries.filter { it.value == limit }.map { it.key }.let { far -> if (far.isEmpty()) null else far[random.nextInt(far.size)] }
}
