package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.CombatRules
import kotlin.math.min

/**
 * Энергощит: одно правило для боя и для карты. Урон идёт сначала в щит - только виды, что щит принимает ([DamageType.shielded]: всё,
 * кроме хаоса, как в PoE1), - удары, урон во времени по виду тика, удары Предначертания, отражение и ловушки; щит
 * перезаряжается, когда с последнего удара прошла задержка правил, укороченная [Combatant.rechargeStart].
 */
internal object EnergyShield {
    /** Сколько из урона [parts] (вид и число; вид без названия щит принимает) примет щит [shield]. */
    fun absorbed(shield: Double, parts: Iterable<Pair<DamageType?, Double>>): Double = min(shield, parts.sumOf { (type, amount) -> if (type?.shielded == false) 0.0 else amount.coerceAtLeast(0.0) })

    /** Сколько из урона [parts] по видам примет щит [shield]. */
    fun absorbed(shield: Double, parts: Map<DamageType, Double>): Double = absorbed(shield, parts.toList())

    /** Сколько из урона [amount] одного вида [type] примет щит [shield]. */
    fun absorbed(shield: Double, amount: Double, type: DamageType?): Double = absorbed(shield, listOf(type to amount))

    /** Щит [shield] тела [body] через [dt] секунд, когда с последнего удара прошло [sinceHit] секунд: свой реген и перезарядка. */
    fun recovered(body: Combatant, rules: CombatRules, shield: Double, sinceHit: Double, dt: Double): Double {
        val recharge = if (sinceHit >= rules.shield.rechargeDelay / body.rechargeStart) body.maxShield * rules.shield.rechargePerSecond / 100 * body.shieldRecharge else 0.0
        return min(body.maxShield, shield + (body.regen(RegenPool.SHIELD) * body.recoveryRate + recharge) * dt)
    }
}
