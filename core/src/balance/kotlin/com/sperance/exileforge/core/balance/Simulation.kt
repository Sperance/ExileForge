package com.sperance.exileforge.core.balance

import com.sperance.exileforge.core.campaign.AutoPlan
import com.sperance.exileforge.core.campaign.Battle
import com.sperance.exileforge.core.campaign.Combatant
import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.Foe
import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Outcome
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.RunPhase
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.WorldKind
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.RarityBonus
import com.sperance.exileforge.rules.run.RewardDraws
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.run.RunEventKind
import kotlin.random.Random

/** One autorun of a map to its end: how long it took, whether the hero lived, and what it paid. */
data class MapRun(val seconds: Double, val died: Boolean, val kills: Int, val experience: Double, val gold: Long, val reward: Reward)

/**
 * Autoruns of a zone as the app plays them (3.2.0 autorun): the real [ExpeditionRun] on the seed, walking,
 * resting between steps and fighting every pack, with no chests, crystals or cracks, on no map and no atlas.
 * The guardian is down, as it is when the autorun opens — the farm, not the first clear; the guardian is
 * the arena's to weigh.
 */
class MapSimulator(private val index: ContentIndex) {
    fun run(hero: BuiltHero, zone: Zone, seed: Long): MapRun {
        val gear = hero.gear
        val run = Run(index, zone, seed, context(hero, zone))
        val journal = RunJournal("sim-$seed", "sim", zone.code)
        // The server's part (1.30.0): the rewards of the kills, on a stream of their own the run never sees.
        val draws = RewardDraws(seed xor SERVER_STREAM, 0)
        val expedition = ExpeditionRun.start(index, zone, run, journal, gear, CampaignState(bosses = mapOf(zone.code to Long.MAX_VALUE)), 0L,
            heroExperience = 0.0, heroLevel = hero.spec.level, vaalOrbs = { 0L }, auto = AutoPlan(chests = false, crystals = false, abyss = false))
        var time = 0.0
        while (time < LIMIT) {
            val hud = expedition.hud.value
            when (hud.phase) {
                RunPhase.CLEARED, RunPhase.DEAD, RunPhase.LEFT -> break
                RunPhase.LOOT -> expedition.send(RunCommand.Continue)
                RunPhase.GATE -> expedition.send(RunCommand.StepBack)
                RunPhase.ABYSS, RunPhase.CRYSTAL -> expedition.send(RunCommand.StepOff)
                RunPhase.MAP -> if (hud.auto == null && time > 0) break
                RunPhase.FIGHT -> Unit
            }
            expedition.update(STEP)
            answer(expedition, journal, run, draws)
            time += STEP
        }
        // The last answers land before the count is read.
        expedition.update(0.0)
        val hud = expedition.hud.value
        val died = hud.phase == RunPhase.DEAD || time >= LIMIT
        return MapRun(time, died, hud.kills, hud.experience, hud.gold, hud.autoReward ?: Reward())
    }

    /** The journal answered as the server would: every kill paid on the server's stream, nothing else on this farm. */
    private fun answer(expedition: ExpeditionRun, journal: RunJournal, run: Run, draws: RewardDraws) {
        val pending = journal.pending.takeIf { it.isNotEmpty() } ?: return
        val rewards = pending.filter { it.kind == RunEventKind.KILL }.mapNotNull { e -> run.kill(e.i, e.m, e.vaal, draws)?.let { e.n to it } }.toMap()
        journal.confirm(pending.last().n + 1)
        expedition.send(RunCommand.Settled(journal.applied, rewards))
    }

    /** What the server would hand the run: the hero's quantity, rarity, experience and gold, and the uniques' world powers. */
    private fun context(hero: BuiltHero, zone: Zone): RunContext {
        val sheet = hero.sheet.stats
        val bonuses = MonsterRarity.entries.associate { rarity ->
            fun power(kind: WorldKind) = index.powers.worldBonus(sheet, kind, rarity)
            rarity.name to RarityBonus(
                quantity = (sheet[CoreStat.QUANTITY.code] ?: 0.0) + power(WorldKind.QUANTITY),
                rarity = (sheet[CoreStat.RARITY.code] ?: 0.0) + power(WorldKind.RARITY),
                experience = (sheet[CoreStat.EXPERIENCE.code] ?: 0.0) + power(WorldKind.EXPERIENCE),
                gold = (sheet[CoreStat.GOLD.code] ?: 0.0) + power(WorldKind.GOLD),
                map = power(WorldKind.MAP), book = power(WorldKind.BOOK), unique = power(WorldKind.UNIQUE),
            )
        }
        return RunContext(hero.spec.heroClass, hero.spec.level, bonuses, next = index.world.next(zone.code))
    }

    private companion object {
        const val STEP = 0.1
        const val SERVER_STREAM = 0x5345525645L
        /** A run that has not ended in half an hour is stuck; it counts as lost. */
        const val LIMIT = 1800.0
    }
}

/** Seconds to kill a harmless guardian and seconds to fall to an undying one; their ratio is the hero's power against guardians. */
data class Duel(val killSeconds: Double, val surviveSeconds: Double) {
    val power: Double get() = surviveSeconds / killSeconds.coerceAtLeast(0.1)
}

/**
 * A farming hero's worth from the sheet alone: quantity and rarity of loot, gold, chests, experience and pace,
 * multiplied, each at the weight of what it pays. Loot per hour measured by runs is too noisy to weigh one node.
 */
