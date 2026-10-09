package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapStats
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.NodeRole
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TreeAllocation
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.sheet.CombatProfile
import com.sperance.exileforge.rules.sheet.Requirements
import com.sperance.exileforge.rules.sheet.SheetCalculator
import java.util.concurrent.ConcurrentHashMap

/** Что билд даёт герою к бою: надетые вещи, взятые узлы дерева, умения и пояс. */
data class SimBuild(val items: List<ItemInstance>, val tree: List<TakenNode>, val skills: HeroSkills, val flasks: List<BeltFlask?>)

/**
 * Как герой класса собран к уровню (стратегия билда сима). Новый билд - новая реализация: сим и отчёт его подхватят.
 * [corridor] - коридор доли побед владельца для этого билда.
 */
interface BuildStrategy {
    val name: String
    val corridor: ClosedFloatingPointRange<Double>

    /** Билд класса [heroClass] на уровне [level] в акте со штрафом сопротивлений [penalty]; [kit] - общие инструменты. */
    fun build(kit: SimKit, heroClass: HeroClass, level: Int, penalty: Double): SimBuild
}

/** «Худший» билд: стартовое оружие класса, обычные вещи уровня, пустое дерево, одно стартовое умение. */
object WorstBuild : BuildStrategy {
    override val name = "worst"
    override val corridor = 0.0..0.2

    override fun build(kit: SimKit, heroClass: HeroClass, level: Int, penalty: Double): SimBuild {
        val items = kit.plain(heroClass, level)
        val starter = kit.index.skills.ofClass(heroClass.code).first { it.type.kind == SkillKind.ACTIVE && it.unlock == 1 }
        val skills = HeroSkills(learned = mapOf(starter.code to 1), active = listOf(ActiveSlot(starter.code, starter.condition)))
        return SimBuild(items, listOf(TakenNode(heroClass.startNode)), skills, kit.flasks(level, Rarity.COMMON))
    }
}

/**
 * «Средний» билд - игрок, на которого равняется баланс: из волшебных и редких вещей уровня со средними роллами на каждое место
 * берёт ту, что лучше по [SimKit.worth] (живучесть с сопротивлениями, добитыми до потолка с учётом штрафа акта, × урон), дерево -
 * жадно по той же цене (ключевые узлы - если они того стоят), умения - открытые к уровню на высшем уровне, какой он тянет.
 */
object AverageBuild : BuildStrategy {
    override val name = "average"
    override val corridor = 0.4..0.7

    override fun build(kit: SimKit, heroClass: HeroClass, level: Int, penalty: Double): SimBuild {
        val tree = kit.tree(heroClass, kit.index.classes.pointsTotal(level))
        val items = kit.outfit(heroClass, level, tree, penalty)
        val stats = kit.stats(heroClass, level, tree, items)
        return SimBuild(items, tree, kit.skills(heroClass.code, level, stats), kit.flasks(level, Rarity.MAGIC))
    }
}

/** Общие инструменты стратегий: цена листа, вещи по местам, дерево, умения по листу, пояс. */
class SimKit(val index: ContentIndex) {
    private val factory = ItemFactory(index)
    private val calculator = SheetCalculator(index)
    private val skillRules = SkillRules(index.skills)
    private val rules = index.campaign.combat
    private val orders = ConcurrentHashMap<String, List<TakenNode>>()

    /** Лист класса на уровне с деревом [tree] и вещами [items]. */
    fun stats(heroClass: HeroClass, level: Int, tree: List<TakenNode>, items: List<ItemInstance>): Map<String, Double> = calculator.hero(level, heroClass, tree, emptyList(), items).stats

    /**
     * Цена листа глазами игрока: живучесть - здоровье и щит, делённые на долю урона, что проходит сквозь сопротивления после
     * штрафа акта [penalty] (стихии - полностью, хаос - вполовину), - умноженная на урон ([CombatProfile]).
     */
    fun worth(stats: Map<String, Double>, level: Int, penalty: Double): Double {
        val body = Combatant(MapEffects.hero(stats, if (penalty > 0) mapOf(MapStats.HERO_RESIST to penalty) else emptyMap()), level, rules)
        val taken = DamageType.entries.sumOf { type -> TAKEN_WEIGHT[type]!! * (1 - body.resist(type)) } / TAKEN_WEIGHT.values.sum()
        return (body.maxLife + body.maxShield) / taken.coerceAtLeast(0.05) * CombatProfile.of(index, stats).best
    }

