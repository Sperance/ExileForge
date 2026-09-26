package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.campaign.CombatRules
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.modifier.Modifier
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierOperation
import com.sperance.exileforge.core.model.powers.PowerBook
import com.sperance.exileforge.core.model.skills.HeroSkills
import com.sperance.exileforge.core.model.skills.SkillBook
import com.sperance.exileforge.core.model.skills.SkillDefinition
import com.sperance.exileforge.core.model.skills.SkillStat
import com.sperance.exileforge.core.model.skills.SkillType
import com.sperance.exileforge.core.model.skills.SlotCondition
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.math.max

/** What a flask does when drunk (2.78.0), by its base: brings life back, brings mana back, or only lays its lines for a while. */
enum class FlaskKind { LIFE, MANA, UTILITY }

/**
 * A flask on the belt as a fight takes it (2.78.0): its base and its own affixes summed by stat, as the
 * server rolled them. What the hero's sheet adds — more charges, a longer or stronger draught — is the
 * fight's to lay on, with the sheet of the moment: see the functions taking `hero`.
 */
data class Flask(
    /** The template's code: the name and the icon. */
    val code: String,
    val kind: FlaskKind,
    val condition: SlotCondition,
    val quality: Int = 0,
    private val added: Map<String, Double> = emptyMap(),
    private val increased: Map<String, Double> = emptyMap(),
    /** What it lays on the hero while its effect lasts, before its effect's increase. */
    val lines: List<StatLine> = emptyList(),
) {
    fun own(stat: String): Double = added[stat] ?: 0.0
    private fun inc(stat: String): Double = increased[stat] ?: 0.0

    val maxCharges: Double get() = max(1.0, own("FLASK_CHARGES"))
    fun perUse(hero: Combatant): Double = own("FLASK_CHARGES_PER_USE") * max(0.0, 1 - (own("STOCK_FLASK_CHARGES_USED") + hero["STOCK_FLASK_CHARGES_USED"]) / 100)
    fun duration(hero: Combatant): Double = max(0.5, own("FLASK_DURATION") * (1 + (inc("STOCK_FLASK_DURATION") + hero["STOCK_FLASK_DURATION"]) / 100))
    /** How much stronger its lines are: its own and the hero's increase, and a utility flask's quality. */
    fun effect(hero: Combatant): Double =
        max(0.0, 1 + (inc("STOCK_FLASK_EFFECT") + hero["STOCK_FLASK_EFFECT"] + if (kind == FlaskKind.UTILITY) quality.toDouble() else 0.0) / 100)
    /** How much more it brings back: its own and the hero's increase, life flasks' own, and a recovery flask's quality. */
    fun recovery(hero: Combatant): Double = max(0.0, 1 + (inc("STOCK_FLASK_RECOVERY") + hero["STOCK_FLASK_RECOVERY"] +
        (if (kind == FlaskKind.LIFE) hero["STOCK_FLASK_LIFE_RECOVERY"] else 0.0) + (if (kind != FlaskKind.UTILITY) quality.toDouble() else 0.0)) / 100)
    /** The charges a kill of [base] brings: the rule's, more by its own and the hero's increase, plus both flat additions. */
    fun gained(base: Double, hero: Combatant): Double =
        max(0.0, base * (1 + (inc("STOCK_FLASK_CHARGES_GAINED") + hero["STOCK_FLASK_CHARGES_GAINED"]) / 100) + own("STOCK_FLASK_CHARGES_PER_KILL") + hero["STOCK_FLASK_CHARGES_PER_KILL"])
    /** The share of its recovery that comes at once; the rest runs over its duration. */
    val instant: Double get() = (own("FLASK_INSTANT") / 100).coerceIn(0.0, 1.0)
    val usesAll: Boolean get() = own("FLASK_USES_ALL") > 0
    val skillsFree: Boolean get() = own("FLASK_SKILLS_FREE") > 0
    val hexes: Boolean get() = own("FLASK_HITS_CURSE") > 0

    companion object {
        /** Stats that are the flask's own working rather than what it lays on the hero. */
        private fun mechanic(stat: String) = stat.startsWith("FLASK_") || stat.startsWith("STOCK_FLASK_")

        /** The flask [item] of [template] as the fight takes it; [condition] is the belt's, or the kind's own when null. */
        fun of(item: EquipmentInstance, template: JsonObject, definitions: Map<String, ModifierDefinition>, condition: SlotCondition?): Flask {
            val base = (template["baseParams"] as? JsonArray)
                ?.let { runCatching { WireJson.decodeFromJsonElement(ListSerializer(Modifier.serializer()), it) }.getOrNull() }.orEmpty()
            val added = mutableMapOf<String, Double>()
            val increased = mutableMapOf<String, Double>()
            val lines = mutableListOf<StatLine>()
            (base + item.params).forEach { modifier ->
                definitions[modifier.modifierCode]?.effects?.forEachIndexed { index, effect ->
                    val value = modifier.values.getOrNull(index) ?: return@forEachIndexed
                    when {
                        !mechanic(effect.stat) -> lines += StatLine(effect.stat, effect.operation, value)
                        effect.operation == ModifierOperation.INCREASED -> increased.merge(effect.stat, value, Double::plus)
                        else -> added.merge(effect.stat, value, Double::plus)
                    }
                }
            }
            val kind = when {
                (added["FLASK_LIFE"] ?: 0.0) > 0 -> FlaskKind.LIFE
                (added["FLASK_MANA"] ?: 0.0) > 0 -> FlaskKind.MANA
                else -> FlaskKind.UTILITY
            }
            return Flask(template.text("code"), kind, condition ?: defaultCondition(kind), item.quality, added, increased, lines)
        }

        /** When a flask is drunk by itself unless the belt says otherwise: a life flask at half life, a mana flask low on mana, the rest at the start. */
        fun defaultCondition(kind: FlaskKind): SlotCondition = when (kind) {
            FlaskKind.LIFE -> SlotCondition.LIFE_50
            FlaskKind.MANA -> SlotCondition.MANA_30
            FlaskKind.UTILITY -> SlotCondition.FIGHT_START
        }
    }
}

