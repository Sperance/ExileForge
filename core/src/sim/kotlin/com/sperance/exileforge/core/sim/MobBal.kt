package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.FightKinds
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.MAP_TEMPLATE
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Region
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.content.TreeAllocation
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.rules.content.WeaponType
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.run.RunWorth
import com.sperance.exileforge.rules.sheet.CombatProfile
import com.sperance.exileforge.rules.sheet.HeroBody
import com.sperance.exileforge.rules.sheet.HeroInputs
import com.sperance.exileforge.rules.sheet.Might
import com.sperance.exileforge.rules.sheet.Requirements
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.SkillProfile
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Качество вещей: кандидатов на место, доля ролла в тире, тир аффикса (-1 - как выпал, 0 - лучший открытый, 1 - на ступень ниже),
 * редкость фляг пояса; вещи, дерево и пассивные умения подобраны по цене листа, набор активных - боем ([LoadoutTuner]). Слабый билд
 * ([WEAK]) - небрежный игрок: подбирает так же, но роллы вещей - нижней четверти, редкие и волшебные вперемешку.
 *
 * Баланс монстров (отчёт, не код игры): эталонные герои по архетипам против монстров и боссов кампании и карт - реальными функциями правил
 * и настоящим боем клиента ([Battle]). Вывод - TSV; цели: обычный 2-3 действия, редкий 10-20 с, босс 50-150 с и 55-60% побед эталона над
 * стражем своего уровня ([GuardianTarget]). Быстрый сторож на выборке - `MobBalGuardTest`.
 */
enum class GearBand(val candidates: Int, val share: Double, val tierStep: Int, val rarities: List<Rarity>, val maxUniques: Int, val flasks: Rarity) {
    WEAK(12, 0.125, -1, listOf(Rarity.RARE, Rarity.MAGIC), 2, Rarity.MAGIC),
    TYPICAL(12, 0.5, -1, listOf(Rarity.RARE, Rarity.MAGIC), 2, Rarity.MAGIC),
    GOOD(24, 0.7, 1, listOf(Rarity.RARE), 4, Rarity.RARE),
    OPTIMIZED(40, 0.9, 0, listOf(Rarity.RARE), 99, Rarity.RARE),
}

/** Откуда вещи: только редкие (крафт/находки), только уникалки где есть, смесь - лучшее из обоих. */
enum class GearSource { RARE, UNIQUE, MIXED }

/** Защита по базам брони: любая (что лучше по цене), только броня, только уклонение, только энергощит. */
enum class Guard(val table: String?) { ANY(null), ARMOUR("local:armor"), EVASION("local:evasion"), ES("local:energy_shield") }

/** Умения: любые, только атаки, только чары. */
enum class Style { ANY, ATTACK, SPELL }

/** Архетип эталона; [seed] - номер набора вещей: другие кости поиска вещей того же архетипа, разброс выборки классов. */
data class Archetype(val band: GearBand, val source: GearSource = GearSource.MIXED, val guard: Guard = Guard.ANY, val style: Style = Style.ANY, val shield: Boolean = true, val seed: Int = 0) {
    val name: String get() = listOf(band.name.lowercase(), source.name.lowercase(), guard.name.lowercase(), style.name.lowercase()).joinToString("/") + (if (shield) "" else "/noshield") + if (seed == 0) "" else "/s$seed"
}

@kotlinx.serialization.Serializable
data class SavedHero(val items: List<ItemInstance>, val tree: List<TakenNode>, val skills: HeroSkills, val note: String)

val CACHE = File(System.getProperty("heroCache") ?: "build/mobbal-heroes")
val CACHE_JSON = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Готовый герой: вещи, дерево, умения, пояс и сводка листа. */
data class Hero(val heroClass: HeroClass, val level: Int, val archetype: Archetype, val build: SimBuild, val note: String)

class MobKit(val index: ContentIndex) {
    private val factory = ItemFactory(index)
    private val calculator = SheetCalculator(index)
    private val skillRules = SkillRules(index.skills)
    val tuner = LoadoutTuner(index)
    val rules = index.campaign.combat
    private val orders = ConcurrentHashMap<String, List<TakenNode>>()
    private val defenceTables = Guard.entries.mapNotNull { it.table }.toSet()

    fun stats(heroClass: HeroClass, level: Int, tree: List<TakenNode>, items: List<ItemInstance>): Map<String, Double> = calculator.hero(level, heroClass, tree, items).stats

    /**
     * Цена листа глазами игрока: эффективное здоровье против смеси ударов (физика 40% с бронёй, стихии и хаос по 15%; уклонение и блок)
     * × урон (умения [SkillProfile] + замахи оружием). Удар по броне и меткость - эталонного врага уровня ([com.sperance.exileforge.rules.sheet.ReferenceFoe]), того же,
     * что меряет Мощь и сводка героя: подбор вещей и бой видят одну защиту.
     */
    fun worth(heroClass: HeroClass, level: Int, tree: List<TakenNode>, items: List<ItemInstance>, skills: HeroSkills, penalty: Double): Double {
        val body = HeroBody.of(index, HeroInputs(level, heroClass.code, tree, items, skills = skills))
        val stats = body.stats
        val c = Combatant(MapEffects.hero(stats, if (penalty > 0) mapOf(MapStat.HERO_RESIST.code to penalty) else emptyMap()), level, rules)
        val foe = index.referenceFoe
        val evade = rules.accuracy.evaded(foe.accuracy(level), c.evasion).coerceAtMost(c.evasionCap)
        val avoid = (1 - evade) * (1 - c.block)
        val phys = 1 - c.physicalMitigation(foe.hit(level), rules.armour.factor)
        val elem = DamageType.ELEMENTS.sumOf { 1 - c.resist(it) }
        val taken = (0.4 * phys + 0.15 * elem + 0.15 * (1 - c.resist(DamageType.CHAOS))) * avoid
        val ehp = (c.maxLife + c.maxShield) / taken.coerceAtLeast(0.03)
        val growth = body.fold.growth(body.passiveLines)
        val dps = SkillProfile(index, stats, level, growth).dps(body.kit.actives) + CombatProfile.of(index, stats, level, growth).total
        return ehp * dps.coerceAtLeast(1.0)
    }

    private fun own(heroClass: HeroClass, level: Int) = stats(heroClass, level, emptyList(), emptyList())

    /** Базы, что герой [heroClass] уровня [level] наденет с деревом [tree]: требования к атрибутам - по листу класса и дерева, как у игрока до вещей. */
    private fun wearable(heroClass: HeroClass, level: Int, tree: List<TakenNode>, unique: Boolean): List<ItemTemplate> {
        val own = stats(heroClass, level, tree, emptyList())
        return index.templates.values.filter {
            it.unique == unique && it.rarity != Rarity.MYTHICAL && (unique || it.tables.isNotEmpty()) && (it.heroClass == null || it.heroClass == heroClass.code) &&
                it.dropsAt(level) && !it.slot.isFlask && it.slot != Slot.JEWEL && Requirements.unmet(it, level, own).isEmpty()
        }
    }

    private fun places(heroClass: HeroClass, shield: Boolean): List<Slot> {
        val start = index.template(heroClass.weapon)
        val offhand = when {
            start?.weaponType == WeaponType.BOW -> Slot.QUIVER
            start?.slot == Slot.WEAPON_1H && shield -> Slot.SHIELD
            else -> null
        }
        return listOfNotNull(start?.slot, offhand) + SimKit.ARMOUR + Slot.RING_2
    }

    /** Броня с защитой - только чистые базы своего вида (уникалка - по её базе). */
    private fun fits(template: ItemTemplate, slot: Slot, heroClass: HeroClass, guard: Guard): Boolean {
        val start = index.template(heroClass.weapon)
        val slotOk = when {
            slot == Slot.RING_2 -> template.slot == Slot.RING
            slot.isWeapon -> template.slot == slot && template.weaponType == start?.weaponType
            else -> template.slot == slot
        }
        if (!slotOk) return false
        val table = guard.table ?: return true
        val defences = template.tables.filter { it in defenceTables }
        return when {
            template.unique -> defenceOf(template).let { it.isEmpty() || it == setOf(table) }
            defences.isEmpty() -> true
            else -> defences == listOf(table)
        }
    }

    private fun defenceOf(template: ItemTemplate): Set<String> = template.base.mapNotNull {
        when (it.code.value) {
            "BASE_ARMOUR" -> "local:armor"
            "BASE_EVASION" -> "local:evasion"
            "BASE_ENERGY_SHIELD" -> "local:energy_shield"
            else -> null
        }
    }.toSet()

    /** Вещь по качеству [band]: тиры и доли роллов по правилу полосы. */
    private fun item(id: String, template: ItemTemplate, rarity: Rarity, slot: Slot, dice: Dice, level: Int, band: GearBand): ItemInstance {
        val itemLevel = index.rules.loot.itemLevel(level)
        val item = factory.create(id, template, if (template.unique) template.rarity else rarity, dice, level = itemLevel)
        val rolls = item.rolls.map { roll ->
            val def = index.modifier(roll.code)
            if (def == null || !roll.rolled || def.tiers.isEmpty() || band.tierStep < 0) return@map roll.copy(share = band.share)
            val open = def.tiers.withIndex().filter { it.value.level <= itemLevel }.sortedBy { it.value.level }
            if (open.isEmpty()) return@map roll.copy(share = band.share)
            val pick = open[(open.size - 1 - band.tierStep).coerceAtLeast(0)].index + 1
            val better = if (band.tierStep == 0) pick else maxOf(pick, roll.tier).let { t -> if (def.tiers[t - 1].level > def.tiers[pick - 1].level) t else pick }
            roll.copy(tier = better, share = band.share)
        }
        return item.copy(rolls = rolls, slot = slot)
    }

    fun skills(heroClass: String, level: Int, stats: Map<String, Double>, style: Style): HeroSkills {
        val learned = index.skills.ofClass(heroClass).filter { it.unlock <= level }.mapNotNull { skill -> skillLevel(skill, level, stats)?.let { skill to it } }
        fun styled(s: SkillDefinition): Boolean = when (style) {
            Style.ANY -> true
            Style.ATTACK -> s.type != SkillType.SPELL
            Style.SPELL -> s.type != SkillType.ATTACK
        }
        val forced = System.getProperty("force").orEmpty().split(',').filter { it.isNotBlank() }.toSet()
        val actives = learned.filter { it.first.type.kind == SkillKind.ACTIVE && styled(it.first) }.map { it.first }
            .sortedWith(compareByDescending<SkillDefinition> { it.code in forced }.thenByDescending { it.hit != null && it.condition == SlotCondition.READY && !it.common }.thenByDescending { it.unlock })
            .take(skillRules.activeSlots(level))
        val passives = learned.filter { it.first.type.kind == SkillKind.PASSIVE }.map { it.first }.sortedByDescending { it.unlock }.take(skillRules.passiveSlots(level))
        return HeroSkills(learned = learned.associate { (skill, at) -> skill.code to at }, active = actives.map { ActiveSlot(it.code, it.condition) }, passive = passives.map { it.code })
    }