    private fun wearable(heroClass: HeroClass, level: Int): List<ItemTemplate> {
        val own = stats(heroClass, level, emptyList(), emptyList())
        return index.templates.values.filter {
            !it.unique && !it.corrupted && it.tables.isNotEmpty() && (it.heroClass == null || it.heroClass == heroClass.code) && Requirements.unmet(it, level, own).isEmpty()
        }
    }

    /** Места героя класса: оружие рода стартового, к нему колчан (лук) или щит (одноручное), броня, бижутерия, второе кольцо. */
    private fun places(heroClass: HeroClass): List<Slot> {
        val start = index.template(heroClass.weapon)
        val offhand = when {
            start?.weaponType == WeaponType.BOW -> Slot.QUIVER
            start?.slot == Slot.WEAPON_1H -> Slot.SHIELD
            else -> null
        }
        return listOfNotNull(start?.slot, offhand) + ARMOUR + Slot.RING_2
    }

    /** Подходит ли шаблон месту [slot] класса: оружие - рода стартового, второе кольцо - кольцо. */
    private fun fits(template: ItemTemplate, slot: Slot, heroClass: HeroClass): Boolean {
        val start = index.template(heroClass.weapon)
        return when {
            slot == Slot.RING_2 -> template.slot == Slot.RING
            slot.isWeapon -> template.slot == slot && template.weaponType == start?.weaponType
            else -> template.slot == slot
        }
    }

    private fun item(id: String, template: ItemTemplate, rarity: Rarity, slot: Slot, dice: Dice, level: Int): ItemInstance {
        val item = factory.create(id, template, rarity, dice, level = index.rules.loot.itemLevel(level))
        return item.copy(rolls = item.rolls.map { it.copy(share = AVERAGE) }, slot = slot)
    }

    /** Обычные вещи уровня: стартовое оружие и лучший по уровню шаблон каждого места, роллы средние. */
    fun plain(heroClass: HeroClass, level: Int): List<ItemInstance> {
        val wearable = wearable(heroClass, level)
        val dice = Dice(Streams.mix(SEED, level.toLong(), heroClass.code.hashCode().toLong() * 2 + 1))
        return places(heroClass).mapIndexedNotNull { n, slot ->
            val template = if (slot.isWeapon) index.template(heroClass.weapon) else wearable.filter { fits(it, slot, heroClass) }.maxWithOrNull(compareBy({ it.requiredLevel }, { it.level }, { it.code }))
            template?.let { item("g$n", it, Rarity.COMMON, slot, dice, level) }
        }
    }

    /**
     * Вещи игрока: на каждое место - [CANDIDATES] находок (волшебные и редкие из [BASES] старших шаблонов места, роллы средние),
     * надета лучшая по [worth] при уже надетом; второй проход пересматривает каждое место при всех остальных.
     */
    fun outfit(heroClass: HeroClass, level: Int, tree: List<TakenNode>, penalty: Double): List<ItemInstance> {
        val wearable = wearable(heroClass, level)
        val dice = Dice(Streams.mix(SEED, level.toLong(), heroClass.code.hashCode().toLong() * 2))
        val places = places(heroClass)
        val found = places.mapIndexed { n, slot ->
            val bases = wearable.filter { fits(it, slot, heroClass) }.sortedWith(compareByDescending<ItemTemplate> { it.requiredLevel }.thenByDescending { it.level }.thenBy { it.code }).take(BASES)
            if (bases.isEmpty()) emptyList() else List(CANDIDATES) { k -> item("g$n-$k", bases[k % bases.size], if (k % 2 == 0) Rarity.RARE else Rarity.MAGIC, slot, dice, level) }
        }
        val worn = arrayOfNulls<ItemInstance>(places.size)
        fun value(): Double = worth(stats(heroClass, level, tree, worn.filterNotNull()), level, penalty)
        repeat(PASSES) {
            found.forEachIndexed { n, candidates ->
                worn[n] = candidates.maxByOrNull { candidate ->
                    worn[n] = candidate
                    value()
                }
            }
        }
        return worn.filterNotNull()
    }

    /**
     * Дерево игрока: порядок узлов класса - жадно по приросту [worth] на очко на уровне [ORDER_LEVEL] без вещей (ключевой узел
     * берётся, только если он того стоит, гнёзда пусты - их не берут), на уровне - столько первых узлов порядка, сколько хватает
     * [points]. Порядок связен по правилам сервера ([TreeAllocation]): каждый узел примыкает к взятым раньше.
     */
    fun tree(heroClass: HeroClass, points: Int): List<TakenNode> {
        val order = orders.getOrPut(heroClass.code) { order(heroClass) }
        val graph = index.tree
        var left = points
        return order.takeWhile { taken ->
            val cost = graph.node(taken.code)?.let { graph.cost(it, heroClass.startNode) } ?: 0
            (cost <= left).also { if (it) left -= cost }
        }
    }

