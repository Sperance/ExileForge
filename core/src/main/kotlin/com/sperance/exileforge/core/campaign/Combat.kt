package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.AilmentRule
import com.sperance.exileforge.core.model.campaign.CombatRules
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Who acted. */
enum class Side { HERO, MONSTER; val other: Side get() = if (this == HERO) MONSTER else HERO }

/** What a fighter did: swung a weapon, let an ailment burn on, drank, or turned to leave. Spells left the game in 2.48.0. */
enum class Action { ATTACK, TICK, RETREAT }

/** How a blow ended: it landed, landed hard, or never reached. */
enum class HitKind { HIT, CRIT, EVADED, BLOCKED }

/** How the whole fight ended; a retreat is a fight nobody won inside the time limit, or one the hero walked out of. */
enum class Outcome { WIN, LOSS, RETREAT }

/** Damage by type, as the sheet names it. */
enum class DamageType(val attack: String, val resist: String?) {
    PHYSICAL("STOCK_ATTACK_PHYSICAL", null),
    FIRE("STOCK_ATTACK_FIRE", "STOCK_RESIST_FIRE"),
    COLD("STOCK_ATTACK_COLD", "STOCK_RESIST_COLD"),
    LIGHTNING("STOCK_ATTACK_LIGHTNING", "STOCK_RESIST_LIGHTNING"),
    CHAOS("STOCK_ATTACK_CHAOS", "STOCK_RESIST_CHAOS");

    /** The stat that lifts this resistance's ceiling (since server 0.36.0). */
    val maxResist: String? get() = resist?.replace("STOCK_RESIST_", "STOCK_RESIST_MAX_")

    companion object { fun of(stat: String) = entries.firstOrNull { it.attack == stat } }
}

/**
 * The six ailments the server rules (its `EnumStatBool` without the prefix). Which damage brings which is the rule's, not ours.
 * [word] is how the sheet's stats name it since server 0.36.0 — `STOCK_IGNITE_CHANCE`, `STOCK_AVOID_IGNITE`,
 * `STOCK_IGNITE_DURATION_ON_SELF` — and [damage] the stat that makes its damage over time heavier.
 */
enum class Ailment(val word: String, val damage: String? = null) {
    BURNING("IGNITE", "STOCK_BURNING_DAMAGE"), CHILLED("CHILL"), FROZEN("FREEZE"), SHOCKED("SHOCK"),
    POISONED("POISON", "STOCK_POISON_DAMAGE"), BLEEDING("BLEED", "STOCK_BLEED_DAMAGE");
    /** Deals damage over time, as opposed to slowing, weakening or stopping. */
    val hurts: Boolean get() = this == BURNING || this == POISONED || this == BLEEDING
    companion object { fun of(name: String) = entries.firstOrNull { it.name == name } }
}

/**
 * One side of a fight, read off a stat table — the hero's sheet or a rolled monster — under the
 * server's [rules].
 *
 * Every number here is one the server sent; the fight only decides what they do to each other. A
 * hero with no weapon still swings — unarmed, as in PoE — and a missing critical chance is the one
 * every attack has. Spells and mana left the game in 2.48.0 (server 0.43.0): nobody casts, and a
 * sheet's spell or mana lines count for nothing here.
 */
data class Combatant(val stats: Map<String, Double>, val level: Int, val rules: CombatRules = CombatRules()) {
    private fun stat(name: String) = stats[name] ?: 0.0
    private fun percent(name: String, cap: Double = 100.0) = stat(name).coerceIn(0.0, cap) / 100