    /**
     * Пассивные слоты: жадно по цене листа [worth] - на каждом шаге пассивное умение (бонус, аура, триггер), что сильнее
     * всего растит цену; защитные идут в расчёт наравне с уроном. Без прироста слот остаётся пустым.
     */
    fun passives(heroClass: HeroClass, level: Int, tree: List<TakenNode>, items: List<ItemInstance>, skills: HeroSkills, penalty: Double): HeroSkills {
        val pool = skills.learned.keys.mapNotNull { index.skills.byCode[it] }.filter { it.type.kind == SkillKind.PASSIVE }.map { it.code }
        val chosen = mutableListOf<String>()
        fun value(passive: List<String>) = worth(heroClass, level, tree, items, skills.copy(passive = passive), penalty)
        var best = value(emptyList())
        repeat(skillRules.passiveSlots(level)) {
            val (pick, score) = pool.filter { it !in chosen }.map { it to value(chosen + it) }.maxByOrNull { it.second } ?: return@repeat
            if (score <= best) return@repeat
            chosen += pick
            best = score
        }
        return skills.copy(passive = chosen)
    }

    private fun skillLevel(skill: SkillDefinition, level: Int, stats: Map<String, Double>): Int? = (SkillRules.MAX_LEVEL downTo 1).firstOrNull { !skillRules.unmet(skill, it, level, stats) }

    /** Дерево: порядок узлов жадно по [worth] на уровне 70 со стилем; на уровне - сколько хватает очков. */
    fun tree(heroClass: HeroClass, points: Int, style: Style): List<TakenNode> {
        val order = orders.getOrPut("${heroClass.code}/$style") { order(heroClass, style) }
        val graph = index.tree
        var left = points
        return order.takeWhile { taken ->
            val cost = graph.node(taken.code)?.let { graph.cost(it) } ?: 0
            (cost <= left).also { if (it) left -= cost }
        }
    }

    private fun order(heroClass: HeroClass, style: Style): List<TakenNode> {
        val graph = index.tree
        val start = heroClass.startNode
        val taken = linkedMapOf(start to TakenNode(start))
        var left = index.classes.pointsTotal(ORDER_LEVEL)
        val skills = skills(heroClass.code, ORDER_LEVEL, own(heroClass, ORDER_LEVEL), style)
        fun value(tree: Collection<TakenNode>) = worth(heroClass, ORDER_LEVEL, tree.toList(), emptyList(), skills, 0.0)
        var base = value(taken.values)
        while (left > 0) {
            val options = taken.keys.flatMap(graph::neighbours).distinct().filter { it !in taken }.mapNotNull(graph::node)
                .filter { it.type != SkillNodeType.JEWEL_SOCKET && TreeAllocation.refusal(graph, it, taken.keys, start, left) == null }
                .flatMap { node -> (if (node.options.isEmpty()) listOf(null) else node.options.indices.toList()).map { node to it } }
            val pick = options.maxWithOrNull(
                compareBy<Pair<TreeNode, Int?>>({ (node, choice) -> (value(taken.values + TakenNode(node.code, choice)) - base) / graph.cost(node).coerceAtLeast(1) }, { it.first.code }),
            ) ?: break
            taken[pick.first.code] = TakenNode(pick.first.code, pick.second)
            left -= graph.cost(pick.first)
            base = value(taken.values)
        }
        return taken.values.toList()
    }

    fun hero(heroClass: HeroClass, level: Int, archetype: Archetype, penalty: Double): Hero {
        val band = archetype.band
        val dice = Dice(Streams.mix(SimKit.SEED, level.toLong() * 31 + archetype.name.hashCode().toLong(), heroClass.code.hashCode().toLong()))
        val tree = tree(heroClass, index.classes.pointsTotal(level), archetype.style)
        val rareTemplates = wearable(heroClass, level, tree, unique = false)
        val uniqueTemplates = if (archetype.source == GearSource.RARE) emptyList() else wearable(heroClass, level, tree, unique = true)
        val places = places(heroClass, archetype.shield)
        val found = places.mapIndexed { n, slot ->
            val bases = rareTemplates.filter { fits(it, slot, heroClass, archetype.guard) }.sortedWith(compareByDescending<ItemTemplate> { it.requiredLevel }.thenByDescending { it.level }.thenBy { it.code }).take(SimKit.BASES)
            val rares = if (bases.isEmpty()) emptyList() else List(band.candidates) { k -> item("g$n-$k", bases[k % bases.size], band.rarities[k % band.rarities.size], slot, dice, level, band) }
            val uniques = uniqueTemplates.filter { fits(it, slot, heroClass, archetype.guard) }.mapIndexed { k, t -> item("u$n-$k", t, t.rarity, slot, dice, level, band) }
            when (archetype.source) {
                GearSource.RARE -> rares
                GearSource.UNIQUE -> uniques.ifEmpty { rares }
                GearSource.MIXED -> rares + uniques
            }
        }
        val worn = arrayOfNulls<ItemInstance>(places.size)
        var skills = skills(heroClass.code, level, own(heroClass, level), archetype.style)
        fun value(): Double = worth(heroClass, level, tree, worn.filterNotNull(), skills, penalty)
        repeat(SimKit.PASSES) {
            found.forEachIndexed { n, candidates ->
                val others = worn.withIndex().count { (i, w) -> i != n && w != null && isUnique(w) }
                val cap = if (archetype.source == GearSource.UNIQUE) 99 else band.maxUniques
                worn[n] = candidates.filter { !isUnique(it) || others < cap }.maxByOrNull { candidate ->
                    worn[n] = candidate
                    value()
                }
            }
            skills = skills(heroClass.code, level, stats(heroClass, level, tree, worn.filterNotNull()), archetype.style)
        }
        val items = worn.filterNotNull()
        val flasks = flasks(level, band.flasks)
        val uniqueCount = items.count { index.template(it.template)?.unique == true }
        val passives = passives(heroClass, level, tree, items, skills, penalty)
        val tuned = tuner.tune(Hero(heroClass, level, archetype, SimBuild(items, tree, passives, flasks), ""))
        return tuned.copy(note = "uniques=$uniqueCount skills=${tuned.build.skills.active.filterNotNull().joinToString("+") { it.skill }} passives=${tuned.build.skills.passive.filterNotNull().joinToString("+")}")
    }

    /**
     * Эталон [hero] на уровне [level] (не ниже его): те же вещи, а дерево, умения и фляги - уровня [level]. Поиск вещей - самая дорогая
     * часть постройки, быстрый сторож (`MobBalGuardTest`) строит вещи на сетке уровней и переносит их на уровень зоны.
     *
     * Набор, подобранный боем на уровне вещей ([LoadoutTuner]), остаётся; новые слоты уровня - по правилу.
     */
    fun relevel(hero: Hero, level: Int): Hero {
        require(level >= hero.level) { "items of level ${hero.level} cannot be worn below it, at $level" }
        if (level == hero.level) return hero
        val tree = tree(hero.heroClass, index.classes.pointsTotal(level), hero.archetype.style)
        val ruled = skills(hero.heroClass.code, level, stats(hero.heroClass, level, tree, hero.build.items), hero.archetype.style)
        val kept = hero.build.skills.active.filterNotNull().filter { it.skill in ruled.learned }
        val actives = (kept + ruled.active.filterNotNull().filter { slot -> kept.none { it.skill == slot.skill } }).take(skillRules.activeSlots(level))
        val passive = (hero.build.skills.passive.filterNotNull().filter { it in ruled.learned } + ruled.passive.filterNotNull()).distinct().take(skillRules.passiveSlots(level))
        val skills = ruled.copy(active = actives, passive = passive)
        val build = SimBuild(hero.build.items, tree, skills, flasks(level, hero.archetype.band.flasks))
        return hero.copy(level = level, build = build, note = hero.note + " gear@${hero.level}")
    }

    private fun isUnique(item: ItemInstance) = index.template(item.template)?.unique == true

    fun flasks(level: Int, rarity: Rarity): List<BeltFlask?> {
        val template = index.templates.values.filter { it.slot.isFlask && !it.unique && it.code.contains("LIFE") && it.requiredLevel <= level }
            .maxWithOrNull(compareBy({ it.requiredLevel }, { it.level }, { it.code })) ?: return emptyList()
        return List(2) { n ->
            val item = factory.create("flask-$n", template, rarity, Dice(Streams.mix(SimKit.SEED, level.toLong(), n.toLong())), level = index.rules.loot.itemLevel(level))
            BeltFlask.of(item.copy(rolls = item.rolls.map { it.copy(share = SimKit.AVERAGE) }), template, index, null)
        }
    }

    companion object {
        const val ORDER_LEVEL = 70
    }
}

/** Бой одного героя против врагов: снимок героя, как его снимает поход; [effects] - строки карты и штраф акта. */
class Arena(private val index: ContentIndex, private val hero: Hero, effects: Map<String, Double>) {
    private val rules = index.campaign.combat
    val phases = PhaseFoes(index, rules)
    private val loadout: Loadout
    private val gear: HeroGear
    val body: Combatant
    private val pools: HeroPools
    private val build: HeroBuild

    init {
        val b = hero.build
        val sheet = Sheets.calculate(index, hero.level, hero.heroClass.code, b.tree, b.items)
        loadout = Loadout.of(b.skills, index, hero.heroClass.code, b.flasks, sheet.stats, index.powers, index.rules.charges)
        gear = HeroGear(sheet.stats, hero.level, sheet.model, HeroStance.of(hero.heroClass.code), loadout, index.stats.percent)
        build = HeroBuild(gear, effects, rules)
        body = build.body
        pools = HeroPools(body.maxLife, loadout.reserve(ReservePool.MANA, body).free, loadout.flasks.map { it?.sheet?.maxCharges ?: 0.0 }, loadout.flasks.map { 0.0 }, loadout.flasks.map { DraughtRate() })
    }

    val damage: Double get() = CombatProfile.of(index, body.stats, hero.level).total

    fun battle(foes: List<Foe>, seed: Long, retinue: Boolean): Battle {
        val all = if (retinue) phases.withRetinue(foes, Dice(seed)) else foes
        return Battle(body, all, rules, index.rules.fight, pools.life, Random(seed), gear.stance, kit = loadout, model = build, pools = pools, percent = gear.percent, cleared = false, kind = FightKinds.expedition(all))
    }

    /** Один бой против [foes] до [cap] секунд на костях [seed]. */
    fun fight(foes: List<Foe>, seed: Long, cap: Double, retinue: Boolean): Fought {
        val battle = battle(foes, seed, retinue)
        var firstKill = -1.0
        while (battle.outcome == null && battle.time < cap) {
            battle.advance(0.25)
            if (firstKill < 0 && !battle.foeFighters[0].alive) firstKill = battle.time
        }
        val events = battle.events
        val death = events.indexOfFirst { it.actor == Side.HERO && it.foe == 0 && it.monsterLife <= 0.0 && it.damage > 0 }.let { if (it < 0) events.size else it + 1 }
        val actions = events.subList(0, death).count { it.actor == Side.HERO && !it.onSelf && it.foe == 0 && (it.action == Action.ATTACK || it.action == Action.SKILL) }
        val taken = events.filter { it.actor == Side.MONSTER && it.damage > 0 && !it.onSelf }.sumOf { it.damage }
        val strongest = events.filter { it.actor == Side.MONSTER && it.ailment == null && !it.onSelf }.maxOfOrNull { it.damage } ?: 0.0
        val dealt = events.filter { it.actor == Side.HERO && it.foe == 0 && !it.onSelf }.sumOf { it.damage }
        return Fought(battle.outcome, battle.time, firstKill, actions, taken, 1 - battle.heroLife / body.maxLife.coerceAtLeast(1.0), strongest / (body.maxLife + body.maxShield).coerceAtLeast(1.0), battle.foeFighters[0].restored, dealt)
    }
}

