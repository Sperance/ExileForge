package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.run.ProvokeView
import com.sperance.exileforge.rules.content.CoreStat

/** Устрашение героя для подсказки боя; null - героя без устрашения. */
fun Battle.provokeView(): ProvokeView? = heroProvoke().takeIf { it > 0 }?.let { chance ->
    val rule = rules.provoke
    ProvokeView(chance, rule.duration, rule.cut(chance, false), rule.cut(chance, true), rule.immunity, rule.bossImmunity)
}

/** Источник плитки устрашения на враге: строк не несёт, только показывает, сколько ещё оно держится. */
const val PROVOKED = "PROVOKED"

/** Сила устрашения героя: `STOCK_INTIMIDATE` листа, не выше `combat.caps.provoke`. */
internal fun Battle.heroProvoke(): Double = heroFighter.body[CoreStat.INTIMIDATE.code].coerceIn(0.0, rules.caps.provoke)

/**
 * Удар героя по своей основной цели [target] с шансом силы устрашения устрашает её на `combat.provoke.duration` секунд; повторно -
 * не раньше невосприимчивости после конца. Кость тянется только у героя с устрашением: бой без него лишних костей не тянет.
 */
internal fun Battle.provoke(target: Fighter) {
    if (target.side != Side.MONSTER || target.provoked || time < target.provokeFree) return
    val provoke = heroProvoke()
    if (provoke <= 0 || random.nextDouble() * 100 >= provoke) return
    val rule = rules.provoke
    target.provoke = provoke
    target.provokedUntil = time + rule.duration
    target.provokeFree = target.provokedUntil + rule.immunity(target.chief)
    lay(target, TimedEffect(EffectKind.HINDER, PROVOKED, emptyList(), target.provokedUntil, rule.duration))
    note(target, NoteKind.PROVOKED, "", rule.duration)
}

/** Доля удара [attacker] по [target], что проходит: устрашённый враг бьёт героя слабее (боссы - меньше). */
internal fun Battle.provokedTaken(attacker: Fighter, target: Fighter): Double = if (target !== heroFighter || attacker.side != Side.MONSTER || !attacker.provoked) 1.0 else 1 - rules.provoke.cut(attacker.provoke, attacker.chief) / 100

/**
 * Умения устрашённого [foe] за шаг [dt]: обычный их не применяет - перезарядки стоят; босс применяет, но его перезарядки идут
 * медленнее в `bossCooldownSlow` раз. True - умения сейчас под запретом.
 */
internal fun Battle.provokedSkills(foe: Fighter, dt: Double): Boolean {
    if (!foe.provoked) return false
    val boss = foe.chief
    val held = if (boss) dt * (1 - 1 / rules.provoke.bossCooldownSlow) else dt
    foe.readyAt.replaceAll { _, at -> at + held }
    return !boss
}