/** A skill as the fight uses it: what it is, the level it was learned to and when its slot fires. */
data class KitSkill(val skill: SkillDefinition, val learned: Int, val condition: SlotCondition = skill.condition) {
    /**
     * The level it acts at on [hero]: the learned one and what gear, the atlas and the map add — every
     * skill's, its type's, and a passive's.
     */
    fun level(hero: Combatant): Int {
        val type = when (skill.type) {
            SkillType.ATTACK -> hero["STOCK_ATTACK_LEVEL"]
            SkillType.SPELL -> hero["STOCK_SPELL_LEVEL"]
            SkillType.WARCRY -> hero["STOCK_WARCRY_LEVEL"]
            SkillType.CURSE -> hero["STOCK_CURSE_LEVEL"]
            SkillType.AURA -> hero["STOCK_AURA_LEVEL"] + hero["STOCK_PASSIVE_LEVEL"]
            SkillType.BONUS, SkillType.TRIGGER -> hero["STOCK_PASSIVE_LEVEL"]
            SkillType.HEAL, SkillType.GUARD -> 0.0
        }
        return (learned + hero["STOCK_SKILL_LEVEL"] + type).toInt().coerceAtLeast(1)
    }
}

/** A skill's stat lines at [level], each [scale]d — an aura's, a warcry's or a curse's effect. */
fun List<SkillStat>.lines(level: Int, scale: Double = 1.0): List<StatLine> = map { StatLine(it.stat, it.operation, it.value.at(level) * scale) }

/**
 * What the hero brings to a fight beyond the sheet (2.78.0): the three active slots in the order they
 * are tried, the passive skills, the three flasks of the belt — null where a place is empty — and the
 * class's curses, for a flask that hexes with them. The level of every skill is read off the sheet of
 * the moment, so gear, the atlas and the map count.
 */
