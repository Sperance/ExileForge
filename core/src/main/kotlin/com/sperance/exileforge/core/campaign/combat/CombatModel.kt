package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillType
import kotlin.math.max

/** A spell pays in cast speed and is not evaded; an attack pays in its weapon. */
internal val SkillDefinition.spell: Boolean get() = type == SkillType.SPELL

/** Who acted. */
enum class Side {
    HERO,
    MONSTER,
    ;

    val other: Side get() = if (this == HERO) MONSTER else HERO
}

/**
 * What a fighter did: swung a weapon, let an ailment burn on, gave a blow back (thorns and reflect, 2.75.0),
 * turned to leave, used a skill — a class's, a passive's answer or a monster's — or drank a flask (2.78.0).
 */
enum class Action {
    ATTACK,
    TICK,
    REFLECT,
    RETREAT,
    SKILL,
    FLASK,

    /** Something that happened without a blow (3.37.0): a buff, a charge, a power, a condition, a kill's reward. */
    NOTE,
}

/** How a blow ended: it landed, landed hard, or never reached. */
enum class HitKind { HIT, CRIT, EVADED, BLOCKED }

/** How the whole fight ended; a retreat is the hero walking out of it. */
enum class Outcome { WIN, LOSS, RETREAT }

/** Damage by type, as the sheet names it. */
enum class DamageType(val attack: String, val resist: String?) {
    PHYSICAL(CoreStat.ATTACK_PHYSICAL.code, null),
    FIRE(CoreStat.ATTACK_FIRE.code, CoreStat.RESIST_FIRE.code),
    COLD(CoreStat.ATTACK_COLD.code, CoreStat.RESIST_COLD.code),
    LIGHTNING(CoreStat.ATTACK_LIGHTNING.code, CoreStat.RESIST_LIGHTNING.code),
    CHAOS(CoreStat.ATTACK_CHAOS.code, CoreStat.RESIST_CHAOS.code),
    ;

    /** The stat that lifts this resistance's ceiling (since server 0.36.0). */
    val maxResist: String? get() = resist?.replace("STOCK_RESIST_", "STOCK_RESIST_MAX_")

    companion object {
        fun of(stat: String) = entries.firstOrNull { it.attack == stat }

        /** A skill's element by its name (server 0.69.0): `FIRE`, `COLD`, `LIGHTNING`, `CHAOS` or `PHYSICAL`. */
        fun element(name: String?) = entries.firstOrNull { it.name == name }
        val ELEMENTS = listOf(FIRE, COLD, LIGHTNING)
    }
}

/**
 * [value] that [increase] percent increased made, grown by [extra] percent more of increase beside it (server 1.57.0):
 * increases add up before they multiply, as in PoE — `(100 + increase + extra) / (100 + increase)`.
 */
internal fun regrow(value: Double, increase: Double, extra: Double): Double {
    val before = 100 + increase
    return if (extra == 0.0 || before <= 0) value else value * max(0.0, before + extra) / before
}

/**
 * The six ailments the server rules (its `EnumStatBool` without the prefix). Which damage brings which is the rule's, not ours.
 * [word] is how the sheet's stats name it since server 0.36.0 — `STOCK_IGNITE_CHANCE`, `STOCK_AVOID_IGNITE`,
 * `STOCK_IGNITE_DURATION_ON_SELF` — and [damage] the stat that makes its damage over time heavier.
 */

/**
 * The buildups of server 1.73.0 (3.78.0, as in PoE2): a blow fills a bar on the struck one and a full bar sets it off.
 * [gain] is the striker's line that fills it faster — the old «chance to freeze» fills the freeze — and [avoid] the struck
 * one's that fills it slower.
 */
enum class Buildup(val gain: String, val avoid: String?) {
    STUN(CoreStat.STUN_BUILDUP.code, CoreStat.AVOID_STUN.code),
    FREEZE(CoreStat.FREEZE_CHANCE.code, CoreStat.AVOID_FREEZE.code),
    ELECTROCUTE(CoreStat.ELECTROCUTE_BUILDUP.code, null),
}

enum class Ailment(val word: String, val damage: String? = null) {
    BURNING("IGNITE", CoreStat.BURNING_DAMAGE.code),
    CHILLED("CHILL"),
    FROZEN("FREEZE"),
    SHOCKED("SHOCK"),
    POISONED("POISON", CoreStat.POISON_DAMAGE.code),
    BLEEDING("BLEED", CoreStat.BLEED_DAMAGE.code),
    ;

    /** Deals damage over time, as opposed to slowing, weakening or stopping. */
    val hurts: Boolean get() = this == BURNING || this == POISONED || this == BLEEDING

    /** The sheet's "increased damage against X enemies" stat for this ailment (server 0.66.0). */
    val against: String get() = "STOCK_DAMAGE_VS_$name"
    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name }

        /** An ailment as a skill names it (server 0.69.0): `IGNITE`, `CHILL`, `FREEZE`, `SHOCK`, `POISON`, `BLEED`. */
        fun byWord(word: String) = entries.firstOrNull { it.word == word }

        /** The ailment a damage brings by nature: fire burns, cold chills, lightning shocks, chaos poisons, a blade bleeds. */
        fun of(type: DamageType) = when (type) {
            DamageType.PHYSICAL -> BLEEDING
            DamageType.FIRE -> BURNING
            DamageType.COLD -> CHILLED
            DamageType.LIGHTNING -> SHOCKED
            DamageType.CHAOS -> POISONED
        }
    }
}