    val maxLife = max(1.0, stat("STOCK_HEALTH"))
    val maxShield = max(0.0, stat("STOCK_ENERGY_SHIELD"))
    val damage: Map<DamageType, Double> = DamageType.entries.associateWith { max(0.0, stat(it.attack)) }
        .let { rolled -> if (rolled.values.sum() > 0) rolled else rolled + (DamageType.PHYSICAL to rules.unarmed.damage) }
    val attackSpeed = stat("STOCK_ATTACK_SPEED").takeIf { it > 0 }?.coerceIn(0.3, 5.0) ?: rules.unarmed.speed
    val critChance = (stats["STOCK_CRITICAL_CHANCE"] ?: rules.critical.chance).coerceIn(0.0, 100.0) / 100
    val critMultiplier = max(100.0, (stats["STOCK_CRITICAL_MULTIPLIER"] ?: rules.critical.multiplier) + stat("STOCK_CRITICAL_DAMAGE")) / 100
    val armour = max(0.0, stat("STOCK_ARMOR"))
    val evasion = max(0.0, stat("STOCK_EVASION"))
    val block = stat("STOCK_BLOCK_CHANCE").coerceIn(0.0, rules.blockCap) / 100
    /** Taken off physical damage after armour, under armour's own cap. */
    val physicalReduction = percent("STOCK_PHYSICAL_REDUCTION", rules.armour.cap)
    /** Chaos stands alone, as in PoE; "all resistances" and "all maximum resistances" cover the three elements. */
    fun resist(type: DamageType): Double {
        val name = type.resist ?: return 0.0
        val chaos = name == "STOCK_RESIST_CHAOS"
        val ceiling = (rules.resistCap + stat(type.maxResist.orEmpty()) + (if (chaos) 0.0 else stat("STOCK_RESIST_MAX_ALL"))).coerceIn(0.0, rules.resistHardCap)
        return (stat(name) + (if (chaos) 0.0 else stat("STOCK_RESIST_ALL"))).coerceIn(0.0, ceiling) / 100
    }
    val lifeRegen = max(0.0, stat("STOCK_HEALTH_REGEN"))
    val shieldRegen = max(0.0, stat("STOCK_ENERGY_REGEN"))
    val leechPhysical = max(0.0, stat("STOCK_LEECH_PHYSICAL")) / 100
    val leechAll = max(0.0, stat("STOCK_LEECH_ALL")) / 100
    val critLeech = max(0.0, stat("STOCK_CRITICAL_VAMPIRE")) / 100
    val stunThreshold = max(0.0, stat("STOCK_STUN_THRESHOLD"))
    val avoidStun = percent("STOCK_AVOID_STUN")
    /** What gear adds to the rule's chance to inflict [ailment], in percent; chill has no such stat. */
    fun inflictChance(ailment: Ailment) = max(0.0, stat("STOCK_${ailment.word}_CHANCE"))
    /** How much heavier [ailment]'s damage over time runs. */
    fun ailmentDamage(ailment: Ailment) = 1 + max(0.0, ailment.damage?.let(::stat) ?: 0.0) / 100
    fun avoid(ailment: Ailment) = percent("STOCK_AVOID_${ailment.word}")
    /** What is left of [ailment]'s duration on this fighter, never less than the rule's cap allows. */
    fun ailmentDuration(ailment: Ailment) = 1 - percent("STOCK_${ailment.word}_DURATION_ON_SELF", rules.ailmentDurationCap)
    val lifeOnHit = max(0.0, stat("STOCK_HEALTH_ON_HIT"))
    val lifeOnKill = max(0.0, stat("STOCK_HEALTH_ON_KILL"))
    /** Taunts (since server 0.62.0): while it stands, its foes must strike it first, past any row. */
    val taunt: Boolean get() = stat("STOCK_TAUNT") > 0
    /** How hard it presses, before anyone's defences: its damage per swing times its swings per second. */
    val threat: Double get() = damage.values.sum() * attackSpeed
    /** The damage type most of its blow is made of. */
    val leading: DamageType get() = damage.maxBy { it.value }.key
}

/**
 * An ailment on a fighter: what, until when, how hard, and who put it there — the side and, for a
 * monster's, which foe of the pack ([foe], since 2.70.0).
 */
data class ActiveAilment(val ailment: Ailment, val until: Double, val magnitude: Double, val duration: Double, val source: Side, val foe: Int = 0)

/**
 * One foe of the pack as the fight takes it (2.70.0): its sheet and its row — a [ranged] foe
 * stands in the back and strikes from the first second; a melee one stands in front of it.
 */
data class Foe(val body: Combatant, val ranged: Boolean = false)

/**
 * Whom the hero strikes when the player has not said (2.70.0), by class — the owner's table: a
 * Marauder, a Duelist and a Ranger go for the one that presses hardest, a Shadow finishes the
 * weakest, a Witch the one least resistant to her leading element, a Templar answers whoever
 * struck last, a Scion finishes the weakest while healthy and answers when hurt.
 */
