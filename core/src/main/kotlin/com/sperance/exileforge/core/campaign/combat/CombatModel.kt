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
 * What a fighter did: swung a weapon, let an ailment burn on, gave a blow back (thorns and reflect),
 * used a skill — a class's, a passive's answer or a monster's — or drank a flask.
 */
enum class Action {
    ATTACK,
    TICK,
    REFLECT,
    SKILL,
    FLASK,

    /** Something that happened without a blow: a buff, a charge, a power, a condition, a kill's reward. */
    NOTE,
}

/** How a blow ended: it landed, landed hard, or never reached. */
enum class HitKind { HIT, CRIT, EVADED, BLOCKED }

/** Чем кончился бой: из боя не уходят - он идёт до победы или поражения. */
enum class Outcome { WIN, LOSS }

/** Damage by type, as the sheet names it. */
enum class DamageType(val attack: String, val resist: String?) {
    PHYSICAL(CoreStat.ATTACK_PHYSICAL.code, null),
    FIRE(CoreStat.ATTACK_FIRE.code, CoreStat.RESIST_FIRE.code),
    COLD(CoreStat.ATTACK_COLD.code, CoreStat.RESIST_COLD.code),
    LIGHTNING(CoreStat.ATTACK_LIGHTNING.code, CoreStat.RESIST_LIGHTNING.code),
    CHAOS(CoreStat.ATTACK_CHAOS.code, CoreStat.RESIST_CHAOS.code),
    ;

    /** The stat that lifts this resistance's ceiling. */
    val maxResist: String? get() = resist?.replace("STOCK_RESIST_", "STOCK_RESIST_MAX_")

    companion object {
        fun of(stat: String) = entries.firstOrNull { it.attack == stat }

        /** A skill's element by its name: `FIRE`, `COLD`, `LIGHTNING`, `CHAOS` or `PHYSICAL`. */
        fun element(name: String?) = entries.firstOrNull { it.name == name }
        val ELEMENTS = listOf(FIRE, COLD, LIGHTNING)
    }
}

/**
 * [value] that [increase] percent increased made, grown by [extra] percent more of increase beside it:
 * increases add up before they multiply, as in PoE — `(100 + increase + extra) / (100 + increase)`.
 */
internal fun regrow(value: Double, increase: Double, extra: Double): Double {
    val before = 100 + increase
    return if (extra == 0.0 || before <= 0) value else value * max(0.0, before + extra) / before
}

/**
 * The buildups (as in PoE2): a blow fills a bar on the struck one and a full bar sets it off.
 * [gain] is the striker's line that fills it faster — «chance to freeze» fills the freeze — and [avoid] the struck
 * one's that fills it slower.
 */
enum class Buildup(val gain: String, val avoid: String?) {
    STUN(CoreStat.STUN_BUILDUP.code, CoreStat.AVOID_STUN.code),
    FREEZE(CoreStat.FREEZE_CHANCE.code, CoreStat.AVOID_FREEZE.code),
    ELECTROCUTE(CoreStat.ELECTROCUTE_BUILDUP.code, null),
}

/**
 * The six ailments the server rules (its `EnumStatBool` without the prefix). Which damage brings which is the rule's, not ours.
 * [word] is how the sheet's stats name it — `STOCK_IGNITE_CHANCE`, `STOCK_AVOID_IGNITE`,
 * `STOCK_IGNITE_DURATION_ON_SELF` — and [damage] the stat that makes its damage over time heavier.
 * [general] - шанс недуга поднимает общая строка карты «шанс наложить состояние» (`MAP_MONSTER_AILMENTS`); у холода своего шанса
 * нет, изранение и иссушение поднимает своя строка карты. [ailing] - состояние считается недугом для строк «против
 * состояний», условий «под недугом» и добивающих ударов: Разложение и урон во времени умения - не недуги.
 */
enum class Ailment(val word: String, val damage: String? = null, val general: Boolean = true, val ailing: Boolean = true) {
    BURNING("IGNITE", CoreStat.BURNING_DAMAGE.code),
    CHILLED("CHILL", general = false),
    FROZEN("FREEZE"),
    SHOCKED("SHOCK"),
    POISONED("POISON", CoreStat.POISON_DAMAGE.code),
    BLEEDING("BLEED", CoreStat.BLEED_DAMAGE.code),

    /** Изранение: режет регенерацию и вампиризм здоровья на свою силу - что именно режет, решает правило (`curbs`). */
    WOUNDED("WOUND", general = false),

    /** Иссушение: режет регенерацию, вампиризм и ману при ударе - запас маны правила (`curbs`). */
    PARCHED("PARCH", general = false),

    /**
     * Разложение: хаос-удары кладут стаки, каждый действующий - +сила% получаемого урона хаосом цели (правило
     * `exposes`, предел и обновление всех стаков - правило); ударов и тиков хаоса, яда тоже. Не недуг.
     */
    DECAYED("DECAY", general = false, ailing = false),

    /** Урон во времени умения, что не яд («Высасывание сути»): хаос каждый тик, один экземпляр на цель. Не недуг. */
    DRAINED("DRAIN", general = false, ailing = false),
    ;

    /** Deals damage over time, as opposed to slowing, weakening or stopping. */
    val hurts: Boolean get() = this == BURNING || this == POISONED || this == BLEEDING || this == DRAINED

    /** The sheet's "increased damage against X enemies" stat for this ailment. */
    val against: String get() = "STOCK_DAMAGE_VS_$name"
    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name }

        /** An ailment as a skill names it: `IGNITE`, `CHILL`, `FREEZE`, `SHOCK`, `POISON`, `BLEED`, `WOUND`, `PARCH`, `DECAY`, `DRAIN`. */
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
