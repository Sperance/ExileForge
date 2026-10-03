package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.Trace

/**
 * One thing that happened, and where both sides stood after it.
 *
 * [actor] is who did it; what it did was done to the other side. A [Action.TICK] is an ailment's damage gathered over the last second, so the
 * log is not a flood; [type] is the damage that led — the biggest share of a hit, or the ailment's. [foe] (2.70.0) is the
 * foe of the pack the event was about — the one that struck, or was struck — and [monsterLife], [monsterShield] are its.
 */
data class CombatEvent(
    val time: Double,
    val actor: Side,
    val action: Action,
    val kind: HitKind,
    val damage: Double,
    val type: DamageType?,
    val healed: Double,
    val stunned: Boolean,
    /** What this blow inflicted on its target. */
    val inflicted: List<Ailment>,
    /** For a tick: which ailment burned. */
    val ailment: Ailment?,
    val heroLife: Double,
    val heroShield: Double,
    val monsterLife: Double,
    val monsterShield: Double,
    val foe: Int = 0,
    /** The skill used or the flask drunk (2.78.0), by its code; null for a swing, a tick, a reflection or a retreat. */
    val skill: String? = null,
    /** The actor did it to themselves (2.78.0): a buff, a heal, a draught. */
    val onSelf: Boolean = false,
    /** The hero's mana after it (2.78.0). */
    val heroMana: Double = 0.0,
    /**
     * The combat pet on the hero's side of this line (3.70.0), by species: it struck, healed or gave a blow back when the
     * [actor] is the hero's side, it was struck when the actor is a monster. The figures count as they always did.
     */
    val pet: String? = null,
) {
    /** How this line came about (3.37.0), for its card; outside the event's identity, so two fights of one seed still compare equal. */
    var trace: Trace? = null
        internal set

    /** Who the number floats off. */
    val target: Side get() = if (onSelf) actor else actor.other

    /** A monster's line at the combat pet (3.70.0): a blow, a tick or a blow back it took — the pet's, never the hero's damage taken. */
    val atPet: Boolean get() = pet != null && actor == Side.MONSTER
    val landed: Boolean get() = kind == HitKind.HIT || kind == HitKind.CRIT
}

/** A whole fight, as a test or a replay reads it. */
data class CombatLog(val events: List<CombatEvent>, val outcome: Outcome, val heroLife: Double, val duration: Double)

/**
 * One blow of a fighter (2.78.0): a weapon's swing, a skill's hit or a spell — its damage by type before
 * anyone's defences, and what the skill adds. A [spell] is not evaded; its damage was rolled already
 * unless it [spread]s by the rule's variance as a weapon's does. [body] is the striker's sheet for this
 * blow with the skill's own lines laid on; [stun] and [ailments] are chances in percent beyond the rules.
 */
internal class Blow(
    val damage: Map<DamageType, Double>,
    val action: Action = Action.ATTACK,
    val spell: Boolean = false,
    val skill: String? = null,
    val body: Combatant? = null,
    val spread: Boolean = true,
    val stun: Double = 0.0,
    val ailments: List<Pair<Ailment, Double>> = emptyList(),
    /** Whether [Battle.strike]'s target is the blow's primary one: an area skill's other foes neither build nor break momentum. */
    val primary: Boolean = true,
    /**
     * The increases in percent the hero's [damage] was grown by, by type (server 1.57.0): an increase the blow meets later —
     * against the target's state — adds to them instead of multiplying. Null: the sheet's own increases of each type.
     */
    val increase: Map<DamageType, Double>? = null,
) {
    /** A weapon's blow: attacks, not spells, bring life on hit. */
    val weapon: Boolean get() = !spell && (action == Action.ATTACK || action == Action.SKILL)
}

/** Life damage the hero takes a second until [until] (3.33.0): the delayed share of a hit. */
internal class Delayed(val rate: Double, val until: Double)

/** Life and mana a draught gives a second until [until] (2.78.0). */

/**
 * A recovery running over time: a draught's (its belt [slot]) or a recoup's (slot -1). A pure life draught
 * stops once life is full, as in PoE (3.79.0); [restored] and [wasted] go to the log's recovery shelf.
 */
internal class Recovery(val life: Double, val mana: Double, var until: Double, val slot: Int, val stopsAtFull: Boolean = false) {
    var restored = 0.0
    var wasted = 0.0
}
