package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.ActiveAilment
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.EffectKind
import com.sperance.exileforge.core.campaign.combat.FateBeat
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster

/**
 * How a line of the fight's log came about: the formula step by step, what each side stood on
 * at that moment and where the damage went. Every number is the fight's own, taken as it was computed — the card that
 * explains a line reads it, nothing is worked out again.
 */
sealed interface Trace

/** Бросок костей боя, что удача рода Предначертания может взять лучшим из двух (`FateReaction.lucky`). */
enum class RollKey { EVADE, BLOCK, CRIT, AILMENT, AVOID }

/**
 * One step of a hit's formula: its multiplier [value] (the first step is the base damage itself) and the stats of the
 * striker and of the target it reads, whose sources the card lays out.
 */
data class FactorTrace(val key: FactorKey, val value: Double, val attacker: List<String> = emptyList(), val target: List<String> = emptyList())

/**
 * Множители удара по порядку; [SEALS] - печати Стража Врат, [LAW] - Закон Владыки Разлома, [FATE] - множитель
 * Предначертания героя.
 */
enum class FactorKey { BASE, SPREAD, CRIT, NON_CRIT, DAMAGE, HIT, AGAINST, DOUBLE, VERSUS, FATE, DEFENCE, SHOCK, TAKEN, DECAY, EASED, SEALS, LAW, TOTAL }

/** One damage type of a hit: its [base], [raw] after the striker's multipliers, the [armour] and [resist] shares it lost, and what landed. */
data class TypeTrace(val type: DamageType, val base: Double, val raw: Double, val armour: Double, val resist: Double, val penetration: Double, val dealt: Double)

/** A line laid on a fighter in the fight, named by what laid it. */
data class LineSource(val kind: LineKind, val ref: String, val line: StatLine)

enum class LineKind { BUFF, CURSE, FLASK, CHARGE, CONDITION, LOW_LIFE, POWER, AURA, SKILL }

/** A fighter at the moment of a line: its sheet as it stood, what the fight laid on it, its ailments and pools. */
data class FighterShot(
    val side: Side,
    val index: Int,
    val stats: Map<String, Double>,
    val lines: List<LineSource>,
    val ailments: List<ActiveAilment>,
    val life: Double,
    val maxLife: Double,
    val shield: Double,
    val conditions: Set<Condition> = emptySet(),
)

/** Where a hit went: the barrier, the shield, the mana before life, the delayed share, life; and what the striker got back. */
data class Landing(
    val barrier: Double,
    val shield: Double,
    val mana: Double,
    val delayed: Double,
    val life: Double,
    val leech: Double,
    val onHit: Double,
    val recoup: Double,
    val culled: Boolean,
)

/**
 * A blow — a swing, a skill's hit, a spell, a reflection. [landing] is null for an evaded or blocked one; у такого [avoided] - оценка урона, которого он избежал: удар без бросков сквозь защиты цели, для разбора боя.
 */
data class HitTrace(
    val attacker: FighterShot,
    val target: FighterShot,
    val factors: List<FactorTrace>,
    val types: List<TypeTrace>,
    val landing: Landing?,
    val origin: TraceOrigin,
    val avoided: Double = 0.0,
    /** Урон удара, что прибавило Разложение цели - графа разбора боя. */
    val exposed: Double = 0.0,
) : Trace

/** A second of an ailment's damage: how strong, how long, who laid it and what grew it. */
data class TickTrace(
    val ailment: Ailment,
    val perSecond: Double,
    val left: Double,
    val duration: Double,
    val stacks: Int,
    val target: FighterShot,
    val factors: List<FactorTrace>,
    val origin: TraceOrigin,
    val striker: FighterShot? = null,
    /** Урон тика, что прибавило Разложение цели - графа разбора боя. */
    val exposed: Double = 0.0,
) : Trace