/**
 * Итог боя; [maxHit] - сильнейший удар врага долей запаса героя (здоровье и щит); [restored] - сколько здоровья реген и вампиризм
 * вернули первому врагу, [dealt] - урон героя по нему за бой.
 */
data class Fought(val outcome: Outcome?, val time: Double, val firstKill: Double, val actions: Int, val taken: Double, val lifeLost: Double, val maxHit: Double = 0.0, val restored: Double = 0.0, val dealt: Double = 0.0)

/** Одна строка результата. */
data class Row(val values: List<Any>) {
    fun tsv(): String = values.joinToString("\t") { v -> if (v is Double) String.format(Locale.ROOT, "%.3f", v) else v.toString() }
}

/**
 * Готовые эталоны баланса: постройка героя (поиск вещей, дерево, набор умений боем) - самая дорогая часть прогона, поэтому герой
 * строится один раз на ключ «класс, уровень, архетип, штраф» (параллельные запросы одного героя ждут одну постройку). [cache] -
 * папка готовых героев (null - строить, ничего не писать; файл ложится целиком - через временный);
 * сторожа тестов делят один пул на прогон (эталон уровня зоны кампании - тот же на карте, в Бездне, Скверне и Разломе).
 */
class HeroPool(val kit: MobKit, private val cache: File? = CACHE) {
    private val heroes = ConcurrentHashMap<String, Hero>()

    /** `-Pretune=1` подбирает набор умений заново боем на тех же вещах. */
    fun hero(heroClass: HeroClass, level: Int, archetype: Archetype, penalty: Double): Hero = heroes.computeIfAbsent("${heroClass.code}/$level/${archetype.name}/$penalty") {
        val key = "${heroClass.code}_${level}_${archetype.name.replace('/', '-')}_${penalty.toInt()}" + System.getProperty("force").orEmpty().let { if (it.isBlank()) "" else "_F$it" }
        val file = cache?.let { File(it, "$key.json") } ?: return@computeIfAbsent kit.hero(heroClass, level, archetype, penalty)
        if (file.exists()) {
            val saved = CACHE_JSON.decodeFromString(SavedHero.serializer(), file.readText())
            val hero = Hero(heroClass, level, archetype, SimBuild(saved.items, saved.tree, saved.skills, kit.flasks(level, archetype.band.flasks)), saved.note)
            if (System.getProperty("retune") == "1") kit.tuner.tune(hero) else hero
        } else {
            kit.hero(heroClass, level, archetype, penalty).also { hero ->
                file.parentFile.mkdirs()
                val part = File(file.parentFile, "${file.name}.${Thread.currentThread().id}.part")
                part.writeText(CACHE_JSON.encodeToString(SavedHero.serializer(), SavedHero(hero.build.items, hero.build.tree, hero.build.skills, hero.note)))
                part.renameTo(file)
            }
        }
    }
}

/** Прогоны баланса: [fights] боёв на [rolls] бросков босса; эталоны - из пула [heroes]. */
class MobBal(val index: ContentIndex, private val fights: Int, private val rolls: Int, private val monsterSamples: Int, private val heroes: HeroPool = HeroPool(MobKit(index))) {
    val kit = heroes.kit
    private val classes = index.classes.classes.mapNotNull { index.heroClass(it.code) }

    fun hero(heroClass: HeroClass, level: Int, archetype: Archetype, penalty: Double): Hero = heroes.hero(heroClass, level, archetype, penalty)

    fun classesOf(only: Set<String>) = classes.filter { only.isEmpty() || it.code in only }

