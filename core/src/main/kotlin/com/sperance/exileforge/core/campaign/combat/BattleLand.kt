package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.Landing
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.PowerMoment
import com.sperance.exileforge.core.campaign.RollKey
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.BuildupRule
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.SkillEvent
import kotlin.math.max
import kotlin.math.min

// ==================== The landing (2.78.0) ====================

/**
 * A blow that got through: a barrier soaks it first (2.78.0), the shield takes what it can, chaos goes
 * around it; leech, stun and ailments follow, and the passives hear of it.
 */
internal fun Battle.land(me: Fighter, target: Fighter, kind: HitKind, taken: Map<DamageType, Double>, foe: Int, blow: Blow, body: Combatant) {
    val dealt = taken.values.sum()
    var rest = dealt
    var soakedBarrier = 0.0
    if (target.barrier > 0) {
        val soaked = min(target.barrier, rest)
        target.barrier -= soaked
        rest -= soaked
        soakedBarrier = soaked
    }
    val chaos = if (dealt > 0) (taken[DamageType.CHAOS] ?: 0.0) * rest / dealt else 0.0
    val shielded = rest - chaos
    val absorbed = min(target.shield, shielded)
    target.shield -= absorbed
    val manaBefore = target.mana
    val bound = shielded - absorbed + chaos
    val toLifeNow = toLife(target, bound)
    val manaPaid = manaBefore - target.mana
    target.life = max(0.0, target.life - toLifeNow)
    target.lastHit = time
    // The blow that breaks a buildup's ice (3.78.0): heavier, and the fighter thaws.
    if (target.shatter) {
        target.shatter = false
        target.ailments.removeAll { it.ailment == Ailment.FROZEN }
    }
    // 3.35.0: the hero's recoup gives a share of the hit back over the rule's seconds; a culling blow finishes a foe left low.
    val recouped = if (target === heroFighter && target.alive && target.body.recoup > 0 && dealt > 0) dealt * target.body.recoup else 0.0
    if (recouped > 0) recoveries += Recovery(recouped / rules.defence.recoup, 0.0, time + rules.defence.recoup, -1)
    val culled = target.alive && !target.invulnerable && body.culling > 0 && target.life < target.body.maxLife * body.culling
    if (culled) target.life = 0.0
    val physical = taken[DamageType.PHYSICAL] ?: 0.0
    // Vampirism is capped per second (server 1.76.0): a share of the maximum life, whatever the blows.
    val leech = me.leechRoom((physical * body.leechPhysical + dealt * body.leechAll + (if (kind == HitKind.CRIT) dealt * body.critLeech else 0.0)) * body.recoveryRate, time, rules.caps.leechPerSecond)
    val onHit = (if (blow.weapon) body.lifeOnHit else 0.0) * body.recoveryRate
    // Life leech into the shield (server 1.32.0): what it restores is not life.
    if (body.leechToShield) me.shield = min(me.body.maxShield, me.shield + leech)
    val healed = if (body.leechToShield) onHit else leech + onHit
    me.life = min(me.body.maxLife, me.life + healed)
    // Mana (server 0.69.0): leeched and gained on hit; a burning blow takes the struck one's.
    if (me.body.maxMana > 0) me.mana = min(manaCap(me), me.mana + dealt * body.leechMana + if (blow.weapon) body.manaOnHit else 0.0)
    if (body.manaBurn > 0) target.mana = max(0.0, target.mana - target.body.maxMana * body.manaBurn)

    var stunned = false
    val bars = rules.buildup
    if (bars != null && target.alive) {
        stunned = buildUp(bars, me, target, taken, kind == HitKind.CRIT, blow.stun)
    } // Only a fighter with a chance to avoid draws for it, so a sheet without one plays the same seed as before.
    else if (target.alive && !target.body.immuneStun && (
            dealt >= target.body.maxLife * rules.stun.share / 100 + target.body.stunThreshold &&
                !(target.body.avoidStun > 0 && draw(RollKey.AVOID_STUN, target.body.avoidStun) < target.body.avoidStun) || blow.stun > 0 && draw(RollKey.STUN, blow.stun / 100) * 100 < blow.stun
            )
    ) {
        stunned = true
        target.heldUntil = max(target.heldUntil, time + rules.stun.duration)
    }
    val inflicted = if (target.alive) inflict(me, target, taken, blow.ailments, blow.spell) else emptyList()
    val trace = pendingHit?.copy(
        rolls = takeTape(),
        landing = Landing(
            soakedBarrier, absorbed, manaPaid, bound - toLifeNow - manaPaid,
            toLifeNow, if (body.leechToShield) 0.0 else leech, onHit, recouped, culled,
        ),
    )
    pendingHit = null
    record(
        me.side, blow.action, kind, dealt, taken.maxByOrNull { it.value }?.key, healed, stunned, inflicted, null, foe, blow.skill, trace = trace,
        pet = isPet(me) || isPet(target),
    )
    if (me === heroFighter) {
        if (target.alive && hexing()) hex(target)
        if (blow.weapon) chanceBuff(BuffKind.FORTIFY)
        if (kind == HitKind.CRIT) critAt = time
        if (kind == HitKind.CRIT) {
            flaskCharge("FLASK_CHARGE_ON_CRIT")
            trigger(SkillEvent.CRIT, target, taken = taken)
            if (blow.spell) trigger(SkillEvent.SPELL_CRIT, target, taken = taken)
        }
        // Momentum (3.32.0) grows its standing lines with every hit: the body for the next blow is made again now.
        if (powers.dealt(PowerMoment(target, taken, blow.spell), kind == HitKind.CRIT, stunned, inflicted, blow.primary) && powers.restand()) remake(heroFighter)
    }
    // The pet's blow (3.33.0, server 1.32.0): the powers answering it hear of its damage and its foe.
    if (me === allyFighter) powers.fire(PowerEvent.PET_HIT, PowerMoment(target, taken, blow.spell))
    if (target === allyFighter) petWatch(me)
    if (target === heroFighter) {
        flaskCharge("FLASK_CHARGE_WHEN_HIT")
        trigger(SkillEvent.HIT_TAKEN, me)
        powers.taken(PowerMoment(me, taken, blow.spell), kind == HitKind.CRIT)
        // «Horror» (an essence) and the curse-when-hit essence (server 1.57.0): struck, the hero may lay their own curse on the one who struck.
        val chance = target.body[CoreStat.CURSE_ON_HIT.code] + target.body[CoreStat.CURSE_WHEN_HIT.code]
        if (chance > 0 && me.alive && random.nextDouble() * 100 < chance) curseOf()?.let { curse(it, listOf(me)) }
        watch()
    }
    if (!target.alive) fell(target, blow.spell, me)
}

