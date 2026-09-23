package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.AilmentRule
import com.sperance.exileforge.core.model.campaign.CombatRules
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Who acted. */
enum class Side { HERO, MONSTER; val other: Side get() = if (this == HERO) MONSTER else HERO }

/** What a fighter did: swung a weapon, cast the spell, let an ailment burn on, drank, or turned to leave. */
enum class Action { ATTACK, SPELL, TICK, FLASK, RETREAT }

/** How a blow ended: it landed, landed hard, or never reached. */
enum class HitKind { HIT, CRIT, EVADED, BLOCKED }

/** How the whole fight ended; a retreat is a fight nobody won inside the time limit, or one the hero walked out of. */
enum class Outcome { WIN, LOSS, RETREAT }

/** Damage by type, as the sheet names it; magical is the spell's and goes around armour, evasion and resistances alike. */
enum class DamageType(val attack: String, val resist: String?) {
    PHYSICAL("STOCK_ATTACK_PHYSICAL", null),
    FIRE("STOCK_ATTACK_FIRE", "STOCK_RESIST_FIRE"),
    COLD("STOCK_ATTACK_COLD", "STOCK_RESIST_COLD"),
    LIGHTNING("STOCK_ATTACK_LIGHTNING", "STOCK_RESIST_LIGHTNING"),
    CHAOS("STOCK_ATTACK_CHAOS", "STOCK_RESIST_CHAOS"),
    MAGICAL("STOCK_ATTACK_MAGICAL", null);

    companion object { fun of(stat: String) = entries.firstOrNull { it.attack == stat } }
}

/** The six ailments the server rules (its `EnumStatBool` without the prefix). Which damage brings which is the rule's, not ours. */
enum class Ailment { BURNING, CHILLED, FROZEN, SHOCKED, POISONED, BLEEDING;
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
 * every attack has. The hero also knows one spell ([innateSpell]): its base is the rule's innate
 * damage per level on top of whatever spell damage the sheet has, and it is cast beside the swings
 * at the sheet's cast speed (or the rule's) for a share of the mana. A monster casts only when its
 * table gives it spell damage and mana.
 */
data class Combatant(val stats: Map<String, Double>, val level: Int, val rules: CombatRules = CombatRules(), val innateSpell: Boolean = false) {
    private fun stat(name: String) = stats[name] ?: 0.0
    private fun cap(value: Double) = value.coerceIn(0.0, rules.resistCap) / 100

    val maxLife = max(1.0, stat("STOCK_HEALTH"))
    val maxShield = max(0.0, stat("STOCK_ENERGY_SHIELD"))
    val maxMana = max(0.0, stat("STOCK_MANA"))
    val damage: Map<DamageType, Double> = DamageType.entries.filter { it != DamageType.MAGICAL }.associateWith { max(0.0, stat(it.attack)) }
        .let { rolled -> if (rolled.values.sum() > 0) rolled else rolled + (DamageType.PHYSICAL to rules.unarmed.damage) }
    val attackSpeed = stat("STOCK_ATTACK_SPEED").takeIf { it > 0 }?.coerceIn(0.3, 5.0) ?: rules.unarmed.speed
    val spellDamage = max(0.0, stat("STOCK_ATTACK_MAGICAL")) + (if (innateSpell) rules.spell.innateDamage + rules.spell.innatePerLevel * (level - 1) else 0.0)
    val castSpeed = stat("STOCK_CAST_SPEED").takeIf { it > 0 }?.coerceIn(0.2, 5.0) ?: rules.spell.castSpeed
    val castStrength = 1 + max(0.0, stat("STOCK_CAST_STRENGTH")) / 100
    val casts = spellDamage > 0 && maxMana > 0
    val manaCost = maxMana * rules.spell.manaCost / 100
    val manaRegen = max(0.0, stat("STOCK_MANA_REGEN")) + maxMana * rules.spell.manaRegenShare / 100
    val critChance = (stats["STOCK_CRITICAL_CHANCE"] ?: rules.critical.chance).coerceIn(0.0, 100.0) / 100
    val critMultiplier = max(100.0, (stats["STOCK_CRITICAL_MULTIPLIER"] ?: rules.critical.multiplier) + stat("STOCK_CRITICAL_DAMAGE")) / 100
    val armour = max(0.0, stat("STOCK_ARMOR"))
    val evasion = max(0.0, stat("STOCK_EVASION"))
    val block = stat("STOCK_BLOCK_CHANCE").coerceIn(0.0, rules.blockCap) / 100
    /** Chaos stands alone, as in PoE; "all resistances" covers the three elements. */
    fun resist(type: DamageType): Double = when (val name = type.resist) {
        null -> 0.0
        "STOCK_RESIST_CHAOS" -> cap(stat(name))
        else -> cap(stat(name) + stat("STOCK_RESIST_ALL"))
    }
    val lifeRegen = max(0.0, stat("STOCK_HEALTH_REGEN"))
    val shieldRegen = max(0.0, stat("STOCK_ENERGY_REGEN"))
    val leechPhysical = max(0.0, stat("STOCK_LEECH_PHYSICAL")) / 100
    val leechMagical = max(0.0, stat("STOCK_LEECH_MAGICAL")) / 100
    val leechAll = max(0.0, stat("STOCK_LEECH_ALL")) / 100
    val critLeech = max(0.0, stat("STOCK_CRITICAL_VAMPIRE")) / 100
    val stunThreshold = max(0.0, stat("STOCK_STUN_THRESHOLD"))
}

