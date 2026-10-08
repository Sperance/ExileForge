package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.core.campaign.EffectTrace
import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.PowerMoment
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.campaign.hexes
import com.sperance.exileforge.core.campaign.lines
import com.sperance.exileforge.core.campaign.usesAll
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.SkillAilment
import com.sperance.exileforge.rules.content.SkillBarrier
import com.sperance.exileforge.rules.content.SkillDot
import com.sperance.exileforge.rules.content.SkillEvent
import com.sperance.exileforge.rules.content.SkillHeal
import com.sperance.exileforge.rules.content.SkillHit
import com.sperance.exileforge.rules.content.SkillTrigger
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.SlotCondition
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ==================== Skills, flasks and answers (2.78.0) ====================

/** How deep a passive's answer may set off another's. */
private const val MAX_DEPTH = 2

/** A skill's element picked at random, and its ailment named by the element that struck (server 0.69.0). */
private const val RANDOM = "RANDOM"

/** Quick preparation (3.13.0): shortens how much of a skill's cooldown still runs as a fight opens. */
private val PREPARATION: String = CoreStat.SKILL_PREPARATION.code
private const val ELEMENT = "ELEMENT"

/** Whether a slot's [condition] holds now; an opening one only until its slot has fired this fight. */
private fun Battle.holds(condition: SlotCondition, opened: Boolean): Boolean {
    val hero = heroFighter
    return when (condition) {
        SlotCondition.READY -> true
        SlotCondition.FIGHT_START -> !opened
        SlotCondition.RARE_OR_BOSS -> foeFighters.any { it.alive && foes[it.index].rarity >= MonsterRarity.RARE }
        SlotCondition.LIFE_50 -> hero.life < hero.body.maxLife * 0.5
        SlotCondition.LIFE_35 -> hero.life < hero.body.maxLife * 0.35
        SlotCondition.LIFE_20 -> hero.life < hero.body.maxLife * 0.2
        SlotCondition.MANA_30 -> hero.mana < manaCap() * 0.3
        SlotCondition.SHIELD_BROKEN -> hero.body.maxShield > 0 && hero.shield <= 0.5
        SlotCondition.ENEMIES_3 -> enemies() >= 3
        SlotCondition.AILING -> hero.ailments.isNotEmpty()
        SlotCondition.MANUAL -> false
    }
}

/**
 * The active slots in order: a ready one whose condition holds, or that was tapped, is used; one short of
 * mana is passed over (3.17.0), so a cheaper heal or guard further down still answers.
 */
internal fun Battle.useSkills() {
    val hero = heroFighter
    for ((slot, kitSkill) in kit.actives.withIndex()) {
        kitSkill ?: continue
        val tapped = slot in taps
        if (time < hero.readyAt.getOrPut(slotKey(slot)) { openingReady(kitSkill) }) continue
        if (skillLock(slot) > 0) continue
        if (!tapped && !holds(kitSkill.condition, opened[slot])) continue
        if (foeFighters.none { it.alive }) return
        val level = kitSkill.level(hero.body)
        val cost = cost(kitSkill, level)
        if (hero.mana + 1e-9 < cost && !skillsFree()) continue
        opened[slot] = true
        castSlot(slot, kitSkill, level, cost)
        if (!hero.alive || outcome != null) return
    }
}

/**
 * When a slot is first ready in this fight: after the skill's preparation at its level — its share of the cooldown,
 * shortened by the hero's quick preparation (3.13.0) — a fight-start skill at once.
 */
private fun Battle.openingReady(kitSkill: KitSkill): Double {
    if (kitSkill.condition == SlotCondition.FIGHT_START) return time
    val body = heroFighter.body
    val share = rules.preparation(kitSkill.skill, kitSkill.level(body), body[PREPARATION])
    return time + share * kitSkill.skill.cooldown / body.recovery(kitSkill.skill.spell)
}

private fun Battle.castSlot(slot: Int, kitSkill: KitSkill, level: Int, cost: Double) {
    val hero = heroFighter
    val skill = kitSkill.skill
    val chance = hero.body[CoreStat.FREE_SKILL_CHANCE.code]
    val free = skillsFree() || chance > 0 && random.nextDouble() * 100 < chance
    if (!free) hero.mana = max(0.0, hero.mana - cost)
    hero.readyAt[slotKey(slot)] = time + skill.cooldown / hero.body.recovery(skill.spell)
    perform(kitSkill, level)
    trigger(SkillEvent.SKILL_USE, refund = if (free) 0.0 else cost)
    powers.fire(PowerEvent.SKILL_USE, PowerMoment(target(), spell = skill.spell))
}

