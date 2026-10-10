package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Combatant
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
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillStat
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.FlaskKind
import com.sperance.exileforge.rules.sheet.FlaskSheet
import com.sperance.exileforge.rules.sheet.SheetExplainer
import com.sperance.exileforge.rules.sheet.Shift
import kotlin.math.max

/**
 * Фляга на поясе (4.2.0): её лист из правил ([FlaskSheet] - ёмкость, расход, длительность, лечение, эффект с качеством, аффиксами
 * и бонусами героя) и когда её пьют сами - условие пояса, а без него - вида фляги. Бой и карточка читают одно и то же правило.
 */
data class BeltFlask(val sheet: FlaskSheet, val condition: SlotCondition) {
    companion object {
        /** Фляга [item] шаблона [template] на поясе; [condition] - пояса, null - своё условие вида. */
        fun of(item: ItemInstance, template: ItemTemplate, index: ContentIndex, condition: SlotCondition?): BeltFlask {
            val sheet = FlaskSheet.of(item, template, index)
            return BeltFlask(sheet, condition ?: defaultCondition(sheet.kind))
        }

        /** When a flask is drunk by itself unless the belt says otherwise: a life flask at half life, a mana flask low on mana, the rest at the start. */
        fun defaultCondition(kind: FlaskKind): SlotCondition = when (kind) {
            FlaskKind.LIFE -> SlotCondition.LIFE_50
            FlaskKind.MANA -> SlotCondition.MANA_30
            FlaskKind.UTILITY -> SlotCondition.FIGHT_START
        }
    }
}

/** Глоток тратит все заряды (уникальная фляга). */
val FlaskSheet.usesAll: Boolean get() = own("FLASK_USES_ALL") > 0

/** Пока действует, навыки ничего не стоят. */
val FlaskSheet.skillsFree: Boolean get() = own("FLASK_SKILLS_FREE") > 0

/** Пока действует, удары накладывают проклятия класса. */
val FlaskSheet.hexes: Boolean get() = own("FLASK_HITS_CURSE") > 0

/**
 * A skill as the fight uses it: what it is, the level it was learned to and when its slot fires. С 4.4.0 [skill] - умение
 * героя в бою ([com.sperance.exileforge.rules.content.SkillForge]: тир и руны поверх контента), [tier] - его тир в бою.
 */
data class KitSkill(
    val skill: SkillDefinition,
    val learned: Int,
    /** The rules' ceiling for a skill level boosted by gear, the atlas and the map (`skills.rules.boostedMaxLevel`). */
    val ceiling: Int,
    val condition: SlotCondition = skill.condition,
    val tier: Int = HeroSkills.FIRST_TIER,
    /** Все открытые гнёзда рун умения заняты (4.6.2, `SkillGrowth.full`): его читает Рунный круг Предначертания. */
    val filled: Boolean = false,
) {
    /**
     * The level it acts at on [hero]: the learned one and what gear, the atlas and the map add — every
     * skill's, its type's, and a passive's — never past the rules' ceiling (3.19.0).
     */
    fun level(hero: Combatant): Int {
        val type = when (skill.type) {
            SkillType.ATTACK -> hero[CoreStat.ATTACK_LEVEL.code]
            SkillType.SPELL -> hero[CoreStat.SPELL_LEVEL.code]
            SkillType.WARCRY -> hero[CoreStat.WARCRY_LEVEL.code]
            SkillType.CURSE -> hero[CoreStat.CURSE_LEVEL.code]
            SkillType.AURA -> hero[CoreStat.AURA_LEVEL.code] + hero[CoreStat.PASSIVE_LEVEL.code]
            SkillType.BONUS, SkillType.TRIGGER -> hero[CoreStat.PASSIVE_LEVEL.code]
            SkillType.HEAL, SkillType.GUARD -> 0.0
        }
        return (learned + hero[CoreStat.SKILL_LEVEL.code] + type).toInt().coerceIn(1, ceiling)
    }
}

/** A skill's stat lines at [level], each [scale]d — an aura's, a warcry's or a curse's effect. */
fun List<SkillStat>.lines(level: Int, scale: Double = 1.0): List<StatLine> = map { StatLine(it.stat, it.op, it.value.at(level) * scale) }

