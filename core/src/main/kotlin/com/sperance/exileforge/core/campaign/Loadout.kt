package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.ConditionalLine
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.ChargeRules
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FlaskStat
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.PowerBook
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.roll.GuardianFloor
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.DamageGrowth
import com.sperance.exileforge.rules.sheet.FlaskKind
import com.sperance.exileforge.rules.sheet.FlaskSheet
import com.sperance.exileforge.rules.sheet.HeroKit
import com.sperance.exileforge.rules.sheet.SheetExplainer
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.StatFold

/**
 * Фляга на поясе: её лист из правил ([FlaskSheet] - ёмкость, расход, длительность, лечение, эффект с качеством, аффиксами
 * и бонусами героя) и когда её пьют сами - условие пояса, а без него - вида фляги. Бой и карточка читают одно и то же правило.
 */
data class BeltFlask(val sheet: FlaskSheet, val condition: SlotCondition) {
    companion object {
        /** Фляга [item] шаблона [template] на поясе; [condition] - пояса, null - условие её вида ([FlaskKind.drunk]). */
        fun of(item: ItemInstance, template: ItemTemplate, index: ContentIndex, condition: SlotCondition?): BeltFlask {
            val sheet = FlaskSheet.of(item, template, index)
            return BeltFlask(sheet, condition ?: sheet.kind.drunk)
        }

        /**
         * Пояс героя уровня [level]: фляги надетого [equipped] по местам правила [FlaskSheet.slots] (null - место пусто или фляга не
         * по уровню) с условиями пояса [conditions] тех же мест. Те же фляги, что берёт Мощь сервера (`FlaskSheet.belt`), - для захода
         * и для листа героя.
         */
        fun belt(index: ContentIndex, equipped: Collection<ItemInstance>, level: Int, conditions: List<SlotCondition?>): List<BeltFlask?> = FlaskSheet.slots(index, equipped, level).mapIndexed { n, slot ->
            slot?.let { (item, template) -> of(item, template, index, conditions.getOrNull(n)) }
        }
    }
}

/** Глоток тратит все заряды (уникальная фляга). */
val FlaskSheet.usesAll: Boolean get() = own(FlaskStat.USES_ALL.code) > 0

/** Пока действует, навыки ничего не стоят. */
val FlaskSheet.skillsFree: Boolean get() = own(FlaskStat.SKILLS_FREE.code) > 0

/** Пока действует, удары накладывают проклятия класса. */
val FlaskSheet.hexes: Boolean get() = own(FlaskStat.HITS_CURSE.code) > 0

/**
 * Умение в бою (одно с правилами, [com.sperance.exileforge.rules.sheet.KitSkill]) - что оно, изученный уровень и когда
 * срабатывает его слот; уровень в бою - по телу героя ([Combatant] читает лист как [com.sperance.exileforge.rules.sheet.StatReader]).
 */
typealias KitSkill = com.sperance.exileforge.rules.sheet.KitSkill

/**
 * What the hero brings to a fight beyond the sheet: the three active slots in the order they
 * are tried, the passive skills, the three flasks of the belt — null where a place is empty — and the
 * class's curses, for a flask that hexes with them. The level of every skill is read off the sheet of
 * the moment, so gear, the atlas and the map count.
 */
