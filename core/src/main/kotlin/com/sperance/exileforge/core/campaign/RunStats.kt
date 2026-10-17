package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.run.PackHit
import com.sperance.exileforge.rules.content.FightTally
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.roundToLong

/**
 * A run's figures, gathered fight by fight from the combat's own events. Сумма разборов боёв
 * ([FightStats]): урон по типам и умениям, полученное, защиты и восстановление; секунды - только боя.
 */
class RunStats {
    private var total = FightStats.NONE

    /** Последняя сводка ([summary]): та же, пока цифры и убийства не сдвинулись - снимок экрана не копирует их на кадр. */
    private var shown: RunSummary? = null

    fun add(pack: List<PackHit>, duration: Double) {
        shown = null
        total += FightStats.of(pack, duration)
    }

    /** Another run's figures summed into these: a Vaal zone's, back on the map it was entered from. */
    fun add(other: RunSummary) {
        shown = null
        total += other.fights
    }

    fun summary(kills: Int): RunSummary = shown?.takeIf { it.kills == kills } ?: RunSummary(total, kills).also { shown = it }

    companion object {
        /** A plain swing, an ailment's tick and a reflection have no skill of their own: they are named by what they are. */
        const val ATTACK = "ATTACK"
        const val TICK = "TICK"
        const val REFLECT = "REFLECT"

        /** The last [count] blows the hero took before the fall, oldest first, each with who struck it. */
        fun recap(pack: List<PackHit>, count: Int = 5): List<DeathHit> = pack.flatMap { hit -> hit.events.map { it to hit.monster } }
            .filter { (event, _) -> event.actor == Side.MONSTER && !event.onSelf && event.damage > 0 }
            .sortedBy { (event, _) -> event.time }.takeLast(count)
            .map { (event, monster) -> DeathHit(monster, event.damage, event.type, skillKey(event), event.kind == HitKind.CRIT, event.heroLife) }

        internal fun skillKey(event: CombatEvent): String = event.skill ?: when (event.action) {
            Action.TICK -> TICK
            Action.REFLECT -> REFLECT
            else -> ATTACK
        }

        internal fun split(event: CombatEvent): Map<DamageType, Double> = (event.trace as? HitTrace)?.types?.filter { it.dealt > 0 }?.associate { it.type to it.dealt }?.takeIf { it.isNotEmpty() }
            ?: mapOf((event.type ?: DamageType.PHYSICAL) to event.damage)
    }
}

/**
 * One fight as the hero's statistics count it: damage by type dealt, taken and leeched, the
 * outcomes of the hero's blows and of the foes', ailments laid, the seconds and the hardest blow. Sent with the journal's
 * `FIGHT` event; the server only adds it up.
 */
object FightFigures {
    /** The damage types in the order the chronicle lists them. */
    val types: List<String> = DamageType.entries.map { it.name }

    /** Враг, чей удар кончил проигранный бой, - убийца: статистика героя считает смерти по убийцам. */
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
            killer = if (won) {
                null
            } else {
                pack.flatMap { hit -> hit.events.map { it to hit.monster } }
                    .filter { (event, _) -> event.actor == Side.MONSTER && event.damage > 0 && !event.onSelf }.maxByOrNull { (event, _) -> event.time }?.second?.code?.value
            },
        )
    }
}

/**
 * The run so far, as the report reads it: разбор всех боёв захода ([fights]) и убийства; итоги нанесённого и полученного и секунды
 * боя - из него.
 */
data class RunSummary(
    val fights: FightStats = FightStats.NONE,
    val kills: Int = 0,
) {
    val seconds: Double get() = fights.seconds
    val totalDealt: Double get() = fights.dealt.total
    val totalTaken: Double get() = fights.taken.total

    /** Damage a second of fighting — the walking between fights is not the hero's damage. */
    val dps: Double get() = fights.dps
    val killsPerMinute: Double get() = if (seconds > 0) kills / seconds * 60 else 0.0
}

/** One blow of the death recap: who struck, how hard, of what type, with what, whether it was a critical, and the life left after it. */
data class DeathHit(val monster: RolledMonster, val damage: Double, val type: DamageType?, val skill: String, val crit: Boolean, val lifeAfter: Double)