/** What a class skill does at [level]: strike, poison, curse, buff, heal, shield or ward — or several. */
private fun Battle.perform(kitSkill: KitSkill, level: Int) {
    val hero = heroFighter
    val skill = kitSkill.skill
    // Charges (3.33.0, server 1.32.0): a spender takes every charge of its kind as it is used, and its blow grows by each.
    val charged = skill.charges
    val spent = if (charged?.consume == true) consumeCharges(charged.kind) else 0
    val bonus = 1 + (charged?.perCharge?.at(level) ?: 0.0) * spent / 100
    val landed = skill.hit?.let { heroHit(it, level, skill.code, skill.spell, skill.type == SkillType.ATTACK, bonus = bonus) } ?: false
    skill.dot?.let { heroDot(it, level, skill.code, skill.spell) }
    skill.curse?.let { curse(kitSkill, targets(it.targets), level) }
    // A generator gives its charges once its blow lands on a foe — or, with no blow, a warcry's, on use.
    charged?.gain?.let { gain -> if (skill.hit == null || landed) gainCharges(charged.kind, gain.at(level).roundToInt()) }
    if (skill.hit != null || skill.dot != null || skill.curse != null) return
    val warcry = skill.type == SkillType.WARCRY
    skill.buff?.let { buff ->
        val speed = if (warcry) hero.body[CoreStat.WARCRY_SPEED.code] else 0.0
        buff(
            hero,
            skill.code,
            buff.stats.lines(level, if (warcry) 1 + hero.body[CoreStat.WARCRY_EFFECT.code] / 100 else 1.0) +
                listOfNotNull(StatLine(CoreStat.ATTACK_SPEED.code, Op.INCREASED, speed).takeIf { speed > 0 }),
            buff.duration,
            buff.counter?.at(level) ?: 0.0,
        )
        if (buff.nextCrit) nextCrit = true
    }
    var healed = skill.heal?.let { heal(it, level, hero.body.skillHealing) } ?: 0.0
    if (warcry && hero.body[CoreStat.WARCRY_HEAL.code] > 0) healed += restore(hero.body.maxLife * hero.body[CoreStat.WARCRY_HEAL.code] / 100)
    skill.shield?.let { hero.shield = min(hero.body.maxShield, hero.shield + hero.body.maxShield * it.at(level) / 100) }
    skill.barrier?.let { ward(it, level) }
    self(skill.code, healed)
}

/** A class skill's blow at its targets — a passive's answer at [only] — each struck [SkillHit.hits] times. */
private fun Battle.heroHit(hit: SkillHit, level: Int, code: String, spell: Boolean, attack: Boolean, only: Fighter? = null, bonus: Double = 1.0): Boolean {
    val hero = heroFighter
    val own = hit.stats.lines(level)
    val body = if (own.isEmpty()) hero.body else heroBody(own)
    val more = if (attack && hit.targets > 0) body[CoreStat.SKILL_TARGETS.code].toInt().coerceAtLeast(0) else 0
    val struck = only?.let { listOf(it) } ?: targets(if (hit.targets <= 0) 0 else hit.targets + more)
    val element = hit.element?.let { if (it == RANDOM) DamageType.ELEMENTS.random(random) else DamageType.element(it) }
    val primary = struck.firstOrNull()
    var landed = false
    struck.forEach { target ->
        repeat(hit.hits.coerceAtLeast(1)) {
            if (!target.alive || !hero.alive || outcome != null) return@repeat
            val grown = heroDamage(hit, level, body, target, element, own)
            val damage = if (bonus == 1.0) grown.damage else grown.damage.mapValues { it.value * bonus }
            val leading = damage.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
            if (strike(
                    hero,
                    target,
                    Blow(
                        damage, Action.SKILL, spell, code, body, spread = hit.spell == null, stun = hit.stun?.at(level) ?: 0.0,
                        ailments = hit.ailments.mapNotNull { resolve(it, element ?: leading, level) }, primary = target === primary, increase = grown.increase,
                    ),
                )
            ) {
                landed = true
            }
        }
    }
    return landed
}

/** A hero skill's damage by type, and the increases in percent each type was grown by (server 1.57.0). */
private class Grown(val damage: Map<DamageType, Double>, val increase: Map<DamageType, Double>)

/**
 * A class skill's damage before defences: a share of the weapon — a finisher's larger one on a target ailing or nearly
 * dead — or a spell's own; and a share of all of it turned to the skill's element. Since server 1.57.0 every increase
 * adds up before it multiplies, as in PoE: the skill damage joins the weapon's increases of each type, a spell grows
 * by the increases of its element — the damage in general, the conditions' and the charges' among them — of spells and of skills.
 */