data class Loadout(
    val actives: List<KitSkill?> = emptyList(),
    val passives: List<KitSkill> = emptyList(),
    val flasks: List<BeltFlask?> = emptyList(),
    val curses: List<KitSkill> = emptyList(),
    /** The book of the unique items' powers: which of them the hero has, their sheet says. */
    val powers: PowerBook = PowerBook(),
    /** The rules of the hero's frenzy, power and endurance charges; null - зарядов нет. */
    val charges: ChargeRules? = null,
    /** Предначертание аккаунта: рычаги боя ([FateKit]). */
    val fate: FateKit = FateKit.NONE,
) {
    /** Умения героя правилами: резерв аур и строки пассивных считает [HeroKit], тот же, что у Мощи сервера. */
    val heroKit: HeroKit by lazy { HeroKit(actives, passives, curses) }

    /** Резерв запаса [pool] аурами тела [hero] - правило [HeroKit.reserved], как его держит бой. */
    fun reserve(pool: ReservePool, hero: Combatant): PoolReserve = PoolReserve.of(pool, hero, heroKit.reserved(pool, hero, hero.rules))

    /** Сколько процентов своего запаса держит пассивное умение [passive]: аура - свою долю на эффективности резерва листа. */
    fun reserve(passive: KitSkill, hero: Combatant): Double = heroKit.reserve(passive, hero, hero.rules)

    /**
     * What the passives lay on the sheet all the time: an aura's lines stronger by the aura effect, a bonus's
     * and a trigger's standing lines as they are. [hero] is the sheet without them.
     */
    fun passiveLines(hero: Combatant): List<StatLine> = heroKit.passiveLines(hero, hero.rules)

    /** Each passive with the lines it lays on [hero], the low-life bonuses among them: a figure's window names them one by one. */
    fun passiveSources(hero: Combatant): List<Pair<KitSkill, List<StatLine>>> = heroKit.passiveSources(hero, hero.rules)

    /** What the low-life bonuses lay on while the hero's life is under half. */
    fun lowLifeLines(hero: Combatant): List<StatLine> = heroKit.lowLifeLines(hero)

    companion object {
        /**
         * The hero's loadout from the character's [skills] as the server keeps them, the world's [index] and
         * the flasks worn on the belt, in its order — [flasks] null where a place is empty. Каждое умение -
         * умение героя в бою (`SkillForge`: тир и руны) по листу героя [stats].
         */
        fun of(
            skills: HeroSkills,
            index: ContentIndex,
            heroClass: String,
            flasks: List<BeltFlask?>,
            stats: Map<String, Double> = emptyMap(),
            powers: PowerBook = PowerBook(),
            charges: ChargeRules? = null,
            fate: FateKit = FateKit.NONE,
        ): Loadout {
            val kit = HeroKit.of(index, skills, heroClass, stats)
            return Loadout(
                actives = kit.actives,
                passives = kit.passives,
                flasks = flasks,
                curses = kit.curses,
                powers = powers,
                charges = charges,
                fate = fate,
            )
        }
    }
}

/**
 * Предначертание в бою: рычаги дара на его силе ([effects], один резолвер правил). Без дара - [NONE]: бой идёт, как шёл.
 */
class FateKit(val effects: FateEffects) {
    companion object {
        val NONE = FateKit(FateEffects.NONE)

        /**
         * Дар героя [fate] на силе его листа [stats] (`STOCK_FATE_EFFECT`) с судьбоносными строками надетого [equipped] и
         * переносом Разгона [carry] (`HeroInfo.fateCarry`), как его собирает сервер. В заходе бой берёт замороженный дар захода
         * ([of] от `Run.fate`).
         */
        fun of(index: ContentIndex, fate: String?, stats: Map<String, Double>, equipped: Collection<ItemInstance> = emptyList(), carry: Int = 0): FateKit = of(index.fates.effects(fate, stats[CoreStat.FATE_EFFECT.code] ?: 0.0, index.fates.worn(index, fate, equipped), carry))

        /** Готовые рычаги [effects]; без дара - [NONE]. */
        fun of(effects: FateEffects): FateKit = if (effects.fate == null) NONE else FateKit(effects)
    }
}

/**
 * How a fighter's sheet takes the lines laid on it mid-fight — a buff, a curse, a flask — and
 * becomes a body again. The hero's goes back to the sheet's own operations; a monster's folds the lines
 * over its rolled stats as its modifiers were.
 */
fun interface BodyModel {
    fun body(lines: List<StatLine>): Combatant

    companion object {
        fun of(body: Combatant, percent: Set<String> = emptySet()): BodyModel = BodyModel { lines ->
            if (lines.isEmpty()) body else Combatant(StatLines.fold(body.stats, lines, percent), body.level, body.rules)
        }
    }
}