    /** Стражи кампании [count] ближайших зон не выше уровня [level] (первая - самая близкая). */
    fun nearZones(level: Int, count: Int): List<Zone> {
        val guarded = index.campaign.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null }
        return guarded.filter { it.level <= level }.sortedWith(compareByDescending<Zone> { it.level }.thenBy { it.code.value }).take(count).ifEmpty { listOf(guarded.minBy { it.level }) }
    }

    /**
     * Урон эталона класса [heroClass] уровня [level] в бою (выравнивание классов): вещи источника [source] полосы уровня
     * ([heroBand]), штраф сопротивлений ближайшей зоны, набор умений подобран боем ([LoadoutTuner]).
     */
    fun classDamage(heroClass: HeroClass, level: Int, source: GearSource, seed: Int = 0): Pair<Hero, LoadoutTuner.Damage> {
        val zone = nearZones(level, 1).first()
        val hero = hero(heroClass, level, Archetype(heroBand(level), source, seed = seed), index.campaign.resistPenalty(zone.code, false))
        return hero to kit.tuner.damage(hero)
    }

    /** Ваал-зоны стражей порчи: у каждого стража - его зоны низшего, среднего и высшего уровня. */
    fun vaalZones(): List<Zone> = index.campaign.zones.filter { it.corrupted.value.isNotBlank() }.groupBy { it.corrupted }.values.flatMap { own ->
        val sorted = own.sortedBy { it.level }
        listOf(sorted.first(), sorted[sorted.size / 2], sorted.last()).distinct()
    }.mapNotNull { com.sperance.exileforge.core.campaign.VaalZones.zone(it, index.campaign.vaal) }

    /** Зоны со стражем, чьи карты несут тир (`maps.tiers.fromLevel`): стражи карт своего уровня. */
    fun mapZones(): List<Zone> {
        val from = index.campaign.maps.tiers?.fromLevel ?: return emptyList()
        return index.campaign.zones.filter { it.level >= from && it.boss.value.isNotBlank() && index.monster(it.boss) != null }
    }

    /**
     * Эталон своего уровня на карте тира [tier] зоны [zone]: уровень героя - уровень района карты, вещи полосы
     * этого уровня ([campaignBand]), штраф сопротивлений карты.
     */
    fun mapHero(heroClass: HeroClass, zone: Zone, tier: Int): Hero {
        val level = index.campaign.maps.areaLevel(zone.level, tier)
        return hero(heroClass, level, Archetype(campaignBand(level)), index.campaign.resistPenalty(zone.code, true))
    }

    /** Стиль, что класс умеет: чары только у тех, у кого есть чары с ударом; атаки - у кого есть атаки. */
    fun styles(heroClass: HeroClass): List<Style> {
        val skills = index.skills.ofClass(heroClass.code)
        val attack = skills.any { it.type == SkillType.ATTACK }
        val spell = skills.any { it.type == SkillType.SPELL && it.hit != null }
        return listOfNotNull(Style.ATTACK.takeIf { attack }, Style.SPELL.takeIf { spell })
    }

    /** Карта-предмет тира [tier] редкости [rarity] зоны [zone]: настоящие строки, тир и угрозы - правилами добычи. */
    fun mapEffects(zone: Zone, tier: Int, rarity: Rarity, seed: Long, heroLevel: Int): Pair<Map<String, Double>, com.sperance.exileforge.rules.roll.ActiveMap?> {
        if (tier <= 0) return emptyMap<String, Double>() to null
        val loot = LootRoller(index, heroLevel)
        val dice = Dice(seed)
        val area = index.campaign.maps.areaLevel(zone.level, tier)
        val base = index.template(MAP_TEMPLATE) ?: error("no map template")
        val map = ItemFactory(index).create("map", base, rarity, dice, level = area).also {
            it.mapZone = zone.code
            it.mapTier = tier
        }
        loot.mapPerils(map, dice)
        val effects = loot.mapEffects(map)
        return effects to loot.activeMap(zone.code, effects, rarity, tier)
    }

    private fun run(zone: Zone, hero: Hero, seed: Long, active: com.sperance.exileforge.rules.roll.ActiveMap?) = Run(index, zone, seed, RunContext(hero.heroClass.code, hero.level, active = active))

    private fun heroEffects(zone: Zone, effects: Map<String, Double>, onMap: Boolean): Map<String, Double> {
        val penalty = index.campaign.resistPenalty(zone.code, onMap)
        val out = effects.toMutableMap()
        if (penalty > 0) out.merge(MapStat.HERO_RESIST.code, penalty, Double::plus)
        return out
    }

    /**
     * Босс зоны [zone] (на карте тира [tier], [rarity] - редкость карты): [rolls] бросков босса (каждый - своя карта при тире) ×
     * fights/rolls боёв. Время - секунды; урон по герою - доля пула в секунду; два последних столбца - сколько здоровья реген и
     * вампиризм вернули боссу и сколько урона нанёс ему герой, в среднем за бой.
     */
    fun boss(zone: Zone, hero: Hero, tier: Int, rarity: Rarity, cap: Double, extra: Map<String, Double> = emptyMap()): Row {
        var wins = 0
        var n = 0
        var winTime = 0.0
        var allTime = 0.0
        var taken = 0.0
        var timeouts = 0
        var bossLife = 0.0
        var might = 0.0
        var heroPool = 0.0
        var heroDmg = 0.0
        var restored = 0.0
        var dealt = 0.0
        repeat(rolls) { r ->
            val (rolled, active) = mapEffects(zone, tier, rarity, SimKit.SEED + 7919L * r + zone.level, hero.level)
            val effects = MapEffects.sum(rolled, extra)
            val run = run(zone, hero, SimKit.SEED + r, active)
            val arena = Arena(index, hero, heroEffects(zone, effects, active != null))
            val spawns = Spawns(index, run)
            val boss = spawns.boss(zone, MapEffects.buffs(effects), MapEffects.bossBuffs(effects)) ?: return@repeat
            val level = run.levelOf(zone)
            val foe = arena.phases.foe(boss, level)
            bossLife += foe.body.maxLife + foe.body.maxShield
            might += Might.of(index, boss.stats, level).value
            heroPool += arena.body.maxLife + arena.body.maxShield
            heroDmg += arena.damage
            repeat(fights / rolls) { i ->
                val f = arena.fight(listOf(foe), 9_173L + i + 1000L * r, cap, retinue = true)
                n++
                allTime += f.time
                taken += f.taken / (arena.body.maxLife + arena.body.maxShield).coerceAtLeast(1.0) / f.time.coerceAtLeast(0.1)
                restored += f.restored
                dealt += f.dealt
                if (f.outcome == Outcome.WIN) {
                    wins++
                    winTime += f.time
                }
                if (f.outcome == null) timeouts++
            }
        }
        val k = rolls.toDouble()
        return Row(
            listOf(
                "boss", zone.code.value, zone.boss.value, zone.level, index.campaign.maps.areaLevel(zone.level, tier), tier, rarity.name, hero.heroClass.code, hero.level, hero.archetype.name,
                wins.toDouble() / n.coerceAtLeast(1), if (wins > 0) winTime / wins else -1.0, allTime / n.coerceAtLeast(1), timeouts, taken / n.coerceAtLeast(1),
                bossLife / k, might / k, heroPool / k, heroDmg / k, n, hero.note, restored / n.coerceAtLeast(1), dealt / n.coerceAtLeast(1),
            ),
        )
    }

    /** Урон героя в бою: по одной цели (редкий манекен зоны, 30 с) и по стае из трёх обычных манекенов (20 с), в секунду. */
    fun offense(zone: Zone, hero: Hero): Pair<Double, Double> {
        val run = run(zone, hero, SimKit.SEED + 3, null)
        val arena = Arena(index, hero, heroEffects(zone, emptyMap(), false))
        val spawns = Spawns(index, run)
        fun mob(rarity: MonsterRarity, s: Int): com.sperance.exileforge.rules.roll.RolledMonster {
            val dice = Dice(Streams.mix(SimKit.SEED, zone.level.toLong() * 977 + s, rarity.ordinal.toLong()))
            return spawns.skilled(run.monsters.roll(zone, zone.monsters[s % zone.monsters.size], run.pool, dice, 0, index.campaign.rarity(rarity), run.areaLevel), dice)
        }
        val rare = mob(MonsterRarity.RARE, 0)
        val single = Prober.probe(index, arena, rare, run.areaLevel, 7_000L).let { it.dps + it.lifeShare * it.life }
        val pack = (0 until 3).map { s -> arena.phases.foe(Prober.dummy(mob(MonsterRarity.NORMAL, s), 100.0), run.areaLevel) }
        val b = arena.battle(pack, 7_100L, false)
        while (b.time < 20.0 && b.outcome == null) b.advance(0.25)
        val dealt = b.events.filter { it.actor == Side.HERO && !it.onSelf }.sumOf { it.damage }
        return single to dealt / b.time.coerceAtLeast(0.1)
    }

    /**
     * Заход целиком: проход клиента без игрока (`ExpeditionRun` + [Walkthrough], бои настоящими стаями и стражем)
     * даёт секунды боёв; ходьба - жадный маршрут по проходимым клеткам карты `MapGenerator` (вход → ближайшая стая → … → выход/босс)
     * со скоростью героя; опыт - `LootRoller.experience` всех монстров стай и стража зоны.
     */
    fun runtime(zone: Zone, hero: Hero, seed: Long, scale: Double): Row {
        val b = hero.build
        val sheet = Sheets.calculate(index, hero.level, hero.heroClass.code, b.tree, b.items)
        val loadout = Loadout.of(b.skills, index, hero.heroClass.code, b.flasks, sheet.stats, index.powers, index.rules.charges)
        val gear = HeroGear(sheet.stats, hero.level, sheet.model, HeroStance.of(hero.heroClass.code), loadout, index.stats.percent)
        val run = Run(index, zone, seed, RunContext(hero.heroClass.code, hero.level))
        val journal = com.sperance.exileforge.core.campaign.RunJournal("run-$seed", "hero", zone.code.value)
        val state = com.sperance.exileforge.core.model.campaign.CampaignState(chests = mapOf(zone.code.value to com.sperance.exileforge.rules.roll.ChestWindow(Long.MAX_VALUE, 2)))
        val expedition = com.sperance.exileforge.core.campaign.run.ExpeditionRun.start(index, zone, run, journal, gear, state, 0L, 0.0, hero.level, { 0L })
        val walk = Walkthrough(expedition)
        var guard = 0
        while (guard++ < 400_000 && walk.step(0.1)) Unit
        val phase = expedition.hud.value.phase.name
        val share = expedition.share()
        val rules = index.campaign.expedition
        val layout = com.sperance.exileforge.core.campaign.MapGenerator.generate(seed, zone.biome, run.count, rules.terrain, zone.size, rules.minChasm, rules.minPool, rules.shapeOf(zone.biome).shape)
        fun bfs(from: com.sperance.exileforge.core.campaign.Cell): IntArray {
            val w = layout.width
            val dist = IntArray(w * layout.height) { -1 }
            val q = ArrayDeque<Int>()
            val gate = layout.gate.map { it.y * w + it.x }.toSet()
            dist[from.y * w + from.x] = 0
            q.add(from.y * w + from.x)
            while (q.isNotEmpty()) {
                val i = q.removeFirst()
                val x = i % w
                val y = i / w
                for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nx = x + dx
                    val ny = y + dy
                    val j = ny * w + nx
                    if (nx in 0 until w && ny in 0 until layout.height && dist[j] < 0 && (layout.walkable(nx, ny) || j in gate)) {
                        dist[j] = dist[i] + 1
                        q.add(j)
                    }
                }
            }
            return dist
        }
        var at = layout.start
        val left = layout.spawns.toMutableList()
        var cells = 0.0
        while (left.isNotEmpty()) {
            val d = bfs(at)
            val next = left.minBy { c -> d[c.y * layout.width + c.x].let { if (it < 0) Int.MAX_VALUE else it } }
            val step = d[next.y * layout.width + next.x]
            cells += if (step < 0) kotlin.math.hypot((next.x - at.x).toDouble(), (next.y - at.y).toDouble()) else step.toDouble()
            at = next
            left.remove(next)
        }
        cells += bfs(at)[layout.exit.y * layout.width + layout.exit.x].let { if (it < 0) 0 else it }
        val speed = rules.heroSpeed * (1 + (sheet.stats["STOCK_MOVEMENT_SPEED"] ?: 0.0) / 100).coerceIn(0.5, 2.5)
        val loot = LootRoller(index, hero.level)
        val mobsXp = (0 until run.count).sumOf { i -> run.spawn(i).pack.sumOf { m -> loot.experience(index.monster(m.code)!!, run.areaLevel, index.campaign.rarity(m.rarity), 0.0, hero.level) } }
        val bossXp = index.monster(zone.boss)?.let { loot.experience(it, run.areaLevel, index.campaign.rarity(MonsterRarity.UNIQUE), 0.0, hero.level) } ?: 0.0
        val monsters = (0 until run.count).sumOf { run.spawn(it).pack.size }
        return Row(
            listOf(
                "runtime", zone.code.value, zone.level, zone.size, rules.shapeOf(zone.biome).shape.name, scale, run.count, monsters, hero.heroClass.code, hero.level, hero.archetype.band.name,
                share.seconds, cells / speed, cells, share.kills, share.bosses, share.deaths, phase, mobsXp, bossXp,
            ),
        )
    }

    /** Зонд героя против целей зоны: обычные/волшебные/редкие ([monsterSamples] кодов) и босс ([rolls] бросков). */
    fun probe(zone: Zone, hero: Hero, tier: Int, mapRarity: Rarity): List<Row> {
        val (run, arena, probes) = probes(zone, hero, tier, mapRarity)
        return probes.map { (kind, list) ->
            fun med(f: (Probe) -> Double) = list.map(f).sorted().let { it[it.size / 2] }
            Row(
                listOf(
                    "probe", zone.code.value, kind, zone.level, run.areaLevel, tier, mapRarity.name, hero.heroClass.code, hero.level, hero.archetype.name,
                    med { it.life }, med { it.dps }, med { it.perAction }, med { it.ttd }, med { it.actionsPerSecond }, arena.body.maxLife + arena.body.maxShield, list.size, med { it.seconds }, med { it.lifeShare }, med { it.actions }, med { it.regen }, hero.note,
                ),
            )
        }
    }

    /** Темп зоны (сторож баланса): медианы действий героя на обычного монстра, секунд на редкого и на стража. */
    fun tempo(zone: Zone, hero: Hero): Tempo {
        val probes = probes(zone, hero, 0, Rarity.COMMON).third
        fun med(kind: String, f: (Probe) -> Double) = probes.getValue(kind).map(f).sorted().let { it[it.size / 2] }
        return Tempo(med(MonsterRarity.NORMAL.name) { it.actions }, med(MonsterRarity.RARE.name) { it.seconds }, med(BOSS) { it.seconds })
    }

    private fun probes(zone: Zone, hero: Hero, tier: Int, mapRarity: Rarity): Triple<Run, Arena, Map<String, List<Probe>>> {
        val (effects, active) = mapEffects(zone, tier, mapRarity, SimKit.SEED + 104_729L + zone.level, hero.level)
        val run = run(zone, hero, SimKit.SEED + 3, active)
        val arena = Arena(index, hero, heroEffects(zone, effects, active != null))
        val spawns = Spawns(index, run)
        val buffs = MapEffects.buffs(effects)
        val out = linkedMapOf<String, List<Probe>>()
        listOf(MonsterRarity.NORMAL, MonsterRarity.MAGIC, MonsterRarity.RARE).forEach { rarity ->
            val rule = index.campaign.rarity(rarity)
            out[rarity.name] = List(monsterSamples) { s ->
                val dice = Dice(Streams.mix(SimKit.SEED, zone.level.toLong() * 977 + s, rarity.ordinal.toLong()))
                val code = zone.monsters[s % zone.monsters.size]
                val m = spawns.skilled(spawns.buffed(run.monsters.roll(zone, code, run.pool, dice, 0, rule, run.areaLevel), buffs), dice)
                Prober.probe(index, arena, m, run.areaLevel, 7_000L + s)
            }
        }
        out[BOSS] = bossProbes(zone, hero, tier, mapRarity)
        return Triple(run, arena, out)
    }

    /** Зонды стража зоны [zone] ([rolls] бросков босса со строками карты тира [tier]): урон героя по нему и время до смерти героя. */
    fun bossProbes(zone: Zone, hero: Hero, tier: Int = 0, mapRarity: Rarity = Rarity.COMMON): List<Probe> = List(rolls) { r ->
        val (fx, act) = mapEffects(zone, tier, mapRarity, SimKit.SEED + 7919L * r + zone.level, hero.level)
        val rr = run(zone, hero, SimKit.SEED + r, act)
        val a = Arena(index, hero, heroEffects(zone, fx, act != null))
        val boss = Spawns(index, rr).boss(zone, MapEffects.buffs(fx), MapEffects.bossBuffs(fx))!!
        Prober.probe(index, a, boss, rr.levelOf(zone), 8_000L + r)
    }

    /**
     * Монстры зоны по редкости [rarity] 1 на 1: [monsterSamples] монстров (код и строки - кости), бой до смерти одной из сторон.
     * Время убийства, действий героя до убийства, урон по герою (доля пула в секунду), смерти героя.
     */
    fun monsters(zone: Zone, hero: Hero, tier: Int, mapRarity: Rarity, rarity: MonsterRarity, cap: Double, extra: Map<String, Double> = emptyMap()): Row {
        val (rolled, active) = mapEffects(zone, tier, mapRarity, SimKit.SEED + 104_729L + zone.level, hero.level)
        val effects = MapEffects.sum(rolled, extra)
        val run = run(zone, hero, SimKit.SEED + 3, active)
        val arena = Arena(index, hero, heroEffects(zone, effects, active != null))
        val spawns = Spawns(index, run)
        val buffs = MapEffects.buffs(effects)
        val rule = index.campaign.rarity(rarity)
        val kills = mutableListOf<Double>()
        val actions = mutableListOf<Int>()
        var taken = 0.0
        var deaths = 0
        var life = 0.0
        var lifeLost = 0.0
        repeat(monsterSamples) { s ->
            val dice = Dice(Streams.mix(SimKit.SEED, zone.level.toLong() * 977 + s, rarity.ordinal.toLong()))
            val code = zone.monsters[s % zone.monsters.size]
            val rolled = run.monsters.roll(zone, code, run.pool, dice, 0, rule, run.areaLevel)
            val monster = spawns.skilled(spawns.buffed(rolled, buffs), dice)
            val foe = arena.phases.foe(monster, run.areaLevel)
            life += foe.body.maxLife + foe.body.maxShield
            val f = arena.fight(listOf(foe), 5_000L + s, cap, retinue = false)
            if (f.outcome == Outcome.WIN) {
                kills += f.time
                actions += f.actions
            } else {
                deaths++
            }
            lifeLost += f.lifeLost
            taken += f.taken / (arena.body.maxLife + arena.body.maxShield).coerceAtLeast(1.0) / f.time.coerceAtLeast(0.1)
        }
        val n = monsterSamples.toDouble()
        fun med(list: List<Double>) = if (list.isEmpty()) -1.0 else list.sorted()[list.size / 2]
        return Row(
            listOf(
                "mob", zone.code.value, rarity.name, zone.level, run.areaLevel, tier, mapRarity.name, hero.heroClass.code, hero.level, hero.archetype.name,
                med(kills), if (actions.isEmpty()) -1.0 else actions.sorted()[actions.size / 2].toDouble(), kills.average().takeIf { !it.isNaN() } ?: -1.0,
                deaths.toDouble() / n, taken / n, lifeLost / n, life / n, arena.body.maxLife + arena.body.maxShield, arena.damage, monsterSamples, hero.note,
            ),
        )
    }
}

