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
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
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
        val expedition = ExpeditionRun.start(index, zone, run, RunJournal("sim-$seed", "sim", zone.code), gear, CampaignState(bosses = mapOf(zone.code to Long.MAX_VALUE)), 0L,
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
            time += STEP
        }
        val hud = expedition.hud.value
        val died = hud.phase == RunPhase.DEAD || time >= LIMIT
        return MapRun(time, died, hud.kills, hud.experience, hud.gold, hud.autoReward ?: Reward())
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
        /** A run that has not ended in half an hour is stuck; it counts as lost. */
        const val LIMIT = 1800.0
    }
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
    }
}