/**
 * What the hero brings to a fight beyond the sheet (2.78.0): the three active slots in the order they
 * are tried, the passive skills, the three flasks of the belt — null where a place is empty — and the
 * class's curses, for a flask that hexes with them. The level of every skill is read off the sheet of
 * the moment, so gear, the atlas and the map count.
 */
data class Loadout(
    val actives: List<KitSkill?> = emptyList(),
    val passives: List<KitSkill> = emptyList(),
    val flasks: List<BeltFlask?> = emptyList(),
    val curses: List<KitSkill> = emptyList(),
    /** The book of the unique items' powers (2.79.0): which of them the hero has, their sheet says. */
    val powers: PowerBook = PowerBook(),
    /** The rules of the hero's frenzy, power and endurance charges (3.33.0, server 1.32.0). */
    val charges: ChargeRules = ChargeRules(),
    /** Предначертание аккаунта (4.6.0): рычаги боя ([FateKit]). */
    val fate: FateKit = FateKit.NONE,
) {
    /** How many percent of the maximum mana the passive auras hold, after the sheet's reservation efficiency. */
    fun reserved(hero: Combatant): Double = (passives.filter { it.skill.type == SkillType.AURA }.sumOf { it.skill.reserve } / max(0.1, 1 + hero[CoreStat.RESERVATION.code] / 100)).coerceIn(0.0, 100.0)

    /**
     * What the passives lay on the sheet all the time: an aura's lines stronger by the aura effect, a bonus's
     * and a trigger's standing lines as they are. [hero] is the sheet without them.
     */
    fun passiveLines(hero: Combatant): List<StatLine> = passives.filterNot { it.skill.lowLife }.flatMap { lines(it, hero) }

    /** Each passive with the lines it lays on [hero], the low-life bonuses among them: a figure's window names them one by one. */
    fun passiveSources(hero: Combatant): List<Pair<KitSkill, List<StatLine>>> = passives.map { it to lines(it, hero) }

    /** What the low-life bonuses lay on while the hero's life is under half. */
    fun lowLifeLines(hero: Combatant): List<StatLine> = passives.filter { it.skill.lowLife }.flatMap { lines(it, hero) }

    private fun lines(passive: KitSkill, hero: Combatant): List<StatLine> {
        val level = passive.level(hero)
        return if (passive.skill.type == SkillType.AURA) {
            passive.skill.stats.lines(level, 1 + hero[CoreStat.AURA_EFFECT.code] / 100)
        } else {
            passive.skill.stats.lines(level)
        }
    }

    companion object {
        /**
         * The hero's loadout from the character's [skills] as the server keeps them, the world's [index] and
         * the flasks worn on the belt, in its order — [flasks] null where a place is empty. С 4.4.0 каждое умение -
         * умение героя в бою (`SkillForge`: тир и руны) по листу героя [stats].
         */
        fun of(
            skills: HeroSkills,
            index: ContentIndex,
            heroClass: String,
            flasks: List<BeltFlask?>,
            stats: Map<String, Double> = emptyMap(),
            powers: PowerBook = PowerBook(),
            charges: ChargeRules = ChargeRules(),
            fate: FateKit = FateKit.NONE,
        ): Loadout {
            val book = index.skills
            fun forged(skill: SkillDefinition, condition: SlotCondition? = null) = KitSkill(
                index.skillForge.effective(skill, skills, stats),
                skills.level(skill.code).coerceAtLeast(1),
                book.rules.boostedMaxLevel,
                condition ?: skill.condition,
                index.skillGrowth.tier(skill, skills, stats),
                index.skillGrowth.full(skill, skills, stats),
            )
            fun kit(code: String?, condition: SlotCondition? = null) = code?.let(book.byCode::get)?.let { forged(it, condition) }
            return Loadout(
                actives = skills.active.map { slot -> slot?.let { kit(it.skill, it.condition) } },
                passives = skills.passive.mapNotNull { kit(it) },
                flasks = flasks,
                curses = book.ofClass(heroClass).filter { it.type == SkillType.CURSE }.map { forged(it) },
                powers = powers,
                charges = charges,
                fate = fate,
            )
        }
    }
}

/**
 * Предначертание в бою (4.6.0): рычаги дара на его силе ([effects], один резолвер правил). Без дара - [NONE]: бой идёт, как шёл.
 */
