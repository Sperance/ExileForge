package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.rules.content.AccuracyRule
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.Ceiling
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.ManaRule
import kotlin.math.max

/**
 * One side of a fight, read off a stat table — the hero's sheet or a rolled monster — under the
 * server's [rules].
 *
 * Every number here is one the server sent; the fight only decides what they do to each other. A
 * hero with no weapon still swings — unarmed, as in PoE — and a missing critical chance is the one
 * every attack has. Mana came back in 2.78.0 (server 0.69.0) with the class skills and the monsters'
 * spells: it is what a skill is paid with, and it comes back by the rule's share a second.
 */
data class Combatant(val stats: Map<String, Double>, val level: Int, val rules: CombatRules) {
    private fun stat(name: String) = stats[name] ?: 0.0
    private fun percent(name: String, cap: Double = 100.0) = stat(name).coerceIn(0.0, cap) / 100

    /** A stat of the sheet as it stands, zero when it has none. */
    operator fun get(name: String): Double = stat(name)

    val maxLife = max(1.0, stat(CoreStat.HEALTH.code))

    /** A monster's «shield of life» (an essence, server 0.69.0) adds a share of its life to its shield. */
    val maxShield = max(0.0, stat(CoreStat.ENERGY_SHIELD.code)) + maxLife * max(0.0, stat(CoreStat.SHIELD_OF_LIFE.code)) / 100
    val maxMana = max(0.0, stat(CoreStat.MANA.code))

    /** Immune to chaos (3.17.0, server 1.15.0): its hits and poison do nothing. */
    val chaosImmune = stat(CoreStat.CHAOS_IMMUNE.code) > 0

    /** Mana back a second (server 0.69.0): the rule's share of the maximum, faster by the sheet's regeneration. */
    fun manaRegen(rule: ManaRule): Double = maxMana * rule.regen / 100 * max(0.0, 1 + stat(CoreStat.MANA_REGEN.code) / 100)

    /** Every damage it deals, as a multiplier: the sheet's "damage" (server 0.69.0), a monster's "deals more damage". */
    val damageMore = max(0.0, 1 + stat(CoreStat.DAMAGE.code) / 100)

    /**
     * How fast its skills recover: the cooldown recovery, and a spell's cast speed on top - the cast speed no higher than its cap,
     * the whole no faster than the rules' shortest cooldown (server 1.76.0).
     */
    fun recovery(spell: Boolean): Double {
        val cast = if (spell) stat(CoreStat.CAST_SPEED.code).coerceAtMost(rules.caps.castSpeed) else 0.0
        return (1 + (stat(CoreStat.COOLDOWN_RECOVERY.code) + cast) / 100).coerceIn(0.1, rules.caps.recoveryMax)
    }

    /** What is left of a skill's price in mana. */
    val skillCost = max(0.0, 1 - stat(CoreStat.SKILL_COST.code) / 100)
    val skillHealing = max(0.0, 1 + stat(CoreStat.SKILL_HEALING.code) / 100)

    /** Immune to [ailment] (a flask's suffix, server 0.69.0); an immunity to freezing keeps the cold's chill off too. */
    fun immune(ailment: Ailment): Boolean = stat("STOCK_IMMUNE_${ailment.word}") > 0 || (ailment == Ailment.CHILLED && stat(CoreStat.IMMUNE_FREEZE.code) > 0)
    val immuneCurse: Boolean get() = stat(CoreStat.IMMUNE_CURSE.code) > 0
    val immuneStun: Boolean get() = stat(CoreStat.IMMUNE_STUN.code) > 0

    /** How much faster this striker fills [kind] on others (3.78.0). */
    fun buildupGain(kind: Buildup) = max(0.0, 1 + stat(kind.gain) / 100)

    /** How much of [kind] reaches this fighter's bar: its avoidance takes a share off. */
    fun buildupTaken(kind: Buildup) = 1 - (kind.avoid?.let { percent(it) } ?: 0.0)

    /** The stun bar's own size against the rule's: «Непоколебимый» doubles it. */
    val stunPool = max(.1, 1 + stat(CoreStat.STUN_POOL.code) / 100)