/**
 * Зонд: урон героя и его выживание против одной цели, без исхода боя. «Манекен» - цель с здоровьем ×[BIG] и без урона
 * (удары, умения нулевые): урон героя в секунду ([dps]) и за действие ([perAction]) за [DUMMY] секунд. «Бессмертный» - цель с
 * здоровьем ×[BIG] и своим уроном: секунд до смерти героя ([ttd], потолок [TTD_CAP]).
 */
data class Probe(val life: Double, val dps: Double, val perAction: Double, val ttd: Double, val actionsPerSecond: Double, val lifeShare: Double, val regen: Double = 0.0) {
    /** Секунд на цель с жизнью [life]: плоский урон и доля от жизни цели (силы «урон от здоровья цели») минус регенерация цели. */
    val seconds: Double get() = (life / (dps + (lifeShare - regen) * life).coerceAtLeast(1e-9)).coerceAtMost(9999.0)

    /** Действий героя на цель: её жизнь на урон за действие. */
    val actions: Double get() = life / perAction.coerceAtLeast(1.0)
}

/** Темп зоны у эталона: действий на обычного монстра, секунд на редкого и на стража (цели: 2-3, 10-20, 60-120). */
data class Tempo(val normalActions: Double, val rareSeconds: Double, val bossSeconds: Double)

const val BOSS = "BOSS"

/** Силы, чей урон - доля здоровья цели (`of: TARGET_LIFE`): на манекене их урон раздут, их считают долей в секунду. */
fun targetLifePowers(index: ContentIndex): Set<String> = index.powers.powers.filter { power -> power.effects.any { it.of == com.sperance.exileforge.rules.content.PowerBase.TARGET_LIFE } }.mapTo(HashSet()) { it.stat }

object Prober {
    const val BIG = 10_000.0
    const val DUMMY = 30.0
    const val TTD_CAP = 300.0
    private val strikes = com.sperance.exileforge.rules.roll.GuardianFloor.STRIKES
    private val targetLife = ConcurrentHashMap<ContentIndex, Set<String>>()

    fun dummy(m: com.sperance.exileforge.rules.roll.RolledMonster, big: Double = BIG) = m.copy(
        stats = m.stats + ("STOCK_HEALTH" to (m.stats["STOCK_HEALTH"] ?: 1.0) * big) + strikes.associateWith { 0.0 } + ("STOCK_ENERGY_SHIELD" to 0.0),
        skills = emptyList(),
    )

    fun immortal(m: com.sperance.exileforge.rules.roll.RolledMonster) = m.copy(stats = m.stats + ("STOCK_HEALTH" to (m.stats["STOCK_HEALTH"] ?: 1.0) * BIG))

    /** Два манекена (×100 и ×1000 жизни): урон в секунду = плоский + доля жизни цели. */
    fun probe(index: ContentIndex, arena: Arena, m: com.sperance.exileforge.rules.roll.RolledMonster, level: Int, seed: Long): Probe {
        val life = arena.phases.foe(m, level).body.let { it.maxLife + it.maxShield }

        fun rate(big: Double): Triple<Double, Double, Pair<Double, Int>> {
            val d = arena.battle(listOf(arena.phases.foe(dummy(m, big), level)), seed, false)
            while (d.time < DUMMY && d.outcome == null) d.advance(0.25)
            val ev = d.events.filter { it.actor == Side.HERO && !it.onSelf && it.foe == 0 }
            val dl = arena.phases.foe(dummy(m, big), level).body.let { it.maxLife + it.maxShield }
            val acts = ev.filter { (it.action == Action.ATTACK || it.action == Action.SKILL) && it.skill !in targetLife.getOrPut(index) { targetLifePowers(index) } }
            return Triple(ev.sumOf { it.damage } / d.time.coerceAtLeast(0.1), dl, (acts.sumOf { it.damage } / acts.size.coerceAtLeast(1)) to acts.size)
        }
        val (r1, l1, pa) = rate(100.0)
        val (r2, l2, _) = rate(1000.0)
        val share = ((r2 - r1) / (l2 - l1)).coerceAtLeast(0.0)
        val flat = (r1 - share * l1).coerceAtLeast(0.0)
        val dealt = flat * DUMMY
        val actions = pa.second
        val perAction = pa.first
        val t = arena.battle(listOf(arena.phases.foe(immortal(m), level)), seed + 1, false)
        while (t.time < TTD_CAP && t.outcome == null) t.advance(0.5)
        val body = arena.phases.foe(m, level).body
        val regen = (com.sperance.exileforge.rules.sheet.Recovery.lifeFlow(body, index.campaign.combat).gain / body.maxLife).coerceAtMost(index.rules.fight.recoveryCap / 100)
        return Probe(life, flat, perAction, if (t.outcome == Outcome.LOSS) t.time else TTD_CAP, actions / DUMMY, share, regen)
    }
}

/**
 * Запуск: `./gradlew :core:mobBal -Pcontent=<папка> -Pmode=<campaign|sweep|maps|might|heroes> -Pout=<файл> [-Pfights=16 -Prolls=4 -Psamples=9 -Pclasses=A,B -Pzones=...]`.
 *
 * Режимы: кампания (каждый босс каждым классом), архетипы, стражи Ваал, карты по тирам, Мощь боссов, лист эталонов, зонд, Мощь при
 * вариантах `might.skills`, время и опыт захода, выравнивание классов (урон и доля побед на уровнях [levels]), разбор по стражам, раш и
 * башня, стражи кристаллов и Бездна, Бездна по глубинам, Разлом недели, Скверна, алтари, Мощь стражей зон правилом `BossMight`.
 */
