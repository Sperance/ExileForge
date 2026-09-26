package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import com.sperance.exileforge.core.model.powers.Power
import com.sperance.exileforge.core.model.powers.PowerAct
import com.sperance.exileforge.core.model.powers.PowerBase
import com.sperance.exileforge.core.model.powers.PowerBook
import com.sperance.exileforge.core.model.powers.PowerCheck
import com.sperance.exileforge.core.model.powers.PowerCheckKind
import com.sperance.exileforge.core.model.powers.PowerEffect
import com.sperance.exileforge.core.model.powers.PowerEvent
import com.sperance.exileforge.core.model.powers.PowerLine
import com.sperance.exileforge.core.model.powers.PowerScale
import com.sperance.exileforge.core.model.powers.PowerTarget
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * What a power's event was about (2.79.0): the foe struck or striking, what the blow did, whether a
 * spell's, the ailment laid, and a fallen foe's ailments before they were cleared.
 */
internal class PowerMoment(
    val target: Battle.Fighter? = null,
    val taken: Map<DamageType, Double> = emptyMap(),
    val spell: Boolean = false,
    val ailment: Ailment? = null,
    val ailments: List<ActiveAilment> = emptyList(),
) {
    val damage: Double get() = taken.values.sum()
}

/**
 * The hero's powers in a fight (2.79.0, server 0.70.0): the book's answers to the fight's events, read
 * off the hero's body as it stands, so a flask's power works while its draught runs and no longer.
 *
 * A power never answers a power: what one strikes or lays sets off no other. Chances are drawn only
 * for a power the hero carries, so a fight without any plays its seed as before.
 */
internal class PowerRunner(private val battle: Battle, book: PowerBook) {
    private val byEvent: Map<PowerEvent, List<Power>> = book.powers.filter { it.on != null }.groupBy { it.on!! }
    private val standingPowers = byEvent[PowerEvent.STANDING].orEmpty()
    private val readyAt = mutableMapOf<String, Double>()
    private val counts = mutableMapOf<String, Int>()
    private val stackCount = mutableMapOf<String, Int>()
    private val lastBeat = mutableMapOf<String, Double>()
    private var busy = false
    private var started = false

    /** Kills of this fight, and the last blow the hero dealt and took. */
    var kills = 0
        private set
    private var lastTaken = 0.0

    /** The lines the standing powers lay on the hero right now. */
    var standing: List<StatLine> = emptyList()
        private set

    private val hero get() = battle.heroFighter
    private fun value(power: Power) = hero.body[power.stat]

    /** One slice has passed: the fight's opening, the powers on a beat. */
    fun tick() {
        if (!started) { started = true; fire(PowerEvent.FIGHT_START) }
        byEvent[PowerEvent.EVERY]?.forEach { power ->
            if (value(power) == 0.0) return@forEach
            val last = lastBeat.getOrPut(power.stat) { battle.time }
            if (battle.time - last >= power.every - 1e-9) { lastBeat[power.stat] = battle.time; run(power, PowerMoment()) }
        }
    }

    fun dealt(moment: PowerMoment, crit: Boolean, stunned: Boolean, inflicted: List<Ailment>) {
        fire(PowerEvent.HIT, moment)
        if (crit) fire(PowerEvent.CRIT, moment)
        if (stunned) fire(PowerEvent.STUN, moment)
        inflicted.forEach { fire(PowerEvent.INFLICT, PowerMoment(moment.target, moment.taken, moment.spell, it)) }
    }

    fun taken(moment: PowerMoment, crit: Boolean) {
        lastTaken = moment.damage
        fire(PowerEvent.HIT_TAKEN, moment)
        if (crit) fire(PowerEvent.CRIT_TAKEN, moment)
    }

    fun killed(moment: PowerMoment) { kills++; fire(PowerEvent.KILL, moment) }

    /** Whether the standing lines changed since the last look: the hero's body is made again when they do. */
    fun restand(): Boolean {
        if (standingPowers.isEmpty()) return false
        val lines = standingPowers.filter { value(it) != 0.0 && it.checks.all { check -> holds(check, it, PowerMoment(battle.target())) } }
            .flatMap { power -> power.effects.flatMap { effect -> effect.lines.map { line(power, it) } } }
        if (lines == standing) return false
        standing = lines
        return true
    }

    fun fire(event: PowerEvent, moment: PowerMoment = PowerMoment()) {
        if (busy || battle.outcome != null || (!hero.alive && event != PowerEvent.DEATH)) return
        byEvent[event]?.forEach { power -> if (value(power) != 0.0) run(power, moment) }
    }

