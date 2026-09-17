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
 * new battle snapshot arrives and are never changed by the walk or the animation.
 */
class WorldActor(
    val id: String, val side: WorldSide, val name: String, val icon: String,
    val element: String = "physical", val boss: Boolean = false, val radius: Float = .42f,
    /** Pack members the server is not fighting: they mill around their camp and take no part. */
    val ambient: Boolean = false, home: Vec2 = Vec2()
) {
    var position: Vec2 = home; internal set
    var anchor: Vec2 = home; internal set
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
    val alive: Boolean get() = life > 0.0
    val gone: Boolean get() = pose == WorldPose.GONE
    val walking: Boolean get() = velocity.length > WALK_EPSILON
    val lifeRatio: Float get() = (life / maxLife.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    /** 0..1 through the current pose; the renderer drives lunges, wards and fades from it. */
    val poseProgress: Float get() = if(!pose.timed) 0f else (poseElapsed.toFloat() / pose.millis).coerceIn(0f, 1f)

    internal fun play(next: WorldPose) { pose = next; poseElapsed = 0L }
    internal fun seedBob(value: Float) { bob = value }
    internal fun face(target: Vec2) { (target - position).direction.takeIf { it != Vec2.ZERO }?.let { heading = it } }

    /**
     * Kinematics only: steer towards [desired] (a unit vector at most, zero to halt), then obey the walls.
     *
     * Momentum is what makes a flick of the thumb read as a step rather than a teleport, and it is the
     * only physics in the game — every number belongs to the server.
     */
    internal fun step(dt: Long, desired: Vec2, speed: Float, field: Battlefield) {
        val seconds = dt / 1000f
        val blend = (seconds * if(desired.length > 0f) ACCELERATION else BRAKING).coerceIn(0f, 1f)
        velocity = velocity.lerp(desired * speed, blend)
        if(velocity.length < WALK_EPSILON) velocity = Vec2.ZERO
        else {
            position = field.resolve(position + velocity * seconds, radius)
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
    }
}