/** An ailment on a fighter: what, until when, how hard, and who put it there. */
data class ActiveAilment(val ailment: Ailment, val until: Double, val magnitude: Double, val duration: Double, val source: Side)

/**
 * One thing that happened, and where both sides stood after it.
 *
 * [actor] is who did it; what it did was done to the other side, except a [Action.FLASK], which is
 * drunk by the actor. A [Action.TICK] is an ailment's damage gathered over the last second, so the
 * log is not a flood; [type] is the damage that led — the biggest share of a hit, or the ailment's.
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
    val heroMana: Double,
    val monsterLife: Double,
    val monsterShield: Double,
) {
    /** Who the number floats off. */
    val target: Side get() = if (action == Action.FLASK) actor else actor.other
    val landed: Boolean get() = kind == HitKind.HIT || kind == HitKind.CRIT
}

/** A whole fight, as a test or a replay reads it. */
data class CombatLog(val events: List<CombatEvent>, val outcome: Outcome, val heroLife: Double, val heroMana: Double, val flasks: Int, val duration: Double)

/**
 * The fight, alive: stepped in fixed slices of time so that the same seed is the same fight on any
 * screen, and open to the player while it runs — a flask can be drunk and a retreat begun.
 *
 * Each side swings at its own attack speed and, if it has a spell and the mana, casts at its cast
 * speed beside the swings. A swing can be evaded (evasion against the attacker's level), blocked,
 * or land; a landing hit rolls the rule's variance per damage type, may be a critical strike, and is
 * reduced by armour (physical, `armour / (armour + factor × damage)`) and by resistances (elements
 * and chaos, capped). A spell cannot be evaded, is blocked at a share of the block chance, ignores
 * armour and resistances, and costs mana. Energy shield takes a hit before life, except chaos,
 * which goes around it; a shield left alone for the rule's delay recharges. Leech gives back a share
 * of what was dealt; a hit big enough against the target's life stuns it, and a frozen or stunned
 * fighter does nothing until it passes. Every landing hit may inflict the ailments its damage types
 * carry: burning, poison and bleeding deal a share of the hit over time, chill slows the target's
 * actions, shock makes it take more, a freeze stops it. Life, mana and shield regenerate as they go.
 *
 * The client fights by the owner's decision (rule 23); every constant here is the server's [rules].
 * Auras and curses are not part of it: `STOCK_AURA_EFFECT` and `STOCK_CURSE_EFFECT` have nothing
 * to act on yet.
 */
