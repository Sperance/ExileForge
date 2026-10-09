package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.MapStats
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.FightKinds
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.LossCause
import com.sperance.exileforge.core.campaign.run.OddsPlan
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.sheet.CombatProfile
import java.io.File
import java.util.Locale

/** Исход одного класса одним билдом против босса зоны: победы из боёв, средняя длина боя и поражения по причинам. */
data class SimCell(
    val heroClass: String,
    val build: String,
    val zone: String,
    val boss: String,
    val level: Int,
    val bossLife: Double,
    val wins: Int,
    val fights: Int,
    val seconds: Double,
    val causes: Map<LossCause, Int>,
    /** Герой в бою: здоровье и щит, среднее сопротивление стихиям после штрафа акта (доля), урон по [CombatProfile]. */
    val heroPool: Double = 0.0,
    val heroResist: Double = 0.0,
    val heroDamage: Double = 0.0,
) {
    val share: Double get() = if (fights > 0) wins.toDouble() / fights else 0.0
    val cause: LossCause? get() = causes.maxByOrNull { it.value }?.key
}

/**
 * Баланс-сим «классы против боссов» (отчёт, не тест): каждый класс каждым билдом [strategies] на уровне зоны против её босса.
 * Бой - прогон «Весов» ([OddsPlan.run]) без своей логики боя: [rolls] бросков босса (как в заходе, по семени захода), на
 * каждый - `fights / rolls` боёв на своих костях. Детерминирован семенами.
 */
class BossSim(private val index: ContentIndex, private val fights: Int, private val rolls: Int, private val strategies: List<BuildStrategy>) {
    private val kit = SimKit(index)
    private val rules = index.campaign.combat

    /** Зоны похода с боссом по уровню; [only] - коды зон, пусто - все. */
    fun zones(only: Set<String> = emptySet()): List<Zone> = index.campaign.zones
        .filter { it.boss.value.isNotBlank() && index.monster(it.boss) != null && (only.isEmpty() || it.code.value in only) }
        .sortedWith(compareBy({ it.level }, { it.code.value }))

    fun cells(zones: List<Zone>, classes: List<HeroClass>): List<SimCell> = zones.parallelStream().flatMap { zone -> zone(zone, classes).stream() }.toList()

    private fun zone(zone: Zone, classes: List<HeroClass>): List<SimCell> {
        val phases = PhaseFoes(index, rules)
        val foes = List(rolls) { r ->
            val run = Run(index, zone, SimKit.SEED + r, RunContext(classes.first().code, zone.level))
            val boss = Spawns(index, run).boss(zone, emptyList(), emptyList()) ?: error("no boss in ${zone.code}")
            listOf(phases.foe(boss, run.levelOf(zone)))
        }
        val penalty = index.campaign.resistPenalty(zone.code, onMap = false)
        val effects = if (penalty > 0) mapOf(MapStats.HERO_RESIST to penalty) else emptyMap()
        return classes.flatMap { heroClass ->
            strategies.map { strategy ->
                val build = strategy.build(kit, heroClass, zone.level, penalty)
                val plans = foes.map { pack -> plan(build, heroClass, zone.level, pack, effects, phases) }
                val odds = plans.map { it.first.run(fights / rolls) }
                val body = plans.first().second
                SimCell(
                    heroClass.code, strategy.name, zone.code.value, zone.boss.value, zone.level, foes.map { it.first().body.maxLife }.average(),
                    odds.sumOf { it.wins }, odds.sumOf { it.fights }, odds.sumOf { it.seconds * it.fights } / odds.sumOf { it.fights }.coerceAtLeast(1),
                    odds.flatMap { it.causes.entries }.groupingBy { it.key }.fold(0) { sum, entry -> sum + entry.value },
                    body.maxLife + body.maxShield, DamageType.ELEMENTS.map(body::resist).average(), CombatProfile.of(index, body.stats).best,
                )
            }
        }
    }

    /** Снимок боя героя билда [build] против [foes], как его снимает поход: полные запасы, без питомца, вид боя - по врагам. */
    private fun plan(build: SimBuild, heroClass: HeroClass, level: Int, foes: List<com.sperance.exileforge.core.campaign.combat.Foe>, effects: Map<String, Double>, phases: PhaseFoes): Pair<OddsPlan, Combatant> {
        val sheet = Sheets.calculate(index, level, heroClass.code, build.tree, build.items)
        val loadout = Loadout.of(build.skills, index, heroClass.code, build.flasks, sheet.stats, index.powers, index.rules.charges)
        val gear = HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass.code), loadout, index.stats.percent)
        val hero = HeroBuild(gear, effects, rules)
        val body = hero.body
        val pools = HeroPools(
            body.maxLife,
            body.maxMana * (1 - loadout.reserved(body) / 100),
            loadout.flasks.map { it?.sheet?.maxCharges ?: 0.0 },
            loadout.flasks.map { 0.0 },
            loadout.flasks.map { DraughtRate() },
        )
        return OddsPlan(body, foes, rules, index.rules.fight, pools, gear.stance, loadout, hero, gear.percent, null, phases, false, FightKinds.expedition(foes)) to body
    }
}

/** Отчёт сима: сверху - боссы вне коридора (среднее по классам), ниже - класс × босс по каждому билду. */
object BossReport {
    private fun pct(value: Double) = String.format(Locale.ROOT, "%.0f%%", value * 100)
    private fun sec(value: Double) = String.format(Locale.ROOT, "%.0f", value)

