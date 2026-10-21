package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.FactorKey
import com.sperance.exileforge.core.campaign.FactorTrace
import com.sperance.exileforge.core.campaign.HitTrace
import com.sperance.exileforge.core.campaign.Landing
import com.sperance.exileforge.core.campaign.PowerMoment
import com.sperance.exileforge.core.campaign.RollKey
import com.sperance.exileforge.core.campaign.StatLines
import com.sperance.exileforge.core.campaign.TypeTrace
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.SkillEvent
import kotlin.math.max
import kotlin.math.min

/** Доля удара [attacker] по [target], что проходит: осквернённый монстр бьёт героя слабее на его снижение от Скверны. */
internal fun Battle.blightTaken(attacker: Fighter, target: Fighter): Double = if (target === heroFighter && attacker.side == Side.MONSTER && foes[attacker.index].tainted) 1 - target.body.blightReduction else 1.0

/**
 * One blow of [me] at [target] — a weapon's swing, a skill's hit, a spell: evaded unless a spell,
 * blocked, or landed and maybe critical; then armour, resistance and shock.
 *
 * Порядок удара:
 * - месть Предначертания - крит наверняка, заряд «из-под плаща» цел, множитель крита - до траты заряда; броски берутся до любых перемен;
 * Удача рода - уклонение, блок и крит героя лучшим из двух бросков;
 * - крит не легче удара; доп. урон крита по цели - доля `critTaken` от прибавки крита сверх удара; двойной удар, строки героя против
 * состояния цели (увеличения рядом с увеличениями удара) и что пропускают подавление и отклонение;
 * - пробивание снимает часть сопротивления, «получаемый урон» и проклятие цели растят прошедшее; слабее всего сопротивляемая стихия
 * пробивается глубже;
 * - Разлом («Тяжкие удары», печати Стража, «Одна стихия» Владыки) и Предначертание (рычаги героя тяжелее, враг легче); разброс урона героя
 * - больший из двух бросков;
 * - зеркало урона - запас сверх удара теми же типами по долям, до защит цели;
 * - урон для недугов - после защит и «получаемого урона» цели, без шока, подавления и Укрепления; основа яда - до защит без множителя крита
 * ([unmitigated]), урон ударами недуги не растит; Разложение - урон своего вида, что прибавили стаки цели, графа разбора;
 * - удар конечен: NaN и бесконечность - не урон; отражение - только от ударов, чары и недуги не отражаются.
 */
