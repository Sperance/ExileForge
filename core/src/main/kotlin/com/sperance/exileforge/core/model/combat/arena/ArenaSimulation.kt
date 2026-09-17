package com.sperance.exileforge.core.model.combat.arena

import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.combat.Battle
import com.sperance.exileforge.core.model.combat.BattleAction
import com.sperance.exileforge.core.model.combat.BattleReward
import com.sperance.exileforge.core.model.combat.BattleStatus
import com.sperance.exileforge.core.model.combat.Monster
import kotlin.math.PI

/** What the arena needs from the server's icon set, without dragging the set itself into the engine. */
class ArenaArt(
    val forMonster: (Monster) -> String = { "" },
    val forReward: (BattleReward) -> String = { "" },
    val hero: String = ""
)

/**
 * The 2D stage: where the fighters stand, what they are playing, and the debris of the last hit.
 *
 * It is a renderer, not a referee. Life, mana, shield, damage, rewards and the outcome all arrive in
 * [observe] as server snapshots; the only arithmetic here is kinematics — walking towards a spot and
 * ageing particles — plus the subtraction in [battleDelta] that reads what the server changed. There
 * is deliberately no hit roll, no armour, no resistance and no loot chance: those live on the server,
 * and a second implementation here would be a second set of numbers to disagree with. Modifiers,
 * passives and resistances are therefore already inside every number the stage shows.
 *
 * Mutable on purpose: it is stepped once per displayed frame, and rebuilding an immutable graph sixty
 * times a second to move a few dozen sprites would cost more than the drawing does.
 */
class ArenaSimulation {
    private val pack = mutableListOf<ArenaActor>()
    private val beats = ArrayDeque<ArenaBeat>()
    private val floaters = mutableListOf<ArenaNumber>()
    private val debris = mutableListOf<ArenaSpark>()
    private val motes = mutableListOf<ArenaLootMote>()
    private val fallen = mutableListOf<ArenaCorpse>()
    private var snapshot: Battle? = null
    private var battleId = ""
    private var zoneId = ""
    private var appliedTurn = -1
    private var appliedStatus: BattleStatus? = null
    private var seed = 0

    var hero: ArenaActor? = null; private set
    var banner: ArenaBanner? = null; private set
    /** Camera kick, 0..1, decaying; the renderer offsets the whole stage by it. */
    var shake = 0f; private set
    var clock = 0L; private set

    val mobs: List<ArenaActor> get() = pack
    val numbers: List<ArenaNumber> get() = floaters
    val sparks: List<ArenaSpark> get() = debris
    val loot: List<ArenaLootMote> get() = motes
    val corpses: List<ArenaCorpse> get() = fallen
    /** The one mob the server is actually fighting; the rest of [mobs] is the zone waiting its turn. */
    val engaged: ArenaActor? get() = pack.firstOrNull { !it.queued }
    /** True while a turn is still playing out; the HUD greys the action rail until it settles. */
    val playing: Boolean get() = beats.isNotEmpty()

    /** Forgets the stage completely: another character, another server, or a logout. */
    fun reset() {
        hero = null; banner = null; shake = 0f; clock = 0L; seed = 0
        pack.clear(); beats.clear(); floaters.clear(); debris.clear(); motes.clear(); fallen.clear()
        snapshot = null; battleId = ""; zoneId = ""; appliedTurn = -1; appliedStatus = null
    }

    /**
     * Folds a server snapshot into the stage, animating what changed since the previous one.
     *
     * Returns false when the snapshot carries nothing new, which is the normal answer to a replayed
     * idempotent command: the server repeats the same battle and the stage must not play it twice.
     */
    fun observe(battle: Battle, roster: List<Monster> = emptyList(), art: ArenaArt = ArenaArt(),
        action: BattleAction? = null): Boolean {
        val fresh = battle.id != battleId
        if(!fresh && battle.turn == appliedTurn && battle.status == appliedStatus) return false
        val delta = battleDelta(if(fresh) null else snapshot, battle, action)
        if(delta.silent) return false
        if(fresh) build(battle, roster, art)
        sync(battle)
        val script = arenaScript(delta, HERO_ID, mobId(battle.monster), battle.monster.name,
            battle.monster.boss, battle.monster.element, art.forReward)
        // Turns queue instead of overlapping, so a fast tapper still sees each swing land in order.
        val offset = if(beats.isEmpty()) clock else beats.last().at
        script.forEach { beats += it.copy(at = offset + it.at) }
        snapshot = battle; battleId = battle.id; appliedTurn = battle.turn; appliedStatus = battle.status
        return true
    }