/** Lines over a finished stat table, for a fighter with no sheet behind it. */
object StatLines {
    /**
     * Строки поверх готовых характеристик - свод правил [StatFold]: как у монстра и листа героя, процент-стат [percent]
     * складывает увеличение прибавкой и берёт «больше» на весь множитель, общие характеристики раскладываются, увеличение урона
     * вообще ложится на каждый удар монстра ([GuardianFloor.STRIKES]). Своей формулы клиент не держит.
     */
    fun fold(stats: Map<String, Double>, lines: List<StatLine>, percent: Set<String>): Map<String, Double> = StatFold.over(stats, lines, { it in percent || it == DAMAGE }, GuardianFloor.STRIKES)

    /** Every damage a fighter deals, in percent more — a percent stat whether or not the tables say so. */
    val DAMAGE: String = CoreStat.DAMAGE.code
}

/**
 * The hero as a run takes them: the sheet and what it was added up from, their level, whom
 * they pick and how far they reach, and what they bring beyond the sheet.
 */
data class HeroGear(
    val stats: Map<String, Double>,
    val level: Int,
    val model: SheetModel? = null,
    val stance: HeroStance = HeroStance(),
    val kit: Loadout = Loadout(),
    /** The stats that are a percent already, for lines laid over a sheet without a [model]. */
    val percent: Set<String> = emptySet(),
    /** Надетое по месту: id вещи - смена набора в походе узнаёт по нему новую флягу ([com.sperance.exileforge.core.campaign.run.ExpeditionRun.wear]). */
    val worn: Map<Slot, String> = emptyMap(),
)

/**
 * How the hero's body is made in a run: the sheet with the passives and the lines of the
 * moment laid among its operations, then the map's and the atlas's share over it. Зелье и древо гильдии - строки свода листа ([RunSheet]).
 */
class HeroBuild(
    val gear: HeroGear,
    private val mapEffects: Map<String, Double>,
    private val rules: CombatRules,
    /** Строки поверх листа на весь заход: дары и проклятия Разлома. */
    private val extra: List<StatLine> = emptyList(),
    /** Источники долей [mapEffects]: древо гильдии и зелье подписывают свои сдвиги листа. */
    private val origins: EffectOrigins = EffectOrigins.NONE,
) : HeroModel {
    private fun sheet(lines: List<StatLine>): Map<String, Double> = (extra + lines).let { all -> gear.model?.with(all) ?: StatLines.fold(gear.stats, all, gear.percent) }

    /** Лист под эффектами захода: зелье и гильдия - сводом листа, карта и Атлас - поверх. */
    private val run = RunSheet(mapEffects, origins, ::sheet)

    /** The body without passives: what the passives' levels and the auras' effect are read off. */
    val bare: Combatant by lazy { Combatant(run.stats(), gear.level, rules) }
    private val passives: List<StatLine> by lazy { gear.kit.passiveLines(bare) }

    /** The body between fights: the passives on, the lines of a flask still running on top. */
    val body: Combatant by lazy { body(emptyList()) }

    /** Тела и рост урона по строкам боя: наборы строк в бою повторяются (эффект лёг, истёк, лёг снова) - лист на них сводится один раз. */
    private val bodies = LineMemo<Combatant>()
    private val growths = LineMemo<DamageGrowth>()

    override fun body(lines: List<StatLine>): Combatant = bodies.get(lines) ?: freshBody(lines).also { bodies.put(lines, it) }

    override fun freshBody(lines: List<StatLine>): Combatant = Combatant(run.stats(passives + lines), gear.level, rules)

    /** The passives' lines that wait for low life; the fight lays them on while it lasts. */
    override val lowLife: List<StatLine> by lazy { gear.kit.lowLifeLines(bare) }

    /** Рост урона листа с пассивами, строками зелья и гильдии и [lines]: увеличения и «больше» вида, на них растут чары и урон во времени. */
    override fun growth(lines: List<StatLine>): DamageGrowth {
        val model = gear.model ?: return DamageGrowth.NONE
        return growths.get(lines) ?: TypedGrowth(model.growth(run.boons + passives + lines)).also { growths.put(lines, it) }
    }

    override fun conditional(active: Set<Condition>): List<StatLine> = gear.model?.conditional(active).orEmpty()
    override fun conditionalSourced(active: Set<Condition>): List<Pair<Condition, StatLine>> = gear.model?.conditionalSourced(active).orEmpty()
    override val explainer: SheetExplainer? get() = gear.model?.explainer
    override val passiveLines: List<StatLine> get() = passives
    override fun shifts(): Map<String, List<Shift>> = run.shifts(passives)
    override fun against(states: Set<Condition>): Double = gear.model?.against(states) ?: 0.0
}

