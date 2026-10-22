package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.run.IntimidateView
import com.sperance.exileforge.rules.content.CoreStat

/** Устрашение героя для подсказки боя; null - героя без устрашения. */
fun Battle.intimidateView(): IntimidateView? = heroIntimidate().takeIf { it > 0 }?.let { chance ->
    val rule = rules.intimidate
    IntimidateView(chance, rule.duration, rule.cut(chance, false), rule.cut(chance, true), rule.immunity, rule.bossImmunity)
}

/** Источник плитки устрашения на враге: строк не несёт, только показывает, сколько ещё оно держится. */
const val INTIMIDATED = "INTIMIDATED"

/** Сила устрашения героя: `STOCK_INTIMIDATE` листа, не выше `combat.caps.intimidate`. */
internal fun Battle.heroIntimidate(): Double = heroFighter.body[CoreStat.INTIMIDATE.code].coerceIn(0.0, rules.caps.intimidate)

/**
 * Удар героя по своей основной цели [target] с шансом силы устрашения устрашает её на `combat.intimidate.duration` секунд; повторно -
 * не раньше невосприимчивости после конца. Кость тянется только у героя с устрашением: бой без него лишних костей не тянет.
 */
internal fun Battle.intimidate(target: Fighter) {
    if (target.side != Side.MONSTER || target.intimidated || time < target.intimidateFree) return
    val chance = heroIntimidate()
    if (chance <= 0 || random.nextDouble() * 100 >= chance) return
    val rule = rules.intimidate
    target.intimidation = chance
    target.intimidatedUntil = time + rule.duration
    target.intimidateFree = target.intimidatedUntil + rule.immunity(target.chief)
    lay(target, TimedEffect(EffectKind.HINDER, INTIMIDATED, emptyList(), target.intimidatedUntil, rule.duration))
    note(target, NoteKind.INTIMIDATED, "", rule.duration)
}

/** Доля удара [attacker] по [target], что проходит: устрашённый враг бьёт героя слабее (боссы - меньше). */
internal fun Battle.intimidatedTaken(attacker: Fighter, target: Fighter): Double = if (target !== heroFighter || attacker.side != Side.MONSTER || !attacker.intimidated) 1.0 else 1 - rules.intimidate.cut(attacker.intimidation, attacker.chief) / 100

/**
 * Умения устрашённого [foe] за шаг [dt]: обычный их не применяет - перезарядки стоят; босс применяет, но его перезарядки идут
 * медленнее в `bossCooldownSlow` раз. True - умения сейчас под запретом.
 */
internal fun Battle.intimidatedSkills(foe: Fighter, dt: Double): Boolean {
    if (!foe.intimidated) return false
    val boss = foe.chief
    val held = if (boss) dt * (1 - 1 / rules.intimidate.bossCooldownSlow) else dt
    foe.readyAt.replaceAll { _, at -> at + held }
    return !boss
}
