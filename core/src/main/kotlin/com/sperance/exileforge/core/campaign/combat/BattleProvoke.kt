package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.run.ProvokeView
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity

// ==================== Провокация героя (4.12.x, STAT-DEF-05) ====================

/** Провокация героя для подсказки боя; null - героя без провокации. */
fun Battle.provokeView(): ProvokeView? = heroProvoke().takeIf { it > 0 }?.let { chance ->
    val rule = rules.provoke
    ProvokeView(chance, rule.duration, rule.cut(chance, false), rule.cut(chance, true), rule.immunity, rule.bossImmunity)
}

/** Источник плитки провокации на враге: строк не несёт, только показывает, сколько ещё она держится. */
const val PROVOKED = "PROVOKED"

/** Сила провокации героя: `STOCK_PROVOKE` листа, не выше `combat.caps.provoke`. */
internal fun Battle.heroProvoke(): Double = heroFighter.body[CoreStat.PROVOKE.code].coerceIn(0.0, rules.caps.provoke)

/** Босс для провокации: уникальный враг - стражи, боссы карт, башни и Разлома. */
private fun Battle.bossFoe(foe: Fighter): Boolean = foes[foe.index].rarity >= MonsterRarity.UNIQUE

/**
 * Удар героя по своей основной цели [target] с шансом силы провокации провоцирует её на `combat.provoke.duration` секунд; повторно -
 * не раньше невосприимчивости после конца. Кость тянется только у героя с провокацией: бой без неё катится как прежде.
 */
internal fun Battle.provoke(target: Fighter) {
    if (target.side != Side.MONSTER || target.provoked || time < target.provokeFree) return
    val provoke = heroProvoke()
    if (provoke <= 0 || random.nextDouble() * 100 >= provoke) return
    val rule = rules.provoke
    target.provoke = provoke
    target.provokedUntil = time + rule.duration
    target.provokeFree = target.provokedUntil + rule.immunity(bossFoe(target))
    lay(target, TimedEffect(EffectKind.HINDER, PROVOKED, emptyList(), target.provokedUntil, rule.duration))
    note(target, NoteKind.PROVOKED, "", rule.duration)
}

/** Доля удара [attacker] по [target], что проходит: спровоцированный враг бьёт героя слабее (боссы - меньше). */
internal fun Battle.provokedTaken(attacker: Fighter, target: Fighter): Double = if (target !== heroFighter || attacker.side != Side.MONSTER || !attacker.provoked) 1.0 else 1 - rules.provoke.cut(attacker.provoke, bossFoe(attacker)) / 100

/**
 * Умения спровоцированного [foe] за шаг [dt]: обычный их не применяет - перезарядки стоят; босс применяет, но его перезарядки идут
 * медленнее в `bossCooldownSlow` раз. True - умения сейчас под запретом.
 */
internal fun Battle.provokedSkills(foe: Fighter, dt: Double): Boolean {
    if (!foe.provoked) return false
    val boss = bossFoe(foe)
    val held = if (boss) dt * (1 - 1 / rules.provoke.bossCooldownSlow) else dt
    foe.readyAt.replaceAll { _, at -> at + held }
    return !boss
}
