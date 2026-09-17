package com.sperance.exileforge.core.model.combat.world

import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.BattleAction
import com.sperance.exileforge.core.model.combat.BattleStatus
import com.sperance.exileforge.core.model.combat.Monster
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The isometric expedition: where everyone stands, what they are playing, and what lies on the floor.
 *
 * It is a renderer and a pair of legs, never a referee. Life, mana, shield, damage, rewards and the
 * outcome all arrive in [observe] as server snapshots; the only arithmetic here is kinematics — walking,
 * colliding with a stone, ageing particles — plus the subtraction in [battleDelta] that reads what the
 * server changed. There is deliberately no hit roll, no armour, no resistance and no loot chance: those
 * live on the server, and a second implementation here would be a second set of numbers to disagree
 * with. Modifiers, passives and resistances are therefore already inside every number it shows.
 *
 * The one thing a position decides is *when* the client asks: [takeAction] offers the next command once
 * the hero is within reach and the cadence has elapsed. The caller sends it as the same durable,
 * idempotent command a tap would send, and the answer comes back through [observe] like any other.
 *
 * Mutable on purpose: it is stepped once per displayed frame, and rebuilding an immutable graph sixty
 * times a second to move a few dozen bodies would cost more than the drawing does.
 */
class WorldSimulation {
    private val pack = mutableListOf<WorldActor>()
    private val beats = ArrayDeque<WorldBeat>()
    private val floaters = mutableListOf<DamageNumber>()
    private val debris = mutableListOf<Spark>()
    private val flying = mutableListOf<LootMote>()
    private val fallen = mutableListOf<Corpse>()
    private val ground = mutableListOf<GroundLoot>()
    private val picked = mutableListOf<GroundLoot>()
    private var snapshot: Battle? = null
    private var battleId = ""
    private var zoneId = ""
    private var appliedTurn = -1
    private var appliedStatus: BattleStatus? = null
    private var seed = 0
    private var desire = Vec2.ZERO
    private var cooldown = 0L
    private var swings = 0

    var field: Battlefield = Battlefield.of(""); private set
    var hero: WorldActor? = null; private set
    var banner: Banner? = null; private set
    /** The point the camera is centred on; it chases the hero instead of snapping to them. */
    var focus: Vec2 = field.centre; private set
    /** Camera kick, 0..1, decaying; the renderer offsets the whole view by it. */
    var shake = 0f; private set
    var clock = 0L; private set
    /** The hero fights on its own. Switching it off leaves the manual rail and nothing else. */
    var auto = true

    val mobs: List<WorldActor> get() = pack
    val numbers: List<DamageNumber> get() = floaters
    val sparks: List<Spark> get() = debris
    val motes: List<LootMote> get() = flying
    val corpses: List<Corpse> get() = fallen
    /** Rewards the server granted, still lying where they fell. */
    val loot: List<GroundLoot> get() = ground
    /** What the hero has walked over, oldest first; the HUD shows the tail of it. */
    val collected: List<GroundLoot> get() = picked
    /** The one mob the server is actually fighting; the rest of [mobs] is the zone milling about. */
    val engaged: WorldActor? get() = pack.firstOrNull { !it.ambient }
    /** True while a turn is still playing out; nothing new is asked for until it settles. */
    val playing: Boolean get() = beats.isNotEmpty()
    /** Whether the hero stands close enough to swing at the mob the server put in the battle. */
    val inStrikeRange: Boolean get() {
        val body = hero?.takeIf { it.alive } ?: return false
        val target = engaged?.takeIf { it.alive && !it.gone } ?: return false
        return body.position.distanceTo(target.position) <= STRIKE_RANGE
    }

    /** Forgets the world completely: another character, another server, or a logout. */
    fun reset() {
        hero = null; banner = null; shake = 0f; clock = 0L; seed = 0; desire = Vec2.ZERO
        cooldown = 0L; swings = 0
        pack.clear(); beats.clear(); floaters.clear(); debris.clear(); flying.clear(); fallen.clear()
        ground.clear(); picked.clear()
        snapshot = null; battleId = ""; zoneId = ""; appliedTurn = -1; appliedStatus = null
        field = Battlefield.of(""); focus = field.centre
    }