fun main(args: Array<String>) {
    val content = File(System.getProperty("content"))
    val mode = System.getProperty("mode") ?: "campaign"
    val fights = System.getProperty("fights")?.toIntOrNull() ?: 16
    val rolls = System.getProperty("rolls")?.toIntOrNull() ?: 4
    val samples = System.getProperty("samples")?.toIntOrNull() ?: 9
    val cap = System.getProperty("cap")?.toDoubleOrNull() ?: 300.0
    val only = System.getProperty("classes").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val zonesOnly = System.getProperty("zones").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val out = File(args.firstOrNull() ?: "build/reports/mobbal.tsv")
    val index = ContentLoader.load { File(content, it).readText() }
    val sim = MobBal(index, fights, rolls, samples, HeroPool(MobKit(index), HeroCacheKey.dir(CACHE, System.getProperty("guardCode").orEmpty(), content, index)))
    val zones = index.campaign.zones.filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null && (zonesOnly.isEmpty() || it.code.value in zonesOnly) }.sortedWith(compareBy({ it.level }, { it.code.value }))
    val classes = sim.classesOf(only)
    val started = System.nanoTime()
    data class Task(val run: () -> List<Row>)
    val tasks = mutableListOf<Task>()
    fun penalty(zone: Zone, onMap: Boolean) = index.campaign.resistPenalty(zone.code, onMap)
    when (mode) {
        "campaign", "guard" -> zones.forEach { zone ->
            classes.forEach { c ->
                tasks += Task {
                    val band = if (mode == "guard" || System.getProperty("ladder") == "1") campaignBand(zone.level) else GearBand.TYPICAL
                    val hero = sim.hero(c, zone.level, Archetype(band), penalty(zone, false))
                    val rows = mutableListOf(sim.boss(zone, hero, 0, Rarity.COMMON, cap))
                    if (System.getProperty("mobs") == "1") MonsterRarity.entries.filter { it != MonsterRarity.UNIQUE }.forEach { r -> rows += sim.monsters(zone, hero, 0, Rarity.COMMON, r, 120.0) }
                    rows
                }
            }
        }

        "sweep" -> zones.forEach { zone ->
            classes.forEach { c ->
                val archetypes = buildList {
                    GearBand.entries.forEach { band -> add(Archetype(band)) }
                    listOf(GearSource.RARE, GearSource.UNIQUE).forEach { add(Archetype(GearBand.GOOD, it)) }
                    listOf(Guard.ARMOUR, Guard.EVASION, Guard.ES).forEach { add(Archetype(GearBand.GOOD, guard = it)) }
                    sim.styles(c).takeIf { it.size > 1 }?.forEach { add(Archetype(GearBand.GOOD, style = it)) }
                    if (index.template(c.weapon)?.slot == Slot.WEAPON_1H) add(Archetype(GearBand.GOOD, shield = false))
                }
                archetypes.forEach { a ->
                    tasks += Task {
                        val hero = sim.hero(c, zone.level, a, penalty(zone, false))
                        listOf(sim.boss(zone, hero, 0, Rarity.COMMON, cap)) + MonsterRarity.entries.filter { it != MonsterRarity.UNIQUE }.map { r -> sim.monsters(zone, hero, 0, Rarity.COMMON, r, 120.0) }
                    }
                }
            }
        }

        "vaal" -> sim.vaalZones().filter { zonesOnly.isEmpty() || it.code.value in zonesOnly }.forEach { zone ->
            classes.forEach { c ->
                tasks += Task { listOf(sim.boss(zone, sim.hero(c, zone.level, Archetype(campaignBand(zone.level)), penalty(zone, false)), 0, Rarity.COMMON, cap)) }
            }
        }

        "maps" -> {
            val tiers = (System.getProperty("tiers") ?: "1,4,8,12,16").split(',').map { it.trim().toInt() }
            val mapRarities = (System.getProperty("mapRarities") ?: "COMMON").split(',').map { Rarity.valueOf(it.trim()) }
            sim.mapZones().filter { zonesOnly.isEmpty() || it.code.value in zonesOnly }.forEach { zone ->
                tiers.forEach { tier ->
                    classes.forEach { c ->
                        mapRarities.forEach { mr ->
                            tasks += Task {
                                val hero = sim.mapHero(c, zone, tier)
                                listOf(sim.boss(zone, hero, tier, mr, cap)) + (if (System.getProperty("mobs") == "1") MonsterRarity.entries.filter { it != MonsterRarity.UNIQUE }.map { r -> sim.monsters(zone, hero, tier, mr, r, 120.0) } else emptyList())
                            }
                        }
                    }
                }
            }
        }

        "might" -> zones.forEach { zone ->
            tasks += Task {
                val roller = com.sperance.exileforge.rules.roll.MonsterRoller(index)
                val ms = (0 until 8).map { r ->
                    val run = Run(index, zone, SimKit.SEED + r, RunContext(classes.first().code, zone.level))
                    val boss = Spawns(index, run).boss(zone, emptyList(), emptyList())!!
                    Triple(Might.of(index, boss.stats, zone.level), boss.stats, boss)
                }
                val base = roller.stats(index.monster(zone.boss)!!, zone.level)
                listOf(
                    Row(
                        listOf(
                            "might", zone.code.value, zone.boss.value, zone.level, ms.map { it.first.value }.average(), ms.map { it.first.offense }.average(), ms.map { it.first.defense }.average(),
                            ms.map { it.second["STOCK_HEALTH"] ?: 0.0 }.average(), ms.map { t -> com.sperance.exileforge.rules.roll.GuardianFloor.STRIKES.sumOf { t.second[it] ?: 0.0 } }.average(),
                            ms.map { it.second["STOCK_ATTACK_SPEED"] ?: 0.0 }.average(), base["STOCK_HEALTH"] ?: 0.0, com.sperance.exileforge.rules.roll.GuardianFloor.STRIKES.sumOf { base[it] ?: 0.0 },
                            index.monster(zone.boss)!!.power,
                        ),
                    ),
                )
            }
        }

        "heroes" -> listOf(10, 30, 50, 68, 72, 80, 88).forEach { level ->
            classes.forEach { c ->
                val archetypes = GearBand.entries.map { Archetype(it) } + listOf(Guard.ARMOUR, Guard.EVASION, Guard.ES).map { Archetype(GearBand.GOOD, guard = it) } + sim.styles(c).map { Archetype(GearBand.GOOD, style = it) }
                archetypes.forEach { a ->
                    tasks += Task {
                        val hero = sim.hero(c, level, a, if (level >= 70) 50.0 else index.campaign.combat.resistPenalty.getOrElse((level - 1) / 12) { 50.0 })
                        val arena = Arena(index, hero, emptyMap())
                        val b = arena.body
                        listOf(
                            Row(
                                listOf(
                                    "hero", c.code, level, a.name, b.maxLife, b.maxShield, b.armour, b.evasion, b.block, DamageType.ELEMENTS.map { b.resist(it) }.average(), b.resist(DamageType.CHAOS),
                                    arena.damage, b.attackSpeed, hero.note,
                                ),
                            ),
                        )
                    }
                }
            }
        }

        "dump" -> classes.forEach { c ->
            val level = System.getProperty("level")!!.toInt()
            tasks += Task {
                val band = GearBand.valueOf(System.getProperty("band") ?: "TYPICAL")
                val hero = sim.hero(c, level, Archetype(band), 50.0)
                val all = sim.kit.stats(c, level, hero.build.tree, hero.build.items)
                val lines = hero.build.items.map { item ->
                    val without = sim.kit.stats(c, level, hero.build.tree, hero.build.items - item)
                    val keys = listOf("STOCK_ATTACK_SPEED", "STOCK_ATTACK_PHYSICAL", "STOCK_HEALTH", "STOCK_ENERGY_SHIELD")
                    Row(listOf("item", c.code, level, item.template, item.rarity.name, keys.joinToString(" ") { k -> "$k ${String.format(Locale.ROOT, "%.2f", (all[k] ?: 0.0) - (without[k] ?: 0.0))}" }, item.rolls.joinToString(",") { "${it.code.value}:${it.tier}" }))
                }
                lines + Row(listOf("tree", c.code, level, hero.build.tree.joinToString(",") { it.code }))
            }
        }

        "trace" -> zones.forEach { zone ->
            classes.forEach { c ->
                tasks += Task {
                    val band = GearBand.valueOf(System.getProperty("band") ?: "TYPICAL")
                    val hero = sim.hero(c, System.getProperty("level")?.toIntOrNull() ?: zone.level, Archetype(band), penalty(zone, false))
                    val run = Run(index, zone, SimKit.SEED, RunContext(c.code, hero.level))
                    val arena = Arena(index, hero, mapOf(MapStat.HERO_RESIST.code to penalty(zone, false)))
                    val boss = Spawns(index, run).boss(zone, emptyList(), emptyList())!!
                    val foe = arena.phases.foe(boss, run.levelOf(zone))
                    val battle = if (System.getProperty("dummy") == "1") arena.battle(listOf(arena.phases.foe(Prober.dummy(boss), run.levelOf(zone))), 9173L, false) else arena.battle(listOf(foe), 9173L, true)
                    while (battle.outcome == null && battle.time < 60) battle.advance(0.25)
                    listOf(Row(listOf("trace", c.code, zone.code.value, "bossLife", foe.body.maxLife, "items", hero.build.items.joinToString(",") { it.template }))) +
                        battle.events.take(40).map { e -> Row(listOf("ev", String.format(Locale.ROOT, "%.2f", e.time), e.actor, e.action, e.kind, e.skill ?: "-", e.type ?: "-", e.damage, e.monsterLife, e.heroLife, e.foe)) }
                }
            }
        }

        "probe" -> {
            val tiers = (System.getProperty("tiers") ?: "0").split(',').map { it.trim().toInt() }
            val bands = System.getProperty("band")?.split(',')?.map { GearBand.valueOf(it.trim()) }
            zones.forEach { zone ->
                tiers.forEach { tier ->
                    (bands ?: listOf(if (tier == 0) campaignBand(zone.level) else bandOf(tier))).forEach { band ->
                        classes.forEach { c ->
                            tasks += Task {
                                val level = if (tier == 0) zone.level else heroLevelOf(band)
                                val hero = sim.hero(c, level, Archetype(band), penalty(zone, tier > 0))
                                sim.probe(zone, hero, tier, Rarity.COMMON)
                            }
                        }
                    }
                }
            }
        }

        "might5" -> {
            val levels = (System.getProperty("levels") ?: "20,40,60,72,80,88").split(',').map { it.trim().toInt() }
            val rulesText = File(content, "rules.json").readText()
            val variants = (System.getProperty("variants") ?: "0.4:0.5,0.2:0.5,0.6:0.5,1.0:0.5,0.4:0.3,0.4:0.7,0.4:1.0").split(',').map { v -> v.split(':').let { it[0].toDouble() to it[1].toDouble() } }
            val indices = variants.associateWith { (share, extra) ->
                val patched = rulesText.replace(Regex("\"shares\"\\s*:\\s*\\{\\s*\"ENEMIES_3\"\\s*:\\s*[0-9.]+\\s*\\}"), "\"shares\": {\"ENEMIES_3\": $share}")
                    .replace(Regex("\"extraTarget\"\\s*:\\s*[0-9.]+"), "\"extraTarget\": $extra")
                ContentLoader.load { name -> if (name == "rules.json") patched else File(content, name).readText() }
            }
            val zone0 = { level: Int -> zones.minBy { kotlin.math.abs(it.level - level) } }
            levels.forEach { level ->
                val bands = if (level <= 70) listOf(GearBand.TYPICAL) else listOf(heroBand(level))
                bands.forEach { band ->
                    classes.forEach { c ->
                        tasks += Task {
                            val zone = zone0(level)
                            val hero = sim.hero(c, level, Archetype(band), penalty(zone, level > 70))
                            val (single, pack) = sim.offense(zone, hero)
                            val mights = variants.map { v ->
                                val idx = indices.getValue(v)
                                val body = HeroBody.of(idx, HeroInputs(level, c.code, hero.build.tree, hero.build.items, skills = hero.build.skills))
                                Might.hero(idx, body).offense
                            }
                            listOf(Row(listOf("might5", c.code, level, band.name, single, pack) + mights + listOf(hero.note)))
                        }
                    }
                }
            }
        }

        "runtime" -> {
            val scales = (System.getProperty("countScales") ?: "1.0").split(',').map { it.trim().toDouble() }
            val seeds = (System.getProperty("rolls") ?: "3").toInt()
            zones.forEach { zone0 ->
                scales.forEach { k ->
                    classes.forEach { c ->
                        tasks += Task {
                            val zone = zone0.copy(count = zone0.count.map { kotlin.math.round(it * k).toInt().coerceAtLeast(1) })
                            val band = campaignBand(zone.level)
                            val hero = sim.hero(c, zone.level, Archetype(band), penalty(zone, false))
                            (0 until seeds).map { s -> sim.runtime(zone, hero, SimKit.SEED + 31L * s, k) }
                        }
                    }
                }
            }
        }

        "classes" -> {
            val levels = (System.getProperty("levels") ?: "10,20,40,60,72,80,88").split(',').map { it.trim().toInt() }
            val sources = (System.getProperty("source") ?: "RARE,MIXED").split(',').map { GearSource.valueOf(it.trim()) }
            val gearSeeds = (System.getProperty("gearSeeds") ?: "1").toInt()
            levels.forEach { level ->
                classes.forEach { c ->
                    tasks += Task {
                        val near = sim.nearZones(level, CLASS_BOSSES)
                        val runs = sources.flatMap { source -> (0 until gearSeeds).map { source to it } }.map { (source, seed) ->
                            val (hero, damage) = sim.classDamage(c, level, source, seed)
                            val bosses = near.map { sim.boss(it, hero, 0, Rarity.COMMON, cap) }
                            Triple(hero, damage, bosses)
                        }
                        fun geo(f: (LoadoutTuner.Damage) -> Double) = kotlin.math.exp(runs.map { kotlin.math.ln(f(it.second).coerceAtLeast(1.0)) }.average())
                        val bosses = runs.flatMap { it.third }
                        val wins = bosses.map { it.values[10] as Double }.average()
                        val times = bosses.map { it.values[11] as Double }.filter { it > 0 }
                        val parts = HashMap<String, Double>()
                        runs.forEach { (_, d, _) -> d.bySkill.forEach { (k, v) -> parts.merge(k, v / d.bySkill.values.sum().coerceAtLeast(1.0), Double::plus) } }
                        val types = HashMap<String, Double>()
                        runs.forEach { (_, d, _) -> d.byType.forEach { (k, v) -> types.merge(k, v / d.byType.values.sum().coerceAtLeast(1.0), Double::plus) } }
                        val perBoss = bosses.groupBy { it.values[2] as String }.map { (boss, rows) ->
                            Row(listOf("classboss", c.code, level, boss, rows.map { it.values[10] as Double }.average(), rows.map { it.values[12] as Double }.average(), rows.sumOf { it.values[13] as Int }, rows.map { it.values[14] as Double }.average()))
                        }
                        perBoss + listOf(
                            Row(
                                listOf(
                                    "class", c.code, level, heroBand(level).name, geo { it.single }, geo { it.pack }, geo { it.score }, wins, if (times.isEmpty()) -1.0 else times.average(),
                                    near.joinToString(",") { it.boss.value }, runs.joinToString(" | ") { it.first.note },
                                    parts.entries.sortedByDescending { it.value }.joinToString(",") { "${it.key}:${(100 * it.value / runs.size).toInt()}" },
                                    types.entries.sortedByDescending { it.value }.joinToString(",") { "${it.key}:${(100 * it.value / runs.size).toInt()}" },
                                ),
                            ),
                        )
                    }
                }
            }
        }

        "modes" -> {
            val modes = ModesBal(index)
            val seeds = (System.getProperty("rolls") ?: "2").toInt()
            index.campaign.regions.forEachIndexed { act, region ->
                val level = region.zones.maxOf { it.level }
                classes.forEach { c ->
                    tasks += Task {
                        val hero = sim.hero(c, level, Archetype(campaignBand(level)), index.campaign.resistPenalty(region.zones.last().code, false))
                        (0 until seeds).map { s ->
                            val r = modes.rush(hero, region, 0, SimKit.SEED + 101L * s + act)
                            Row(listOf("rush", act + 1, level, c.code, r.killed, r.bosses, r.wall, r.fast))
                        }
                    }
                }
            }
            index.campaign.regions.forEachIndexed { act, region ->
                val picked = listOf(region.zones.first(), region.zones[region.zones.size / 2], region.zones.last()).distinct()
                picked.forEach { zone ->
                    classes.forEach { c ->
                        tasks += Task {
                            val hero = sim.hero(c, zone.level, Archetype(campaignBand(zone.level)), index.campaign.resistPenalty(zone.code, false))
                            val crystals = (0 until seeds * 2).map { p -> modes.crystal(hero, zone, p, SimKit.SEED + zone.level) }
                            val crystalRows = crystals.map { f -> Row(listOf("crystal", act + 1, zone.code.value, zone.level, c.code, f.outcome == Outcome.WIN, f.time, f.lifeLost)) }
                            val abyssRows = if (zone.level < (index.campaign.abyss?.minLevel ?: 0)) emptyList() else modes.abyss(hero, zone, SimKit.SEED + 7L * zone.level).mapIndexed { d, f -> Row(listOf("abyss", act + 1, zone.code.value, zone.level, c.code, d + 1, f.outcome == Outcome.WIN, f.time, f.lifeLost)) }
                            crystalRows + abyssRows
                        }
                    }
                }
            }
            (System.getProperty("levels") ?: "20,40,60,75,85").split(',').map { it.trim().toInt() }.forEach { level ->
                classes.forEach { c ->
                    tasks += Task {
                        val zone = sim.nearZones(level, 1).first()
                        val hero = sim.hero(c, level, Archetype(campaignBand(level)), index.campaign.resistPenalty(zone.code, false))
                        val t = modes.tower(hero, SimKit.SEED + level, 100)
                        listOf(Row(listOf("tower", c.code, level, t.floor, t.level, t.seconds)))
                    }
                }
            }
        }

        "abyss" -> {
            val modes = ModesBal(index)
            val rule = checkNotNull(index.campaign.abyss)
            val seeds = (System.getProperty("rolls") ?: "6").toInt()
            val picked = if (zonesOnly.isNotEmpty()) {
                index.campaign.zones.filter { it.code.value in zonesOnly }
            } else {
                index.campaign.regions.mapNotNull { region -> region.zones.filter { it.level >= rule.minLevel }.minByOrNull { it.level } }
            }
            val levels = System.getProperty("levels")?.split(',')?.map { it.trim().toInt() }
            picked.forEach { zone ->
                (levels ?: listOf(zone.level)).forEach { level ->
                    classes.forEach { c ->
                        tasks += Task {
                            val hero = sim.hero(c, level, Archetype(campaignBand(level)), penalty(zone, false))
                            (0 until seeds).flatMap { s ->
                                modes.abyss(hero, zone, modes.abyssSeed(hero, zone, s)).mapIndexed { d, f ->
                                    Row(listOf("abyss", zone.code.value, zone.level, level, c.code, s, d + 1, rule.waves[d].leader?.value ?: "-", f.outcome == Outcome.WIN, f.time, f.lifeLost))
                                }
                            }
                        }
                    }
                }
            }
        }

        "rift" -> {
            val modes = ModesBal(index)
            val weeks = (System.getProperty("rolls") ?: "3").toInt()
            (System.getProperty("levels") ?: "21,31,41,61,71,81,88").split(',').map { it.trim().toInt() }.forEach { level ->
                classes.forEach { c ->
                    tasks += Task {
                        val zone = sim.nearZones(level, 1).first()
                        val hero = sim.hero(c, level, Archetype(campaignBand(level)), penalty(zone, false))
                        (0 until weeks).flatMap { w ->
                            val week = RIFT_WEEK + w
                            val r = modes.rift(hero, week)
                            listOf(Row(listOf("rift", level, c.code, week, r.guardians, r.acts, r.fights, r.fallen, r.fatal?.name ?: "-"))) +
                                r.wardens.mapIndexed { act, (won, seconds) -> Row(listOf("riftward", level, c.code, week, act + 1, won, seconds)) }
                        }
                    }
                }
            }
        }

        "blight" -> {
            val modes = ModesBal(index)
            val seeds = (System.getProperty("rolls") ?: "2").toInt()
            index.campaign.regions.forEachIndexed { act, region ->
                val picked = if (zonesOnly.isNotEmpty()) region.zones.filter { it.code.value in zonesOnly } else listOf(region.zones.first(), region.zones[region.zones.size / 2], region.zones.last()).distinct()
                picked.forEach { zone ->
                    classes.forEach { c ->
                        tasks += Task {
                            val hero = sim.hero(c, zone.level, Archetype(campaignBand(zone.level)), penalty(zone, false))
                            (0 until seeds).flatMap { s ->
                                modes.blight(hero, zone, SimKit.SEED + 13L * zone.level + 1009L * s).map { p ->
                                    Row(listOf("blight", act + 1, zone.code.value, zone.level, c.code, s, p.point, p.won, p.seconds, p.lifeLost, p.maxHit))
                                }
                            }
                        }
                    }
                }
            }
        }

        "altars" -> {
            val offers = index.campaign.features?.altar?.offers.orEmpty()
            val mapZones = zones.filter { it.level in 61..70 }.take(4)
            mapZones.forEach { zone ->
                classes.forEach { c ->
                    tasks += Task {
                        val hero = sim.hero(c, heroLevelOf(GearBand.TYPICAL), Archetype(GearBand.TYPICAL), penalty(zone, true))
                        (listOf(null) + offers).flatMap { offer ->
                            val extra = offer?.let { o -> listOfNotNull(index.modifier(com.sperance.exileforge.rules.content.ModifierCode(o.curse)), index.modifier(com.sperance.exileforge.rules.content.ModifierCode(o.boon))).flatMap { m -> m.effects.zip(m.tiers.first().values) { e, v -> e.stat to v.first() } }.toMap() }.orEmpty()
                            val boss = sim.boss(zone, hero, 1, Rarity.COMMON, cap, extra)
                            val rare = sim.monsters(zone, hero, 1, Rarity.COMMON, MonsterRarity.RARE, 120.0, extra)
                            listOf(Row(listOf("altar", offer?.code ?: "-", zone.code.value, c.code, boss.values[10], boss.values[11], boss.values[12], rare.values[10], rare.values[13])))
                        }
                    }
                }
            }
        }

        "trials" -> {
            val modes = ModesBal(index)
            val trials = Trials(index, sim, modes)
            index.campaign.regions.forEachIndexed { act, region ->
                classes.forEach { c -> trials.rushTiers().forEach { tier -> tasks += Task { (0 until rolls).map { s -> trials.rush(region, c, tier, s).row(act + 1) } } } }
            }
            Trials.LEVELS.forEach { level ->
                classes.forEach { c ->
                    tasks += Task { (0 until samples).flatMap { s -> listOf(trials.tower(level, c, s).row(), trials.farm(level, c, s).row("farm")) } + trials.run(level, c).row() }
                }
            }
        }

        "bossmight" -> zones.forEach { zone -> tasks += Task { listOf(Row(listOf("bossmight", zone.code.value, zone.boss.value, zone.level, com.sperance.exileforge.rules.sheet.BossMight.of(index, zone)))) } }

        "spread" -> {
            val k = (System.getProperty("k") ?: "3").toInt()
            val bands = (System.getProperty("band") ?: "WEAK,REF").split(',').map { it.trim() }
            zones.forEach { zone ->
                (-k..k).map { zone.level + it }.filter { it in 1..index.classes.maxLevel }.forEach { level ->
                    bands.forEach { name ->
                        classes.forEach { c ->
                            tasks += Task {
                                val band = if (name == REFERENCE_BAND) campaignBand(level) else GearBand.valueOf(name)
                                val boss = sim.boss(zone, sim.hero(c, level, Archetype(band), penalty(zone, false)), 0, Rarity.COMMON, cap)
                                listOf(Row(listOf("spread", zone.code.value, zone.boss.value, zone.level, level, level - zone.level, name, c.code, boss.values[10])))
                            }
                        }
                    }
                }
            }
        }

        else -> error("mode $mode")
    }
    out.parentFile?.mkdirs()
    val done = java.util.concurrent.atomic.AtomicInteger()
    val rows = tasks.parallelStream().flatMap { t ->
        val r = t.run()
        val k = done.incrementAndGet()
        if (k % 20 == 0) System.err.println("mobbal: $k/${tasks.size} ${(System.nanoTime() - started) / 1_000_000_000}s")
        r.stream()
    }.toList()
    val written = when (mode) {
        "guard" -> rows + guard(index, rows)
        "spread" -> rows + spread(rows)
        "trials" -> rows + Trials.summary(index, rows)
        else -> rows
    }
    out.writeText(written.joinToString("\n") { it.tsv() } + "\n")
    if (mode == "guard") written.filter { it.values[0] == "guard" && it.values.last() == false }.forEach { System.err.println("mobbal guard: ${it.tsv()}") }
    println("mobbal $mode: ${rows.size} rows in ${(System.nanoTime() - started) / 1_000_000_000} s -> ${out.path}")
}