internal fun Battle.strike(me: Fighter, target: Fighter, blow: Blow): Boolean {
    val body = blow.body ?: me.body
    val foe = if (me.side == Side.MONSTER) me.index else target.index
    if (target.invulnerable) {
        record(me.side, blow.action, HitKind.INVULNERABLE, 0.0, null, 0.0, false, emptyList(), null, foe, blow.skill, spell = blow.spell)
        return false
    }
    val sure = me === heroFighter && nextCrit
    val fatedSure = !sure && fateSure(me)
    val keener = fateKeener(me)
    val striker = shot(me, blow)
    val struck = shot(target)
    val evade = evasion(me, target)
    val blockChance = if (blow.spell) target.body.spellBlock(target.side == Side.MONSTER) else target.body.block
    val kind = when {
        target.frozen() -> if (sure || fatedSure || crit(me, body, blow.spell)) HitKind.CRIT else HitKind.HIT
        blow.evadable && checkDraw(target, RollKey.EVADE) < evade -> HitKind.EVADED
        checkDraw(target, RollKey.BLOCK) < blockChance -> HitKind.BLOCKED
        sure || fatedSure || crit(me, body, blow.spell) -> HitKind.CRIT
        else -> HitKind.HIT
    }.let { if (it == HitKind.CRIT && (critBanned(me) || fateWarded(target))) HitKind.HIT else it }
    if (sure && kind == HitKind.CRIT) nextCrit = false
    fateSwung(me, kind)
    if (me.side == Side.MONSTER) lastStriker = me.index
    if (kind == HitKind.EVADED || kind == HitKind.BLOCKED) {
        record(
            me.side, blow.action, kind, 0.0, null, 0.0, false, emptyList(), null, foe, blow.skill,
            trace = HitTrace(striker, struck, emptyList(), emptyList(), null, origin, avoided(body, target, blow)), spell = blow.spell,
        )
        if (kind == HitKind.BLOCKED) blocked(target)
        if (kind == HitKind.EVADED) traitsEvaded(target)
        if (target === heroFighter) {
            trigger(if (kind == HitKind.EVADED) SkillEvent.EVADE else SkillEvent.BLOCK, me)
            powers.fire(if (kind == HitKind.EVADED) PowerEvent.EVADE else PowerEvent.BLOCK, PowerMoment(me))
            if (kind == HitKind.BLOCKED) counter(me)
        }
        return false
    }
    val multiplier = if (kind == HitKind.CRIT) max(1.0, 1 + (body.critMultiplier(blow.spell) * keener - 1) * target.body.critTaken) else body.nonCritMore
    val doubled = if (body.doubleDamage > 0 && draw() < body.doubleDamage) 2.0 else 1.0
    val facing = if (me === heroFighter) states(target).let { states -> max(0.0, model.against(states) + me.actingEffects.sumOf { it.against(states) }) } else 0.0
    val versus = versus(blow, facing)
    val eased = eased(target, blow) * target.body.hitTaken * blightTaken(me, target) * provokedTaken(me, target)
    val against = body.damageAgainst(target.ailments.map { it.ailment }) * (if (target.cursed) 1 + max(0.0, body[CoreStat.DAMAGE_VS_CURSED.code]) / 100 else 1.0)
    val weakest = if (body.lowestResistPenetrate > 0) DamageType.ELEMENTS.minBy { target.body.resistTo(it) } else null
    val strongest = if (target.body.highestResistElementTaken != 0.0) DamageType.ELEMENTS.maxBy { target.body.resistTo(it) } else null
    val heavy = heavy(me, blow)
    val fateMore = fateHeavier(me, target, blow)
    val luckySpread = blow.spread && fateLuckySpread(me)
    val seals = sealed(target)
    var unruled = 0.0
    val types = mutableListOf<TypeTrace>()
    var spreadSum = 0.0
    var takenSum = 0.0
    var defended = 0.0
    val parts = converted(body, target.body, blow.damage.filterValues { it > 0 })
    val stored = fateExtra(me)
    val partsSum = parts.values.sum()
    val ailing = mutableMapOf<DamageType, Double>()
    val unmitigated = mutableMapOf<DamageType, Double>()
    val hitMore = hitMore(me, body, blow, facing, versus)
    var exposed = 0.0
    var exposedSum = 0.0
    val taken = parts.mapValues { (type, base) ->
        val spread = if (blow.spread) 1 + ((if (luckySpread) max(random.nextDouble(), random.nextDouble()) else random.nextDouble()) * 2 - 1) * rules.variance / 100 else 1.0
        val hurt = base * spread * against * body.damageMore * doubled * versus * fateMore
        val grown = hurt * multiplier * hitMore
        val heavied = if (heavy == 1.0) grown else grown * heavy
        val share = if (stored > 0 && partsSum > 0) stored * base / partsSum else 0.0
        val raw = heavied + share
        val pierce = body.penetration(type) + if (type == weakest) body.lowestResistPenetrate else 0.0
        val (armour, resist, defence) = defended(target.body, type, raw, pierce)
        val exposure = target.exposure(type)
        val typeTaken = target.body.damageTaken(type) * (if (type == strongest) max(0.0, 1 + target.body.highestResistElementTaken / 100) else 1.0) * exposure
        val landed = defence * target.weakness() * typeTaken * eased * exposure(target, type)
        val sealedOff = if (seals == 1.0) landed else landed * seals
        unruled += sealedOff
        val ruled = lawTaken(target, type)
        val dealt = (if (ruled == 1.0) sealedOff else sealedOff * ruled).takeIf { it.isFinite() } ?: 0.0
        ailing[type] = (defence * typeTaken * seals * ruled / hitMore).takeIf { it.isFinite() } ?: 0.0
        unmitigated[type] = ((if (heavy == 1.0) hurt else hurt * heavy) + share).takeIf { it.isFinite() } ?: 0.0
        if (exposure != 1.0) {
            exposed += dealt * (1 - 1 / exposure)
            exposedSum += dealt
        }
        spreadSum += base * spread
        takenSum += defence * typeTaken / exposure
        defended += defence
        types += TypeTrace(type, base, raw, armour, resist, pierce, dealt)
        dealt
    }
    val baseSum = types.sumOf { it.base }
    val rawSum = types.sumOf { it.raw }
    val factors = buildList {
        add(
            FactorTrace(
                FactorKey.BASE,
                baseSum,
                DamageType.entries.map { it.attack } + listOf(CoreStat.SKILL_DAMAGE.code, CoreStat.SPELL_DAMAGE.code) +
                    DamageType.entries.filter { it != DamageType.PHYSICAL }.map { "STOCK_PHYSICAL_AS_EXTRA_${it.name}" },
                DamageType.ELEMENTS.map { "STOCK_PHYSICAL_TAKEN_AS_${it.name}" },
            ),
        )
        if (blow.spread && baseSum > 0) add(FactorTrace(FactorKey.SPREAD, spreadSum / baseSum))
        if (kind == HitKind.CRIT) {
            add(
                FactorTrace(
                    FactorKey.CRIT,
                    multiplier,
                    listOf(if (blow.spell) CoreStat.SPELL_CRITICAL_MULTIPLIER.code else CoreStat.CRITICAL_MULTIPLIER.code),
                    listOf(CoreStat.CRITICAL_TAKEN.code),
                ),
            )
        } else if (multiplier != 1.0) {
            add(FactorTrace(FactorKey.NON_CRIT, multiplier, listOf(CoreStat.NON_CRIT_DAMAGE.code)))
        }
        add(FactorTrace(FactorKey.DAMAGE, body.damageMore, listOf(StatLines.DAMAGE)))
        if (hitMore != 1.0) add(FactorTrace(FactorKey.HIT, hitMore, listOf(CoreStat.HIT_DAMAGE.code)))
        if (against != 1.0) add(FactorTrace(FactorKey.AGAINST, against, listOf(CoreStat.DAMAGE_VS_AILED.code, CoreStat.DAMAGE_VS_CURSED.code) + Ailment.entries.map { it.against }))
        if (doubled != 1.0) add(FactorTrace(FactorKey.DOUBLE, doubled, listOf(CoreStat.DOUBLE_DAMAGE.code)))
        if (versus != 1.0) add(FactorTrace(FactorKey.VERSUS, versus))
        if (fateMore != 1.0) add(FactorTrace(FactorKey.FATE, fateMore))
        if (rawSum > 0) {
            add(
                FactorTrace(
                    FactorKey.DEFENCE,
                    defended / rawSum,
                    DamageType.entries.map { "STOCK_PENETRATE_${it.name}" } + CoreStat.PENETRATE_ELEMENTAL.code,
                    listOf(CoreStat.ARMOR.code, CoreStat.PHYSICAL_REDUCTION.code, CoreStat.ARMOUR_ELEMENTAL.code, CoreStat.ALL_ELEMENTAL_RESISTANCES.code, CoreStat.RESIST_MAX_ALL.code) + DamageType.entries.mapNotNull { it.resist },
                ),
            )
        }
        if (target.weakness() != 1.0) add(FactorTrace(FactorKey.SHOCK, target.weakness(), listOf(CoreStat.SHOCK_EFFECT.code), listOf(CoreStat.SHOCK_TAKEN.code)))
        if (defended > 0 && takenSum != defended) add(FactorTrace(FactorKey.TAKEN, takenSum / defended, target = listOf(CoreStat.DAMAGE_TAKEN.code, CoreStat.PHYSICAL_TAKEN.code, CoreStat.ELEMENTAL_TAKEN.code, CoreStat.CHAOS_TAKEN.code)))
        if (exposedSum > 0) add(FactorTrace(FactorKey.DECAY, exposedSum / (exposedSum - exposed)))
        if (eased != 1.0) add(FactorTrace(FactorKey.EASED, eased, target = listOf(CoreStat.SPELL_SUPPRESSION.code, CoreStat.DEFLECTION.code, CoreStat.HIT_TAKEN.code, CoreStat.BLIGHT_REDUCTION.code)))
        if (seals != 1.0) add(FactorTrace(FactorKey.SEALS, seals))
        val law = heavy * if (unruled > 0) taken.values.sum() / unruled else 1.0
        if (law != 1.0) add(FactorTrace(FactorKey.LAW, law))
        add(FactorTrace(FactorKey.TOTAL, taken.values.sum()))
    }
    pendingHit = HitTrace(striker, struck, factors, types, null, origin, exposed = exposed)
    land(me, target, kind, taken, foe, blow, body, types.associate { it.type to it.raw }, AilingHit(ailing, unmitigated, if (kind == HitKind.CRIT) multiplier else 1.0))
    if (me.alive && !blow.spell && target.body.thorns + target.body.reflect > 0) reflect(target, me, taken, foe)
    return true
}

