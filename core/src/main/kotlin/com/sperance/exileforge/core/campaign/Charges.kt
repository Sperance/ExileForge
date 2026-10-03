package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.ChargeRules
import java.util.EnumMap
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** A kind of the hero's charges as the fight's counter draws it (3.33.0): how many, the most, and the seconds left. */
data class ChargeView(val kind: ChargeKind, val count: Int, val max: Int, val seconds: Double) {
    /** The stat whose drawing the kind borrows: its maximum's. */
    val stat: String get() = "STOCK_MAX_${kind.name}_CHARGES"
}

/**
 * The hero's frenzy, power and endurance charges in one fight (3.33.0, server 1.32.0) under the rules' [rules]: at most
 * [ChargeRules.max] of a kind, all of a kind living as long as the last one gained — a gain refreshes every charge of it —
 * each laying its kind's lines on the hero. A fight starts with none unless the stage before hands its own on.
 */
internal class HeroCharges(private val rules: ChargeRules, carried: Map<ChargeKind, Int> = emptyMap()) {
    private val counts = EnumMap<ChargeKind, Int>(ChargeKind::class.java)
    private val until = EnumMap<ChargeKind, Double>(ChargeKind::class.java)

    init {
        carried.forEach { (kind, count) -> if (kind.real && count > 0) counts[kind] = count }
    }

    val any: Boolean get() = counts.values.any { it > 0 }

    fun count(kind: ChargeKind): Int = if (kind == ChargeKind.ALL) ChargeKind.REAL.sumOf { count(it) } else counts[kind] ?: 0

    /** The charges handed on to the next stage of a staged fight. */
    fun snapshot(): Map<ChargeKind, Int> = counts.filterValues { it > 0 }

    /** The carried charges start their lives now, capped by [sheet]'s maximum. */
    fun start(now: Double, sheet: Map<String, Double>) {
        val life = rules.lifetime(sheet)
        counts.keys.toList().forEach { kind ->
            counts[kind] = min(counts.getValue(kind), rules.max(kind, sheet))
            until[kind] = now + life
        }
    }

    /** [amount] charges of [kind] — each of `RANDOM` drawn from [random] — up to the maximum; whether any count changed. */
    fun gain(kind: ChargeKind, amount: Int, now: Double, sheet: Map<String, Double>, random: Random): Boolean {
        if (amount <= 0 || kind == ChargeKind.ALL) return false
        val life = rules.lifetime(sheet)
        var changed = false
        repeat(amount) {
            val real = if (kind == ChargeKind.RANDOM) ChargeKind.REAL[random.nextInt(ChargeKind.REAL.size)] else kind
            val cap = rules.max(real, sheet)
            if (cap <= 0) return@repeat
            val was = counts[real] ?: 0
            counts[real] = min(cap, was + 1)
            until[real] = now + life
            changed = changed || counts[real] != was
        }
        return changed
    }

    /** Takes every charge of [kind] (`ALL` — of every kind); how many were taken. */
    fun consume(kind: ChargeKind): Int {
        val kinds = if (kind == ChargeKind.ALL) ChargeKind.REAL else listOf(kind)
        return kinds.sumOf { counts.remove(it) ?: 0 }.also { kinds.forEach(until::remove) }
    }

    /** The kinds whose time ran out go; whether any did. */
    fun expire(now: Double): Boolean {
        val gone = until.filterValues { it <= now + 1e-9 }.keys
        if (gone.isEmpty()) return false
        gone.forEach {
            counts.remove(it)
            until.remove(it)
        }
        return true
    }

    /** What the charges lay on the hero: each kind's lines times its count. */
    fun lines(): List<StatLine> = counts.flatMap { (kind, count) ->
        if (count <= 0) emptyList() else rules.kinds[kind]?.lines.orEmpty().map { StatLine(it.stat, it.op, (it.value ?: 0.0) * count) }
    }

    fun views(now: Double, sheet: Map<String, Double>): List<ChargeView> = ChargeKind.REAL.mapNotNull { kind ->
        val count = counts[kind]?.takeIf { it > 0 } ?: return@mapNotNull null
        ChargeView(kind, count, rules.max(kind, sheet), max(0.0, (until[kind] ?: now) - now))
    }
}

/**
 * The hero's sheet on their combat pet (3.33.0, server 1.32.0): increased damage, life and attack speed, flat life,
 * armour and every elemental resistance. Levels ([LEVEL]) are the pet's sheet rolled higher, which the caller does.
 */
object PetBoons {
    const val DAMAGE = "STOCK_PET_DAMAGE"
    const val HEALTH = "STOCK_PET_HEALTH"
    const val LEVEL = "STOCK_PET_LEVEL"
    const val SPEED = "STOCK_PET_ATTACK_SPEED"
    const val LIFE = "STOCK_PET_LIFE"
    const val ARMOR = "STOCK_PET_ARMOR"
    const val RESIST = "STOCK_PET_RESIST"
    val STATS = listOf(DAMAGE, HEALTH, LEVEL, SPEED, LIFE, ARMOR, RESIST)

    /** What of the hero's [hero] sheet reaches the pet: the pet stats that are not zero. */
    fun of(hero: Map<String, Double>): Map<String, Double> = STATS.mapNotNull { stat -> hero[stat]?.takeIf { it != 0.0 }?.let { stat to it } }.toMap()

    /** The pet's sheet [pet] under the hero's [boons]. */
    fun apply(pet: Map<String, Double>, boons: Map<String, Double>): Map<String, Double> {
        if (boons.isEmpty()) return pet
        val sheet = pet.toMutableMap()
        fun boon(stat: String) = boons[stat] ?: 0.0
        val damage = max(0.0, 1 + boon(DAMAGE) / 100)
        if (damage != 1.0) DamageType.entries.forEach { type -> sheet[type.attack]?.let { sheet[type.attack] = it * damage } }
        if (boon(LIFE) != 0.0 || boon(HEALTH) != 0.0) sheet["STOCK_HEALTH"] = max(1.0, ((pet["STOCK_HEALTH"] ?: 0.0) + boon(LIFE)) * max(0.0, 1 + boon(HEALTH) / 100))
        if (boon(SPEED) != 0.0) pet["STOCK_ATTACK_SPEED"]?.takeIf { it > 0 }?.let { sheet["STOCK_ATTACK_SPEED"] = it * max(0.1, 1 + boon(SPEED) / 100) }
        if (boon(ARMOR) != 0.0) sheet["STOCK_ARMOR"] = (pet["STOCK_ARMOR"] ?: 0.0) + boon(ARMOR)
        if (boon(RESIST) != 0.0) sheet["STOCK_RESIST_ALL"] = (pet["STOCK_RESIST_ALL"] ?: 0.0) + boon(RESIST)
        return sheet
    }
}
