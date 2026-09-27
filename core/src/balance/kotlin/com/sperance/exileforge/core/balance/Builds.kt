package com.sperance.exileforge.core.balance

import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.HeroStance
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.character.HeroSheet
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TreeGraph
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.rules.roll.ItemInstance

/**
 * A way to play a class: what its tree walk values, which skills it slots first, and what it wears.
 *
 * A weight is matched against a line's modifier code by substring, the largest match wins; lines that
 * touch nothing a fight reads (work, rarity, light) weigh nothing, so no build buys them.
 */
enum class Archetype(val title: String, private val weights: Map<String, Double>, val skillOrder: List<SkillType>) {
    STRIKER(
        "Удар",
        mapOf("ATTACK_SPEED" to 3.0, "PHYSICAL_DAMAGE" to 3.0, "CRITICAL" to 2.5, "SKILL_DAMAGE" to 2.0, "BLEED" to 1.5, "LEECH" to 1.5,
            "STRENGTH" to 1.0, "DEXTERITY" to 1.0, "ALL_ATTRIBUTES" to 1.0, "MAXIMUM_LIFE" to 2.0, "RESISTANCE" to 1.5, "ARMOUR" to 1.0, "EVASION" to 1.0),
        listOf(SkillType.ATTACK, SkillType.WARCRY, SkillType.CURSE, SkillType.GUARD, SkillType.SPELL, SkillType.HEAL),
    ),
    ELEMENTALIST(
        "Стихии",
        mapOf("ELEMENTAL_DAMAGE" to 3.0, "FIRE_DAMAGE" to 3.0, "COLD_DAMAGE" to 3.0, "LIGHTNING_DAMAGE" to 3.0, "SPELL_DAMAGE" to 3.0,
            "CAST_SPEED" to 3.0, "CRITICAL" to 2.0, "IGNITE" to 1.5, "FREEZE" to 1.5, "SHOCK" to 1.5, "SKILL_DAMAGE" to 2.0, "INTELLIGENCE" to 1.0,
            "ALL_ATTRIBUTES" to 1.0, "MAXIMUM_MANA" to 1.0, "MANA_REGENERATION" to 1.0, "ENERGY_SHIELD" to 1.5, "MAXIMUM_LIFE" to 2.0, "RESISTANCE" to 1.5),
        listOf(SkillType.SPELL, SkillType.ATTACK, SkillType.CURSE, SkillType.WARCRY, SkillType.GUARD, SkillType.HEAL),
    ),
    BULWARK(
        "Стена",
        mapOf("MAXIMUM_LIFE" to 4.0, "ARMOUR" to 3.0, "EVASION" to 2.0, "ENERGY_SHIELD" to 2.0, "BLOCK" to 3.0, "RESISTANCE" to 3.0,
            "REGENERATION" to 2.0, "DAMAGE_REDUCTION" to 3.0, "STUN_THRESHOLD" to 1.0, "LIFE_ON_KILL" to 1.0, "ALL_ATTRIBUTES" to 1.0,
            "DAMAGE" to 1.0, "ATTACK_SPEED" to 1.0),
        listOf(SkillType.GUARD, SkillType.HEAL, SkillType.ATTACK, SkillType.SPELL, SkillType.WARCRY, SkillType.CURSE),
    ),
    CRITIC(
        "Крит",
        mapOf("CRITICAL" to 3.5, "ATTACK_SPEED" to 2.5, "CAST_SPEED" to 2.0, "DAMAGE" to 1.5, "DEXTERITY" to 1.0, "ALL_ATTRIBUTES" to 1.0,
            "COOLDOWN" to 1.0, "SKILL_LEVEL" to 1.0, "MAXIMUM_LIFE" to 2.0, "RESISTANCE" to 1.5, "EVASION" to 1.0),
        listOf(SkillType.ATTACK, SkillType.SPELL, SkillType.CURSE, SkillType.WARCRY, SkillType.GUARD, SkillType.HEAL),
    ),
    AFFLICTOR(
        "Недуги",
        mapOf("POISON" to 3.5, "BLEED" to 3.5, "IGNITE" to 3.0, "BURNING" to 3.0, "SHOCK" to 2.0, "FREEZE" to 2.0, "CHAOS_DAMAGE" to 2.5,
            "CURSE" to 2.0, "SKILL_DAMAGE" to 1.5, "ATTACK_SPEED" to 1.5, "MAXIMUM_LIFE" to 2.0, "RESISTANCE" to 1.5, "LEECH" to 1.0),
        listOf(SkillType.ATTACK, SkillType.SPELL, SkillType.CURSE, SkillType.WARCRY, SkillType.GUARD, SkillType.HEAL),
    ),
    WARDEN(
        "Энергощит",
        mapOf("ENERGY_SHIELD" to 4.0, "ENERGY_REGENERATION" to 3.0, "INTELLIGENCE" to 1.5, "SPELL_DAMAGE" to 2.0, "CAST_SPEED" to 2.0,
            "ELEMENTAL_DAMAGE" to 1.5, "MAXIMUM_MANA" to 1.0, "MANA_REGENERATION" to 1.0, "RESERVATION" to 1.0, "AURA" to 1.5,
            "SKILL_COST" to 1.0, "RESISTANCE" to 2.0),
        listOf(SkillType.SPELL, SkillType.GUARD, SkillType.CURSE, SkillType.ATTACK, SkillType.HEAL, SkillType.WARCRY),
    ),
    FARMER(
        "Фармер",
        mapOf("ITEM_QUANTITY" to 4.0, "ITEM_RARITY" to 3.0, "CHEST_QUANTITY" to 2.0, "MOVEMENT_SPEED" to 2.0, "EXPERIENCE_GAIN" to 2.0,
            "SELL_VALUE" to 1.0, "LIGHT_RADIUS" to 1.0, "FLASK" to 1.0, "DESECRATION" to 1.0, "MAXIMUM_LIFE" to 2.5, "RESISTANCE" to 2.0,
            "DAMAGE" to 1.0, "ATTACK_SPEED" to 1.0),
        listOf(SkillType.ATTACK, SkillType.SPELL, SkillType.GUARD, SkillType.HEAL, SkillType.WARCRY, SkillType.CURSE),
    );

