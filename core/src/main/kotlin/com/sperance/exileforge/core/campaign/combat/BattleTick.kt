package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.FactorKey
import com.sperance.exileforge.core.campaign.FactorTrace
import com.sperance.exileforge.core.campaign.LOW_LIFE
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.PowerMoment
import com.sperance.exileforge.core.campaign.RollKey
import com.sperance.exileforge.core.campaign.StatLines
import com.sperance.exileforge.core.campaign.TickTrace
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.TICK
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.SkillEvent
import kotlin.math.max
import kotlin.math.min

// ==================== One slice of time ====================

internal fun Battle.step(dt: Double) {
    time += dt
    lookLife()
    (listOf(heroFighter) + listOfNotNull(allyFighter) + foeFighters).forEach {
        regenerate(it, dt)
        degenerate(it, dt)
        burn(it, dt)
    }
    // A support pet mends the hero while it stands (3.5.0).
    allyFighter?.takeIf { it.alive && heroFighter.alive && ally!!.heal > 0 && joined() }?.let {
        mended(lifeBack(heroFighter, heroFighter.body.maxLife * ally!!.heal / 100 * dt))
    }
    expire()
    if (finished()) return
    rage()
    riftTick()
    powers.tick()
    if (finished()) return
    val hero = heroFighter
    // A draught goes down even stunned; a skill waits until the hero can move again. Засада Разлома (3.96.0) - и то и другое позже.
    if (hero.alive && joined()) {
        if (!autoDrunk) {
            autoDrunk = true
            if (hero.body.flasksAuto) {
                kit.flasks.forEachIndexed { i, flask ->
                    if (flask != null && draughtOf(i) == null) {
                        flaskOpened[i] = true
                        drink(i, flask, free = true)
                    }
                }
            }
        }
        useFlasks()
        if (!hero.held) useSkills()
    }
    taps.clear()
    drinks.clear()
    foeFighters.forEach { if (it.alive && !it.held) monsterCast(it) }
    if (finished()) return
    // Whoever is due first acts first; several may be due in one slice.
    (listOf(heroFighter) + listOfNotNull(allyFighter) + foeFighters).sortedBy { it.nextAttack }.forEach { me ->
        if (!me.alive || me.held || me.nextAttack > time) return@forEach
        val target = if (me.side == Side.HERO) target() else foeTarget()
        if (target != null) strike(me, target, Blow(firstStrike(me)))
        me.nextAttack = time + me.attackInterval * me.slow() * cadence(me)
        if (finished()) return
    }
    watch()
    // Без предела времени (2.74.0) и без отступления (3.88.8): бой идёт, пока не падёт одна из сторон.
}

private fun Battle.regenerate(me: Fighter, dt: Double) {
    if (!me.alive) return
    // The bars melt once no blow has filled them for the rule's delay (3.78.0).
    rules.buildup?.let { rule -> if (time - me.builtAt >= rule.decayDelay) for (i in me.buildup.indices) me.buildup[i] = max(0.0, me.buildup[i] - rule.decayPerSecond * dt) }
    val regenerated = lifeBack(me, (me.body.lifeRegen + me.body.maxLife * me.body.lifeRegenShare) * me.body.recoveryRate * dt)
    if (me === heroFighter) regenLogged += regenerated
    if (!shieldless(me)) me.shield = EnergyShield.recovered(me.body, rules, me.shield, time - me.lastHit, dt)
    if (!manaless(me)) me.mana = min(manaCap(me), me.mana + me.body.manaRegen(rules.mana) * dt)
    if (me === heroFighter) {
        recoveries.forEach { draught ->
            val slice = min(dt, draught.until - (time - dt)).coerceAtLeast(0.0)
            val gain = draught.life * slice
            val restored = lifeBack(me, gain)
            draught.restored += restored
            draught.wasted += gain - restored
            me.mana = min(manaCap(), me.mana + draught.mana * slice)
            if (draught.stopsAtFull && me.life >= me.body.maxLife) stopDraught(draught)
        }
        if (time - regenLoggedAt >= 1.0) {
            if (regenLogged >= 1) note(me, NoteKind.REGEN, "", regenLogged)
            regenLogged = 0.0
            regenLoggedAt = time
        }
    }
}

/** A pure life draught ends at full life (3.79.0): its buff goes with it, so the belt may drink it again when life falls. */
private fun Battle.stopDraught(draught: Recovery) {
    draught.until = time
    draughtOf(draught.slot)?.let { running -> heroFighter.effects[heroFighter.effects.indexOf(running)] = running.copy(until = time) }
}

