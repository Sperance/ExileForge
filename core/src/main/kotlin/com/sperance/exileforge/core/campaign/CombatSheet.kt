package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.RegenPool
import com.sperance.exileforge.core.character.HeroSheet
import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.ChargeRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.rules.content.WorldKind
import com.sperance.exileforge.rules.content.WorldPart
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.Grant
import com.sperance.exileforge.rules.sheet.HeroBody
import com.sperance.exileforge.rules.sheet.Might
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
 * Пассивное умение героя в бою: код [skill], уровень [level], на котором оно действует, доля своего запаса [reserve], что держит аура,
 * его строки [lines] на листе (аура - на силе `STOCK_AURA_EFFECT`) и ждёт ли оно низкого здоровья ([lowLife]).
 */
data class PassiveSource(val skill: String, val level: Int, val reserve: Double, val lines: List<StatLine>, val lowLife: Boolean)

/**
 * Что бой читает вместе с характеристикой (как [Combatant]): к цифре [stat] он прибавляет часть тела из [from] или шаги [steps] -
 * так строка сводки равна тому, что даёт бой, а разбор называет каждую часть. [creates] - строка сводки появляется и без своей
 * характеристики; иначе прибавка ложится лишь на уже бывшую строку.
 */
interface SheetReading {
    val stat: String
    val from: String
    val creates: Boolean

    /** Мера строки-потока ([FlowMeasure]): итог и вклады её разбора печатаются с ней; не поток - null. */
    val measure: FlowMeasure? get() = null

    /** Шаги чтения тела [body] поверх цифры [stat]; null - чтение одной прибавкой характеристики [from]. */
    fun steps(body: Combatant): List<ReadingStep>? = null

    /** Что бой прибавляет к цифре [stat] тела [body]: сумма шагов или сама характеристика [from]. */
    fun adds(body: Combatant): Double = steps(body)?.sumOf { it.delta } ?: body[from]

    /** Сдвиги разбора: шаг - своим сдвигом (источник [ReadingPart.ref], касание - его характеристика), иначе одна прибавка [from]. */
    fun shifts(body: Combatant): List<Shift> = steps(body)?.map { Shift(StatSource(SourceKind.FIGHT, it.part.ref), it.delta, it.part.stat) }
        ?: listOf(Shift(StatSource(SourceKind.FIGHT, from), body[from], from)).filter { it.delta != 0.0 }
}

/**
 * Чтения боя без правил движка: общее сопротивление стихиям к каждой стихии; восстановление - потоком правил по шагам: регенерация
 * здоровья и маны (`Recovery.lifeFlow`/`manaFlow`), щита и здоровье за убийство. Общее сопротивление даёт строки стихий, доля
 * максимума - строку регенерации ([creates]). Часть строки сводки из контента (`StatDef.partOf`) читается здесь же - так строка и её
 * разбор сходятся с боем.
 */
enum class FightReading(override val stat: String, override val from: String, override val creates: Boolean, override val measure: FlowMeasure? = null) : SheetReading {
    FIRE(CoreStat.RESIST_FIRE.code, CoreStat.ALL_ELEMENTAL_RESISTANCES.code, true),
    COLD(CoreStat.RESIST_COLD.code, CoreStat.ALL_ELEMENTAL_RESISTANCES.code, true),
    LIGHTNING(CoreStat.RESIST_LIGHTNING.code, CoreStat.ALL_ELEMENTAL_RESISTANCES.code, true),

    /**
     * Регенерация здоровья: строка сводки - итог потока здоровья в секунду, как его читает бой ([Recovery.lifeFlow]): плоская,
     * доля максимума, общий множитель правил и доля восстановления, регенерация из маны, скорость восстановления, потери и деген.
     */
    LIFE_REGEN(RegenPool.LIFE.flatStat, RegenPool.LIFE.shareStat, true, FlowMeasure.PER_SECOND) {
        /** Отрицательная сумма - потеря: множитель и доля её не режут. */
        override fun steps(body: Combatant): List<ReadingStep> {
            val flow = Recovery.lifeFlow(body, body.rules)
            return ReadingSteps(body[stat]).apply {
                to(ReadingPart.LIFE_OF_MAX, flow.raw)
                if (flow.raw > 0) {
                    to(ReadingPart.LIFE_RULE, flow.raw * flow.rule, flow.rule)
                    to(ReadingPart.LIFE_SHARE, flow.regen, flow.share)
                }
                to(ReadingPart.FROM_MANA, flow.regen - flow.loss + flow.mana)
                to(ReadingPart.RATE, flow.gain - flow.loss, flow.rate.takeIf { flow.loss == 0.0 })
                to(ReadingPart.DRAIN, flow.total)
            }.steps
        }
    },

