package com.sperance.exileforge.core.model.combat.arena

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Arena space is the unit square: x grows to the right, y downwards, 1 is the whole stage.
 *
 * Positions are a picture, never an input to an outcome: the server has no notion of where anyone
 * stands, so nothing here may feed back into damage, hit chance or loot. See [ArenaSimulation].
 */
data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(factor: Float) = Vec2(x * factor, y * factor)
    val length: Float get() = sqrt(x * x + y * y)
    fun lerp(other: Vec2, t: Float) = Vec2(x + (other.x - x) * t, y + (other.y - y) * t)
    /** Walks at most [step] units towards [target], landing exactly on it instead of overshooting. */
    fun toward(target: Vec2, step: Float): Vec2 = (target - this).let { d ->
        if(d.length <= step || d.length == 0f) target else this + d * (step / d.length)
    }
}

/** Which line of the stage an actor fights on. */
enum class ArenaSide { HERO, MOB }

/** Animation states and how long each one plays; IDLE and GONE never expire. */
enum class ArenaPose(val millis: Long) {
    SPAWN(420), IDLE(0), ADVANCE(300), WINDUP(220), STRIKE(170), RECOIL(240),
    GUARD(560), QUAFF(460), CAST(320), DEATH(680), GONE(0);
    val timed: Boolean get() = millis > 0L
}

/**
 * One drawn fighter.
 *
 * [life], [maxLife] and [shield] are copied from the server's `Combatant`; they are refreshed when a
 * new battle snapshot arrives and are never decremented by the animation itself.
 */