    private fun order(heroClass: HeroClass): List<TakenNode> {
        val graph = index.tree
        val start = heroClass.startNode
        val taken = linkedMapOf(start to TakenNode(start))
        var left = index.classes.pointsTotal(ORDER_LEVEL)
        fun value(tree: Collection<TakenNode>) = worth(stats(heroClass, ORDER_LEVEL, tree.toList(), emptyList()), ORDER_LEVEL, 0.0)
        var base = value(taken.values)
        while (left > 0) {
            val options = taken.keys.flatMap(graph::neighbours).distinct().filter { it !in taken }.mapNotNull(graph::node)
                .filter { it.type != SkillNodeType.JEWEL_SOCKET && it.roleFor(start) == NodeRole.OWN && TreeAllocation.refusal(graph, it, taken.keys, start, left) == null }
                .flatMap { node -> (if (node.options.isEmpty()) listOf(null) else node.options.indices.toList()).map { node to it } }
            val pick = options.maxWithOrNull(
                compareBy<Pair<TreeNode, Int?>>({ (node, choice) -> (value(taken.values + TakenNode(node.code, choice)) - base) / graph.cost(node, start).coerceAtLeast(1) }, { it.first.code }),
            ) ?: break
            taken[pick.first.code] = TakenNode(pick.first.code, pick.second)
            left -= graph.cost(pick.first, start)
            base = value(taken.values)
        }
        return taken.values.toList()
    }

    /**
     * Умения класса, открытые к [level]: каждое - на высшем уровне, какой тянут уровень героя и атрибуты [stats]. В активные места
     * первым - сильнейший удар без условия, дальше - по позднему открытию; пассивные - по позднему открытию.
     */
    fun skills(heroClass: String, level: Int, stats: Map<String, Double>): HeroSkills {
        val learned = index.skills.ofClass(heroClass).filter { it.unlock <= level }.mapNotNull { skill -> skillLevel(skill, level, stats)?.let { skill to it } }
        val actives = learned.filter { it.first.type.kind == SkillKind.ACTIVE }.map { it.first }
            .sortedWith(compareByDescending<SkillDefinition> { it.hit != null && it.condition == SlotCondition.READY }.thenByDescending { it.unlock })
            .take(skillRules.activeSlots(level))
        val passives = learned.filter { it.first.type.kind == SkillKind.PASSIVE }.map { it.first }.sortedByDescending { it.unlock }.take(skillRules.passiveSlots(level))
        return HeroSkills(
            learned = learned.associate { (skill, at) -> skill.code to at },
            active = actives.map { ActiveSlot(it.code, it.condition) },
            passive = passives.map { it.code },
        )
    }

    private fun skillLevel(skill: SkillDefinition, level: Int, stats: Map<String, Double>): Int? = (SkillRules.MAX_LEVEL downTo 1).firstOrNull { skillRules.unmet(skill, it, level, stats).isEmpty() }

    /** Две фляги жизни редкости [rarity]: лучший шаблон фляги жизни, какой носят на [level]. */
    fun flasks(level: Int, rarity: Rarity): List<BeltFlask?> {
        val template = index.templates.values.filter { it.slot.isFlask && !it.unique && it.code.contains(LIFE) && it.requiredLevel <= level }
            .maxWithOrNull(compareBy({ it.requiredLevel }, { it.level }, { it.code })) ?: return emptyList()
        return List(2) { n ->
            val item = factory.create("flask-$n", template, rarity, Dice(Streams.mix(SEED, level.toLong(), n.toLong())), level = index.rules.loot.itemLevel(level))
            BeltFlask.of(item.copy(rolls = item.rolls.map { it.copy(share = AVERAGE) }), template, index, null)
        }
    }

    companion object {
        const val SEED = 20_261_009L
        const val AVERAGE = 0.5

        /** Находок на место, старших шаблонов места среди них и проходов выбора. */
        const val CANDIDATES = 12
        const val BASES = 3
        const val PASSES = 2

        /** Уровень, на котором считается порядок дерева: почти все очки и узлы открыты. */
        const val ORDER_LEVEL = 70
        private const val LIFE = "LIFE"

        /** Вес доли урона каждого типа в живучести: хаос бьёт реже стихий. */
        private val TAKEN_WEIGHT = DamageType.entries.associateWith { if (it == DamageType.CHAOS) 0.5 else 1.0 }
        val ARMOUR = listOf(Slot.HELMET, Slot.BODY, Slot.GLOVES, Slot.BOOTS, Slot.WINGS, Slot.BELT, Slot.AMULET, Slot.RING)
    }
}