/** A draught, a buff, a curse, a heal: the lines it laid, for how long, what it healed and what made it stronger. */
data class EffectTrace(
    val kind: EffectKind,
    val source: String,
    val lines: List<StatLine>,
    val duration: Double,
    val healed: Double,
    val scale: Double,
    val actor: FighterShot,
    val origin: TraceOrigin,
) : Trace

/**
 * Something that happened without a blow: a buff or a charge gained, a power, a condition, what a kill brought. [flow] - разбор
 * итога здоровья за секунду у [NoteKind.LIFE_FLOW], у прочих null.
 */
data class NoteTrace(val kind: NoteKind, val ref: String, val value: Double, val actor: FighterShot, val origin: TraceOrigin, val flow: LifeFlow? = null) : Trace

/**
 * Срабатывание Предначертания героя: рычаг или правило строки дара [fate] сделал [beat] с числами [args] (порядок - KDoc
 * [FateBeat]) и предметом [ref] (недуг, умение - по [FateBeat.ref]).
 * [repeats] больше нуля - свёртка частого срабатывания за бой: сколько раз ещё оно было после первого, [args] - их итог.
 */
data class FateTrace(
    val fate: String,
    val beat: FateBeat,
    val args: List<Double>,
    val ref: String,
    val repeats: Int,
    val actor: FighterShot,
    val origin: TraceOrigin,
) : Trace

enum class NoteKind {
    BUFF,
    CHARGE,
    POWER,
    CONDITION_ON,
    CONDITION_OFF,
    KILL,
    TRAIT,

    /** Шаг фазы босса: ref - шаблон, value - порог здоровья в процентах. */
    PHASE,

    /** Тотем босса: ref - код тотема; встал и рассыпался. */
    TOTEM,
    TOTEM_FALL,

    /** Ярость стража: ref - ступень. */
    RAGE,

    /** Страж Врат: печать снята и печати вернулись - value - сколько стоит теперь. */
    SEAL_BROKEN,
    SEALS_RETURNED,

    /** «Запечатывание» Стража Врат: ref - код умения или флакона, value - на сколько секунд заперт. */
    SEAL_LOCK_SKILL,
    SEAL_LOCK_FLASK,

    /** Поглотитель эха украл дар: ref - код дара. */
    BOON_STOLEN,

    /** Закон Владыки Разлома вступил: ref - Закон. «Время вспять» вернуло урон: value - сколько здоровья. */
    LAW,
    LAW_REWOUND,

    /** The recovery shelf: a draught's total, a recoup's, healing spilled over a full bar. */
    RECOVER_FLASK,
    RECOVER_RECOUP,
    RECOVER_WASTE,

    /** Глоток, что только лечит, погас на полном здоровье, как в PoE: ref - флакон, value - сколько секунд ему оставалось. */
    RECOVER_HALT,

    /**
     * Итог здоровья героя за секунду (вместо строки регенерации): value - здоровья в секунду со знаком,
     * разбор - [NoteTrace.flow]. Недуги пишутся своими строками.
     */
    LIFE_FLOW,

    /**
     * Применение умения: ref - умение, value - секунды; прервано оглушением или заморозкой - [CAST_BROKEN] (value -
     * пауза до повтора); к концу применения у героя не хватило маны - [CAST_NO_MANA] (value - цена).
     */
    CAST,
    CAST_BROKEN,
    CAST_NO_MANA,

    /** Устрашение героя: враг устрашён на value секунд. */
    PROVOKED,

    /** Подрыв метки героя: ref - умение, value - урон подрыва по каждой цели. */
    MARK_BURST,
    ;

    val recovery: Boolean get() = this in RECOVERY
    private companion object {
        val RECOVERY = setOf(RECOVER_FLASK, RECOVER_RECOUP, RECOVER_WASTE, RECOVER_HALT, LIFE_FLOW)
    }
}

/**
 * What the card needs beyond the numbers: the hero's sheet, to lay a stat out by source, and each foe's roll with the
 * fight's level, to lay a monster's out the same way.
 */
class TraceOrigin(val hero: HeroModel, val foes: List<Foe>)