/**
 * Цель побед эталона над стражем своего уровня (RULES «Общее»): [WINS] боёв, финал акта - у нижней границы ([FINALS]).
 * Карты своего уровня, вожди Бездны, Разлом недели и Матерь Скверны равняются на неё же (отчёты режимов, сторож `ModesBalanceGuardTest`).
 */
object GuardianTarget {

    val WINS = 0.55..0.60
    val FINALS = 0.55..0.575

    /** Цель стража: финал акта ([final]) - нижняя половина коридора. */
    fun of(final: Boolean): ClosedFloatingPointRange<Double> = if (final) FINALS else WINS
}

/**
 * Цель режимов, что тяжелеют с ростом (RULES «Общее», цели баланса): доля побед эталона своего уровня падает линейно от [EASY] в начале
 * отрезка до [HARD] в его конце - карты по тиру, Разлом недели по уровню героя.
 */
object FallingTarget {
    const val EASY = 0.60
    const val HARD = 0.30

    /** Цель на месте [at] отрезка [from]..[to]: вне отрезка - как на его краю. */
    fun at(at: Double, from: Double, to: Double): Double = EASY + (HARD - EASY) * ((at - from) / (to - from)).coerceIn(0.0, 1.0)
}

/**
 * Матерь Скверны не убивает эталон одним ударом: с уровня [FROM] её сильнейший удар в бою - не больше доли [SHARE] запаса эталона
 * (здоровье и щит) в середине выборки и меньше всего запаса в любом бою.
 */
object MotherHit {
    const val FROM = 81
    const val SHARE = 0.35
}

/**
 * Цель слабого билда (RULES «Общее», цели баланса): герой полосы [GearBand.WEAK] на [LEAD] уровней выше стража кампании побеждает его
 * не реже [SHARE] боёв - в среднем по стражам и классам; стражи ради неё не слабеют (эталон своего уровня держит [GuardianTarget]).
 */
object WeakTarget {
    const val LEAD = 6
    const val SHARE = 0.30
}

/**
 * Сторож стражей (полный прогон `-Pmode=guard`; доля побед эталонов вместо разброса Мощи): у каждого стража доля
 * побед эталонов всех классов - в цели [GuardianTarget] с допуском выборки [tolerance]. Строка `guard`: зона, страж, акт, финал ли, доля
 * побед, среднее время победы (-1 - побед нет), в коридоре ли.
 */
