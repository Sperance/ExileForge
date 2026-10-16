package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.ActiveAilment
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Blow
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.EffectKind
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.TimedEffect
import com.sperance.exileforge.core.campaign.combat.afflict
import com.sperance.exileforge.core.campaign.combat.afflictSelf
import com.sperance.exileforge.core.campaign.combat.chargeFlasks
import com.sperance.exileforge.core.campaign.combat.consumeCharges
import com.sperance.exileforge.core.campaign.combat.echoCast
import com.sperance.exileforge.core.campaign.combat.enemies
import com.sperance.exileforge.core.campaign.combat.flaskCharges
import com.sperance.exileforge.core.campaign.combat.gainCharges
import com.sperance.exileforge.core.campaign.combat.hex
import com.sperance.exileforge.core.campaign.combat.lay
import com.sperance.exileforge.core.campaign.combat.note
import com.sperance.exileforge.core.campaign.combat.place
import com.sperance.exileforge.core.campaign.combat.powerShown
import com.sperance.exileforge.core.campaign.combat.remake
import com.sperance.exileforge.core.campaign.combat.restore
import com.sperance.exileforge.core.campaign.combat.slay
import com.sperance.exileforge.core.campaign.combat.strike
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Power
import com.sperance.exileforge.rules.content.PowerAct
import com.sperance.exileforge.rules.content.PowerBase
import com.sperance.exileforge.rules.content.PowerBook
import com.sperance.exileforge.rules.content.PowerCheck
import com.sperance.exileforge.rules.content.PowerCheckKind
import com.sperance.exileforge.rules.content.PowerEffect
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.PowerLine
import com.sperance.exileforge.rules.content.PowerScale
import com.sperance.exileforge.rules.content.PowerTarget
import com.sperance.exileforge.rules.content.RecoveryPool
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * What a power's event was about (2.79.0): the foe struck or striking, what the blow did, whether a
 * spell's, the ailment laid, and a fallen foe's ailments before they were cleared. С 4.4.0 у `SKILL_USE` - умение [cast]
 * на уровне [level], что сила `RECAST` повторяет. С 4.12.x (пакет 7): [cursed] - павший был проклят до снятия эффектов
 * (STAT-PW2-02), [slot] - слот только что применённого умения (STAT-PW2-08), [flask] - характеристики, что кладёт выпитая
 * фляга (STAT-PW2-11).
 */
internal class PowerMoment(
    val target: Battle.Fighter? = null,
    val taken: Map<DamageType, Double> = emptyMap(),
    val spell: Boolean = false,
    val ailment: Ailment? = null,
    val ailments: List<ActiveAilment> = emptyList(),
    val cast: KitSkill? = null,
    val level: Int = 1,
    val cursed: Boolean = false,
    val slot: String? = null,
    val flask: Set<String>? = null,
) {
    val damage: Double get() = taken.values.sum()
}

/** A hit to strike again (3.32.0): its foe, the damage, at [at]; dropped if the foe or the fight is gone by then. */
private class Echo(val at: Double, val foe: Battle.Fighter, val damage: Map<DamageType, Double>, val spell: Boolean, val skill: String)

/**
 * The hero's powers in a fight (2.79.0, server 0.70.0): the book's answers to the fight's events, read
 * off the hero's body as it stands, so a flask's power works while its draught runs and no longer.
 *
 * A power never answers a power: what one strikes or lays sets off no other. Chances are drawn only
 * for a power the hero carries, so a fight without any plays its seed as before.
 */

/** The roll [value] where a power puts it, or the effect's own number. */
private fun Power.amount(own: Double?, value: Double): Double = own ?: if (roll == com.sperance.exileforge.rules.content.PowerRoll.AMOUNT) value else 0.0
private fun Power.duration(own: Double?, value: Double): Double = own ?: if (roll == com.sperance.exileforge.rules.content.PowerRoll.DURATION) value else 0.0
private fun Power.chance(value: Double): Double = chance ?: if (roll == com.sperance.exileforge.rules.content.PowerRoll.CHANCE) value else 100.0