    /** Регенерация щита: плоская и доля максимума на скорость восстановления - как в бою (`EnergyShield.recovered`). */
    SHIELD_REGEN(RegenPool.SHIELD.flatStat, RegenPool.SHIELD.shareStat, true, FlowMeasure.PER_SECOND) {
        override fun steps(body: Combatant): List<ReadingStep> = ReadingSteps(body[stat]).apply {
            val raw = RegenPool.SHIELD.flat(body) + RegenPool.SHIELD.fromShare(body)
            to(ReadingPart.SHIELD_OF_MAX, raw)
            to(ReadingPart.RATE, raw * body.recoveryRate, body.recoveryRate)
        }.steps
    },

    /**
     * Регенерация маны: строка сводки - итог потока маны в секунду ([Recovery.manaFlow]): к плоской - доля максимума по
     * правилу боя, её «увеличение» `STOCK_MANA_REGEN` и доля восстановления. Не положительная база останавливает регенерацию
     * шагом [ReadingPart.MANA_FLOOR], не положительное «увеличение» - своим шагом в ноль: цепочка доходит до нуля, маны не отнимает.
     */
    MANA_REGEN(CoreStat.MANA_REGEN_FLAT.code, CoreStat.MANA_REGEN.code, true, FlowMeasure.PER_SECOND) {
        override fun steps(body: Combatant): List<ReadingStep> {
            val flow = Recovery.manaFlow(body, body.rules.mana)
            return ReadingSteps(body[stat]).apply {
                to(ReadingPart.MANA_OF_MAX, flow.raw)
                if (flow.raw > 0) {
                    val increase = flow.increase.coerceAtLeast(0.0)
                    to(ReadingPart.MANA_INCREASE, flow.raw * increase, increase)
                    to(ReadingPart.MANA_SHARE, flow.total, flow.share)
                } else {
                    to(ReadingPart.MANA_FLOOR, flow.total)
                }
            }.steps
        }
    },

    /** Скорость атаки: цифра листа в пределах правил на темп боя (`combat.tempo`) - так часто бой и замахивается (`Fighter.slow`). */
    ATTACK_SPEED(CoreStat.ATTACK_SPEED.code, CoreStat.ATTACK_SPEED.code, false) {
        override fun steps(body: Combatant): List<ReadingStep> = ReadingSteps(body[stat]).apply {
            to(ReadingPart.TEMPO, body.attackSpeed * body.rules.tempo, body.rules.tempo)
        }.steps
    },

    /** Здоровье за убийство: плоское и доля максимума на скорость восстановления - как в бою (`Battle.slain`). */
    LIFE_ON_KILL(CoreStat.HEALTH_ON_KILL.code, CoreStat.HEALTH_ON_KILL_PERCENT.code, true, FlowMeasure.PER_KILL) {
        override fun steps(body: Combatant): List<ReadingStep> = ReadingSteps(body[stat]).apply {
            val raw = body.lifeOnKill + body.maxLife * body.lifeOnKillShare
            to(ReadingPart.KILL_OF_MAX, raw)
            to(ReadingPart.RATE, raw * body.recoveryRate, body.recoveryRate)
        }.steps
    },

    /** Мана за убийство: бой отдаёт её как есть (`Battle.slain`) - шагов нет, чтение несёт только меру строки. */
    MANA_ON_KILL(CoreStat.MANA_ON_KILL.code, CoreStat.MANA_ON_KILL.code, false, FlowMeasure.PER_KILL) {
        override fun steps(body: Combatant): List<ReadingStep> = emptyList()
    },

    /** Энергощит за убийство: бой отдаёт его как есть, до максимума щита (`Battle.slain`) - шагов нет, только мера строки. */
    SHIELD_ON_KILL(CoreStat.SHIELD_ON_KILL.code, CoreStat.SHIELD_ON_KILL.code, false, FlowMeasure.PER_KILL) {
        override fun steps(body: Combatant): List<ReadingStep> = emptyList()
    },
    ;

