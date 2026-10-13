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
import com.sperance.exileforge.rules.content.RecoveryPool
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.WorldKind
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.Grant
import com.sperance.exileforge.rules.sheet.HeroBody
import com.sperance.exileforge.rules.sheet.Might
import com.sperance.exileforge.rules.sheet.MightDefense
import com.sperance.exileforge.rules.sheet.MightExtras
import com.sperance.exileforge.rules.sheet.MightReading
import com.sperance.exileforge.rules.sheet.Recovery
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

    /**
     * Регенерация маны (4.11.x): строка сводки - маны в секунду, как её читает бой ([Recovery.manaRegen]): к плоской - доля максимума
     * по правилу боя, её «увеличение» `STOCK_MANA_REGEN` и доля восстановления.
     */
    MANA_REGEN(CoreStat.MANA_REGEN_FLAT.code, CoreStat.MANA_REGEN.code, true) {
        override fun adds(body: Combatant): Double = Recovery.manaRegen(body, body.rules.mana) - body[CoreStat.MANA_REGEN_FLAT.code]
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
class CombatSheet(private val index: ContentIndex, private val model: SheetModel, level: Int, val kit: Loadout) {
    /** Боевой лист героя правилами (4.9.0): свод листа с пассивными умениями - тот же, из которого сервер считает Мощь. */
    private val hero = HeroBody(index, model.fold, level, kit.heroKit, kit.flasks.mapNotNull { it?.sheet })

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
     * умениями, одна функция с сервером.
     */
    val might: MightReading by lazy { Might.hero(index, hero) }

    /** Что Мощь берёт сверх листа (4.9.x): урон умений, резерв маны аурами, фляги - с ним бой меряет Мощь героя под эффектами карты. */
    val mightExtras: MightExtras get() = hero.extras

    /** Разбор стойкости Мощи героя (4.9.x) для листа героя: запас, мана перед здоровьем, уклонение, получаемый урон, пополнение. */
    val mightDefense: MightDefense by lazy { Might.defense(index, hero) }

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
        // Прибавки к добыче (4.10.x): характеристика листа и силы уникалок против всех монстров - тот же `Powers.loot`, что катит сервер
        LOOT.forEach { kind ->
            val stat = kind.stock!!.code
            index.powers.loot(fighter.stats, kind).takeIf { it != 0.0 || stat in fighter.stats }?.let { values[stat] = it }
        }
        // Регенерация здоровья (4.11.0) - как её читает бой: «меньше регенерации», запрет и порог листа (`Recovery.share` правил)
        values[LIFE_REGEN]?.let { values[LIFE_REGEN] = it * lifeShare(fighter.stats) }
        return values
    }

    /** Доля регенерации здоровья листа [stats] без недугов (4.11.0): одна функция правил с боем. */
    private fun lifeShare(stats: Map<String, Double>): Double = Recovery.share({ stats[it] ?: 0.0 }, RecoveryPool.LIFE)

    /**
     * Сдвиг регенерации здоровья долей [lifeShare] (4.11.0) поверх разбора с итогом [total]: под картой ([layer]) - доля листа под
     * её сдвигами; null - доля полная.
     */
    private fun lifeCurb(stat: String, total: Double, layer: Map<String, List<Shift>>): Shift? {
        if (stat != LIFE_REGEN) return null
        val moved = layer.mapValues { (_, shifts) -> shifts.sumOf { it.delta } }
        val share = lifeShare(stats + moved.mapValues { (key, delta) -> (stats[key] ?: 0.0) + delta })
        return Shift(StatSource(SourceKind.FIGHT, CoreStat.LIFE_REGEN_LESS.code), total * (share - 1)).takeIf { abs(it.delta) >= MOVED }
    }

    /** Силы уникалок в прибавке добычи [stat] (4.10.x): каждая сила против всех монстров - своим сдвигом, как её считает [read]. */
    private fun lootPowers(stat: String): List<Shift> = LOOT.filter { it.stock?.code == stat }.flatMap { kind ->
        index.powers.worldParts(stats, kind).filter { it.on(null) }.map { Shift(StatSource(SourceKind.POWER, it.stat), it.value) }
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
     *
     * 4.10.x: [layer] - сдвиги листа эффектами карты, атласа, гильдии и зелья по характеристике (`MapEffects.heroShifts`): свои
     * сдвиги цифры ложатся как есть, а сдвиги того, что бой читает вместе с ней (общее сопротивление стихиям, доля регенерации,
     * максимум здоровья), - на её чтение: каждый источник своим сдвигом, так итог равен цифре [figures] листа под картой.
     */
    fun explain(stat: String, layer: Map<String, List<Shift>> = emptyMap()): StatBreakdown {
        val readings = FightReading.entries.filter { it.stat == stat }
        val fight = readings.map { Shift(StatSource(SourceKind.FIGHT, it.from), it.adds(body), it.from) }.filter { it.delta != 0.0 }
        val raw = explainer.explain(stat).shifted(scaled[stat].orEmpty() + fight + lootPowers(stat) + layer[stat].orEmpty() + readings.flatMap { read(it, layer) })
        return lifeCurb(stat, raw.total, layer)?.let { raw.shifted(listOf(it)) } ?: raw
    }

    /**
     * Что сдвиги [layer] сделали с чтением [reading]: каждый источник положен на лист один, а что они делают только вместе -
     * последнему, как в `MapEffects.heroShifts`; сумма равна чтению листа под всеми сдвигами без своего.
     */
    private fun read(reading: FightReading, layer: Map<String, List<Shift>>): List<Shift> {
        if (layer.isEmpty()) return emptyList()
        val own = reading.adds(body)
        fun under(moves: List<Pair<String, Double>>): Double = reading.adds(Combatant(moves.fold(stats) { sheet, (key, delta) -> sheet + (key to (sheet[key] ?: 0.0) + delta) }, body.level, body.rules)) - own
        val bySource = LinkedHashMap<StatSource, MutableList<Pair<String, Double>>>()
        layer.forEach { (key, shifts) -> if (key != reading.stat) shifts.forEach { bySource.getOrPut(it.source) { mutableListOf() } += key to it.delta } }
        val parts = bySource.mapNotNull { (source, moves) -> under(moves).takeIf { abs(it) >= MOVED }?.let { Shift(source, it, reading.from) } }.toMutableList()
        val rest = under(bySource.values.flatten()) - parts.sumOf { it.delta }
        if (abs(rest) < MOVED) return parts
        if (parts.isEmpty()) return listOf(Shift(bySource.keys.last(), rest, reading.from))
        parts[parts.lastIndex] = parts.last().let { it.copy(delta = it.delta + rest) }
        return parts
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

        /** Плоская регенерация здоровья: её цифра сводки - с долей [Recovery.share]. */
        private val LIFE_REGEN = RegenPool.LIFE.flatStat

        /** Виды небоевых сил с характеристикой листа: их прибавка к добыче в сводке - вместе с силами. */
        private val LOOT = WorldKind.entries.filter { it.stock != null }

        private fun moved(before: Map<String, Double>, after: Map<String, Double>): Map<String, Double> = (before.keys + after.keys)
            .associateWith { (after[it] ?: 0.0) - (before[it] ?: 0.0) }.filterValues { abs(it) >= MOVED }

        /**
         * Лист боя героя: его лист [sheet] (без модели - null), уровень [level], класс [heroClass], умения [skills] и Предначертание
         * [fate] с надетым [equipped] (судьбоносные строки) - те же рычаги, что берёт заход (`ExpeditionActions.gear`), без фляг.
         */
        fun of(index: ContentIndex, sheet: HeroSheet, level: Int, heroClass: String, skills: HeroSkills, fate: String?, equipped: Collection<ItemInstance>): CombatSheet? {
            val model = sheet.model ?: return null
            val kit = Loadout.of(skills, index, heroClass, emptyList(), sheet.stats, index.powers, index.rules.charges, FateKit.of(index, fate, sheet.stats, equipped))
            return CombatSheet(index, model, level, kit)
        }
    }
}
