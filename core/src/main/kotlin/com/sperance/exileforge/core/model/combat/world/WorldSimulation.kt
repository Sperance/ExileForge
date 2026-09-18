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
 * The isometric expedition: a generated map, the pack that roams it, and everything on the floor.
 *
 * It is a renderer and a pair of legs, never a referee. Life, mana, shield, damage, rewards and the
 * outcome all arrive in [observe] as server snapshots; the only arithmetic here is kinematics — walking,
 * routing around a wall, ageing particles — plus the subtraction in [battleDelta] that reads what the
 * server changed. There is deliberately no hit roll, no armour, no resistance and no loot chance: those
 * live on the server, and a second implementation here would be a second set of numbers to disagree
 * with. Modifiers, passives and resistances are therefore already inside every number it shows.
 *
 * What a position decides is *when* the client asks. [takeIntent] offers the next command once the
 * exile is within reach of a monster, standing on the boss altar, or ready to swing at the mob the
 * server already put in the battle. The caller sends it as the same durable, idempotent command a tap
 * would send, and the answer comes back through [observe] like any other.
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
    private var spawnedFresh = false

    var map: Battlefield = Battlefield.of(""); private set
    var hero: WorldActor? = null; private set
    var banner: Banner? = null; private set
    /** The point the camera is centred on; it chases the exile instead of snapping to them. */
    var focus: Vec2 = map.spawn; private set
    /** Camera kick, 0..1, decaying; the renderer offsets the whole view by it. */
    var shake = 0f; private set
    var clock = 0L; private set
    /** Who is driving. The screen owns it; the world only obeys. */
    var mode: WorldMode = WorldMode.AUTO_STRIKE
    /** What the camp would allow right now, refreshed from the server's own counters every frame. */
    var rules: WorldRules = WorldRules()

    val mobs: List<WorldActor> get() = pack
    val numbers: List<DamageNumber> get() = floaters
    val sparks: List<Spark> get() = debris
    val motes: List<LootMote> get() = flying
    val corpses: List<Corpse> get() = fallen
    /** Rewards the server granted, still lying where they fell. */
    val loot: List<GroundLoot> get() = ground
    /** What the exile has walked over, oldest first; the HUD shows the tail of it. */
    val collected: List<GroundLoot> get() = picked
    /** The one mob the server is actually fighting; the rest of [mobs] is the zone's pack. */
    val engaged: WorldActor? get() = pack.firstOrNull { it.engaged && !it.gone }
    /** How many of the zone's monsters are still walking the map. */
    val roaming: Int get() = pack.count { it.alive && !it.gone }
    /** True while a turn is still playing out; nothing new is asked for until it settles. */
    val playing: Boolean get() = beats.isNotEmpty()
    /** True while the exile stands on the summoning circle and the zone has opened it. */
    val onAltar: Boolean get() = hero?.position?.distanceTo(map.altar).let { it != null && it <= ALTAR_RANGE }
    /** Whether the exile stands close enough to swing at the mob the server put in the battle. */
    val inStrikeRange: Boolean get() {
        val body = hero?.takeIf { it.alive } ?: return false
        val target = engaged?.takeIf { it.alive && !it.gone } ?: return false
        return body.position.distanceTo(target.position) <= STRIKE_RANGE
    }

    /** Forgets the world completely: another character, another server, or a logout. */
    fun reset() {
        hero = null; banner = null; shake = 0f; clock = 0L; seed = 0; desire = Vec2.ZERO
        cooldown = 0L; swings = 0; spawnedFresh = false; rules = WorldRules()
        pack.clear(); beats.clear(); floaters.clear(); debris.clear(); flying.clear(); fallen.clear()
        ground.clear(); picked.clear()
        snapshot = null; battleId = ""; zoneId = ""; appliedTurn = -1; appliedStatus = null
        map = Battlefield.of(""); focus = map.spawn
    }

    /** The finger's direction on the ground, at most one unit long; the zero vector halts the exile. */
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
     * The command the exile wants to send next, or null while there is nothing to ask for.
     *
     * Taking one restarts the cadence, so a caller that cannot send right now simply asks again on the
     * next frame rather than queueing a stale swing. [WorldMode.MANUAL] never asks for anything.
     */
    fun takeIntent(): WorldIntent? {
        if(mode == WorldMode.MANUAL || cooldown > 0L || playing) return null
        val body = hero?.takeIf { it.alive } ?: return null
        val open = snapshot?.takeIf { it.status == BattleStatus.ACTIVE }
        if(open != null) {
            val action = AutoPilot.choose(open, inStrikeRange, swings) ?: return null
            cooldown = CADENCE
            if(action == BattleAction.ATTACK || action == BattleAction.POWER) swings++
            return WorldIntent.Strike(action)
        }
        // Between battles the map itself is the input: a summoning circle underfoot, or a monster in reach.
        if(rules.boss && body.position.distanceTo(map.altar) <= ALTAR_RANGE) {
            cooldown = ENGAGE_PAUSE
            return WorldIntent.Engage(boss = true)
        }
        if(rules.engage && touching(body) != null) {
            cooldown = ENGAGE_PAUSE
            return WorldIntent.Engage(boss = false)
        }
        return null
    }

    /** Advances the world by [dt] milliseconds: due beats fire, then everything walks and ages. */
    fun advance(dt: Long) {
        require(dt >= 0L) { tr("Время в мире идёт только вперёд", "World time only moves forward") }
        clock += dt
        while(beats.isNotEmpty() && beats.first().at <= clock) fire(beats.removeFirst())
        hero?.let { body ->
            body.step(dt, heroDesire(body, dt), HERO_SPEED, map)
            if(!body.walking) engaged?.takeIf { it.alive && !it.gone }?.let { body.face(it.position) }
            collect(body)
            focus = focus.lerp(body.position, (dt / CAMERA_FOLLOW).coerceIn(0f, 1f))
        }
        pack.forEach { mob ->
            mob.step(dt, mobDesire(mob, dt), if(mob.boss) BOSS_SPEED else MOB_SPEED, map)
            if(!mob.walking) hero?.let { mob.face(it.position) }
        }
        // A body that has finished dying is a corpse now; its drops read the corpse, not the sprite.
        if(pack.any { it.gone }) pack.removeAll { it.gone }
        sweep(floaters, dt); sweep(debris, dt); sweep(flying, dt); sweep(fallen, dt)
        banner = banner?.takeIf { !it.spent(dt) }
        shake = (shake - dt / 360f).coerceAtLeast(0f)
        cooldown = (cooldown - dt).coerceAtLeast(0L)
    }

    private fun <T : WorldParticle> sweep(list: MutableList<T>, dt: Long) {
        if(list.isNotEmpty()) list.removeAll { it.spent(dt) }
    }

    /** Lays out the map and the exile, tops the pack back up, and binds the mob the server chose. */
    private fun build(battle: Battle, roster: List<Monster>, art: WorldArt) {
        seed = battle.id.hashCode()
        floaters.clear(); debris.clear(); flying.clear(); beats.clear()
        // The ground of a zone survives between its encounters and not past them.
        if(battle.zoneId != zoneId || hero == null) {
            map = Battlefield.of(battle.zoneId)
            zoneId = battle.zoneId
            pack.clear(); fallen.clear(); ground.clear(); picked.clear()
            hero = WorldActor(HERO_ID, WorldSide.HERO, battle.hero.name, art.hero,
                radius = WorldActor.HERO_RADIUS, home = map.spawn).also { it.play(WorldPose.IDLE) }
            focus = map.spawn
        }
        hero?.play(WorldPose.IDLE)
        populate(roster, art)
        bind(battle.monster, art)
    }

    /** Keeps the zone inhabited: the map always carries a pack, not one monster at a time. */
    private fun populate(roster: List<Monster>, art: WorldArt) {
        if(roster.isEmpty()) return
        val away = hero?.position ?: map.spawn
        var index = pack.size
        var guard = 0
        while(roaming < PACK_SIZE && guard++ < PACK_SIZE * 4) {
            pack += mobActor(roster[index % roster.size], art, camp(index, away))
            index++
        }
    }

    /**
     * Binds the monster the server chose to a body on the map.
     *
     * The nearest roamer of that species becomes the enemy, which is what makes the thing the exile
     * walked into the thing it ends up fighting; only when the zone has none does one walk in.
     */
    private fun bind(monster: Monster, art: WorldArt) {
        pack.forEach { it.engaged = false }
        val id = mobId(monster)
        val here = hero?.position ?: map.spawn
        val known = pack.filter { it.id == id && it.alive && !it.gone }.minByOrNull { it.position.distanceTo(here) }
        spawnedFresh = known == null
        val chosen = known ?: mobActor(monster, art, camp(pack.size, here)).also { pack += it }
        chosen.engaged = true
        chosen.maxLife = monster.life
        if(spawnedFresh) chosen.life = monster.life
    }

    private fun mobActor(monster: Monster, art: WorldArt, at: Vec2): WorldActor {
        val id = mobId(monster)
        return WorldActor(id, WorldSide.MOB, monster.name, art.forMonster(monster), monster.element,
            monster.boss, if(monster.boss) WorldActor.BOSS_RADIUS else WorldActor.MOB_RADIUS, at).also {
            it.life = monster.life; it.maxLife = monster.life
            it.seedBob((id.hashCode() and 0xFF) / 40f)
            it.play(WorldPose.IDLE)
        }
    }

    /** A camp of the generated map, kept clear of the exile so nothing materialises in their face. */
    private fun camp(index: Int, away: Vec2): Vec2 {
        val far = map.camps.filter { it.distanceTo(away) > SPAWN_CLEAR }
        val pool = far.ifEmpty { map.camps }
        if(pool.isEmpty()) return map.settle(map.spawn, WorldActor.MOB_RADIUS)
        val base = pool[(((index * 3 + (seed and 0xFF)) % pool.size) + pool.size) % pool.size]
        val drift = Vec2((index % 5 - 2) * .7f, (index / 5 % 5 - 2) * .7f)
        return map.settle(base + drift, WorldActor.MOB_RADIUS)
    }

    /** Copies the server's bars onto the bodies. The only writer of life, max life and shield. */
    private fun sync(battle: Battle) {
        hero?.let { it.life = battle.hero.life; it.maxLife = battle.hero.maxLife; it.shield = battle.hero.shield }
        engaged?.let { it.life = battle.enemy.life; it.maxLife = battle.enemy.maxLife }
    }

    /** The nearest monster the exile is standing next to, or null when nothing is in arm's reach. */
    private fun touching(body: WorldActor): WorldActor? = pack
        .filter { it.alive && !it.gone && it.position.distanceTo(body.position) <= TOUCH_RANGE }
        .minByOrNull { it.position.distanceTo(body.position) }

    /** The thumb always wins; [WorldMode.AUTO_RUN] only steers when it is not on the glass. */
    private fun heroDesire(body: WorldActor, dt: Long): Vec2 {
        if(desire != Vec2.ZERO) { body.route.forget(); return desire }
        if(mode != WorldMode.AUTO_RUN || !body.alive) return Vec2.ZERO
        val errand = errand(body) ?: return Vec2.ZERO
        if(body.position.distanceTo(errand.at) <= errand.within) return Vec2.ZERO
        return body.route.direction(body.position, errand.at, body.radius, map, dt)
    }

    /**
     * Where the exile takes itself next: the mob it is fighting, the reward on the floor, the altar
     * once the zone opens it, or the nearest thing still walking.
     */
    private fun errand(body: WorldActor): Errand? {
        val open = snapshot?.takeIf { it.status == BattleStatus.ACTIVE }
        if(open != null) return engaged?.takeIf { it.alive && !it.gone }
            ?.let { Errand(it.position, STRIKE_RANGE * .75f) }
        ground.minByOrNull { it.position.distanceTo(body.position) }
            ?.let { return Errand(it.position, PICKUP_RANGE * .5f) }
        if(rules.boss) return Errand(map.altar, ALTAR_RANGE * .6f)
        if(!rules.engage) return null
        return pack.filter { it.alive && !it.gone }.minByOrNull { it.position.distanceTo(body.position) }
            ?.let { Errand(it.position, TOUCH_RANGE * .7f) }
    }

    private class Errand(val at: Vec2, val within: Float)

    /** The engaged mob closes to its reach; the rest of the pack aggroes, then patrols its camp. */
    private fun mobDesire(mob: WorldActor, dt: Long): Vec2 {
        if(!mob.alive || mob.gone) return Vec2.ZERO
        val target = hero?.takeIf { it.alive } ?: return Vec2.ZERO
        val gap = mob.position.distanceTo(target.position)
        if(mob.engaged) return if(gap <= MOB_REACH) Vec2.ZERO
        else mob.route.direction(mob.position, target.position, mob.radius, map, dt)
        if(gap <= AGGRO_RANGE) return if(gap <= TOUCH_RANGE * .8f) Vec2.ZERO
        else mob.route.direction(mob.position, target.position, mob.radius, map, dt)
        val orbit = map.settle(mob.anchor + Vec2(cos(mob.bob * .7f), sin(mob.bob * .5f)) * PATROL_ORBIT, mob.radius)
        return if(mob.position.distanceTo(orbit) < .4f) Vec2.ZERO else (orbit - mob.position).direction
    }

    /** Anything the exile walks over is claimed. It was already in the stash; this is the picture of it. */
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

    /** Beats always land on the mob the server is fighting, even when the zone holds its twin. */
    private fun actorOf(id: String): WorldActor? = if(id == HERO_ID) hero
    else pack.firstOrNull { it.id == id && it.engaged } ?: pack.firstOrNull { it.id == id }

    private fun fire(beat: WorldBeat) {
        val actor = actorOf(beat.actor)
        val target = actorOf(beat.target)
        val byHero = beat.actor == HERO_ID
        when(beat.kind) {
            // Only a body that walked in for this battle fades in; one already on the map just turns.
            WorldBeatKind.SPAWN -> if(spawnedFresh) actor?.play(WorldPose.SPAWN)
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
            // The body may already have finished dying, so the drop falls on the corpse it left.
            WorldBeatKind.DROP -> beat.reward?.let { reward ->
                val from = actorOf(beat.actor)?.position ?: fallen.lastOrNull()?.position ?: map.spawn
                ground += GroundLoot(reward, beat.loot, beat.rarity, beat.icon, scatter(from, ground.size))
            }
            WorldBeatKind.BANNER -> banner = Banner(beat.text, beat.crit)
        }
    }

    /**
     * Drops fan out around the corpse so a pile of four rewards is four things to walk over.
     *
     * Settled with the exile's own girth, which is what guarantees every pile is somewhere it can
     * actually stand rather than inside the wall the fight happened against.
     */
    private fun scatter(from: Vec2, index: Int): Vec2 {
        val angle = index * 2.39996f
        return map.settle(from + Vec2(cos(angle), sin(angle)) * DROP_SPREAD, WorldActor.HERO_RADIUS)
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
        /** Tiles per second. The exile outruns a mob, which is what makes running away a tactic. */
        const val HERO_SPEED = 3.8f
        const val MOB_SPEED = 2.6f
        const val BOSS_SPEED = 2.1f
        /** How close the exile must stand before the client will ask the server for a swing. */
        const val STRIKE_RANGE = 1.6f
        const val MOB_REACH = 1.2f
        /** Arm's reach: walking into a roaming monster is what opens a battle with the zone. */
        const val TOUCH_RANGE = 1.3f
        /** How far a roamer notices the exile and starts closing in. */
        const val AGGRO_RANGE = 5.5f
        const val ALTAR_RANGE = 1.3f
        const val PICKUP_RANGE = 1.0f
        const val PATROL_ORBIT = 1.6f
        const val DROP_SPREAD = .85f
        /** Monsters walking the map at once. The zone is a place, not a queue of one. */
        const val PACK_SIZE = 6
        const val SPAWN_CLEAR = 7f
        const val FEED = 8
        /** Shortest gap between two commands; the animation of a turn usually outlasts it anyway. */
        const val CADENCE = 620L
        /** A longer pause after opening a battle, so one touch is one fight. */
        const val ENGAGE_PAUSE = 1_100L
        /** Milliseconds for the camera to close the distance to the exile. */
        const val CAMERA_FOLLOW = 190f
        fun mobId(monster: Monster) = monster.id.ifBlank { "mob" }
    }
}