enum class TargetRule {
    THREAT, WEAKEST, EXPOSED, AVENGE, ADAPTIVE;

    companion object {
        fun of(classCode: String?): TargetRule = when (classCode) {
            "SHADOW" -> WEAKEST
            "WITCH" -> EXPOSED
            "TEMPLAR" -> AVENGE
            "SCION" -> ADAPTIVE
            else -> THREAT
        }
    }
}

/**
 * The hero's side of a fight beyond the sheet (2.70.0): whom they pick by [rule], and whether
 * their weapon reaches the back row while the front still stands — a bow or a wand does, a blade
 * or bare hands do not.
 */
data class HeroStance(val rule: TargetRule = TargetRule.THREAT, val ranged: Boolean = false) {
    companion object {
        private val reaching = setOf("BOW", "WAND")
        fun of(classCode: String?, weaponType: String?) = HeroStance(TargetRule.of(classCode), weaponType in reaching)
    }
}

/**
 * One thing that happened, and where both sides stood after it.
 *
 * [actor] is who did it; what it did was done to the other side. A [Action.TICK] is an ailment's damage gathered over the last second, so the
 * log is not a flood; [type] is the damage that led — the biggest share of a hit, or the ailment's. [foe] (2.70.0) is the
 * foe of the pack the event was about — the one that struck, or was struck — and [monsterLife], [monsterShield] are its.
 */
data class CombatEvent(
    val time: Double,
    val actor: Side,
    val action: Action,
    val kind: HitKind,
    val damage: Double,
    val type: DamageType?,
    val healed: Double,
    val stunned: Boolean,
    /** What this blow inflicted on its target. */
    val inflicted: List<Ailment>,
    /** For a tick: which ailment burned. */
    val ailment: Ailment?,
    val heroLife: Double,
    val heroShield: Double,
    val monsterLife: Double,
    val monsterShield: Double,
    val foe: Int = 0,
) {
    /** Who the number floats off. */
    val target: Side get() = actor.other
    val landed: Boolean get() = kind == HitKind.HIT || kind == HitKind.CRIT
}

/** A whole fight, as a test or a replay reads it. */
data class CombatLog(val events: List<CombatEvent>, val outcome: Outcome, val heroLife: Double, val duration: Double)

/**
 * The fight, alive: stepped in fixed slices of time so that the same seed is the same fight on any
 * screen, and open to the player while it runs — a retreat can be begun, a foe can be singled out.
 *
 * Since 2.70.0 it is the hero against the whole pack at once, in two rows: every foe swings at the
 * hero at its own speed from the first second, and the hero at one foe of the rows its weapon
 * reaches — the back row only with a bow or a wand, or once the front has fallen. Whom is the
 * player's [focus] when given, the class's [TargetRule] otherwise, chosen afresh at every swing.
 *
 * Each swing can be evaded (evasion against the attacker's level), blocked, or land; a landing hit
 * rolls the rule's variance per damage type, may be a critical strike, and is reduced by armour
 * (physical, `armour / (armour + factor × damage)`) and by resistances (elements and chaos,
 * capped). Energy shield takes a hit before life, except chaos, which goes around it; a shield left
 * alone for the rule's delay recharges. Leech gives back a share of what was dealt; a hit big enough
 * against the target's life stuns it, and a frozen or stunned fighter does nothing until it passes.
 * Every landing hit may inflict the ailments its damage types carry: burning, poison and bleeding
 * deal a share of the hit over time, chill slows the target's actions, shock makes it take more, a
 * freeze stops it. Life and shield regenerate as they go.
 *
 * The client fights by the owner's decision (rule 23); every constant here is the server's [rules].
 */