fun guard(index: ContentIndex, rows: List<Row>, tolerance: Double = GUARD_TOLERANCE): List<Row> {
    val finals = index.campaign.regions.mapNotNull { region -> region.zones.filter { it.boss.value.isNotBlank() }.maxByOrNull { it.level }?.code?.value }.toSet()
    val actOf = index.campaign.regions.withIndex().flatMap { (act, region) -> region.zones.map { it.code.value to act + 1 } }.toMap()
    return rows.filter { it.values[0] == "boss" }.groupBy { it.values[1] as String }.map { (zone, list) ->
        val wins = list.map { it.values[10] as Double }.average()
        val times = list.map { it.values[11] as Double }.filter { it > 0 }
        val final = zone in finals
        val target = GuardianTarget.of(final)
        val corridor = (target.start - tolerance)..(target.endInclusive + tolerance)
        Row(listOf("guard", zone, list.first().values[2], actOf[zone] ?: 0, final, wins, if (times.isEmpty()) -1.0 else times.average(), wins in corridor))
    }
}

/**
 * Отчёт «босс L против героя L±k» (`-Pmode=spread -Pk=3 -Pband=WEAK,REF`): строка `spreadsum` - полоса вещей (`REF` - эталона
 * кампании своего уровня, [campaignBand]), сдвиг уровня героя от уровня стража и средняя доля побед по всем стражам и классам.
 */
fun spread(rows: List<Row>): List<Row> = rows.filter { it.values[0] == "spread" }.groupBy { it.values[6] as String to it.values[5] as Int }.toSortedMap(compareBy({ it.first }, { it.second }))
    .map { (key, list) -> Row(listOf("spreadsum", key.first, key.second, list.map { it.values[8] as Double }.average(), list.size)) }

/** Полоса отчёта `spread` - эталон кампании своего уровня ([campaignBand]). */
const val REFERENCE_BAND = "REF"

/** Допуск полного прогона: 8 боёв на класс, 56 на стража - разброс выборки ~7 п.п. */
const val GUARD_TOLERANCE = 0.10

/** Первая неделя Разлома режима `rift`: недели схем - подряд от неё. */
const val RIFT_WEEK = 2_900L

/**
 * Стражей на уровень в режиме `classes` и в `ClassBalanceGuardTest`: ближайшие зоны не выше уровня героя. Восемь: четыре
 * ближайших - стражи одного акта и часто одного вида урона (у 40-го все бьют физикой), их подбор решал победы класса больше его обороны.
 */
const val CLASS_BOSSES = 8

fun bandOf(tier: Int): GearBand = when {
    tier <= 5 -> GearBand.TYPICAL
    tier <= 10 -> GearBand.GOOD
    else -> GearBand.OPTIMIZED
}

/** Эталон кампании (допущение баланса): до 75 - типичный, 76-85 - хороший, с 86 - оптимизированный (район как у T11+). */
fun campaignBand(level: Int): GearBand = when {
    level <= 75 -> GearBand.TYPICAL
    level <= 85 -> GearBand.GOOD
    else -> GearBand.OPTIMIZED
}

fun heroBand(level: Int): GearBand = when {
    level <= 75 -> GearBand.TYPICAL
    level <= 84 -> GearBand.GOOD
    else -> GearBand.OPTIMIZED
}

fun heroLevelOf(band: GearBand): Int = when (band) {
    GearBand.WEAK, GearBand.TYPICAL -> 72
    GearBand.GOOD -> 80
    GearBand.OPTIMIZED -> 88
}

/**
 * Испытания баланса (RULES «Общее», цели баланса): раш региона на каждой ступени эталоном уровня финала региона, башня с первого
 * этажа до цели [TowerTarget] эталоном своего уровня и обычный заход зоны того же уровня ([MobBal.runtime]) - мерило награды:
 * опыт и золото испытания в «заходах» (`RunWorth`) за минуту стены против одного захода за его минуты; этаж башни ниже героя
 * платит долей опыта своего уровня через окно уровней.
 */
class Trials(private val index: ContentIndex, private val sim: MobBal, private val modes: ModesBal) {
    private val rules = checkNotNull(index.campaign.trials)
    private val speed = index.rules.fight.speeds.max().toDouble()
    private val worth = RunWorth(index)
    private val loot = LootRoller(index)

    /** Ступени раша по порядку, с 0. */
    fun rushTiers(): List<Int> = (0 until rules.rush.tierCount).toList()

    /** Раш региона [region] ступени [tier] эталоном класса [heroClass] уровня финала региона, кости [seed]. */
    fun rush(region: Region, heroClass: HeroClass, tier: Int, seed: Int): RushTry {
        val level = region.zones.maxOf { it.level }
        val hero = sim.hero(heroClass, level, Archetype(campaignBand(level)), index.campaign.resistPenalty(region.zones.last().code, false))
        val r = modes.rush(hero, region, tier, SimKit.SEED + 101L * seed + 7L * tier + level)
        val share = r.killed.toDouble() / r.bosses.coerceAtLeast(1)
        return RushTry(region.code, level, heroClass.code, tier, r.killed >= r.bosses && r.bosses > 0, rules.rush.tier(tier).runs * share, r.wall)
    }

    /** Башня эталоном класса [heroClass] уровня [level] с первого этажа до этажа цели ([TowerTarget.floor]) или гибели, кости [seed]. */
    fun tower(level: Int, heroClass: HeroClass, seed: Int): TowerTry {
        val zone = sim.nearZones(level, 1).first()
        val hero = sim.hero(heroClass, level, Archetype(campaignBand(level)), index.campaign.resistPenalty(zone.code, false))
        val goal = TowerTarget.floor(level)
        val t = modes.tower(hero, SimKit.SEED + 31L * seed + level, goal)
        val runs = (1..t.floor).sumOf { floor -> (rules.tower.floorRuns + if (rules.tower.hoard(floor)) rules.tower.hoardCap else 0.0) * share(rules.tower.level(floor), level) }
        return TowerTry(level, heroClass.code, t.floor, goal, runs, t.wall)
    }

    /** Опыт захода уровня [area] герою уровня [hero] (окно уровней) - долей опыта захода его уровня: так башня платит этажами ниже героя. */
    private fun share(area: Int, hero: Int): Double = worth.at(area).experience * loot.window(area, hero) / worth.at(hero).experience.coerceAtLeast(1.0)

    /**
     * Фарм башни эталоном класса [heroClass] уровня [level], кости [seed]: вход с чекпоинта десятка ниже этажа цели (как игрок
     * продолжает с рекорда) и подъём на десяток выше цели или до гибели - мерило награды башни в минуту.
     */
    fun farm(level: Int, heroClass: HeroClass, seed: Int): TowerTry {
        val zone = sim.nearZones(level, 1).first()
        val hero = sim.hero(heroClass, level, Archetype(campaignBand(level)), index.campaign.resistPenalty(zone.code, false))
        val goal = TowerTarget.floor(level)
        val from = rules.tower.start(goal - rules.tower.checkpoint)
        val t = modes.tower(hero, SimKit.SEED + 37L * seed + level, goal + rules.tower.checkpoint, from)
        val runs = (from..t.floor).sumOf { floor -> (rules.tower.floorRuns + if (rules.tower.hoard(floor)) rules.tower.hoardCap else 0.0) * share(rules.tower.level(floor), level) }
        return TowerTry(level, heroClass.code, t.floor, goal, runs, t.wall)
    }

    /** Обычный заход зоны уровня [level] эталоном класса [heroClass]: его стена - бои на высшей скорости и ходьба. */
    fun run(level: Int, heroClass: HeroClass): RunTry {
        val zone = sim.nearZones(level, 1).first()
        val hero = sim.hero(heroClass, zone.level, Archetype(campaignBand(zone.level)), index.campaign.resistPenalty(zone.code, false))
        val row = sim.runtime(zone, hero, SimKit.SEED, 1.0)
        return RunTry(level, heroClass.code, (row.values[11] as Double) / speed + (row.values[12] as Double))
    }

    /** Попытка раша: ступень [tier], зачищен ли, «заходов» награды [runs] по доле павших, секунды стены [wall]. */
    data class RushTry(val region: String, val level: Int, val heroClass: String, val tier: Int, val cleared: Boolean, val runs: Double, val wall: Double) {
        fun row(act: Int) = Row(listOf("rush", act, region, level, heroClass, tier, cleared, runs, wall))
    }

    /** Подъём на башню: этаж [floor] из цели [goal], «заходов» награды [runs] (этажи и клады), секунды стены [wall]. */
    data class TowerTry(val level: Int, val heroClass: String, val floor: Int, val goal: Int, val runs: Double, val wall: Double) {
        val reached: Boolean get() = floor >= goal
        fun row(kind: String = "tower") = Row(listOf(kind, level, heroClass, floor, goal, reached, runs, wall))
    }

    /** Обычный заход уровня [level]: секунды стены [wall]. */
    data class RunTry(val level: Int, val heroClass: String, val wall: Double) {
        fun row() = Row(listOf("run", level, heroClass, wall))
    }

    companion object {
        /** Уровни башни и награды: уровни зон кампании от 21 до 88. */
        val LEVELS = listOf(21, 41, 61, 81)

        /**
         * Сводка `-Pmode=trials`: доля зачисток раша по ступени против [RushTarget], доля героев, дошедших до этажа цели башни,
         * и награда испытаний в минуту долей заходов в минуту ([RewardTarget]): раш - ступенью, башня - фармом с чекпоинта.
         */
        fun summary(index: ContentIndex, rows: List<Row>): List<Row> {
            val runs = rows.filter { it.values[0] == "run" }
            val perMinute = runs.map { 60.0 / (it.values[3] as Double).coerceAtLeast(1.0) }.average()
            val tiers = checkNotNull(index.campaign.trials).rush.tierCount
            val rush = rows.filter { it.values[0] == "rush" }.groupBy { it.values[5] as Int }.toSortedMap().map { (tier, list) ->
                val clear = list.count { it.values[6] == true }.toDouble() / list.size
                val pay = list.sumOf { it.values[7] as Double } / (list.sumOf { it.values[8] as Double } / 60) / perMinute
                Row(listOf("rushsum", tier + 1, clear, RushTarget.at(tier, tiers), pay))
            }
            val farms = rows.filter { it.values[0] == "farm" }.groupBy { it.values[1] as Int }
            val tower = rows.filter { it.values[0] == "tower" }.groupBy { it.values[1] as Int }.toSortedMap().map { (level, list) ->
                val reached = list.count { it.values[5] == true }.toDouble() / list.size
                val farm = farms[level].orEmpty()
                val pay = farm.sumOf { it.values[6] as Double } / (farm.sumOf { it.values[7] as Double } / 60) / perMinute
                Row(listOf("towersum", level, reached, TowerTarget.SHARE, pay))
            }
            return rush + tower
        }
    }
}

/** Раш (RULES «Общее», цели баланса): ступень I зачищает эталон уровня финала региона в [FIRST] попыток, дальше линейно до [LAST] на последней. */
object RushTarget {
    const val FIRST = 0.50
    const val LAST = 0.15

    /** Цель ступени [tier] (с 0) из [tiers]. */
    fun at(tier: Int, tiers: Int): Double = if (tiers <= 1) FIRST else FIRST + (LAST - FIRST) * tier / (tiers - 1)
}

/** Башня (RULES «Общее», цели баланса): эталон уровня L с первого этажа доходит до этажа L − [BELOW] в [SHARE] попыток. */
object TowerTarget {
    const val BELOW = 10
    const val SHARE = 0.50

    /** Этаж цели эталона уровня [level]. */
    fun floor(level: Int): Int = (level - BELOW).coerceAtLeast(1)
}

/** Награда испытаний (RULES «Общее», цели баланса): опыт и золото в минуту - не меньше [SHARE] обычного захода своего уровня в минуту. */
object RewardTarget {
    const val SHARE = 1.2
}
