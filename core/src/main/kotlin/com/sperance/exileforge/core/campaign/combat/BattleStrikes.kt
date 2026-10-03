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
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.SkillEvent
import kotlin.math.max
import kotlin.math.min

// ==================== The strike (2.78.0) ====================

/**
 * One blow of [me] at [target] — a weapon's swing, a skill's hit, a spell: evaded unless a spell,
 * blocked, or landed and maybe critical; then armour, resistance, shock and the lone wolf's share.
 */
internal fun Battle.strike(me: Fighter, target: Fighter, blow: Blow): Boolean {
    val body = blow.body ?: me.body
    val foe = if (me.side == Side.MONSTER) me.index else target.index
    val petLine = isPet(me) || isPet(target)
    if (target.invulnerable) {
        record(me.side, blow.action, HitKind.BLOCKED, 0.0, null, 0.0, false, emptyList(), null, foe, blow.skill, pet = petLine)
        return false
    }
    val sure = me === heroFighter && nextCrit
    // 3.37.0: every draw of this blow goes on its tape, and the shots are taken before anything changes.
    tape = mutableListOf()
    val striker = shot(me, blow)
    val struck = shot(target)
    val evade = evasion(me, target)
    val blockChance = if (blow.spell) target.body.spellBlock else target.body.block
    val kind = when {
        target.frozen() -> if (sure || crit(body, blow.spell)) HitKind.CRIT else HitKind.HIT
        !blow.spell && draw(RollKey.EVADE, evade) < evade -> HitKind.EVADED
        draw(RollKey.BLOCK, blockChance) < blockChance -> HitKind.BLOCKED
        sure || crit(body, blow.spell) -> HitKind.CRIT
        else -> HitKind.HIT
    }
    if (sure && kind == HitKind.CRIT) nextCrit = false
    if (me.side == Side.MONSTER) lastStriker = me.index
    if (kind == HitKind.EVADED || kind == HitKind.BLOCKED) {
        record(
            me.side, blow.action, kind, 0.0, null, 0.0, false, emptyList(), null, foe, blow.skill,
            trace = HitTrace(striker, struck, takeTape(), emptyList(), emptyList(), null, origin), pet = petLine,
        )
        if (kind == HitKind.BLOCKED) blocked(target)
        if (target === heroFighter) {
            trigger(if (kind == HitKind.EVADED) SkillEvent.EVADE else SkillEvent.BLOCK, me)
            powers.fire(if (kind == HitKind.EVADED) PowerEvent.EVADE else PowerEvent.BLOCK, PowerMoment(me))
            if (kind == HitKind.BLOCKED) counter(me)
        }
        return false
    }
    // The lone wolf's share rides the blow itself, so the ailments it brings carry it once and no more.
    val lone = when {
        !loneWolf -> 1.0
        me.side == Side.HERO -> 1 + rules.loneWolf.dealt / 100
        else -> 1 - rules.loneWolf.taken / 100
    }
    // Server 1.32.0: a critical strike no heavier than a hit on one who takes none, and the non-critical ones more or less.
    val multiplier = if (kind == HitKind.CRIT) max(1.0, body.critMultiplier(blow.spell) + target.body.critTaken) else body.nonCritMore
    // 3.35.0: a double blow, the hero's lines against the target's state, and what suppression or deflection lets through.
    val doubled = if (body.doubleDamage > 0 && draw(RollKey.DOUBLE, body.doubleDamage) < body.doubleDamage) 2.0 else 1.0
    // Server 1.57.0: the lines against the target's state are increases beside the blow's own, not a multiplier of their own.
    val facing = if (me === heroFighter) max(0.0, model.against(states(target))) else 0.0
    val versus = versus(blow, facing)
    val eased = eased(target, blow) * target.body.hitTaken
    // Server 0.66.0: a penetrating blow ignores part of the resistance, an ailed target takes more, and
    // "damage taken" of the target scales what got through; server 0.69.0: so does a curse on it.
    val against = body.damageAgainst(target.ailments.map { it.ailment }) * (if (target.cursed) 1 + max(0.0, body["STOCK_DAMAGE_VS_CURSED"]) / 100 else 1.0)
    // Server 1.32.0: the element the target resists least is pierced deeper, the one it resists most may hurt it less.
    val weakest = if (body.lowestResistPenetrate > 0) DamageType.ELEMENTS.minBy { target.body.resistTo(it) } else null
    val strongest = if (target.body.highestResistElementTaken != 0.0) DamageType.ELEMENTS.maxBy { target.body.resistTo(it) } else null
    val types = mutableListOf<TypeTrace>()
    var spreadSum = 0.0
    var takenSum = 0.0
    var defended = 0.0
    val taken = converted(body, target.body, blow.damage.filterValues { it > 0 }).mapValues { (type, base) ->
        val spread = if (blow.spread) 1 + (random.nextDouble() * 2 - 1) * rules.variance / 100 else 1.0
        val raw = base * spread * multiplier * against * body.damageMore * doubled * versus
        val pierce = body.penetration(type) + if (type == weakest) body.lowestResistPenetrate else 0.0
        val armour = when (type) {
            DamageType.PHYSICAL -> target.body.physicalMitigation(raw, rules.armour.factor)
            else -> elementalArmour(target.body, type, raw)
        }
        val resist = if (type == DamageType.PHYSICAL) 0.0 else target.body.resistTo(type, pierce)
        val defence = when (type) {
            DamageType.PHYSICAL -> raw * (1 - armour)
            else -> raw * (1 - armour) * (1 - resist)
        }.coerceAtLeast(0.0)
        val typeTaken = target.body.damageTaken(type) * (if (type == strongest) max(0.0, 1 + target.body.highestResistElementTaken / 100) else 1.0)
        val dealt = defence * target.weakness() * lone * typeTaken * eased * exposure(target, type)
        spreadSum += base * spread
        takenSum += defence * typeTaken
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
                DamageType.entries.map { it.attack } + listOf("STOCK_SKILL_DAMAGE", "STOCK_SPELL_DAMAGE") +
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
                    listOf(if (blow.spell) "STOCK_SPELL_CRITICAL_MULTIPLIER" else "STOCK_CRITICAL_MULTIPLIER", "STOCK_CRITICAL_DAMAGE"),
                    listOf("STOCK_CRITICAL_TAKEN"),
                ),
            )
        } else if (multiplier != 1.0) {
            add(FactorTrace(FactorKey.NON_CRIT, multiplier, listOf("STOCK_NON_CRIT_DAMAGE")))
        }
        add(FactorTrace(FactorKey.DAMAGE, body.damageMore, listOf(StatLines.DAMAGE)))
        if (against != 1.0) add(FactorTrace(FactorKey.AGAINST, against, listOf("STOCK_DAMAGE_VS_AILED", "STOCK_DAMAGE_VS_CURSED") + Ailment.entries.map { it.against }))
        if (doubled != 1.0) add(FactorTrace(FactorKey.DOUBLE, doubled, listOf("STOCK_DOUBLE_DAMAGE")))
        if (versus != 1.0) add(FactorTrace(FactorKey.VERSUS, versus))
        if (rawSum > 0) {
            add(
                FactorTrace(
                    FactorKey.DEFENCE,
                    defended / rawSum,
                    DamageType.entries.map { "STOCK_PENETRATE_${it.name}" } + "STOCK_PENETRATE_ELEMENTAL",
                    listOf("STOCK_ARMOR", "STOCK_PHYSICAL_REDUCTION", "STOCK_ARMOUR_ELEMENTAL", "STOCK_RESIST_ALL", "STOCK_RESIST_MAX_ALL") + DamageType.entries.mapNotNull { it.resist },
                ),
            )
        }
        if (target.weakness() != 1.0) add(FactorTrace(FactorKey.SHOCK, target.weakness(), listOf("STOCK_SHOCK_EFFECT"), listOf("STOCK_SHOCK_TAKEN")))
        if (lone != 1.0) add(FactorTrace(FactorKey.LONE_WOLF, lone))
        if (defended > 0 && takenSum != defended) add(FactorTrace(FactorKey.TAKEN, takenSum / defended, target = listOf("STOCK_DAMAGE_TAKEN", "STOCK_PHYSICAL_TAKEN", "STOCK_ELEMENTAL_TAKEN", "STOCK_CHAOS_TAKEN")))
        if (eased != 1.0) add(FactorTrace(FactorKey.EASED, eased, target = listOf("STOCK_SPELL_SUPPRESSION", "STOCK_DEFLECTION", "STOCK_HIT_TAKEN")))
        add(FactorTrace(FactorKey.TOTAL, taken.values.sum()))
    }
    pendingHit = HitTrace(striker, struck, emptyList(), factors, types, null, origin)
    land(me, target, kind, taken, foe, blow, body)
    if (me.alive && target.body.thorns + target.body.reflect > 0) reflect(target, me, taken, foe)
    return true
}