class Battle(
    val hero: Combatant,
    val foes: List<Foe>,
    val rules: CombatRules,
    heroLife: Double,
    private val random: Random,
    val stance: HeroStance = HeroStance(),
    /** How many fight on the hero's side (2.71.0); alone, the hero is a lone wolf. */
    val party: Int = 1,
) {
    /** «Волк-одиночка»: the hero alone deals more and takes less of every damage, by the server's [CombatRules.loneWolf]. */
    val loneWolf: Boolean get() = party <= 1
    /** One side in motion: its pools, its clocks and what is on it; [index] is its place in the pack, -1 for the hero. */
    inner class Fighter(val side: Side, val body: Combatant, life: Double, val index: Int = -1, val ranged: Boolean = false) {
        var life = life.coerceIn(0.0, body.maxLife)
        var shield = body.maxShield
        var nextAttack = if (side == Side.HERO) 0.35 else 0.55 + index * 0.13
        var attackInterval = 1 / body.attackSpeed
        /** Stunned or frozen until then: nothing is swung before it. */
        var heldUntil = 0.0
        var lastHit = -1e9
        val ailments = mutableListOf<ActiveAilment>()
        /** Damage over time gathered per ailment since its last tick was logged, and when that was. */
        val ticking = mutableMapOf<Ailment, Double>()
        val tickedAt = mutableMapOf<Ailment, Double>()

        val alive: Boolean get() = life > 0
        val held: Boolean get() = heldUntil > time
        fun frozen() = ailments.any { it.ailment == Ailment.FROZEN }
        /** Chill slows every action; the strongest chill counts. */
        fun slow() = 1 + (ailments.filter { it.ailment == Ailment.CHILLED }.maxOfOrNull { it.magnitude } ?: 0.0) / 100
        /** Shock makes every hit heavier. */
        fun weakness() = 1 + (ailments.filter { it.ailment == Ailment.SHOCKED }.maxOfOrNull { it.magnitude } ?: 0.0) / 100
        fun stacks(ailment: Ailment) = ailments.count { it.ailment == ailment }
    }

    val heroFighter = Fighter(Side.HERO, hero, heroLife)
    val foeFighters: List<Fighter> = foes.mapIndexed { i, foe -> Fighter(Side.MONSTER, foe.body, foe.body.maxLife, i, foe.ranged) }
    private val ailmentRules: Map<AilmentRule, Pair<Ailment, DamageType>> = rules.ailments
        .mapNotNull { rule -> Ailment.of(rule.ailment)?.let { a -> DamageType.of(rule.type)?.let { t -> rule to (a to t) } } }.toMap()

    var time = 0.0
        private set
    /** When the fight ended; equals [time] then, and stays. */
    var duration = 0.0
        private set
    var outcome: Outcome? = null
        private set
    /** The foe the player singled out, until it falls or is tapped again. */
    var focus: Int? = null
        private set
    /** The foe that struck the hero last — the Templar's answer. */
    private var lastStriker: Int? = null
    private var retreatAt = Double.NaN
    private var carry = 0.0
    private val log = mutableListOf<CombatEvent>()
    val events: List<CombatEvent> get() = log
    private val fallenOrder = mutableListOf<Int>()
    /** The foes that fell, in the order they fell: each is a kill to report the moment it happens. */
    val fallen: List<Int> get() = fallenOrder

    fun foe(index: Int) = foeFighters[index]
    val heroLife: Double get() = heroFighter.life
    val retreating: Boolean get() = !retreatAt.isNaN()

    /** Moves the fight on by [dt] seconds in fixed slices, so a frame's length never changes what happens. */
    fun advance(dt: Double) {
        // Once it is over the clock still runs, so the last blow's lunge and fade can play out.
        if (outcome != null) { time += dt; return }
        carry += dt
        while (carry >= STEP - 1e-12 && outcome == null) { carry -= STEP; step(STEP) }
    }

    /** Turns to leave: the hero stops swinging, the pack gets the rule's delay of free swings, then the fight is over. */
    fun retreat(): Boolean {
        if (outcome != null || retreating) return false
        retreatAt = time + rules.retreat.delay
        record(Side.HERO, Action.RETREAT, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, target()?.index ?: 0)
        return true
    }

    /** Singles out foe [index]; the same foe again, or a fallen one, gives the choice back to the class. */
    fun focus(index: Int?) {
        focus = index?.takeIf { it != focus && foeFighters.getOrNull(it)?.alive == true }
    }

    /** Foes taunting right now: while any stands, only they can be struck. */
    private fun taunters() = foeFighters.filter { it.alive && it.body.taunt }

    /**
     * Whether the hero can strike foe [index] right now: a taunter always, past any row (2.71.0),
     * and while one stands nobody else; otherwise as the weapon reaches.
     */
    fun reachable(index: Int): Boolean {
        val foe = foeFighters.getOrNull(index)?.takeIf { it.alive } ?: return false
        if (taunters().isNotEmpty()) return foe.body.taunt
        return stance.ranged || !foe.ranged || foeFighters.none { it.alive && !it.ranged }
    }

    /**
     * The foe the hero's next swing goes to: the focus while it can be struck, else the class's
     * pick among those that can. A focus behind the front, or behind a taunter, waits its turn.
     */
    fun target(): Fighter? {
        val reach = foeFighters.filter { reachable(it.index) }
        if (reach.isEmpty()) return null
        focus?.let { f -> reach.firstOrNull { it.index == f }?.let { return it } }
        fun weakest() = reach.minBy { it.life + it.shield }
        fun threat() = reach.maxBy { it.body.threat }
        fun avenge() = lastStriker?.let { s -> reach.firstOrNull { it.index == s } } ?: threat()
        return when (stance.rule) {
            TargetRule.THREAT -> threat()
            TargetRule.WEAKEST -> weakest()
            TargetRule.EXPOSED -> hero.leading.let { type -> reach.minWith(compareBy<Fighter> { it.body.resist(type) }.thenBy { it.life + it.shield }) }
            TargetRule.AVENGE -> avenge()
            TargetRule.ADAPTIVE -> if (heroFighter.life >= hero.maxLife / 2) weakest() else avenge()
        }
    }

    /** The blow whose lunge is on screen right now, and how far into it the scene is (0..1). */
    fun lunge(): Pair<CombatEvent, Double>? = log.lastOrNull { it.action != Action.TICK && time - it.time in -LUNGE..LUNGE }
        ?.let { it to ((time - it.time + LUNGE) / (2 * LUNGE)).coerceIn(0.0, 1.0) }

    /** How far [fighter] is into its next swing, 0 just after one and 1 as the next lands. */
    fun swing(fighter: Fighter): Float = when {
        outcome != null || !fighter.alive || (fighter.side == Side.HERO && retreating) -> 0f
        else -> (1 - (fighter.nextAttack - time) / fighter.attackInterval).toFloat().coerceIn(0f, 1f)
    }

    /** The log as a test or a report reads it, once the fight is over. */
    fun log(): CombatLog = CombatLog(log.toList(), outcome ?: Outcome.RETREAT, heroLife, duration)

    // ==================== One slice of time ====================

    private fun step(dt: Double) {
        time += dt
        (listOf(heroFighter) + foeFighters).forEach { regenerate(it, dt); burn(it, dt) }
        if (finished()) return
        // Whoever is due first acts first; several may be due in one slice.
        (listOf(heroFighter) + foeFighters).sortedBy { it.nextAttack }.forEach { me ->
            if (!me.alive || me.held || (me.side == Side.HERO && retreating) || me.nextAttack > time) return@forEach
            val target = if (me.side == Side.HERO) target() else heroFighter
            if (target != null) attack(me, target)
            me.nextAttack = time + me.attackInterval * me.slow()
            if (finished()) return
        }
        if (retreating && time >= retreatAt) end(Outcome.RETREAT)
        else if (time >= rules.timeLimit) end(Outcome.RETREAT)
    }

    private fun regenerate(me: Fighter, dt: Double) {
        if (!me.alive) return
        me.life = min(me.body.maxLife, me.life + me.body.lifeRegen * dt)
        val recharge = if (time - me.lastHit >= rules.shield.rechargeDelay) me.body.maxShield * rules.shield.rechargePerSecond / 100 else 0.0
        me.shield = min(me.body.maxShield, me.shield + (me.body.shieldRegen + recharge) * dt)
    }

    /** Ailments run their course: damage over time is applied every slice and logged once a second. */
    private fun burn(me: Fighter, dt: Double) {
        if (me.ailments.isEmpty()) return
        val wasAlive = me.alive
        me.ailments.filter { it.ailment.hurts }.forEach { active ->
            val slice = active.magnitude * min(dt, active.until - (time - dt)).coerceAtLeast(0.0) * me.weakness()
            if (slice <= 0 || !me.alive) return@forEach
            val chaos = active.ailment == Ailment.POISONED
            val absorbed = if (chaos) 0.0 else min(me.shield, slice)
            me.shield -= absorbed
            me.life = max(0.0, me.life - (slice - absorbed))
            me.ticking.merge(active.ailment, slice, Double::plus)
        }
        val expired = me.ailments.filter { it.until <= time }
        me.ailments.removeAll(expired)
        me.ticking.keys.toList().forEach { ailment ->
            val since = me.tickedAt[ailment] ?: time.also { me.tickedAt[ailment] = it }
            val due = time - since >= TICK - 1e-9
            if (due || expired.any { it.ailment == ailment } || !me.alive) {
                val amount = me.ticking.remove(ailment) ?: 0.0
                me.tickedAt[ailment] = time
                if (amount > 0) {
                    val active = (me.ailments + expired).firstOrNull { it.ailment == ailment }
                    val source = active?.source ?: me.side.other
                    val type = ailmentRules.entries.firstOrNull { it.value.first == ailment }?.value?.second
                    record(source, Action.TICK, HitKind.HIT, amount, type, 0.0, false, emptyList(), ailment,
                        if (me.side == Side.MONSTER) me.index else active?.foe ?: 0)
                }
            }
        }
        if (wasAlive && !me.alive) fell(me)
    }

    private fun attack(me: Fighter, target: Fighter) {
        val foe = if (me.side == Side.MONSTER) me.index else target.index
        val evade = (target.body.evasion / (target.body.evasion + rules.evasion.base + rules.evasion.perLevel * me.body.level)).coerceAtMost(rules.evasion.cap / 100)
        val kind = when {
            target.frozen() -> if (random.nextDouble() < me.body.critChance) HitKind.CRIT else HitKind.HIT
            random.nextDouble() < evade -> HitKind.EVADED
            random.nextDouble() < target.body.block -> HitKind.BLOCKED
            random.nextDouble() < me.body.critChance -> HitKind.CRIT
            else -> HitKind.HIT
        }
        if (me.side == Side.MONSTER) lastStriker = me.index
        if (kind == HitKind.EVADED || kind == HitKind.BLOCKED) { record(me.side, Action.ATTACK, kind, 0.0, null, 0.0, false, emptyList(), null, foe); return }
        // The lone wolf's share rides the blow itself, so the ailments it brings carry it once and no more.
        val lone = when {
            !loneWolf -> 1.0
            me.side == Side.HERO -> 1 + rules.loneWolf.dealt / 100
            else -> 1 - rules.loneWolf.taken / 100
        }
        val multiplier = if (kind == HitKind.CRIT) me.body.critMultiplier else 1.0
        val taken = me.body.damage.filterValues { it > 0 }.mapValues { (type, base) ->
            val raw = base * (1 + (random.nextDouble() * 2 - 1) * rules.variance / 100) * multiplier
            when (type) {
                DamageType.PHYSICAL -> raw * (1 - (target.body.armour / (target.body.armour + rules.armour.factor * raw)).coerceAtMost(rules.armour.cap / 100)) * (1 - target.body.physicalReduction)
                else -> raw * (1 - target.body.resist(type))
            }.coerceAtLeast(0.0) * target.weakness() * lone
        }
        land(me, target, Action.ATTACK, kind, taken, foe)
    }

    /** A blow that got through: the shield takes what it can, chaos goes around it, leech and stun and ailments follow. */
    private fun land(me: Fighter, target: Fighter, action: Action, kind: HitKind, taken: Map<DamageType, Double>, foe: Int) {
        val chaos = taken[DamageType.CHAOS] ?: 0.0
        val shielded = taken.values.sum() - chaos
        val absorbed = min(target.shield, shielded)
        target.shield -= absorbed
        target.life = max(0.0, target.life - (shielded - absorbed) - chaos)
        target.lastHit = time
        val dealt = taken.values.sum()
        val physical = taken[DamageType.PHYSICAL] ?: 0.0
        val healed = physical * me.body.leechPhysical + dealt * me.body.leechAll +
            (if (kind == HitKind.CRIT) dealt * me.body.critLeech else 0.0) + (if (action == Action.ATTACK) me.body.lifeOnHit else 0.0)
        me.life = min(me.body.maxLife, me.life + healed)

        var stunned = false
        // Only a fighter with a chance to avoid draws for it, so a sheet without one plays the same seed as before.
        if (target.alive && dealt >= target.body.maxLife * rules.stun.share / 100 + target.body.stunThreshold &&
            !(target.body.avoidStun > 0 && random.nextDouble() < target.body.avoidStun)) {
            stunned = true
            target.heldUntil = max(target.heldUntil, time + rules.stun.duration)
        }
        val inflicted = if (target.alive) inflict(me, target, taken) else emptyList()
        record(me.side, action, kind, dealt, taken.maxByOrNull { it.value }?.key, healed, stunned, inflicted, null, foe)
        if (!target.alive) fell(target)
    }

    /**
     * Which ailments this blow's damage brings, by the server's rules: a roll per rule whose type did some damage.
     * The hero starts from the rule's [AilmentRule.heroChance] where it has one, and the striker's gear adds to it;
     * the target may avoid it and shortens it by its own gear, and the damage over time runs heavier by the striker's.
     */
    private fun inflict(me: Fighter, target: Fighter, taken: Map<DamageType, Double>): List<Ailment> = ailmentRules.mapNotNull { (rule, what) ->
        val (ailment, type) = what
        val amount = taken[type] ?: 0.0
        val base = if (me.side == Side.HERO) rule.heroChance ?: rule.chance else rule.chance
        val chance = (base + me.body.inflictChance(ailment)).coerceAtMost(100.0) / 100
        if (amount <= 0 || chance <= 0 || amount < target.body.maxLife * rule.threshold / 100 || random.nextDouble() >= chance) return@mapNotNull null
        val avoid = target.body.avoid(ailment)
        if (avoid > 0 && random.nextDouble() < avoid) return@mapNotNull null
        val duration = rule.duration * target.body.ailmentDuration(ailment)
        val magnitude = if (ailment.hurts) amount * rule.magnitude / 100 / rule.duration * me.body.ailmentDamage(ailment) else rule.magnitude
        val fresh = ActiveAilment(ailment, time + duration, magnitude, duration, me.side, me.index.coerceAtLeast(0))
        if (ailment.hurts) target.tickedAt.putIfAbsent(ailment, time)
        val existing = target.ailments.filter { it.ailment == ailment }
        when {
            rule.stacks || existing.isEmpty() -> target.ailments += fresh
            // The strongest of its kind stays; a weaker one only refreshes how long it lasts.
            existing.maxOf { it.magnitude } <= magnitude -> { target.ailments.removeAll(existing); target.ailments += fresh }
            else -> { val kept = existing.maxBy { it.magnitude }; target.ailments.remove(kept); target.ailments += kept.copy(until = max(kept.until, fresh.until)) }
        }
        if (ailment == Ailment.FROZEN) target.heldUntil = max(target.heldUntil, fresh.until)
        ailment
    }

    /** A foe down: a kill to report, life on kill for the hero, and a focus on it let go. */
    private fun fell(fighter: Fighter) {
        if (fighter.side != Side.MONSTER || fighter.index in fallenOrder) return
        fighter.ailments.clear()
        fallenOrder += fighter.index
        if (focus == fighter.index) focus = null
        if (lastStriker == fighter.index) lastStriker = null
        if (heroFighter.alive) heroFighter.life = min(hero.maxLife, heroFighter.life + hero.lifeOnKill)
    }

    private fun finished(): Boolean {
        if (outcome != null) return true
        when {
            !heroFighter.alive -> end(Outcome.LOSS)
            foeFighters.none { it.alive } -> end(Outcome.WIN)
            else -> return false
        }
        return true
    }

    private fun end(how: Outcome) { outcome = how; duration = time }

    private fun record(actor: Side, action: Action, kind: HitKind, damage: Double, type: DamageType?, healed: Double, stunned: Boolean,
                       inflicted: List<Ailment>, ailment: Ailment?, foe: Int) {
        val m = foeFighters.getOrNull(foe)
        log += CombatEvent(time, actor, action, kind, damage, type, healed, stunned, inflicted, ailment, heroFighter.life, heroFighter.shield,
            m?.life ?: 0.0, m?.shield ?: 0.0, foe)
    }

    companion object {
        /** The slice the fight is stepped in: fine enough for a 5-per-second swing, fixed so a fight is a function of its seed. */
        const val STEP = 1.0 / 60
        /** How often an ailment's damage is written into the log. */
        const val TICK = 1.0
        const val LUNGE = 0.16
    }
}