    private fun run(power: Power, moment: PowerMoment) {
        val value = value(power)
        if (battle.time < (readyAt[power.stat] ?: -1.0)) return
        if (!power.checks.filter { it.check != PowerCheckKind.NTH }.all { holds(it, power, moment) }) return
        power.checks.firstOrNull { it.check == PowerCheckKind.NTH }?.let { nth ->
            val count = (counts[power.stat] ?: 0) + 1
            counts[power.stat] = count
            if (count % max(1, nth.value.toInt()) != 0) return
        }
        val chance = power.chance(value)
        if (chance < 100 && battle.random.nextDouble() * 100 >= chance) return
        if (power.cooldown != 0.0) readyAt[power.stat] = if (power.cooldown < 0) Double.MAX_VALUE else battle.time + power.cooldown
        busy = true
        try {
            var healed = 0.0
            var shown = false
            power.effects.forEach { effect ->
                healed += act(power, effect, value, moment)
                shown = shown || effect.act !in SILENT
            }
            if (shown && power.on !in QUIET) battle.powerShown(power.stat, healed)
        } finally { busy = false }
    }

    private fun line(power: Power, line: PowerLine, times: Int = 1): StatLine {
        val base = line.value ?: power.amount(null, value(power))
        val count = line.scale?.let(::count)?.let { if (line.cap > 0) min(it, line.cap) else it } ?: 1.0
        return StatLine(line.stat, ModifierOperation.valueOf(line.operation), base * count * times)
    }

    /** A count of the fight a line grows with. */
    private fun count(scale: PowerScale): Double = when (scale) {
        PowerScale.FOES -> battle.foeFighters.count { it.alive }.toDouble()
        PowerScale.MISSING_LIFE -> floor((1 - hero.life / hero.body.maxLife) * 10)
        PowerScale.SECONDS -> floor(battle.time)
        PowerScale.KILLS -> kills.toDouble()
        PowerScale.CURSED_FOES -> battle.foeFighters.count { it.alive && it.cursed }.toDouble()
        PowerScale.AILED_FOES -> battle.foeFighters.count { it.alive && it.ailments.isNotEmpty() }.toDouble()
        PowerScale.SHIELD -> if (hero.body.maxShield > 0) floor(hero.shield / hero.body.maxShield * 10) else 0.0
        PowerScale.MANA -> battle.manaCap().takeIf { it > 0 }?.let { floor(hero.mana / it * 10) } ?: 0.0
        PowerScale.CHARGES -> battle.flaskCharges()
    }

    private fun holds(check: PowerCheck, power: Power, moment: PowerMoment): Boolean {
        val target = moment.target?.takeIf { it.side == Side.MONSTER }
        val share = check.value / 100
        return when (check.check) {
            PowerCheckKind.LIFE_BELOW -> hero.life < hero.body.maxLife * share
            PowerCheckKind.LIFE_ABOVE -> hero.life > hero.body.maxLife * share
            PowerCheckKind.LIFE_FULL -> hero.life >= hero.body.maxLife - 0.5
            PowerCheckKind.SHIELD_FULL -> hero.body.maxShield > 0 && hero.shield >= hero.body.maxShield - 0.5
            PowerCheckKind.SHIELD_EMPTY -> hero.shield <= 0.5
            PowerCheckKind.MANA_BELOW -> hero.mana < battle.manaCap() * share
            PowerCheckKind.MANA_ABOVE -> hero.mana > battle.manaCap() * share
            PowerCheckKind.FOES_AT_LEAST -> battle.foeFighters.count { it.alive } >= check.value
            PowerCheckKind.FOES_AT_MOST -> battle.foeFighters.count { it.alive } <= check.value
            PowerCheckKind.TARGET_RARE -> target != null && battle.foes[target.index].rarity >= MonsterRarity.RARE
            PowerCheckKind.TARGET_AILED -> target != null && (target.ailments.isNotEmpty() || moment.ailments.isNotEmpty())
            PowerCheckKind.TARGET_AILMENT -> target != null && (target.ailments + moment.ailments).any { it.ailment.word == check.word }
            PowerCheckKind.TARGET_CURSED -> target?.cursed == true
            PowerCheckKind.TARGET_LIFE_BELOW -> target != null && target.life < target.body.maxLife * share
            PowerCheckKind.TARGET_LIFE_ABOVE -> target != null && target.life > target.body.maxLife * share
            PowerCheckKind.FIGHT_BEFORE -> battle.time < check.value
            PowerCheckKind.FIGHT_AFTER -> battle.time >= check.value
            PowerCheckKind.SELF_AILED -> hero.ailments.isNotEmpty()
            PowerCheckKind.SELF_CLEAN -> hero.ailments.isEmpty()
            PowerCheckKind.FLASK_RUNNING -> hero.effects.any { it.kind == EffectKind.FLASK && it.until > battle.time }
            PowerCheckKind.BARRIER_UP -> hero.barrier > 0
            PowerCheckKind.NTH -> true
            PowerCheckKind.SPELL -> moment.spell
            PowerCheckKind.ATTACK -> !moment.spell
            PowerCheckKind.DAMAGE_TYPE -> moment.taken.filterValues { it > 0 }.maxByOrNull { it.value }?.key?.name == check.word
            PowerCheckKind.AILMENT -> moment.ailment?.word == check.word
        }
    }