data class Loadout(
    val actives: List<KitSkill?> = emptyList(),
    val passives: List<KitSkill> = emptyList(),
    val flasks: List<Flask?> = emptyList(),
    val curses: List<KitSkill> = emptyList(),
    /** The book of the unique items' powers (2.79.0): which of them the hero has, their sheet says. */
    val powers: PowerBook = PowerBook(),
) {
    /** How many percent of the maximum mana the passive auras hold, after the sheet's reservation efficiency. */
    fun reserved(hero: Combatant): Double =
        (passives.filter { it.skill.type == SkillType.AURA }.sumOf { it.skill.reserve } / max(0.1, 1 + hero["STOCK_RESERVATION"] / 100)).coerceIn(0.0, 100.0)

    /**
     * What the passives lay on the sheet all the time: an aura's lines stronger by the aura effect, a bonus's
     * and a trigger's standing lines as they are. [hero] is the sheet without them.
     */
    fun passiveLines(hero: Combatant): List<StatLine> = passives.filterNot { it.skill.lowLife }.flatMap { lines(it, hero) }

    /** What the low-life bonuses lay on while the hero's life is under half. */
    fun lowLifeLines(hero: Combatant): List<StatLine> = passives.filter { it.skill.lowLife }.flatMap { lines(it, hero) }

    private fun lines(passive: KitSkill, hero: Combatant): List<StatLine> {
        val level = passive.level(hero)
        return if (passive.skill.type == SkillType.AURA) passive.skill.stats.lines(level, 1 + hero["STOCK_AURA_EFFECT"] / 100)
        else passive.skill.stats.lines(level)
    }

    companion object {
        /**
         * The hero's loadout from the character's [skills] as the server keeps them, the world's [book] and
         * the flasks worn on the belt, in its order — [flasks] null where a place is empty.
         */
        fun of(skills: HeroSkills, book: SkillBook, heroClass: String, flasks: List<Flask?>, powers: PowerBook = PowerBook()): Loadout {
            fun kit(code: String?, condition: SlotCondition? = null) = code?.let(book.byCode::get)
                ?.let { KitSkill(it, skills.level(it.code).coerceAtLeast(1), condition ?: it.condition) }
            return Loadout(
                actives = skills.active.map { slot -> slot?.let { kit(it.skill, it.condition) } },
                passives = skills.passive.mapNotNull { kit(it) },
                flasks = flasks,
                curses = book.ofClass(heroClass).filter { it.type == SkillType.CURSE }.map { KitSkill(it, skills.level(it.code).coerceAtLeast(1)) },
                powers = powers,
            )
        }
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
            val added = (stats[stat] ?: 0.0) + own.filter { it.operation == ModifierOperation.ADD }.sumOf { it.value }
            val increase = own.filter { it.operation == ModifierOperation.INCREASED }.sumOf { it.value }
            val more = own.filter { it.operation == ModifierOperation.MORE }
            result[stat] = own.lastOrNull { it.operation == ModifierOperation.SET }?.value ?: if (stat in percent || stat == DAMAGE)
                more.fold(added + increase) { value, line -> (100 + value) * (1 + line.value / 100) - 100 }
            else more.fold(added * (1 + increase / 100)) { value, line -> value * (1 + line.value / 100) }
        }
        return result
    }

    /** Every damage a fighter deals, in percent more — a percent stat whether or not the tables say so. */
    const val DAMAGE = "STOCK_DAMAGE"
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
class HeroBuild(val gear: HeroGear, private val mapEffects: Map<String, Double>, private val rules: CombatRules) : HeroModel {
    private fun sheet(lines: List<StatLine>): Map<String, Double> = gear.model?.with(lines) ?: StatLines.fold(gear.stats, lines, gear.percent)

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
}

/**
 * The hero's [BodyModel] (2.78.0): the lines low life lays on and, for a spell of an element, the
 * increases of that element on the sheet — a monster has neither.
 */
interface HeroModel : BodyModel {
    val lowLife: List<StatLine>
    fun increased(stat: String, lines: List<StatLine>): Double

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
data class Draught(val life: Double, val mana: Double, val shield: Double, val lifeRate: Double, val manaRate: Double,
                   val lines: List<StatLine>, val duration: Double, val invulnerable: Double)

/**
 * A draught of this flask by [hero] at [life], mana up to [manaCap]: the recovery grows at low life by
 * its own line, the instant share comes now and the rest runs over the duration; a life flask may also
 * give mana or shield back, and any flask a sip of either.
 */
fun Flask.draught(hero: Combatant, life: Double, manaCap: Double): Draught {
    val duration = duration(hero)
    val low = life < hero.maxLife * LOW_LIFE
    val recover = recovery(hero) * if (low) 1 + own("FLASK_LOW_LIFE_RECOVERY") / 100 else 1.0
    val lifeAmount = own("FLASK_LIFE") * recover
    val manaAmount = own("FLASK_MANA") * recover
    return Draught(
        life = lifeAmount * instant,
        mana = manaAmount * instant + lifeAmount * own("FLASK_LIFE_TO_MANA") / 100 + manaCap * own("FLASK_SIP_MANA") / 100,
        shield = lifeAmount * own("FLASK_LIFE_TO_SHIELD") / 100 + hero.maxShield * own("FLASK_SIP_SHIELD") / 100,
        lifeRate = lifeAmount * (1 - instant) / duration,
        manaRate = manaAmount * (1 - instant) / duration,
        lines = effect(hero).let { scale -> lines.map { it.copy(value = it.value * scale) } },
        duration = duration,
        invulnerable = own("FLASK_INVULNERABLE"),
    )
}

/** "Low life", as a flask and a passive's answer read it: under this share of the maximum. */
const val LOW_LIFE = 0.35