/**
 * Силы героя в бою. [carried] (4.12.x, STAT-PW2-05) - счётчики «каждое N-е», принесённые из прошлых боёв захода или забега
 * испытания: счёт идёт дальше, а не с нуля ([tally] отдаёт его следующему бою).
 */
internal class PowerRunner(private val battle: Battle, private val book: PowerBook, private val cleared: Boolean = false, carried: Map<String, Int> = emptyMap()) {
    private val byEvent: Map<PowerEvent, List<Power>> = book.powers.filter { it.on != null }.groupBy { it.on!! }
    private val standingPowers = byEvent[PowerEvent.STANDING].orEmpty()
    private val readyAt = mutableMapOf<String, Double>()
    private val counts = carried.toMutableMap()

    /** Счётчики «каждое N-е» на конце боя - их несёт следующий бой захода. */
    val tally: Map<String, Int> get() = counts.toMap()
    private val stackCount = mutableMapOf<String, Int>()
    private val lastBeat = mutableMapOf<String, Double>()
    private var busy = false
    private var started = false

    /** Возрождение этого боя потрачено (0.15.0, реш. 7): одно на бой на все силы смерти. */
    private var revived = false

    /** Сколько процентов удара уже вернули силы ответного урона на принятый сейчас удар (0.15.0, реш. 14: сумма - под `caps.retaliate`). */
    private var retaliated = 0.0
    private val echoes = mutableListOf<Echo>()

    /** Whether a standing line grows with [momentum]: only then does a hit make the hero's body again. */
    private val momentous = standingPowers.any { power -> power.effects.any { effect -> effect.lines.any { it.scale == PowerScale.MOMENTUM } } }

