package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.core.model.campaign.AilmentRule
import com.sperance.exileforge.core.model.campaign.CombatRules
import com.sperance.exileforge.core.model.campaign.ManaRule
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import com.sperance.exileforge.core.model.powers.PowerEvent
import com.sperance.exileforge.core.model.skills.MonsterSkill
import com.sperance.exileforge.core.model.skills.SkillAilment
import com.sperance.exileforge.core.model.skills.SkillBarrier
import com.sperance.exileforge.core.model.skills.SkillDot
import com.sperance.exileforge.core.model.skills.SkillEvent
import com.sperance.exileforge.core.model.skills.SkillHeal
import com.sperance.exileforge.core.model.skills.SkillHit
import com.sperance.exileforge.core.model.skills.SkillTrigger
import com.sperance.exileforge.core.model.skills.SkillType
import com.sperance.exileforge.core.model.skills.SlotCondition
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Who acted. */
enum class Side { HERO, MONSTER; val other: Side get() = if (this == HERO) MONSTER else HERO }

/**
 * What a fighter did: swung a weapon, let an ailment burn on, gave a blow back (thorns and reflect, 2.75.0),
 * turned to leave, used a skill — a class's, a passive's answer or a monster's — or drank a flask (2.78.0).
 */
enum class Action { ATTACK, TICK, REFLECT, RETREAT, SKILL, FLASK }

/** How a blow ended: it landed, landed hard, or never reached. */
enum class HitKind { HIT, CRIT, EVADED, BLOCKED }

/** How the whole fight ended; a retreat is the hero walking out of it. */
enum class Outcome { WIN, LOSS, RETREAT }

/** Damage by type, as the sheet names it. */
enum class DamageType(val attack: String, val resist: String?) {
    PHYSICAL("STOCK_ATTACK_PHYSICAL", null),
    FIRE("STOCK_ATTACK_FIRE", "STOCK_RESIST_FIRE"),
    COLD("STOCK_ATTACK_COLD", "STOCK_RESIST_COLD"),
    LIGHTNING("STOCK_ATTACK_LIGHTNING", "STOCK_RESIST_LIGHTNING"),
    CHAOS("STOCK_ATTACK_CHAOS", "STOCK_RESIST_CHAOS");

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
 * The six ailments the server rules (its `EnumStatBool` without the prefix). Which damage brings which is the rule's, not ours.
 * [word] is how the sheet's stats name it since server 0.36.0 — `STOCK_IGNITE_CHANCE`, `STOCK_AVOID_IGNITE`,
 * `STOCK_IGNITE_DURATION_ON_SELF` — and [damage] the stat that makes its damage over time heavier.
 */
enum class Ailment(val word: String, val damage: String? = null) {
    BURNING("IGNITE", "STOCK_BURNING_DAMAGE"), CHILLED("CHILL"), FROZEN("FREEZE"), SHOCKED("SHOCK"),
    POISONED("POISON", "STOCK_POISON_DAMAGE"), BLEEDING("BLEED", "STOCK_BLEED_DAMAGE");
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

/**
 * One side of a fight, read off a stat table — the hero's sheet or a rolled monster — under the
 * server's [rules].
 *
 * Every number here is one the server sent; the fight only decides what they do to each other. A
 * hero with no weapon still swings — unarmed, as in PoE — and a missing critical chance is the one
 * every attack has. Mana came back in 2.78.0 (server 0.69.0) with the class skills and the monsters'
 * spells: it is what a skill is paid with, and it comes back by the rule's share a second.
 */
data class Combatant(val stats: Map<String, Double>, val level: Int, val rules: CombatRules = CombatRules()) {
    private fun stat(name: String) = stats[name] ?: 0.0
    private fun percent(name: String, cap: Double = 100.0) = stat(name).coerceIn(0.0, cap) / 100

    /** A stat of the sheet as it stands, zero when it has none. */
    operator fun get(name: String): Double = stat(name)