    /** The finger's direction on the ground, at most one unit long; the zero vector halts the hero. */
    fun steer(direction: Vec2) { desire = direction.clamp(1f) }

    /**
     * Folds a server snapshot into the world, animating what changed since the previous one.
     *
     * Returns false when the snapshot carries nothing new, which is the normal answer to a replayed
     * idempotent command: the server repeats the same battle and the world must not play it twice.
     */
    fun observe(battle: Battle, roster: List<Monster> = emptyList(), art: WorldArt = WorldArt(),
        action: BattleAction? = null): Boolean {
        val fresh = battle.id != battleId
        if(!fresh && battle.turn == appliedTurn && battle.status == appliedStatus) return false
        val delta = battleDelta(if(fresh) null else snapshot, battle, action)
        if(delta.silent) return false
        if(fresh) build(battle, roster, art)
        sync(battle)
        val script = worldScript(delta, HERO_ID, mobId(battle.monster), battle.monster.name,
            battle.monster.boss, battle.monster.element, art)
        // Turns queue instead of overlapping, so a burst of commands still reads in order.
        val offset = if(beats.isEmpty()) clock else beats.last().at
        script.forEach { beats += it.copy(at = offset + it.at) }
        snapshot = battle; battleId = battle.id; appliedTurn = battle.turn; appliedStatus = battle.status
        return true
    }

    /**
     * The command the hero wants to send next, or null while there is nothing to do.
     *
     * Taking one restarts the cadence, so a caller that cannot send right now simply asks again on the
     * next frame rather than queueing a stale swing.
     */
    fun takeAction(): BattleAction? {
        if(!auto || cooldown > 0L || playing) return null
        val battle = snapshot?.takeIf { it.status == BattleStatus.ACTIVE } ?: return null
        val action = AutoPilot.choose(battle, inStrikeRange, swings) ?: return null
        cooldown = CADENCE
        if(action == BattleAction.ATTACK || action == BattleAction.POWER) swings++
        return action
    }

    /** Advances the world by [dt] milliseconds: due beats fire, then everything walks and ages. */
    fun advance(dt: Long) {
        require(dt >= 0L) { tr("Время в мире идёт только вперёд", "World time only moves forward") }
        clock += dt
        while(beats.isNotEmpty() && beats.first().at <= clock) fire(beats.removeFirst())
        hero?.let { body ->
            body.step(dt, if(body.alive) desire else Vec2.ZERO, HERO_SPEED, field)
            if(!body.walking) engaged?.takeIf { it.alive && !it.gone }?.let { body.face(it.position) }
            collect(body)
            focus = focus.lerp(body.position, (dt / CAMERA_FOLLOW).coerceIn(0f, 1f))
        }
        pack.forEach { mob ->
            mob.step(dt, mobDesire(mob), if(mob.boss) BOSS_SPEED else MOB_SPEED, field)
            if(!mob.ambient && !mob.walking) hero?.let { mob.face(it.position) }
        }
        sweep(floaters, dt); sweep(debris, dt); sweep(flying, dt); sweep(fallen, dt)
        banner = banner?.takeIf { !it.spent(dt) }
        shake = (shake - dt / 360f).coerceAtLeast(0f)
        cooldown = (cooldown - dt).coerceAtLeast(0L)
    }

    private fun <T : WorldParticle> sweep(list: MutableList<T>, dt: Long) {
        if(list.isNotEmpty()) list.removeAll { it.spent(dt) }
    }

    /** Lays out the ground, the hero and the zone roster; the engaged mob walks in from the dark. */
    private fun build(battle: Battle, roster: List<Monster>, art: WorldArt) {
        seed = battle.id.hashCode()
        pack.clear(); floaters.clear(); debris.clear(); flying.clear(); beats.clear()
        // The ground of a zone survives between its encounters and not past them.
        if(battle.zoneId != zoneId || hero == null) {
            field = Battlefield.of(battle.zoneId)
            zoneId = battle.zoneId
            fallen.clear(); ground.clear(); picked.clear()
            hero = WorldActor(HERO_ID, WorldSide.HERO, battle.hero.name, art.hero, home = field.centre)
                .also { it.play(WorldPose.IDLE) }
            focus = field.centre
        }
        hero?.play(WorldPose.IDLE)
        pack += mobActor(battle.monster, art, ambient = false, index = 0)
        roster.filter { mobId(it) != mobId(battle.monster) }.take(AMBIENT_MOBS)
            .forEachIndexed { index, monster -> pack += mobActor(monster, art, ambient = true, index = index + 1) }
    }

