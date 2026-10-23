package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterSkill
import kotlin.math.max

/**
 * Удар умения монстра для листа умения: [raw] - урон по типам его листом, как его считает бой без разброса и
 * крита; [taken] - сколько из него дойдёт до героя после брони и сопротивлений; null - героя для счёта нет.
 */
data class SkillDamage(val raw: Map<DamageType, Double>, val taken: Double?) {
    val total: Double get() = raw.values.sum()
}

/**
 * Урон удара умения [skill] врага [foe] по видам до защит цели - одна функция боя и листа умения: доля замаха умения, у чар - урон
 * заклинаний в стихии умения, перевод в стихию и урон умений монстра (`STOCK_SKILL_DAMAGE`, сила «Мастер умений» и слабость
 * «Неумелый»). Умение без удара - null.
 */
fun monsterSkillBlow(skill: MonsterSkill, foe: Combatant): Map<DamageType, Double>? {
    val hit = skill.hit ?: return null
    val element = DamageType.element(hit.element)
    val share = (hit.weapon?.at(1) ?: 100.0) / 100 * max(0.0, 1 + foe[CoreStat.SKILL_DAMAGE.code] / 100)
    val damage = foe.damage.mapValues { it.value * share }.toMutableMap()
    val magical = foe[CoreStat.ATTACK_MAGICAL.code]
    if (skill.spell && magical > 0) damage.merge(element ?: foe.leading, magical * share, Double::plus)
    val convert = (hit.convert?.at(1) ?: 0.0).coerceIn(0.0, 100.0) / 100
    if (element != null && convert > 0) {
        val total = damage.values.sum()
        damage.replaceAll { _, value -> value * (1 - convert) }
        damage.merge(element, total * convert, Double::plus)
    }
    return damage
}

/** Удар [skill] врага [foe] по герою [hero] - тот же счёт, что у боя ([monsterSkillBlow]), без случайностей; null - умение не бьёт. */
fun monsterSkillDamage(skill: MonsterSkill, foe: Combatant, hero: Combatant?): SkillDamage? {
    val damage = monsterSkillBlow(skill, foe) ?: return null
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