/** The recovery shelf's line for a draught or a recoup that ran out: what it gave back, and what spilled over a full bar. */
private fun Battle.logRecovery(recovery: Recovery) {
    val code = kit.flasks.getOrNull(recovery.slot)?.sheet?.code
    if (recovery.restored >= 1) note(heroFighter, if (code != null) NoteKind.RECOVER_FLASK else NoteKind.RECOVER_RECOUP, code.orEmpty(), recovery.restored)
    if (recovery.wasted >= 1) note(heroFighter, NoteKind.RECOVER_WASTE, code.orEmpty(), recovery.wasted)
}

/** What ran out this slice goes: buffs, curses, draughts, a barrier, and the body is made again without them. */
private fun Battle.expire() {
    (listOf(heroFighter) + listOfNotNull(allyFighter) + foeFighters).forEach { fighter ->
        if (fighter.barrier > 0 && fighter.barrierUntil <= time) fighter.barrier = 0.0
        if (fighter.effects.removeAll { it.until <= time }) remake(fighter)
    }
    recoveries.removeAll { recovery -> (recovery.until <= time).also { if (it) logRecovery(recovery) } }
    if (heroCharges.expire(time)) remake(heroFighter)
}

/** Edges the hero's passives answer — low life, a broken shield — and the low-life lines of either side. */
internal fun Battle.watch() {
    val hero = heroFighter
    if (hero.alive) {
        val below = hero.life < hero.body.maxLife * LOW_LIFE
        if (below && !belowLow) {
            trigger(SkillEvent.LOW_LIFE)
            powers.fire(PowerEvent.LOW_LIFE)
        }
        belowLow = below
        if (hero.body.maxShield > 0) {
            val up = hero.shield > 0.5
            if (!up && shieldUp) {
                trigger(SkillEvent.SHIELD_BROKEN)
                powers.fire(PowerEvent.SHIELD_BROKEN)
            }
            shieldUp = up
        }
        val low = hero.life < hero.body.maxLife / 2
        if (low != hero.low && model.lowLife.isNotEmpty()) {
            hero.low = low
            remake(hero)
        }
        if (powers.restand()) remake(hero)
        if (conditioned) {
            heroConditions().let { now ->
                if (now != conditions) {
                    // 3.37.0: each condition that came or went is a note of the log.
                    val was = conditions
                    conditions = now
                    remake(hero)
                    (now - was).forEach { note(hero, NoteKind.CONDITION_ON, it.name) }
                    (was - now).forEach { note(hero, NoteKind.CONDITION_OFF, it.name) }
                }
            }
        }
    }
    petWatch()
    foeFighters.forEach { foe -> if (foe.alive) enrage(foe) }
    foeFighters.forEach { foe -> if (foe.alive) phase(foe) }
    if (slotHolders.isNotEmpty()) totemTick()
    foeFighters.forEach { foe ->
        val low = foe.alive && foe.life < foe.body.maxLife / 2
        if (low != foe.low && foe.model.body(emptyList())[CoreStat.LOW_LIFE_SPEED.code] > 0) {
            foe.low = low
            remake(foe)
        }
    }
}

/** The pet fell since the last look (3.33.0): the powers answering its death hear of it once. */
internal fun Battle.petWatch(killer: Fighter? = null) {
    val pet = allyFighter ?: return
    if (pet.alive || !petStood) return
    petStood = false
    powers.fire(PowerEvent.PET_DEATH, PowerMoment(killer))
}

/** [fighter]'s body made again from its sheet with what lies on it — and the hero's under the auras of the foes still standing. */
internal fun Battle.remake(fighter: Fighter) {
    val lines = fighter.effects.flatMap { it.lines }
    if (fighter === heroFighter) {
        fighter.rebody(heroBody(emptyList()))
    } else {
        val speed = fighter.model.body(emptyList())[CoreStat.LOW_LIFE_SPEED.code]
        fighter.rebody(fighter.model.body(lines + if (fighter.low && speed > 0) listOf(StatLine(CoreStat.ATTACK_SPEED.code, Op.INCREASED, speed)) else emptyList()))
    }
}

/** The hero's body for one blow: what lies on them and [extra], a skill's own lines. */
internal fun Battle.heroBody(extra: List<StatLine>): Combatant = model.body(heroLines() + extra).under(auras())

