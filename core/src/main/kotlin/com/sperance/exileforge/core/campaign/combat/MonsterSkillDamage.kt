package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterSkill

/**
 * Удар умения монстра для листа умения (3.92.0): [raw] - урон по типам его листом, как его считает бой без разброса и
 * крита; [taken] - сколько из него дойдёт до героя после брони и сопротивлений; null - героя для счёта нет.
 */
data class SkillDamage(val raw: Map<DamageType, Double>, val taken: Double?) {
    val total: Double get() = raw.values.sum()
}

/** Удар [skill] врага [foe] по герою [hero] - тот же счёт, что у боя ([monsterSkill]), без случайностей; null - умение не бьёт. */
fun monsterSkillDamage(skill: MonsterSkill, foe: Combatant, hero: Combatant?): SkillDamage? {
    val hit = skill.hit ?: return null
    val element = DamageType.element(hit.element)
    val share = (hit.weapon?.at(1) ?: 100.0) / 100
    val damage = foe.damage.mapValues { it.value * share }.toMutableMap()
    val magical = foe[CoreStat.ATTACK_MAGICAL.code]
    if (skill.spell && magical > 0) damage.merge(element ?: foe.leading, magical * share, Double::plus)
    val convert = (hit.convert?.at(1) ?: 0.0).coerceIn(0.0, 100.0) / 100
    if (element != null && convert > 0) {
        val total = damage.values.sum()
        damage.replaceAll { _, value -> value * (1 - convert) }
        damage.merge(element, total * convert, Double::plus)
    }
    val raw = damage.filterValues { it > 0 }.mapValues { it.value * foe.damageMore }
    val taken = hero?.let { body ->
        raw.entries.sumOf { (type, amount) ->
            if (type == DamageType.PHYSICAL) {
                amount * (1 - body.physicalMitigation(amount, foe.rules.armour.factor))
            } else {
                amount * (1 - body.resistTo(type))
            }.coerceAtLeast(0.0) * body.damageTaken(type)
        }
    }
    return SkillDamage(raw, taken)
}