/**
 * Урон ударами удара [blow]: у героя - увеличение рядом с увеличениями удара (как строки «против состояний» [facing],
 * [versus] - их рост), у прочих - множитель листа [body]. Недуги и урон во времени его не берут.
 */
private fun Battle.hitMore(me: Fighter, body: Combatant, blow: Blow, facing: Double, versus: Double): Double {
    val hit = body[CoreStat.HIT_DAMAGE.code]
    if (hit == 0.0) return 1.0
    if (me !== heroFighter || versus <= 0) return body.hitMore
    return versus(blow, facing + hit) / versus
}

/** Защита [target] от урона [raw] типа [type] при пробивании [pierce]: доли брони и сопротивления и что прошло сквозь них. */
private data class Defended(val armour: Double, val resist: Double, val passed: Double)

/** Броня и сопротивление цели [target] против урона [raw] типа [type] - одна формула удара и оценки избежанного. */
private fun Battle.defended(target: Combatant, type: DamageType, raw: Double, pierce: Double): Defended {
    val armour = when (type) {
        DamageType.PHYSICAL -> target.physicalMitigation(raw, rules.armour.factor)
        else -> elementalArmour(target, type, raw)
    }
    val resist = if (type == DamageType.PHYSICAL) 0.0 else target.resistTo(type, pierce)
    val passed = when (type) {
        DamageType.PHYSICAL -> raw * (1 - armour)
        else -> raw * (1 - armour) * (1 - resist)
    }.coerceAtLeast(0.0)
    return Defended(armour, resist, passed)
}