    /** Whom an effect reaches: the foe of the moment, every foe, one at random, the others, or none — the hero. */
    private fun targets(to: PowerTarget, moment: PowerMoment): List<Battle.Fighter> {
        val alive = battle.foeFighters.filter { it.alive }
        val focus = moment.target?.takeIf { it.side == Side.MONSTER && it.alive } ?: battle.target()
        return when (to) {
            PowerTarget.TARGET -> listOfNotNull(focus)
            PowerTarget.ALL -> alive
            PowerTarget.RANDOM -> if (alive.isEmpty()) emptyList() else listOf(alive[battle.random.nextInt(alive.size)])
            PowerTarget.OTHERS -> alive - setOfNotNull(moment.target)
            PowerTarget.SELF -> emptyList()
        }
    }

    /** What [effect] does; the life it gave back, for the log. */
    private fun act(power: Power, effect: PowerEffect, value: Double, moment: PowerMoment): Double {
        val amount = power.amount(effect.amount, value)
        val duration = power.duration(effect.duration, value)
        val body = hero.body
        when (effect.act) {
            PowerAct.BUFF -> {
                val stacks = stacksOf(hero, power.stat, EffectKind.BUFF, effect.stacks)
                battle.lay(hero, TimedEffect(EffectKind.BUFF, power.stat, effect.lines.map { line(power, it, stacks) }, battle.time + duration, duration))
            }
            PowerAct.HEX -> targets(effect.to, moment).forEach { foe ->
                val stacks = stacksOf(foe, power.stat, EffectKind.CURSE, effect.stacks)
                battle.lay(foe, TimedEffect(EffectKind.CURSE, power.stat, effect.lines.map { line(power, it, stacks) }, battle.time + duration, duration))
            }
            PowerAct.HEAL -> return when (effect.of) {
                PowerBase.MANA -> { hero.mana = min(battle.manaCap(), hero.mana + battle.manaCap() * amount / 100); 0.0 }
                PowerBase.SHIELD -> { hero.shield = min(body.maxShield, hero.shield + body.maxShield * amount / 100); 0.0 }
                PowerBase.DEALT -> battle.restore(moment.damage * amount / 100 * body.recoveryRate)
                PowerBase.TAKEN -> battle.restore(lastTaken * amount / 100 * body.recoveryRate)
                else -> battle.restore(body.maxLife * amount / 100)
            }
            PowerAct.HURT -> when (effect.of) {
                PowerBase.MANA -> hero.mana = max(0.0, hero.mana - battle.manaCap() * amount / 100)
                PowerBase.SHIELD -> hero.shield = max(0.0, hero.shield - body.maxShield * amount / 100)
                else -> hero.life = max(1.0, hero.life - body.maxLife * amount / 100)
            }
            PowerAct.BARRIER -> {
                hero.barrier = max(hero.barrier, body.maxLife * amount / 100)
                hero.barrierUntil = battle.time + duration
            }
            PowerAct.DAMAGE -> {
                var dealt = 0.0
                targets(effect.to, moment).forEach { foe ->
                    val damage = damage(effect, amount, moment, foe)
                    if (damage.values.sum() <= 0) return@forEach
                    dealt += damage.values.sum()
                    battle.strike(hero, foe, Blow(damage, Action.SKILL, spell = effect.of != PowerBase.WEAPON, skill = power.stat, spread = effect.of == PowerBase.WEAPON))
                }
                if (effect.leech > 0) return battle.restore(dealt * effect.leech / 100 * body.recoveryRate)
            }
            PowerAct.AILMENT -> {
                val ailment = Ailment.byWord(effect.ailment.orEmpty()) ?: Ailment.of(effect.ailment.orEmpty()) ?: return 0.0
                targets(effect.to, moment).forEach { foe -> battle.afflict(hero, foe, ailment, moment.taken.ifEmpty { body.damage }) }
            }
            PowerAct.CURSE -> targets(effect.to, moment).forEach(battle::hex)
            PowerAct.EXECUTE -> targets(effect.to, moment).filter { battle.foes[it.index].rarity < MonsterRarity.UNIQUE && it.life < it.body.maxLife * amount / 100 }
                .forEach(battle::slay)
            PowerAct.STUN -> targets(effect.to, moment).forEach { it.heldUntil = max(it.heldUntil, battle.time + duration) }
            PowerAct.DELAY -> targets(effect.to, moment).forEach { it.nextAttack += duration }
            PowerAct.RUSH -> hero.nextAttack = min(hero.nextAttack, battle.time)
            PowerAct.CHARGES -> battle.chargeFlasks(amount)
            PowerAct.COOLDOWNS -> hero.readyAt.replaceAll { _, at -> at - amount }
            PowerAct.NEXT_CRIT -> battle.nextCrit = true
            PowerAct.INVULNERABLE -> hero.invulnerableUntil = max(hero.invulnerableUntil, battle.time + duration)
            PowerAct.CLEANSE -> hero.ailments.clear()
            PowerAct.SPREAD -> targets(PowerTarget.OTHERS, moment).forEach { foe ->
                moment.ailments.forEach { spread -> battle.place(foe, spread.copy(until = battle.time + spread.duration), stacks = spread.ailment.hurts) }
            }
        }
        return 0.0
    }