    /** Never fills [kind] at all. */
    fun immuneTo(kind: Buildup) = when (kind) {
        Buildup.STUN -> immuneStun
        Buildup.FREEZE -> immune(Ailment.FROZEN)
        Buildup.ELECTROCUTE -> stat(CoreStat.IMMUNE_ELECTROCUTE.code) > 0
    }

    /** Life back a second as a share of the maximum (server 0.69.0), beside the flat regeneration. */
    val lifeRegenShare = max(0.0, stat(CoreStat.LIFE_REGEN_PERCENT.code)) / 100

    /** Energy shield back a second as a share of the maximum (server 1.58.0), beside the flat regeneration. */
    val shieldRegenShare = max(0.0, stat(CoreStat.ENERGY_REGEN_PERCENT.code)) / 100

    /** Life lost a second as a share of the maximum (3.4.0). */
    val lifeDegenShare = max(0.0, stat(CoreStat.LIFE_DEGEN_PERCENT.code)) / 100
    val leechMana = max(0.0, stat(CoreStat.LEECH_MANA.code)) / 100
    val manaOnHit = max(0.0, stat(CoreStat.MANA_ON_HIT.code))
    val manaOnKill = max(0.0, stat(CoreStat.MANA_ON_KILL.code))

    /** The share of the struck one's maximum mana its blows burn (a monster's essence). */
    val manaBurn = max(0.0, stat(CoreStat.MANA_BURN.code)) / 100

    /** How much more damage over time, a critical strike, a bleeding and a shock do to this fighter — a curse's work. */
    val dotTaken = max(0.0, 1 + stat(CoreStat.DOT_TAKEN.code) / 100)

    /** Extra critical multiplier this fighter takes; below zero (server 1.32.0) a critical strike hurts it no more than a hit. */
    val critTaken = stat(CoreStat.CRITICAL_TAKEN.code) / 100
    fun ailmentTaken(ailment: Ailment): Double = when (ailment) {
        Ailment.BLEEDING -> max(0.0, 1 + stat(CoreStat.BLEED_TAKEN.code) / 100)
        Ailment.SHOCKED -> max(0.0, 1 + stat(CoreStat.SHOCK_TAKEN.code) / 100)
        else -> 1.0
    }
    val damage: Map<DamageType, Double> = DamageType.entries.associateWith { max(0.0, stat(it.attack)) }
        .let { rolled -> if (rolled.values.sum() > 0) rolled else rolled + (DamageType.PHYSICAL to rules.unarmed.damage) }
    val attackSpeed = stat(CoreStat.ATTACK_SPEED.code).takeIf { it > 0 }?.coerceIn(rules.attackSpeedMin, rules.attackSpeedMax) ?: rules.unarmed.speed

    /** A limit raised by the sheet's own lines (3.13.0): block, evasion, physical reduction and critical chance, each as the resistances are. */
    fun ceiling(limit: Ceiling): Double = limit.at(stat(limit.raise))
    val critChance = (stats[CRIT_CHANCE] ?: rules.critical.chance).coerceIn(0.0, ceiling(rules.ceilings.critical)) / 100

    /**
     * The critical multiplier with the critical strike damage (server 1.57.0): that damage — 100 at base, on the hero's sheet
     * and on a monster's — grows the crit's bonus over a hit, 150% at 140 striking for 170%; at 0 a crit hits as a hit.
     */
    val critMultiplier = rules.critical.effective(stats[CRIT_MULTIPLIER] ?: rules.critical.multiplier, stats[CRIT_DAMAGE]) / 100

    /**
     * A spell's own critical chance and multiplier (server 1.56.0): the attacks' lines do nothing to it. The hero's sheet
     * always holds both from the rule's base; a fighter without them — a monster — casts with its attacks' figures.
     */
    val spellCritChance = stats[SPELL_CRIT_CHANCE]?.let { it.coerceIn(0.0, ceiling(rules.ceilings.critical)) / 100 } ?: critChance
    val spellCritMultiplier = stats[SPELL_CRIT_MULTIPLIER]?.let { rules.critical.effective(it, stats[CRIT_DAMAGE]) / 100 } ?: critMultiplier
    val armour = max(0.0, stat(CoreStat.ARMOR.code))
    val evasion = max(0.0, stat(CoreStat.EVASION.code))
    val block = stat(CoreStat.BLOCK_CHANCE.code).coerceIn(0.0, ceiling(rules.ceilings.block)) / 100