/**
 * Память листа по строкам боя: последние [capacity] наборов строк и что на них сведено. Ключ - сами строки по порядку: свод
 * зависит от него (замена «последняя держит», порядок сложения), а бой собирает строки героя всегда в одном порядке.
 * Без синхронизации: один поток на [HeroBuild], как и сам бой.
 */
internal class LineMemo<V : Any>(private val capacity: Int = CAPACITY) {
    private val kept = object : LinkedHashMap<List<StatLine>, V>(capacity * 2, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<List<StatLine>, V>?): Boolean = size > capacity
    }

    fun get(lines: List<StatLine>): V? = kept[lines]

    /** Кладёт [value] под копией [lines]: ключ не меняется вслед за списком, который передал бой. */
    fun put(lines: List<StatLine>, value: V) {
        kept[ArrayList(lines)] = value
    }

    private companion object {
        const val CAPACITY = 16
    }
}

/**
 * Рост урона [inner] с памятью по виду и по набору видов (доп. урон от физического): удар читает увеличение каждого своего вида, лист раскладывает строки один раз на вид.
 * Без синхронизации: один поток на [HeroBuild], как и сам бой.
 */
private class TypedGrowth(private val inner: DamageGrowth) : DamageGrowth {
    private val increases = HashMap<String, Double>()
    private val mores = HashMap<String, Double>()
    private val jointIncreases = HashMap<Set<String>, Double>()
    private val jointMores = HashMap<Set<String>, Double>()

    override fun increased(type: String): Double = increases[type] ?: inner.increased(type).also { increases[type] = it }

    override fun more(type: String): Double = mores[type] ?: inner.more(type).also { mores[type] = it }

    override fun increasedAny(types: Set<String>): Double = jointIncreases[types] ?: inner.increasedAny(types).also { jointIncreases[types] = it }

    override fun moreAny(types: Set<String>): Double = jointMores[types] ?: inner.moreAny(types).also { jointMores[types] = it }
}

/**
 * The hero's [BodyModel]: the lines low life lays on and, for a spell of an element, the
 * increases of that element on the sheet — a monster has neither. Рост урона [growth]: увеличения и «больше» вида.
 */
interface HeroModel : BodyModel {
    val lowLife: List<StatLine>

    /**
     * Тело со строками [lines], как [body], но мимо памяти тел боя: память - одного потока боя, а это тело собирают и вне его
     * (лестница тела героя для разбора характеристики, [com.sperance.exileforge.core.campaign.combat.HeroLadder]).
     */
    fun freshBody(lines: List<StatLine>): Combatant

    /** Рост урона вида листа со строками [lines]: увеличения и «больше» ([DamageGrowth] правил). */
    fun growth(lines: List<StatLine>): DamageGrowth

    /** The conditional lines of the sheet that hold under [active]. */
    fun conditional(active: Set<Condition>): List<StatLine> = emptyList()

    /** The same lines with the condition each waits for. */
    fun conditionalSourced(active: Set<Condition>): List<Pair<Condition, StatLine>> = emptyList()

    /** The sheet taken apart by source: what the log's card lays a hero's stat out with; null for a bare body. */
    val explainer: SheetExplainer? get() = null

    /** The passives' lines laid over the sheet in a run. */
    val passiveLines: List<StatLine> get() = emptyList()

    /** What the map and the atlas moved on the sheet, by stat. */
    fun shifts(): Map<String, List<Shift>> = emptyMap()

    /** The increased damage the sheet's lines give against a target in [states]. */
    fun against(states: Set<Condition>): Double = 0.0

    companion object {
        fun of(body: Combatant): HeroModel = object : HeroModel {
            private val model = BodyModel.of(body)
            override fun body(lines: List<StatLine>): Combatant = model.body(lines)
            override fun freshBody(lines: List<StatLine>): Combatant = model.body(lines)
            override val lowLife: List<StatLine> = emptyList()
            override fun growth(lines: List<StatLine>): DamageGrowth = DamageGrowth.NONE
        }
    }
}

