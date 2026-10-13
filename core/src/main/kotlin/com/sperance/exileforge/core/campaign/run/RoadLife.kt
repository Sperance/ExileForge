package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.RegenPool
import com.sperance.exileforge.rules.content.ExpeditionRules

/**
 * Здоровье героя на карте между боями (4.10.0): одна чистая скорость [net] - здоровья в секунду. Прибывает регенерация героя, как в
 * бою (`regen(LIFE)` × скорость восстановления), и основа вне боя `offCombatRegen` - процент максимума; убывает деген героя. Без
 * задержки после урона. Вне боя здоровье не выше максимума и деген не опускает его ниже 1: заход кончает только бой.
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
        const val FLOOR = 1.0

        /** Скорость здоровья героя [body] на карте по темпу похода [pace]. */
        fun of(body: Combatant, pace: ExpeditionRules): RoadLife = RoadLife(
            body.regen(RegenPool.LIFE) * body.recoveryRate + body.maxLife * (pace.offCombatRegen / 100 - body.lifeDegenShare),
        )
    }
}