class FateKit(val effects: FateEffects) {
    companion object {
        val NONE = FateKit(FateEffects.NONE)

        /**
         * Дар героя [fate] на силе его листа [stats] (`STOCK_FATE_EFFECT`) с судьбоносными строками надетого [equipped] (4.6.2). В
         * заходе бой берёт замороженный дар захода ([of] от `Run.fate`).
         */
        fun of(index: ContentIndex, fate: String?, stats: Map<String, Double>, equipped: Collection<ItemInstance> = emptyList()): FateKit = of(index.fates.effects(fate, stats[CoreStat.FATE_EFFECT.code] ?: 0.0, index.fates.worn(index, fate, equipped)))

        /** Готовые рычаги [effects]; без дара - [NONE]. */
        fun of(effects: FateEffects): FateKit = if (effects.fate == null) NONE else FateKit(effects)
    }
}

/**
 * How a fighter's sheet takes the lines laid on it mid-fight (2.78.0) — a buff, a curse, a flask — and
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

/** Lines over a finished stat table (2.78.0), for a fighter with no sheet behind it. */
object StatLines {
    /**
     * `(value + ΣADD) × (1 + ΣINCREASED/100) × Π(1 + MORE/100)`, `SET` last — a monster modifier's formula;
     * a [percent] stat takes an increase as an addition and MORE on its whole multiplier, as on the sheet.
     */
    fun fold(stats: Map<String, Double>, lines: List<StatLine>, percent: Set<String>): Map<String, Double> {
        if (lines.isEmpty()) return stats
        val result = stats.toMutableMap()
        lines.groupBy { it.stat }.forEach { (stat, own) ->
            val added = (stats[stat] ?: 0.0) + own.filter { it.op == Op.ADD }.sumOf { it.value }
            val increase = own.filter { it.op == Op.INCREASED }.sumOf { it.value }
            val more = own.filter { it.op == Op.MORE }
            result[stat] = own.lastOrNull { it.op == Op.SET }?.value ?: if (stat in percent || stat == DAMAGE) {
                more.fold(added + increase) { value, line -> (100 + value) * (1 + line.value / 100) - 100 }
            } else {
                more.fold(added * (1 + increase / 100)) { value, line -> value * (1 + line.value / 100) }
            }
        }
        return result
    }

    /** Every damage a fighter deals, in percent more — a percent stat whether or not the tables say so. */
    val DAMAGE: String = CoreStat.DAMAGE.code
}

/**
 * The hero as a run takes them (2.78.0): the sheet and what it was added up from, their level, whom
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
)

/**
 * How the hero's body is made in a run (2.78.0): the sheet with the passives and the lines of the
 * moment laid among its operations, then the map's and the atlas's share over it.
 */
class HeroBuild(
    val gear: HeroGear,
    private val mapEffects: Map<String, Double>,
    private val rules: CombatRules,
    /** Строки поверх листа на весь заход (3.96.0): дары и проклятия Разлома. */
    private val extra: List<StatLine> = emptyList(),
) : HeroModel {
    private fun sheet(lines: List<StatLine>): Map<String, Double> = (extra + lines).let { all -> gear.model?.with(all) ?: StatLines.fold(gear.stats, all, gear.percent) }

    /** The body without passives: what the passives' levels and the auras' effect are read off. */
    val bare: Combatant by lazy { Combatant(MapEffects.hero(sheet(emptyList()), mapEffects), gear.level, rules) }
    private val passives: List<StatLine> by lazy { gear.kit.passiveLines(bare) }

    /** The body between fights: the passives on, the lines of a flask still running on top. */
    val body: Combatant by lazy { body(emptyList()) }

    override fun body(lines: List<StatLine>): Combatant = Combatant(MapEffects.hero(sheet(passives + lines), mapEffects), gear.level, rules)

    /** The passives' lines that wait for low life; the fight lays them on while it lasts. */
    override val lowLife: List<StatLine> by lazy { gear.kit.lowLifeLines(bare) }

    /** The increases of [stat] on the sheet, [lines] among them: a spell of that element is multiplied by them. */
    override fun increased(stat: String, lines: List<StatLine>): Double = gear.model?.increased(stat, passives + lines) ?: 0.0

    override fun conditional(active: Set<Condition>): List<StatLine> = gear.model?.conditional(active).orEmpty()
    override fun conditionalSourced(active: Set<Condition>): List<Pair<Condition, StatLine>> = gear.model?.conditionalSourced(active).orEmpty()
    override val explainer: SheetExplainer? get() = gear.model?.explainer
    override val passiveLines: List<StatLine> get() = passives
    override fun shifts(): Map<String, List<Shift>> = MapEffects.heroShifts(sheet(passives), mapEffects)
    override fun against(states: Set<Condition>): Double = gear.model?.against(states) ?: 0.0
}