/**
 * Урон, которого удар [blow] избежал уклонением или блоком цели [target]: удар без разброса, крита и прочих
 * бросков - урон по типам на множители урона ударившего [body], затем броня, сопротивления и «получаемый урон» цели. Костей не
 * тратит: бой идёт так же, число - лишь оценка для разбора боя ([com.sperance.exileforge.core.campaign.FightStats]).
 */
private fun Battle.avoided(body: Combatant, target: Fighter, blow: Blow): Double = blow.damage.entries.sumOf { (type, base) ->
    if (base <= 0) {
        0.0
    } else {
        val passed = defended(target.body, type, base * body.nonCritMore * body.damageMore, body.penetration(type)).passed
        (passed * target.body.damageTaken(type) * target.weakness()).takeIf { it.isFinite() } ?: 0.0
    }
}

/** A critical roll of [body]'s chance — a spell's own; a lucky one gets a second. */
private fun Battle.crit(me: Fighter, body: Combatant, spell: Boolean = false): Boolean {
    val chance = body.critChance(spell)
    return checkDraw(me, RollKey.CRIT) < chance || body.luckyCrit && draw() < chance
}

/** The share of an elemental hit of [raw] the armour turns aside where some of it applies to elements. */
private fun Battle.elementalArmour(target: Combatant, type: DamageType, raw: Double): Double {
    if (target.armourElemental <= 0 || type !in DamageType.ELEMENTS || raw <= 0) return 0.0
    return (target.armour / (target.armour + rules.armour.factor * raw)).coerceAtMost(target.armourCap) * target.armourElemental
}