    /** Advances the stage by [dt] milliseconds: due beats fire, then everything ages. */
    fun advance(dt: Long) {
        require(dt >= 0L) { tr("Время арены идёт только вперёд", "Arena time only moves forward") }
        clock += dt
        while(beats.isNotEmpty() && beats.first().at <= clock) fire(beats.removeFirst())
        hero?.step(dt, ArenaStageLayout.WALK_SPEED)
        pack.forEach { it.step(dt, ArenaStageLayout.WALK_SPEED) }
        sweep(floaters, dt); sweep(debris, dt); sweep(motes, dt); sweep(fallen, dt)
        banner = banner?.takeIf { !it.spent(dt) }
        shake = (shake - dt / 360f).coerceAtLeast(0f)
    }

    private fun <T : ArenaParticle> sweep(list: MutableList<T>, dt: Long) {
        if(list.isNotEmpty()) list.removeAll { it.spent(dt) }
    }

    /** Lays out the hero and the zone roster; the engaged mob is the one the server put in the battle. */
    private fun build(battle: Battle, roster: List<Monster>, art: ArenaArt) {
        seed = battle.id.hashCode()
        pack.clear(); motes.clear(); floaters.clear(); debris.clear(); beats.clear()
        // Corpses are scenery of the zone run, so they survive between encounters and not past them.
        if(battle.zoneId != zoneId) fallen.clear()
        zoneId = battle.zoneId
        hero = ArenaActor(HERO_ID, ArenaSide.HERO, battle.hero.name, art.hero, home = ArenaStageLayout.heroHome)
            .also { it.play(ArenaPose.IDLE) }
        pack += actor(battle.monster, art, queued = false, index = 0)
        roster.filter { mobId(it) != mobId(battle.monster) }.take(ArenaStageLayout.queue.size)
            .forEachIndexed { index, monster -> pack += actor(monster, art, queued = true, index = index) }
    }

    private fun actor(monster: Monster, art: ArenaArt, queued: Boolean, index: Int): ArenaActor {
        val home = if(queued) ArenaStageLayout.queueSlot(index) else ArenaStageLayout.mobMelee
        return ArenaActor(mobId(monster), ArenaSide.MOB, monster.name, art.forMonster(monster),
            monster.element, monster.boss, home, queued).also {
            it.position = if(queued) home else ArenaStageLayout.mobEntry
            it.goal = home
            it.life = monster.life; it.maxLife = monster.life
            it.seedBob((mobId(monster).hashCode() and 0xFF) / 40f)
            it.play(if(queued) ArenaPose.IDLE else ArenaPose.SPAWN)
        }
    }

    /** Copies the server's bars onto the actors. The only writer of life, max life and shield. */
    private fun sync(battle: Battle) {
        hero?.let { it.life = battle.hero.life; it.maxLife = battle.hero.maxLife; it.shield = battle.hero.shield }
        engaged?.let { it.life = battle.enemy.life; it.maxLife = battle.enemy.maxLife }
    }

    private fun actorOf(id: String): ArenaActor? = if(id == HERO_ID) hero else pack.firstOrNull { it.id == id }