/**
 * The hero's [BodyModel] (2.78.0): the lines low life lays on and, for a spell of an element, the
 * increases of that element on the sheet — a monster has neither.
 */
interface HeroModel : BodyModel {
    val lowLife: List<StatLine>
    fun increased(stat: String, lines: List<StatLine>): Double

    /** The conditional lines of the sheet (3.35.0) that hold under [active]. */
    fun conditional(active: Set<Condition>): List<StatLine> = emptyList()

    /** The same lines with the condition each waits for (3.37.0). */
    fun conditionalSourced(active: Set<Condition>): List<Pair<Condition, StatLine>> = emptyList()

    /** The sheet taken apart by source (3.37.0): what the log's card lays a hero's stat out with; null for a bare body. */
    val explainer: SheetExplainer? get() = null

    /** The passives' lines laid over the sheet in a run (3.37.0). */
    val passiveLines: List<StatLine> get() = emptyList()

    /** What the map and the atlas moved on the sheet, by stat (3.37.0). */
    fun shifts(): Map<String, List<Shift>> = emptyMap()

    /** The increased damage the sheet's lines give against a target in [states]. */
    fun against(states: Set<Condition>): Double = 0.0

    companion object {
        fun of(body: Combatant): HeroModel = object : HeroModel {
            private val model = BodyModel.of(body)
            override fun body(lines: List<StatLine>): Combatant = model.body(lines)
            override val lowLife: List<StatLine> = emptyList()
            override fun increased(stat: String, lines: List<StatLine>): Double = 0.0
        }
    }
}

/**
 * What one draught gives (2.78.0): life, mana and shield at once, life and mana a second while it
 * runs, the lines it lays on for [duration] seconds, and the seconds nothing touches the hero.
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
) {
    /** Gives nothing but life (3.79.0): such a draught stops at full life and is not drunk on a full bar. */
    val lifeOnly: Boolean get() = (life > 0 || lifeRate > 0) && mana <= 0 && manaRate <= 0 && shield <= 0 && lines.isEmpty() && invulnerable <= 0
}

/**
 * A draught of this flask by [hero] at [life], mana up to [manaCap]: the recovery grows at low life by
 * its own line, the instant share comes now and the rest runs over the duration; a life flask may also
 * give mana or shield back, and any flask a sip of either.
 */
fun FlaskSheet.draught(hero: Combatant, life: Double, manaCap: Double): Draught {
    val duration = duration(hero::get)
    val low = life < hero.maxLife * LOW_LIFE
    val recover = recovery(hero::get) * if (low) 1 + own("FLASK_LOW_LIFE_RECOVERY") / 100 else 1.0
    val lifeAmount = own(FlaskStat.LIFE.code) * recover
    val manaAmount = own(FlaskStat.MANA.code) * recover
    return Draught(
        life = lifeAmount * instant,
        mana = manaAmount * instant + lifeAmount * own("FLASK_LIFE_TO_MANA") / 100 + manaCap * own("FLASK_SIP_MANA") / 100,
        shield = lifeAmount * own("FLASK_LIFE_TO_SHIELD") / 100 + hero.maxShield * own("FLASK_SIP_SHIELD") / 100,
        lifeRate = lifeAmount * (1 - instant) / duration,
        manaRate = manaAmount * (1 - instant) / duration,
        lines = effect(hero::get).let { scale -> lines.map { StatLine(it.stat, it.op, it.value * scale) } },
        duration = duration,
        invulnerable = own("FLASK_INVULNERABLE"),
    )
}

/** "Low life", as a flask and a passive's answer read it: under this share of the maximum. */
const val LOW_LIFE = 0.35
