package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.CombatRules
import kotlin.math.min

/**
 * Энергощит (3.95.2): одно правило для боя и для карты. Удар идёт сначала в щит - кроме хаоса, что проходит мимо него; щит
 * перезаряжается, когда с последнего удара прошла задержка правил, укороченная [Combatant.rechargeStart].
 */
internal object EnergyShield {
    /** Сколько из удара [amount], где [chaos] - его хаос, примет щит [shield]. */
    fun absorbed(shield: Double, amount: Double, chaos: Double = 0.0): Double = min(shield, (amount - chaos).coerceAtLeast(0.0))

    /** Щит [shield] тела [body] через [dt] секунд, когда с последнего удара прошло [sinceHit] секунд: свой реген и перезарядка. */
    fun recovered(body: Combatant, rules: CombatRules, shield: Double, sinceHit: Double, dt: Double): Double {
        val recharge = if (sinceHit >= rules.shield.rechargeDelay / body.rechargeStart) body.maxShield * rules.shield.rechargePerSecond / 100 * body.shieldRecharge else 0.0
        return min(body.maxShield, shield + ((body.shieldRegen + body.maxShield * body.shieldRegenShare) * body.recoveryRate + recharge) * dt)
    }
}