/**
 * What of [amount] bound for [target]'s life reaches it now (server 1.32.0): mana takes its share first, and the hero's
 * delayed share runs over the next seconds instead.
 */
private fun Battle.toLife(target: Fighter, amount: Double): Double {
    var rest = amount
    val share = target.body.manaBeforeLife
    if (share > 0 && rest > 0) {
        val paid = min(target.mana, rest * share)
        target.mana -= paid
        rest -= paid
    }
    val later = target.body.lifeDelayed
    if (target === heroFighter && later > 0 && rest > 0) {
        val seconds = target.body.lifeDelay
        delayed += Delayed(rest * later / seconds, time + seconds)
        rest *= 1 - later
    }
    return rest
}

/**
 * Which ailments this blow's damage brings, by the server's rules: a roll per rule whose type did some damage.
 * The hero starts from the rule's [AilmentRule.heroChance] where it has one, and the striker's gear adds to it;
 * the target may avoid it, be immune to it (2.78.0) and shortens it by its own gear, and the damage over time
 * runs heavier by the striker's. A skill's own chances ([extra], in percent) roll after the rules'.
 */

/**
 * The bars a landed blow fills (3.78.0): stun by its physical part whole and the rest by the rule's share (a critical
 * strike more), freeze by its cold, electrocute by its lightning, each against the struck one's pool — its life by its
 * rarity's share, the stun pool grown by the threshold — and a skill's stun chance straight onto the stun bar. A full
 * bar goes off and is shut for a while. True if the blow stunned.
 */
