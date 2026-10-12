package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.RegenPool
import com.sperance.exileforge.core.character.HeroSheet
import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.Grant
import com.sperance.exileforge.rules.sheet.HeroBody
import com.sperance.exileforge.rules.sheet.Might
import com.sperance.exileforge.rules.sheet.MightReading
import com.sperance.exileforge.rules.sheet.PetFighter
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatBreakdown
import com.sperance.exileforge.rules.sheet.StatOperation
import com.sperance.exileforge.rules.sheet.StatSource
import kotlin.math.abs

/**
 * Пассивное умение героя в бою (4.6.4): код [skill], уровень [level], на котором оно действует, доля маны [reserve], что держит аура,
 * его строки [lines] на листе (аура - на силе `STOCK_AURA_EFFECT`) и ждёт ли оно низкого здоровья ([lowLife]).
 */
data class PassiveSource(val skill: String, val level: Int, val reserve: Double, val lines: List<StatLine>, val lowLife: Boolean)

/**
 * Что бой читает вместе с характеристикой (4.6.4, как [Combatant]): к цифре [stat] он прибавляет часть тела из [from] - общее
 * сопротивление стихиям к каждой стихии, долю максимума к плоской регенерации и к здоровью за убийство, проклятие в ответ
 * второй строкой (4.6.5). [creates] - строка сводки появляется и без своей характеристики (общее сопротивление даёт строки
 * стихий, доля максимума - строку регенерации); иначе прибавка ложится лишь на уже бывшую строку. Часть строки сводки из
 * контента (`StatDef.partOf`) читается здесь же - так строка и её разбор сходятся с боем.
 */
enum class FightReading(val stat: String, val from: String, val creates: Boolean) {
    FIRE(CoreStat.RESIST_FIRE.code, CoreStat.RESIST_ALL.code, true),
    COLD(CoreStat.RESIST_COLD.code, CoreStat.RESIST_ALL.code, true),
    LIGHTNING(CoreStat.RESIST_LIGHTNING.code, CoreStat.RESIST_ALL.code, true),
    LIFE_REGEN(RegenPool.LIFE.flatStat, RegenPool.LIFE.shareStat, true) {
        override fun adds(body: Combatant): Double = RegenPool.LIFE.fromShare(body)
    },
    SHIELD_REGEN(RegenPool.SHIELD.flatStat, RegenPool.SHIELD.shareStat, true) {
        override fun adds(body: Combatant): Double = RegenPool.SHIELD.fromShare(body)
    },
    LIFE_ON_KILL(CoreStat.HEALTH_ON_KILL.code, CoreStat.HEALTH_ON_KILL_PERCENT.code, true) {
        override fun adds(body: Combatant): Double = body.maxLife * body.lifeOnKillShare
    },
    CURSE_WHEN_HIT(CoreStat.CURSE_ON_HIT.code, CoreStat.CURSE_WHEN_HIT.code, true),
    ;

    /** Что бой прибавляет к цифре [stat] тела [body]: по умолчанию - саму характеристику [from]. */
    open fun adds(body: Combatant): Double = body[from]
}

/**
 * Цифры сводки героя (4.6.4): [values] - что показывает строка (с частями, что бой читает вместе с ней, [FightReading]),
 * [skilled] - строки, что сдвинули пассивные умения (значок ауры), [fated] - Предначертание (✦).
 */
data class StatFigures(
    val values: Map<String, Double>,
    val skilled: Set<String> = emptySet(),
    val fated: Set<String> = emptySet(),
    /** Характеристики, из которых прочтены цифры: по ним бой режет потолки (`StatLimits`). */
    val stats: Map<String, Double> = values,
)

/**
 * Лист героя, как его читает бой (4.6.4): лист правил с пассивными умениями (ауры - на силе `STOCK_AURA_EFFECT`, с резервом маны) и
 * преобразованиями Предначертания - боевым листом правил ([HeroBody], 4.9.0: тот же свод, что у тела героя в заходе без карты и у Мощи сервера). Одна точка для сводки, разбора
 * характеристики и сравнений вещи и узла дерева: их цифры не расходятся с боем и между собой.
 */