    companion object {
        private val measures: Map<String, FlowMeasure> = entries.mapNotNull { reading -> reading.measure?.let { reading.stat to it } }.toMap()

        /** Мера строки сводки [stat], если она поток; иначе null. */
        fun measure(stat: String): FlowMeasure? = measures[stat]
    }
}

/**
 * Мера строки-потока сводки: в секунду или за убийство. [key] - ключ словаря числа с мерой («+23.9/с», «+12 за убийство»),
 * [shareKey] - доли максимума запаса в этой мере («0.7% максимума в секунду»).
 */
enum class FlowMeasure(val key: String, val shareKey: String) {
    PER_SECOND("unit.per_second", "stat.flow.share_per_second"),
    PER_KILL("unit.per_kill", "stat.flow.share_per_kill"),
}

/**
 * Максимум зарядов вида [kind] - как его держит бой ([ChargeRules.max]): база правил `charges.maximum` и прибавка листа, не меньше
 * нуля. Строка сводки есть у каждого героя - база правил есть всегда; разбор - «База правил» и источники прибавки.
 */
class ChargeReading private constructor(private val kind: ChargeKind, private val rules: ChargeRules, override val stat: String) : SheetReading {
    override val from: String get() = stat
    override val creates: Boolean get() = true

    override fun steps(body: Combatant): List<ReadingStep> = ReadingSteps(body[stat]).apply { to(ReadingPart.CHARGE_BASE, rules.max(kind, body.stats).toDouble()) }.steps

    companion object {
        /** Чтения максимумов всех видов зарядов правил [rules]. */
        fun of(rules: ChargeRules): List<ChargeReading> = rules.kinds.map { (kind, rule) -> ChargeReading(kind, rules, rule.max) }
    }
}

/**
 * Часть чтения боя - строка разбора восстановления со своим именем (`stat.flow.<часть>`) и характеристикой [stat],
 * что её задаёт (касание открывает её разбор; у правила боя её нет). В разборе часть - сдвиг чтения боя с источником [ref].
 * [ofMax] - характеристика части - доля максимума запаса в процентах: разбор потока печатает её числом рядом с вкладом.
 */
enum class ReadingPart(val stat: String?, val ofMax: Boolean = false) {
    LIFE_OF_MAX(CoreStat.LIFE_REGEN_PERCENT.code, ofMax = true),
    LIFE_RULE(null),
    LIFE_SHARE(CoreStat.LIFE_REGEN_LESS.code),
    FROM_MANA(CoreStat.MANA_REGEN_TO_LIFE.code),
    RATE(CoreStat.RECOVERY_RATE.code),
    DRAIN(CoreStat.LIFE_DEGEN_PERCENT.code),
    MANA_OF_MAX(CoreStat.MANA.code),
    MANA_INCREASE(CoreStat.MANA_REGEN.code),
    MANA_SHARE(CoreStat.MANA_REGEN_LESS.code),

    /** Регенерация маны не ниже нуля: не положительная база её останавливает. */
    MANA_FLOOR(null),

    SHIELD_OF_MAX(CoreStat.ENERGY_REGEN_PERCENT.code, ofMax = true),
    KILL_OF_MAX(CoreStat.HEALTH_ON_KILL_PERCENT.code, ofMax = true),
    CHARGE_BASE(null),

    /** Темп боя правил (`combat.tempo`): множитель скорости всех действий в бою. */
    TEMPO(null),
    ;

    /** Источник сдвига части в разборе ([SourceKind.FIGHT]). */
    val ref: String get() = PREFIX + name

    companion object {
        private const val PREFIX = "flow."

        /** Часть по источнику сдвига разбора; не часть - null. */
        fun of(ref: String): ReadingPart? = if (ref.startsWith(PREFIX)) entries.firstOrNull { it.ref == ref } else null
    }
}

/**
 * Шаг чтения боя: часть [part], что она меняет в цифре ([delta]), и множитель [factor] прежнего итога, если шаг - множитель
 * (правило боя, доля, скорость восстановления); прибавка - null.
 */
data class ReadingStep(val part: ReadingPart, val delta: Double, val factor: Double? = null)

