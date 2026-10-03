package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.rules.content.ChargeKind
import kotlin.math.min

// ==================== What the powers reach (2.79.0) ====================

/** The share of the hero's life a self-inflicted burning takes a second (server 1.32.0). */

/** A power that did something of its own, for the log and the number over the hero's card. */
internal fun Battle.powerShown(code: String, healed: Double) = self(code, healed)

/** Every charge on the belt, summed. */
internal fun Battle.flaskCharges(): Double = charges.sum()

/** [amount] charges to every flask of the belt, up to each one's maximum. */
internal fun Battle.chargeFlasks(amount: Double) = kit.flasks.forEachIndexed { i, flask -> flask?.let { charges[i] = min(it.maxCharges, charges[i] + amount) } }

/** [amount] charges of [kind] for the hero (3.33.0): the body is made again with them. */
internal fun Battle.gainCharges(kind: ChargeKind, amount: Int) {
    if (heroCharges.gain(kind, amount, time, heroFighter.body.stats, random)) {
        remake(heroFighter)
        note(heroFighter, NoteKind.CHARGE, kind.name, heroCharges.count(ChargeKind.ALL).toDouble())
    }
}

/** Every charge of [kind] taken (3.33.0); how many there were. */
internal fun Battle.consumeCharges(kind: ChargeKind): Int = heroCharges.consume(kind).also { if (it > 0) remake(heroFighter) }

/** The pet's maximum life (3.33.0), zero without one. */
internal fun Battle.petLife(): Double = allyFighter?.body?.maxLife ?: 0.0

/** [share] percent of the pet's life back while it stands (3.33.0). */
internal fun Battle.healPet(share: Double) {
    val pet = allyFighter?.takeIf { it.alive } ?: return
    pet.life = min(pet.body.maxLife, pet.life + pet.body.maxLife * share / 100)
}

/**
 * [ailment] on the hero by their own power for [duration] seconds (3.33.0, server 1.32.0): a damage over time burns
 * [CombatRules.selfBurn] of their life a second, the rest are the rule's magnitude.
 */
internal fun Battle.afflictSelf(ailment: Ailment, duration: Double) {
    val hero = heroFighter
    val (rule, _) = ruleOf[ailment] ?: return
    if (duration <= 0 || hero.body.immune(ailment)) return
    val magnitude = if (ailment.hurts) hero.body.maxLife * rules.selfBurn else rule.magnitude
    place(hero, ActiveAilment(ailment, time + duration, magnitude, duration, Side.MONSTER), rule.stacks)
}

/** A foe finished off by a power: down at once, a kill as any other. */
internal fun Battle.slay(foe: Fighter) {
    if (!foe.alive) return
    foe.life = 0.0
    record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, foe.index)
    fell(foe)
}