    fun weight(code: String): Double = weights.entries.filter { code.contains(it.key) }.maxOfOrNull { it.value } ?: 0.0

    /**
     * A line's worth: its weight, against the archetype when its value is negative. A line that sets a stat
     * outright is a keystone's trade: setting it to next to nothing (a life of one) is a price the walk
     * never pays; setting it higher is worth nothing more than the walk can judge.
     */
    fun value(lines: List<Line>): Double = lines.sumOf { line ->
        val amount = line.values.firstOrNull() ?: 0.0
        when {
            line.code.startsWith(SET_PREFIX) -> if (amount <= 1) -FORBIDDEN else 0.0
            else -> weight(line.code) * if (amount < 0) -1 else 1
        }
    }

    private companion object {
        const val SET_PREFIX = "PASSIVE_SET_"
        const val FORBIDDEN = 100.0
    }
}

/** One hero the report measures: a class played one way at one level, and the seed its gear was rolled with. */
data class BuildSpec(val heroClass: String, val archetype: Archetype, val level: Int, val seed: Long) {
    val key: String get() = "$heroClass/${archetype.name}/$level"
}

/** A built hero: what it wears, walked and slotted, its sheet, and the gear a run takes. */
class BuiltHero(val spec: BuildSpec, val tree: List<TakenNode>, val items: List<ItemInstance>, val skills: HeroSkills, val sheet: HeroSheet, val gear: HeroGear)

/**
 * Makes the typical hero of a [BuildSpec]: the tree walked for the archetype, rare gear of the best
 * bases the level allows in the class's defence, and the best skills the level and attributes admit.
 */
class HeroFactory(private val index: ContentIndex) {
    private val skillRules = SkillRules(index.skills)

    fun build(spec: BuildSpec, tree: List<TakenNode> = TreePlanner(index.tree, spec.archetype).plan(heroClass(spec).startNode, index.classes.pointsTotal(spec.level)),
              skills: ((HeroSheet) -> HeroSkills)? = null): BuiltHero {
        val heroClass = heroClass(spec)
        val items = GearPlanner(index, heroClass, spec.archetype, spec.level, spec.seed).gear()
        val sheet = Sheets.calculate(index, spec.level, heroClass.code, tree, items)
        val chosen = skills?.invoke(sheet) ?: skillsFor(heroClass.code, spec.archetype, spec.level, sheet.stats)
        return BuiltHero(spec, tree, items, chosen, sheet, gear(heroClass.code, spec.level, items, chosen, sheet))
    }