/** A critical roll of [body]'s chance — a spell's own (server 1.56.0); a lucky one (server 1.32.0) gets a second. */
private fun Battle.crit(body: Combatant, spell: Boolean = false): Boolean {
    val chance = body.critChance(spell)
    return draw(RollKey.CRIT, chance) < chance || body.luckyCrit && draw(RollKey.CRIT_LUCKY, chance) < chance
}

/** The share of an elemental hit of [raw] the armour turns aside where some of it applies to elements (3.35.0). */
private fun Battle.elementalArmour(target: Combatant, type: DamageType, raw: Double): Double {
    if (target.armourElemental <= 0 || type !in DamageType.ELEMENTS || raw <= 0) return 0.0
    return (target.armour / (target.armour + rules.armour.factor * raw)).coerceAtMost(target.armourCap) * target.armourElemental
}

/** A block by [target] (3.35.0): life, mana and shield on block, and the hero's «blocked recently». */
private fun Battle.blocked(target: Fighter) {
    val body = target.body
    target.life = min(body.maxLife, target.life + body.lifeOnBlock * body.recoveryRate)
    target.mana = min(manaCap(target), target.mana + body.manaOnBlock)
    target.shield = min(body.maxShield, target.shield + body.shieldOnBlock)
    if (target === heroFighter) blockedAt = time
}