private fun Battle.buildUp(rule: BuildupRule, me: Fighter, target: Fighter, taken: Map<DamageType, Double>, crit: Boolean, skillStun: Double): Boolean {
    val rarity = if (target.side == Side.MONSTER) foes[target.index].rarity else null
    val pool = target.body.maxLife * (rarity?.let { rule.pools[it] } ?: rule.heroPool)
    if (pool <= 0) return false
    val physical = taken[DamageType.PHYSICAL] ?: 0.0
    val rest = taken.values.sum() - physical
    val fills = mapOf(
        Buildup.STUN to (physical + rest * rule.stunOther) * (if (crit) rule.stunCrit else 1.0) / (pool * target.body.stunPool + target.body.stunThreshold) + skillStun / 100,
        Buildup.FREEZE to (taken[DamageType.COLD] ?: 0.0) / pool,
        Buildup.ELECTROCUTE to (taken[DamageType.LIGHTNING] ?: 0.0) / pool,
    )
    var stunned = false
    fills.forEach { (kind, fill) ->
        val i = kind.ordinal
        if (fill <= 0 || target.buildupShutUntil[i] > time || target.body.immuneTo(kind)) return@forEach
        target.buildup[i] += fill * me.body.buildupGain(kind) * target.body.buildupTaken(kind)
        target.builtAt = time
        if (target.buildup[i] < 1) return@forEach
        target.buildup[i] = 0.0
        target.buildupShutUntil[i] = time + rule.immunity
        val effect = when (kind) {
            Buildup.STUN -> rule.stun
            Buildup.FREEZE -> rule.freeze
            Buildup.ELECTROCUTE -> rule.electrocute
        }
        val held = if (rarity == MonsterRarity.UNIQUE) effect.bossDuration else effect.duration
        when (kind) {
            Buildup.STUN -> {
                target.heldUntil = max(target.heldUntil, time + held)
                target.stunnedUntil = time + held
                stunned = true
            }

            Buildup.FREEZE -> {
                place(target, ActiveAilment(Ailment.FROZEN, time + held, 0.0, held, me.side, me.index.coerceAtLeast(0), false), false)
                target.shatter = true
            }

            Buildup.ELECTROCUTE -> {
                target.heldUntil = max(target.heldUntil, time + held)
                target.electrocutedUntil = time + held
            }
        }
    }
    return stunned
}

private fun Battle.inflict(me: Fighter, target: Fighter, taken: Map<DamageType, Double>, extra: List<Pair<Ailment, Double>>, spell: Boolean): List<Ailment> {
    val rolled = ailmentRules.mapNotNull { (rule, what) ->
        val (ailment, type) = what
        // The freeze is a bar since 3.78.0: no chance to roll for it while the rule's bars are on.
        if (ailment == Ailment.FROZEN && rules.buildup != null) return@mapNotNull null
        val amount = taken[type] ?: 0.0
        val base = if (me.side == Side.HERO) rule.heroChance ?: rule.chance else rule.chance
        val chance = (base + me.body.inflictChance(ailment)).coerceAtMost(100.0) / 100
        if (amount <= 0 || chance <= 0 || amount < target.body.maxLife * rule.threshold / 100 || draw(RollKey.AILMENT, chance, ailment) >= chance) return@mapNotNull null
        afflict(me, target, ailment, taken, spell)
    }
    val forced = extra.filter { it.first !in rolled && it.second > 0 }.mapNotNull { (ailment, chance) ->
        if (draw(RollKey.AILMENT, chance / 100, ailment) * 100 < chance) afflict(me, target, ailment, taken, spell) else null
    }
    return rolled + forced
}

/**
 * [ailment] on [target] by [me], from a blow that dealt [taken]: a damage over time is a share of the
 * damage of its own type — of the whole blow when it had none — the rest are the rule's magnitude.
 */
