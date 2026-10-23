package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.rules.sheet.Recovery

/**
 * Здоровье героя на карте между боями: одна чистая скорость [net] - здоровья в секунду. Прибывает только пополнение потока здоровья,
 * как в бою ([Recovery.lifeFlow]: регенерация листа), своей основы вне боя нет; убывают потери и деген героя. Вне боя здоровье не выше
 * максимума и деген листа не опускает его ниже 1; урон недугов идёт мимо этой скорости и может убить. Регенерацию героя режет доля
 * восстановления [Recovery.share] - «меньше регенерации», запрет и дно строк карты - и недуги, принесённые из боя или от ловушек, на свою
 * силу (`curb`). Пока на герое ранящий недуг (кровотечение, яд, поджог - [flow] с `stalledBy`), регенерации нет; фляги идут своим путём.
 * В бою так не бывает - там регенерация идёт.
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

        /** Скорость здоровья героя [body] на карте; [curb] и [manaCurb] - срез недугов здоровья и маны, [stalledBy] - ранящий недуг, что глушит восстановление. */
        fun of(body: Combatant, curb: Double, manaCurb: Double, stalledBy: Ailment?): RoadLife = RoadLife(flow(body, curb, manaCurb, stalledBy).net)

        /**
         * Из чего скорость: пополнение потока здоровья правил ([Recovery.lifeFlow]: регенерация листа и доля регенерации маны под
         * долей восстановления и срезом недугов [curb] ([manaCurb] - маны) на скорость восстановления) против потерь и дегена потока.
         * Под ранящим недугом [stalledBy] регенерации нет: её несбывшийся итог - [LifeFlow.stalled], для разбора.
         *
         * Отрицательная регенерация - потеря здоровья в секунду, как деген.
         */
        fun flow(body: Combatant, curb: Double, manaCurb: Double, stalledBy: Ailment?): LifeFlow {
            val flow = Recovery.lifeFlow(body, body.rules, curb, manaCurb)
            val recovery = flow.gain
            val stalled = stalledBy?.takeIf { it.hurts }
            return LifeFlow(
                regen = if (stalled == null) recovery else 0.0,
                degen = flow.drain,
                degenShare = body.lifeDegenShare * 100,
                stalledBy = stalled,
                stalled = if (stalled == null) 0.0 else recovery,
            )
        }
    }
}