/** What lies on the hero now: low life's lines, the standing powers', the charges', the conditions' and the effects'. */
private fun Battle.heroLines(): List<StatLine> = (if (heroFighter.low) model.lowLife else emptyList()) +
    powers.standing + heroCharges.lines() + chargeLines() + model.conditional(conditions) + heroFighter.effects.flatMap { it.lines }

/**
 * The increases of [type] a hero's blow is grown by now, in percent (server 1.57.0): the sheet's own, the damage in
 * general among them, and those of the lines on the hero and [extra] — the conditions', the charges', a flask's.
 */
internal fun Battle.heroIncrease(type: DamageType, extra: List<StatLine> = emptyList()): Double = model.increased(type.attack, heroLines() + extra)

/** How much a hero's [blow] grows by [facing] percent increased beside its own increases, over all its damage. */
internal fun Battle.versus(blow: Blow, facing: Double): Double {
    val total = blow.damage.values.sum()
    if (facing == 0.0 || total <= 0) return 1.0
    return blow.damage.entries.sumOf { (type, value) -> regrow(value, blow.increase?.get(type) ?: heroIncrease(type), facing) } / total
}

/** The hero's damage for the charges held (3.35.0): so many percent increased for each of a kind, by the sheet — beside the other increases (server 1.57.0). */
internal fun Battle.chargeLines(): List<StatLine> = ChargeKind.REAL.mapNotNull { kind ->
    val per = heroFighter.body.perCharge(kind)
    val count = heroCharges.count(kind)
    if (per == 0.0 || count == 0) null else StatLine(StatLines.DAMAGE, Op.INCREASED, per * count)
}

/** What holds for the hero now (3.35.0): the states their conditional lines wait for. */
internal fun Battle.heroConditions(): Set<Condition> {
    val hero = heroFighter
    val body = hero.body
    fun recent(at: Double) = time - at <= rules.recentSeconds
    return buildSet {
        if (hero.life < body.maxLife * LOW_LIFE) add(Condition.LOW_LIFE)
        if (hero.life >= body.maxLife - 0.5) add(Condition.FULL_LIFE)
        if (body.maxShield > 0 && hero.shield >= body.maxShield - 0.5) add(Condition.FULL_SHIELD)
        if (recent(killedAt)) add(Condition.RECENT_KILL)
        if (recent(hero.lastHit)) add(Condition.RECENT_HIT_TAKEN)
        if (recent(blockedAt)) add(Condition.RECENT_BLOCK)
        if (recent(critAt)) add(Condition.RECENT_CRIT)
        BuffKind.entries.forEach { kind -> kind.condition?.let { if (hero.effects.any { e -> e.source == kind.source }) add(it) } }
        if (heroCharges.count(ChargeKind.FRENZY) > 0) add(Condition.FRENZY_CHARGE)
        if (heroCharges.count(ChargeKind.POWER) > 0) add(Condition.POWER_CHARGE)
        if (heroCharges.count(ChargeKind.ENDURANCE) > 0) add(Condition.ENDURANCE_CHARGE)
        if (hero.effects.any { it.kind == EffectKind.FLASK && it.until > time }) add(Condition.FLASK_ACTIVE)
        if (allyFighter?.alive == true) add(Condition.PET_ALIVE)
    }
}

/** How the hero's lines waiting for a target's state see [target] (3.35.0). */
internal fun Battle.states(target: Fighter): Set<Condition> {
    if (target.side != Side.MONSTER) return emptySet()
    val rarity = foes[target.index].rarity
    return buildSet {
        if (rarity >= MonsterRarity.RARE) add(Condition.VS_RARE)
        if (rarity == MonsterRarity.UNIQUE) add(Condition.VS_UNIQUE)
        if (foes[target.index].tainted) add(Condition.VS_BLIGHTED)
        if (target.life >= target.body.maxLife - 0.5) add(Condition.VS_FULL_LIFE)
    }
}

/** [kind] laid on [fighter] for the rule's time, longer by its buff duration (3.35.0). */
private fun Battle.gainBuff(fighter: Fighter, kind: BuffKind) {
    val rule = rules.buffs[kind]
    buff(fighter, kind.source, rule.lines.map { StatLine(it.stat, it.op, it.value) }, rule.duration * fighter.body.buffDuration)
    note(fighter, NoteKind.BUFF, kind.name, rule.duration * fighter.body.buffDuration)
}