    /** The hero's hits in a row on one foe (3.32.0): another foe starts it over. */
    var momentum = 0
        private set
    private var momentumOn: Battle.Fighter? = null

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
        if (!started) {
            started = true
            if (cleared) fire(PowerEvent.FIGHT_CLEAR)
            fire(PowerEvent.FIGHT_START)
        }
        echo()
        byEvent[PowerEvent.EVERY]?.forEach { power ->
            if (value(power) == 0.0) return@forEach
            val last = lastBeat.getOrPut(power.stat) { battle.time }
            if (battle.time - last >= power.every - 1e-9) {
                lastBeat[power.stat] = battle.time
                run(power, PowerMoment())
            }
        }
    }

    /**
     * The hero's hit landed; whether it moved a standing line's momentum, so the body must be made again.
     * Only a [primary] target counts: an area blow's other foes leave the streak as it was.
     */
    fun dealt(moment: PowerMoment, crit: Boolean, stunned: Boolean, inflicted: List<Ailment>, primary: Boolean = true): Boolean {
        val moved = !busy && primary && moment.target != null
        if (moved) build(moment.target!!)
        fire(PowerEvent.HIT, moment)
        if (crit) fire(PowerEvent.CRIT, moment)
        if (stunned) fire(PowerEvent.STUN, moment)
        inflicted.forEach { fire(PowerEvent.INFLICT, PowerMoment(moment.target, moment.taken, moment.spell, it)) }
        return moved && momentous
    }

    /** One more hit in a row on [foe], or the first on a new one; a power's own blows count for nothing. */
    private fun build(foe: Battle.Fighter) {
        if (momentumOn != null && momentumOn !== foe) momentum = 0
        momentumOn = foe
        momentum++
    }

    /** The hits due to strike again: each once, on its foe if it still stands, answering nothing. */
    private fun echo() {
        if (echoes.isEmpty()) return
        val due = echoes.filter { it.at <= battle.time + 1e-9 }
        if (due.isEmpty()) return
        echoes.removeAll(due)
        due.forEach { echo ->
            if (battle.outcome != null || !hero.alive || !echo.foe.alive) return@forEach
            busy = true
            try {
                battle.strike(hero, echo.foe, Blow(echo.damage, Action.SKILL, spell = echo.spell, skill = echo.skill, spread = false))
            } finally {
                busy = false
            }
        }
    }

    fun taken(moment: PowerMoment, crit: Boolean) {
        lastTaken = moment.damage
        retaliated = 0.0
        fire(PowerEvent.HIT_TAKEN, moment)
        if (crit) fire(PowerEvent.CRIT_TAKEN, moment)
    }

    fun killed(moment: PowerMoment) {
        kills++
        fire(PowerEvent.KILL, moment)
    }

    /** Whether the standing lines changed since the last look: the hero's body is made again when they do. */
    fun restand(): Boolean {
        if (standingPowers.isEmpty()) return false
        val lines = standingPowers.filter { value(it) != 0.0 && it.checks.all { check -> holds(check, it, PowerMoment(battle.target())) } }
            .flatMap { power -> power.effects.flatMap { effect -> effect.lines.map { line(power, it, draw = false) } } }
        if (lines == standing) return false
        standing = lines
        return true
    }

    fun fire(event: PowerEvent, moment: PowerMoment = PowerMoment()) {
        if (busy || battle.outcome != null || (!hero.alive && event != PowerEvent.DEATH)) return
        if (event == PowerEvent.DEATH) return revive(moment)
        byEvent[event]?.forEach { power -> if (value(power) != 0.0) run(power, moment) }
    }

    /**
     * Смерть героя (0.15.0, реш. 7): возрождение одно на бой на все силы смерти - срабатывает только сильнейшая по возвращаемому
     * здоровью ([PowerBook.revival] правил); после неё до конца боя ни одна сила смерти не срабатывает.
     */
    private fun revive(moment: PowerMoment) {
        if (revived) return
        val power = book.revival { hero.body[it] } ?: return
        revived = true
        run(power, moment)
    }

    private fun run(power: Power, moment: PowerMoment) {
        val value = value(power)
        if (battle.time < (readyAt[power.stat] ?: -1.0)) return
        if (!ownSip(power, moment)) return
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
            if (shown && power.on !in QUIET) {
                battle.powerShown(power.stat, healed)
            } // 3.37.0: a power that fired without a line of its own is a note the log's «events» show.
            else {
                battle.note(battle.heroFighter, NoteKind.POWER, power.stat, value)
            }
        } finally {
            busy = false
        }
    }

    /**
     * Сила глотка на уникальной фляге (4.12.x, STAT-PW2-11) отвечает только на глоток своей фляги: пока её строки лежат на герое,
     * глоток другой фляги её не будит. Та же сила с прочих вещей отвечает на любой глоток.
     */
    private fun ownSip(power: Power, moment: PowerMoment): Boolean {
        val drunk = moment.flask ?: return true
        if (power.stat in drunk) return true
        return hero.effects.none { it.kind == EffectKind.FLASK && it.until > battle.time && it.lines.any { line -> line.stat == power.stat } }
    }

    private fun line(power: Power, line: PowerLine, times: Int = 1, draw: Boolean = true): StatLine {
        // A spread line (server 1.32.0) draws its value between `value` and `upto` as it is laid; a standing one keeps its low end.
        val base = line.value?.let { low -> line.upto?.takeIf { draw }?.let { high -> low + battle.random.nextDouble() * (high - low).coerceAtLeast(0.0) } ?: low }
            ?: power.amount(null, value(power))
        val count = line.scale?.let(::count)?.let { if (line.cap > 0) min(it, line.cap) else it } ?: 1.0
        return StatLine(line.stat, line.op, base * count * times)
    }

    /** A count of the fight a line grows with; доли - целыми шагами своего счёта ([PowerScale.steps], его же называет текст силы). */
    private fun count(scale: PowerScale): Double = when (scale) {
        PowerScale.FOES -> battle.enemies().toDouble()
        PowerScale.MISSING_LIFE -> scale.steps(1 - hero.life / hero.body.maxLife)
        PowerScale.SECONDS -> floor(battle.time)
        PowerScale.KILLS -> kills.toDouble()
        PowerScale.CURSED_FOES -> battle.foeFighters.count { it.alive && it.cursed }.toDouble()
        PowerScale.AILED_FOES -> battle.foeFighters.count { it.alive && it.ailments.isNotEmpty() }.toDouble()
        PowerScale.SHIELD -> if (hero.body.maxShield > 0) scale.steps(hero.shield / hero.body.maxShield) else 0.0
        PowerScale.MANA -> battle.manaCap().takeIf { it > 0 }?.let { scale.steps(hero.mana / it) } ?: 0.0
        PowerScale.CHARGES -> battle.flaskCharges()
        PowerScale.MOMENTUM -> momentum.toDouble()
        PowerScale.FRENZY_CHARGES -> battle.heroCharges.count(ChargeKind.FRENZY).toDouble()
        PowerScale.POWER_CHARGES -> battle.heroCharges.count(ChargeKind.POWER).toDouble()
        PowerScale.ENDURANCE_CHARGES -> battle.heroCharges.count(ChargeKind.ENDURANCE).toDouble()
        PowerScale.TOTEMS -> battle.totems.size.toDouble()
    }

    /** What an effect's number is multiplied by (server 1.32.0): its scale's count at this moment, under its cap; one without a scale. */
    private fun scaled(effect: PowerEffect): Double = effect.scale?.let(::count)?.let { if (effect.cap > 0) min(it, effect.cap) else it } ?: 1.0

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
            PowerCheckKind.FOES_AT_LEAST -> battle.enemies() >= check.value
            PowerCheckKind.FOES_AT_MOST -> battle.enemies() <= check.value
            PowerCheckKind.TARGET_RARE -> target != null && battle.foes[target.index].rarity >= MonsterRarity.RARE
            PowerCheckKind.TARGET_BLIGHTED -> target != null && battle.foes[target.index].tainted
            PowerCheckKind.TARGET_AILED -> target != null && (target.ailments.isNotEmpty() || moment.ailments.isNotEmpty())
            PowerCheckKind.TARGET_AILMENT -> target != null && (target.ailments + moment.ailments).any { it.ailment.word == check.word }
            PowerCheckKind.TARGET_CURSED -> target != null && (target.cursed || moment.cursed)
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
            PowerCheckKind.CHARGES_AT_LEAST -> ChargeKind.of(check.word)?.let { battle.heroCharges.count(it) >= check.value } ?: false
        }
    }

    /** Whom an effect reaches: the foe of the moment, every foe, one at random, the others, or none — the hero, each act's own. */
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
        val amount = power.amount(effect.amount, value) * scaled(effect)
        val duration = power.duration(effect.duration, value)
        val body = hero.body
        when (effect.act) {
            // Скорость передвижения бафф несёт на карту остатком времени (4.12.x, STAT-PW1-04): в бою она ничего не меняет
            PowerAct.BUFF -> {
                if (effect.lines.isEmpty()) return 0.0
                val stacks = stacksOf(hero, power.stat, EffectKind.BUFF, effect.stacks)
                battle.lay(hero, TimedEffect(EffectKind.BUFF, power.stat, effect.lines.map { line(power, it, stacks) }, battle.time + duration, duration, stacks = stacks))
            }

            PowerAct.HEX -> targets(effect.to, moment).forEach { foe ->
                val stacks = stacksOf(foe, power.stat, EffectKind.CURSE, effect.stacks)
                battle.lay(foe, TimedEffect(EffectKind.CURSE, power.stat, effect.lines.map { line(power, it, stacks) }, battle.time + duration, duration, stacks = stacks))
            }

            // Лечение сил (4.12.x, COMBAT-01, STAT-PW1-10) читает `Recovery`: здоровье - долей здоровья (запрет, Изранение), без потолка
            // вампиризма; мана - долей маны (Иссушение); щит не режется
            PowerAct.HEAL -> return when (effect.of) {
                PowerBase.MANA -> {
                    hero.mana = min(battle.manaCap(), hero.mana + battle.manaCap() * amount / 100 * hero.share(RecoveryPool.MANA))
                    0.0
                }

                PowerBase.SHIELD -> {
                    hero.shield = min(body.maxShield, hero.shield + body.maxShield * amount / 100)
                    0.0
                }

                PowerBase.DEALT -> battle.restore(moment.damage * amount / 100 * body.recoveryRate * hero.share(RecoveryPool.LIFE))

                PowerBase.TAKEN -> battle.restore(lastTaken * amount / 100 * body.recoveryRate * hero.share(RecoveryPool.LIFE))

                else -> battle.restore(body.maxLife * amount / 100 * hero.share(RecoveryPool.LIFE))
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
                if (effect.leech > 0) return battle.restore(dealt * effect.leech / 100 * body.recoveryRate * hero.share(RecoveryPool.LIFE))
            }

            PowerAct.AILMENT -> {
                val ailment = Ailment.byWord(effect.ailment.orEmpty()) ?: Ailment.of(effect.ailment.orEmpty()) ?: return 0.0
                // On the hero themselves (server 1.32.0): a self-ignition burns the rule's share of their life a second.
                if (effect.to == PowerTarget.SELF) {
                    battle.afflictSelf(ailment, duration)
                    return 0.0
                }
                targets(effect.to, moment).forEach { foe -> battle.afflict(hero, foe, ailment, moment.taken.ifEmpty { body.damage }) }
            }

            PowerAct.CURSE -> targets(effect.to, moment).forEach(battle::hex)

            PowerAct.EXECUTE ->
                targets(effect.to, moment).filter { !it.chief && it.life < it.body.maxLife * amount / 100 }
                    .forEach(battle::slay)

            PowerAct.STUN -> targets(effect.to, moment).forEach { it.heldUntil = max(it.heldUntil, battle.time + duration) }

            PowerAct.DELAY -> targets(effect.to, moment).forEach { it.nextAttack += duration }

            // Рывок (4.12.x, STAT-PW1-01) переживает замах, в котором сработал: следующий удар - сразу
            PowerAct.RUSH -> {
                hero.nextAttack = min(hero.nextAttack, battle.time)
                hero.rushed = true
            }

            PowerAct.CHARGES -> battle.chargeFlasks(amount)

            // «Остальные» перезарядки (4.12.x, STAT-PW2-08) - кроме только что применённого умения
            PowerAct.COOLDOWNS -> hero.readyAt.replaceAll { slot, at -> if (effect.to == PowerTarget.OTHERS && slot == moment.slot) at else cooled(slot, at, effect, amount) }

            PowerAct.NEXT_CRIT -> battle.nextCrit = true

            PowerAct.INVULNERABLE -> hero.invulnerableUntil = max(hero.invulnerableUntil, battle.time + duration)

            PowerAct.CLEANSE -> hero.ailments.clear()

            // Снятие проклятий (4.12.x, STAT-PW2-09): лист героя - без их строк
            PowerAct.UNCURSE -> if (hero.effects.removeAll { it.kind == EffectKind.CURSE }) battle.remake(hero)

            PowerAct.SPREAD -> targets(PowerTarget.OTHERS, moment).forEach { foe ->
                moment.ailments.forEach { spread -> battle.place(foe, spread.copy(until = battle.time + spread.duration), stacks = spread.ailment.hurts) }
            }

            PowerAct.ECHO -> {
                val foe = moment.target?.takeIf { it.side == Side.MONSTER && it.alive } ?: return 0.0
                val damage = share(moment.taken, amount, effect.type)
                if (damage.values.sum() > 0) echoes += Echo(battle.time + max(duration, 0.0), foe, damage, moment.spell, power.stat)
            }

            PowerAct.CHARGE -> {
                val kind = effect.charge ?: return 0.0
                if (effect.consume) battle.consumeCharges(kind) else battle.gainCharges(kind, effect.amount?.toInt() ?: 1)
            }

            PowerAct.ONE_OF -> if (effect.options.isNotEmpty()) return act(power, effect.options[battle.random.nextInt(effect.options.size)], value, moment)

            // Повтор (4.4.0): только что применённое умение ещё раз на amount% силы - эхом, без нового SKILL_USE
            PowerAct.RECAST -> moment.cast?.let { battle.echoCast(it, moment.level, amount / 100) }

            // Мгновенность (4.12.x, SKILL-01: «Вечерня»): следующие применения умений героя - без времени применения
            PowerAct.QUICKEN -> battle.quickCasts += amount.roundToInt()

            // Ответный урон (0.15.0, реш. 14) бьёт только атакующего; доли всех сил на один удар - под потолком `caps.retaliate`
            PowerAct.RETALIATE -> {
                val attacker = moment.target?.takeIf { it.side == Side.MONSTER && it.alive } ?: return 0.0
                val answer = battle.rules.caps.retaliated(amount, retaliated)
                val damage = share(moment.taken, answer, effect.type)
                if (damage.values.sum() <= 0) return 0.0
                retaliated += answer
                battle.strike(hero, attacker, Blow(damage, Action.SKILL, spell = true, skill = power.stat, spread = false))
            }
        }
        return 0.0
    }

    /**
     * Готовность умения в ячейке [slot] после силы перезарядок (0.15.0, реш. 7): минус [amount] секунд или, с базой
     * [PowerBase.REMAINING], [amount]% оставшегося - но не раньше дна правил ([com.sperance.exileforge.rules.content.CombatCaps.readyFloor]).
     */
    private fun cooled(slot: String, at: Double, effect: PowerEffect, amount: Double): Double {
        val cut = if (effect.of == PowerBase.REMAINING) max(0.0, at - battle.time) * amount / 100 else amount
        return max(hero.readyFloor[slot] ?: Double.NEGATIVE_INFINITY, at - cut)
    }

    /** [amount] percent of a blow's damage, split as it was, or all of element [type] when one is named. */
    private fun share(taken: Map<DamageType, Double>, amount: Double, type: String?): Map<DamageType, Double> {
        val part = taken.filterValues { it > 0 }.mapValues { it.value * amount / 100 }
        val element = DamageType.element(type) ?: return part
        return mapOf(element to part.values.sum())
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

            // Здоровье врага момента - ударенного или павшего; у босса и уникального - доля `combat.bossTargetLife` (MOB-BAL, В-7: прежняя пятая часть ×0,25)
            PowerBase.TARGET_LIFE -> (moment.target?.takeIf { it.side == Side.MONSTER } ?: foe).let { of ->
                of.body.maxLife * if (of.chief) battle.rules.bossTargetLife else 1.0
            }

            PowerBase.STRENGTH -> body[CoreStat.STRENGTH.code]

            PowerBase.AGILITY -> body[CoreStat.AGILITY.code]

            PowerBase.INTELLECT -> body[CoreStat.INTELLECT.code]

            // Доля оставшейся перезарядки (0.15.0) - база только `COOLDOWNS` (проверка контента), урона не даёт
            PowerBase.WEAPON, PowerBase.REMAINING -> 0.0
        }
        return mapOf((type ?: DamageType.PHYSICAL) to base * share)
    }

    /**
     * How many times a power's effect lies on [fighter] with this one: one more while it still lies, up to [most]. Лежащий эффект
     * помнит свои стопки (4.12.x, STAT-PW2-15): бафф, перенесённый из прошлого боя захода, растёт дальше, а не начинает с одной.
     */
    private fun stacksOf(fighter: Battle.Fighter, source: String, kind: EffectKind, most: Int): Int {
        val key = "$source#${fighter.index}"
        val lying = fighter.effects.filter { it.kind == kind && it.source == source && it.until > battle.time }.maxOfOrNull { it.stacks }
        return (if (lying != null) min(max(1, most), max(stackCount[key] ?: 0, lying) + 1) else 1).also { stackCount[key] = it }
    }

    private companion object {
        /** Effects that show nothing of their own in the log: a blow logs itself, a curse and an ailment land on the foe's line. */
        val SILENT = setOf(
            PowerAct.DAMAGE, PowerAct.AILMENT, PowerAct.CURSE, PowerAct.SPREAD, PowerAct.DELAY, PowerAct.STUN, PowerAct.ECHO, PowerAct.RETALIATE,
            PowerAct.CHARGE, PowerAct.RECAST,
        )

        /** Events too frequent to log a line each. */
        val QUIET = setOf(PowerEvent.HIT, PowerEvent.HIT_TAKEN, PowerEvent.STANDING, PowerEvent.INFLICT)
    }
}