    val maxLife = max(1.0, stat("STOCK_HEALTH"))
    /** A monster's «shield of life» (an essence, server 0.69.0) adds a share of its life to its shield. */
    val maxShield = max(0.0, stat("STOCK_ENERGY_SHIELD")) + maxLife * max(0.0, stat("STOCK_SHIELD_OF_LIFE")) / 100
    val maxMana = max(0.0, stat("STOCK_MANA"))
    /** Mana back a second (server 0.69.0): the rule's share of the maximum, faster by the sheet's regeneration. */
    fun manaRegen(rule: ManaRule): Double = maxMana * rule.regen / 100 * max(0.0, 1 + stat("STOCK_MANA_REGEN") / 100)
    /** Every damage it deals, as a multiplier: the sheet's "damage" (server 0.69.0), a monster's "deals more damage". */
    val damageMore = max(0.0, 1 + stat("STOCK_DAMAGE") / 100)
    /** How fast its skills recover: the cooldown recovery, and a spell's cast speed on top. */
    fun recovery(spell: Boolean): Double = max(0.1, 1 + (stat("STOCK_COOLDOWN_RECOVERY") + if (spell) stat("STOCK_CAST_SPEED") else 0.0) / 100)
    /** What is left of a skill's price in mana. */
    val skillCost = max(0.0, 1 - stat("STOCK_SKILL_COST") / 100)
    val skillHealing = max(0.0, 1 + stat("STOCK_SKILL_HEALING") / 100)
    /** Immune to [ailment] (a flask's suffix, server 0.69.0); an immunity to freezing keeps the cold's chill off too. */
    fun immune(ailment: Ailment): Boolean = stat("STOCK_IMMUNE_${ailment.word}") > 0 || (ailment == Ailment.CHILLED && stat("STOCK_IMMUNE_FREEZE") > 0)
    val immuneCurse: Boolean get() = stat("STOCK_IMMUNE_CURSE") > 0
    val immuneStun: Boolean get() = stat("STOCK_IMMUNE_STUN") > 0
    /** Life back a second as a share of the maximum (server 0.69.0), beside the flat regeneration. */
    val lifeRegenShare = max(0.0, stat("STOCK_LIFE_REGEN_PERCENT")) / 100
    val leechMana = max(0.0, stat("STOCK_LEECH_MANA")) / 100
    val manaOnHit = max(0.0, stat("STOCK_MANA_ON_HIT"))
    val manaOnKill = max(0.0, stat("STOCK_MANA_ON_KILL"))
    /** The share of the struck one's maximum mana its blows burn (a monster's essence). */
    val manaBurn = max(0.0, stat("STOCK_MANA_BURN")) / 100
    /** How much more damage over time, a critical strike, a bleeding and a shock do to this fighter — a curse's work. */
    val dotTaken = max(0.0, 1 + stat("STOCK_DOT_TAKEN") / 100)
    val critTaken = max(0.0, stat("STOCK_CRITICAL_TAKEN")) / 100
    fun ailmentTaken(ailment: Ailment): Double = when (ailment) {
        Ailment.BLEEDING -> max(0.0, 1 + stat("STOCK_BLEED_TAKEN") / 100)
        Ailment.SHOCKED -> max(0.0, 1 + stat("STOCK_SHOCK_TAKEN") / 100)
        else -> 1.0
    }
    val damage: Map<DamageType, Double> = DamageType.entries.associateWith { max(0.0, stat(it.attack)) }
        .let { rolled -> if (rolled.values.sum() > 0) rolled else rolled + (DamageType.PHYSICAL to rules.unarmed.damage) }
    val attackSpeed = stat("STOCK_ATTACK_SPEED").takeIf { it > 0 }?.coerceIn(0.3, 5.0) ?: rules.unarmed.speed
    val critChance = (stats["STOCK_CRITICAL_CHANCE"] ?: rules.critical.chance).coerceIn(0.0, 100.0) / 100
    val critMultiplier = max(100.0, (stats["STOCK_CRITICAL_MULTIPLIER"] ?: rules.critical.multiplier) + stat("STOCK_CRITICAL_DAMAGE")) / 100
    val armour = max(0.0, stat("STOCK_ARMOR"))
    val evasion = max(0.0, stat("STOCK_EVASION"))
    val block = stat("STOCK_BLOCK_CHANCE").coerceIn(0.0, rules.blockCap) / 100
    /** Taken off physical damage after armour, under armour's own cap. */
    val physicalReduction = percent("STOCK_PHYSICAL_REDUCTION", rules.armour.cap)
    /**
     * Chaos stands alone, as in PoE; "all resistances" and "all maximum resistances" cover the three elements.
     * [penetration] (server 0.66.0) is the striker's: it is taken off the resistance, and can push it below
     * zero down to minus the cap, so a monster without resistance is still hurt more by a penetrating blow.
     */
    fun resist(type: DamageType, penetration: Double = 0.0): Double {
        val name = type.resist ?: return 0.0
        val chaos = name == "STOCK_RESIST_CHAOS"
        val ceiling = (rules.resistCap + stat(type.maxResist.orEmpty()) + (if (chaos) 0.0 else stat("STOCK_RESIST_MAX_ALL"))).coerceIn(0.0, rules.resistHardCap)
        val own = (stat(name) + (if (chaos) 0.0 else stat("STOCK_RESIST_ALL"))).coerceIn(0.0, ceiling)
        return (own - penetration).coerceIn(-rules.resistCap, ceiling) / 100
    }
    /** How much of the target's [type] resistance this fighter's blows ignore, in percent (server 0.66.0). */
    fun penetration(type: DamageType): Double = when (type) {
        DamageType.PHYSICAL -> 0.0
        DamageType.CHAOS -> max(0.0, stat("STOCK_PENETRATE_CHAOS"))
        else -> max(0.0, stat("STOCK_PENETRATE_${type.name}")) + max(0.0, stat("STOCK_PENETRATE_ELEMENTAL"))
    }
    /**
     * What a blow of [type] does to this fighter after its defences (server 0.66.0): "damage taken" of every
     * kind and of that kind together, never below a tenth so no stack of it makes a fighter untouchable.
     */
    fun damageTaken(type: DamageType): Double {
        val own = when (type) {
            DamageType.PHYSICAL -> stat("STOCK_PHYSICAL_TAKEN")
            DamageType.CHAOS -> stat("STOCK_CHAOS_TAKEN")
            else -> stat("STOCK_ELEMENTAL_TAKEN")
        }
        return ((1 + stat("STOCK_DAMAGE_TAKEN") / 100) * (1 + own / 100)).coerceAtLeast(0.1)
    }
    /** How much harder this fighter hits a target under [ailments]: any ailment counts once, each named one on top. */
    fun damageAgainst(ailments: Collection<Ailment>): Double {
        if (ailments.isEmpty()) return 1.0
        val named = ailments.toSet().sumOf { max(0.0, stat(it.against)) }
        return 1 + (max(0.0, stat("STOCK_DAMAGE_VS_AILED")) + named) / 100
    }
    /** How much longer the ailments this fighter inflicts last on its foes. */
    fun ailmentDurationOnFoes(ailment: Ailment): Double =
        1 + (max(0.0, stat("STOCK_AILMENT_DURATION")) + max(0.0, stat("STOCK_${ailment.word}_DURATION"))) / 100
    /** The pace of everything that gives life or shield back: regeneration, leech, on hit and on kill (server 0.66.0). */
    val recoveryRate = max(0.0, 1 + stat("STOCK_RECOVERY_RATE") / 100)
    /** The pace of the shield's recharge after the rule's delay. */
    val shieldRecharge = max(0.0, 1 + stat("STOCK_SHIELD_RECHARGE") / 100)
    /** Flat physical damage every attacker takes on hit, and the share of any damage taken given back the same way. */
    val thorns = max(0.0, stat("STOCK_THORNS"))
    val reflect = max(0.0, stat("STOCK_REFLECT")) / 100
    /** What this fighter's presence does to its foes while it stands (server 0.66.0): the `AURA_*` stats, if any. */
    val auras: Map<String, Double> = stats.filterKeys { it.startsWith("AURA_") }.filterValues { it > 0 }
    /**
     * This fighter under the [auras] of the foes still standing: fewer resistances, more damage taken, slower
     * swings and slower recovery. The same sheet with the auras written into it, so every reading stays one.
     */
    fun under(auras: Map<String, Double>): Combatant {
        if (auras.isEmpty()) return this
        val sheet = stats.toMutableMap()
        // Server 0.69.0, the essences: weaker blows, fewer criticals and slower skills near their guardian.
        auras["AURA_WEAKEN"]?.let { v -> sheet["STOCK_DAMAGE"] = (100 + (stats["STOCK_DAMAGE"] ?: 0.0)) * max(0.0, 1 - v / 100) - 100 }
        auras["AURA_CRIT"]?.let { v -> sheet["STOCK_CRITICAL_CHANCE"] = critChance * 100 * max(0.0, 1 - v / 100) }
        auras["AURA_COOLDOWN"]?.let { v -> sheet["STOCK_COOLDOWN_RECOVERY"] = (stats["STOCK_COOLDOWN_RECOVERY"] ?: 0.0) - v }
        auras["AURA_RESIST"]?.let { v -> sheet["STOCK_RESIST_ALL"] = (stats["STOCK_RESIST_ALL"] ?: 0.0) - v; sheet["STOCK_RESIST_CHAOS"] = (stats["STOCK_RESIST_CHAOS"] ?: 0.0) - v }
        auras["AURA_DAMAGE_TAKEN"]?.let { v -> sheet["STOCK_DAMAGE_TAKEN"] = (stats["STOCK_DAMAGE_TAKEN"] ?: 0.0) + v }
        auras["AURA_SLOW"]?.let { v -> sheet["STOCK_ATTACK_SPEED"] = attackSpeed * max(0.1, 1 - v / 100) }
        auras["AURA_RECOVERY"]?.let { v -> sheet["STOCK_RECOVERY_RATE"] = (stats["STOCK_RECOVERY_RATE"] ?: 0.0) - v }
        return Combatant(sheet, level, rules)
    }
    val lifeRegen = max(0.0, stat("STOCK_HEALTH_REGEN"))
    val shieldRegen = max(0.0, stat("STOCK_ENERGY_REGEN"))
    val leechPhysical = max(0.0, stat("STOCK_LEECH_PHYSICAL")) / 100
    val leechAll = max(0.0, stat("STOCK_LEECH_ALL")) / 100
    val critLeech = max(0.0, stat("STOCK_CRITICAL_VAMPIRE")) / 100
    val stunThreshold = max(0.0, stat("STOCK_STUN_THRESHOLD"))
    val avoidStun = percent("STOCK_AVOID_STUN")
    /** What gear adds to the rule's chance to inflict [ailment], in percent; chill has no such stat. */
    fun inflictChance(ailment: Ailment) = max(0.0, stat("STOCK_${ailment.word}_CHANCE"))
    /** How much heavier [ailment]'s damage over time runs. */
    fun ailmentDamage(ailment: Ailment) = 1 + max(0.0, ailment.damage?.let(::stat) ?: 0.0) / 100
    fun avoid(ailment: Ailment) = percent("STOCK_AVOID_${ailment.word}")
    /** What is left of [ailment]'s duration on this fighter, never less than the rule's cap allows. */
    fun ailmentDuration(ailment: Ailment) = 1 - percent("STOCK_${ailment.word}_DURATION_ON_SELF", rules.ailmentDurationCap)
    val lifeOnHit = max(0.0, stat("STOCK_HEALTH_ON_HIT"))
    val lifeOnKill = max(0.0, stat("STOCK_HEALTH_ON_KILL"))
    /** Taunts (since server 0.62.0): while it stands, its foes must strike it first, past any row. */
    val taunt: Boolean get() = stat("STOCK_TAUNT") > 0
    /** How hard it presses, before anyone's defences: its damage per swing times its swings per second. */
    val threat: Double get() = damage.values.sum() * attackSpeed
    /** The damage type most of its blow is made of. */
    val leading: DamageType get() = damage.maxBy { it.value }.key
}

/**
 * An ailment on a fighter: what, until when, how hard, and who put it there — the side and, for a
 * monster's, which foe of the pack ([foe], since 2.70.0).
 */
data class ActiveAilment(val ailment: Ailment, val until: Double, val magnitude: Double, val duration: Double, val source: Side, val foe: Int = 0,
    /** Put there by a spell (2.78.0): a kill by it is a spell's kill. */
    val spell: Boolean = false)

/**
 * One foe of the pack as the fight takes it (2.70.0): its sheet and its row — a [ranged] foe
 * stands in the back and strikes from the first second; a melee one stands in front of it. Since
 * 2.78.0 its [rarity] tells a skill waiting for a rare or a boss, and its [skills] are what it casts
 * for its mana — a boss's own, a caster's spell of its element, one a mad essence borrowed.
 */
data class Foe(val body: Combatant, val ranged: Boolean = false, val rarity: MonsterRarity = MonsterRarity.NORMAL,
               val skills: List<MonsterSkill> = emptyList())

/** What lies on a fighter for a while (2.78.0). */
enum class EffectKind { BUFF, CURSE, FLASK }

/**
 * A buff, a curse or a flask's draught on a fighter (2.78.0): its [lines] until [until], named by its
 * [source] — a skill's code or a flask's. [counter] strikes back at a blocked blow with that percent of
 * the weapon; [slot] is a flask's place on the belt.
 */
data class TimedEffect(val kind: EffectKind, val source: String, val lines: List<StatLine>, val until: Double, val duration: Double,
                       val counter: Double = 0.0, val slot: Int = -1)

/**
 * What the hero carries from fight to fight (2.78.0): life, mana, each flask's charges and how long
 * its draught still runs, in seconds.
 */
data class HeroPools(val life: Double, val mana: Double, val charges: List<Double> = emptyList(), val flaskLeft: List<Double> = emptyList(),
                     val rates: List<DraughtRate> = emptyList())

/**
 * How much life and mana a running draught still gives each second (2.81.0). It rides with the draught's
 * time left, so a flask drunk at the end of a fight keeps healing on the map and in the next fight.
 */
data class DraughtRate(val life: Double = 0.0, val mana: Double = 0.0) {
    val flows: Boolean get() = life > 0 || mana > 0
}

/** An active slot as the fight's buttons draw it (2.78.0): how far it has recovered (1 ready), and whether the mana is there. */
data class SkillView(val slot: Int, val code: String, val icon: String, val level: Int, val cost: Int, val ready: Float, val affordable: Boolean,
                     val condition: SlotCondition, val seconds: Double = 0.0)

/** A flask of the belt as its button draws it (2.78.0): charges, the price of a draught, and how much of one is left (0 none). */
data class FlaskView(val slot: Int, val code: String, val kind: FlaskKind, val charges: Int, val maxCharges: Int, val perUse: Int, val active: Float,
                     val condition: SlotCondition) {
    val usable: Boolean get() = charges >= perUse && active <= 0f
}

/** A buff or a curse on a fighter as its tile shows it (2.78.0): what, of which kind, and how much of it is left. */
data class EffectView(val source: String, val kind: EffectKind, val left: Float, val seconds: Double, val icon: String = "")

/**
 * Whom the hero strikes when the player has not said (2.70.0), by class — the owner's table: a
 * Marauder, a Duelist and a Ranger go for the one that presses hardest, a Shadow finishes the
 * weakest, a Witch the one least resistant to her leading element, a Templar answers whoever
 * struck last, a Scion finishes the weakest while healthy and answers when hurt.
 */
enum class TargetRule {
    THREAT, WEAKEST, EXPOSED, AVENGE, ADAPTIVE;