/** A block by [target]: life, mana and shield on block, and the hero's «blocked recently». */
private fun Battle.blocked(target: Fighter) {
    val body = target.body
    lifeBack(target, body.lifeOnBlock * body.recoveryRate)
    manaBack(target, body.manaOnBlock)
    target.shield = min(body.maxShield, target.shield + body.shieldOnBlock)
    if (target === heroFighter) blockedAt = time
    fateBlocked(target)
}

/**
 * A blow's damage by type as it meets the target: the striker's whole hit turned to one random element,
 * then the target's physical damage taken as elements.
 *
 * Доля физического урона ударившего - ещё раз другими видами.
 */
private fun Battle.converted(body: Combatant, target: Combatant, damage: Map<DamageType, Double>): Map<DamageType, Double> {
    var out = damage
    if (body.randomElementHits && out.isNotEmpty()) out = mapOf(DamageType.ELEMENTS[random.nextInt(DamageType.ELEMENTS.size)] to out.values.sum())
    val own = out[DamageType.PHYSICAL] ?: 0.0
    if (own > 0 && body.extraAs.isNotEmpty()) out = out.toMutableMap().also { extra -> body.extraAs.forEach { (type, share) -> extra.merge(type, own * share, Double::plus) } }
    val physical = out[DamageType.PHYSICAL] ?: 0.0
    val shares = target.physicalTakenAs
    if (physical <= 0 || shares.isEmpty()) return out
    val moved = out.toMutableMap()
    moved[DamageType.PHYSICAL] = physical * (1 - shares.values.sum()).coerceAtLeast(0.0)
    shares.forEach { (type, share) -> moved.merge(type, physical * share, Double::plus) }
    return moved.filterValues { it > 0 }
}

/** A block under a riposte: the hero strikes the attacker back with that share of the weapon. */
private fun Battle.counter(attacker: Fighter) {
    val riposte = heroFighter.effects.firstOrNull { it.counter > 0 } ?: return
    if (!attacker.alive) return
    strike(heroFighter, attacker, Blow(heroFighter.body.damage.mapValues { it.value * riposte.counter / 100 }, Action.SKILL, skill = riposte.source))
}

/**
 * Thorns and reflect: the struck fighter gives a flat physical blow and a share of
 * what it took back to the attacker, each part reduced by the attacker's own armour or resistance. По герою (В-5)
 * отражённое за секунду боя - не больше `caps.reflected`% его максимума здоровья ([reflectCap]): длинный бой со стражем-зеркалом
 * не убивает героя его же уроном.
 *
 * По видам: что вернулось, доли брони и сопротивления, что потерялись на атакующем, и что легло.
 */
