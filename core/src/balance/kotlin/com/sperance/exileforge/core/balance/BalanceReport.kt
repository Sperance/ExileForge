package com.sperance.exileforge.core.balance

import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillNodeType
import com.sperance.exileforge.rules.content.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * The balance report (`./gradlew :core:balanceReport`, `-Pdeep` for more runs): typical heroes of every
 * class and archetype at a ladder of levels, autorunning the zone of their level and fighting in the
 * arena, and what that says about builds, progress, loot and the worth of nodes and skills. It measures
 * and changes nothing.
 *
 * Arguments: the content folder, the output folder, and `deep` for the long run.
 */
fun main(args: Array<String>) {
    val content = File(args.getOrElse(0) { "backend/src/main/resources/content" })
    val out = File(args.getOrElse(1) { "build/balance" }).apply { mkdirs() }
    val config = if (args.getOrNull(2) == "deep") BalanceConfig.DEEP else BalanceConfig.FAST
    val index = ContentLoader.load { File(content, it).readText() }
    lateinit var report: BalanceData
    val millis = measureTimeMillis { report = BalanceStudy(index, config).study() }
    val json = Json { prettyPrint = true; encodeDefaults = true }
    File(out, "report.json").writeText(json.encodeToString(BalanceData.serializer(), report.copy(seconds = millis / 1000.0)))
    File(out, "report.html").writeText(ReportPage.render(report.copy(seconds = millis / 1000.0)))
    println("balance report: ${out.absolutePath}/report.html (${millis / 1000} s)")
}

/** How much to simulate: the levels, the autoruns per build and level, and the arena's seeds. */
data class BalanceConfig(val name: String, val levels: List<Int>, val runs: Int, val arenaSeeds: Int, val utilityLevel: Int) {
    companion object {
        val FAST = BalanceConfig("fast", listOf(10, 30, 50, 70, 90), runs = 4, arenaSeeds = 3, utilityLevel = 70)
        val DEEP = BalanceConfig("deep", listOf(5, 10, 20, 30, 40, 50, 60, 70, 80, 90), runs = 16, arenaSeeds = 8, utilityLevel = 70)
    }
}

@Serializable data class BalanceData(
    val config: String, val levels: List<Int>, val runs: Int, val seconds: Double = 0.0,
    val builds: List<BuildResult>, val progress: List<ProgressResult>, val loot: List<LootResult>,
    val nodes: List<NodeResult>, val untakenNodes: Map<String, Int>, val skills: List<SkillResult>,
)

/** One class, archetype and level: its sheet, its autoruns and its arena time. */
@Serializable data class BuildResult(
    val heroClass: String, val archetype: String, val level: Int, val zone: String, val zoneLevel: Int,
    val life: Double, val energyShield: Double, val resistance: Double, val inactiveItems: Int,
    /** Autoruns lost in the zone of the hero's own level; the rest is measured in the farm zone. */
    val ownDeathRate: Double, val farmZone: String, val farmZoneLevel: Int,
    val clearMinutes: Double, val deathRate: Double, val killsPerMinute: Double,
    val experiencePerHour: Double, val goldPerHour: Double, val arenaSecondsPerPack: Double, val arenaLossRate: Double,
    val bossSeconds: Double, val bossWinRate: Double,
    val flag: String = "",
)

/** Hours of autoruns in the farm zones to reach each level, deaths costing the run. */
@Serializable data class ProgressResult(val heroClass: String, val archetype: String, val hoursTo: Map<Int, Double>)

/** What an hour of autoruns at a level pays, over every build: per item, and in the merchant's gold. */
@Serializable data class LootResult(val level: Int, val goldPerHour: Double, val itemsPerHour: Map<String, Double>,
                                    val rarePerHour: Double, val uniquePerHour: Double, val itemValuePerHour: Double)

/** A node of the tree: how many builds take it and how much slower the arena is without it (percent). */
@Serializable data class NodeResult(val code: String, val type: String, val takenBy: Int, val loss: Double)

/** A skill alone in the class's best build: its arena cost against the class's plain attack. */
@Serializable data class SkillResult(val heroClass: String, val code: String, val type: String, val secondsPerPack: Double, val lossRate: Double, val relative: Double)

/** The study itself: builds, runs, benches, all on the seeds of the config, so two reports compare. */
class BalanceStudy(private val index: ContentIndex, private val config: BalanceConfig) {
    private val factory = HeroFactory(index)
    private val maps = MapSimulator(index)
    private val zones = index.campaign.zones
    private val classes = index.classes.classes.map { it.code }

    fun study(): BalanceData = runBlocking(Dispatchers.Default) {
        val specs = classes.flatMap { cls -> Archetype.entries.flatMap { arch -> config.levels.map { BuildSpec(cls, arch, it, seedOf(cls, arch, it)) } } }
        val measured = specs.map { spec -> async { measure(spec) } }.awaitAll()
        val builds = flag(measured.map { it.result })
        val utility = classes.flatMap { cls -> Archetype.entries.map { arch -> async { nodeUtility(BuildSpec(cls, arch, config.utilityLevel, seedOf(cls, arch, config.utilityLevel))) } } }.awaitAll().flatten()
        val skills = classes.map { cls -> async { skillUtility(cls, builds) } }.awaitAll().flatten()
        BalanceData(config.name, config.levels, config.runs, builds = builds, progress = progress(builds), loot = loot(measured),
            nodes = nodes(utility), untakenNodes = untaken(utility, measured.flatMap { it.tree }), skills = skills)
    }