internal fun Battle.afflict(me: Fighter, target: Fighter, ailment: Ailment, taken: Map<DamageType, Double>, spell: Boolean = false): Ailment? {
    val (rule, type) = ruleOf[ailment] ?: return null
    if (target.body.immune(ailment)) return null
    val avoid = target.body.avoid(ailment)
    if (avoid > 0 && draw(RollKey.AVOID, avoid, ailment) < avoid) return null
    val amount = (taken[type] ?: 0.0).takeIf { it > 0 } ?: taken.values.sum()
    // 3.35.0: a faster damaging ailment deals the same in less time; the striker's shock and chill are stronger by their effect.
    val faster = if (ailment.hurts) 1 + me.body.fasterAilments else 1.0
    val duration = rule.duration * target.body.ailmentDuration(ailment) * me.body.ailmentDurationOnFoes(ailment) / faster
    val magnitude = if (ailment.hurts) {
        amount * rule.magnitude / 100 / rule.duration * me.body.ailmentDamage(ailment) * faster
    } else {
        rule.magnitude * when (ailment) {
            Ailment.SHOCKED -> me.body.shockEffect
            Ailment.CHILLED -> me.body.chillEffect
            else -> 1.0
        }
    }
    place(
        target,
        ActiveAilment(
            ailment,
            time + duration,
            magnitude,
            duration,
            me.side,
            me.index.coerceAtLeast(0),
            spell,
            chaos = ailment == Ailment.BURNING && me.body.igniteAsChaos,
        ),
        rule.stacks,
    )
    if (target === heroFighter) powers.fire(PowerEvent.AILED, PowerMoment(me, taken, ailment = ailment))
    return ailment
}

/** An ailment laid on: a stacking one adds up, the others keep the strongest of their kind and refresh how long it lasts. */
internal fun Battle.place(target: Fighter, fresh: ActiveAilment, stacks: Boolean) {
    val ailment = fresh.ailment
    if (ailment.hurts) target.tickedAt.putIfAbsent(ailment, time)
    val existing = target.ailments.filter { it.ailment == ailment }
    when {
        stacks || existing.isEmpty() -> target.ailments += fresh

        existing.maxOf { it.magnitude } <= fresh.magnitude -> {
            target.ailments.removeAll(existing)
            target.ailments += fresh
        }

        else -> {
            val kept = existing.maxBy { it.magnitude }
            target.ailments.remove(kept)
            target.ailments += kept.copy(until = max(kept.until, fresh.until))
        }
    }
}

/** A foe down: a kill to report, life and mana on kill and the flasks' charges for the hero (2.78.0), its aura lifted, and a focus on it let go. */
internal fun Battle.fell(fighter: Fighter, spell: Boolean = false, killer: Fighter? = null) {
    if (fighter.side != Side.MONSTER || fighter.index in fallenOrder) return
    val ailing = fighter.ailments.toList()
    fighter.ailments.clear()
    fighter.effects.clear()
    fallenOrder += fighter.index
    if (focus == fighter.index) focus = null
    if (lastStriker == fighter.index) lastStriker = null
    val stepped = stepIn()
    if (fighter.body.auras.isNotEmpty() || stepped) remake(heroFighter)
    val hero = heroFighter
    if (!hero.alive) return
    val lifeBefore = hero.life
    hero.life = min(hero.body.maxLife, hero.life + (hero.body.lifeOnKill + hero.body.maxLife * hero.body.lifeOnKillShare) * hero.body.recoveryRate)
    note(fighter, NoteKind.KILL, "", hero.life - lifeBefore)
    hero.mana = min(manaCap(), hero.mana + hero.body.manaOnKill)
    hero.shield = min(hero.body.maxShield, hero.shield + hero.body.shieldOnKill)
    killedAt = time
    BuffKind.entries.filterNot { it.onHit }.forEach(::chanceBuff)
    val rarity = foes[fighter.index].rarity
    val base = (rules.flasks.perKill[rarity] ?: 1.0) + if (rarity >= MonsterRarity.RARE) hero.body[AtlasStat.FLASK_RARE.code] else 0.0
    kit.flasks.forEachIndexed { i, flask ->
        flask ?: return@forEachIndexed
        charges[i] = min(flask.maxCharges, charges[i] + flask.gained(base, hero.body))
        // A lingering draught runs on for every kill made while it runs.
        val longer = flask.own("FLASK_DURATION_PER_KILL")
        if (longer > 0) draughtOf(i)?.let { running -> hero.effects[hero.effects.indexOf(running)] = running.copy(until = running.until + longer) }
    }
    trigger(SkillEvent.KILL)
    if (spell) trigger(SkillEvent.SPELL_KILL)
    powers.killed(PowerMoment(fighter, spell = spell, ailments = ailing))
    if (killer != null && killer === allyFighter) powers.fire(PowerEvent.PET_KILL, PowerMoment(fighter, spell = spell, ailments = ailing))
    lastWords(fighter)
}
