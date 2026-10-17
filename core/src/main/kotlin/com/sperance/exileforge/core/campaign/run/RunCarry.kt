package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.ChargeView
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.AilmentLaw
import com.sperance.exileforge.core.campaign.combat.CarriedAilment
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.EnergyShield
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.RecoveryPool
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Недуги, баффы, проклятия и заряды на герое вне боя за [dt] секунд дороги: урон недугов каждый кадр
 * по законам боя ([AilmentLaw]: сила, шок, приём урона во времени) - в щит, затем в здоровье (яд и хаос мимо щита); ноль здоровья -
 * гибель на карте. На экран - тиком недуга раз в [TICK_SECONDS]. Затем всё несомое тает ([com.sperance.exileforge.core.campaign.combat.HeroCarry.after]).
 *
 * Гибель от тика: несомое уже снято, заход кончен.
 */
internal fun ExpeditionRun.ailing(dt: Double) {
    if (carry.bare) return
    val ailments = carry.ailments
    for (active in ailments) {
        if (!active.ailment.hurts) continue
        val wound = hurt(law.perSecond(hero, ailments, active) * minOf(dt, active.left), law.type(active))
        if (phase != RunPhase.MAP) return
        ticks.getOrPut(active.ailment) { AilmentTick() }.wound += wound
    }
    val before = carry
    carry = carry.after(dt)
    ticks.entries.removeAll { (ailment, tick) ->
        tick.clock += dt
        val gone = carry.ailments.none { it.ailment == ailment }
        if (tick.clock >= TICK_SECONDS || gone) {
            if (tick.wound.total >= .5) {
                val type = ailments.firstOrNull { it.ailment == ailment }?.let(law::type) ?: return@removeAll gone
                showHazard(HazardView(nextHazard++, type, tick.wound.life.roundToInt(), tick.wound.shield.roundToInt(), of = ailment))
            }
            tick.wound = Wound()
            tick.clock = 0.0
        }
        gone
    }
    if (carry.effects.size != before.effects.size || carry.charges.size != before.charges.size) rebody()
}

/** Новый недуг [fresh] на герое вне боя (ловушка): копится или сменяет прежний по правилу недуга. */
internal fun ExpeditionRun.afflict(fresh: CarriedAilment) {
    carry = carry.copy(ailments = law.laid(carry.ailments, fresh))
}

/** Сколько процентов запаса [pool] режут недуги на герое вне боя ([AilmentLaw.curb]). */
internal fun ExpeditionRun.curb(pool: RecoveryPool): Double = law.curb(hero, carry.ailments, pool)

/**
 * Ранящие недуги на герое вне боя с уроном в секунду каждого ([AilmentLaw.perSecond]); сильнейший из них глушит восстановление
 * на карте ([stalling]).
 */
private fun ExpeditionRun.hurtingRates(): List<Pair<CarriedAilment, Double>> = carry.ailments.filter { it.ailment.hurts }.map { it to law.perSecond(hero, carry.ailments, it) }

/** Ранящий недуг, что глушит регенерацию и отдых на карте: сильнейший по урону в секунду; нет - null. */
internal fun ExpeditionRun.stalling(): Ailment? = hurtingRates().maxByOrNull { it.second }?.first?.ailment

/**
 * Итог здоровья на карте в секунду: регенерация листа (под долей восстановления и недугами) и отдых вне боя - только без ранящего
 * недуга ([RoadLife]), деген листа и урон недугов; что из него принимает энергощит (яд и хаос - мимо), здоровья не
 * трогает ([LifeFlow.shielded]). Метка на полосе жизни и лист эффектов карты.
 */
internal fun ExpeditionRun.roadFlow(): LifeFlow {
    val rates = hurtingRates()
    val total = rates.sumOf { it.second }
    return RoadLife.flow(hero, pace, curb(RecoveryPool.LIFE), curb(RecoveryPool.MANA), rates.maxByOrNull { it.second }?.first?.ailment)
        .copy(ailments = total, shielded = EnergyShield.absorbed(shield, rates.map { (active, rate) -> law.type(active) to rate }))
}

/** Недуги на герое вне боя - плитками полки, как в бою: по недугу - доля времени, стаки, секунды (до десятой) и сила. */
internal fun ExpeditionRun.carriedAilments(): List<AilmentView> = carry.ailments.groupBy { it.ailment }.map { (ailment, same) ->
    val left = same.maxOf { it.left }
    AilmentView(
        ailment,
        (left / same.maxOf { it.duration }).toFloat().coerceIn(0f, 1f),
        same.size,
        tenths(left),
        if (ailment.hurts) same.sumOf { it.magnitude } else law.held(hero, carry.ailments, ailment),
    )
}

/** Баффы и проклятия на герое вне боя - плитками полки, как в бою. */
internal fun ExpeditionRun.carriedEffects(): List<EffectView> = carry.effects.map {
    EffectView(it.source, it.kind, (it.left / it.duration).toFloat().coerceIn(0f, 1f), tenths(it.left), it.icon, BuffKind.of(it.source), it.lines)
}

/** Заряды героя вне боя - плитками полки, как в бою. */
internal fun ExpeditionRun.carriedCharges(): List<ChargeView> = carry.charges.map { ChargeView(it.kind, it.count, kit.charges?.max(it.kind, hero.stats) ?: 0, tenths(it.left)) }

/** Секунды до десятой вверх: снимок экрана не меняется каждый кадр. */
private fun tenths(seconds: Double): Double = ceil(seconds * 10) / 10