/** The hero's chance at [kind]: drawn only when there is one, so a sheet without it plays the same seed as before. */
internal fun Battle.chanceBuff(kind: BuffKind) {
    val chance = heroFighter.body.buffChance(kind)
    if (chance > 0 && random.nextDouble() < chance) gainBuff(heroFighter, kind)
}

/** The buffs [fighter] wears for the whole fight (3.35.0). */
internal fun Battle.wear(fighter: Fighter) = BuffKind.entries.filter { fighter.body.wears(it) }.forEach { kind ->
    lay(fighter, TimedEffect(EffectKind.BUFF, kind.source, rules.buffs[kind].lines.map { StatLine(it.stat, it.op, it.value) }, FOREVER, FOREVER))
}

/** What of a hit on [target] its spell suppression or deflection lets through (3.35.0); drawn only for a target with a chance. */
internal fun Battle.eased(target: Fighter, blow: Blow): Double {
    val rule = rules.defence
    val chance = if (blow.spell) target.body.suppression else target.body.deflection
    if (chance <= 0 || draw(if (blow.spell) RollKey.SUPPRESS else RollKey.DEFLECT, chance) >= chance) return 1.0
    return 1 - (if (blow.spell) rule.suppressed else rule.deflected) / 100
}

/** Degeneration eats life (3.4.0): a share of the maximum a second, past the shield, and it can kill. */
private fun Battle.degenerate(me: Fighter, dt: Double) {
    if (!me.alive || me.invulnerable) return
    // The shield wastes away (3.33.0), and the delayed share of the hero's hits comes due.
    if (me.body.shieldDegenShare > 0) me.shield = max(0.0, me.shield - me.body.maxShield * me.body.shieldDegenShare * dt)
    if (me === heroFighter && delayed.isNotEmpty()) {
        delayed.forEach { me.life = max(0.0, me.life - it.rate * min(dt, it.until - (time - dt)).coerceAtLeast(0.0)) }
        delayed.removeAll { it.until <= time }
    }
    val share = me.body.lifeDegenShare
    if (share > 0) me.life = max(0.0, me.life - max(me.body.maxLife * share, rules.minDot / TICK) * dt)
    if (!me.alive) fell(me)
}