    private class Measured(val result: BuildResult, val runs: List<MapRun>, val tree: Set<String>)

    /**
     * The build's own zone decides its difficulty; its loot and pace are measured where it would farm — the
     * highest zone, stepping down [FARM_STEP] levels at a time, where it dies in at most [FARM_DEATHS] of the runs.
     */
    private fun measure(spec: BuildSpec): Measured {
        val hero = factory.build(spec)
        val own = zoneFor(spec.level)
        fun runs(zone: Zone) = (0 until config.runs).map { maps.run(hero, zone, spec.seed * 1_000 + zone.level * 17 + it) }
        val ownRuns = runs(own)
        var zone = own
        var runs = ownRuns
        while (runs.deaths() > FARM_DEATHS && zone.level > zones.minOf { it.level }) {
            zone = zoneFor(zone.level - FARM_STEP)
            runs = runs(zone)
        }
        val hours = runs.sumOf { it.seconds } / 3600
        val bench = arena(own, spec.heroClass).bench(hero.gear)
        val stats = hero.sheet.stats
        val resist = RESISTS.map { (stats[it] ?: 0.0) + (stats[RESIST_ALL] ?: 0.0) }.average()
        return Measured(BuildResult(spec.heroClass, spec.archetype.name, spec.level, own.code, own.level,
            stats["STOCK_HEALTH"] ?: 0.0, stats["STOCK_ENERGY_SHIELD"] ?: 0.0, resist, hero.sheet.inactive.size,
            ownDeathRate = ownRuns.deaths(), farmZone = zone.code, farmZoneLevel = zone.level,
            clearMinutes = runs.filterNot { it.died }.map { it.seconds / 60 }.averageOrZero(),
            deathRate = runs.deaths(),
            killsPerMinute = runs.sumOf { it.kills } / (hours * 60),
            experiencePerHour = runs.filterNot { it.died }.sumOf { it.experience } / hours,
            goldPerHour = runs.filterNot { it.died }.sumOf { it.gold } / hours,
            arenaSecondsPerPack = bench.secondsPerPack, arenaLossRate = bench.lossRate,
            bossSeconds = bench.bossSeconds, bossWinRate = bench.bossWinRate), runs, hero.tree.map { it.code }.toSet())
    }

    /** At each level, a build a half slower than the level's median in the arena is weak, a third faster strong. */
    private fun flag(builds: List<BuildResult>): List<BuildResult> = builds.groupBy { it.level }.flatMap { (_, same) ->
        val median = same.map { it.arenaSecondsPerPack + it.arenaLossRate * Bench.LOSS_SECONDS }.sorted().let { it[it.size / 2] }
        same.map { b ->
            val cost = b.arenaSecondsPerPack + b.arenaLossRate * Bench.LOSS_SECONDS
            b.copy(flag = when {
                b.ownDeathRate >= 0.5 -> "гибнет"
                cost > median * 1.5 -> "слабый"
                cost < median / 1.5 -> "сильный"
                else -> ""
            })
        }
    }

    private fun progress(builds: List<BuildResult>): List<ProgressResult> = builds.groupBy { it.heroClass to it.archetype }.map { (key, rows) ->
        val rate = rows.sortedBy { it.level }.map { it.level.toDouble() to it.experiencePerHour.coerceAtLeast(1.0) }
        fun perHour(level: Int): Double {
            val lower = rate.lastOrNull { it.first <= level } ?: rate.first()
            val upper = rate.firstOrNull { it.first >= level } ?: rate.last()
            if (upper.first == lower.first) return lower.second
            val t = (level - lower.first) / (upper.first - lower.first)
            return lower.second + (upper.second - lower.second) * t
        }
        val table = index.classes
        var hours = 0.0
        val to = HashMap<Int, Double>()
        for (level in 1 until table.maxLevel) {
            val need = (table.threshold(level + 1) ?: break) - (table.threshold(level) ?: 0.0)
            hours += need / perHour(level)
            if (level + 1 in MILESTONES) to[level + 1] = hours
        }
        ProgressResult(key.first, key.second, to.toSortedMap())
    }

    private fun loot(measured: List<Measured>): List<LootResult> = measured.groupBy { it.result.level }.toSortedMap().map { (level, rows) ->
        val runs = rows.flatMap { it.runs }.filterNot { it.died }
        val hours = rows.flatMap { it.runs }.sumOf { it.seconds } / 3600
        val items = HashMap<String, Double>()
        runs.forEach { run -> run.reward.items.forEach { (code, n) -> items.merge(code, n.toDouble(), Double::plus) } }
        val equipment = runs.flatMap { it.reward.equipment }
        LootResult(level, runs.sumOf { it.gold } / hours, items.mapValues { it.value / hours }.toSortedMap(),
            equipment.count { it.rarity == Rarity.RARE } / hours, equipment.count { it.rarity == Rarity.UNIQUE || it.rarity == Rarity.MYTHICAL } / hours,
            items.entries.sumOf { (code, n) -> n * (index.item(code)?.price ?: 0L) } / hours)
    }

