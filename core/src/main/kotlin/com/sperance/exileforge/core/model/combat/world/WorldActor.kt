package com.sperance.exileforge.core.model.combat.world

/** Which camp a body belongs to. */
enum class WorldSide { HERO, MOB }

/** Animation states and how long each one plays; IDLE and GONE never expire. */
enum class WorldPose(val millis: Long) {
    SPAWN(420), IDLE(0), WINDUP(200), STRIKE(180), RECOIL(240),
    GUARD(520), QUAFF(440), CAST(340), DEATH(760), GONE(0);
    val timed: Boolean get() = millis > 0L
}

/**
 * One body on the ground.
 *
 * [life], [maxLife] and [shield] are copied from the server's `Combatant`; they are refreshed when a
 * new battle snapshot arrives and are never changed by the walk or the animation. A roaming monster
 * carries the life its zone entry declares until the server puts it in a battle and [engaged] is set.
 */
class WorldActor(
    val id: String, val side: WorldSide, val name: String, val icon: String,
    val element: String = "physical", val boss: Boolean = false, val radius: Float = MOB_RADIUS,
    home: Vec2 = Vec2()
) {
    var position: Vec2 = home; internal set
    /** Where a roamer drifts back to when it has nobody to chase. */
    var anchor: Vec2 = home; internal set
    /** True for the one monster the server actually put in the open battle. */
    var engaged: Boolean = false; internal set
    var velocity: Vec2 = Vec2.ZERO; private set
    var heading: Vec2 = Vec2(0f, 1f); private set
    var pose: WorldPose = WorldPose.SPAWN; private set
    var poseElapsed: Long = 0L; private set
    var life: Double = 0.0; internal set
    var maxLife: Double = 1.0; internal set
    var shield: Double = 0.0; internal set
    /** Hit whiten: 1 right after an impact, decaying to 0. */
    var flash: Float = 0f; internal set
    /** Idle clock, seeded per actor so a pack does not breathe in lockstep. */
    var bob: Float = 0f; private set
    /** The walk this body is following, so a corner in a corridor is not a dead end. */
    internal val route = Route()
    val alive: Boolean get() = life > 0.0
    val gone: Boolean get() = pose == WorldPose.GONE
    val walking: Boolean get() = velocity.length > WALK_EPSILON
    val lifeRatio: Float get() = (life / maxLife.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    /** 0..1 through the current pose; the renderer drives lunges, wards and fades from it. */
    val poseProgress: Float get() = if(!pose.timed) 0f else (poseElapsed.toFloat() / pose.millis).coerceIn(0f, 1f)

    internal fun play(next: WorldPose) { pose = next; poseElapsed = 0L }
    internal fun seedBob(value: Float) { bob = value }
    internal fun place(at: Vec2) { position = at; anchor = at; velocity = Vec2.ZERO; route.forget() }
    internal fun face(target: Vec2) { (target - position).direction.takeIf { it != Vec2.ZERO }?.let { heading = it } }

    /**
     * Kinematics only: steer towards [desired] (a unit vector at most, zero to halt), then obey the map.
     *
     * Momentum is what makes a flick of the thumb read as a step rather than a teleport, and it is the
     * only physics in the game — every number belongs to the server.
     */
    internal fun step(dt: Long, desired: Vec2, speed: Float, map: Battlefield) {
        val seconds = dt / 1000f
        val blend = (seconds * if(desired.length > 0f) ACCELERATION else BRAKING).coerceIn(0f, 1f)
        velocity = velocity.lerp(desired * speed, blend)
        if(velocity.length < WALK_EPSILON) velocity = Vec2.ZERO
        else {
            val moved = map.resolve(position, position + velocity * seconds, radius)
            // A wall took the step: drop the speed that went into it rather than grinding along at full tilt.
            if(moved.distanceTo(position) < velocity.length * seconds * .5f) velocity = velocity * .35f
            position = moved
            heading = velocity.direction
        }
        bob = (bob + seconds) % 1000f
        flash = (flash - dt / 260f).coerceAtLeast(0f)
        if(pose.timed) {
            poseElapsed += dt
            if(poseElapsed >= pose.millis) play(if(pose == WorldPose.DEATH) WorldPose.GONE else WorldPose.IDLE)
        }
    }

    companion object {
        const val ACCELERATION = 9f
        const val BRAKING = 13f
        const val WALK_EPSILON = .06f
        /** Every body stays narrower than a tile, so no doorway the maps carve can swallow one. */
        const val HERO_RADIUS = .38f
        const val MOB_RADIUS = .38f
        const val BOSS_RADIUS = .45f
    }
}

/**
 * A walk towards a moving goal, recomputed only when it is worth recomputing.
 *
 * Steering straight at a target is what walks into a wall and stays there, so the route is a chain of
 * tile centres from [Battlefield.path]; waypoints the body can already see past are dropped, which is
 * what keeps an open floor from being walked like a staircase.
 */
internal class Route {
    private var goal: Vec2? = null
    private var steps = ArrayList<Vec2>()
    private var age = 0L

    fun forget() { goal = null; steps = ArrayList(); age = 0L }

    fun direction(from: Vec2, to: Vec2, radius: Float, map: Battlefield, dt: Long): Vec2 {
        age += dt
        val known = goal
        if(known == null || known.distanceTo(to) > RETARGET || age > REFRESH) {
            goal = to
            steps = ArrayList(map.path(from, to, radius))
            age = 0L
        }
        while(steps.isNotEmpty() && from.distanceTo(steps[0]) < REACHED) steps.removeAt(0)
        while(steps.size > 1 && map.clear(from, steps[1], radius)) steps.removeAt(0)
        return ((steps.firstOrNull() ?: to) - from).direction
    }

    private companion object {
        const val RETARGET = 1.5f
        const val REFRESH = 450L
        const val REACHED = .45f
    }
}
