package com.sperance.exileforge.core.campaign

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Who struck. */
enum class Side { HERO, MONSTER }

/** How a swing ended: it landed, landed hard, or never reached. */
enum class HitKind { HIT, CRIT, EVADED, BLOCKED }

/** How the whole fight ended; a retreat is a fight nobody won inside [Combat.TIME_LIMIT]. */
enum class Outcome { WIN, LOSS, RETREAT }

/** Damage by type, as the sheet names it. */
enum class DamageType(val attack: String, val resist: String?) {
    PHYSICAL("STOCK_ATTACK_PHYSICAL", null),
    FIRE("STOCK_ATTACK_FIRE", "STOCK_RESIST_FIRE"),
    COLD("STOCK_ATTACK_COLD", "STOCK_RESIST_COLD"),
    LIGHTNING("STOCK_ATTACK_LIGHTNING", "STOCK_RESIST_LIGHTNING"),
    CHAOS("STOCK_ATTACK_CHAOS", "STOCK_RESIST_CHAOS"),
    MAGICAL("STOCK_ATTACK_MAGICAL", null),
}

/**
 * One side of a fight, read off a stat table — the hero's sheet or a rolled monster.
 *
 * Every number here is one the server sent; the fight only decides what they do to each other.
 * A hero with no weapon still swings — unarmed, as in PoE — and a missing critical chance is the
 * 5% every attack has.
 */
data class Combatant(val stats: Map<String, Double>, val level: Int) {
    private fun stat(name: String) = stats[name] ?: 0.0

    val maxLife = max(1.0, stat("STOCK_HEALTH"))
    val maxShield = max(0.0, stat("STOCK_ENERGY_SHIELD"))
    val damage: Map<DamageType, Double> = DamageType.entries.associateWith { max(0.0, stat(it.attack)) }
        .let { rolled -> if (rolled.values.sum() > 0) rolled else rolled + (DamageType.PHYSICAL to UNARMED_DAMAGE) }
    val attackSpeed = stat("STOCK_ATTACK_SPEED").takeIf { it > 0 }?.coerceIn(0.3, 5.0) ?: UNARMED_SPEED
    val critChance = (stats["STOCK_CRITICAL_CHANCE"] ?: 5.0).coerceIn(0.0, 100.0) / 100
    val critMultiplier = max(100.0, (stats["STOCK_CRITICAL_MULTIPLIER"] ?: 150.0) + stat("STOCK_CRITICAL_DAMAGE")) / 100
    val armour = max(0.0, stat("STOCK_ARMOR"))
    val evasion = max(0.0, stat("STOCK_EVASION"))
    val block = stat("STOCK_BLOCK_CHANCE").coerceIn(0.0, CAP) / 100
    /** Chaos stands alone, as in PoE; "all resistances" covers the three elements. */
    fun resist(type: DamageType): Double = when (val name = type.resist) {
        null -> 0.0
        "STOCK_RESIST_CHAOS" -> stat(name)
        else -> stat(name) + stat("STOCK_RESIST_ALL")
    }.coerceIn(-100.0, CAP) / 100
    val lifeRegen = max(0.0, stat("STOCK_HEALTH_REGEN"))
    val shieldRegen = max(0.0, stat("STOCK_ENERGY_REGEN"))
    val leechPhysical = max(0.0, stat("STOCK_LEECH_PHYSICAL")) / 100
    val leechMagical = max(0.0, stat("STOCK_LEECH_MAGICAL")) / 100
    val leechAll = max(0.0, stat("STOCK_LEECH_ALL")) / 100
    val critLeech = max(0.0, stat("STOCK_CRITICAL_VAMPIRE")) / 100
    val stunThreshold = max(0.0, stat("STOCK_STUN_THRESHOLD"))

    companion object {
        const val UNARMED_DAMAGE = 4.0
        const val UNARMED_SPEED = 1.2
        const val CAP = 75.0
    }
}

/** One swing, and where both sides stand after it. */
data class CombatEvent(
    val time: Double,
    val attacker: Side,
    val kind: HitKind,
    val damage: Double,
    val healed: Double,
    val stunned: Boolean,
    val heroLife: Double,
    val heroShield: Double,
    val monsterLife: Double,
    val monsterShield: Double,
)

data class CombatLog(val events: List<CombatEvent>, val outcome: Outcome, val heroLife: Double, val duration: Double)