    private fun mobActor(monster: Monster, art: WorldArt, ambient: Boolean, index: Int): WorldActor {
        val home = spawnPoint(index, if(ambient) CAMP_DISTANCE else ENTRY_DISTANCE)
        return WorldActor(mobId(monster), WorldSide.MOB, monster.name, art.forMonster(monster), monster.element,
            monster.boss, radius = if(monster.boss) .60f else .42f, ambient = ambient, home = home).also {
            it.life = monster.life; it.maxLife = monster.life
            it.seedBob((mobId(monster).hashCode() and 0xFF) / 40f)
            it.play(if(ambient) WorldPose.IDLE else WorldPose.SPAWN)
        }
    }

    /** A seeded ring around the hero, so a zone's pack always walks in from the same quarter. */
    private fun spawnPoint(index: Int, distance: Float): Vec2 {
        val base = hero?.position ?: field.centre
        val degrees = ((seed / (index + 1)) % 360 + 360) % 360 + index * 47
        val angle = degrees * PI.toFloat() / 180f
        return field.resolve(base + Vec2(cos(angle), sin(angle)) * distance, MOB_MARGIN)
    }

    /** Copies the server's bars onto the bodies. The only writer of life, max life and shield. */
    private fun sync(battle: Battle) {
        hero?.let { it.life = battle.hero.life; it.maxLife = battle.hero.maxLife; it.shield = battle.hero.shield }
        engaged?.let { it.life = battle.enemy.life; it.maxLife = battle.enemy.maxLife }
    }

    private fun mobDesire(mob: WorldActor): Vec2 {
        if(!mob.alive || mob.gone) return Vec2.ZERO
        val target = hero?.takeIf { it.alive } ?: return Vec2.ZERO
        if(mob.ambient) {
            val orbit = mob.anchor + Vec2(cos(mob.bob * .7f), sin(mob.bob * .5f)) * AMBIENT_ORBIT
            return if(mob.position.distanceTo(orbit) < .3f) Vec2.ZERO else (orbit - mob.position).direction
        }
        return if(mob.position.distanceTo(target.position) <= MOB_REACH) Vec2.ZERO
        else (target.position - mob.position).direction
    }

    /** Anything the hero walks over is claimed. It was already in the stash; this is the picture of it. */
    private fun collect(body: WorldActor) {
        if(ground.isEmpty() || !body.alive) return
        val reached = ground.filter { it.position.distanceTo(body.position) <= PICKUP_RANGE }
        if(reached.isEmpty()) return
        ground.removeAll(reached)
        reached.forEach {
            flying += LootMote(it, body)
            floaters += DamageNumber(it.title, it.position, WorldTint.LOOT)
            picked += it
        }
        while(picked.size > FEED) picked.removeAt(0)
    }

    private fun actorOf(id: String): WorldActor? = if(id == HERO_ID) hero else pack.firstOrNull { it.id == id }