/** One source of a monster's stat, as the card prints it: what, the operation and the value. */
data class MonsterShare(val kind: MonsterShareKind, val ref: String, val op: Op, val value: Double)

/**
 * Источник доли стата монстра: шаблон, редкость, строка модификатора, сила стража ([MonsterShare.ref] - её множитель), сила от уровня
 * героя ([RolledMonster.heroScale]), карта.
 */
enum class MonsterShareKind { BASE, RARITY, MODIFIER, POWER, HERO, MAP }

/**
 * A monster's stat taken apart by the roller's own fold: the template grown to the level, the rarity's lines,
 * each modifier, and the map's buffs over it.
 */
object MonsterBreakdown {
    fun explain(
        roller: MonsterRoller,
        rarityLines: (MonsterRarity) -> List<com.sperance.exileforge.rules.roll.MonsterEffect>,
        template: com.sperance.exileforge.rules.content.Monster?,
        monster: RolledMonster,
        level: Int,
        stat: String,
    ): List<MonsterShare> = buildList {
        template?.let { roller.stats(it, level)[stat] }?.takeIf { it != 0.0 }?.let { add(MonsterShare(MonsterShareKind.BASE, monster.code.value, Op.ADD, it)) }
        rarityLines(monster.rarity).filter { it.stat == stat }.forEach { add(MonsterShare(MonsterShareKind.RARITY, monster.rarity.name, it.op, it.value)) }
        monster.modifiers.forEach { mod -> mod.effects.filter { it.stat == stat }.forEach { add(MonsterShare(MonsterShareKind.MODIFIER, mod.code.value, it.op, it.value)) } }
        val power = monster.power
        if (template != null && power != null) {
            roller.powerEffects(template, power).filter { it.stat == stat }.forEach { add(MonsterShare(MonsterShareKind.POWER, (1 + it.value / 100).toString(), it.op, it.value)) }
        }
        monster.heroScale?.let(roller::heroEffects)?.filter { it.stat == stat }?.forEach { add(MonsterShare(MonsterShareKind.HERO, "", it.op, it.value)) }
        monster.mapBuffs.filter { it.stat == stat }.forEach { add(MonsterShare(MonsterShareKind.MAP, "", it.op, it.value)) }
    }
}

/** The log's shelves: blows, the ticks of ailments, and the notes of what happened without a blow. */
enum class LogKind {
    HITS,
    AILMENTS,
    EVENTS,

    /** Recovery: draughts, life on kill, recoup, spilled healing and regeneration. */
    RECOVERY,

    /** Предначертание героя: срабатывания рычагов и строк дара ([FateTrace]). */
    FATE,

    ;

    companion object {
        val DEFAULT: Set<LogKind> = setOf(HITS, AILMENTS, RECOVERY, FATE)
        fun of(event: CombatEvent): LogKind = when {
            event.trace is FateTrace -> FATE
            event.action == Action.TICK -> AILMENTS
            event.action == Action.FLASK -> RECOVERY
            (event.trace as? NoteTrace)?.kind?.let { it.recovery || it == NoteKind.KILL } == true -> RECOVERY
            event.action == Action.NOTE -> EVENTS
            else -> HITS
        }

        /**
         * The saved shelves: the shown ones, and after a `;` every shelf the save knew of — a shelf added since
         * opens as [DEFAULT] has it, so a player who once changed the filter still sees a new default-on shelf.
         */
        fun parse(value: String?): Set<LogKind> {
            if (value == null) return DEFAULT
            val (shown, known) = value.split(';', limit = 2).let { it[0] to it.getOrNull(1) }
            fun names(list: String) = list.split(',').mapNotNull { name -> entries.firstOrNull { it.name == name } }.toSet()
            val seen = known?.let(::names).orEmpty()
            return names(shown) + DEFAULT.filterNot { it in seen }
        }
        fun write(kinds: Set<LogKind>): String = kinds.joinToString(",") { it.name } + ";" + entries.joinToString(",") { it.name }
    }
}
