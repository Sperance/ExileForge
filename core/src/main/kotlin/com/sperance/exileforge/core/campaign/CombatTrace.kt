package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster

/**
 * How a line of the fight's log came about (3.37.0): the dice it drew, the formula step by step, what each side stood on
 * at that moment and where the damage went. Every number is the fight's own, taken as it was computed — the card that
 * explains a line reads it, nothing is worked out again.
 */
sealed interface Trace

/** A roll of the fight's dice: its [chance] of coming true and the number [rolled]; it came true under the chance. */
data class RollTrace(val key: RollKey, val chance: Double, val rolled: Double, val ailment: Ailment? = null) {
    val success: Boolean get() = rolled < chance
}

enum class RollKey { EVADE, BLOCK, CRIT, CRIT_LUCKY, SUPPRESS, DEFLECT, DOUBLE, AILMENT, AVOID, STUN, AVOID_STUN, BUFF }

/**
 * One step of a hit's formula: its multiplier [value] (the first step is the base damage itself) and the stats of the
 * striker and of the target it reads, whose sources the card lays out.
 */
data class FactorTrace(val key: FactorKey, val value: Double, val attacker: List<String> = emptyList(), val target: List<String> = emptyList())

enum class FactorKey { BASE, SPREAD, CRIT, NON_CRIT, DAMAGE, AGAINST, DOUBLE, VERSUS, DEFENCE, SHOCK, LONE_WOLF, TAKEN, EASED, TOTAL }

/** One damage type of a hit: its [base], [raw] after the striker's multipliers, the [armour] and [resist] shares it lost, and what landed. */
data class TypeTrace(val type: DamageType, val base: Double, val raw: Double, val armour: Double, val resist: Double, val penetration: Double, val dealt: Double)

/** A line laid on a fighter in the fight, named by what laid it. */
data class LineSource(val kind: LineKind, val ref: String, val line: StatLine)

enum class LineKind { BUFF, CURSE, FLASK, CHARGE, CONDITION, LOW_LIFE, POWER, AURA, SKILL }

/** A fighter at the moment of a line: its sheet as it stood, what the fight laid on it, its ailments and pools. */
data class FighterShot(
    val side: Side, val index: Int, val stats: Map<String, Double>, val lines: List<LineSource>,
    val ailments: List<ActiveAilment>, val life: Double, val maxLife: Double, val shield: Double,
    val conditions: Set<Condition> = emptySet(),
)

/** Where a hit went: the barrier, the shield, the mana before life, the delayed share, life; and what the striker got back. */
data class Landing(
    val barrier: Double, val shield: Double, val mana: Double, val delayed: Double, val life: Double,
    val leech: Double, val onHit: Double, val recoup: Double, val culled: Boolean,
)

/** A blow — a swing, a skill's hit, a spell, a reflection. [landing] is null for an evaded or blocked one. */
data class HitTrace(
    val attacker: FighterShot, val target: FighterShot, val rolls: List<RollTrace>, val factors: List<FactorTrace>,
    val types: List<TypeTrace>, val landing: Landing?, val origin: TraceOrigin,
) : Trace

/** A second of an ailment's damage: how strong, how long, who laid it and what grew it. */
data class TickTrace(
    val ailment: Ailment, val perSecond: Double, val left: Double, val duration: Double, val stacks: Int,
    val target: FighterShot, val factors: List<FactorTrace>, val origin: TraceOrigin, val striker: FighterShot? = null,
) : Trace

/** A draught, a buff, a curse, a heal: the lines it laid, for how long, what it healed and what made it stronger. */
data class EffectTrace(
    val kind: EffectKind, val source: String, val lines: List<StatLine>, val duration: Double, val healed: Double,
    val scale: Double, val actor: FighterShot, val origin: TraceOrigin,
) : Trace

/** Something that happened without a blow: a buff or a charge gained, a power, a condition, what a kill brought. */
data class NoteTrace(val kind: NoteKind, val ref: String, val value: Double, val actor: FighterShot, val origin: TraceOrigin) : Trace

enum class NoteKind { BUFF, CHARGE, POWER, CONDITION_ON, CONDITION_OFF, KILL }

/**
 * What the card needs beyond the numbers: the hero's sheet, to lay a stat out by source, and each foe's roll with the
 * fight's level, to lay a monster's out the same way.
 */
class TraceOrigin(val hero: HeroModel, val foes: List<Foe>)

/** One source of a monster's stat, as the card prints it: what, the operation and the value. */
data class MonsterShare(val kind: MonsterShareKind, val ref: String, val op: Op, val value: Double)

enum class MonsterShareKind { BASE, RARITY, MODIFIER, MAP }

/**
 * A monster's stat taken apart by the roller's own fold (3.37.0): the template grown to the level, the rarity's lines,
 * each modifier, and the map's buffs over it.
 */
object MonsterBreakdown {
    fun explain(roller: MonsterRoller, rarityLines: (MonsterRarity) -> List<com.sperance.exileforge.rules.roll.MonsterEffect>,
                template: com.sperance.exileforge.rules.content.Monster?, monster: RolledMonster, level: Int, stat: String): List<MonsterShare> = buildList {
        template?.let { roller.stats(it, level)[stat] }?.takeIf { it != 0.0 }?.let { add(MonsterShare(MonsterShareKind.BASE, monster.code, Op.ADD, it)) }
        rarityLines(monster.rarity).filter { it.stat == stat }.forEach { add(MonsterShare(MonsterShareKind.RARITY, monster.rarity.name, it.op, it.value)) }
        monster.modifiers.forEach { mod -> mod.effects.filter { it.stat == stat }.forEach { add(MonsterShare(MonsterShareKind.MODIFIER, mod.code, it.op, it.value)) } }
        monster.mapBuffs.filter { it.stat == stat }.forEach { add(MonsterShare(MonsterShareKind.MAP, "", it.op, it.value)) }
    }
}

/** The log's shelves (3.37.0): blows, the ticks of ailments, and the notes of what happened without a blow. */
enum class LogKind {
    HITS, AILMENTS, EVENTS;

    companion object {
        val DEFAULT: Set<LogKind> = setOf(HITS, AILMENTS)
        fun of(event: CombatEvent): LogKind = when (event.action) {
            Action.TICK -> AILMENTS
            Action.NOTE -> EVENTS
            else -> HITS
        }
        fun parse(value: String?): Set<LogKind> = value?.split(',')?.mapNotNull { name -> entries.firstOrNull { it.name == name } }?.toSet() ?: DEFAULT
        fun write(kinds: Set<LogKind>): String = kinds.joinToString(",") { it.name }
    }
}