    private fun fire(beat: WorldBeat) {
        val actor = actorOf(beat.actor)
        val target = actorOf(beat.target)
        val byHero = beat.actor == HERO_ID
        when(beat.kind) {
            WorldBeatKind.SPAWN -> actor?.play(WorldPose.SPAWN)
            WorldBeatKind.WINDUP -> actor?.let { it.play(WorldPose.WINDUP); target?.let { other -> it.face(other.position) } }
            WorldBeatKind.STRIKE -> actor?.play(WorldPose.STRIKE)
            WorldBeatKind.CAST -> actor?.play(WorldPose.CAST)
            WorldBeatKind.GUARD -> actor?.let { it.play(WorldPose.GUARD); caption(it, beat.text, WorldTint.GUARD) }
            WorldBeatKind.QUAFF -> actor?.let {
                it.play(WorldPose.QUAFF)
                if(beat.amount > 0.0) number(it, "+" + formatAmount(beat.amount), WorldTint.HEAL)
            }
            WorldBeatKind.RETREAT -> actor?.let { caption(it, beat.text, WorldTint.GUARD) }
            WorldBeatKind.IMPACT -> target?.let {
                val tint = if(byHero) WorldTint.HERO_DAMAGE else WorldTint.MOB_DAMAGE
                it.play(WorldPose.RECOIL); it.flash = 1f
                number(it, formatAmount(beat.amount), tint, beat.crit)
                burst(it, beat.element, if(beat.crit) 13 else 7, tint)
                shake = maxOf(shake, if(beat.crit) 1f else .55f)
            }
            WorldBeatKind.MISS -> target?.let { caption(it, tr("Мимо", "Miss"), WorldTint.MISS) }
            WorldBeatKind.ABSORB -> actor?.let { number(it, formatAmount(beat.amount), WorldTint.SHIELD) }
            WorldBeatKind.REGEN -> actor?.let { number(it, "+" + formatAmount(beat.amount), WorldTint.HEAL) }
            WorldBeatKind.MANA -> actor?.let {
                number(it, (if(beat.amount < 0.0) "−" else "+") + formatAmount(beat.amount), WorldTint.MANA)
            }
            WorldBeatKind.DEATH -> actor?.let {
                it.play(WorldPose.DEATH)
                fallen += Corpse(it.icon, it.position, it.boss)
                burst(it, it.element, 14, if(byHero) WorldTint.MOB_DAMAGE else WorldTint.HERO_DAMAGE)
                shake = 1f
            }
            WorldBeatKind.DROP -> beat.reward?.let { reward ->
                val from = actor?.position ?: field.centre
                ground += GroundLoot(reward, beat.loot, beat.rarity, beat.icon, scatter(from, ground.size))
            }
            WorldBeatKind.BANNER -> banner = Banner(beat.text, beat.crit)
        }
    }

    /**
     * Drops fan out around the corpse so a pile of four rewards is four things to walk over.
     *
     * Settled with the hero's own girth, which is what guarantees every pile is somewhere the hero can
     * actually stand rather than behind a stone it can never reach.
     */
    private fun scatter(from: Vec2, index: Int): Vec2 {
        val angle = index * 2.39996f
        return field.resolve(from + Vec2(cos(angle), sin(angle)) * DROP_SPREAD, DROP_MARGIN)
    }

    private fun number(actor: WorldActor, text: String, tint: WorldTint, crit: Boolean = false) {
        floaters += DamageNumber(text, actor.position, tint, crit)
    }

    private fun caption(actor: WorldActor, text: String, tint: WorldTint) {
        if(text.isNotBlank()) floaters += DamageNumber(text, actor.position, tint)
    }

    /** Deterministic fan of debris: the same hit bursts the same way if the frame is drawn twice. */
    private fun burst(actor: WorldActor, element: String, count: Int, tint: WorldTint) {
        val base = (element.hashCode() xor seed) and 0x7FFFFFFF
        repeat(count) { index ->
            val spread = (base / (index + 1) % 90) / 90f
            debris += Spark(actor.position, (index.toFloat() / count + spread * .12f) * 2f * PI.toFloat(),
                .9f + spread * 1.1f, tint)
        }
    }

    companion object {
        const val HERO_ID = "hero"
        /** Tiles per second. The hero outruns a mob, which is what makes running away a tactic. */
        const val HERO_SPEED = 3.6f
        const val MOB_SPEED = 2.5f
        const val BOSS_SPEED = 2.0f
        /** How close the hero must stand before the client will ask the server for a swing. */
        const val STRIKE_RANGE = 1.5f
        const val MOB_REACH = 1.15f
        const val PICKUP_RANGE = .95f
        const val ENTRY_DISTANCE = 7.5f
        const val CAMP_DISTANCE = 10.5f
        const val AMBIENT_ORBIT = 1.4f
        const val DROP_SPREAD = .85f
        const val DROP_MARGIN = .45f
        const val MOB_MARGIN = .8f
        const val AMBIENT_MOBS = 4
        const val FEED = 8
        /** Shortest gap between two commands; the animation of a turn usually outlasts it anyway. */
        const val CADENCE = 620L
        /** Milliseconds for the camera to close the distance to the hero. */
        const val CAMERA_FOLLOW = 190f
        fun mobId(monster: Monster) = monster.id.ifBlank { "mob" }
    }
}