    /** The class's skills at the highest level the hero may learn them, ordered as the archetype slots them. */
    fun skillsFor(heroClass: String, archetype: Archetype, level: Int, stats: Map<String, Double>): HeroSkills {
        val learned = index.skills.ofClass(heroClass).mapNotNull { skill -> learnable(skill, level, stats)?.let { skill to it } }
        val actives = learned.filter { it.first.kind == SkillKind.ACTIVE }
            .sortedWith(compareBy<Pair<SkillDefinition, Int>> { archetype.skillOrder.indexOf(it.first.type).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenByDescending { it.first.unlock })
            .take(skillRules.activeSlots(level))
        var reserve = 0.0
        val passives = learned.filter { it.first.kind == SkillKind.PASSIVE }
            .sortedWith(compareBy<Pair<SkillDefinition, Int>> { PASSIVE_ORDER.indexOf(it.first.type) }.thenByDescending { it.first.unlock })
            .filter { (skill, _) -> skill.type != SkillType.AURA || (reserve + skill.reserve <= MAX_RESERVE).also { fits -> if (fits) reserve += skill.reserve } }
            .take(skillRules.passiveSlots(level))
        return HeroSkills(learned.associate { (skill, lv) -> skill.code to lv },
            actives.map { (skill, _) -> ActiveSlot(skill.code, skill.condition) }, passives.map { it.first.code })
    }

    /** One skill alone in the first active or passive slot, for weighing it against its siblings. */
    fun alone(skill: SkillDefinition, level: Int, stats: Map<String, Double>): HeroSkills? {
        val lv = learnable(skill, level, stats) ?: return null
        return if (skill.kind == SkillKind.ACTIVE) HeroSkills(mapOf(skill.code to lv), listOf(ActiveSlot(skill.code, skill.condition)))
        else HeroSkills(mapOf(skill.code to lv), passive = listOf(skill.code))
    }

    fun gear(heroClass: String, level: Int, items: List<ItemInstance>, skills: HeroSkills, sheet: HeroSheet): HeroGear {
        val equipped = items.associateBy { it.slot }
        val flasks = Slot.FLASKS.mapIndexed { i, slot -> equipped[slot]?.let { item -> index.template(item.template)?.let { Flask.of(item, it, index, skills.flasks.getOrNull(i)) } } }
        val weapon = (equipped[Slot.WEAPON_1H] ?: equipped[Slot.WEAPON_2H])?.let { index.template(it.template)?.weaponType }
        return HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass, weapon), Loadout.of(skills, index.skills, heroClass, flasks, index.powers), index.stats.percent)
    }

    private fun learnable(skill: SkillDefinition, level: Int, stats: Map<String, Double>): Int? =
        (SkillRules.MAX_LEVEL downTo 1).firstOrNull { lv -> skillRules.unmet(skill, lv, level, stats).isEmpty() }

    private fun heroClass(spec: BuildSpec): HeroClass = requireNotNull(index.heroClass(spec.heroClass)) { "unknown class ${spec.heroClass}" }

    private companion object {
        val PASSIVE_ORDER = listOf(SkillType.AURA, SkillType.BONUS, SkillType.TRIGGER)
        /** Auras reserve at most half the mana: a hero with none left casts nothing. */
        const val MAX_RESERVE = 50.0
    }
}

/**
 * The archetype's walk of the tree: from what is taken, the path of up to [REACH] untaken nodes whose
 * value per point is best, again and again until the points run out. A mastery or an attribute node
 * takes its richest option; a taken mastery opens nothing beyond itself, as the rules say.
 */
class TreePlanner(private val graph: TreeGraph, private val archetype: Archetype) {
    fun plan(start: String, points: Int): List<TakenNode> {
        val taken = linkedMapOf(start to TakenNode(start))
        var left = points
        while (left > 0) {
            val path = bestPath(taken.keys, left) ?: break
            path.forEach { code -> taken[code] = TakenNode(code, choice(code)) }
            left -= path.sumOf { cost(it) }
        }
        return taken.values.toList()
    }

    private fun bestPath(taken: Set<String>, left: Int): List<String>? {
        val parent = HashMap<String, String?>()
        val depth = HashMap<String, Int>()
        val queue = ArrayDeque<String>()
        taken.filterNot { isMastery(it) }.flatMap { graph.neighbours(it) }.filter { it !in taken && open(it) }.distinct().forEach {
            parent[it] = null; depth[it] = 1; queue += it
        }
        while (queue.isNotEmpty()) {
            val code = queue.removeFirst()
            if (depth.getValue(code) >= REACH || isMastery(code)) continue
            graph.neighbours(code).filter { it !in taken && it !in parent && open(it) }.forEach { next ->
                parent[next] = code; depth[next] = depth.getValue(code) + 1; queue += next
            }
        }
        return parent.keys.map { end -> generateSequence(end) { parent[it] }.toList().reversed() }
            .filter { path -> path.sumOf { cost(it) } <= left }
            .maxWithOrNull(compareBy<List<String>> { path -> path.sumOf { value(it) } / path.sumOf { cost(it) } }.thenBy { -it.size })
    }

