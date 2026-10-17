package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.core.campaign.EffectTrace
import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.PowerMoment
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.campaign.hexes
import com.sperance.exileforge.core.campaign.usesAll
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FlaskStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.SkillAilment
import com.sperance.exileforge.rules.content.SkillBarrier
import com.sperance.exileforge.rules.content.SkillCast
import com.sperance.exileforge.rules.content.SkillDot
import com.sperance.exileforge.rules.content.SkillEvent
import com.sperance.exileforge.rules.content.SkillHeal
import com.sperance.exileforge.rules.content.SkillHit
import com.sperance.exileforge.rules.content.SkillTrigger
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.sheet.CastTime
import com.sperance.exileforge.rules.sheet.extraTargets
import com.sperance.exileforge.rules.sheet.lines
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** How deep a passive's answer may set off another's. */
private const val MAX_DEPTH = 2

/** A skill's element picked at random, and its ailment named by the element that struck. */
private const val RANDOM = "RANDOM"

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
        SlotCondition.AILING -> hero.ailed
        SlotCondition.MANUAL -> false
    }
}

/**
 * The active slots in order: a ready one whose condition holds, or that was tapped, is used; one short of
 * mana is passed over, so a cheaper heal or guard further down still answers. Подготовки нет - слоты
 * готовы с начала боя; умение применяется своё время ([CastTime]), и пока идёт применение, прочие слоты и удар ждут.
 */
internal fun Battle.useSkills() {
    val hero = heroFighter
    if (hero.casting != null) return
    for ((slot, kitSkill) in kit.actives.withIndex()) {
        kitSkill ?: continue
        val tapped = slot in taps
        if (time < (hero.readyAt[slotKey(slot)] ?: 0.0)) continue
        if (skillLock(slot) > 0) continue
        if (!tapped && !holds(kitSkill.condition, opened[slot])) continue
        if (foeFighters.none { it.alive }) return
        val level = kitSkill.level(hero.body)
        if (!skillsFree() && !affords(cost(kitSkill, level))) continue
        opened[slot] = true
        beginCast(hero, slotKey(slot), kitSkill.skill.code, castSeconds(kitSkill)) { applied(slot, kitSkill, level) }
        if (hero.casting != null || !hero.alive || outcome != null) return
    }
}

/** Секунды применения слота героя: «Вечерня» делает его мгновенным, холод и замедление растягивают, как замах. */
private fun Battle.castSeconds(kitSkill: KitSkill): Double {
    if (quickCasts > 0) {
        quickCasts--
        return 0.0
    }
    return CastTime.of(kitSkill.skill, heroFighter.body, rules) * heroFighter.slow()
}

/**
 * Применение слота [slot] закончено: умение срабатывает, если на него ещё хватает маны, иначе пропадает без перезарядки - со строкой
 * журнала.
 */
private fun Battle.applied(slot: Int, kitSkill: KitSkill, level: Int) {
    if (foeFighters.none { it.alive }) return
    val cost = cost(kitSkill, level)
    if (!skillsFree() && !affords(cost)) {
        note(heroFighter, NoteKind.CAST_NO_MANA, kitSkill.skill.code, cost)
        return
    }
    castSlot(slot, kitSkill, level, cost)
}

/**
 * Цена - сперва из перелива маны (Переполнение), у Обета крови - здоровьем. Резонанс слышит применение до удара и до перезарядки, что он
 * укорачивает. Холод и замедление растягивают перезарядку, как замах; она идёт с конца применения. Кость эха руны тянется лишь у умения с
 * эхом.
 */
private fun Battle.castSlot(slot: Int, kitSkill: KitSkill, level: Int, cost: Double) {
    val hero = heroFighter
    val skill = kitSkill.skill
    val chance = hero.body[CoreStat.FREE_SKILL_CHANCE.code]
    val free = skillsFree() || chance > 0 && random.nextDouble() * 100 < chance
    if (!free) payFor(cost, skill.code)
    fateCast(skill.code)
    hero.readyAt[slotKey(slot)] = time + skill.cooldown * fateCooldown(skill.code) / hero.body.recovery * hero.slow()
    hero.readyFloor[slotKey(slot)] = rules.caps.readyFloor(time, skill.cooldown)
    perform(kitSkill, level)
    skill.echo?.let { echo -> if (random.nextDouble() * 100 < echo.chance) echoCast(kitSkill, level, echo.power / 100) }
    trigger(SkillEvent.SKILL_USE, refund = if (free) 0.0 else cost)
    powers.fire(PowerEvent.SKILL_USE, PowerMoment(target(), spell = skill.spell, cast = kitSkill, level = level, slot = slotKey(slot)))
}