/**
 * The automatic fight — the client's since server 0.26.0, by the owner's decision.
 *
 * Each side swings at its own attack speed until one falls. A swing can be evaded (evasion against
 * the attacker's level), blocked, or land; a landing hit rolls ±20% per damage type, may be a
 * critical strike, and is reduced by armour (physical, PoE's `armour / (armour + 5 × damage)`)
 * and by resistances (elemental and chaos, capped at 75%). Energy shield takes a hit before life,
 * except chaos, which goes around it. Leech gives back a share of what was dealt; a hit big enough
 * against the target's life stuns it, pushing its next swing back. Life and shield regenerate
 * between swings. Spells, mana, curses and auras are not part of it yet.
 *
 * TODO: spells — cast speed, cast strength, mana and its regeneration, magical damage as a
 *  second rhythm beside attacks; curses and auras.
 *
 * The fight is drawn from the returned log, event by event, so the scene can play it at any speed
 * and a replay is the same fight. The hero's life carries over between fights; the shield
 * recharges, as it does in PoE once a fight is over.
 */
object Combat {
    const val TIME_LIMIT = 60.0
    private const val STUN_DELAY = 0.4
    private const val STUN_SHARE = 0.15
    private const val VARIANCE = 0.2

    fun fight(hero: Combatant, monster: Combatant, heroLife: Double, random: Random): CombatLog {
        val life = doubleArrayOf(heroLife.coerceIn(1.0, hero.maxLife), monster.maxLife)
        val shield = doubleArrayOf(hero.maxShield, monster.maxShield)
        val next = doubleArrayOf(0.35, 0.55)
        val sides = arrayOf(hero, monster)
        val events = mutableListOf<CombatEvent>()
        var now = 0.0

        while (true) {
            val index = if (next[0] <= next[1]) 0 else 1
            val at = next[index]
            if (at > TIME_LIMIT) return CombatLog(events, Outcome.RETREAT, life[0], TIME_LIMIT)
            val elapsed = at - now
            now = at
            for (i in 0..1) {
                life[i] = min(sides[i].maxLife, life[i] + sides[i].lifeRegen * elapsed)
                shield[i] = min(sides[i].maxShield, shield[i] + sides[i].shieldRegen * elapsed)
            }

            val attacker = sides[index]
            val target = 1 - index
            val defender = sides[target]
            next[index] = now + 1 / attacker.attackSpeed

            val evade = (defender.evasion / (defender.evasion + 150 + 40 * attacker.level)).coerceAtMost(Combatant.CAP / 100)
            val kind = when {
                random.nextDouble() < evade -> HitKind.EVADED
                random.nextDouble() < defender.block -> HitKind.BLOCKED
                random.nextDouble() < attacker.critChance -> HitKind.CRIT
                else -> HitKind.HIT
            }
            var dealt = 0.0
            var healed = 0.0
            var stunned = false
            if (kind == HitKind.HIT || kind == HitKind.CRIT) {
                val multiplier = if (kind == HitKind.CRIT) attacker.critMultiplier else 1.0
                var physical = 0.0
                var magical = 0.0
                var chaos = 0.0
                attacker.damage.forEach { (type, base) ->
                    if (base <= 0) return@forEach
                    val raw = base * (1 + (random.nextDouble() * 2 - 1) * VARIANCE) * multiplier
                    val taken = when (type) {
                        DamageType.PHYSICAL -> raw * (1 - (defender.armour / (defender.armour + 5 * raw)).coerceAtMost(0.9))
                        else -> raw * (1 - defender.resist(type))
                    }.coerceAtLeast(0.0)
                    when (type) {
                        DamageType.PHYSICAL -> physical += taken
                        DamageType.CHAOS -> chaos += taken
                        else -> magical += taken
                    }
                }
                val shielded = physical + magical
                val absorbed = min(shield[target], shielded)
                shield[target] -= absorbed
                dealt = physical + magical + chaos
                life[target] = max(0.0, life[target] - (shielded - absorbed) - chaos)

                healed = physical * attacker.leechPhysical + (magical + chaos) * attacker.leechMagical + dealt * attacker.leechAll +
                    (if (kind == HitKind.CRIT) dealt * attacker.critLeech else 0.0)
                life[index] = min(attacker.maxLife, life[index] + healed)

                if (life[target] > 0 && dealt >= defender.maxLife * STUN_SHARE + defender.stunThreshold) {
                    stunned = true
                    next[target] += STUN_DELAY
                }
            }
            events += CombatEvent(now, if (index == 0) Side.HERO else Side.MONSTER, kind, dealt, healed, stunned, life[0], shield[0], life[1], shield[1])
            if (life[1] <= 0) return CombatLog(events, Outcome.WIN, life[0], now)
            if (life[0] <= 0) return CombatLog(events, Outcome.LOSS, 0.0, now)
        }
    }

    /** Life regained per second while walking: the hero's own regeneration, and never less than 3% of life. */
    fun walkingRegen(hero: Combatant): Double = max(hero.lifeRegen, hero.maxLife * 0.03)
}
