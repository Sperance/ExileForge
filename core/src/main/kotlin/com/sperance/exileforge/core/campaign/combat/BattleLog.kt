package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.EffectTrace
import com.sperance.exileforge.core.campaign.FighterShot
import com.sperance.exileforge.core.campaign.LineKind
import com.sperance.exileforge.core.campaign.LineSource
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.RollKey
import com.sperance.exileforge.core.campaign.RollTrace
import com.sperance.exileforge.core.campaign.Trace
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.campaign.lines
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.Op

// ==================== The log's traces (3.37.0) ====================

/** One number off the fight's dice, written on the tape: the same draw as before, only remembered. */
internal fun Battle.draw(key: RollKey, chance: Double, ailment: Ailment? = null): Double = random.nextDouble().also { tape?.add(RollTrace(key, chance, it, ailment)) }

internal fun Battle.takeTape(): List<RollTrace> = tape.orEmpty().toList().also { tape = null }

/** [fighter] as it stands now: its sheet — the blow's own for the striker — what the fight laid on it, ailments and pools. */
internal fun Battle.shot(fighter: Fighter, blow: Blow? = null): FighterShot {
    val lines = buildList {
        fighter.effects.forEach { effect ->
            val kind = when (effect.kind) {
                EffectKind.BUFF -> LineKind.BUFF
                EffectKind.CURSE -> LineKind.CURSE
                EffectKind.FLASK -> LineKind.FLASK
            }
            effect.lines.forEach { add(LineSource(kind, effect.source, it)) }
        }
        if (fighter === heroFighter) {
            heroCharges.lines().forEach { add(LineSource(LineKind.CHARGE, "", it)) }
            chargeLines().forEach { add(LineSource(LineKind.CHARGE, "", it)) }
            model.conditionalSourced(conditions).forEach { (condition, line) -> add(LineSource(LineKind.CONDITION, condition.name, line)) }
            if (fighter.low) model.lowLife.forEach { add(LineSource(LineKind.LOW_LIFE, "", it)) }
            powers.standing.forEach { add(LineSource(LineKind.POWER, "", it)) }
            auras().forEach { (aura, value) -> add(LineSource(LineKind.AURA, aura, StatLine(aura, Op.ADD, value))) }
            val own = blow?.body?.takeIf { it !== fighter.body }
            if (own != null) {
                blow.skill?.let { code -> kit.actives.filterNotNull().firstOrNull { it.skill.code == code } }
                    ?.let { skill -> skill.skill.hit?.stats?.lines(skill.level(fighter.body))?.forEach { add(LineSource(LineKind.SKILL, skill.skill.code, it)) } }
            }
        }
    }
    return FighterShot(
        fighter.side, fighter.index, (blow?.body ?: fighter.body).stats, lines, fighter.ailments.toList(), fighter.life, fighter.body.maxLife,
        fighter.shield, if (fighter === heroFighter) conditions else emptySet(),
    )
}

/** A note in the log (3.37.0): something that happened to [fighter] without a blow. */
internal fun Battle.note(fighter: Fighter, kind: NoteKind, ref: String, value: Double = 0.0) {
    record(
        fighter.side, Action.NOTE, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, if (fighter.side == Side.MONSTER) fighter.index else target()?.index ?: 0,
        ref, onSelf = true, trace = NoteTrace(kind, ref, value, shot(fighter), origin),
    )
}

internal fun Battle.record(
    actor: Side,
    action: Action,
    kind: HitKind,
    damage: Double,
    type: DamageType?,
    healed: Double,
    stunned: Boolean,
    inflicted: List<Ailment>,
    ailment: Ailment?,
    foe: Int,
    skill: String? = null,
    onSelf: Boolean = false,
    trace: Trace? = null,
    pet: Boolean = false,
) {
    val m = foeFighters.getOrNull(foe)
    log += CombatEvent(
        time, actor, action, kind, damage, type, healed, stunned, inflicted, ailment, heroFighter.life, heroFighter.shield,
        m?.life ?: 0.0, m?.shield ?: 0.0, foe, skill, onSelf, heroFighter.mana, ally?.code?.takeIf { pet },
    )
        .also { it.trace = trace ?: skillTrace(actor, action, foe, skill, onSelf, healed) }
}

/** [fighter] is the combat pet. */
internal fun Battle.isPet(fighter: Fighter): Boolean = allyFighter != null && fighter === allyFighter

internal fun Battle.mended(amount: Double) {
    petMend += amount
    val pet = allyFighter ?: return
    if (time - petMendFrom < rules.petMendEvery) return
    if (petMend >= 1) {
        record(
            Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, petMend, false, emptyList(), null, target()?.index ?: 0, onSelf = true,
            trace = EffectTrace(EffectKind.BUFF, ally!!.code, emptyList(), time - petMendFrom, petMend, 1.0, shot(pet), origin), pet = true,
        )
    }
    petMend = 0.0
    petMendFrom = time
}

/**
 * A skill's line without a blow (3.37.0): the buff or curse it laid, read off the fighter that holds it now — the user
 * for one on themselves, the target for a curse.
 */
private fun Battle.skillTrace(actor: Side, action: Action, foe: Int, skill: String?, onSelf: Boolean, healed: Double): Trace? {
    if (action != Action.SKILL || skill == null) return null
    val user = if (actor == Side.HERO) heroFighter else foeFighters.getOrNull(foe) ?: return null
    val holder = if (onSelf) {
        user
    } else if (actor == Side.HERO) {
        foeFighters.getOrNull(foe) ?: return null
    } else {
        heroFighter
    }
    val effect = holder.effects.lastOrNull { it.source == skill }
    return EffectTrace(effect?.kind ?: EffectKind.BUFF, skill, effect?.lines.orEmpty(), effect?.duration ?: 0.0, healed, 1.0, shot(holder), origin)
}