private fun Battle.heroDamage(hit: SkillHit, level: Int, body: Combatant, target: Fighter, element: DamageType?, own: List<StatLine>): Grown {
    val damage = mutableMapOf<DamageType, Double>()
    val increase = mutableMapOf<DamageType, Double>()
    val skill = body[CoreStat.SKILL_DAMAGE.code]
    hit.weapon?.let { weapon ->
        val finisher = hit.finisher?.takeIf { target.ailments.isNotEmpty() || target.life < target.body.maxLife * 0.3 }
        val share = (finisher ?: weapon).at(level) / 100
        body.damage.forEach { (type, value) ->
            val grown = heroIncrease(type, own)
            damage.merge(type, regrow(value, grown, skill) * share, Double::plus)
            increase[type] = grown + skill
        }
    }
    hit.spell?.let { spell ->
        val type = DamageType.element(spell.element) ?: DamageType.FIRE
        val low = spell.min.at(level)
        val base = low + random.nextDouble() * (spell.max.at(level) - low).coerceAtLeast(0.0)
        val spells = body[CoreStat.SPELL_DAMAGE.code] + skill
        fun grow(of: DamageType, value: Double) {
            val grown = heroIncrease(of, own) + spells
            damage.merge(of, value * max(0.0, 1 + grown / 100), Double::plus)
            increase[of] = grown
        }
        grow(type, base)
        // 3.35.0: the flat damage the sheet adds to spells, each type grown by its own increases.
        DamageType.ELEMENTS.forEach { added -> body.spellAdded(added).takeIf { it > 0 }?.let { grow(added, it) } }
    }
    val convert = (hit.convert?.at(level) ?: 0.0).coerceIn(0.0, 100.0) / 100
    if (element != null && convert > 0) {
        val total = damage.values.sum()
        damage.replaceAll { _, value -> value * (1 - convert) }
        damage.merge(element, total * convert, Double::plus)
    }
    return Grown(damage, increase)
}

/** A skill's chance of an ailment: `ELEMENT` is the one [element] brings. */
internal fun Battle.resolve(ailment: SkillAilment, element: DamageType, level: Int): Pair<Ailment, Double>? = (if (ailment.ailment == ELEMENT) Ailment.of(element) else Ailment.byWord(ailment.ailment))?.let { it to ailment.chance.at(level) }

/**
 * A spell of damage over time: its roll grown like a spell's, taken by the target's resistance at once
 * and laid on as the ailment of its element — a poison stacks — for its duration.
 */
private fun Battle.heroDot(dot: SkillDot, level: Int, code: String, spell: Boolean) {
    val hero = heroFighter
    val type = DamageType.element(dot.element) ?: DamageType.CHAOS
    val ailment = Ailment.of(type).takeIf { it.hurts } ?: Ailment.POISONED
    val increase = heroIncrease(type) + hero.body[CoreStat.SPELL_DAMAGE.code] + hero.body[CoreStat.SKILL_DAMAGE.code]
    targets(dot.targets).forEach { target ->
        val low = dot.min.at(level)
        val total = (low + random.nextDouble() * (dot.max.at(level) - low).coerceAtLeast(0.0)) * max(0.0, 1 + increase / 100) * hero.body.damageMore
        val mitigated = total * (1 - target.body.resist(type, hero.body.penetration(type))) * target.body.damageTaken(type)
        val inflicted = mutableListOf<Ailment>()
        if (mitigated > 0 && !target.body.immune(ailment)) {
            val duration = dot.duration * hero.body.ailmentDurationOnFoes(ailment)
            place(
                target,
                ActiveAilment(
                    ailment,
                    time + duration,
                    mitigated / duration * hero.body.ailmentDamage(ailment),
                    duration,
                    Side.HERO,
                    0,
                    spell,
                    chaos = ailment == Ailment.BURNING && hero.body.igniteAsChaos,
                ),
                stacks = true,
            )
            inflicted += ailment
        }
        dot.ailments.mapNotNull { resolve(it, type, level) }.filter { it.first != ailment }.forEach { (other, chance) ->
            if (random.nextDouble() * 100 < chance) afflict(hero, target, other, mapOf(type to mitigated), spell)?.let(inflicted::add)
        }
        record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, type, 0.0, false, inflicted, null, target.index, code)
    }
}