object FarmValue {
    private val WEIGHTS = mapOf("STOCK_QUANTITY" to 1.0, "STOCK_RARITY" to 0.5, "STOCK_GOLD" to 0.3, "STOCK_CHEST_QUANTITY" to 0.2,
        "STOCK_EXPERIENCE" to 0.5, "STOCK_MOVEMENT_SPEED" to 0.5)

    fun of(stats: Map<String, Double>): Double = WEIGHTS.entries.fold(1.0) { value, (stat, weight) -> value * (1 + (stats[stat] ?: 0.0) / 100 * weight) }
}

/** The arena's verdict on a hero: seconds per pack and the share of packs that killed them; the guardians apart. */
data class Bench(val secondsPerPack: Double, val lossRate: Double, val bossSeconds: Double = 0.0, val bossWinRate: Double = 0.0) {
    /** One number to rank by: time per pack, a loss costing a minute. */
    val cost: Double get() = secondsPerPack + lossRate * LOSS_SECONDS

    companion object { const val LOSS_SECONDS = 60.0 }
}

/**
 * Fights alone, with none of the map: the same packs of a zone, each met with full life, so two heroes
 * — or one hero with and without a node — are compared on exactly the same foes.
 */
class Arena(private val index: ContentIndex, zone: Zone, seeds: List<Long>, heroClass: String, level: Int) {
    private val rules = index.campaign.combat
    private val level = zone.level
    private val spawns = seeds.map { seed -> Spawns(index, Run(index, zone, seed, RunContext(heroClass, level))) }
    private val packs: List<List<RolledMonster>> = spawns.flatMap { it.packs(false, emptyList()).take(PACKS_PER_SEED) }
    private val bosses: List<List<RolledMonster>> = spawns.mapNotNull { it.boss(zone, emptyList(), emptyList()) }.map { listOf(it) }

    fun bench(gear: HeroGear, withBoss: Boolean = true): Bench {
        val build = HeroBuild(gear, emptyMap(), rules)
        val (packSeconds, packWins) = fightAll(build, gear, packs, FIGHT_LIMIT)
        val (bossSeconds, bossWins) = if (withBoss && bosses.isNotEmpty()) fightAll(build, gear, bosses, BOSS_LIMIT) else 0.0 to 0.0
        return Bench(packSeconds, 1 - packWins, bossSeconds, bossWins)
    }

    /**
     * The hero against the zone's guardians, split in two so neither side saturates: how long the hero takes to
     * kill a guardian that deals no damage, and how long they last against one that cannot die. [Duel.power] is
     * the second over the first — how many guardians the hero outlasts — the measure a node or a skill is weighed by.
     */
    fun duel(gear: HeroGear): Duel {
        val build = HeroBuild(gear, emptyMap(), rules)
        fun timed(harmless: Boolean) = bosses.mapIndexed { i, pack ->
            val boss = pack.single()
            val stats = boss.stats.mapValues { (stat, value) -> when {
                harmless && stat in DAMAGE -> 0.0
                !harmless && stat == HEALTH -> value * UNDYING
                else -> value } }
            val hero = build.body
            val battle = Battle(hero, listOf(Foe(Combatant(stats, level, rules), boss.ranged, boss.rarity, if (harmless) emptyList() else boss.skills.mapNotNull(index.skills.monsterByCode::get))),
                rules, hero.maxLife, Random(i.toLong() * 7919), gear.stance, kit = gear.kit, model = build, percent = gear.percent)
            while (battle.outcome == null && battle.time < DUEL_LIMIT) battle.advance(1.0)
            battle.time
        }.average()
        return Duel(timed(harmless = true), timed(harmless = false))
    }

    /** Every pack of [foes] met with full life: the mean seconds of the fights and the share won. */
    private fun fightAll(build: HeroBuild, gear: HeroGear, foes: List<List<RolledMonster>>, limit: Double): Pair<Double, Double> {
        val hero = build.body
        var seconds = 0.0
        var wins = 0
        foes.forEachIndexed { i, pack ->
            val battle = Battle(hero, pack.map(::foe), rules, hero.maxLife, Random(i.toLong() * 7919), gear.stance, kit = gear.kit, model = build, percent = gear.percent)
            while (battle.outcome == null && battle.time < limit) battle.advance(1.0)
            seconds += battle.time
            if (battle.outcome == Outcome.WIN) wins++
        }
        return seconds / foes.size to wins.toDouble() / foes.size
    }

    private fun foe(monster: RolledMonster) =
        Foe(Combatant(monster.stats, level, rules), monster.ranged, monster.rarity, monster.skills.mapNotNull(index.skills.monsterByCode::get))

    private companion object {
        const val PACKS_PER_SEED = 6
        const val FIGHT_LIMIT = 120.0
        const val BOSS_LIMIT = 300.0
        const val DUEL_LIMIT = 600.0
        const val UNDYING = 1_000.0
        const val HEALTH = "STOCK_HEALTH"
        val DAMAGE = setOf("STOCK_ATTACK_PHYSICAL", "STOCK_ATTACK_FIRE", "STOCK_ATTACK_COLD", "STOCK_ATTACK_LIGHTNING", "STOCK_ATTACK_CHAOS", "STOCK_ATTACK_MAGICAL",
            "STOCK_REFLECT", "STOCK_THORNS")
    }
}
