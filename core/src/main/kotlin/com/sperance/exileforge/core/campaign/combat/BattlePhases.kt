package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.PhaseStep
import kotlin.math.min

/** Шаг фазы босса в бою: шаг правил и умение, которое он даёт, уже найденное среди умений монстров. */
data class FoePhase(val step: PhaseStep, val skill: MonsterSkill? = null, val totems: List<FoeTotem> = emptyList())

/** Источник строк шага [step] фазы [code] на боссе: у каждого шага свой, так что два шага одного порога не заменяют друг друга. */
internal fun phaseSource(code: String, step: Int) = "$code#$step"

/**
 * Фазы босса: каждый шаг, до чьего порога опустилось здоровье [foe], срабатывает раз за бой - строки, умение (готово сразу; у вождя
 * Бездны и башни - через долю перезарядки от начала фазы, как любое умение босса от начала боя, [firstReadyAt]), свита, удар,
 * лечение, барьер, неуязвимость - и пишется в журнал заметкой фазы.
 *
 * Взрыв фазы - вторичный урон: от него не уклониться.
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
        phase.skill?.let {
            learned.getOrPut(foe.index) { mutableListOf() } += it
            foe.readyAt[it.code] = if (plan.origin?.leader == true) firstReadyAt(foe, it) else time
        }
        if (step.heal > 0) foe.life = min(max, foe.life + max * step.heal / 100)
        if (step.shield > 0) {
            foe.barrier += max * step.shield / 100
            foe.barrierUntil = FOREVER
        }
        if (step.invulnerable > 0 && time + step.invulnerable > foe.invulnerableUntil) {
            foe.invulnerableUntil = time + step.invulnerable
            foe.invulnerableTotal = step.invulnerable
        }
        if (step.summon > 0) summon(foe, step.summon)
        if (phase.totems.isNotEmpty()) raiseTotem(foe, phase.totems)
        note(foe, NoteKind.PHASE, code, step.at)
        if (step.burst > 0 && heroFighter.alive) {
            val element = DamageType.element(step.element)
            val total = foe.body.damage.values.sum() * step.burst / 100
            val damage = if (element != null) mapOf(element to total) else foe.body.damage.mapValues { it.value * step.burst / 100 }
            strike(foe, heroFighter, Blow(damage, Action.SKILL, spell = false, skill = phaseSource(code, i), spread = false, secondary = true))
        }
    }
}

/**
 * Свита [count] босса [boss], что бьётся, встаёт на поле сразу (зов фаз, тени Поглотителя); идолов ([Foe.inert]) не зовёт. Кто встал -
 * номера в стае (тень Поглотителя получает украденный дар).
 */
internal fun Battle.summon(boss: Fighter, count: Int): List<Int> = call(boss, count) { !it.inert }

/** Идолы [count] босса [boss] ([Foe.inert]) встают с началом боя. */
internal fun Battle.raiseIdols(boss: Fighter, count: Int): List<Int> = call(boss, count) { it.inert }

/**
 * Свита [count] босса [boss], что проходит [which], встаёт на поле: без свободного слота вокруг босса - не встаёт; строки вставшей на
 * босса ([Foe.ward]) ложатся сразу.
 */
private fun Battle.call(boss: Fighter, count: Int, which: (Foe) -> Boolean): List<Int> {
    val risen = mutableListOf<Int>()
    foes.indices.filter { foes[it].summonOf == boss.index && which(foes[it]) && it !in called }.take(count).forEach { index ->
        val slot = freeSlot() ?: return@forEach
        slotHolders[slot] = SlotHolder.Minion(index)
        called += index
        risen += index
        enterField(index)
        bannerFor(foeFighters[index])
        foes[index].ward.takeIf { it.isNotEmpty() }?.let { lines -> buff(boss, wardSource(index), lines, FOREVER, origin = foes[index].origin?.code?.let(EffectOrigin::Ward)) }
    }
    if (foes.indices.any { it in called && foeFighters[it].body.auras.isNotEmpty() }) remake(heroFighter)
    return risen
}

/** Босс пал: его свита падает с ним; добычи она не даёт и убийством не считается. */
internal fun Battle.dismissRetinue(boss: Fighter) {
    fellTotems(boss)
    foes.indices.filter { foes[it].summonOf == boss.index }.forEach { index ->
        freeMinionSlot(index)
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

/** Приспешник пал: его слот свободен для новой свиты или тотема, его строки на боссе ([Foe.ward]) сняты. */
internal fun Battle.freeMinionSlot(index: Int) {
    slotHolders.indices.firstOrNull { (slotHolders[it] as? SlotHolder.Minion)?.index == index }?.let { slotHolders[it] = null }
    val boss = foes[index].summonOf?.let(foeFighters::get) ?: return
    if (boss.effects.removeAll { it.source == wardSource(index) }) remake(boss)
}

/** Источник строк свиты [index] на её боссе ([Foe.ward]): по нему они снимаются, когда она падает. */
private fun wardSource(index: Int) = "ward#$index"