/** A curse of the hero's on [targets]: its lines stronger by the curse effect, for its duration. */
internal fun Battle.curse(kitSkill: KitSkill, targets: List<Fighter>, level: Int = kitSkill.level(heroFighter.body)) {
    val curse = kitSkill.skill.curse ?: return
    val scale = 1 + heroFighter.body[CoreStat.CURSE_EFFECT.code] / 100
    targets.filter { it.alive }.forEach { target ->
        lay(target, TimedEffect(EffectKind.CURSE, kitSkill.skill.code, curse.stats.lines(level, scale), time + curse.duration, curse.duration))
        record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, target.index, kitSkill.skill.code)
    }
}

/** The curse the hero answers a blow with: the first in their slots, else the class's first. */
internal fun Battle.curseOf(): KitSkill? = kit.actives.firstOrNull { it?.skill?.curse != null } ?: kit.curses.firstOrNull()

/** A hexing draught runs (a unique flask): every blow that lands lays a random curse of the class. */
internal fun Battle.hexing(): Boolean = kit.flasks.indices.any { i -> kit.flasks[i]?.sheet?.hexes == true && draughtOf(i) != null }

internal fun Battle.hex(target: Fighter) {
    if (target.cursed || kit.curses.isEmpty()) return
    curse(kit.curses[random.nextInt(kit.curses.size)], listOf(target))
}

internal fun Battle.buff(fighter: Fighter, source: String, lines: List<StatLine>, duration: Double, counter: Double = 0.0) = lay(fighter, TimedEffect(EffectKind.BUFF, source, lines, time + duration, duration, counter))

/** Lays [effect] on [fighter], in place of the same one from the same source, and makes the body again. */
internal fun Battle.lay(fighter: Fighter, effect: TimedEffect) {
    fighter.effects.removeAll { it.kind == effect.kind && it.source == effect.source && it.slot == effect.slot }
    fighter.effects += effect
    remake(fighter)
}

/** Healing in shares of the maximums, [scale] stronger; a cleansing one lifts the ailment that would last longest. */
private fun Battle.heal(heal: SkillHeal, level: Int, scale: Double = 1.0): Double {
    val hero = heroFighter
    val life = heal.life?.let { restore(hero.body.maxLife * it.at(level) / 100 * scale) } ?: 0.0
    heal.mana?.let { hero.mana = min(manaCap(), hero.mana + manaCap() * it.at(level) / 100 * scale) }
    if (heal.cleanse) hero.ailments.maxByOrNull { it.until }?.let { worst -> hero.ailments.removeAll { it.ailment == worst.ailment } }
    return life
}

/** Life given back to the hero; the passives waiting for a heal hear of it. */
internal fun Battle.restore(amount: Double): Double {
    val hero = heroFighter
    val healed = lifeBack(hero, amount)
    if (healed > 0) trigger(SkillEvent.HEALED)
    return healed
}

private fun Battle.ward(barrier: SkillBarrier, level: Int) {
    val hero = heroFighter
    hero.barrier = hero.body.maxLife * barrier.life.at(level) / 100
    hero.barrierUntil = time + barrier.duration
}

/** A skill the hero used on themselves, for the log and the number over their card. */
internal fun Battle.self(code: String, healed: Double = 0.0) = record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, healed, false, emptyList(), null, target()?.index ?: 0, code, onSelf = true)

/** A chance per flask of the belt to gain a charge, by a line of its own ([stat], in percent). */
internal fun Battle.flaskCharge(stat: String) = kit.flasks.forEachIndexed { i, flask ->
    val chance = flask?.sheet?.own(stat) ?: return@forEachIndexed
    if (chance > 0 && random.nextDouble() * 100 < chance) charges[i] = min(flask.sheet.maxCharges, charges[i] + 1)
}

/**
 * The passives answering [event]: each by its chance at its level, at most once per its cooldown.
 * [target] is the foe the event was about — the one struck, the one that struck — and [refund] the
 * mana a skill just cost.
 */
internal fun Battle.trigger(event: SkillEvent, target: Fighter? = null, refund: Double = 0.0, taken: Map<DamageType, Double> = emptyMap()) {
    if (depth >= MAX_DEPTH || !heroFighter.alive || outcome != null) return
    kit.passives.forEach { passive ->
        val answer = passive.skill.trigger?.takeIf { it.on == event } ?: return@forEach
        val code = passive.skill.code
        if (time < (triggerReady[code] ?: -1.0)) return@forEach
        val level = passive.level(heroFighter.body)
        answer.chance?.let { chance -> if (random.nextDouble() * 100 >= chance.at(level)) return@forEach }
        if (answer.cooldown > 0) triggerReady[code] = time + answer.cooldown
        depth++
        try {
            answer(code, answer, level, target, refund, taken)
        } finally {
            depth--
        }
    }
}

