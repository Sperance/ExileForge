package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.BeltFlask
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
import com.sperance.exileforge.rules.sheet.Requirements
import com.sperance.exileforge.rules.sheet.SheetCalculator

/** Что билд даёт герою к бою: надетые вещи, взятые узлы дерева, умения и пояс. */
data class SimBuild(val items: List<ItemInstance>, val tree: List<TakenNode>, val skills: HeroSkills, val flasks: List<BeltFlask?>)

/**
 * Как герой класса собран к уровню (стратегия билда сима). Новый билд - новая реализация: сим и отчёт его подхватят.
 * [corridor] - коридор доли побед владельца для этого билда.
 */
interface BuildStrategy {
    val name: String
    val corridor: ClosedFloatingPointRange<Double>

    /** Билд класса [heroClass] на уровне [level]; [kit] - общие инструменты: вещи, дерево, умения, пояс. */
    fun build(kit: SimKit, heroClass: HeroClass, level: Int): SimBuild
}

/** «Худший» билд: стартовое оружие класса, обычные вещи уровня, пустое дерево, одно стартовое умение. */
object WorstBuild : BuildStrategy {
    override val name = "worst"
    override val corridor = 0.0..0.2

    override fun build(kit: SimKit, heroClass: HeroClass, level: Int): SimBuild {
        val items = kit.outfit(heroClass, level, starter = true) { Rarity.COMMON }
        val starter = kit.index.skills.ofClass(heroClass.code).first { it.type.kind == SkillKind.ACTIVE && it.unlock == 1 }
        val skills = HeroSkills(learned = mapOf(starter.code to 1), active = listOf(ActiveSlot(starter.code, starter.condition)))
        return SimBuild(items, listOf(TakenNode(heroClass.startNode)), skills, kit.flasks(level, Rarity.COMMON))
    }
}

/**
 * «Средний» билд: волшебные и редкие вещи уровня со средними роллами, дерево - жадно по очкам уровня по атрибутам класса из
 * своей ветки, умения - открытые к уровню умения класса на высшем уровне, какой герой тянет.
 */
object AverageBuild : BuildStrategy {
    override val name = "average"
    override val corridor = 0.4..0.7

    override fun build(kit: SimKit, heroClass: HeroClass, level: Int): SimBuild {
        val dice = Dice(Streams.mix(SimKit.SEED, level.toLong(), heroClass.code.hashCode().toLong()))
        val items = kit.outfit(heroClass, level, starter = false) { if (dice.chance(0.5)) Rarity.RARE else Rarity.MAGIC }
        val tree = kit.climb(heroClass, kit.index.classes.pointsTotal(level))
        val stats = kit.stats(heroClass, level, tree, items)
        return SimBuild(items, tree, kit.skills(heroClass.code, level, stats), kit.flasks(level, Rarity.MAGIC))
    }
}

/** Общие инструменты стратегий: вещи по местам, жадное дерево, умения по листу, пояс. */
class SimKit(val index: ContentIndex) {
    private val factory = ItemFactory(index)
    private val calculator = SheetCalculator(index)
    private val skillRules = SkillRules(index.skills)

    /** Лист класса на уровне с деревом [tree] и вещами [items]. */
    fun stats(heroClass: HeroClass, level: Int, tree: List<TakenNode>, items: List<ItemInstance>): Map<String, Double> = calculator.hero(level, heroClass, tree, emptyList(), items).stats