/**
 * Эхо применения: умение [kitSkill] ещё раз на доле [power] своей силы - руной эха или силой `RECAST`. Строки лога
 * помечены эхом; применением оно не считается: ни мана, ни перезарядка, ни отклики пассивных и сил на `SKILL_USE`.
 */
internal fun Battle.echoCast(kitSkill: KitSkill, level: Int, power: Double) {
    if (power <= 0 || outcome != null || !heroFighter.alive || foeFighters.none { it.alive }) return
    val was = echoing
    echoing = true
    try {
        perform(kitSkill, level, power)
    } finally {
        echoing = was
    }
}

/**
 * What a class skill does at [level]: strike, poison, curse, buff, heal, shield or ward — or several. [power] (эхо) -
 * доля силы: урона, лечения, щита и барьера; благо и проклятие ложатся как есть.
 *
 * Трата забирает все заряды своего вида при применении, и удар растёт на каждый - не больше чем за `charges.most` зарядов
 * (`SkillCharges.bonus`). Накопитель даёт заряды, когда его удар лёг на врага, а без удара (клич) - при применении.
 */
private fun Battle.perform(kitSkill: KitSkill, level: Int, power: Double = 1.0) {
    val hero = heroFighter
    val skill = kitSkill.skill
    val charged = skill.charges
    val spent = if (charged?.consume == true) consumeCharges(charged.kind) else 0
    val bonus = (charged?.bonus(level, spent) ?: 1.0) * power
    val landed = skill.hit?.let { heroHit(it, level, skill.code, skill.spell, skill.type == SkillType.ATTACK, bonus = bonus) } ?: false
    skill.dot?.let { heroDot(it, level, skill.code, skill.spell, power) }
    skill.curse?.let { curse(kitSkill, targets(it.targets), level) }
    skill.mark?.let { mark(kitSkill, it, level, power) }
    charged?.gain?.let { gain -> if (skill.hit == null || landed) gainCharges(charged.kind, gain.at(level).roundToInt()) }
    skill.cast?.let { gifts(it, level) }
    if (skill.hit != null || skill.dot != null || skill.curse != null || skill.mark != null) return
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
    var healed = skill.heal?.let { heal(it, level, hero.body.skillHealing * power) } ?: 0.0
    if (warcry && hero.body[CoreStat.WARCRY_HEAL.code] > 0) healed += warcryHeal(hero.body.maxLife * hero.body[CoreStat.WARCRY_HEAL.code] / 100 * power, skill.buff?.duration ?: 0.0)
    skill.shield?.let { hero.shield = min(hero.body.maxShield, hero.shield + hero.body.maxShield * it.at(level) / 100 * power) }
    skill.barrier?.let { ward(it, level, power, keep = power < 1) }
    self(skill.code, healed)
}

/**
 * Лечение клича: [total] здоровья - восстановлением за время действия баффа клича [seconds], здоровья в секунду;
 * клич с полного здоровья лечит урон, что придёт за это время. Без баффа - сразу. Ответ - что вернулось сразу.
 */
private fun Battle.warcryHeal(total: Double, seconds: Double): Double {
    if (seconds <= 0) return restore(total)
    recoveries += Recovery(total / seconds, 0.0, time + seconds, -1)
    return 0.0
}

/** Дары применения (руны): барьер не ниже того, что уже стоит, и заряды своего вида. */
private fun Battle.gifts(cast: SkillCast, level: Int) {
    cast.barrier?.let { ward(it, level, keep = true) }
    val kind = cast.charge ?: return
    if (cast.charges > 0) gainCharges(kind, cast.charges)
}

/**
 * A class skill's blow at its targets — a passive's answer at [only] — each struck [SkillHit.hits] times.
 *
 * Доп. цели атакам и заклинаниям - каждым своя характеристика, одна функция с Мощью.
 */