    companion object {
        fun of(classCode: String?): TargetRule = when (classCode) {
            "SHADOW" -> WEAKEST
            "WITCH" -> EXPOSED
            "TEMPLAR" -> AVENGE
            "SCION" -> ADAPTIVE
            else -> THREAT
        }
    }
}

/**
 * The hero's side of a fight beyond the sheet (2.70.0): whom they pick by [rule], and whether
 * their weapon reaches the back row while the front still stands — a bow or a wand does, a blade
 * or bare hands do not.
 */
data class HeroStance(val rule: TargetRule = TargetRule.THREAT, val ranged: Boolean = false) {
    companion object {
        private val reaching = setOf("BOW", "WAND")
        fun of(classCode: String?, weaponType: String?) = HeroStance(TargetRule.of(classCode), weaponType in reaching)
    }
}

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
) {
    /** Who the number floats off. */
    val target: Side get() = if (onSelf) actor else actor.other
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
) {
    /** A weapon's blow: attacks, not spells, bring life on hit. */
    val weapon: Boolean get() = !spell && (action == Action.ATTACK || action == Action.SKILL)
}

/** Life and mana a draught gives a second until [until] (2.78.0). */
private class Recovery(val life: Double, val mana: Double, val until: Double, val slot: Int)

/**
 * The fight, alive: stepped in fixed slices of time so that the same seed is the same fight on any
 * screen, and open to the player while it runs — a retreat can be begun, a foe can be singled out.
 *
 * Since 2.70.0 it is the hero against the whole pack at once, in two rows: every foe swings at the
 * hero at its own speed from the first second, and the hero at one foe of the rows its weapon
 * reaches — the back row only with a bow or a wand, or once the front has fallen. Whom is the
 * player's [focus] when given, the class's [TargetRule] otherwise, chosen afresh at every swing.
 *
 * Each swing can be evaded (evasion against the attacker's level), blocked, or land; a landing hit
 * rolls the rule's variance per damage type, may be a critical strike, and is reduced by armour
 * (physical, `armour / (armour + factor × damage)`) and by resistances (elements and chaos,
 * capped). Energy shield takes a hit before life, except chaos, which goes around it; a shield left
 * alone for the rule's delay recharges. Leech gives back a share of what was dealt; a hit big enough
 * against the target's life stuns it, and a frozen or stunned fighter does nothing until it passes.
 * Every landing hit may inflict the ailments its damage types carry: burning, poison and bleeding
 * deal a share of the hit over time, chill slows the target's actions, shock makes it take more, a
 * freeze stops it. Life and shield regenerate as they go.
 *
 * Since 2.78.0 (server 0.69.0) mana is back, and the hero brings a [Loadout]: the active skills are
 * tried in their slots' order whenever one is ready and its condition holds — a ready one short of
 * mana holds back the ones after it — or at a tap; the passives lie on the sheet or answer the fight's
 * events; the flasks are drunk by their conditions or a tap. Bosses and casters cast for their own
 * mana. A buff, a curse or a draught lies on a fighter as a [TimedEffect], and its body is made again
 * from its sheet with their lines whenever one comes or goes.
 *
 * The client fights by the owner's decision (rule 23); every constant here is the server's [rules].
 */
class Battle(
    val hero: Combatant,
    val foes: List<Foe>,
    val rules: CombatRules,
    heroLife: Double,
    internal val random: Random,
    val stance: HeroStance = HeroStance(),
    /** How many fight on the hero's side (2.71.0); alone, the hero is a lone wolf. */
    val party: Int = 1,
    /** What the hero brings beyond the sheet (2.78.0), and how their sheet takes the lines of the moment. */
    val kit: Loadout = Loadout(),
    private val model: HeroModel = HeroModel.of(hero),
    /** What the hero walks in with (2.78.0): mana, the flasks' charges and draughts still running; null brings them full. */
    pools: HeroPools? = null,
    /** The stats that are a percent already: the lines laid on a monster fold by them. */
    private val percent: Set<String> = emptySet(),
) {
    /** «Волк-одиночка»: the hero alone deals more and takes less of every damage, by the server's [CombatRules.loneWolf]. */
    val loneWolf: Boolean get() = party <= 1
    /** One side in motion: its pools, its clocks and what is on it; [index] is its place in the pack, -1 for the hero. */
    inner class Fighter(val side: Side, body: Combatant, life: Double, val index: Int = -1, val ranged: Boolean = false) {
        /** The sheet as it stands: the hero's changes under the auras of the foes still standing (2.75.0) and with what lies on them (2.78.0). */
        var body: Combatant = body
            private set
        /** A monster's sheet taking the lines laid on it (2.78.0). */
        val model: BodyModel = BodyModel.of(body, percent)
        var life = life.coerceIn(0.0, body.maxLife)
        var shield = body.maxShield
        /** Mana (2.78.0): the hero's comes in from the fight before, a monster's is full. */
        var mana = body.maxMana
        var nextAttack = if (side == Side.HERO) 0.35 else 0.55 + index * 0.13
        var attackInterval = 1 / body.attackSpeed
        /** A new sheet mid-fight: the pools keep their share, the swing keeps its pace from the next one. */
        fun rebody(next: Combatant) {
            if (next === body) return
            life = if (body.maxLife > 0) life / body.maxLife * next.maxLife else next.maxLife
            shield = if (body.maxShield > 0) shield / body.maxShield * next.maxShield else min(shield, next.maxShield)
            mana = min(mana, next.maxMana)
            body = next
            attackInterval = 1 / next.attackSpeed
        }
        /** Stunned or frozen until then: nothing is swung before it. */
        var heldUntil = 0.0
        var lastHit = -1e9
        val ailments = mutableListOf<ActiveAilment>()
        /** Damage over time gathered per ailment since its last tick was logged, and when that was. */
        val ticking = mutableMapOf<Ailment, Double>()
        val tickedAt = mutableMapOf<Ailment, Double>()
        /** Buffs, curses and draughts on it (2.78.0). */
        val effects = mutableListOf<TimedEffect>()
        /** What a barrier still soaks, and until when (2.78.0). */
        var barrier = 0.0
        var barrierUntil = 0.0
        var invulnerableUntil = -1.0
        /** When each of its skills is ready again, by code for a monster and by slot for the hero (2.78.0). */
        val readyAt = mutableMapOf<String, Double>()
        /** Its low-life lines are on (2.78.0). */
        var low = false

        val alive: Boolean get() = life > 0
        val held: Boolean get() = heldUntil > time
        val cursed: Boolean get() = effects.any { it.kind == EffectKind.CURSE }
        val invulnerable: Boolean get() = invulnerableUntil > time
        fun frozen() = ailments.any { it.ailment == Ailment.FROZEN }
        /** Chill slows every action; the strongest chill counts. */
        fun slow() = 1 + (ailments.filter { it.ailment == Ailment.CHILLED }.maxOfOrNull { it.magnitude } ?: 0.0) / 100
        /** Shock makes every hit heavier; a curse can make it heavier still (2.78.0). */
        fun weakness() = 1 + (ailments.filter { it.ailment == Ailment.SHOCKED }.maxOfOrNull { it.magnitude } ?: 0.0) * body.ailmentTaken(Ailment.SHOCKED) / 100
        fun stacks(ailment: Ailment) = ailments.count { it.ailment == ailment }
    }

    val foeFighters: List<Fighter> = foes.mapIndexed { i, foe -> Fighter(Side.MONSTER, foe.body, foe.body.maxLife, i, foe.ranged) }
    val heroFighter = Fighter(Side.HERO, hero.under(auras()), heroLife)

    /** The auras of the foes still standing, summed per stat (server 0.66.0). */
    private fun auras(): Map<String, Double> {
        val sum = mutableMapOf<String, Double>()
        foeFighters.filter { it.alive }.forEach { foe -> foe.body.auras.forEach { (stat, value) -> sum.merge(stat, value, Double::plus) } }
        return sum
    }
    private val ailmentRules: Map<AilmentRule, Pair<Ailment, DamageType>> = rules.ailments
        .mapNotNull { rule -> Ailment.of(rule.ailment)?.let { a -> DamageType.of(rule.type)?.let { t -> rule to (a to t) } } }.toMap()
    private val ruleOf: Map<Ailment, Pair<AilmentRule, DamageType>> = ailmentRules.entries.associate { (rule, what) -> what.first to (rule to what.second) }

    var time = 0.0
        private set
    /** When the fight ended; equals [time] then, and stays. */
    var duration = 0.0
        private set
    var outcome: Outcome? = null
        private set
    /** The foe the player singled out, until it falls or is tapped again. */
    var focus: Int? = null
        private set
    /** The foe that struck the hero last — the Templar's answer. */
    private var lastStriker: Int? = null
    private var retreatAt = Double.NaN
    private var carry = 0.0
    private val log = mutableListOf<CombatEvent>()
    val events: List<CombatEvent> get() = log
    private val fallenOrder = mutableListOf<Int>()
    /** The foes that fell, in the order they fell: each is a kill to report the moment it happens. */
    val fallen: List<Int> get() = fallenOrder

    // ==================== The hero's skills and flasks (2.78.0) ====================

    /** A slot whose opening condition has fired this fight, and the slots tapped since the last slice. */
    private val opened = BooleanArray(kit.actives.size)
    private val taps = mutableSetOf<Int>()
    private val charges = DoubleArray(kit.flasks.size) { i -> kit.flasks[i]?.let { pools?.charges?.getOrNull(i)?.coerceIn(0.0, it.maxCharges) ?: it.maxCharges } ?: 0.0 }
    private val flaskOpened = BooleanArray(kit.flasks.size)
    private val drinks = mutableSetOf<Int>()
    private val triggerReady = mutableMapOf<String, Double>()
    private val recoveries = mutableListOf<Recovery>()
    /** The next blow of the hero is a critical strike (a cloak of shadows). */
    internal var nextCrit = false
    /** How deep in answers the fight is: an answer may set off one more, never a chain. */
    private var depth = 0
    private var belowLow = false
    private var shieldUp = true
    /** The hero's powers (2.79.0): the unique items' answers to what happens here. */
    private val powers = PowerRunner(this, kit.powers)

    init {
        // A draught still running from the map comes into the fight, and the belt's opening ones count as drunk.
        pools?.flaskLeft?.forEachIndexed { i, left ->
            val flask = kit.flasks.getOrNull(i) ?: return@forEachIndexed
            if (left <= 0) return@forEachIndexed
            val draught = flask.draught(heroFighter.body, heroFighter.life, 0.0)
            heroFighter.effects += TimedEffect(EffectKind.FLASK, flask.code, draught.lines, left, draught.duration, slot = i)
            // Its recovery comes along with it (2.81.0): before, the draught's buff ran on but its healing stopped.
            pools?.rates?.getOrNull(i)?.takeIf { it.flows }?.let { recoveries += Recovery(it.life, it.mana, left, i) }
            flaskOpened[i] = true
        }
        if (heroFighter.effects.isNotEmpty()) remake(heroFighter)
        heroFighter.mana = (pools?.mana ?: Double.MAX_VALUE).coerceIn(0.0, manaCap())
        shieldUp = heroFighter.shield > 0
    }

    fun foe(index: Int) = foeFighters[index]
    val heroLife: Double get() = heroFighter.life
    val heroMana: Double get() = heroFighter.mana
    val retreating: Boolean get() = !retreatAt.isNaN()

    /** The hero's mana the auras leave free (2.78.0). */
    fun manaCap(): Double = heroFighter.body.maxMana * (1 - kit.reserved(heroFighter.body) / 100)
    private fun manaCap(fighter: Fighter): Double = if (fighter === heroFighter) manaCap() else fighter.body.maxMana

    /** Uses the skill of active slot [slot] at the next slice, if it is ready and the mana is there — its condition aside. */
    fun useSkill(slot: Int) { if (kit.actives.getOrNull(slot) != null) taps += slot }

    /** Drinks the flask of belt place [slot] at the next slice, if it has the charges and is not running already. */
    fun useFlask(slot: Int) { if (kit.flasks.getOrNull(slot) != null) drinks += slot }

    /** What the hero walks out with: life, mana, the flasks' charges and the seconds each draught still runs. */
    fun pools(): HeroPools = HeroPools(heroFighter.life, heroFighter.mana, charges.toList(),
        kit.flasks.indices.map { i -> draughtOf(i)?.let { (it.until - time).coerceAtLeast(0.0) } ?: 0.0 },
        kit.flasks.indices.map { i -> recoveries.firstOrNull { it.slot == i && it.until > time }?.let { DraughtRate(it.life, it.mana) } ?: DraughtRate() })

    /** The active slots as their buttons draw them; null where a slot is empty. */
    fun skillViews(): List<SkillView?> = kit.actives.mapIndexed { slot, kitSkill ->
        kitSkill?.let {
            val body = heroFighter.body
            val level = it.level(body)
            val cost = cost(it, level)
            val cooldown = it.skill.cooldown / body.recovery(it.skill.spell)
            val left = ((heroFighter.readyAt[slotKey(slot)] ?: 0.0) - time).coerceAtLeast(0.0)
            SkillView(slot, it.skill.code, it.skill.icon, level, cost.roundToInt(), if (cooldown > 0) (1 - left / cooldown).toFloat().coerceIn(0f, 1f) else 1f,
                skillsFree() || heroFighter.mana + 1e-9 >= cost, it.condition, left)
        }
    }

    /** The belt as its buttons draw it; null where a place is empty. */
    fun flaskViews(): List<FlaskView?> = kit.flasks.mapIndexed { i, flask ->
        flask?.let {
            val running = draughtOf(i)
            FlaskView(i, it.code, it.kind, charges[i].toInt(), it.maxCharges.toInt(), ceil(it.perUse(heroFighter.body) - 1e-9).toInt(),
                running?.let { d -> ((d.until - time) / d.duration).toFloat().coerceIn(0f, 1f) } ?: 0f, it.condition)
        }
    }

    /** What lies on [fighter], for its tiles. */
    fun effects(fighter: Fighter): List<EffectView> = fighter.effects.filter { it.kind != EffectKind.FLASK }.map {
        EffectView(it.source, it.kind, ((it.until - time) / it.duration).toFloat().coerceIn(0f, 1f), (it.until - time).coerceAtLeast(0.0), icons[it.source].orEmpty())
    }

    /** The drawings of every skill in this fight, by code: the hero's and the foes'. */
    private val icons: Map<String, String> = (kit.actives.filterNotNull() + kit.passives + kit.curses).associate { it.skill.code to it.skill.icon } +
        foes.flatMap { it.skills }.associate { it.code to it.icon }

    private fun draughtOf(slot: Int): TimedEffect? = heroFighter.effects.firstOrNull { it.kind == EffectKind.FLASK && it.slot == slot && it.until > time }
    private fun skillsFree(): Boolean = kit.flasks.indices.any { i -> kit.flasks[i]?.skillsFree == true && draughtOf(i) != null }
    private fun slotKey(slot: Int) = "#$slot"
    private fun cost(kitSkill: KitSkill, level: Int): Double = (kitSkill.skill.mana?.at(level) ?: 0.0) * heroFighter.body.skillCost

    /** Moves the fight on by [dt] seconds in fixed slices, so a frame's length never changes what happens. */
    fun advance(dt: Double) {
        // Once it is over the clock still runs, so the last blow's lunge and fade can play out.
        if (outcome != null) { time += dt; return }
        carry += dt
        while (carry >= STEP - 1e-12 && outcome == null) { carry -= STEP; step(STEP) }
    }

    /** Turns to leave: the hero stops swinging, the pack gets the rule's delay of free swings, then the fight is over. */
    fun retreat(): Boolean {
        if (outcome != null || retreating) return false
        retreatAt = time + rules.retreat.delay
        record(Side.HERO, Action.RETREAT, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, target()?.index ?: 0)
        return true
    }

    /** Singles out foe [index]; the same foe again, or a fallen one, gives the choice back to the class. */
    fun focus(index: Int?) {
        focus = index?.takeIf { it != focus && foeFighters.getOrNull(it)?.alive == true }
    }

    /** Foes taunting right now: while any stands, only they can be struck. */
    private fun taunters() = foeFighters.filter { it.alive && it.body.taunt }

    /**
     * Whether the hero can strike foe [index] right now: a taunter always, past any row (2.71.0),
     * and while one stands nobody else; otherwise as the weapon reaches — a spell reaches every row (2.78.0).
     */
    fun reachable(index: Int, spell: Boolean = false): Boolean {
        val foe = foeFighters.getOrNull(index)?.takeIf { it.alive } ?: return false
        if (taunters().isNotEmpty()) return foe.body.taunt
        return spell || stance.ranged || !foe.ranged || foeFighters.none { it.alive && !it.ranged }
    }

    /**
     * The foe the hero's next swing goes to: the focus while it can be struck, else the class's
     * pick among those that can. A focus behind the front, or behind a taunter, waits its turn.
     */
    fun target(): Fighter? {
        val reach = foeFighters.filter { reachable(it.index) }
        if (reach.isEmpty()) return null
        focus?.let { f -> reach.firstOrNull { it.index == f }?.let { return it } }
        fun weakest() = reach.minBy { it.life + it.shield }
        fun threat() = reach.maxBy { it.body.threat }
        fun avenge() = lastStriker?.let { s -> reach.firstOrNull { it.index == s } } ?: threat()
        return when (stance.rule) {
            TargetRule.THREAT -> threat()
            TargetRule.WEAKEST -> weakest()
            TargetRule.EXPOSED -> hero.leading.let { type -> reach.minWith(compareBy<Fighter> { it.body.resist(type) }.thenBy { it.life + it.shield }) }
            TargetRule.AVENGE -> avenge()
            TargetRule.ADAPTIVE -> if (heroFighter.life >= heroFighter.body.maxLife / 2) weakest() else avenge()
        }
    }

    /** Whom a skill of [count] targets strikes: the hero's target first, then the others it reaches; zero is all of them. */
    private fun targets(count: Int, spell: Boolean): List<Fighter> {
        val reach = foeFighters.filter { reachable(it.index, spell) }
        if (reach.isEmpty()) return emptyList()
        val first = target()?.takeIf { it in reach } ?: reach.first()
        val order = listOf(first) + (reach - first)
        return if (count <= 0) order else order.take(count)
    }

    /** The blow whose lunge is on screen right now, and how far into it the scene is (0..1). */
    fun lunge(): Pair<CombatEvent, Double>? = log.lastOrNull { it.action != Action.TICK && it.action != Action.REFLECT && it.action != Action.FLASK && !it.onSelf &&
        time - it.time in -LUNGE..LUNGE }?.let { it to ((time - it.time + LUNGE) / (2 * LUNGE)).coerceIn(0.0, 1.0) }

    /** How far [fighter] is into its next swing, 0 just after one and 1 as the next lands. */
    fun swing(fighter: Fighter): Float = when {
        outcome != null || !fighter.alive || (fighter.side == Side.HERO && retreating) -> 0f
        else -> (1 - (fighter.nextAttack - time) / fighter.attackInterval).toFloat().coerceIn(0f, 1f)
    }

    /** The log as a test or a report reads it, once the fight is over. */
    fun log(): CombatLog = CombatLog(log.toList(), outcome ?: Outcome.RETREAT, heroLife, duration)

    // ==================== One slice of time ====================

    private fun step(dt: Double) {
        time += dt
        (listOf(heroFighter) + foeFighters).forEach { regenerate(it, dt); burn(it, dt) }
        expire()
        if (finished()) return
        powers.tick()
        if (finished()) return
        val hero = heroFighter
        // A draught goes down even stunned; a skill waits until the hero can move again.
        if (hero.alive && !retreating) {
            useFlasks()
            if (!hero.held) useSkills()
        }
        taps.clear()
        drinks.clear()
        foeFighters.forEach { if (it.alive && !it.held) monsterCast(it) }
        if (finished()) return
        // Whoever is due first acts first; several may be due in one slice.
        (listOf(heroFighter) + foeFighters).sortedBy { it.nextAttack }.forEach { me ->
            if (!me.alive || me.held || (me.side == Side.HERO && retreating) || me.nextAttack > time) return@forEach
            val target = if (me.side == Side.HERO) target() else heroFighter
            if (target != null) strike(me, target, Blow(me.body.damage))
            me.nextAttack = time + me.attackInterval * me.slow()
            if (finished()) return
        }
        watch()
        // No time limit since 2.74.0: a fight runs until a side falls or the hero walks out.
        if (retreating && time >= retreatAt) end(Outcome.RETREAT)
    }

    private fun regenerate(me: Fighter, dt: Double) {
        if (!me.alive) return
        me.life = min(me.body.maxLife, me.life + (me.body.lifeRegen + me.body.maxLife * me.body.lifeRegenShare) * me.body.recoveryRate * dt)
        val recharge = if (time - me.lastHit >= rules.shield.rechargeDelay) me.body.maxShield * rules.shield.rechargePerSecond / 100 * me.body.shieldRecharge else 0.0
        me.shield = min(me.body.maxShield, me.shield + (me.body.shieldRegen * me.body.recoveryRate + recharge) * dt)
        me.mana = min(manaCap(me), me.mana + me.body.manaRegen(rules.mana) * dt)
        if (me === heroFighter) recoveries.forEach { draught ->
            val slice = min(dt, draught.until - (time - dt)).coerceAtLeast(0.0)
            me.life = min(me.body.maxLife, me.life + draught.life * slice)
            me.mana = min(manaCap(), me.mana + draught.mana * slice)
        }
    }

    /** What ran out this slice goes: buffs, curses, draughts, a barrier, and the body is made again without them. */
    private fun expire() {
        (listOf(heroFighter) + foeFighters).forEach { fighter ->
            if (fighter.barrier > 0 && fighter.barrierUntil <= time) fighter.barrier = 0.0
            if (fighter.effects.removeAll { it.until <= time }) remake(fighter)
        }
        recoveries.removeAll { it.until <= time }
    }

    /** Edges the hero's passives answer — low life, a broken shield — and the low-life lines of either side. */
    private fun watch() {
        val hero = heroFighter
        if (hero.alive) {
            val below = hero.life < hero.body.maxLife * LOW_LIFE
            if (below && !belowLow) { trigger(SkillEvent.LOW_LIFE); powers.fire(PowerEvent.LOW_LIFE) }
            belowLow = below
            if (hero.body.maxShield > 0) {
                val up = hero.shield > 0.5
                if (!up && shieldUp) { trigger(SkillEvent.SHIELD_BROKEN); powers.fire(PowerEvent.SHIELD_BROKEN) }
                shieldUp = up
            }
            val low = hero.life < hero.body.maxLife / 2
            if (low != hero.low && model.lowLife.isNotEmpty()) { hero.low = low; remake(hero) }
            if (powers.restand()) remake(hero)
        }
        foeFighters.forEach { foe ->
            val low = foe.alive && foe.life < foe.body.maxLife / 2
            if (low != foe.low && foe.model.body(emptyList())["STOCK_LOW_LIFE_SPEED"] > 0) { foe.low = low; remake(foe) }
        }
    }

    /** [fighter]'s body made again from its sheet with what lies on it — and the hero's under the auras of the foes still standing. */
    private fun remake(fighter: Fighter) {
        val lines = fighter.effects.flatMap { it.lines }
        if (fighter === heroFighter) fighter.rebody(model.body((if (fighter.low) model.lowLife else emptyList()) + powers.standing + lines).under(auras()))
        else {
            val speed = fighter.model.body(emptyList())["STOCK_LOW_LIFE_SPEED"]
            fighter.rebody(fighter.model.body(lines + if (fighter.low && speed > 0) listOf(StatLine("STOCK_ATTACK_SPEED", ModifierOperation.INCREASED, speed)) else emptyList()))
        }
    }

    /** The hero's body for one blow: what lies on them and [extra], a skill's own lines. */
    private fun heroBody(extra: List<StatLine>): Combatant = model.body((if (heroFighter.low) model.lowLife else emptyList()) +
        powers.standing + heroFighter.effects.flatMap { it.lines } + extra).under(auras())

    /** Ailments run their course: damage over time is applied every slice and logged once a second. */
    private fun burn(me: Fighter, dt: Double) {
        if (me.ailments.isEmpty()) return
        val wasAlive = me.alive
        me.ailments.filter { it.ailment.hurts }.forEach { active ->
            val slice = active.magnitude * min(dt, active.until - (time - dt)).coerceAtLeast(0.0) * me.weakness() * me.body.dotTaken *
                me.body.ailmentTaken(active.ailment)
            if (slice <= 0 || !me.alive || me.invulnerable) return@forEach
            val chaos = active.ailment == Ailment.POISONED
            var rest = slice
            if (me.barrier > 0) { val soaked = min(me.barrier, rest); me.barrier -= soaked; rest -= soaked }
            val absorbed = if (chaos) 0.0 else min(me.shield, rest)
            me.shield -= absorbed
            me.life = max(0.0, me.life - (rest - absorbed))
            me.ticking.merge(active.ailment, slice, Double::plus)
        }
        val expired = me.ailments.filter { it.until <= time }
        val bySpell = me.ailments.any { it.spell && it.source == Side.HERO }
        me.ailments.removeAll(expired)
        me.ticking.keys.toList().forEach { ailment ->
            val since = me.tickedAt[ailment] ?: time.also { me.tickedAt[ailment] = it }
            val due = time - since >= TICK - 1e-9
            if (due || expired.any { it.ailment == ailment } || !me.alive) {
                val amount = me.ticking.remove(ailment) ?: 0.0
                me.tickedAt[ailment] = time
                if (amount > 0) {
                    val active = (me.ailments + expired).firstOrNull { it.ailment == ailment }
                    val source = active?.source ?: me.side.other
                    val type = ruleOf[ailment]?.second
                    record(source, Action.TICK, HitKind.HIT, amount, type, 0.0, false, emptyList(), null,
                        if (me.side == Side.MONSTER) me.index else active?.foe ?: 0)
                }
            }
        }
        if (wasAlive && !me.alive) fell(me, bySpell)
    }

    private fun evasion(me: Fighter, target: Fighter): Double =
        (target.body.evasion / (target.body.evasion + rules.evasion.base + rules.evasion.perLevel * me.body.level)).coerceAtMost(rules.evasion.cap / 100)

    /**
     * One blow of [me] at [target] — a weapon's swing, a skill's hit, a spell: evaded unless a spell,
     * blocked, or landed and maybe critical; then armour, resistance, shock and the lone wolf's share.
     */
    internal fun strike(me: Fighter, target: Fighter, blow: Blow) {
        val body = blow.body ?: me.body
        val foe = if (me.side == Side.MONSTER) me.index else target.index
        if (target.invulnerable) { record(me.side, blow.action, HitKind.BLOCKED, 0.0, null, 0.0, false, emptyList(), null, foe, blow.skill); return }
        val sure = me === heroFighter && nextCrit
        val kind = when {
            target.frozen() -> if (sure || random.nextDouble() < body.critChance) HitKind.CRIT else HitKind.HIT
            !blow.spell && random.nextDouble() < evasion(me, target) -> HitKind.EVADED
            random.nextDouble() < target.body.block -> HitKind.BLOCKED
            sure || random.nextDouble() < body.critChance -> HitKind.CRIT
            else -> HitKind.HIT
        }
        if (sure && kind == HitKind.CRIT) nextCrit = false
        if (me.side == Side.MONSTER) lastStriker = me.index
        if (kind == HitKind.EVADED || kind == HitKind.BLOCKED) {
            record(me.side, blow.action, kind, 0.0, null, 0.0, false, emptyList(), null, foe, blow.skill)
            if (target === heroFighter) {
                trigger(if (kind == HitKind.EVADED) SkillEvent.EVADE else SkillEvent.BLOCK, me)
                powers.fire(if (kind == HitKind.EVADED) PowerEvent.EVADE else PowerEvent.BLOCK, PowerMoment(me))
                if (kind == HitKind.BLOCKED) counter(me)
            }
            return
        }
        // The lone wolf's share rides the blow itself, so the ailments it brings carry it once and no more.
        val lone = when {
            !loneWolf -> 1.0
            me.side == Side.HERO -> 1 + rules.loneWolf.dealt / 100
            else -> 1 - rules.loneWolf.taken / 100
        }
        val multiplier = if (kind == HitKind.CRIT) body.critMultiplier + target.body.critTaken else 1.0
        // Server 0.66.0: a penetrating blow ignores part of the resistance, an ailed target takes more, and
        // "damage taken" of the target scales what got through; server 0.69.0: so does a curse on it.
        val against = body.damageAgainst(target.ailments.map { it.ailment }) * (if (target.cursed) 1 + max(0.0, body["STOCK_DAMAGE_VS_CURSED"]) / 100 else 1.0)
        val taken = blow.damage.filterValues { it > 0 }.mapValues { (type, base) ->
            val raw = base * (if (blow.spread) 1 + (random.nextDouble() * 2 - 1) * rules.variance / 100 else 1.0) * multiplier * against * body.damageMore
            when (type) {
                DamageType.PHYSICAL -> raw * (1 - (target.body.armour / (target.body.armour + rules.armour.factor * raw)).coerceAtMost(rules.armour.cap / 100)) * (1 - target.body.physicalReduction)
                else -> raw * (1 - target.body.resist(type, body.penetration(type)))
            }.coerceAtLeast(0.0) * target.weakness() * lone * target.body.damageTaken(type)
        }
        land(me, target, kind, taken, foe, blow, body)
        if (me.alive && target.body.thorns + target.body.reflect > 0) reflect(target, me, taken, foe)
    }

    /** A block under a riposte (2.78.0): the hero strikes the attacker back with that share of the weapon. */
    private fun counter(attacker: Fighter) {
        val riposte = heroFighter.effects.firstOrNull { it.counter > 0 } ?: return
        if (!attacker.alive) return
        strike(heroFighter, attacker, Blow(heroFighter.body.damage.mapValues { it.value * riposte.counter / 100 }, Action.SKILL, skill = riposte.source))
    }

    /**
     * Thorns and reflect (server 0.66.0): the struck fighter gives a flat physical blow and a share of
     * what it took back to the attacker, each part reduced by the attacker's own armour or resistance.
     */
    private fun reflect(me: Fighter, attacker: Fighter, taken: Map<DamageType, Double>, foe: Int) {
        if (attacker.invulnerable) return
        val back = mutableMapOf<DamageType, Double>()
        if (me.body.thorns > 0) back[DamageType.PHYSICAL] = me.body.thorns
        if (me.body.reflect > 0) taken.forEach { (type, amount) -> back.merge(type, amount * me.body.reflect, Double::plus) }
        val mitigated = back.mapValues { (type, raw) ->
            when (type) {
                DamageType.PHYSICAL -> raw * (1 - (attacker.body.armour / (attacker.body.armour + rules.armour.factor * raw)).coerceAtMost(rules.armour.cap / 100)) * (1 - attacker.body.physicalReduction)
                else -> raw * (1 - attacker.body.resist(type))
            }.coerceAtLeast(0.0) * attacker.body.damageTaken(type)
        }.filterValues { it > 0 }
        if (mitigated.isEmpty()) return
        val chaos = mitigated[DamageType.CHAOS] ?: 0.0
        val shielded = mitigated.values.sum() - chaos
        val absorbed = min(attacker.shield, shielded)
        attacker.shield -= absorbed
        attacker.life = max(0.0, attacker.life - (shielded - absorbed) - chaos)
        attacker.lastHit = time
        record(me.side, Action.REFLECT, HitKind.HIT, mitigated.values.sum(), mitigated.maxBy { it.value }.key, 0.0, false, emptyList(), null, foe)
        if (!attacker.alive) fell(attacker)
    }

    /**
     * A blow that got through: a barrier soaks it first (2.78.0), the shield takes what it can, chaos goes
     * around it; leech, stun and ailments follow, and the passives hear of it.
     */
    private fun land(me: Fighter, target: Fighter, kind: HitKind, taken: Map<DamageType, Double>, foe: Int, blow: Blow, body: Combatant) {
        val dealt = taken.values.sum()
        var rest = dealt
        if (target.barrier > 0) { val soaked = min(target.barrier, rest); target.barrier -= soaked; rest -= soaked }
        val chaos = if (dealt > 0) (taken[DamageType.CHAOS] ?: 0.0) * rest / dealt else 0.0
        val shielded = rest - chaos
        val absorbed = min(target.shield, shielded)
        target.shield -= absorbed
        target.life = max(0.0, target.life - (shielded - absorbed) - chaos)
        target.lastHit = time
        val physical = taken[DamageType.PHYSICAL] ?: 0.0
        val healed = (physical * body.leechPhysical + dealt * body.leechAll +
            (if (kind == HitKind.CRIT) dealt * body.critLeech else 0.0) + (if (blow.weapon) body.lifeOnHit else 0.0)) * body.recoveryRate
        me.life = min(me.body.maxLife, me.life + healed)
        // Mana (server 0.69.0): leeched and gained on hit; a burning blow takes the struck one's.
        if (me.body.maxMana > 0) me.mana = min(manaCap(me), me.mana + dealt * body.leechMana + if (blow.weapon) body.manaOnHit else 0.0)
        if (body.manaBurn > 0) target.mana = max(0.0, target.mana - target.body.maxMana * body.manaBurn)

        var stunned = false
        // Only a fighter with a chance to avoid draws for it, so a sheet without one plays the same seed as before.
        if (target.alive && !target.body.immuneStun && (dealt >= target.body.maxLife * rules.stun.share / 100 + target.body.stunThreshold &&
                !(target.body.avoidStun > 0 && random.nextDouble() < target.body.avoidStun) || blow.stun > 0 && random.nextDouble() * 100 < blow.stun)) {
            stunned = true
            target.heldUntil = max(target.heldUntil, time + rules.stun.duration)
        }
        val inflicted = if (target.alive) inflict(me, target, taken, blow.ailments) else emptyList()
        record(me.side, blow.action, kind, dealt, taken.maxByOrNull { it.value }?.key, healed, stunned, inflicted, null, foe, blow.skill)
        if (me === heroFighter) {
            if (target.alive && hexing()) hex(target)
            if (kind == HitKind.CRIT) {
                flaskCharge("FLASK_CHARGE_ON_CRIT")
                trigger(SkillEvent.CRIT, target, taken = taken)
                if (blow.spell) trigger(SkillEvent.SPELL_CRIT, target, taken = taken)
            }
            powers.dealt(PowerMoment(target, taken, blow.spell), kind == HitKind.CRIT, stunned, inflicted)
        }
        if (target === heroFighter) {
            flaskCharge("FLASK_CHARGE_WHEN_HIT")
            trigger(SkillEvent.HIT_TAKEN, me)
            powers.taken(PowerMoment(me, taken, blow.spell), kind == HitKind.CRIT)
            // «Horror» (an essence): struck, the hero may lay their own curse on the one who struck.
            val chance = target.body["STOCK_CURSE_ON_HIT"]
            if (chance > 0 && me.alive && random.nextDouble() * 100 < chance) curseOf()?.let { curse(it, listOf(me)) }
            watch()
        }
        if (!target.alive) fell(target, blow.spell)
    }

    /**
     * Which ailments this blow's damage brings, by the server's rules: a roll per rule whose type did some damage.
     * The hero starts from the rule's [AilmentRule.heroChance] where it has one, and the striker's gear adds to it;
     * the target may avoid it, be immune to it (2.78.0) and shortens it by its own gear, and the damage over time
     * runs heavier by the striker's. A skill's own chances ([extra], in percent) roll after the rules'.
     */
    private fun inflict(me: Fighter, target: Fighter, taken: Map<DamageType, Double>, extra: List<Pair<Ailment, Double>>): List<Ailment> {
        val rolled = ailmentRules.mapNotNull { (rule, what) ->
            val (ailment, type) = what
            val amount = taken[type] ?: 0.0
            val base = if (me.side == Side.HERO) rule.heroChance ?: rule.chance else rule.chance
            val chance = (base + me.body.inflictChance(ailment)).coerceAtMost(100.0) / 100
            if (amount <= 0 || chance <= 0 || amount < target.body.maxLife * rule.threshold / 100 || random.nextDouble() >= chance) return@mapNotNull null
            afflict(me, target, ailment, taken)
        }
        val forced = extra.filter { it.first !in rolled && it.second > 0 }.mapNotNull { (ailment, chance) ->
            if (random.nextDouble() * 100 < chance) afflict(me, target, ailment, taken) else null
        }
        return rolled + forced
    }

    /**
     * [ailment] on [target] by [me], from a blow that dealt [taken]: a damage over time is a share of the
     * damage of its own type — of the whole blow when it had none — the rest are the rule's magnitude.
     */
    internal fun afflict(me: Fighter, target: Fighter, ailment: Ailment, taken: Map<DamageType, Double>, spell: Boolean = false): Ailment? {
        val (rule, type) = ruleOf[ailment] ?: return null
        if (target.body.immune(ailment)) return null
        val avoid = target.body.avoid(ailment)
        if (avoid > 0 && random.nextDouble() < avoid) return null
        val amount = (taken[type] ?: 0.0).takeIf { it > 0 } ?: taken.values.sum()
        val duration = rule.duration * target.body.ailmentDuration(ailment) * me.body.ailmentDurationOnFoes(ailment)
        val magnitude = if (ailment.hurts) amount * rule.magnitude / 100 / rule.duration * me.body.ailmentDamage(ailment) else rule.magnitude
        place(target, ActiveAilment(ailment, time + duration, magnitude, duration, me.side, me.index.coerceAtLeast(0), spell), rule.stacks)
        if (target === heroFighter) powers.fire(PowerEvent.AILED, PowerMoment(me, taken, ailment = ailment))
        return ailment
    }

    /** An ailment laid on: a stacking one adds up, the others keep the strongest of their kind and refresh how long it lasts. */
    internal fun place(target: Fighter, fresh: ActiveAilment, stacks: Boolean) {
        val ailment = fresh.ailment
        if (ailment.hurts) target.tickedAt.putIfAbsent(ailment, time)
        val existing = target.ailments.filter { it.ailment == ailment }
        when {
            stacks || existing.isEmpty() -> target.ailments += fresh
            existing.maxOf { it.magnitude } <= fresh.magnitude -> { target.ailments.removeAll(existing); target.ailments += fresh }
            else -> { val kept = existing.maxBy { it.magnitude }; target.ailments.remove(kept); target.ailments += kept.copy(until = max(kept.until, fresh.until)) }
        }
        if (ailment == Ailment.FROZEN) target.heldUntil = max(target.heldUntil, fresh.until)
    }

    /** A foe down: a kill to report, life and mana on kill and the flasks' charges for the hero (2.78.0), its aura lifted, and a focus on it let go. */
    private fun fell(fighter: Fighter, spell: Boolean = false) {
        if (fighter.side != Side.MONSTER || fighter.index in fallenOrder) return
        val ailing = fighter.ailments.toList()
        fighter.ailments.clear()
        fighter.effects.clear()
        fallenOrder += fighter.index
        if (focus == fighter.index) focus = null
        if (lastStriker == fighter.index) lastStriker = null
        if (fighter.body.auras.isNotEmpty()) remake(heroFighter)
        val hero = heroFighter
        if (!hero.alive) return
        hero.life = min(hero.body.maxLife, hero.life + hero.body.lifeOnKill * hero.body.recoveryRate)
        hero.mana = min(manaCap(), hero.mana + hero.body.manaOnKill)
        val rarity = foes[fighter.index].rarity
        val base = (rules.flasks.perKill[rarity.name] ?: 1.0) + if (rarity >= MonsterRarity.RARE) hero.body["ATLAS_FLASK_RARE"] else 0.0
        kit.flasks.forEachIndexed { i, flask ->
            flask ?: return@forEachIndexed
            charges[i] = min(flask.maxCharges, charges[i] + flask.gained(base, hero.body))
            // A lingering draught runs on for every kill made while it runs.
            val longer = flask.own("FLASK_DURATION_PER_KILL")
            if (longer > 0) draughtOf(i)?.let { running -> hero.effects[hero.effects.indexOf(running)] = running.copy(until = running.until + longer) }
        }
        trigger(SkillEvent.KILL)
        if (spell) trigger(SkillEvent.SPELL_KILL)
        powers.killed(PowerMoment(fighter, spell = spell, ailments = ailing))
    }

    // ==================== Skills, flasks and answers (2.78.0) ====================

    /** Whether a slot's [condition] holds now; an opening one only until its slot has fired this fight. */
    private fun holds(condition: SlotCondition, opened: Boolean): Boolean {
        val hero = heroFighter
        return when (condition) {
            SlotCondition.READY -> true
            SlotCondition.FIGHT_START -> !opened
            SlotCondition.RARE_OR_BOSS -> foeFighters.any { it.alive && foes[it.index].rarity >= MonsterRarity.RARE }
            SlotCondition.LIFE_50 -> hero.life < hero.body.maxLife * 0.5
            SlotCondition.LIFE_35 -> hero.life < hero.body.maxLife * 0.35
            SlotCondition.LIFE_20 -> hero.life < hero.body.maxLife * 0.2
            SlotCondition.MANA_30 -> hero.mana < manaCap() * 0.3
            SlotCondition.SHIELD_BROKEN -> hero.body.maxShield > 0 && hero.shield <= 0.5
            SlotCondition.ENEMIES_3 -> foeFighters.count { it.alive } >= 3
            SlotCondition.AILING -> hero.ailments.isNotEmpty()
            SlotCondition.MANUAL -> false
        }
    }

    /** The active slots in order: a ready one whose condition holds, or that was tapped, is used; one short of mana holds the rest back. */
    private fun useSkills() {
        val hero = heroFighter
        for ((slot, kitSkill) in kit.actives.withIndex()) {
            kitSkill ?: continue
            val tapped = slot in taps
            if (time < (hero.readyAt[slotKey(slot)] ?: 0.0)) continue
            if (!tapped && !holds(kitSkill.condition, opened[slot])) continue
            if (foeFighters.none { it.alive }) return
            val level = kitSkill.level(hero.body)
            val cost = cost(kitSkill, level)
            if (hero.mana + 1e-9 < cost && !skillsFree()) { if (tapped) continue else return }
            opened[slot] = true
            castSlot(slot, kitSkill, level, cost)
            if (!hero.alive || outcome != null) return
        }
    }

    private fun castSlot(slot: Int, kitSkill: KitSkill, level: Int, cost: Double) {
        val hero = heroFighter
        val skill = kitSkill.skill
        val chance = hero.body["STOCK_FREE_SKILL_CHANCE"]
        val free = skillsFree() || chance > 0 && random.nextDouble() * 100 < chance
        if (!free) hero.mana = max(0.0, hero.mana - cost)
        hero.readyAt[slotKey(slot)] = time + skill.cooldown / hero.body.recovery(skill.spell)
        perform(kitSkill, level)
        trigger(SkillEvent.SKILL_USE, refund = if (free) 0.0 else cost)
        powers.fire(PowerEvent.SKILL_USE, PowerMoment(target(), spell = skill.spell))
    }

    /** What a class skill does at [level]: strike, poison, curse, buff, heal, shield or ward — or several. */
    private fun perform(kitSkill: KitSkill, level: Int) {
        val hero = heroFighter
        val skill = kitSkill.skill
        skill.hit?.let { heroHit(it, level, skill.code, skill.spell, skill.type == SkillType.ATTACK) }
        skill.dot?.let { heroDot(it, level, skill.code, skill.spell) }
        skill.curse?.let { curse(kitSkill, targets(it.targets, spell = true), level) }
        if (skill.hit != null || skill.dot != null || skill.curse != null) return
        val warcry = skill.type == SkillType.WARCRY
        skill.buff?.let { buff ->
            val speed = if (warcry) hero.body["STOCK_WARCRY_SPEED"] else 0.0
            buff(hero, skill.code, buff.stats.lines(level, if (warcry) 1 + hero.body["STOCK_WARCRY_EFFECT"] / 100 else 1.0) +
                listOfNotNull(StatLine("STOCK_ATTACK_SPEED", ModifierOperation.INCREASED, speed).takeIf { speed > 0 }), buff.duration, buff.counter?.at(level) ?: 0.0)
            if (buff.nextCrit) nextCrit = true
        }
        var healed = skill.heal?.let { heal(it, level, hero.body.skillHealing) } ?: 0.0
        if (warcry && hero.body["STOCK_WARCRY_HEAL"] > 0) healed += restore(hero.body.maxLife * hero.body["STOCK_WARCRY_HEAL"] / 100)
        skill.shield?.let { hero.shield = min(hero.body.maxShield, hero.shield + hero.body.maxShield * it.at(level) / 100) }
        skill.barrier?.let { ward(it, level) }
        self(skill.code, healed)
    }

    /** A class skill's blow at its targets — a passive's answer at [only] — each struck [SkillHit.hits] times. */
    private fun heroHit(hit: SkillHit, level: Int, code: String, spell: Boolean, attack: Boolean, only: Fighter? = null) {
        val hero = heroFighter
        val own = hit.stats.lines(level)
        val body = if (own.isEmpty()) hero.body else heroBody(own)
        val more = if (attack && hit.targets > 0) body["STOCK_SKILL_TARGETS"].toInt().coerceAtLeast(0) else 0
        val struck = only?.let { listOf(it) } ?: targets(if (hit.targets <= 0) 0 else hit.targets + more, spell)
        val element = hit.element?.let { if (it == RANDOM) DamageType.ELEMENTS.random(random) else DamageType.element(it) }
        val lines = hero.effects.flatMap { it.lines } + own
        struck.forEach { target ->
            repeat(hit.hits.coerceAtLeast(1)) {
                if (!target.alive || !hero.alive || outcome != null) return@repeat
                val damage = heroDamage(hit, level, body, target, element, lines)
                val leading = damage.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
                strike(hero, target, Blow(damage, Action.SKILL, spell, code, body, spread = hit.spell == null, stun = hit.stun?.at(level) ?: 0.0,
                    ailments = hit.ailments.mapNotNull { resolve(it, element ?: leading, level) }))
            }
        }
    }

    /**
     * A class skill's damage before defences: a share of the weapon — a finisher's larger one on a target
     * ailing or nearly dead — more by the skill damage; or a spell's own, grown by the increases of its
     * element, of spells and of skills; and a share of all of it turned to the skill's element.
     */
    private fun heroDamage(hit: SkillHit, level: Int, body: Combatant, target: Fighter, element: DamageType?, lines: List<StatLine>): Map<DamageType, Double> {
        val damage = mutableMapOf<DamageType, Double>()
        hit.weapon?.let { weapon ->
            val finisher = hit.finisher?.takeIf { target.ailments.isNotEmpty() || target.life < target.body.maxLife * 0.3 }
            val share = (finisher ?: weapon).at(level) / 100 * max(0.0, 1 + body["STOCK_SKILL_DAMAGE"] / 100)
            body.damage.forEach { (type, value) -> damage.merge(type, value * share, Double::plus) }
        }
        hit.spell?.let { spell ->
            val type = DamageType.element(spell.element) ?: DamageType.FIRE
            val low = spell.min.at(level)
            val base = low + random.nextDouble() * (spell.max.at(level) - low).coerceAtLeast(0.0)
            val increase = model.increased(type.attack, lines) + body["STOCK_SPELL_DAMAGE"] + body["STOCK_SKILL_DAMAGE"]
            damage.merge(type, base * max(0.0, 1 + increase / 100), Double::plus)
        }
        val convert = (hit.convert?.at(level) ?: 0.0).coerceIn(0.0, 100.0) / 100
        if (element != null && convert > 0) {
            val total = damage.values.sum()
            damage.replaceAll { _, value -> value * (1 - convert) }
            damage.merge(element, total * convert, Double::plus)
        }
        return damage
    }

    /** A skill's chance of an ailment: `ELEMENT` is the one [element] brings. */
    private fun resolve(ailment: SkillAilment, element: DamageType, level: Int): Pair<Ailment, Double>? =
        (if (ailment.ailment == ELEMENT) Ailment.of(element) else Ailment.byWord(ailment.ailment))?.let { it to ailment.chance.at(level) }

    /**
     * A spell of damage over time: its roll grown like a spell's, taken by the target's resistance at once
     * and laid on as the ailment of its element — a poison stacks — for its duration.
     */
    private fun heroDot(dot: SkillDot, level: Int, code: String, spell: Boolean) {
        val hero = heroFighter
        val type = DamageType.element(dot.element) ?: DamageType.CHAOS
        val ailment = Ailment.of(type).takeIf { it.hurts } ?: Ailment.POISONED
        val increase = model.increased(type.attack, hero.effects.flatMap { it.lines }) + hero.body["STOCK_SPELL_DAMAGE"] + hero.body["STOCK_SKILL_DAMAGE"]
        val lone = if (loneWolf) 1 + rules.loneWolf.dealt / 100 else 1.0
        targets(dot.targets, spell).forEach { target ->
            val low = dot.min.at(level)
            val total = (low + random.nextDouble() * (dot.max.at(level) - low).coerceAtLeast(0.0)) * max(0.0, 1 + increase / 100) * hero.body.damageMore * lone
            val mitigated = total * (1 - target.body.resist(type, hero.body.penetration(type))) * target.body.damageTaken(type)
            val inflicted = mutableListOf<Ailment>()
            if (mitigated > 0 && !target.body.immune(ailment)) {
                val duration = dot.duration * hero.body.ailmentDurationOnFoes(ailment)
                place(target, ActiveAilment(ailment, time + duration, mitigated / duration * hero.body.ailmentDamage(ailment), duration, Side.HERO, 0, spell), stacks = true)
                inflicted += ailment
            }
            dot.ailments.mapNotNull { resolve(it, type, level) }.filter { it.first != ailment }.forEach { (other, chance) ->
                if (random.nextDouble() * 100 < chance) afflict(hero, target, other, mapOf(type to mitigated), spell)?.let(inflicted::add)
            }
            record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, type, 0.0, false, inflicted, null, target.index, code)
        }
    }