    /** The most of a blow evasion and armour can each turn aside, in shares. */
    val evasionCap = ceiling(rules.ceilings.evasion) / 100
    val armourCap = ceiling(rules.ceilings.physical) / 100

    /** Taken off physical damage after armour, under the same physical ceiling. */
    val physicalReduction = percent(CoreStat.PHYSICAL_REDUCTION.code, ceiling(rules.ceilings.physical))

    /**
     * The share of a physical blow of [raw] that armour and then the flat reduction take, together under the physical
     * ceiling (3.71.0): the ceiling is the whole of physical mitigation, not each half's, so the two never stack past it.
     */
    fun physicalMitigation(raw: Double, factor: Double): Double = (1 - (1 - (armour / (armour + factor * raw)).coerceAtMost(armourCap)) * (1 - physicalReduction)).coerceAtMost(armourCap)

    /**
     * Chaos stands alone, as in PoE; "all resistances" and "all maximum resistances" cover the three elements.
     * [penetration] (server 0.66.0) is the striker's: it is taken off the resistance, and can push it below
     * zero down to minus the cap, so a monster without resistance is still hurt more by a penetrating blow.
     */
    fun resist(type: DamageType, penetration: Double = 0.0): Double {
        val name = type.resist ?: return 0.0
        val chaos = name == CoreStat.RESIST_CHAOS.code
        val ceiling = (rules.resistCap + stat(type.maxResist.orEmpty()) + (if (chaos) 0.0 else stat(CoreStat.RESIST_MAX_ALL.code))).coerceIn(0.0, rules.resistHardCap)
        // Below zero since 3.18.0: the act's penalty and the map's curse can leave a resistance negative, as in PoE.
        val own = (stat(name) + (if (chaos) 0.0 else stat(CoreStat.RESIST_ALL.code))).coerceIn(-rules.resistFloor, ceiling)
        return (own - penetration).coerceIn(-rules.resistFloor, ceiling) / 100
    }

    /** How much of the target's [type] resistance this fighter's blows ignore, in percent (server 0.66.0). */
    fun penetration(type: DamageType): Double = when (type) {
        DamageType.PHYSICAL -> 0.0
        DamageType.CHAOS -> max(0.0, stat(CoreStat.PENETRATE_CHAOS.code))
        else -> max(0.0, stat("STOCK_PENETRATE_${type.name}")) + max(0.0, stat(CoreStat.PENETRATE_ELEMENTAL.code))
    }

    /**
     * What a blow of [type] does to this fighter after its defences (server 0.66.0): "damage taken" of every
     * kind and of that kind together, never below a tenth so no stack of it makes a fighter untouchable.
     * Chaos is the one exception: a keystone's immunity (3.17.0, server 1.15.0) takes none of it.
     */
    fun damageTaken(type: DamageType): Double {
        if (type == DamageType.CHAOS && chaosImmune) return 0.0
        val own = when (type) {
            DamageType.PHYSICAL -> stat(CoreStat.PHYSICAL_TAKEN.code)
            DamageType.CHAOS -> stat(CoreStat.CHAOS_TAKEN.code)
            else -> stat(CoreStat.ELEMENTAL_TAKEN.code)
        }
        return ((1 + stat(CoreStat.DAMAGE_TAKEN.code) / 100) * (1 + own / 100)).coerceAtLeast(0.1)
    }

    /** How much harder this fighter hits a target under [ailments]: any ailment counts once, each named one on top. */
    fun damageAgainst(ailments: Collection<Ailment>): Double {
        if (ailments.isEmpty()) return 1.0
        val named = ailments.toSet().sumOf { max(0.0, stat(it.against)) }
        return 1 + (max(0.0, stat(CoreStat.DAMAGE_VS_AILED.code)) + named) / 100
    }

    /** How much longer the ailments this fighter inflicts last on its foes. */
    fun ailmentDurationOnFoes(ailment: Ailment): Double = 1 + (max(0.0, stat(CoreStat.AILMENT_DURATION.code)) + max(0.0, stat("STOCK_${ailment.word}_DURATION"))) / 100