    private fun open(code: String): Boolean = graph.node(code)?.type.let { it != null && it != SkillNodeType.START && it != SkillNodeType.JEWEL_SOCKET }
    private fun isMastery(code: String) = graph.node(code)?.type == SkillNodeType.MASTERY
    private fun cost(code: String) = graph.node(code)?.cost ?: 1

    private fun value(code: String): Double {
        val node = graph.node(code) ?: return 0.0
        return archetype.value(node.lines) + (node.options.maxOfOrNull { archetype.value(it) } ?: 0.0) + STEP
    }

    private fun choice(code: String): Int? = graph.node(code)?.options?.takeIf { it.isNotEmpty() }?.let { options -> options.indices.maxBy { archetype.value(options[it]) } }

    private companion object {
        const val REACH = 4
        /** A little for every node, so a walk through lines the archetype ignores still moves on. */
        const val STEP = 0.05
    }
}

/**
 * The gear of a typical hero: in every place the non-unique base of the highest level the hero may wear
 * — the class's own weapon type, armour in the class's defences — as the best of a few rares; three
 * flasks, life, mana and quicksilver.
 */
class GearPlanner(private val index: ContentIndex, private val heroClass: HeroClass, private val archetype: Archetype, private val level: Int, private val seed: Long) {
    private val factory = ItemFactory(index)
    private val weaponType: WeaponType? = index.template(heroClass.weapon)?.weaponType

    fun gear(): List<ItemInstance> {
        val weapon = pick { it.weaponType == weaponType && it.slot in WEAPONS }
        val offHand = when {
            weapon?.slot == Slot.WEAPON_2H -> null
            weaponType == WeaponType.BOW -> pick { it.slot == Slot.QUIVER }
            else -> pick(defended { it.slot == Slot.SHIELD })
        }
        val places = listOfNotNull(weapon?.let { it to it.slot }, offHand?.let { it to it.slot }) +
            ARMOUR.mapNotNull { slot -> pick(defended { it.slot == slot })?.let { it to slot } } +
            listOf(Slot.RING to Slot.RING, Slot.RING to Slot.RING_2, Slot.AMULET to Slot.AMULET, Slot.BELT to Slot.BELT)
                .mapNotNull { (kind, place) -> pick { it.slot == kind }?.let { it to place } } +
            FLASKS.zip(Slot.FLASKS).mapNotNull { (kind, place) -> pick { it.slot == Slot.FLASK && it.code.contains(kind) }?.let { it to place } }
        return places.mapIndexed { i, (template, place) ->
            (0 until TRIES).map { k -> factory.create("sim-$i", template, Rarity.RARE, Dice((seed * 31 + i) * 97 + k)) }
                .maxBy { item -> item.rolls.sumOf { archetype.weight(it.code) } }.copy(slot = place)
        }
    }

    private fun pick(filter: (ItemTemplate) -> Boolean): ItemTemplate? =
        index.templates.values.filter { !it.unique && it.requiredLevel <= level && it.level <= level && filter(it) }
            .maxWithOrNull(compareBy<ItemTemplate> { it.level }.thenBy { it.code })

    /** Bases carrying the class's defences first; any base of the place when none does. */
    private fun defended(filter: (ItemTemplate) -> Boolean): (ItemTemplate) -> Boolean {
        val own = DEFENCES[heroClass.startNode].orEmpty()
        val fitting = index.templates.values.filter { filter(it) && it.base.any { line -> line.code in own } && it.requiredLevel <= level }
        return if (fitting.isEmpty()) filter else { template -> filter(template) && template.base.any { it.code in own } }
    }

    private companion object {
        val WEAPONS = setOf(Slot.WEAPON_1H, Slot.WEAPON_2H)
        val ARMOUR = listOf(Slot.HELMET, Slot.BODY, Slot.GLOVES, Slot.BOOTS, Slot.WINGS)
        val FLASKS = listOf("LIFE", "MANA", "QUICKSILVER")
        /** A player keeps the best of what drops: of this many rares of a base, the one richest in the archetype's lines. */
        const val TRIES = 12
        const val AR = "BASE_ARMOUR"
        const val EV = "BASE_EVASION"
        const val ES = "BASE_ENERGY_SHIELD"
        val DEFENCES = mapOf(
            "STR_START" to setOf(AR), "DEX_START" to setOf(EV), "INT_START" to setOf(ES),
            "STR_DEX_START" to setOf(AR, EV), "STR_INT_START" to setOf(AR, ES), "DEX_INT_START" to setOf(EV, ES),
            "SCION_START" to setOf(AR, EV, ES),
        )
    }
}