    /** A curse of the hero's on [targets]: its lines stronger by the curse effect, for its duration. */
    private fun curse(kitSkill: KitSkill, targets: List<Fighter>, level: Int = kitSkill.level(heroFighter.body)) {
        val curse = kitSkill.skill.curse ?: return
        val scale = 1 + heroFighter.body["STOCK_CURSE_EFFECT"] / 100
        targets.filter { it.alive }.forEach { target ->
            lay(target, TimedEffect(EffectKind.CURSE, kitSkill.skill.code, curse.stats.lines(level, scale), time + curse.duration, curse.duration))
            record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, target.index, kitSkill.skill.code)
        }
    }

    /** The curse the hero answers a blow with: the first in their slots, else the class's first. */
    private fun curseOf(): KitSkill? = kit.actives.firstOrNull { it?.skill?.curse != null } ?: kit.curses.firstOrNull()

    /** A hexing draught runs (a unique flask): every blow that lands lays a random curse of the class. */
    private fun hexing(): Boolean = kit.flasks.indices.any { i -> kit.flasks[i]?.hexes == true && draughtOf(i) != null }
    internal fun hex(target: Fighter) {
        if (target.cursed || kit.curses.isEmpty()) return
        curse(kit.curses[random.nextInt(kit.curses.size)], listOf(target))
    }

    private fun buff(fighter: Fighter, source: String, lines: List<StatLine>, duration: Double, counter: Double = 0.0) =
        lay(fighter, TimedEffect(EffectKind.BUFF, source, lines, time + duration, duration, counter))

    /** Lays [effect] on [fighter], in place of the same one from the same source, and makes the body again. */
    internal fun lay(fighter: Fighter, effect: TimedEffect) {
        fighter.effects.removeAll { it.kind == effect.kind && it.source == effect.source && it.slot == effect.slot }
        fighter.effects += effect
        remake(fighter)
    }

    /** Healing in shares of the maximums, [scale] stronger; a cleansing one lifts the ailment that would last longest. */
    private fun heal(heal: SkillHeal, level: Int, scale: Double = 1.0): Double {
        val hero = heroFighter
        val life = heal.life?.let { restore(hero.body.maxLife * it.at(level) / 100 * scale) } ?: 0.0
        heal.mana?.let { hero.mana = min(manaCap(), hero.mana + manaCap() * it.at(level) / 100 * scale) }
        if (heal.cleanse) hero.ailments.maxByOrNull { it.until }?.let { worst -> hero.ailments.removeAll { it.ailment == worst.ailment } }
        return life
    }

    /** Life given back to the hero; the passives waiting for a heal hear of it. */
    internal fun restore(amount: Double): Double {
        val hero = heroFighter
        val before = hero.life
        hero.life = min(hero.body.maxLife, hero.life + amount)
        val healed = hero.life - before
        if (healed > 0) trigger(SkillEvent.HEALED)
        return healed
    }

    private fun ward(barrier: SkillBarrier, level: Int) {
        val hero = heroFighter
        hero.barrier = hero.body.maxLife * barrier.life.at(level) / 100
        hero.barrierUntil = time + barrier.duration
    }

    /** A skill the hero used on themselves, for the log and the number over their card. */
    private fun self(code: String, healed: Double = 0.0) =
        record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, healed, false, emptyList(), null, target()?.index ?: 0, code, onSelf = true)

    /** A chance per flask of the belt to gain a charge, by a line of its own ([stat], in percent). */
    private fun flaskCharge(stat: String) = kit.flasks.forEachIndexed { i, flask ->
        val chance = flask?.own(stat) ?: return@forEachIndexed
        if (chance > 0 && random.nextDouble() * 100 < chance) charges[i] = min(flask.maxCharges, charges[i] + 1)
    }

    /**
     * The passives answering [event]: each by its chance at its level, at most once per its cooldown.
     * [target] is the foe the event was about — the one struck, the one that struck — and [refund] the
     * mana a skill just cost.
     */
    private fun trigger(event: SkillEvent, target: Fighter? = null, refund: Double = 0.0, taken: Map<DamageType, Double> = emptyMap()) {
        if (depth >= MAX_DEPTH || !heroFighter.alive || outcome != null) return
        kit.passives.forEach { passive ->
            val answer = passive.skill.trigger?.takeIf { it.on == event } ?: return@forEach
            val code = passive.skill.code
            if (time < (triggerReady[code] ?: -1.0)) return@forEach
            val level = passive.level(heroFighter.body)
            if (answer.chance != null && random.nextDouble() * 100 >= answer.chance.at(level)) return@forEach
            if (answer.cooldown > 0) triggerReady[code] = time + answer.cooldown
            depth++
            try { answer(code, answer, level, target, refund, taken) } finally { depth-- }
        }
    }

    private fun answer(code: String, answer: SkillTrigger, level: Int, target: Fighter?, refund: Double, taken: Map<DamageType, Double>) {
        val hero = heroFighter
        val healed = answer.heal?.let { heal(it, level) } ?: 0.0
        answer.shield?.let { hero.shield = min(hero.body.maxShield, hero.shield + hero.body.maxShield * it.at(level) / 100) }
        answer.barrier?.let { ward(it, level) }
        answer.buff?.let { buff(hero, code, it.stats.lines(level), it.duration, it.counter?.at(level) ?: 0.0) }
        if (answer.flaskCharges > 0) kit.flasks.forEachIndexed { i, flask -> flask?.let { charges[i] = min(it.maxCharges, charges[i] + answer.flaskCharges) } }
        if (answer.refund && refund > 0) hero.mana = min(manaCap(), hero.mana + refund)
        val hit = answer.hit
        if (hit != null) { heroHit(hit, level, code, spell = false, attack = true, only = target?.takeIf { hit.targets == 1 && it.alive && it.side == Side.MONSTER }); return }
        val struck = target?.takeIf { it.alive && it.side == Side.MONSTER }
        val word = answer.ailment
        if (word != null && struck != null) {
            val element = taken.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
            val ailment = (if (word == ELEMENT) Ailment.of(element) else Ailment.byWord(word)) ?: return
            val inflicted = List(if (answer.twice) 2 else 1) { afflict(hero, struck, ailment, taken.ifEmpty { hero.body.damage }) }.filterNotNull().distinct()
            record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, inflicted, null, struck.index, code)
            return
        }
        self(code, healed)
    }

    /** The belt in order: a flask whose condition holds, that was tapped, or that drinks itself at low life, is drunk if it has the charges and is not running. */
    private fun useFlasks() {
        val hero = heroFighter
        kit.flasks.forEachIndexed { i, flask ->
            flask ?: return@forEachIndexed
            if (draughtOf(i) != null) return@forEachIndexed
            val auto = flask.own("FLASK_AUTO_LOW_LIFE").let { it > 0 && hero.life < hero.body.maxLife * it / 100 }
            if (i !in drinks && !auto && !holds(flask.condition, flaskOpened[i])) return@forEachIndexed
            if (charges[i] + 1e-9 < flask.perUse(hero.body)) return@forEachIndexed
            flaskOpened[i] = true
            drink(i, flask)
        }
    }

    private fun drink(slot: Int, flask: Flask) {
        val hero = heroFighter
        val keep = flask.own("FLASK_NO_CHARGE_CHANCE").let { it > 0 && random.nextDouble() * 100 < it }
        val draught = flask.draught(hero.body, hero.life, manaCap())
        charges[slot] = when {
            flask.usesAll -> 0.0
            keep -> charges[slot]
            else -> (charges[slot] - flask.perUse(hero.body)).coerceAtLeast(0.0)
        }
        lay(hero, TimedEffect(EffectKind.FLASK, flask.code, draught.lines, time + draught.duration, draught.duration, slot = slot))
        // An immunity drunk lifts what it guards against at once.
        hero.ailments.removeAll { hero.body.immune(it.ailment) }
        hero.mana = min(manaCap(), hero.mana + draught.mana)
        hero.shield = min(hero.body.maxShield, hero.shield + draught.shield)
        if (draught.lifeRate > 0 || draught.manaRate > 0) recoveries += Recovery(draught.lifeRate, draught.manaRate, time + draught.duration, slot)
        if (draught.invulnerable > 0) hero.invulnerableUntil = time + draught.invulnerable
        val before = hero.life
        hero.life = min(hero.body.maxLife, hero.life + draught.life)
        record(Side.HERO, Action.FLASK, HitKind.HIT, 0.0, null, hero.life - before, false, emptyList(), null, target()?.index ?: 0, flask.code, onSelf = true)
        if (hero.life > before || draught.lifeRate > 0) trigger(SkillEvent.HEALED)
        powers.fire(PowerEvent.FLASK)
    }

    /** A monster's skills, the first ready one it has the mana for and a reason to use: a heal when hurt, a buff or a curse not already on. */
    private fun monsterCast(me: Fighter) {
        val skills = foes[me.index].skills
        if (skills.isEmpty() || !heroFighter.alive) return
        for (skill in skills) {
            val ready = me.readyAt.getOrPut(skill.code) { time + skill.cooldown / 2 }
            if (time < ready || me.mana + 1e-9 < skill.mana || !wanted(me, skill)) continue
            me.mana -= skill.mana
            me.readyAt[skill.code] = time + skill.cooldown / me.body.recovery(skill.spell)
            monsterSkill(me, skill)
            return
        }
    }

    private fun wanted(me: Fighter, skill: MonsterSkill): Boolean = when {
        skill.heal != null -> me.life < me.body.maxLife * 0.6
        skill.buff != null -> me.effects.none { it.source == skill.code }
        skill.curse != null -> !heroFighter.body.immuneCurse && heroFighter.effects.none { it.source == skill.code }
        skill.manaBurn > 0 && skill.hit == null -> heroFighter.mana > 0
        else -> true
    }

    /** What a monster's skill does: a share of its own swing in the skill's element, a buff, a curse, a heal, a burn of the hero's mana. */
    private fun monsterSkill(me: Fighter, skill: MonsterSkill) {
        val hero = heroFighter
        skill.hit?.let { hit ->
            val element = DamageType.element(hit.element)
            val share = (hit.weapon?.at(1) ?: 100.0) / 100
            val damage = me.body.damage.mapValues { it.value * share }.toMutableMap()
            // A caster's spell carries its spell damage in the skill's element.
            val magical = me.body["STOCK_ATTACK_MAGICAL"]
            if (skill.spell && magical > 0) damage.merge(element ?: me.body.leading, magical * share, Double::plus)
            val convert = (hit.convert?.at(1) ?: 0.0).coerceIn(0.0, 100.0) / 100
            if (element != null && convert > 0) {
                val total = damage.values.sum()
                damage.replaceAll { _, value -> value * (1 - convert) }
                damage.merge(element, total * convert, Double::plus)
            }
            val leading = damage.maxByOrNull { it.value }?.key ?: DamageType.PHYSICAL
            strike(me, hero, Blow(damage, Action.SKILL, skill.spell, skill.code, stun = hit.stun?.at(1) ?: 0.0,
                ailments = hit.ailments.mapNotNull { resolve(it, element ?: leading, 1) }))
        }
        skill.buff?.let { buff ->
            buff(me, skill.code, buff.stats.lines(1, 1 + me.body["STOCK_WARCRY_EFFECT"] / 100), buff.duration)
            record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, me.index, skill.code, onSelf = true)
        }
        skill.curse?.let { curse ->
            lay(hero, TimedEffect(EffectKind.CURSE, skill.code, curse.stats.lines(1), time + curse.duration, curse.duration))
            record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, me.index, skill.code)
        }
        skill.heal?.let { heal ->
            val before = me.life
            me.life = min(me.body.maxLife, me.life + me.body.maxLife * (heal.life?.at(1) ?: 0.0) / 100)
            record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, me.life - before, false, emptyList(), null, me.index, skill.code, onSelf = true)
        }
        if (skill.manaBurn > 0 && skill.hit == null) {
            hero.mana = max(0.0, hero.mana - manaCap() * skill.manaBurn / 100)
            record(Side.MONSTER, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, me.index, skill.code)
        }
    }

    // ==================== What the powers reach (2.79.0) ====================

    /** A power that did something of its own, for the log and the number over the hero's card. */
    internal fun powerShown(code: String, healed: Double) = self(code, healed)

    /** Every charge on the belt, summed. */
    internal fun flaskCharges(): Double = charges.sum()

    /** [amount] charges to every flask of the belt, up to each one's maximum. */
    internal fun chargeFlasks(amount: Double) = kit.flasks.forEachIndexed { i, flask -> flask?.let { charges[i] = min(it.maxCharges, charges[i] + amount) } }

    /** A foe finished off by a power: down at once, a kill as any other. */
    internal fun slay(foe: Fighter) {
        if (!foe.alive) return
        foe.life = 0.0
        record(Side.HERO, Action.SKILL, HitKind.HIT, 0.0, null, 0.0, false, emptyList(), null, foe.index)
        fell(foe)
    }

    private fun finished(): Boolean {
        if (outcome != null) return true
        // A power may answer the hero's fall (2.79.0) and stand them back up.
        if (!heroFighter.alive) powers.fire(PowerEvent.DEATH)
        when {
            !heroFighter.alive -> end(Outcome.LOSS)
            foeFighters.none { it.alive } -> end(Outcome.WIN)
            else -> return false
        }
        return true
    }

    private fun end(how: Outcome) { outcome = how; duration = time }

    private fun record(actor: Side, action: Action, kind: HitKind, damage: Double, type: DamageType?, healed: Double, stunned: Boolean,
                       inflicted: List<Ailment>, ailment: Ailment?, foe: Int, skill: String? = null, onSelf: Boolean = false) {
        val m = foeFighters.getOrNull(foe)
        log += CombatEvent(time, actor, action, kind, damage, type, healed, stunned, inflicted, ailment, heroFighter.life, heroFighter.shield,
            m?.life ?: 0.0, m?.shield ?: 0.0, foe, skill, onSelf, heroFighter.mana)
    }

    companion object {
        /** The slice the fight is stepped in: fine enough for a 5-per-second swing, fixed so a fight is a function of its seed. */
        const val STEP = 1.0 / 60
        /** How often an ailment's damage is written into the log. */
        const val TICK = 1.0
        const val LUNGE = 0.16
        /** How deep a passive's answer may set off another's. */
        private const val MAX_DEPTH = 2
        /** A skill's element picked at random, and its ailment named by the element that struck (server 0.69.0). */
        private const val RANDOM = "RANDOM"
        private const val ELEMENT = "ELEMENT"
    }
}
