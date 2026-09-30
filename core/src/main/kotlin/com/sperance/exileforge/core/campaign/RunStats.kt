package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.FightTally
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.roundToLong

/**
 * A run's figures (3.47.0), gathered fight by fight from the combat's own events: what the hero dealt by damage
 * type and by skill, what was taken by type, and the seconds spent fighting. A hit carries its trace, which splits
 * it by type exactly; a tick or a line without one counts under the type that led it.
 */
class RunStats {
    private val dealt = mutableMapOf<DamageType, Double>()
    private val bySkill = mutableMapOf<String, Double>()
    private val taken = mutableMapOf<DamageType, Double>()
    private var seconds = 0.0

    fun add(pack: List<PackHit>, duration: Double) {
        seconds += duration
        pack.flatMap { it.events }.filter { it.damage > 0 && !it.onSelf }.forEach { event ->
            val into = if (event.actor == Side.HERO) dealt else taken
            split(event).forEach { (type, amount) -> into.merge(type, amount, Double::plus) }
            if (event.actor == Side.HERO) bySkill.merge(skillKey(event), event.damage, Double::plus)
        }
    }

    fun summary(kills: Int): RunSummary = RunSummary(dealt.toMap(), bySkill.toMap(), taken.toMap(), seconds, kills)

    companion object {
        /** A plain swing, an ailment's tick and a reflection have no skill of their own: they are named by what they are. */
        const val ATTACK = "ATTACK"
        const val TICK = "TICK"
        const val REFLECT = "REFLECT"

        /** The last [count] blows the hero took before the fall, oldest first, each with who struck it. */
        fun recap(pack: List<PackHit>, count: Int = 5): List<DeathHit> =
            pack.flatMap { hit -> hit.events.map { it to hit.monster } }
                .filter { (event, _) -> event.actor == Side.MONSTER && !event.onSelf && event.damage > 0 }
                .sortedBy { (event, _) -> event.time }.takeLast(count)
                .map { (event, monster) -> DeathHit(monster, event.damage, event.type, skillKey(event), event.kind == HitKind.CRIT, event.heroLife) }

        private fun skillKey(event: CombatEvent): String = event.skill ?: when (event.action) {
            Action.TICK -> TICK
            Action.REFLECT -> REFLECT
            else -> ATTACK
        }

        internal fun split(event: CombatEvent): Map<DamageType, Double> =
            (event.trace as? HitTrace)?.types?.filter { it.dealt > 0 }?.associate { it.type to it.dealt }?.takeIf { it.isNotEmpty() }
                ?: mapOf((event.type ?: DamageType.PHYSICAL) to event.damage)
    }
}

/**
 * One fight as the hero's statistics count it (3.51.0, server 1.49.0): damage by type dealt, taken and leeched, the
 * outcomes of the hero's blows and of the foes', ailments laid, the seconds and the hardest blow. Sent with the journal's
 * `FIGHT` event; the server only adds it up.
 */
object FightFigures {
    /** The damage types in the order the chronicle lists them. */
    val types: List<String> = DamageType.entries.map { it.name }

    fun of(pack: List<PackHit>, duration: Double, boss: Boolean, won: Boolean): FightTally {
        val events = pack.flatMap { it.events }.filter { !it.onSelf }
        val mine = events.filter { it.actor == Side.HERO && (it.action == Action.ATTACK || it.action == Action.SKILL || it.action == Action.TICK || it.action == Action.REFLECT) }
        val theirs = events.filter { it.actor == Side.MONSTER && (it.action == Action.ATTACK || it.action == Action.SKILL) }
        val dealt = mutableMapOf<String, Double>()
        mine.filter { it.damage > 0 }.forEach { event -> RunStats.split(event).forEach { (type, amount) -> dealt.merge(type.name, amount, Double::plus) } }
        return FightTally(
            dealt = dealt.mapValues { it.value.roundToLong() }.filterValues { it > 0 },
            taken = events.filter { it.actor == Side.MONSTER && it.damage > 0 }.sumOf { it.damage }.roundToLong(),
            healed = events.filter { it.actor == Side.HERO }.sumOf { it.healed }.roundToLong(),
            hits = mine.count { it.action != Action.TICK && it.action != Action.REFLECT && (it.kind == HitKind.HIT || it.kind == HitKind.CRIT) },
            crits = mine.count { it.kind == HitKind.CRIT },
            misses = mine.count { it.kind == HitKind.EVADED },
            blocked = theirs.count { it.kind == HitKind.BLOCKED },
            evaded = theirs.count { it.kind == HitKind.EVADED },
            ailments = mine.sumOf { it.inflicted.size },
            millis = (duration * 1000).roundToLong(),
            maxHit = mine.maxOfOrNull { it.damage }?.roundToLong() ?: 0,
            boss = boss, won = won,
        )
    }
}

/** The run so far, as the report reads it: damage dealt and taken by type, dealt by skill, the fighting time and the kills. */
data class RunSummary(
    val dealt: Map<DamageType, Double> = emptyMap(),
    val bySkill: Map<String, Double> = emptyMap(),
    val taken: Map<DamageType, Double> = emptyMap(),
    val seconds: Double = 0.0,
    val kills: Int = 0,
) {
    val totalDealt: Double get() = dealt.values.sum()
    val totalTaken: Double get() = taken.values.sum()
    /** Damage a second of fighting — the walking between fights is not the hero's damage. */
    val dps: Double get() = if (seconds > 0) totalDealt / seconds else 0.0
    val killsPerMinute: Double get() = if (seconds > 0) kills / seconds * 60 else 0.0
}

/** One blow of the death recap: who struck, how hard, of what type, with what, whether it was a critical, and the life left after it. */
data class DeathHit(val monster: RolledMonster, val damage: Double, val type: DamageType?, val skill: String, val crit: Boolean, val lifeAfter: Double)
