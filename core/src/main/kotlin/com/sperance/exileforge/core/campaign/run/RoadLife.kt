package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.content.ManaRule
import com.sperance.exileforge.rules.sheet.Recovery

/**
 * Здоровье героя на карте между боями (4.10.0): одна чистая скорость [net] - здоровья в секунду. Прибывает регенерация героя, как в
 * бою ([Recovery.lifeRegen] × скорость восстановления), и основа вне боя `offCombatRegen` - процент максимума; убывает деген героя. Без
 * задержки после урона. Вне боя здоровье не выше максимума и деген листа не опускает его ниже 1; урон недугов идёт мимо этой
 * скорости ([ailing]) и может убить. Регенерацию героя (4.11.0) режет доля восстановления [Recovery.share] - «меньше регенерации»,
 * запрет и дно строк карты - и недуги, принесённые из боя или от ловушек, на свою силу (`curb`). Основа `offCombatRegen` - не
 * регенерация листа, а отдых между боями: её доля не режет. Пока на герое ранящий недуг (0.14.5, #52: кровотечение, яд, поджог -
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
         * Скорость здоровья героя [body] на карте по темпу похода [pace] и правилу маны [mana]; [curb] и [manaCurb] - срез недугов
         * здоровья и маны, [stalledBy] - ранящий недуг, что глушит восстановление.
         */
        fun of(body: Combatant, pace: ExpeditionRules, mana: ManaRule, curb: Double = 0.0, manaCurb: Double = 0.0, stalledBy: Ailment? = null): RoadLife = RoadLife(flow(body, pace, mana, curb, manaCurb, stalledBy).net)

        /**
         * Из чего скорость: регенерация листа и доля регенерации маны ([Recovery.lifeFromMana], 4.11.x) под долей восстановления и
         * срезом недугов [curb] ([manaCurb] - маны) плюс отдых `offCombatRegen` против дегена листа. Под ранящим недугом [stalledBy]
         * (0.14.5, #52) регенерации и отдыха нет: их несбывшийся итог - [LifeFlow.stalled], для разбора.
         */
        fun flow(body: Combatant, pace: ExpeditionRules, mana: ManaRule, curb: Double = 0.0, manaCurb: Double = 0.0, stalledBy: Ailment? = null): LifeFlow {
            val rest = body.maxLife * pace.offCombatRegen / 100
            val recovery = (Recovery.lifeRegen(body, body.rules.regen, curb) + Recovery.lifeFromMana(body, mana, manaCurb, curb)) * body.recoveryRate + rest
            val stalled = stalledBy?.takeIf { it.hurts }
            return LifeFlow(
                regen = if (stalled == null) recovery else 0.0,
                // Отрицательная регенерация (4.12.x, STAT-ATK-04) - потеря здоровья в секунду, как деген
                degen = body.maxLife * body.lifeDegenShare + Recovery.lifeLoss(body),
                degenShare = body.lifeDegenShare * 100,
                rest = if (stalled == null) rest else 0.0,
                stalledBy = stalled,
                stalled = if (stalled == null) 0.0 else recovery,
            )
        }
    }
}