/** Ailments run their course: damage over time is applied every slice and logged once a second. */
private fun Battle.burn(me: Fighter, dt: Double) {
    if (me.ailments.isEmpty()) return
    val wasAlive = me.alive
    // The kill goes to the ailment that dealt the most this slice (3.71.0): a spell's burn beside an attack's bleed takes no one else's kill
    var heaviest: ActiveAilment? = null
    var heaviestSlice = 0.0
    me.ailments.filter { it.ailment.hurts }.forEach { active ->
        val span = min(dt, active.until - (time - dt)).coerceAtLeast(0.0)
        val rate = active.magnitude * me.weakness() * me.body.dotTaken * me.body.ailmentTaken(active.ailment)
        // Whatever deals damage at all deals at least [CombatRules.minDot] a tick, however it is taken.
        val dealt = (if (active.magnitude > 0) max(rate, rules.minDot / TICK) else rate) * span
        // Разлом (3.96.0): печати Стража и «Одна стихия» Владыки
        val rift = sealed(me) * lawTaken(me, if (active.chaos) DamageType.CHAOS else ruleOf[active.ailment]?.second)
        val slice = if (rift == 1.0) dealt else dealt * rift
        if (slice <= 0 || !me.alive || me.invulnerable) return@forEach
        val chaos = active.ailment == Ailment.POISONED || active.chaos
        if (chaos && me.body.chaosImmune) return@forEach
        wound(me, slice, chaos)
        me.ticking.merge(active.ailment, slice, Double::plus)
        if (slice > heaviestSlice) {
            heaviest = active
            heaviestSlice = slice
        }
    }
    val expired = me.ailments.filter { it.until <= time }
    val bySpell = heaviest?.let { it.spell && it.source == Side.HERO } == true
    me.ailments.removeAll(expired)
    me.ticking.keys.toList().forEach { ailment ->
        val since = me.tickedAt[ailment] ?: time.also { me.tickedAt[ailment] = it }
        val due = time - since >= TICK - 1e-9
        if (due || expired.any { it.ailment == ailment } || !me.alive) {
            var amount = me.ticking.remove(ailment) ?: 0.0
            me.tickedAt[ailment] = time
            if (amount > 0) {
                val active = (me.ailments + expired).firstOrNull { it.ailment == ailment }
                // A tick cut short — the ailment ran out or the target fell mid-beat — still deals its [CombatRules.minDot].
                if (amount < rules.minDot && me.alive && !me.invulnerable) {
                    wound(me, rules.minDot - amount, ailment == Ailment.POISONED || active?.chaos == true)
                    amount = rules.minDot
                }
                val source = active?.source ?: me.side.other
                val type = if (active?.chaos == true) DamageType.CHAOS else ruleOf[ailment]?.second
                // 3.37.0: the tick names its ailment and carries how strong it runs and what grows it.
                val own = (me.ailments + expired).filter { it.ailment == ailment }
                val base = own.filter { it.until > time - TICK }.sumOf { it.magnitude }
                val seals = sealed(me)
                val law = lawTaken(me, type)
                val factors = listOfNotNull(
                    FactorTrace(FactorKey.BASE, base, listOfNotNull(ailment.damage, CoreStat.FASTER_AILMENTS.code, CoreStat.AILMENT_DURATION.code, "STOCK_${ailment.word}_DURATION")),
                    FactorTrace(FactorKey.SHOCK, me.weakness(), target = listOf(CoreStat.SHOCK_TAKEN.code)),
                    FactorTrace(FactorKey.TAKEN, me.body.dotTaken * me.body.ailmentTaken(ailment), target = listOf(CoreStat.DOT_TAKEN.code, CoreStat.BLEED_TAKEN.code)),
                    FactorTrace(FactorKey.SEALS, seals).takeIf { seals != 1.0 },
                    FactorTrace(FactorKey.LAW, law).takeIf { law != 1.0 },
                    FactorTrace(FactorKey.TOTAL, amount),
                )
                record(
                    source, Action.TICK, HitKind.HIT, amount, type, 0.0, false, emptyList(), ailment,
                    if (me.side == Side.MONSTER) me.index else active?.foe ?: 0,
                    trace = TickTrace(
                        ailment, base * me.weakness() * me.body.dotTaken * me.body.ailmentTaken(ailment),
                        (own.maxOfOrNull { it.until } ?: time) - time, own.maxOfOrNull { it.duration } ?: 0.0, own.size, shot(me), factors, origin,
                        (if (source == Side.HERO) heroFighter else foeFighters.getOrNull(active?.foe ?: -1))?.let { shot(it) },
                    ),
                    pet = isPet(me),
                )
                if (source == Side.HERO) sealStruck(me, crit = false)
            }
        }
    }
    if (wasAlive && !me.alive) fell(me, bySpell)
}

/** Damage over time on [me]: a barrier soaks it first, then the shield — unless it is [chaos] — then life. */
private fun Battle.wound(me: Fighter, amount: Double, chaos: Boolean) {
    var rest = amount
    if (me.barrier > 0) {
        val soaked = min(me.barrier, rest)
        me.barrier -= soaked
        rest -= soaked
    }
    val absorbed = if (shieldless(me)) 0.0 else EnergyShield.absorbed(me.shield, rest, chaos = if (chaos) rest else 0.0)
    me.shield -= absorbed
    me.life = max(0.0, me.life - (rest - absorbed))
}

/** Accuracy against evasion (3.35.0, server 1.34.0), as in PoE, under the target's ceiling. */
internal fun Battle.evasion(me: Fighter, target: Fighter): Double = rules.accuracy.evaded(me.body.accuracy(rules.accuracy), target.body.evasion).coerceAtMost(target.body.evasionCap)

private fun Battle.finished(): Boolean {
    if (outcome != null) return true
    // A power may answer the hero's fall (2.79.0) and stand them back up.
    if (!heroFighter.alive) powers.fire(PowerEvent.DEATH)
    when {
        !heroFighter.alive -> end(Outcome.LOSS)
        foeFighters.none { it.alive } -> end(Outcome.WIN)
        else -> return false
    }
    return true
}

/**
 * Герой сдаётся (3.95.0): бой кончается поражением, как гибель, - с её ценой в заходе. Выход из боя, что сам не кончается.
 */
fun Battle.surrender() {
    if (outcome != null) return
    heroFighter.life = 0.0
    end(Outcome.LOSS)
}

private fun Battle.end(how: Outcome) {
    lookLife()
    outcome = how
    duration = time
}

/** Низшая доля здоровья героя за бой (3.96.0) - для Испытания чемпиона. */
private fun Battle.lookLife() {
    val max = heroFighter.body.maxLife
    if (max > 0) trial.lowestLife = min(trial.lowestLife, (heroFighter.life / max).coerceIn(0.0, 1.0))
}
