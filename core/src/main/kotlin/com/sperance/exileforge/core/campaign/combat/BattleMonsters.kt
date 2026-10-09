package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.lines
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.MonsterTrait
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.TraitAct
import com.sperance.exileforge.rules.content.TraitLine
import kotlin.math.max
import kotlin.math.min

// ==================== Monster traits (3.73.0) ====================

/** The traits of [fighter] whose answer is [act], with the strength of its rarity. */
private fun Battle.traits(fighter: Fighter, act: TraitAct): List<Pair<MonsterTrait, Double>> = if (fighter.side != Side.MONSTER) {
    emptyList()
} else {
    foes[fighter.index].let { foe -> foe.traits.filter { it.trigger?.act == act }.map { it to foe.traitPower } }
}

private fun List<TraitLine>.lines(power: Double): List<StatLine> = map { StatLine(it.stat, it.op, if (it.op == Op.SET) it.value else it.value * power) }

/** [me]'s weapon damage for this swing: a monster's first one grows by its first-strike traits. */
internal fun Battle.firstStrike(me: Fighter): Map<DamageType, Double> {
    if (me.side != Side.MONSTER || !swung.add(me.index)) return me.body.damage
    val more = traits(me, TraitAct.FIRST_STRIKE).sumOf { (trait, power) -> trait.trigger!!.value * power }
    if (more <= 0) return me.body.damage
    traits(me, TraitAct.FIRST_STRIKE).forEach { (trait, _) -> note(me, NoteKind.TRAIT, trait.code, more) }
    return me.body.damage.mapValues { it.value * (1 + more / 100) }
}

internal fun Battle.enrage(foe: Fighter) = traits(foe, TraitAct.ENRAGE).forEach { (trait, power) ->
    val rule = trait.trigger!!
    if (foe.life >= foe.body.maxLife * rule.threshold / 100 || !enraged.add(foe.index to trait.code)) return@forEach
    buff(foe, trait.code, rule.lines.lines(power), FOREVER)
    note(foe, NoteKind.TRAIT, trait.code)
}

/**
 * Ярость (3.95.0 - стража; 4.3.0 - любого боя): каждые `every` секунд правила вида боя [Battle.rageRule] урон всех живых врагов -
 * стража, стаи, свиты, позже вставших - больше ещё на `damage` процентов; ступени складываются до `limit`. Вставший позже
 * догоняет ступень, что уже идёт. Без бросков: бой остаётся тем же на тех же костях.
 */
internal fun Battle.rage() {
    val stacks = rageRule.stacks(time)
    if (stacks > enrage) {
        enrage = stacks
        (guardian?.let(foeFighters::get) ?: foeFighters.firstOrNull { it.alive })?.let { note(it, NoteKind.RAGE, stacks.toString()) }
    }
    if (enrage <= 0) return
    val lines = listOf(StatLine(CoreStat.DAMAGE.code, Op.MORE, rageRule.damage * enrage))
    foeFighters.forEach { foe ->
        if (!foe.alive || raged[foe.index] == enrage) return@forEach
        raged[foe.index] = enrage
        buff(foe, RAGE, lines, FOREVER)
    }
}

/** Источник баффа ярости: один на врага, каждая ступень заменяет прежнюю. */
private const val RAGE = "GUARDIAN_RAGE"

/** What a fallen foe's traits do as it falls: a burst at the hero, a rallying of the pack, a mending of it. */
internal fun Battle.lastWords(fallen: Fighter) {
    if (outcome != null) return
    val pack = foeFighters.filter { it.alive && it !== fallen }
    traits(fallen, TraitAct.RALLY).forEach { (trait, power) ->
        val rule = trait.trigger!!
        pack.forEach { buff(it, trait.code, rule.lines.lines(power), rule.duration) }
        if (pack.isNotEmpty()) note(fallen, NoteKind.TRAIT, trait.code)
    }
    traits(fallen, TraitAct.MEND).forEach { (trait, power) ->
        val share = trait.trigger!!.value * power / 100
        pack.forEach { it.life = min(it.body.maxLife, it.life + it.body.maxLife * share) }
        if (pack.isNotEmpty()) note(fallen, NoteKind.TRAIT, trait.code, share * 100)
    }
    traits(fallen, TraitAct.BURST).forEach { (trait, power) ->
        val rule = trait.trigger!!
        val type = DamageType.element(rule.element) ?: DamageType.PHYSICAL
        if (heroFighter.alive) {
            strike(
                fallen,
                foeTarget(),
                Blow(
                    mapOf(type to fallen.body.maxLife * rule.value * power / 100),
                    Action.SKILL,
                    spell = true,
                    skill = trait.code,
                    spread = false,
                    primary = false,
                ),
            )
        }
        // The blast heals nobody: whatever its leech gave back, the fallen stays down.
        fallen.life = 0.0
        fallen.shield = 0.0
    }
}