    fun cause(cause: LossCause?): String = when (cause) {
        null -> "-"
        LossCause.Enrage -> "enrage"
        LossCause.Timeout -> "timeout"
        is LossCause.Damage -> cause.type.name.lowercase()
    }

    /** Среднее по классам доли побед билда [build] против босса зоны [zone]. */
    private fun mean(cells: List<SimCell>) = cells.map { it.share }.average()

    fun markdown(cells: List<SimCell>, strategies: List<BuildStrategy>, fights: Int, rolls: Int): String = buildString {
        val zones = cells.map { it.zone }.distinct()
        val classes = cells.map { it.heroClass }.distinct()
        val byZone = cells.groupBy { it.zone to it.build }
        appendLine("# Boss simulation: classes against zone bosses")
        appendLine()
        appendLine("Hero level = zone level. $fights fights per cell: $rolls boss rolls × ${fights / rolls} fights (OddsPlan, cap 180 s).")
        strategies.forEach { appendLine("- `${it.name}`: corridor ${pct(it.corridor.start)}-${pct(it.corridor.endInclusive)} wins (mean over classes)") }
        appendLine()
        appendLine("## Out of corridor")
        appendLine()
        appendLine("| zone | level | boss | build | mean wins | corridor |")
        appendLine("| --- | --- | --- | --- | --- | --- |")
        var out = 0
        zones.forEach { zone ->
            strategies.forEach { strategy ->
                val row = byZone[zone to strategy.name].orEmpty()
                val mean = mean(row)
                if (row.isNotEmpty() && mean !in strategy.corridor) {
                    out++
                    val first = row.first()
                    appendLine("| $zone | ${first.level} | ${first.boss} | ${strategy.name} | ${pct(mean)} | ${if (mean < strategy.corridor.start) "below" else "above"} |")
                }
            }
        }
        appendLine()
        appendLine("Out of corridor: $out of ${zones.size * strategies.size}")
        appendLine()
        appendLine("## Heroes by zone (mean over classes): life + shield, elemental resistance after the act's penalty, damage")
        appendLine()
        appendLine((listOf("zone", "lvl") + strategies.flatMap { listOf("${it.name} pool", "${it.name} resist", "${it.name} damage") }).joinToString(" | ", "| ", " |"))
        appendLine((listOf("zone", "lvl") + strategies.flatMap { listOf("${it.name} pool", "${it.name} resist", "${it.name} damage") }).joinToString(" | ", "| ", " |") { "---" })
        zones.forEach { zone ->
            val level = cells.first { it.zone == zone }.level
            val own = strategies.flatMap { strategy ->
                val row = byZone[zone to strategy.name].orEmpty()
                listOf(sec(row.map { it.heroPool }.average()), pct(row.map { it.heroResist }.average()), sec(row.map { it.heroDamage }.average()))
            }
            appendLine((listOf(zone, "$level") + own).joinToString(" | ", "| ", " |"))
        }
        strategies.forEach { strategy ->
            appendLine()
            appendLine("## Build `${strategy.name}`: wins / mean seconds / most frequent loss")
            appendLine()
            appendLine((listOf("zone", "lvl", "boss", "life", "mean") + classes).joinToString(" | ", "| ", " |"))
            appendLine((listOf("zone", "lvl", "boss", "life", "mean") + classes).joinToString(" | ", "| ", " |") { "---" })
            zones.forEach { zone ->
                val row = byZone[zone to strategy.name].orEmpty()
                val first = row.firstOrNull() ?: return@forEach
                val own = classes.map { c -> row.firstOrNull { it.heroClass == c }?.let { "${pct(it.share)} / ${sec(it.seconds)} / ${cause(it.cause)}" } ?: "" }
                appendLine((listOf(zone, "${first.level}", first.boss, sec(first.bossLife), pct(mean(row))) + own).joinToString(" | ", "| ", " |"))
            }
        }
    }
}

/**
 * Запуск: `./gradlew :core:simulateBosses [-Pcontent=<папка content>] [-Pfights=<K>] [-Prolls=<R>] [-Pzones=<коды через запятую>]`.
 * Отчёт - `build/reports/boss-sim.md` модуля.
 */
fun main(args: Array<String>) {
    val content = File(System.getProperty("content") ?: "../backend/src/main/resources/content")
    val fights = System.getProperty("fights")?.toIntOrNull() ?: 100
    val rolls = (System.getProperty("rolls")?.toIntOrNull() ?: 5).coerceIn(1, fights)
    val zones = System.getProperty("zones").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val out = File(args.firstOrNull() ?: "build/reports/boss-sim.md")
    val index = ContentLoader.load { File(content, it).readText() }
    val strategies = listOf(WorstBuild, AverageBuild)
    val sim = BossSim(index, fights, rolls, strategies)
    val classes = index.classes.classes.mapNotNull { index.heroClass(it.code) }
    val started = System.nanoTime()
    val cells = sim.cells(sim.zones(zones), classes)
    out.parentFile.mkdirs()
    out.writeText(BossReport.markdown(cells, strategies, fights, rolls))
    println("boss-sim: ${cells.size} cells in ${(System.nanoTime() - started) / 1_000_000_000} s -> ${out.path}")
}