/**
 * What one draught gives: life, mana and shield at once, life and mana a second while it
 * runs, the lines it lays on for [duration] seconds, and the seconds nothing touches the hero.
 * [conditional] - строки фляги с условием: держатся во время глотка, лишь пока держится их условие.
 */
data class Draught(
    val life: Double,
    val mana: Double,
    val shield: Double,
    val lifeRate: Double,
    val manaRate: Double,
    val lines: List<StatLine>,
    val duration: Double,
    val invulnerable: Double,
    val conditional: List<ConditionalLine> = emptyList(),
    /** Доли вылеченного здоровья, что глоток отдаёт мане и энергощиту: от фактически вылеченного, сразу и за время. */
    val toMana: Double = 0.0,
    val toShield: Double = 0.0,
) {
    /** Gives nothing but life: such a draught stops at full life. */
    val lifeOnly: Boolean get() = (life > 0 || lifeRate > 0) && mana <= 0 && manaRate <= 0 && shield <= 0 && toMana <= 0 && toShield <= 0 && lines.isEmpty() && conditional.isEmpty() && invulnerable <= 0

    /**
     * Что глоток даёт сверх восполнения полос: строки на время (и силы «при глотке» - они тоже строки фляги), строки с условием и
     * неуязвимость. Доли от вылеченного ([toMana], [toShield]) сюда не входят: на полном здоровье они ничего не дают.
     */
    val boons: Boolean get() = lines.isNotEmpty() || conditional.isNotEmpty() || invulnerable > 0

    /**
     * Полосы, из-за которых глоток впустую при полосах [bars]: он не даёт ничего сверх восполнения ([boons]), а всё, что он
     * восполняет, уже полно. Пусто - глоток не впустую (и глоток, что не восполняет ничего, - тоже).
     */
    fun waste(bars: HeroBars): List<HeroBar> {
        if (boons) return emptyList()
        val refilled = HeroBar.entries.filter { it.refilledBy(this) }
        return refilled.takeIf { it.all { bar -> bar.full(bars) } }.orEmpty()
    }

    /** Глоток впустую ([waste]) - как в PoE, его не пьют ни касанием, ни сами. */
    fun wasted(bars: HeroBars): Boolean = waste(bars).isNotEmpty()
}

/**
 * Полоса героя, что восполняет глоток фляги: [now] и [max] - её запас и предел в полосах героя, [refill] - восполняет ли её глоток сам,
 * сразу или за время.
 */
enum class HeroBar(private val now: (HeroBars) -> Double, private val max: (HeroBars) -> Double, private val refill: (Draught) -> Boolean) {
    LIFE(HeroBars::life, HeroBars::maxLife, { it.life > 0 || it.lifeRate > 0 }),
    MANA(HeroBars::mana, HeroBars::maxMana, { it.mana > 0 || it.manaRate > 0 }),
    SHIELD(HeroBars::shield, HeroBars::maxShield, { it.shield > 0 }),
    ;

    /** Ключ словаря слов «полоса полна» - подсказка у фляги, чей глоток впустую. */
    val fullKey: String get() = "expedition.flask_full.${name.lowercase()}"

    /** Полна ли полоса в полосах героя [bars]. */
    fun full(bars: HeroBars): Boolean = now(bars) >= max(bars)

    /** Восполняет ли глоток [draught] эту полосу сам. */
    fun refilledBy(draught: Draught): Boolean = refill(draught)
}

/** Полосы героя в миг глотка: сколько в каждой и её предел (у маны - свободное от аур). */
data class HeroBars(val life: Double, val maxLife: Double, val mana: Double, val maxMana: Double, val shield: Double, val maxShield: Double)

/**
 * A draught of this flask by [hero] at [life], mana up to [manaCap]: the recovery grows at low life by
 * its own line, the instant share comes now and the rest runs over the duration; a life flask may also
 * give mana or shield back, and any flask a sip of either.
 *
 * Условная строка фляги не ложится на героя без своего условия. Глоток маны - тоже восстановление фляги: «фляги не лечат» гасит и его.
 */
