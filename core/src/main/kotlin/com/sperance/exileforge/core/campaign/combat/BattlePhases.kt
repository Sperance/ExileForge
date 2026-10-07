package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.PhaseStep
import kotlin.math.max
import kotlin.math.min

/** Шаг фазы босса в бою (3.92.0): шаг правил и умение, которое он даёт, уже найденное среди умений монстров. */
data class FoePhase(val step: PhaseStep, val skill: MonsterSkill? = null)

/** Источник строк шага [step] фазы [code] на боссе: у каждого шага свой, так что два шага одного порога не заменяют друг друга. */
internal fun phaseSource(code: String, step: Int) = "$code#$step"

/**
 * Фазы босса (3.92.0): каждый шаг, до чьего порога опустилось здоровье [foe], срабатывает раз за бой - строки, умение, свита,
 * удар, лечение, барьер, неуязвимость - и пишется в журнал заметкой фазы.
 */
internal fun Battle.phase(foe: Fighter) {
    val plan = foes[foe.index]
    if (plan.phases.isEmpty()) return
    val code = plan.phase.orEmpty()
    plan.phases.forEachIndexed { i, phase ->
        val step = phase.step
        if (foe.life > foe.body.maxLife * step.at / 100 || !phased.add(foe.index to i)) return@forEachIndexed
        val max = foe.body.maxLife
        if (step.lines.isNotEmpty()) {
            buff(foe, phaseSource(code, i), step.lines.map { StatLine(it.stat, it.op, it.value) }, if (step.duration > 0) step.duration else FOREVER)
        }
        phase.skill?.let { learned.getOrPut(foe.index) { mutableListOf() } += it }
        if (step.heal > 0) foe.life = min(max, foe.life + max * step.heal / 100)
        if (step.shield > 0) {
            foe.barrier += max * step.shield / 100
            foe.barrierUntil = FOREVER
        }
        if (step.invulnerable > 0) foe.invulnerableUntil = max(foe.invulnerableUntil, time + step.invulnerable)
        if (step.summon > 0) summon(foe, step.summon)
        note(foe, NoteKind.PHASE, code, step.at)
        if (step.burst > 0 && heroFighter.alive) {
            val element = DamageType.element(step.element)
            val total = foe.body.damage.values.sum() * step.burst / 100
            val damage = if (element != null) mapOf(element to total) else foe.body.damage.mapValues { it.value * step.burst / 100 }
            strike(foe, foeTarget(), Blow(damage, Action.SKILL, spell = false, skill = phaseSource(code, i), spread = false))
        }
    }
}

/** Свита [count] босса [boss] встаёт: свободное место берёт сразу, иначе ждёт первой в очереди. */
private fun Battle.summon(boss: Fighter, count: Int) {
    foes.indices.filter { foes[it].summonOf == boss.index && it !in called }.take(count).forEach { index ->
        called += index
        if (window.call(index)) foeFighters[index].enter(window.place(index), time)
    }
    if (foes.indices.any { it in called && foeFighters[it].body.auras.isNotEmpty() }) remake(heroFighter)
}

/** Босс пал (3.92.0): его свита падает с ним - и стоящая, и ждущая; добычи она не даёт и убийством не считается. */
internal fun Battle.dismissRetinue(boss: Fighter) {
    foes.indices.filter { foes[it].summonOf == boss.index }.forEach { index ->
        window.dismiss(index)
        val fighter = foeFighters[index]
        if (fighter.life > 0) {
            fighter.life = 0.0
            fighter.shield = 0.0
            fighter.ailments.clear()
            fighter.effects.clear()
        }
        retinueDown += index
    }
}
