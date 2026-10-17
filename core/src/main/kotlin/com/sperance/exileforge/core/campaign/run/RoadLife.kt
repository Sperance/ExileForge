package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.sheet.Recovery

/**
 * Здоровье героя на карте между боями: одна чистая скорость [net] - здоровья в секунду. Прибывает пополнение потока здоровья,
 * как в бою ([Recovery.lifeFlow]), и основа вне боя `offCombatRegen` - процент максимума, только через `restDelay` секунд после
 * последнего полученного урона (регенерация листа - сразу); убывают потери и деген героя. Вне боя здоровье не выше максимума и
 * деген листа не опускает его ниже 1; урон недугов идёт мимо этой
 * скорости ([ailing]) и может убить. Регенерацию героя режет доля восстановления [Recovery.share] - «меньше регенерации»,
 * запрет и дно строк карты - и недуги, принесённые из боя или от ловушек, на свою силу (`curb`). Основа `offCombatRegen` - не
 * регенерация листа, а отдых между боями: её доля не режет. Пока на герое ранящий недуг (кровотечение, яд, поджог -
 * [flow] с `stalledBy`), нет ни регенерации, ни отдыха; фляги идут своим путём. В бою так не бывает - там регенерация идёт.
 */
@JvmInline
internal value class RoadLife(val net: Double) {
    /** Здоровье [life] из [max] через [dt] секунд. */
    fun after(life: Double, max: Double, dt: Double): Double = when {
        net > 0 -> (life + net * dt).coerceAtMost(max)
        net < 0 -> if (life <= FLOOR) life else (life + net * dt).coerceAtLeast(FLOOR)
        else -> life
    }

    companion object {
        /** Ниже этого здоровья деген на карте не опускает. */
        const val FLOOR = LifeFlow.FLOOR

        /**
         * Скорость здоровья героя [body] на карте по темпу похода [pace] через [sinceHit] секунд после последнего полученного урона;
         * [curb] и [manaCurb] - срез недугов здоровья и маны, [stalledBy] - ранящий недуг, что глушит восстановление.
         */
        fun of(body: Combatant, pace: ExpeditionRules, sinceHit: Double, curb: Double = 0.0, manaCurb: Double = 0.0, stalledBy: Ailment? = null): RoadLife = RoadLife(flow(body, pace, sinceHit, curb, manaCurb, stalledBy).net)

        /**
         * Из чего скорость: пополнение потока здоровья правил ([Recovery.lifeFlow]: регенерация листа и доля регенерации маны под
         * долей восстановления и срезом недугов [curb] ([manaCurb] - маны) на скорость восстановления) плюс отдых `offCombatRegen`
         * против потерь и дегена потока. Отдых ждёт `restDelay` секунд без урона: через [sinceHit] секунд после удара его ещё нет, сколько
         * ждать - [LifeFlow.restIn]. Под ранящим недугом [stalledBy] регенерации и отдыха нет: их несбывшийся итог - [LifeFlow.stalled],
         * для разбора.
         *
         * Отрицательная регенерация - потеря здоровья в секунду, как деген.
         */
        fun flow(body: Combatant, pace: ExpeditionRules, sinceHit: Double, curb: Double = 0.0, manaCurb: Double = 0.0, stalledBy: Ailment? = null): LifeFlow {
            val flow = Recovery.lifeFlow(body, body.rules, curb, manaCurb)
            val wait = (pace.restDelay - sinceHit).coerceAtLeast(0.0)
            val rest = if (wait > 0) 0.0 else body.maxLife * pace.offCombatRegen / 100
            val recovery = flow.gain + rest
            val stalled = stalledBy?.takeIf { it.hurts }
            return LifeFlow(
                regen = if (stalled == null) recovery else 0.0,
                degen = flow.drain,
                degenShare = body.lifeDegenShare * 100,
                rest = if (stalled == null) rest else 0.0,
                restIn = if (stalled == null) wait else 0.0,
                stalledBy = stalled,
                stalled = if (stalled == null) 0.0 else recovery,
            )
        }
    }
}