class CombatSheet(private val index: ContentIndex, private val model: SheetModel, level: Int, val kit: Loadout, pet: Pet? = null) {
    /** Боевой лист героя правилами (4.9.0): свод листа с пассивными умениями - тот же, из которого сервер считает Мощь. */
    private val hero = HeroBody(index, model.fold, level, kit.heroKit, pet)

    /** Тело героя без пассивных умений: с него читаются их уровни и сила аур. */
    private val bare: Combatant by lazy { Combatant(hero.bare, level, index.campaign.combat) }

    /** Тело героя между боями: лист с пассивными умениями. */
    val body: Combatant by lazy { Combatant(hero.stats, level, index.campaign.combat) }

    /** Характеристики боя. */
    val stats: Map<String, Double> get() = body.stats

    /** Предначертание героя на силе его листа, с действующими судьбоносными строками. */
    val fate: FateEffects get() = kit.fate.effects

    /** Сколько процентов максимума маны держат ауры - как её держит бой (`Battle.manaCap`). */
    val reserved: Double by lazy { kit.reserved(body) }

    /** Пассивные умения героя с их уровнем, резервом и строками - уровень и сила аур читаются с листа без них, как в бою. */
    val passives: List<PassiveSource> by lazy {
        kit.passiveSources(bare).map { (skill, lines) -> PassiveSource(skill.skill.code, skill.level(bare), kit.reserve(skill, bare), lines, skill.skill.lowLife) }
    }

    private val steady: List<PassiveSource> get() = passives.filterNot { it.lowLife }

    /** Разбивка листа со строками пассивных умений среди его операций, каждая со своим умением. */
    private val explainer by lazy {
        model.explainer(steady.flatMap { passive -> passive.lines.filter(model::folds).map { StatOperation(it.stat, it.op, it.value, source = StatSource(SourceKind.SKILL, passive.skill)) } })
    }

    /** «Больше» процент-стата от умений ([SheetModel.folds]): бой кладёт его поверх свода - сдвигом своего умения, по порядку строк. */
    private val scaled: Map<String, List<Shift>> by lazy {
        val out = LinkedHashMap<String, MutableList<Shift>>()
        var lines = steady.flatMap { passive -> passive.lines.filter(model::folds) }
        var before = model.with(lines)
        steady.forEach { passive ->
            val own = passive.lines.filterNot(model::folds).ifEmpty { return@forEach }
            lines = lines + own
            val after = model.with(lines)
            moved(before, after).forEach { (stat, delta) -> out.getOrPut(stat) { mutableListOf() } += Shift(StatSource(SourceKind.SKILL, passive.skill), delta) }
            before = after
        }
        out
    }

    /**
     * Мощь героя (4.8.0) по телу боя без карты: печать листа героя и «было» сравнения вещи. С 4.9.0 - [Might.hero] правил: натиск с
     * умениями и боевым питомцем, одна функция с сервером.
     */
    val might: MightReading by lazy { Might.hero(index, hero) }

    /** Урон в секунду сверх листа - умения и боевой питомец (4.9.0): с ним бой меряет Мощь героя под эффектами карты. */
    val addedDps: Double get() = hero.addedDps

    /** Цифры сводки без отметок: что сравнивают вещь и узел дерева. */
    val values: Map<String, Double> by lazy { read(body) }

    /** Цифры сводки с отметками ауры и Предначертания. */
    val figures: StatFigures by lazy { figures(stats) }

    /**
     * Цифры сводки над листом боя [stats] - своим или под эффектами карты: что бой читает вместе с характеристикой ([FightReading]),
     * восстановление строк-долей и отметки.
     */
    fun figures(stats: Map<String, Double>): StatFigures {
        val fighter = Combatant(stats, body.level, body.rules)
        val values = read(fighter)
        return StatFigures(values, marked(skilled, values.keys), marked(fated, values.keys), stats)
    }