    /**
     * Вещи по местам (как экономический сим сервера): оружие рода стартового - само стартовое при [starter], - к нему колчан
     * (лук) или щит (одноручное), броня и бижутерия - лучшие по уровню, редкость места - [rarity], роллы средние.
     */
    fun outfit(heroClass: HeroClass, level: Int, starter: Boolean, rarity: (Slot) -> Rarity): List<ItemInstance> {
        val own = stats(heroClass, level, emptyList(), emptyList())
        val wearable = index.templates.values.filter {
            !it.unique && !it.corrupted && it.tables.isNotEmpty() && (it.heroClass == null || it.heroClass == heroClass.code) && Requirements.unmet(it, level, own).isEmpty()
        }
        fun best(slot: Slot, fits: (ItemTemplate) -> Boolean = { true }) = wearable.filter { it.slot == slot && fits(it) }.maxWithOrNull(compareBy({ it.requiredLevel }, { it.level }, { it.code }))
        val start = index.template(heroClass.weapon)
        val weapon = if (starter) start else start?.let { best(it.slot) { t -> t.weaponType == it.weaponType } }
        val offhand = when {
            start?.weaponType == WeaponType.BOW -> Slot.QUIVER
            start?.slot == Slot.WEAPON_1H -> Slot.SHIELD
            else -> null
        }
        val places = listOfNotNull(weapon?.let { it to it.slot }, offhand?.let { slot -> best(slot)?.let { it to slot } }) +
            ARMOUR.mapNotNull { slot -> best(slot)?.let { it to slot } } +
            listOfNotNull(best(Slot.RING)?.let { it to Slot.RING_2 })
        val dice = Dice(Streams.mix(SEED, level.toLong(), heroClass.code.hashCode().toLong() * 2 + if (starter) 1 else 0))
        return places.mapIndexed { n, (template, slot) ->
            val item = factory.create("g$n", template, rarity(slot), dice, level = index.rules.loot.itemLevel(level))
            item.copy(rolls = item.rolls.map { it.copy(share = AVERAGE) }, slot = slot)
        }
    }

    /**
     * Жадное дерево: от стартового узла, пока хватает [points], берёт доступный по правилам сервера ([TreeAllocation]) свой
     * узел с лучшей ценой очка - строки на атрибуты класса весят [ATTRIBUTE_WEIGHT], прочие - 1, сосед узла - вполовину.
     * Ключевые узлы и гнёзда ([SKIPPED]) средний герой вслепую не берёт: ключевой узел меняет правила героя, гнездо пусто.
     */
    fun climb(heroClass: HeroClass, points: Int): List<TakenNode> {
        val graph = index.tree
        val attributes = index.skills.classesByCode[heroClass.code]?.attributes.orEmpty().toSet()
        val start = heroClass.startNode
        val taken = linkedMapOf(start to TakenNode(start))
        var left = points
        fun worth(node: TreeNode, choice: Int?): Double = (choice?.let { node.options.getOrNull(it) } ?: node.lines).sumOf { line ->
            val stats = index.modifier(line.code)?.effects?.map { it.stat }.orEmpty()
            if (stats.any { it in attributes }) ATTRIBUTE_WEIGHT else 1.0
        }
        fun choice(node: TreeNode): Int? = node.options.indices.maxByOrNull { worth(node, it) }
        while (left > 0) {
            val pick = taken.keys.flatMap(graph::neighbours).distinct().filter { it !in taken }.mapNotNull(graph::node)
                .filter { it.type !in SKIPPED && it.roleFor(start) == NodeRole.OWN && TreeAllocation.refusal(graph, it, taken.keys, start, left) == null }
                .maxWithOrNull(
                    compareBy<TreeNode>({ node ->
                        val ahead = graph.neighbours(node.code).filter { it !in taken && it != node.code }.mapNotNull(graph::node).maxOfOrNull { worth(it, choice(it)) } ?: 0.0
                        (worth(node, choice(node)) + ahead / 2) / graph.cost(node, start).coerceAtLeast(1)
                    }, { it.code }),
                ) ?: break
            taken[pick.code] = TakenNode(pick.code, choice(pick))
            left -= graph.cost(pick, start)
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
        const val ATTRIBUTE_WEIGHT = 3.0
        private const val LIFE = "LIFE"
        val SKIPPED = setOf(SkillNodeType.KEYSTONE, SkillNodeType.JEWEL_SOCKET)
        val ARMOUR = listOf(Slot.HELMET, Slot.BODY, Slot.GLOVES, Slot.BOOTS, Slot.WINGS, Slot.BELT, Slot.AMULET, Slot.RING)
    }
}
