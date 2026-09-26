package com.sperance.exileforge.core.model.powers

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor

/**
 * The powers of unique and mythic items (2.79.0, server 0.70.0): each is a `POWER_*` stat no other item
 * carries. The item rolls its value as any line; what the value does is the book's entry — data, so a
 * new power is a row of the server's `powers.json`, not code here.
 *
 * A power works through its [Power.sheet] rules (conversions, gains, a stat set — on the sheet, the same
 * pass as the server's), its answer to a fight's [Power.on] event (read by the fight), or out of the
 * fight ([Power.world], the server's alone). A number an effect leaves out is the roll, where [Power.roll] says.
 */
@Serializable data class PowerBook(val powers: List<Power> = emptyList()) {
    val byStat: Map<String, Power> by lazy { powers.associateBy { it.stat } }
    private val sheetPowers: List<Power> by lazy { powers.filter { it.sheet.isNotEmpty() } }

    /**
     * The sheet rules of every power whose stat [stats] carries, in the book's order, rounded as the
     * server rounds: the one pass the server makes after adding the modifiers up.
     */
    fun applySheet(stats: MutableMap<String, Double>): MutableMap<String, Double> {
        sheetPowers.forEach { power ->
            val rolled = stats[power.stat] ?: 0.0
            if (rolled == 0.0) return@forEach
            power.sheet.forEach { rule ->
                val source = rule.from?.let { stats[it] } ?: 0.0
                val value = rule.value ?: rolled
                stats[rule.to] = round(when (rule.op) {
                    SheetOp.CONVERT -> {
                        val moved = source * value.coerceIn(0.0, 100.0) / 100
                        stats[rule.from!!] = round(source - moved)
                        (stats[rule.to] ?: 0.0) + moved * rule.factor
                    }
                    SheetOp.GAIN -> (stats[rule.to] ?: 0.0) + source * value / 100 * rule.factor
                    SheetOp.PER -> (stats[rule.to] ?: 0.0) + floor(source / rule.per) * value
                    SheetOp.SET -> value
                    SheetOp.MORE -> (stats[rule.to] ?: 0.0) * (1 + value / 100)
                })
            }
        }
        return stats
    }

    private fun round(value: Double): Double = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toDouble()
}

@Serializable data class Power(
    val stat: String,
    val roll: PowerRoll = PowerRoll.AMOUNT,
    val sheet: List<SheetRule> = emptyList(),
    val on: PowerEvent? = null,
    /** The period of [PowerEvent.EVERY], in seconds. */
    val every: Double = 0.0,
    val checks: List<PowerCheck> = emptyList(),
    /** In percent; null is always — or the roll, when [roll] is [PowerRoll.CHANCE]. */
    val chance: Double? = null,
    /** In seconds; below zero, once a fight. */
    val cooldown: Double = 0.0,
    val effects: List<PowerEffect> = emptyList(),
) {
    /** The roll [value] where this power puts it, or the effect's own number. */
    fun amount(own: Double?, value: Double): Double = own ?: if (roll == PowerRoll.AMOUNT) value else 0.0
    fun duration(own: Double?, value: Double): Double = own ?: if (roll == PowerRoll.DURATION) value else 0.0
    fun chance(value: Double): Double = chance ?: if (roll == PowerRoll.CHANCE) value else 100.0
}

@Serializable enum class PowerRoll { AMOUNT, CHANCE, DURATION }

/** A rule of the sheet: CONVERT and GAIN take a share in percent (times [factor]), PER a gain for every [per] of the source, SET a value, MORE a multiplier. */
@Serializable data class SheetRule(val op: SheetOp, val from: String? = null, val to: String, val factor: Double = 1.0, val per: Double = 1.0, val value: Double? = null)

@Serializable enum class SheetOp { CONVERT, GAIN, PER, SET, MORE }

/** What a power answers in a fight; STANDING is no event but lines that lie on while the checks hold. */
@Serializable enum class PowerEvent {
    STANDING, FIGHT_START, EVERY, HIT, CRIT, KILL, HIT_TAKEN, CRIT_TAKEN, BLOCK, EVADE, SKILL_USE,
    FLASK, LOW_LIFE, SHIELD_BROKEN, INFLICT, AILED, STUN, DEATH,
}

@Serializable data class PowerCheck(val check: PowerCheckKind, val value: Double = 0.0, val word: String? = null)

@Serializable enum class PowerCheckKind {
    LIFE_BELOW, LIFE_ABOVE, LIFE_FULL, SHIELD_FULL, SHIELD_EMPTY, MANA_BELOW, MANA_ABOVE,
    FOES_AT_LEAST, FOES_AT_MOST, TARGET_RARE, TARGET_AILED, TARGET_AILMENT, TARGET_CURSED,
    TARGET_LIFE_BELOW, TARGET_LIFE_ABOVE, FIGHT_BEFORE, FIGHT_AFTER, SELF_AILED, SELF_CLEAN,
    FLASK_RUNNING, BARRIER_UP, NTH, SPELL, ATTACK, DAMAGE_TYPE, AILMENT,
}

/** What a power does: [amount] (null — the roll) of [of], to [to]; BUFF and HEX lay [lines] for [duration], up to [stacks] times. */
@Serializable data class PowerEffect(
    val act: PowerAct,
    val amount: Double? = null,
    val lines: List<PowerLine> = emptyList(),
    val duration: Double? = null,
    val stacks: Int = 1,
    val of: PowerBase = PowerBase.WEAPON,
    val type: String? = null,
    val to: PowerTarget = PowerTarget.TARGET,
    val ailment: String? = null,
    /** The share of the damage dealt given back as life, in percent. */
    val leech: Double = 0.0,
)

@Serializable enum class PowerAct {
    BUFF, HEX, HEAL, HURT, BARRIER, DAMAGE, AILMENT, CURSE, EXECUTE, STUN, DELAY, RUSH, CHARGES,
    COOLDOWNS, NEXT_CRIT, INVULNERABLE, CLEANSE, SPREAD,
}

/** A line of a buff or a curse: [value] null is the roll; [scale] multiplies it by a count of the fight, at most [cap] (0 — no cap). */
@Serializable data class PowerLine(val stat: String, val operation: String = "ADD", val value: Double? = null, val scale: PowerScale? = null, val cap: Double = 0.0)

@Serializable enum class PowerScale { FOES, MISSING_LIFE, SECONDS, KILLS, CURSED_FOES, AILED_FOES, SHIELD, MANA, CHARGES }

@Serializable enum class PowerBase { WEAPON, LIFE, SHIELD, MANA, ARMOUR, EVASION, TAKEN, DEALT, TARGET_LIFE, STRENGTH, AGILITY, INTELLECT }

@Serializable enum class PowerTarget { TARGET, ALL, RANDOM, OTHERS, SELF }