    private fun fire(beat: ArenaBeat) {
        val actor = actorOf(beat.actor)
        val target = actorOf(beat.target)
        val byHero = beat.actor == HERO_ID
        when(beat.kind) {
            ArenaBeatKind.SPAWN -> actor?.play(ArenaPose.SPAWN)
            ArenaBeatKind.ADVANCE -> actor?.let { it.goal = ArenaStageLayout.heroMelee; it.play(ArenaPose.ADVANCE) }
            ArenaBeatKind.WITHDRAW -> actor?.let { it.goal = it.home }
            ArenaBeatKind.WINDUP -> actor?.play(ArenaPose.WINDUP)
            ArenaBeatKind.STRIKE -> actor?.play(ArenaPose.STRIKE)
            ArenaBeatKind.CAST -> actor?.play(ArenaPose.CAST)
            ArenaBeatKind.GUARD -> actor?.let {
                it.goal = it.home; it.play(ArenaPose.GUARD); caption(it, beat.text, ArenaTint.GUARD)
            }
            ArenaBeatKind.QUAFF -> actor?.let {
                it.play(ArenaPose.QUAFF)
                if(beat.amount > 0.0) number(it, "+" + formatAmount(beat.amount), ArenaTint.HEAL)
            }
            ArenaBeatKind.RETREAT -> actor?.let {
                it.goal = ArenaStageLayout.heroRetreat; it.play(ArenaPose.ADVANCE); caption(it, beat.text, ArenaTint.GUARD)
            }
            ArenaBeatKind.IMPACT -> target?.let {
                val tint = if(byHero) ArenaTint.HERO_DAMAGE else ArenaTint.MOB_DAMAGE
                it.play(ArenaPose.RECOIL); it.flash = 1f
                number(it, formatAmount(beat.amount), tint, beat.crit)
                burst(it, beat.element, if(beat.crit) 13 else 7, tint)
                shake = maxOf(shake, if(beat.crit) 1f else .55f)
            }
            ArenaBeatKind.MISS -> target?.let { caption(it, tr("Мимо", "Miss"), ArenaTint.MISS) }
            ArenaBeatKind.ABSORB -> actor?.let { number(it, formatAmount(beat.amount), ArenaTint.SHIELD) }
            ArenaBeatKind.REGEN -> actor?.let { number(it, "+" + formatAmount(beat.amount), ArenaTint.HEAL) }
            ArenaBeatKind.MANA -> actor?.let {
                number(it, (if(beat.amount < 0.0) "−" else "+") + formatAmount(beat.amount), ArenaTint.MANA)
            }
            ArenaBeatKind.DEATH -> actor?.let {
                it.play(ArenaPose.DEATH)
                fallen += ArenaCorpse(it.icon, ArenaStageLayout.corpseSpot(it.position), it.boss)
                burst(it, it.element, 14, if(byHero) ArenaTint.MOB_DAMAGE else ArenaTint.HERO_DAMAGE)
                shake = 1f
            }
            ArenaBeatKind.LOOT -> motes += ArenaLootMote(beat.text, beat.icon, beat.loot,
                actor?.position ?: ArenaStageLayout.mobMelee,
                (hero?.position ?: ArenaStageLayout.heroHome) + Vec2(0f, -.18f))
            ArenaBeatKind.BANNER -> banner = ArenaBanner(beat.text, beat.crit)
        }
    }

    private fun number(actor: ArenaActor, text: String, tint: ArenaTint, crit: Boolean = false) {
        floaters += ArenaNumber(text, actor.position + Vec2(0f, -.10f), tint, crit)
    }
    private fun caption(actor: ArenaActor, text: String, tint: ArenaTint) {
        if(text.isNotBlank()) floaters += ArenaNumber(text, actor.position + Vec2(0f, -.12f), tint)
    }
    /** Deterministic fan of debris: the same hit bursts the same way if the frame is drawn twice. */
    private fun burst(actor: ArenaActor, element: String, count: Int, tint: ArenaTint) {
        val base = (element.hashCode() xor seed) and 0x7FFFFFFF
        repeat(count) { index ->
            val spread = (base / (index + 1) % 90) / 90f
            debris += ArenaSpark(actor.position, (index.toFloat() / count + spread * .12f) * 2f * PI.toFloat(),
                .16f + spread * .18f, tint)
        }
    }

    private companion object {
        const val HERO_ID = "hero"
        fun mobId(monster: Monster) = monster.id.ifBlank { "mob" }
    }
}