/**
 * A blow's damage by type as it meets the target (server 1.32.0): the striker's whole hit turned to one random element,
 * then the target's physical damage taken as elements.
 */
private fun Battle.converted(body: Combatant, target: Combatant, damage: Map<DamageType, Double>): Map<DamageType, Double> {
    var out = damage
    if (body.randomElementHits && out.isNotEmpty()) out = mapOf(DamageType.ELEMENTS[random.nextInt(DamageType.ELEMENTS.size)] to out.values.sum())
    // 3.35.0: a share of the striker's physical damage dealt again as other types.
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

/** A block under a riposte (2.78.0): the hero strikes the attacker back with that share of the weapon. */
private fun Battle.counter(attacker: Fighter) {
    val riposte = heroFighter.effects.firstOrNull { it.counter > 0 } ?: return
    if (!attacker.alive) return
    strike(heroFighter, attacker, Blow(heroFighter.body.damage.mapValues { it.value * riposte.counter / 100 }, Action.SKILL, skill = riposte.source))
}

/**
 * Thorns and reflect (server 0.66.0): the struck fighter gives a flat physical blow and a share of
 * what it took back to the attacker, each part reduced by the attacker's own armour or resistance.
 */
private fun Battle.reflect(me: Fighter, attacker: Fighter, taken: Map<DamageType, Double>, foe: Int) {
    if (attacker.invulnerable) return
    val back = mutableMapOf<DamageType, Double>()
    if (me.body.thorns > 0) back[DamageType.PHYSICAL] = me.body.thorns
    if (me.body.reflect > 0) taken.forEach { (type, amount) -> back.merge(type, amount * me.body.reflect, Double::plus) }
    // Per type (3.70.0): what came back, the armour's and the resistance's shares it lost on the attacker, and what landed.
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
        TypeTrace(type, raw, raw, armour, resist, 0.0, defended * attacker.body.damageTaken(type)) to defended
    }
    val mitigated = types.associate { (trace, _) -> trace.type to trace.dealt }.filterValues { it > 0 }
    if (mitigated.isEmpty()) return
    val striker = shot(me)
    val struck = shot(attacker)
    val chaos = mitigated[DamageType.CHAOS] ?: 0.0
    val shielded = mitigated.values.sum() - chaos
    val absorbed = min(attacker.shield, shielded)
    attacker.shield -= absorbed
    attacker.life = max(0.0, attacker.life - (shielded - absorbed) - chaos)
    attacker.lastHit = time
    val rawSum = back.values.sum()
    val defendedSum = types.sumOf { it.second }
    val total = mitigated.values.sum()
    val factors = buildList {
        add(FactorTrace(FactorKey.BASE, rawSum, listOf("STOCK_THORNS", "STOCK_REFLECT")))
        if (rawSum > 0) {
            add(
                FactorTrace(
                    FactorKey.DEFENCE,
                    defendedSum / rawSum,
                    target = listOf("STOCK_ARMOR", "STOCK_PHYSICAL_REDUCTION", "STOCK_RESIST_ALL", "STOCK_RESIST_MAX_ALL") + DamageType.entries.mapNotNull { it.resist },
                ),
            )
        }
        if (defendedSum > 0 && total != defendedSum) {
            add(
                FactorTrace(
                    FactorKey.TAKEN,
                    total / defendedSum,
                    target = listOf("STOCK_DAMAGE_TAKEN", "STOCK_PHYSICAL_TAKEN", "STOCK_ELEMENTAL_TAKEN", "STOCK_CHAOS_TAKEN"),
                ),
            )
        }
        add(FactorTrace(FactorKey.TOTAL, total))
    }
    val landing = Landing(0.0, absorbed, 0.0, 0.0, shielded - absorbed + chaos, 0.0, 0.0, 0.0, false)
    record(
        me.side, Action.REFLECT, HitKind.HIT, total, mitigated.maxBy { it.value }.key, 0.0, false, emptyList(), null, foe,
        trace = HitTrace(striker, struck, emptyList(), factors, types.map { it.first }, landing, origin), pet = isPet(me) || isPet(attacker),
    )
    if (!attacker.alive) fell(attacker, killer = me)
}

/** What a buildup gone off adds to a blow of [type] on [target] (3.78.0): stunned - all, frozen - the ice-breaker, electrocuted - lightning. */
internal fun Battle.exposure(target: Fighter, type: DamageType): Double {
    val rule = rules.buildup ?: return 1.0
    var more = 0.0
    if (target.stunnedUntil > time) more += rule.stun.bonus
    if (target.shatter) more += rule.freeze.bonus
    if (type == DamageType.LIGHTNING && target.electrocutedUntil > time) more += rule.electrocute.bonus
    return 1 + more / 100
}