class ArenaActor(
    val id: String, val side: ArenaSide, val name: String, val icon: String,
    val element: String = "physical", val boss: Boolean = false, val home: Vec2 = Vec2(),
    /** Queued pack members are drawn waiting behind the fight and take no part in it. */
    val queued: Boolean = false
) {
    var position: Vec2 = home; internal set
    var goal: Vec2 = home; internal set
    var pose: ArenaPose = ArenaPose.SPAWN; private set
    var poseElapsed: Long = 0L; private set
    var life: Double = 0.0; internal set
    var maxLife: Double = 1.0; internal set
    var shield: Double = 0.0; internal set
    var facing: Float = if(side == ArenaSide.HERO) 1f else -1f; internal set
    /** Hit whiten: 1 right after an impact, decaying to 0. */
    var flash: Float = 0f; internal set
    /** Idle bob clock, seeded per actor so a pack does not breathe in lockstep. */
    var bob: Float = 0f; private set
    val alive: Boolean get() = life > 0.0
    val gone: Boolean get() = pose == ArenaPose.GONE
    val lifeRatio: Float get() = (life / maxLife.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    /** 0..1 through the current pose; the renderer drives lunges, shields and fades from it. */
    val poseProgress: Float get() = if(!pose.timed) 0f else (poseElapsed.toFloat() / pose.millis).coerceIn(0f, 1f)

    internal fun play(next: ArenaPose) { pose = next; poseElapsed = 0L }
    internal fun seedBob(value: Float) { bob = value }
    internal fun step(dt: Long, speed: Float) {
        position = position.toward(goal, speed * dt / 1000f)
        bob = (bob + dt / 1000f) % 1000f
        flash = (flash - dt / 260f).coerceAtLeast(0f)
        if(pose.timed) {
            poseElapsed += dt
            if(poseElapsed >= pose.millis) play(if(pose == ArenaPose.DEATH) ArenaPose.GONE else ArenaPose.IDLE)
        }
    }
}

/** Colour family of a floating number or a spark; the renderer owns the actual palette. */
enum class ArenaTint { HERO_DAMAGE, MOB_DAMAGE, HEAL, MANA, SHIELD, MISS, GUARD }

/** What kind of reward a mote carries, decided from the fields the server filled in. */
enum class LootKind { EXPERIENCE, GOLD, EQUIPMENT, CURRENCY }

/** Anything that is born, ages and is swept off the stage once its time is up. */
abstract class ArenaParticle(val life: Long) {
    var age: Long = 0L; private set
    val progress: Float get() = (age.toFloat() / life).coerceIn(0f, 1f)
    internal fun spent(dt: Long): Boolean { age += dt; return age >= life }
}

/** A number rising off a fighter. [text] is already formatted — the arena never rounds twice. */
class ArenaNumber(val text: String, val origin: Vec2, val tint: ArenaTint, val crit: Boolean = false) :
    ArenaParticle(if(crit) 1200L else 950L)

/** Impact debris. Angle and speed are fixed at birth, so a burst keeps its shape across frames. */
class ArenaSpark(val origin: Vec2, val angle: Float, val speed: Float, val tint: ArenaTint) : ArenaParticle(520L)

/** A reward the server granted, drifting from the kill towards the hero. */
class ArenaLootMote(val name: String, val icon: String, val kind: LootKind, val origin: Vec2, val destination: Vec2) :
    ArenaParticle(1500L) {
    val position: Vec2 get() = origin.lerp(destination, progress)
}

/** A banner across the stage: a mob's name, a boss call, victory or defeat. */
class ArenaBanner(val text: String, val boss: Boolean) : ArenaParticle(2200L)

/** What is left where a mob fell. It fades to a stain and keeps its spot for the rest of the run. */
class ArenaCorpse(val icon: String, val position: Vec2, val boss: Boolean) : ArenaParticle(Long.MAX_VALUE) {
    val fade: Float get() = (1f - age / 2600f).coerceIn(.18f, 1f)
}

/** Beats are the timeline the simulation plays back; one server turn expands into several. */
enum class ArenaBeatKind {
    SPAWN, ADVANCE, WITHDRAW, WINDUP, STRIKE, CAST, IMPACT, MISS, GUARD, QUAFF, RETREAT,
    ABSORB, REGEN, MANA, DEATH, LOOT, BANNER
}

/**
 * One scheduled moment of the fight.
 *
 * [amount] only ever carries a number the server sent — a life, mana or shield difference between two
 * snapshots — so playback cannot show a value the server did not produce.
 */
data class ArenaBeat(
    val at: Long, val kind: ArenaBeatKind, val actor: String, val target: String = "",
    val amount: Double = 0.0, val element: String = "physical", val crit: Boolean = false,
    val text: String = "", val loot: LootKind = LootKind.CURRENCY, val icon: String = ""
)

/** Stage layout in arena units: where each side stands, waits and falls back to. */
object ArenaStageLayout {
    val heroHome = Vec2(.20f, .66f)
    val heroMelee = Vec2(.40f, .66f)
    val mobMelee = Vec2(.62f, .64f)
    val mobEntry = Vec2(1.08f, .64f)
    val heroRetreat = Vec2(-.16f, .68f)
    /** Where the rest of the zone waits its turn; an index past the list clamps to the last slot. */
    val queue = listOf(Vec2(.80f, .50f), Vec2(.91f, .70f), Vec2(.99f, .57f))
    const val WALK_SPEED = 1.5f
    fun queueSlot(index: Int) = queue[index.coerceIn(0, queue.lastIndex)]
    fun corpseSpot(from: Vec2) = Vec2(from.x + .03f, from.y + .02f)
}

/** Millisecond offsets inside a turn, so the whole rhythm is tuned in one place. */
object ArenaTiming {
    const val CLOSE = 0L
    const val HERO_WINDUP = 260L
    const val HERO_STRIKE = 470L
    const val HERO_IMPACT = 560L
    const val MOB_WINDUP = 880L
    const val MOB_STRIKE = 1090L
    const val MOB_IMPACT = 1170L
    const val AFTERMATH = 1340L
    const val LOOT_STRIDE = 110L
    /** Guard, flask and retreat skip the hero's swing, so the mob answers sooner. */
    const val DEFENSIVE_WINDUP = 380L
    const val DEFENSIVE_STRIKE = 560L
    const val DEFENSIVE_IMPACT = 640L
}

/** Whole numbers stay whole: the server's doubles are shown as it rolled them, not re-rounded. */
internal fun formatAmount(value: Double): String =
    if(abs(value - value.toLong()) < .05) abs(value.toLong()).toString() else "%.1f".format(abs(value))