/** Сборщик шагов чтения от цифры [start]: каждый шаг - разница его итога с прежним; пустой шаг не пишется. */
private class ReadingSteps(start: Double) {
    private var at = start
    val steps = mutableListOf<ReadingStep>()

    fun to(part: ReadingPart, value: Double, factor: Double? = null) {
        val delta = value - at
        if (abs(delta) > EMPTY) steps += ReadingStep(part, delta, factor)
        at = value
    }

    private companion object {
        /** Шаг меньше этого - след округления, не часть. */
        const val EMPTY = 1e-9
    }
}

/**
 * Цифры сводки героя: [values] - что показывает строка (с частями, что бой читает вместе с ней, [SheetReading]),
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
 * Лист героя, как его читает бой: лист правил с пассивными умениями (ауры - на силе `STOCK_AURA_EFFECT`, с резервом маны) и
 * преобразованиями Предначертания - боевым листом правил ([HeroBody], тот же свод, что у тела героя в заходе без карты и у Мощи сервера). Одна точка для сводки, разбора
 * характеристики и сравнений вещи и узла дерева: их цифры не расходятся с боем и между собой.
 */
class CombatSheet(private val index: ContentIndex, private val model: SheetModel, level: Int, val kit: Loadout) {
    /** Боевой лист героя правилами: свод листа с пассивными умениями - тот же, из которого сервер считает Мощь. */
    private val hero = HeroBody(index, model.fold, level, kit.heroKit, kit.flasks.mapNotNull { it?.sheet })

    /** Тело героя без пассивных умений: с него читаются их уровни и сила аур. */
    private val bare: Combatant by lazy { Combatant(hero.bare, level, index.campaign.combat) }

    /** Что бой читает вместе с цифрами сводки: чтения без правил и максимумы зарядов правил. */
    private val readings: List<SheetReading> = FightReading.entries + ChargeReading.of(index.rules.charges)

    /** Тело героя между боями: лист с пассивными умениями. */
    val body: Combatant by lazy { Combatant(hero.stats, level, index.campaign.combat) }

    /** Характеристики боя. */
    val stats: Map<String, Double> get() = body.stats

    /** Предначертание героя на силе его листа, с действующими судьбоносными строками. */
    val fate: FateEffects get() = kit.fate.effects

    /** Резерв запаса [pool] аурами - как его держит бой (`Battle.manaCap`, `Battle.lifeReserved`). */
    fun reserve(pool: ReservePool): PoolReserve = kit.reserve(pool, body)

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
     * Мощь героя по телу боя без карты: печать листа героя и «было» сравнения вещи - [Might.hero] правил: натиск с
     * умениями, одна функция с сервером.
     */
    val might: MightReading by lazy { Might.hero(index, hero) }

    /** Что Мощь берёт сверх листа: урон умений, резерв маны аурами, фляги - с ним бой меряет Мощь героя под эффектами карты. */
    val mightExtras: MightExtras get() = hero.extras

    /** Цифры сводки без отметок: что сравнивают вещь и узел дерева. */
    val values: Map<String, Double> by lazy { read(body) }

    /** Цифры сводки с отметками ауры и Предначертания. */
    val figures: StatFigures by lazy { figures(stats) }

    /**
     * Цифры сводки над листом боя [stats] - своим или под эффектами карты: что бой читает вместе с характеристикой ([SheetReading]),
     * восстановление строк-долей и отметки.
     */
    fun figures(stats: Map<String, Double>): StatFigures {
        val fighter = Combatant(stats, body.level, body.rules)
        val values = read(fighter)
        return StatFigures(values, marked(skilled, values.keys), marked(fated, values.keys), stats)
    }

    /** Прибавки к добыче - характеристика листа и силы уникалок против всех монстров: тот же `Powers.loot`, что катит сервер. */
    private fun read(fighter: Combatant): Map<String, Double> {
        val values = fighter.stats.toMutableMap()
        readings.forEach { reading ->
            val add = reading.adds(fighter)
            if (reading.stat in fighter.stats || (reading.creates && add != 0.0)) values[reading.stat] = (fighter.stats[reading.stat] ?: 0.0) + add
        }
        LOOT.forEach { kind ->
            val stat = kind.stock!!.code
            index.powers.loot(fighter.stats, kind).takeIf { it != 0.0 || stat in fighter.stats }?.let { values[stat] = it }
        }
        return values
    }