private fun Battle.heroHit(hit: SkillHit, level: Int, code: String, spell: Boolean, attack: Boolean, only: Fighter? = null, bonus: Double = 1.0): Boolean {
    val hero = heroFighter
    val own = hit.stats.lines(level)
    val body = if (own.isEmpty()) hero.body else heroBody(own)
    val more = if (hit.targets > 0) extraTargets(attack, spell) { body[it.code] } else 0
    val struck = only?.let { listOf(it) } ?: targets(if (hit.targets <= 0) 0 else hit.targets + more)
    val element = hit.element?.let { if (it == RANDOM) DamageType.ELEMENTS.random(random) else DamageType.element(it) }
    val primary = struck.firstOrNull()
    var landed = false
    chained(hit, struck).forEach { (target, share) ->
        repeat(hit.hits.coerceAtLeast(1)) {
            if (!target.alive || !hero.alive || outcome != null) return@repeat
            val grown = heroDamage(hit, level, body, target, element, own)
            val scale = bonus * share
            val damage = if (scale == 1.0) grown.damage else grown.damage.mapValues { it.value * scale }
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

/**
 * Цели удара с долей урона: [struck] - целиком, затем цепь руны - ещё [SkillHit.chain] врагов в досягаемости, каждый
 * прыжок теряет [SkillHit.falloff]% урона прежнего.
 */
private fun Battle.chained(hit: SkillHit, struck: List<Fighter>): List<Pair<Fighter, Double>> {
    val direct = struck.map { it to 1.0 }
    if (hit.chain <= 0) return direct
    val keep = 1 - hit.falloff.coerceIn(0.0, 100.0) / 100
    val jumps = foeFighters.filter { it.alive && reachable(it.index) && it !in struck }.take(hit.chain)
    return direct + jumps.mapIndexed { i, foe -> foe to keep.pow(i + 1) }
}

/** A hero skill's damage by type, and the increases in percent each type was grown by. */
private class Grown(val damage: Map<DamageType, Double>, val increase: Map<DamageType, Double>)

/**
 * A class skill's damage before defences: a share of the weapon — a finisher's larger one on a target ailing or nearly
 * dead — or a spell's own; and a share of all of it turned to the skill's element. Every increase
 * adds up before it multiplies, as in PoE: the skill damage joins the weapon's increases of each type, a spell grows
 * by the increases of its element — the damage in general, the conditions' and the charges' among them — of spells and of skills.
 *
 * «Больше» вида урона ложится и на чары: удар оружием несёт его в числе листа, чары - нет. Плоский урон листа к чарам растёт на увеличения
 * своего вида.
 */
private fun Battle.heroDamage(hit: SkillHit, level: Int, body: Combatant, target: Fighter, element: DamageType?, own: List<StatLine>): Grown {
    val damage = mutableMapOf<DamageType, Double>()
    val increase = mutableMapOf<DamageType, Double>()
    val skill = body[CoreStat.SKILL_DAMAGE.code]
    val growth = heroGrowth(own)
    hit.weapon?.let { weapon ->
        val finisher = hit.finisher?.takeIf { target.ailed || target.life < target.body.maxLife * rules.finisherLife / 100 }
        val share = (finisher ?: weapon).at(level) / 100
        body.damage.forEach { (type, value) ->
            val grown = growth.increased(type.attack)
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
            val grown = growth.increased(of.attack) + spells
            damage.merge(of, value * max(0.0, 1 + grown / 100) * growth.more(of.attack), Double::plus)
            increase[of] = grown
        }
        grow(type, base)
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
 * A spell of damage over time: its roll grown like a spell's, laid on as the dot's own state ([SkillDot.ailment]):
 * яд - стаком под пределом яда, свой урон умения («Высасывание сути») - одним экземпляром, что обновляется. Основа до защит
 * ([AilmentRule.raw]) встречает сопротивление цели на каждом тике, без пробивания; иначе - сразу. Длительность недугов на
 * врагах прибавляет урон (больше тиков: сила - бросок за срок умения). Тики идут в разбор боя умением [code]; прочие недуги и
 * Разложение - строками [SkillDot.ailments].
 *
 * Урон во времени растёт и на «больше» своей стихии.
 */
private fun Battle.heroDot(dot: SkillDot, level: Int, code: String, spell: Boolean, power: Double = 1.0) {
    val hero = heroFighter
    val type = DamageType.element(dot.element) ?: DamageType.CHAOS
    val ailment = Ailment.byWord(dot.ailment)?.takeIf { it.hurts } ?: return
    val rule = ruleOf[ailment]?.first
    val growth = heroGrowth()
    val increase = growth.increased(type.attack) + hero.body[CoreStat.SPELL_DAMAGE.code] + hero.body[CoreStat.SKILL_DAMAGE.code]
    targets(dot.targets).forEach { target ->
        val low = dot.min.at(level)
        val total = (low + random.nextDouble() * (dot.max.at(level) - low).coerceAtLeast(0.0)) * max(0.0, 1 + increase / 100) * growth.more(type.attack) * hero.body.damageMore * power
        val laid = if (rule?.raw == true) total else total * (1 - target.body.resist(type, hero.body.penetration(type))) * target.body.damageTaken(type)
        val inflicted = mutableListOf<Ailment>()
        if (laid > 0 && !target.body.immune(ailment)) {
            val duration = dot.duration * hero.body.ailmentDurationOnFoes(ailment)
            place(
                target,
                ActiveAilment(
                    ailment,
                    time + duration,
                    laid / dot.duration * hero.body.ailmentDamage(ailment),
                    duration,
                    Side.HERO,
                    0,
                    spell,
                    chaos = ailment == Ailment.BURNING && hero.body.igniteAsChaos,
                    limit = rule?.active(hero.body.ailmentLimit(ailment)),
                    skill = code,
                ),
                rule,
            )
            inflicted += ailment
        }
        dot.ailments.mapNotNull { resolve(it, type, level) }.filter { it.first != ailment }.forEach { (other, chance) ->
            if (random.nextDouble() * 100 < chance) afflict(hero, target, other, AilingHit.of(mapOf(type to total)), spell)?.let(inflicted::add)
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
    heal.mana?.let { manaBack(hero, manaCap() * it.at(level) / 100 * scale) }
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

/** Барьер умения в доле [scale]; [keep] (руна, эхо) - не ниже и не короче того, что уже стоит, иначе - заново. */
private fun Battle.ward(barrier: SkillBarrier, level: Int, scale: Double = 1.0, keep: Boolean = false) {
    val hero = heroFighter
    val value = hero.body.maxLife * barrier.life.at(level) / 100 * scale
    hero.barrier = if (keep) max(hero.barrier, value) else value
    hero.barrierUntil = if (keep) max(hero.barrierUntil, time + barrier.duration) else time + barrier.duration
}

/** A skill the hero used on themselves, for the log and the number over their card. */
internal fun Battle.self(code: String, healed: Double = 0.0) = record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, healed, false, emptyList(), null, target()?.index ?: 0, code, onSelf = true)

/** A chance per flask of the belt to gain a charge, by a line of its own ([stat], in percent). */
internal fun Battle.flaskCharge(stat: String) = kit.flasks.forEachIndexed { i, flask ->
    val chance = flask?.sheet?.own(stat) ?: return@forEachIndexed
    if (chance > 0 && random.nextDouble() * 100 < chance) chargeFlask(i, 1.0)
}

/** Доли вылеченного глотком здоровья [healed] в ману и энергощит [me]: от фактически вылеченного. */
internal fun Battle.healedInto(me: Fighter, healed: Double, toMana: Double, toShield: Double) {
    if (healed <= 0) return
    if (toMana > 0) manaBack(me, healed * toMana)
    if (toShield > 0) me.shield = min(me.body.maxShield, me.shield + healed * toShield)
}

/**
 * The passives answering [event]: each by its chance at its level, at most once per its cooldown.
 * [target] is the foe the event was about — the one struck, the one that struck — and [refund] the
 * mana a skill just cost.
 *
 * Дары рун у срабатывания - при каждом срабатывании.
 */
internal fun Battle.trigger(event: SkillEvent, target: Fighter? = null, refund: Double = 0.0, hit: AilingHit? = null) {
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
            answer(code, answer, level, target, refund, hit)
            passive.skill.cast?.let { gifts(it, level) }
        } finally {
            depth--
        }
    }
}

/**
 * Возврат цены - не избыток восстановления: перелив Предначертания его не копит. Основа недуга ответа - удар, что его вызвал, иначе урон
 * листа.
 */
private fun Battle.answer(code: String, answer: SkillTrigger, level: Int, target: Fighter?, refund: Double, blow: AilingHit?) {
    val hero = heroFighter
    val healed = answer.heal?.let { heal(it, level) } ?: 0.0
    answer.shield?.let { hero.shield = min(hero.body.maxShield, hero.shield + hero.body.maxShield * it.at(level) / 100) }
    answer.barrier?.let { ward(it, level) }
    answer.buff?.let { buff(hero, code, it.stats.lines(level), it.duration, it.counter?.at(level) ?: 0.0) }
    if (answer.flaskCharges > 0) chargeFlasks(answer.flaskCharges.toDouble())
    if (answer.refund && refund > 0) manaBack(hero, refund, spill = false)
    val hit = answer.hit
    if (hit != null) {
        heroHit(hit, level, code, spell = false, attack = true, only = target?.takeIf { hit.targets == 1 && it.alive && it.side == Side.MONSTER })
        return
    }
    val struck = target?.takeIf { it.alive && it.side == Side.MONSTER }
    val word = answer.ailment
    if (word != null && struck != null) {
        val element = blow?.landed?.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
        val ailment = (if (word == ELEMENT) Ailment.of(element) else Ailment.byWord(word)) ?: return
        val laid = blow ?: AilingHit.of(hero.body.damage)
        val inflicted = List(if (answer.twice) 2 else 1) { afflict(hero, struck, ailment, laid) }.filterNotNull().distinct()
        record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, inflicted, null, struck.index, code)
        return
    }
    self(code, healed)
}

/**
 * The belt in order: a flask whose condition holds, that was tapped, or that drinks itself at low life, is drunk if it has the charges and is not running.
 *
 * Как в PoE: глоток, что только лечит, не тратится на полной полосе.
 */
internal fun Battle.useFlasks() {
    val hero = heroFighter
    kit.flasks.forEachIndexed { i, flask ->
        flask ?: return@forEachIndexed
        if (draughtOf(i) != null || flaskLock(i) > 0) return@forEachIndexed
        val auto = flask.sheet.own(FlaskStat.AUTO_LOW_LIFE.code).let { it > 0 && hero.life < hero.body.maxLife * it / 100 }
        if (i !in drinks && !auto && !holds(flask.condition, flaskOpened[i])) return@forEachIndexed
        if (charges[i] + 1e-9 < flask.sheet.perUse(hero.body::get)) return@forEachIndexed
        if (hero.life >= hero.body.maxLife && flask.sheet.draught(hero.body, hero.life, manaCap()).lifeOnly) return@forEachIndexed
        flaskOpened[i] = true
        drink(i, flask)
    }
}

/** Невосприимчивость глотка сразу снимает то, от чего бережёт. Сила глотка на уникальной фляге отвечает только на свою флягу. */
internal fun Battle.drink(slot: Int, belt: BeltFlask, free: Boolean = false) {
    val flask = belt.sheet
    val hero = heroFighter
    val keep = !free && flask.own(FlaskStat.NO_CHARGE_CHANCE.code).let { it > 0 && random.nextDouble() * 100 < it }
    val draught = flask.draught(hero.body, hero.life, manaCap())
    charges[slot] = when {
        free -> charges[slot]
        flask.usesAll -> 0.0
        keep -> charges[slot]
        else -> (charges[slot] - flask.perUse(hero.body::get)).coerceAtLeast(0.0)
    }
    lay(hero, TimedEffect(EffectKind.FLASK, flask.code, draught.lines, time + draught.duration, draught.duration, slot = slot, conditional = draught.conditional))
    hero.ailments.removeAll { hero.body.immune(it.ailment) }
    manaBack(hero, draught.mana)
    hero.shield = min(hero.body.maxShield, hero.shield + draught.shield)
    if (draught.lifeRate > 0 || draught.manaRate > 0) recoveries += Recovery(draught.lifeRate, draught.manaRate, time + draught.duration, slot, draught.lifeOnly, draught.toMana, draught.toShield)
    if (draught.invulnerable > 0) hero.invulnerableUntil = time + draught.invulnerable
    if (!free) trial.flasks++
    val healed = lifeBack(hero, draught.life, overheal = true)
    healedInto(hero, healed, draught.toMana, draught.toShield)
    record(
        Side.HERO, Action.FLASK, HitKind.HIT, 0.0, null, healed, false, emptyList(), null, target()?.index ?: 0, flask.code, onSelf = true,
        trace = EffectTrace(EffectKind.FLASK, flask.code, draught.lines, draught.duration, healed, flask.effect(hero.body::get), shot(hero), origin),
    )
    if (healed > 0 || draught.lifeRate > 0) trigger(SkillEvent.HEALED)
    powers.fire(PowerEvent.FLASK, PowerMoment(flask = draught.lines.mapTo(HashSet()) { it.stat }))
}