    private class NodeLoss(val code: String, val type: SkillNodeType, val loss: Double, val taken: Set<String>)

    /**
     * Every notable, keystone and mastery of the build taken away in turn: how much weaker the hero is without it,
     * in percent — against the zone's guardians ([Arena.duel]), or, for a farmer, in the loot its sheet brings.
     */
    private fun nodeUtility(spec: BuildSpec): List<NodeLoss> {
        val hero = factory.build(spec)
        val arena = arena(zoneFor(spec.level), spec.heroClass)
        fun worth(built: BuiltHero) = if (spec.archetype == Archetype.FARMER) FarmValue.of(built.sheet.stats) else arena.duel(built.gear).power
        val base = worth(hero)
        val taken = hero.tree.map { it.code }.toSet()
        return hero.tree.mapNotNull { node -> index.tree.node(node.code)?.takeIf { it.type in WEIGHED }?.let { it to node } }.map { (def, node) ->
            val without = factory.build(spec, hero.tree - node) { hero.skills }
            NodeLoss(def.code, def.type, (base / worth(without) - 1) * 100, taken)
        }.ifEmpty { listOf(NodeLoss("", SkillNodeType.START, 0.0, taken)) }
    }

    private fun nodes(losses: List<NodeLoss>): List<NodeResult> = losses.filter { it.code.isNotEmpty() }.groupBy { it.code }.map { (code, rows) ->
        NodeResult(code, rows.first().type.name, rows.size, rows.map { it.loss }.average())
    }.sortedByDescending { it.loss }

    private fun untaken(losses: List<NodeLoss>, measured: Collection<String>): Map<String, Int> {
        val taken = losses.flatMap { it.taken }.toSet() + measured
        return index.tree.byCode.values.filter { it.type in WEIGHED && it.code !in taken }.groupingBy { it.type.name }.eachCount()
    }

    /** Each of the class's skills alone in the class's best build at the utility level; passives beside its first attack. */
    private fun skillUtility(heroClass: String, builds: List<BuildResult>): List<SkillResult> {
        val best = builds.filter { it.heroClass == heroClass && it.level == config.utilityLevel }
            .minByOrNull { it.arenaSecondsPerPack + it.arenaLossRate * Bench.LOSS_SECONDS } ?: return emptyList()
        val spec = BuildSpec(heroClass, Archetype.valueOf(best.archetype), best.level, seedOf(heroClass, Archetype.valueOf(best.archetype), best.level))
        val hero = factory.build(spec)
        val arena = arena(zoneFor(spec.level), heroClass)
        val plain = arena.bench(factory.gear(heroClass, spec.level, hero.items, HeroSkills(), hero.sheet), withBoss = false).cost
        return index.skills.ofClass(heroClass).mapNotNull { skill ->
            val skills = factory.alone(skill, spec.level, hero.sheet.stats) ?: return@mapNotNull null
            val bench = arena.bench(factory.gear(heroClass, spec.level, hero.items, skills, hero.sheet), withBoss = false)
            SkillResult(heroClass, skill.code, skill.type.name, bench.secondsPerPack, bench.lossRate, bench.cost / plain)
        }.sortedBy { it.relative }
    }

    private val arenas = java.util.concurrent.ConcurrentHashMap<Pair<String, String>, Arena>()
    private fun arena(zone: Zone, heroClass: String): Arena = arenas.getOrPut(zone.code to heroClass) {
        Arena(index, zone, (1..config.arenaSeeds).map { it * 104_729L }, heroClass, zone.level)
    }

    /** The zone a hero of [level] farms: the highest one not above it. */
    private fun zoneFor(level: Int): Zone = zones.filter { it.level <= level }.maxByOrNull { it.level } ?: zones.minBy { it.level }

    private fun seedOf(cls: String, arch: Archetype, level: Int): Long = (cls.hashCode() * 31L + arch.ordinal) * 131 + level

    private fun List<Double>.averageOrZero() = if (isEmpty()) 0.0 else average()
    private fun List<MapRun>.deaths() = count { it.died }.toDouble() / size

    private companion object {
        val RESISTS = listOf("STOCK_RESIST_FIRE", "STOCK_RESIST_COLD", "STOCK_RESIST_LIGHTNING", "STOCK_RESIST_CHAOS")
        const val RESIST_ALL = "STOCK_RESIST_ALL"
        val WEIGHED = setOf(SkillNodeType.NOTABLE, SkillNodeType.KEYSTONE, SkillNodeType.MASTERY)
        val MILESTONES = setOf(10, 30, 50, 70, 90, 100)
        const val FARM_DEATHS = 0.25
        const val FARM_STEP = 4
    }
}
