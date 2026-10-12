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
 * each laying its kind's lines on the hero. A fight starts with none.
 */
internal class HeroCharges(private val rules: ChargeRules) {
    private val counts = EnumMap<ChargeKind, Int>(ChargeKind::class.java)
    private val until = EnumMap<ChargeKind, Double>(ChargeKind::class.java)

    fun count(kind: ChargeKind): Int = if (kind == ChargeKind.ALL) ChargeKind.REAL.sumOf { count(it) } else counts[kind] ?: 0

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

/** Лист героя на его боевом питомце (4.9.0: одно правило с Мощью сервера, [com.sperance.exileforge.rules.sheet.PetBoons]). */
typealias PetBoons = com.sperance.exileforge.rules.sheet.PetBoons