    /** The pace of everything that gives life or shield back: regeneration, leech, on hit and on kill (server 0.66.0). */
    val recoveryRate = max(0.0, 1 + stat(CoreStat.RECOVERY_RATE.code) / 100)

    /** The pace of the shield's recharge after the rule's delay. */
    val shieldRecharge = max(0.0, 1 + stat(CoreStat.SHIELD_RECHARGE.code) / 100)

    /** Flat physical damage every attacker takes on hit, and the share of any damage taken given back the same way. */
    val thorns = max(0.0, stat(CoreStat.THORNS.code))
    val reflect = max(0.0, stat(CoreStat.REFLECT.code)) / 100

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
        auras["AURA_WEAKEN"]?.let { v -> sheet[CoreStat.DAMAGE.code] = (100 + (stats[CoreStat.DAMAGE.code] ?: 0.0)) * max(0.0, 1 - v / 100) - 100 }
        auras["AURA_CRIT"]?.let { v ->
            sheet[CRIT_CHANCE] = critChance * 100 * max(0.0, 1 - v / 100)
            if (SPELL_CRIT_CHANCE in stats) sheet[SPELL_CRIT_CHANCE] = spellCritChance * 100 * max(0.0, 1 - v / 100)
        }
        auras["AURA_COOLDOWN"]?.let { v -> sheet[CoreStat.COOLDOWN_RECOVERY.code] = (stats[CoreStat.COOLDOWN_RECOVERY.code] ?: 0.0) - v }
        auras["AURA_RESIST"]?.let { v ->
            sheet[CoreStat.RESIST_ALL.code] = (stats[CoreStat.RESIST_ALL.code] ?: 0.0) - v
            sheet[CoreStat.RESIST_CHAOS.code] = (stats[CoreStat.RESIST_CHAOS.code] ?: 0.0) - v
        }
        auras["AURA_DAMAGE_TAKEN"]?.let { v -> sheet[CoreStat.DAMAGE_TAKEN.code] = (stats[CoreStat.DAMAGE_TAKEN.code] ?: 0.0) + v }
        auras["AURA_SLOW"]?.let { v -> sheet[CoreStat.ATTACK_SPEED.code] = attackSpeed * max(0.1, 1 - v / 100) }
        auras["AURA_RECOVERY"]?.let { v -> sheet[CoreStat.RECOVERY_RATE.code] = (stats[CoreStat.RECOVERY_RATE.code] ?: 0.0) - v }
        return Combatant(sheet, level, rules)
    }
    val lifeRegen = max(0.0, stat(CoreStat.HEALTH_REGEN.code))
    val shieldRegen = max(0.0, stat(CoreStat.ENERGY_REGEN.code))
    val leechPhysical = max(0.0, stat(CoreStat.LEECH_PHYSICAL.code)) / 100
    val leechAll = max(0.0, stat(CoreStat.LEECH_ALL.code)) / 100
    val critLeech = max(0.0, stat(CoreStat.CRITICAL_VAMPIRE.code)) / 100
    val stunThreshold = max(0.0, stat(CoreStat.STUN_THRESHOLD.code))
    val avoidStun = percent(CoreStat.AVOID_STUN.code)

    /** What gear adds to the rule's chance to inflict [ailment], in percent; chill has no such stat. */
    fun inflictChance(ailment: Ailment) = max(0.0, stat("STOCK_${ailment.word}_CHANCE"))

    /** How much heavier [ailment]'s damage over time runs. */
    fun ailmentDamage(ailment: Ailment) = 1 + max(0.0, ailment.damage?.let(::stat) ?: 0.0) / 100
    fun avoid(ailment: Ailment) = percent("STOCK_AVOID_${ailment.word}")

    /** What is left of [ailment]'s duration on this fighter, never less than the rule's cap allows. */
    fun ailmentDuration(ailment: Ailment) = 1 - percent("STOCK_${ailment.word}_DURATION_ON_SELF", rules.ailmentDurationCap)
    val lifeOnHit = max(0.0, stat(CoreStat.HEALTH_ON_HIT.code))

    // ==================== Keystones and uniques of server 1.32.0 (3.33.0) ====================

    /** The share of the damage bound for life that mana takes first. */
    val manaBeforeLife = percent(CoreStat.MANA_BEFORE_LIFE.code)

    /** Life leech restores the shield instead. */
    val leechToShield: Boolean get() = stat(CoreStat.LEECH_TO_SHIELD.code) > 0

    /** The shield lost a second as a share of its maximum. */
    val shieldDegenShare = max(0.0, stat(CoreStat.SHIELD_DEGEN_PERCENT.code)) / 100

    /** The share of damage to life dealt over [lifeDelay] seconds instead of at once. */
    val lifeDelayed = percent(CoreStat.LIFE_DAMAGE_DELAYED.code)
    val lifeDelay: Double get() = stat(CoreStat.LIFE_DAMAGE_DELAY.code).takeIf { it > 0 } ?: rules.lifeDelay

    /** Physical damage of hits taken as an element instead, share by element; the shares together never pass the whole. */
    val physicalTakenAs: Map<DamageType, Double> = DamageType.ELEMENTS.mapNotNull { type ->
        stat("STOCK_PHYSICAL_TAKEN_AS_${type.name}").takeIf { it > 0 }?.let { type to it / 100 }
    }.toMap().let { shares ->
        val sum = shares.values.sum()
        if (sum > 1) shares.mapValues { it.value / sum } else shares
    }

    /** Each of its hits deals all its damage as one random element. */
    val randomElementHits: Boolean get() = stat(CoreStat.RANDOM_ELEMENT_HITS.code) > 0

    /** Elemental damage taken meets its highest elemental resistance. */
    val highestResistTaken: Boolean get() = stat(CoreStat.HIGHEST_RESIST_TAKEN.code) > 0

    /** More or less damage taken of the element it resists most, in percent. */
    val highestResistElementTaken: Double get() = stat(CoreStat.HIGHEST_RESIST_ELEMENT_TAKEN.code)

    /** Its damage of the element its target resists least penetrates this much. */
    val lowestResistPenetrate: Double get() = max(0.0, stat(CoreStat.LOWEST_RESIST_PENETRATE.code))

    /** Its ignites burn as chaos. */
    val igniteAsChaos: Boolean get() = stat(CoreStat.IGNITE_AS_CHAOS.code) > 0

    /** Its critical chance is rolled twice. */
    val luckyCrit: Boolean get() = stat(CoreStat.LUCKY_CRIT.code) > 0

    /** Its hits that are not critical strikes, as a multiplier (−40 — 40% less). */
    val nonCritMore: Double get() = max(0.0, 1 + stat(CoreStat.NON_CRIT_DAMAGE.code) / 100)

    /** Every flask of the belt is drunk as a fight opens, for no charges. */
    val flasksAuto: Boolean get() = stat(CoreStat.FLASKS_AUTO.code) > 0

    /** Its resistance to a blow of [type]: the highest elemental one for an element when it takes elements so. */
    fun resistTo(type: DamageType, penetration: Double = 0.0): Double = if (highestResistTaken && type in DamageType.ELEMENTS) DamageType.ELEMENTS.maxOf { resist(it, penetration) } else resist(type, penetration)

    private companion object {
        val CRIT_CHANCE: String = CoreStat.CRITICAL_CHANCE.code
        val CRIT_MULTIPLIER: String = CoreStat.CRITICAL_MULTIPLIER.code
        val CRIT_DAMAGE: String = CoreStat.CRITICAL_DAMAGE.code
        val SPELL_CRIT_CHANCE: String = CoreStat.SPELL_CRITICAL_CHANCE.code
        val SPELL_CRIT_MULTIPLIER: String = CoreStat.SPELL_CRITICAL_MULTIPLIER.code
    }
    val lifeOnKill = max(0.0, stat(CoreStat.HEALTH_ON_KILL.code))

    // ==================== Server 1.34.0 (3.35.0): accuracy, buffs, defences of PoE ====================

    /** Its accuracy against an evasive target: the rule's base by level and dexterity, and the sheet's own. */
    fun accuracy(rule: AccuracyRule): Double = max(0.0, rule.base + rule.perLevel * level + rule.perDexterity * stat(CoreStat.AGILITY.code) + stat(CoreStat.ACCURACY.code))

    /** The shares of its physical damage it deals again as other types, by type. */
    val extraAs: Map<DamageType, Double> = DamageType.entries.filter { it != DamageType.PHYSICAL }
        .mapNotNull { type -> stat("STOCK_PHYSICAL_AS_EXTRA_${type.name}").takeIf { it > 0 }?.let { type to it / 100 } }.toMap()

    /** Flat damage of [type] its spells add. */
    fun spellAdded(type: DamageType): Double = max(0.0, stat("STOCK_SPELL_ADD_${type.name}"))

    /** Its critical chance and multiplier for a blow: a spell's are its own (server 1.56.0), an attack's the rest. */
    fun critChance(spell: Boolean): Double = if (spell) spellCritChance else critChance
    fun critMultiplier(spell: Boolean): Double = if (spell) spellCritMultiplier else critMultiplier
    val doubleDamage = percent(CoreStat.DOUBLE_DAMAGE.code)

    /** A foe it hits left under this share of its life dies. */
    val culling = percent(CoreStat.CULLING.code)

    /** Increased damage for each charge of [kind] it holds. */
    fun perCharge(kind: ChargeKind): Double = stat("STOCK_DAMAGE_PER_${kind.name}")
    val shockEffect = max(0.0, 1 + stat(CoreStat.SHOCK_EFFECT.code) / 100)
    val chillEffect = max(0.0, 1 + stat(CoreStat.CHILL_EFFECT.code) / 100)

    /** How much faster its damaging ailments run their damage, as a share. */
    val fasterAilments = max(0.0, stat(CoreStat.FASTER_AILMENTS.code)) / 100
    fun buffChance(kind: BuffKind): Double = percent(kind.chance)

    /** It wears [kind] always: a monster's modifier or a map's. */
    fun wears(kind: BuffKind): Boolean = stat(kind.always) > 0
    val buffDuration = max(0.1, 1 + stat(BuffKind.DURATION) / 100)

    /** Damage taken from hits, not over time: Fortify's work. */
    val hitTaken = max(0.1, 1 + stat(CoreStat.HIT_TAKEN.code) / 100)

    /** Доля удара осквернённого монстра, что снимает Скверна (4.0.0): «% меньше урона от монстров Скверны», не больше 100%. */
    val blightReduction = percent(CoreStat.BLIGHT_REDUCTION.code)
    val suppression = percent(CoreStat.SPELL_SUPPRESSION.code, rules.defence.suppressionCap)
    val deflection = percent(CoreStat.DEFLECTION.code, rules.defence.deflectionCap)

    /** Its chance to block a spell: the block, and the spell block on top, under the same ceiling. */
    val spellBlock = (stat(CoreStat.BLOCK_CHANCE.code) + stat(CoreStat.SPELL_BLOCK.code)).coerceIn(0.0, ceiling(rules.ceilings.block)) / 100
    val lifeOnBlock = max(0.0, stat(CoreStat.HEALTH_ON_BLOCK.code))
    val manaOnBlock = max(0.0, stat(CoreStat.MANA_ON_BLOCK.code))
    val shieldOnBlock = max(0.0, stat(CoreStat.SHIELD_ON_BLOCK.code))

    /** The share of its maximum life a kill gives back. */
    val lifeOnKillShare = percent(CoreStat.HEALTH_ON_KILL_PERCENT.code)
    val shieldOnKill = max(0.0, stat(CoreStat.SHIELD_ON_KILL.code))

    /** The share of damage taken that comes back as life over the rule's seconds. */
    val recoup = percent(CoreStat.LIFE_RECOUP.code)

    /** The share of its armour that meets elemental hits too. */
    val armourElemental = percent(CoreStat.ARMOUR_ELEMENTAL.code)

    /** How much sooner its shield starts to recharge after a hit. */
    val rechargeStart = max(0.1, 1 + stat(CoreStat.SHIELD_RECHARGE_START.code) / 100)

    /** Taunts (since server 0.62.0): while it stands, its foes must strike it first. */
    val taunt: Boolean get() = stat(CoreStat.TAUNT.code) > 0

    /** How hard it presses, before anyone's defences: its damage per swing times its swings per second. */
    val threat: Double get() = damage.values.sum() * attackSpeed

    /** The damage type most of its blow is made of. */
    val leading: DamageType get() = damage.maxBy { it.value }.key
}