    /** A blow's damage before defences: a share of the weapon, of a pool or defence, of the last blow, or of the foe's own life. */
    private fun damage(effect: PowerEffect, amount: Double, moment: PowerMoment, foe: Battle.Fighter): Map<DamageType, Double> {
        val body = hero.body
        val share = amount / 100
        val type = DamageType.element(effect.type)
        if (effect.of == PowerBase.WEAPON) {
            val weapon = body.damage.mapValues { it.value * share }
            return if (type == null) weapon else mapOf(type to weapon.values.sum())
        }
        val base = when (effect.of) {
            PowerBase.LIFE -> body.maxLife
            PowerBase.SHIELD -> body.maxShield
            PowerBase.MANA -> body.maxMana
            PowerBase.ARMOUR -> body.armour
            PowerBase.EVASION -> body.evasion
            PowerBase.TAKEN -> lastTaken
            PowerBase.DEALT -> moment.damage
            // The foe of the moment's life — the one struck, or the one that fell — a fifth of it for a boss.
            PowerBase.TARGET_LIFE -> (moment.target?.takeIf { it.side == Side.MONSTER } ?: foe).let { of ->
                of.body.maxLife * if (battle.foes[of.index].rarity >= MonsterRarity.UNIQUE) 0.2 else 1.0
            }
            PowerBase.STRENGTH -> body["STOCK_STRENGTH"]
            PowerBase.AGILITY -> body["STOCK_AGILITY"]
            PowerBase.INTELLECT -> body["STOCK_INTELLECT"]
            PowerBase.WEAPON -> 0.0
        }
        return mapOf((type ?: DamageType.PHYSICAL) to base * share)
    }

    /** How many times a power's effect lies on [fighter] with this one: one more while it still lies, up to [most]. */
    private fun stacksOf(fighter: Battle.Fighter, source: String, kind: EffectKind, most: Int): Int {
        val key = "$source#${fighter.index}"
        val lying = fighter.effects.any { it.kind == kind && it.source == source && it.until > battle.time }
        return (if (lying) min(max(1, most), (stackCount[key] ?: 0) + 1) else 1).also { stackCount[key] = it }
    }

    private companion object {
        /** Effects that show nothing of their own in the log: a blow logs itself, a curse and an ailment land on the foe's line. */
        val SILENT = setOf(PowerAct.DAMAGE, PowerAct.AILMENT, PowerAct.CURSE, PowerAct.SPREAD, PowerAct.DELAY, PowerAct.STUN)
        /** Events too frequent to log a line each. */
        val QUIET = setOf(PowerEvent.HIT, PowerEvent.HIT_TAKEN, PowerEvent.STANDING, PowerEvent.INFLICT)
    }
}