class Battle(
    val hero: Combatant,
    val monster: Combatant,
    val rules: CombatRules,
    heroLife: Double,
    heroMana: Double,
    flasks: Int,
    private val random: Random,
) {
    /** One side in motion: its pools, its clocks and what is on it. */
    inner class Fighter(val side: Side, val body: Combatant, life: Double, mana: Double) {
        var life = life.coerceIn(0.0, body.maxLife)
        var shield = body.maxShield
        var mana = mana.coerceIn(0.0, body.maxMana)
        var nextAttack = if (side == Side.HERO) 0.35 else 0.55
        var nextCast = if (side == Side.HERO) 0.9 else 1.1
        var attackInterval = 1 / body.attackSpeed
        var castInterval = 1 / body.castSpeed
        /** Stunned or frozen until then: nothing is swung or cast before it. */
        var heldUntil = 0.0
        var lastHit = -1e9
        val ailments = mutableListOf<ActiveAilment>()
        /** Damage over time gathered per ailment since its last tick was logged, and when that was. */
        val ticking = mutableMapOf<Ailment, Double>()
        val tickedAt = mutableMapOf<Ailment, Double>()
        var flaskUntil = 0.0
        var flaskRate = 0.0

        val alive: Boolean get() = life > 0
        val held: Boolean get() = heldUntil > time
        fun frozen() = ailments.any { it.ailment == Ailment.FROZEN }
        /** Chill slows every action; the strongest chill counts. */
        fun slow() = 1 + (ailments.filter { it.ailment == Ailment.CHILLED }.maxOfOrNull { it.magnitude } ?: 0.0) / 100
        /** Shock makes every hit heavier. */
        fun weakness() = 1 + (ailments.filter { it.ailment == Ailment.SHOCKED }.maxOfOrNull { it.magnitude } ?: 0.0) / 100
        fun stacks(ailment: Ailment) = ailments.count { it.ailment == ailment }
    }

    private val fighters = mapOf(
        Side.HERO to Fighter(Side.HERO, hero, heroLife, heroMana),
        Side.MONSTER to Fighter(Side.MONSTER, monster, monster.maxLife, monster.maxMana),
    )
    private val ailmentRules: Map<AilmentRule, Pair<Ailment, DamageType>> = rules.ailments
        .mapNotNull { rule -> Ailment.of(rule.ailment)?.let { a -> DamageType.of(rule.type)?.let { t -> rule to (a to t) } } }.toMap()

    var time = 0.0
        private set
    /** When the fight ended; equals [time] then, and stays. */
    var duration = 0.0
        private set
    var outcome: Outcome? = null
        private set
    var flasks = flasks
        private set
    private var retreatAt = Double.NaN
    private var carry = 0.0
    private val log = mutableListOf<CombatEvent>()
    val events: List<CombatEvent> get() = log

    fun fighter(side: Side) = fighters.getValue(side)
    val heroLife: Double get() = fighter(Side.HERO).life
    val heroMana: Double get() = fighter(Side.HERO).mana
    val retreating: Boolean get() = !retreatAt.isNaN()
    val flaskActive: Boolean get() = fighter(Side.HERO).flaskUntil > time

    /** Moves the fight on by [dt] seconds in fixed slices, so a frame's length never changes what happens. */
    fun advance(dt: Double) {
        // Once it is over the clock still runs, so the last blow's lunge and fade can play out.
        if (outcome != null) { time += dt; return }
        carry += dt
        while (carry >= STEP - 1e-12 && outcome == null) { carry -= STEP; step(STEP) }
    }

    /** Drinks a charge: [FlaskRule.heal] percent of life over its duration. One at a time, and never after the fight. */
    fun useFlask(): Boolean {
        val me = fighter(Side.HERO)
        if (outcome != null || flasks <= 0 || me.flaskUntil > time) return false
        flasks--
        me.flaskUntil = time + rules.flask.duration
        me.flaskRate = me.body.maxLife * rules.flask.heal / 100 / rules.flask.duration
        record(Side.HERO, Action.FLASK, HitKind.HIT, 0.0, null, me.body.maxLife * rules.flask.heal / 100, false, emptyList(), null)
        return true
    }

    /** Turns to leave: the hero stops swinging, the monster gets the rule's delay of free swings, then the fight is over. */
    fun retreat(): Boolean {
        if (outcome != null || retreating) return false
        retreatAt = time + rules.retreat.delay
        record(Side.HERO, Action.RETREAT, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null)
        return true
    }

    /** The blow whose lunge is on screen right now, and how far into it the scene is (0..1). */
    fun lunge(): Pair<CombatEvent, Double>? = log.lastOrNull { it.action != Action.TICK && time - it.time in -LUNGE..LUNGE }
        ?.let { it to ((time - it.time + LUNGE) / (2 * LUNGE)).coerceIn(0.0, 1.0) }

    /** How far [side] is into its next swing, 0 just after one and 1 as the next lands. */
    fun swing(side: Side): Float = fighter(side).let { if (outcome != null || (side == Side.HERO && retreating)) 0f else (1 - (it.nextAttack - time) / it.attackInterval).toFloat().coerceIn(0f, 1f) }

    /** The same for the spell; a side that does not cast reads as empty. */
    fun cast(side: Side): Float = fighter(side).let { if (outcome != null || !it.body.casts || (side == Side.HERO && retreating)) 0f else (1 - (it.nextCast - time) / it.castInterval).toFloat().coerceIn(0f, 1f) }

    /** The log as a test or a report reads it, once the fight is over. */
    fun log(): CombatLog = CombatLog(log.toList(), outcome ?: Outcome.RETREAT, heroLife, heroMana, flasks, duration)

    // ==================== One slice of time ====================

    private fun step(dt: Double) {
        time += dt
        fighters.values.forEach { regenerate(it, dt); burn(it, dt) }
        if (finished()) return
        // Whoever is due first acts first; both may be due in one slice.
        listOf(Side.HERO, Side.MONSTER).sortedBy { fighter(it).nextAttack }.forEach { side ->
            val me = fighter(side)
            if (!me.alive || me.held || (side == Side.HERO && retreating)) return@forEach
            if (me.nextAttack <= time) { attack(me, fighter(side.other)); me.nextAttack = time + me.attackInterval * me.slow() }
            if (finished()) return
            if (me.body.casts && me.nextCast <= time) {
                if (me.mana >= me.body.manaCost) { me.mana -= me.body.manaCost; cast(me, fighter(side.other)); me.nextCast = time + me.castInterval * me.slow() }
                else me.nextCast = time + 0.25
            }
            if (finished()) return
        }
        if (retreating && time >= retreatAt) end(Outcome.RETREAT)
        else if (time >= rules.timeLimit) end(Outcome.RETREAT)
    }

    private fun regenerate(me: Fighter, dt: Double) {
        if (!me.alive) return
        me.life = min(me.body.maxLife, me.life + me.body.lifeRegen * dt)
        me.mana = min(me.body.maxMana, me.mana + me.body.manaRegen * dt)
        val recharge = if (time - me.lastHit >= rules.shield.rechargeDelay) me.body.maxShield * rules.shield.rechargePerSecond / 100 else 0.0
        me.shield = min(me.body.maxShield, me.shield + (me.body.shieldRegen + recharge) * dt)
        if (me.flaskUntil > time) me.life = min(me.body.maxLife, me.life + me.flaskRate * dt)
    }

    /** Ailments run their course: damage over time is applied every slice and logged once a second. */
    private fun burn(me: Fighter, dt: Double) {
        if (me.ailments.isEmpty()) return
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
                    val source = (me.ailments + expired).firstOrNull { it.ailment == ailment }?.source ?: me.side.other
                    val type = ailmentRules.entries.firstOrNull { it.value.first == ailment }?.value?.second
                    record(source, Action.TICK, HitKind.HIT, amount, type, 0.0, false, emptyList(), ailment)
                }
            }
        }
    }

    private fun attack(me: Fighter, target: Fighter) {
        val evade = (target.body.evasion / (target.body.evasion + rules.evasion.base + rules.evasion.perLevel * me.body.level)).coerceAtMost(rules.evasion.cap / 100)
        val kind = when {
            target.frozen() -> if (random.nextDouble() < me.body.critChance) HitKind.CRIT else HitKind.HIT
            random.nextDouble() < evade -> HitKind.EVADED
            random.nextDouble() < target.body.block -> HitKind.BLOCKED
            random.nextDouble() < me.body.critChance -> HitKind.CRIT
            else -> HitKind.HIT
        }
        if (kind == HitKind.EVADED || kind == HitKind.BLOCKED) { record(me.side, Action.ATTACK, kind, 0.0, null, 0.0, false, emptyList(), null); return }
        val multiplier = if (kind == HitKind.CRIT) me.body.critMultiplier else 1.0
        val taken = me.body.damage.filterValues { it > 0 }.mapValues { (type, base) ->
            val raw = base * (1 + (random.nextDouble() * 2 - 1) * rules.variance / 100) * multiplier
            when (type) {
                DamageType.PHYSICAL -> raw * (1 - (target.body.armour / (target.body.armour + rules.armour.factor * raw)).coerceAtMost(rules.armour.cap / 100))
                else -> raw * (1 - target.body.resist(type))
            }.coerceAtLeast(0.0) * target.weakness()
        }
        land(me, target, Action.ATTACK, kind, taken)
    }

    private fun cast(me: Fighter, target: Fighter) {
        val kind = when {
            !target.frozen() && random.nextDouble() < target.body.block * rules.spellBlockShare / 100 -> HitKind.BLOCKED
            random.nextDouble() < me.body.critChance -> HitKind.CRIT
            else -> HitKind.HIT
        }
        if (kind == HitKind.BLOCKED) { record(me.side, Action.SPELL, kind, 0.0, DamageType.MAGICAL, 0.0, false, emptyList(), null); return }
        val multiplier = if (kind == HitKind.CRIT) me.body.critMultiplier else 1.0
        val raw = me.body.spellDamage * me.body.castStrength * (1 + (random.nextDouble() * 2 - 1) * rules.variance / 100) * multiplier * target.weakness()
        land(me, target, Action.SPELL, kind, mapOf(DamageType.MAGICAL to raw))
    }

    /** A blow that got through: the shield takes what it can, chaos goes around it, leech and stun and ailments follow. */
    private fun land(me: Fighter, target: Fighter, action: Action, kind: HitKind, taken: Map<DamageType, Double>) {
        val chaos = taken[DamageType.CHAOS] ?: 0.0
        val shielded = taken.values.sum() - chaos
        val absorbed = min(target.shield, shielded)
        target.shield -= absorbed
        target.life = max(0.0, target.life - (shielded - absorbed) - chaos)
        target.lastHit = time
        val dealt = taken.values.sum()
        val physical = taken[DamageType.PHYSICAL] ?: 0.0
        val healed = physical * me.body.leechPhysical + (dealt - physical) * me.body.leechMagical + dealt * me.body.leechAll +
            (if (kind == HitKind.CRIT) dealt * me.body.critLeech else 0.0)
        me.life = min(me.body.maxLife, me.life + healed)

        var stunned = false
        if (target.alive && dealt >= target.body.maxLife * rules.stun.share / 100 + target.body.stunThreshold) {
            stunned = true
            target.heldUntil = max(target.heldUntil, time + rules.stun.duration)
        }
        val inflicted = if (target.alive) inflict(me, target, taken) else emptyList()
        record(me.side, action, kind, dealt, taken.maxByOrNull { it.value }?.key, healed, stunned, inflicted, null)
    }

    /** Which ailments this blow's damage brings, by the server's rules: a roll per rule whose type did some damage. */
    private fun inflict(me: Fighter, target: Fighter, taken: Map<DamageType, Double>): List<Ailment> = ailmentRules.mapNotNull { (rule, what) ->
        val (ailment, type) = what
        val amount = taken[type] ?: 0.0
        if (amount <= 0 || amount < target.body.maxLife * rule.threshold / 100 || random.nextDouble() >= rule.chance / 100) return@mapNotNull null
        val magnitude = if (ailment.hurts) amount * rule.magnitude / 100 / rule.duration else rule.magnitude
        val fresh = ActiveAilment(ailment, time + rule.duration, magnitude, rule.duration, me.side)
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

    private fun finished(): Boolean {
        if (outcome != null) return true
        when {
            !fighter(Side.MONSTER).alive -> end(Outcome.WIN)
            !fighter(Side.HERO).alive -> end(Outcome.LOSS)
            else -> return false
        }
        return true
    }

    private fun end(how: Outcome) { outcome = how; duration = time }

    private fun record(actor: Side, action: Action, kind: HitKind, damage: Double, type: DamageType?, healed: Double, stunned: Boolean, inflicted: List<Ailment>, ailment: Ailment?) {
        val h = fighter(Side.HERO)
        val m = fighter(Side.MONSTER)
        log += CombatEvent(time, actor, action, kind, damage, type, healed, stunned, inflicted, ailment, h.life, h.shield, h.mana, m.life, m.shield)
    }

    companion object {
        /** The slice the fight is stepped in: fine enough for a 5-per-second swing, fixed so a fight is a function of its seed. */
        const val STEP = 1.0 / 60
        /** How often an ailment's damage is written into the log. */
        const val TICK = 1.0
        const val LUNGE = 0.16
    }
}

/** The fight run through from start to finish, for a test or a summary. */
object Combat {
    fun fight(hero: Combatant, monster: Combatant, heroLife: Double, random: Random, rules: CombatRules = hero.rules, heroMana: Double = hero.maxMana, flasks: Int = 0): CombatLog {
        val battle = Battle(hero, monster, rules, heroLife, heroMana, flasks, random)
        while (battle.outcome == null) battle.advance(1.0)
        return battle.log()
    }
}