fun FlaskSheet.draught(hero: Combatant, life: Double, manaCap: Double): Draught {
    val duration = duration(hero::get)
    val low = hero.lowAt(life)
    val recover = recovery(hero::get) * if (low) 1 + own(FlaskStat.LOW_LIFE_RECOVERY.code) / 100 else 1.0
    val lifeAmount = own(FlaskStat.LIFE.code) * recover
    val manaAmount = own(FlaskStat.MANA.code) * recover
    val scale = effect(hero::get)
    val (steady, conditioned) = lines.partition { it.condition == null }
    return Draught(
        life = lifeAmount * instant,
        mana = manaAmount * instant + if (FlaskSheet.banned(CoreStat.FLASK_RECOVERY.code, hero::get)) 0.0 else manaCap * own(FlaskStat.SIP_MANA.code) / 100,
        shield = hero.maxShield * own(FlaskStat.SIP_SHIELD.code) / 100,
        lifeRate = lifeAmount * (1 - instant) / duration,
        manaRate = manaAmount * (1 - instant) / duration,
        lines = steady.map { StatLine(it.stat, it.op, it.value * scale) },
        duration = duration,
        invulnerable = own(FlaskStat.INVULNERABLE.code),
        conditional = conditioned.map { ConditionalLine(requireNotNull(it.condition), StatLine(it.stat, it.op, it.value * scale)) },
        toMana = own(FlaskStat.LIFE_TO_MANA.code) / 100,
        toShield = own(FlaskStat.LIFE_TO_SHIELD.code) / 100,
    )
}

/** Здоровье [life] - низкое для тела [this] (`combat.lowLife` правил): от него глоток растит восстановление. */
private fun Combatant.lowAt(life: Double): Boolean = life < maxLife * rules.lowLife

/**
 * Глотки фляг пояса по местам, запомненные для снимков кадра и проверок «впустую»: глоток ([FlaskSheet.draught]) зависит от листа
 * фляги, тела героя, порога низкого здоровья и свободной маны, а не от каждого очка здоровья, - строится заново лишь при их смене и
 * равен свежему.
 */
class Draughts {
    private val held = HashMap<Int, Held>()

    /** Глоток фляги [sheet] места [slot] телом [hero] при здоровье [life] и свободной мане [manaCap]. */
    fun of(slot: Int, sheet: FlaskSheet, hero: Combatant, life: Double, manaCap: Double): Draught {
        val low = hero.lowAt(life)
        held[slot]?.takeIf { it.sheet === sheet && it.hero === hero && it.low == low && it.manaCap == manaCap }?.let { return it.draught }
        return sheet.draught(hero, life, manaCap).also { held[slot] = Held(sheet, hero, low, manaCap, it) }
    }

    /** Глоток [draught] и то, из чего он построен. */
    private class Held(val sheet: FlaskSheet, val hero: Combatant, val low: Boolean, val manaCap: Double, val draught: Draught)
}

/**
 * Резерв запаса [pool] аурами: весь запас [whole] и доля [percent], что держат ауры; [held] - удержанное, [free] - свободное, что
 * берёт бой. Запас, что резерв урезает строкой листа ([ReservePool.held]: здоровье), в теле уже без доли - весь запас собран обратно.
 */
data class PoolReserve(val pool: ReservePool, val whole: Double, val percent: Double) {
    val held: Double get() = whole * percent / 100
    val free: Double get() = whole - held

    companion object {
        /** Резерв запаса [pool] тела [body] при доле [percent]. */
        fun of(pool: ReservePool, body: Combatant, percent: Double): PoolReserve {
            val amount = when (pool) {
                ReservePool.MANA -> body.maxMana
                ReservePool.LIFE -> body.maxLife
            }
            val whole = if (pool.held != null && percent < 100) amount / (1 - percent / 100) else amount
            return PoolReserve(pool, whole, percent)
        }
    }
}

/** Характеристика листа, что меряет запас: мана - максимум маны, здоровье - максимум здоровья. */
val ReservePool.stat: String get() = when (this) {
    ReservePool.MANA -> CoreStat.MANA.code
    ReservePool.LIFE -> CoreStat.HEALTH.code
}