private fun Battle.reflect(me: Fighter, attacker: Fighter, taken: Map<DamageType, Double>, foe: Int) {
    if (attacker.invulnerable) return
    val back = mutableMapOf<DamageType, Double>()
    if (me.body.thorns > 0) back[DamageType.PHYSICAL] = me.body.thorns
    if (me.body.reflect > 0) taken.forEach { (type, amount) -> back.merge(type, amount * me.body.reflect, Double::plus) }
    val types = back.map { (type, raw) ->
        val armour = if (type == DamageType.PHYSICAL) {
            attacker.body.physicalMitigation(raw, rules.armour.factor)
        } else {
            0.0
        }
        val resist = if (type == DamageType.PHYSICAL) 0.0 else attacker.body.resist(type)
        val defended = when (type) {
            DamageType.PHYSICAL -> raw * (1 - attacker.body.physicalMitigation(raw, rules.armour.factor))
            else -> raw * (1 - attacker.body.resist(type))
        }.coerceAtLeast(0.0)
        val hurt = defended * attacker.body.damageTaken(type)
        val rift = sealed(attacker) * lawTaken(attacker, type)
        TypeTrace(type, raw, raw, armour, resist, 0.0, if (rift == 1.0) hurt else hurt * rift) to defended
    }
    val mitigated = reflectCap(attacker, types.associate { (trace, _) -> trace.type to trace.dealt }.filterValues { it > 0 })
    if (mitigated.isEmpty()) return
    val striker = shot(me)
    val struck = shot(attacker)
    val absorbed = if (shieldless(attacker)) 0.0 else EnergyShield.absorbed(attacker.shield, mitigated)
    attacker.shield -= absorbed
    attacker.life = max(0.0, attacker.life - (mitigated.values.sum() - absorbed))
    attacker.lastHit = time
    val rawSum = back.values.sum()
    val defendedSum = types.sumOf { it.second }
    val total = mitigated.values.sum()
    val factors = buildList {
        add(FactorTrace(FactorKey.BASE, rawSum, listOf(CoreStat.THORNS.code, CoreStat.REFLECT.code)))
        if (rawSum > 0) {
            add(
                FactorTrace(
                    FactorKey.DEFENCE,
                    defendedSum / rawSum,
                    target = listOf(CoreStat.ARMOR.code, CoreStat.PHYSICAL_REDUCTION.code, CoreStat.ALL_ELEMENTAL_RESISTANCES.code, CoreStat.RESIST_MAX_ALL.code) + DamageType.entries.mapNotNull { it.resist },
                ),
            )
        }
        if (defendedSum > 0 && total != defendedSum) {
            add(
                FactorTrace(
                    FactorKey.TAKEN,
                    total / defendedSum,
                    target = listOf(CoreStat.DAMAGE_TAKEN.code, CoreStat.PHYSICAL_TAKEN.code, CoreStat.ELEMENTAL_TAKEN.code, CoreStat.CHAOS_TAKEN.code),
                ),
            )
        }
        add(FactorTrace(FactorKey.TOTAL, total))
    }
    val landing = Landing(0.0, absorbed, 0.0, 0.0, total - absorbed, 0.0, 0.0, 0.0, false)
    record(
        me.side, Action.REFLECT, HitKind.HIT, total, mitigated.maxBy { it.value }.key, 0.0, false, emptyList(), null, foe,
        trace = HitTrace(striker, struck, factors, types.map { it.first }, landing, origin),
    )
    if (!attacker.alive) fell(attacker, killer = me)
}

/**
 * Отражённое [dealt] по [attacker] под потолком (В-5): по герою за секунду боя - не больше `caps.reflected`% его
 * максимума здоровья, части по видам урона - одной долей; по монстру - как есть.
 */
private fun Battle.reflectCap(attacker: Fighter, dealt: Map<DamageType, Double>): Map<DamageType, Double> {
    if (attacker !== heroFighter || dealt.isEmpty()) return dealt
    val second = time.toLong()
    if (second != reflectSecond) {
        reflectSecond = second
        reflectTaken = 0.0
    }
    val room = (attacker.body.maxLife * rules.caps.reflected / 100 - reflectTaken).coerceAtLeast(0.0)
    val total = dealt.values.sum()
    val share = if (total <= room) 1.0 else room / total
    reflectTaken += total * share
    return if (share == 1.0) dealt else dealt.mapValues { it.value * share }.filterValues { it > 0 }
}

/** What a buildup gone off adds to a blow of [type] on [target]: stunned - all, frozen - the ice-breaker, electrocuted - lightning. */
internal fun Battle.exposure(target: Fighter, type: DamageType): Double {
    val rule = rules.buildup ?: return 1.0
    var more = 0.0
    if (target.stunnedUntil > time) more += rule.stun.bonus
    if (target.shatter) more += rule.freeze.bonus
    if (type == DamageType.LIGHTNING && target.electrocutedUntil > time) more += rule.electrocute.bonus
    return 1 + more / 100
}