/** A monster's skills, the first ready one it has the mana for and a reason to use: a heal when hurt, a buff or a curse not already on. */
internal fun Battle.monsterCast(me: Fighter) {
    val skills = foes[me.index].skills + learned[me.index].orEmpty()
    if (skills.isEmpty() || !heroFighter.alive) return
    for (skill in skills) {
        val ready = me.readyAt.getOrPut(skill.code) { time + skill.cooldown / 2 }
        if (time < ready || me.mana + 1e-9 < skill.mana || !wanted(me, skill)) continue
        me.mana -= skill.mana
        me.readyAt[skill.code] = time + skill.cooldown / me.body.recovery(skill.spell)
        monsterSkill(me, skill)
        return
    }
}

private fun Battle.wanted(me: Fighter, skill: MonsterSkill): Boolean = when {
    skill.heal != null -> me.life < me.body.maxLife * 0.6
    skill.buff != null -> me.effects.none { it.source == skill.code }
    skill.curse != null -> !heroFighter.body.immuneCurse && heroFighter.effects.none { it.source == skill.code }
    skill.manaBurn > 0 && skill.hit == null -> heroFighter.mana > 0
    else -> true
}

/** What a monster's skill does: a share of its own swing in the skill's element, a buff, a curse, a heal, a burn of the hero's mana. */
private fun Battle.monsterSkill(me: Fighter, skill: MonsterSkill) {
    val hero = heroFighter
    skill.hit?.let { hit ->
        val element = DamageType.element(hit.element)
        val share = (hit.weapon?.at(1) ?: 100.0) / 100
        val damage = me.body.damage.mapValues { it.value * share }.toMutableMap()
        // A caster's spell carries its spell damage in the skill's element.
        val magical = me.body[CoreStat.ATTACK_MAGICAL.code]
        if (skill.spell && magical > 0) damage.merge(element ?: me.body.leading, magical * share, Double::plus)
        val convert = (hit.convert?.at(1) ?: 0.0).coerceIn(0.0, 100.0) / 100
        if (element != null && convert > 0) {
            val total = damage.values.sum()
            damage.replaceAll { _, value -> value * (1 - convert) }
            damage.merge(element, total * convert, Double::plus)
        }
        val leading = damage.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
        // A single-target hit picks its target as a swing does (3.71.0): a tanking pet takes it; a hit on several lands on the hero
        val target = if (hit.targets <= 1) foeTarget() else hero
        strike(
            me,
            target,
            Blow(
                damage,
                Action.SKILL,
                skill.spell,
                skill.code,
                stun = hit.stun?.at(1) ?: 0.0,
                ailments = hit.ailments.mapNotNull { resolve(it, element ?: leading, 1) },
            ),
        )
    }
    skill.buff?.let { buff ->
        buff(me, skill.code, buff.stats.lines(1, 1 + me.body[CoreStat.WARCRY_EFFECT.code] / 100), buff.duration)
        record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, me.index, skill.code, onSelf = true)
    }
    skill.curse?.let { curse ->
        lay(hero, TimedEffect(EffectKind.CURSE, skill.code, curse.stats.lines(1), time + curse.duration, curse.duration))
        record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, me.index, skill.code)
    }
    skill.heal?.let { heal ->
        val before = me.life
        me.life = min(me.body.maxLife, me.life + me.body.maxLife * (heal.life?.at(1) ?: 0.0) / 100)
        record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, me.life - before, false, emptyList(), null, me.index, skill.code, onSelf = true)
    }
    if (skill.manaBurn > 0 && skill.hit == null) {
        hero.mana = max(0.0, hero.mana - manaCap() * skill.manaBurn / 100)
        record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, me.index, skill.code)
    }
}