    /** Шаги чтения боя цифры [stat] на теле героя: части восстановления с множителями - для строк и формулы разбора. */
    fun steps(stat: String): List<ReadingStep> = readings.filter { it.stat == stat }.flatMap { it.steps(body).orEmpty() }

    /** Силы уникалок в прибавке добычи [stat]: каждая сила против всех монстров - своим сдвигом, как её считает [read]. */
    private fun lootPowers(stat: String): List<Shift> = LOOT.filter { it.stock?.code == stat }.flatMap { kind ->
        index.powers.worldParts(stats, kind).filter { it.on(null) }.map { Shift(StatSource(SourceKind.POWER, it.stat), it.value) }
    }

    /**
     * Силы добычи у строки сводки [stat], что в её цифру не входят: против отдельных редкостей монстров - у своей
     * характеристики, шансы уникальных предметов - у редкости, карт и книг - у количества. Сервер катит их все; сводка называет их
     * отдельно, как бонусы низкого здоровья.
     */
    fun lootApart(stat: String): List<Pair<WorldPart, WorldKind>> = WorldKind.entries.filter { kind -> (kind.stock ?: APART[kind])?.code == stat }.flatMap { kind ->
        index.powers.worldParts(stats, kind).filter { kind.stock == null || !it.on(null) }.map { it to kind }
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

    /** Отмеченные [marks] с цифрами, что бой читает из них ([SheetReading.from]), среди строк сводки [shown]. */
    private fun marked(marks: Set<String>, shown: Set<String>): Set<String> = (marks + readings.filter { it.from in marks }.map { it.stat }).filterTo(HashSet()) { it in shown }

    /**
     * Из чего сложилась цифра сводки [stat]: лист по источникам с пассивными умениями и шагами Предначертания, «больше» процент-стата
     * от умений и что бой читает вместе с ней ([SheetReading], источник [SourceKind.FIGHT]) - итог равен [figures].
     *
     * [layer] - сдвиги листа эффектами карты, атласа, гильдии и зелья по характеристике (`MapEffects.heroShifts`): свои
     * сдвиги цифры ложатся как есть, а сдвиги того, что бой читает вместе с ней (общее сопротивление стихиям, доля регенерации,
     * максимум здоровья), - на её чтение: каждый источник своим сдвигом, так итог равен цифре [figures] листа под картой.
     */
    fun explain(stat: String, layer: Map<String, List<Shift>> = emptyMap()): StatBreakdown {
        val own = readings.filter { it.stat == stat }
        return explainer.explain(stat).shifted(scaled[stat].orEmpty() + own.flatMap { it.shifts(body) } + lootPowers(stat) + layer[stat].orEmpty() + own.flatMap { read(it, layer) })
    }

    /**
     * Что сдвиги [layer] сделали с чтением [reading]: каждый источник положен на лист один, а что они делают только вместе -
     * последнему, как в `MapEffects.heroShifts`; сумма равна чтению листа под всеми сдвигами, и своей цифры тоже.
     *
     * Сдвиг самой цифры тоже двигает чтение (плоскую регенерацию режут множитель и доля) - сверх своего сдвига.
     */
    private fun read(reading: SheetReading, layer: Map<String, List<Shift>>): List<Shift> {
        if (layer.isEmpty()) return emptyList()
        val own = reading.adds(body)
        fun under(moves: List<Pair<String, Double>>): Double = reading.adds(Combatant(moves.fold(stats) { sheet, (key, delta) -> sheet + (key to (sheet[key] ?: 0.0) + delta) }, body.level, body.rules)) - own
        val bySource = LinkedHashMap<StatSource, MutableList<Pair<String, Double>>>()
        layer.forEach { (key, shifts) -> shifts.forEach { bySource.getOrPut(it.source) { mutableListOf() } += key to it.delta } }
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

        /** Виды небоевых сил с характеристикой листа: их прибавка к добыче в сводке - вместе с силами. */
        private val LOOT = WorldKind.entries.filter { it.stock != null }

        /** Строка сводки, у которой названы силы вида без своей характеристики ([lootApart]). */
        private val APART = mapOf(WorldKind.UNIQUE to CoreStat.RARITY, WorldKind.MAP to CoreStat.QUANTITY, WorldKind.BOOK to CoreStat.QUANTITY)

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