    private fun read(fighter: Combatant): Map<String, Double> {
        val values = fighter.stats.toMutableMap()
        FightReading.entries.forEach { reading ->
            val add = reading.adds(fighter)
            if (reading.stat in fighter.stats || (reading.creates && add != 0.0)) values[reading.stat] = (fighter.stats[reading.stat] ?: 0.0) + add
        }
        return values
    }

    /** Прямые цели строк пассивных умений и всё, что от них сдвинулось. */
    private val skilled: Set<String> by lazy {
        val direct = steady.flatMap { it.lines }.flatMap { StatSpread.targets(it.stat, it.op) }
        direct.toSet() + moved(read(bare), read(body)).keys
    }

    /** Прямые цели строк дара и шаги его преобразований листа. */
    private val fated: Set<String> by lazy {
        val direct = model.operations.filter { it.source?.kind == SourceKind.FATE }.flatMap { StatSpread.targets(it.stat, it.op) }
        direct.toSet() + model.reshapes(steady.flatMap { it.lines }).keys
    }

    /** Отмеченные [marks] с цифрами, что бой читает из них ([FightReading.from]), среди строк сводки [shown]. */
    private fun marked(marks: Set<String>, shown: Set<String>): Set<String> = (marks + FightReading.entries.filter { it.from in marks }.map { it.stat }).filterTo(HashSet()) { it in shown }

    /**
     * Из чего сложилась цифра сводки [stat]: лист по источникам с пассивными умениями и шагами Предначертания, «больше» процент-стата
     * от умений и что бой читает вместе с ней ([FightReading], источник [SourceKind.FIGHT]) - итог равен [figures].
     */
    fun explain(stat: String): StatBreakdown {
        val fight = FightReading.entries.filter { it.stat == stat }.map { Shift(StatSource(SourceKind.FIGHT, it.from), it.adds(body), it.from) }.filter { it.delta != 0.0 }
        return explainer.explain(stat).shifted(scaled[stat].orEmpty() + fight)
    }

    fun grants(stat: String): List<Grant> = explainer.grants(stat)

    fun holders(power: String): List<StatSource> = explainer.holders(power)

    /** Бонусы низкого здоровья на цифре [stat], каждый с тем, что прибавит, пока здоровья мало: в цифру сводки они не входят. */
    fun lowLife(stat: String): List<Pair<PassiveSource, Double>> {
        val steadyLines = steady.flatMap { it.lines }
        val now = model.with(steadyLines)[stat] ?: 0.0
        return passives.filter { passive -> passive.lowLife && passive.lines.any { stat in StatSpread.targets(it.stat, it.op) } }
            .map { passive -> passive to (model.with(steadyLines + passive.lines)[stat] ?: 0.0) - now }
    }

    override fun equals(other: Any?): Boolean = other is CombatSheet && other.stats == stats && other.passives == passives && other.fate.effects == fate.effects
    override fun hashCode(): Int = stats.hashCode()

    companion object {
        /** Сдвиг меньше этого - не сдвиг: десятые листа. */
        private const val MOVED = 0.05

        private fun moved(before: Map<String, Double>, after: Map<String, Double>): Map<String, Double> = (before.keys + after.keys)
            .associateWith { (after[it] ?: 0.0) - (before[it] ?: 0.0) }.filterValues { abs(it) >= MOVED }

        /**
         * Лист боя героя: его лист [sheet] (без модели - null), уровень [level], класс [heroClass], умения [skills] и Предначертание
         * [fate] с надетым [equipped] (судьбоносные строки) - те же рычаги, что берёт заход (`ExpeditionActions.gear`), без фляг;
         * [pets] (4.9.0) - активные питомцы: боевой бьёт в Мощи героя.
         */
        fun of(index: ContentIndex, sheet: HeroSheet, level: Int, heroClass: String, skills: HeroSkills, fate: String?, equipped: Collection<ItemInstance>, pets: List<Pet> = emptyList()): CombatSheet? {
            val model = sheet.model ?: return null
            val kit = Loadout.of(skills, index, heroClass, emptyList(), sheet.stats, index.powers, index.rules.charges, FateKit.of(index, fate, sheet.stats, equipped))
            return CombatSheet(index, model, level, kit, PetFighter.combat(index, pets))
        }
    }
}
