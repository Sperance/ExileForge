package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.sheet.CastTime
import com.sperance.exileforge.rules.sheet.lines
import kotlin.math.max
import kotlin.math.min

/**
 * Ярость (стража; любого боя): каждые `every` секунд правила вида боя [Battle.rageRule] урон всех живых врагов -
 * стража, стаи, свиты, позже вставших - больше ещё на `damage` процентов; ступени складываются до `limit`. Вставший позже
 * догоняет ступень, что уже идёт; раунд боя и смена подкреплений начинают её заново, а ступень, что уже
 * лежит на враге, тоже снимают ([calm]). Без бросков: бой остаётся тем же на тех же костях.
 */
internal fun Battle.rage() {
    val stacks = rageRule.stacks(rageClock)
    if (stacks > enrage) {
        enrage = stacks
        (guardian?.let(foeFighters::get) ?: foeFighters.firstOrNull { it.alive })?.let { note(it, NoteKind.RAGE, stacks.toString()) }
    }
    if (enrage <= 0) return
    val lines = listOf(StatLine(CoreStat.DAMAGE.code, Op.MORE, rageRule.damage * enrage))
    foeFighters.forEach { foe ->
        if (!foe.alive || (raged[foe.index] ?: 0) >= enrage) return@forEach
        raged[foe.index] = enrage
        buff(foe, RAGE, lines, FOREVER, origin = EffectOrigin.Rage)
    }
}

/**
 * Смена поля начинает ярость заново и для выживших: их ступень снимается вместе с баффом, следующие ступени
 * они копят с новыми врагами.
 */
internal fun Battle.calm() {
    raged.clear()
    foeFighters.forEach { foe -> if (foe.effects.removeAll { it.source == RAGE }) remake(foe) }
}

/** Источник баффа ярости: один на врага, каждая ступень заменяет прежнюю. */
private const val RAGE = "GUARDIAN_RAGE"

/**
 * A monster's skills, the first ready one it has a reason to use: a heal when hurt, a buff or a curse not already on.
 * Умение применяется своё время ([CastTime]): удар ждёт, оглушение и заморозка прерывают; перезарядка - в конце.
 * Маны умения монстров не тратят: их держит только перезарядка.
 */
internal fun Battle.monsterCast(me: Fighter) {
    if (me.casting != null) return
    val skills = foes[me.index].skills + learned[me.index].orEmpty()
    if (skills.isEmpty() || !heroFighter.alive) return
    for (skill in skills) {
        val ready = me.readyAt.getOrPut(skill.code) { time + skill.cooldown / 2 * me.slow() }
        if (time < ready || !wanted(me, skill)) continue
        beginCast(me, skill.code, skill.code, CastTime.of(skill, me.body, rules) * me.slow()) { monsterApplied(me, skill) }
        return
    }
}

/** Применение монстра [me] закончено: перезарядка (холод и замедление её растягивают) и действие. */
private fun Battle.monsterApplied(me: Fighter, skill: MonsterSkill) {
    if (!heroFighter.alive) return
    me.readyAt[skill.code] = time + skill.cooldown / me.body.recovery * me.slow()
    monsterSkill(me, skill)
}

/** Слоёв проклятия умения [code] на бойце: 0 - не лежит; предел слоёв (`SkillCurse.layers`, не меньше 1) держит проверка контента. */
internal fun Fighter.curseLayers(code: String): Int = effects.firstOrNull { it.kind == EffectKind.CURSE && it.source == code }?.stacks ?: 0

/**
 * Есть ли у монстра [me] повод применить готовое умение [skill]: лечение - раненым, благо - если ещё не лежит, проклятие - если не
 * лежит или его слоёв меньше предела ([com.sperance.exileforge.rules.content.SkillCurse.layers]).
 */
internal fun Battle.wanted(me: Fighter, skill: MonsterSkill): Boolean = when {
    skill.heal != null -> me.life < me.body.maxLife * 0.6
    skill.buff != null -> me.effects.none { it.source == skill.code }
    skill.curse != null -> !heroFighter.body.immuneCurse && skill.curse?.let { heroFighter.curseLayers(skill.code) < it.layers } == true
    skill.manaBurn > 0 && skill.hit == null -> heroFighter.mana > 0
    else -> true
}

/**
 * What a monster's skill does: a share of its own swing in the skill's element, a buff, a curse, a heal, a burn of the hero's mana.
 * Проклятие со слоями кладёт ещё слой поверх своего же: строки - на число слоёв, срок - заново.
 *
 * Чары заклинателя несут его урон заклинаний в стихии умения. «Эффект кличей» монстра растит только клич, не всякое благо.
 */
private fun Battle.monsterSkill(me: Fighter, skill: MonsterSkill) {
    val hero = heroFighter
    skill.hit?.let { hit ->
        val element = DamageType.element(hit.element)
        val share = (hit.weapon?.at(1) ?: 100.0) / 100
        val damage = me.body.damage.mapValues { it.value * share }.toMutableMap()
        val magical = me.body[CoreStat.ATTACK_MAGICAL.code]
        if (skill.spell && magical > 0) damage.merge(element ?: me.body.leading, magical * share, Double::plus)
        val convert = (hit.convert?.at(1) ?: 0.0).coerceIn(0.0, 100.0) / 100
        if (element != null && convert > 0) {
            val total = damage.values.sum()
            damage.replaceAll { _, value -> value * (1 - convert) }
            damage.merge(element, total * convert, Double::plus)
        }
        val leading = damage.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
        val target = hero
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
        val effect = if (skill.warcry) 1 + me.body[CoreStat.WARCRY_EFFECT.code] / 100 else 1.0
        buff(me, skill.code, buff.stats.lines(1, effect), buff.duration)
        record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, me.index, skill.code, onSelf = true)
    }
    skill.curse?.let { curse ->
        val layers = (hero.curseLayers(skill.code) + 1).coerceAtMost(curse.layers)
        lay(hero, TimedEffect(EffectKind.CURSE, skill.code, curse.stats.lines(1, layers.toDouble()), time + curse.duration, curse.duration, stacks = layers))
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