private fun Battle.answer(code: String, answer: SkillTrigger, level: Int, target: Fighter?, refund: Double, taken: Map<DamageType, Double>) {
    val hero = heroFighter
    val healed = answer.heal?.let { heal(it, level) } ?: 0.0
    answer.shield?.let { hero.shield = min(hero.body.maxShield, hero.shield + hero.body.maxShield * it.at(level) / 100) }
    answer.barrier?.let { ward(it, level) }
    answer.buff?.let { buff(hero, code, it.stats.lines(level), it.duration, it.counter?.at(level) ?: 0.0) }
    if (answer.flaskCharges > 0) kit.flasks.forEachIndexed { i, flask -> flask?.let { charges[i] = min(it.sheet.maxCharges, charges[i] + answer.flaskCharges) } }
    if (answer.refund && refund > 0) hero.mana = min(manaCap(), hero.mana + refund)
    val hit = answer.hit
    if (hit != null) {
        heroHit(hit, level, code, spell = false, attack = true, only = target?.takeIf { hit.targets == 1 && it.alive && it.side == Side.MONSTER })
        return
    }
    val struck = target?.takeIf { it.alive && it.side == Side.MONSTER }
    val word = answer.ailment
    if (word != null && struck != null) {
        val element = taken.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
        val ailment = (if (word == ELEMENT) Ailment.of(element) else Ailment.byWord(word)) ?: return
        val inflicted = List(if (answer.twice) 2 else 1) { afflict(hero, struck, ailment, taken.ifEmpty { hero.body.damage }) }.filterNotNull().distinct()
        record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, inflicted, null, struck.index, code)
        return
    }
    self(code, healed)
}

/** The belt in order: a flask whose condition holds, that was tapped, or that drinks itself at low life, is drunk if it has the charges and is not running. */
internal fun Battle.useFlasks() {
    val hero = heroFighter
    kit.flasks.forEachIndexed { i, flask ->
        flask ?: return@forEachIndexed
        if (draughtOf(i) != null || flaskLock(i) > 0) return@forEachIndexed
        val auto = flask.sheet.own("FLASK_AUTO_LOW_LIFE").let { it > 0 && hero.life < hero.body.maxLife * it / 100 }
        if (i !in drinks && !auto && !holds(flask.condition, flaskOpened[i])) return@forEachIndexed
        if (charges[i] + 1e-9 < flask.sheet.perUse(hero.body::get)) return@forEachIndexed
        // As in PoE (3.79.0): a draught that only gives life back is not wasted on a full bar.
        if (hero.life >= hero.body.maxLife && flask.sheet.draught(hero.body, hero.life, manaCap()).lifeOnly) return@forEachIndexed
        flaskOpened[i] = true
        drink(i, flask)
    }
}

internal fun Battle.drink(slot: Int, belt: BeltFlask, free: Boolean = false) {
    val flask = belt.sheet
    val hero = heroFighter
    val keep = !free && flask.own("FLASK_NO_CHARGE_CHANCE").let { it > 0 && random.nextDouble() * 100 < it }
    val draught = flask.draught(hero.body, hero.life, manaCap())
    charges[slot] = when {
        free -> charges[slot]
        flask.usesAll -> 0.0
        keep -> charges[slot]
        else -> (charges[slot] - flask.perUse(hero.body::get)).coerceAtLeast(0.0)
    }
    lay(hero, TimedEffect(EffectKind.FLASK, flask.code, draught.lines, time + draught.duration, draught.duration, slot = slot))
    // An immunity drunk lifts what it guards against at once.
    hero.ailments.removeAll { hero.body.immune(it.ailment) }
    hero.mana = min(manaCap(), hero.mana + draught.mana)
    hero.shield = min(hero.body.maxShield, hero.shield + draught.shield)
    if (draught.lifeRate > 0 || draught.manaRate > 0) recoveries += Recovery(draught.lifeRate, draught.manaRate, time + draught.duration, slot, draught.lifeOnly)
    if (draught.invulnerable > 0) hero.invulnerableUntil = time + draught.invulnerable
    if (!free) trial.flasks++
    val healed = lifeBack(hero, draught.life)
    record(
        Side.HERO, Action.FLASK, HitKind.HIT, 0.0, null, healed, false, emptyList(), null, target()?.index ?: 0, flask.code, onSelf = true,
        trace = EffectTrace(EffectKind.FLASK, flask.code, draught.lines, draught.duration, healed, flask.effect(hero.body::get), shot(hero), origin),
    )
    if (healed > 0 || draught.lifeRate > 0) trigger(SkillEvent.HEALED)
    powers.fire(PowerEvent.FLASK)
}
