package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.FightFigures
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.RunStats
import com.sperance.exileforge.core.campaign.StageCarry
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Ally
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.FlaskView
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.SkillView
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ==================== Fights ====================
/**
 * A fight with [agent]'s pack still standing, all at once, at [level] — the zone's, or a depth's of the Abyss.
 * On the map the packs standing close by are drawn in (3.26.0) and, since 3.28.0, fought a stage at a time — small packs in a row merged into one (3.70.0):
 * the engaged one first, then the rest by their distance to it; the boss and a guardian always fight alone.
 */
internal fun ExpeditionRun.engage(agent: MonsterAgent, level: Int = zone.level, abyssal: Boolean = false, carry: StageCarry? = null) {
    fightPet = pet()
    fightAgents = if (abyssal) listOf(agent) else world.gathered(agent)
    fightStages = if (abyssal) listOf(fightAgents) else world.stages(fightAgents)
    fightStrongest = fightAgents.flatMap { pack -> pack.standing.map { pack.pack[it] } }.maxByOrNull { it.rarity.ordinal }
    stageHits = emptyList()
    stageTime = 0.0
    // Every fight's end drops the carry: one still held here is the journal's, kept over a restart mid-fight.
    stageCarry = carry ?: stageCarry
    fightAgent = agent
    abyssFight = abyssal
    fightLevel = level
    begin(1)
}

/**
 * Stage [number] of the fight stands up, the hero as the last one left them — life, mana, charges and the draughts
 * running, no ailment. A later stage waits `expedition.stagePause` seconds, or the player's word, and an autorun not at all.
 */
internal fun ExpeditionRun.begin(number: Int) {
    stage = number
    members = fightStages[number - 1].flatMap { pack -> pack.standing.map { FightMember(pack, it) } }
    reported = 0
    fightStream = (fights++).toLong()
    fight = battle()
    // An autorun goes straight on; the player gets the pause.
    started = number > 1 && autopilot != null
    interlude = pace.stagePause.takeIf { number > 1 && !started }
    paused = false
    phase = RunPhase.FIGHT
}

/** The stage's battle on the hero's pools now, at the fight's level, on its own dice. */
internal fun ExpeditionRun.battle(): Battle = Battle(
    hero,
    members.map { member ->
        val monster = member.monster
        // Its own level on a map (3.73.0), the fight's otherwise.
        val level = monster.level.takeIf { it > 0 } ?: fightLevel
        Foe(
            Combatant(monster.stats, level, rules),
            monster.rarity,
            monster.skills.mapNotNull(this.index.skills.monsterByCode::get),
            monster,
            level,
            monster.traitsIn(this.index),
            this.index.campaign.traits.power(monster.rarity),
        )
    },
    rules, life, Random(Streams.mix(seed, ExpeditionRun.FIGHT_STREAM, fightStream)), stance, kit = kit, model = build, pools = pools,
    percent = build.gear.percent, ally = ally(), stage = stageCarry,
)

/** The fight is over, whichever way: nothing of it is held any longer. */
internal fun ExpeditionRun.endFight() {
    fight = null
    fightAgent = null
    fightAgents = emptyList()
    fightStages = emptyList()
    members = emptyList()
    stage = 0
    interlude = null
    stageHits = emptyList()
    stageTime = 0.0
    stageCarry = null
    fightStrongest = null
}

/** «Освободить»: the crystal's guardian stands up — the zone's monster, rare, with the lines of the essences it guards — and the fight begins. */
internal fun ExpeditionRun.release() {
    val spot = crystal ?: return
    val extra = MapEffects.guardianBuffs(spot.crystal.stronger, index.essences.crystals.stronger, mapEffects)
    val guardian = spawns.crystalGuardian(zone, spot.crystal, spot.id, MapEffects.buffs(mapEffects), extra) ?: return
    crystal = null
    crystalOutcome = null
    engage(MonsterAgent(-1 - spot.id, listOf(guardian), spot.cell.x + 0.5, spot.cell.y + 0.5, crystal = spot.id))
}

/** A foe of the fight fell: the event by what it was; its reward comes with the server's answer. */
internal fun ExpeditionRun.fell(agent: MonsterAgent, member: Int) {
    kills++
    if (agent === world.boss) bosses++
    if (abyssFight) return
    val spot = agent.crystal?.let { id -> world.crystals.firstOrNull { it.id == id } }
    val event = when {
        spot != null -> record { RunEvent.Crystal(it, world.standingCrystals.indexOf(spot)) }.also { spot.freed = true }
        agent === world.boss -> (if (vaal) record(RunEvent::Corrupt) else record(RunEvent::Boss))?.also { bossDown = true }
        else -> record { RunEvent.Kill(it, agent.id, member, vaal) }
    }
    rewarding(event, fought = true)
}

internal fun ExpeditionRun.play(dt: Double) {
    val battle = fight ?: return
    fightAgent ?: return
    // The pause between stages runs out by itself, at the wall clock's pace.
    if (!started) {
        interlude?.let { left ->
            if (left > dt) {
                interlude = left - dt
            } else {
                interlude = null
                started = true
            }
        }
    }
    if (!started || paused) return
    battle.advance(dt * speed)
    // Every foe is a kill of its own, recorded the moment it falls, the fight still going.
    while (reported < battle.fallen.size) {
        val member = members[battle.fallen[reported++]]
        member.agent.fallen += member.index
        // A pack of a merged stage can be wiped out while the stage still runs: it is gone at once, not a ghost after a retreat.
        if (member.agent.standing.isEmpty()) member.agent.alive = false
        fell(member.agent, member.index)
    }
    val outcome = battle.outcome ?: return
    if (battle.time < battle.duration + pace.aftermath) return
    val out = battle.pools()
    life = out.life
    mana = out.mana
    charges = out.charges
    flaskLeft = out.flaskLeft
    rates = out.rates
    rebody()
    val pack = stageHits + members.mapIndexed { index, member -> PackHit(member.monster, battle.events.filter { it.foe == index }, battle.duration) }
    val duration = stageTime + battle.duration
    // A stage won with packs still waiting: the next one stands up, and the report waits for the last.
    if (outcome == Outcome.WIN && stage < fightStages.size) {
        fightStages[stage - 1].forEach { it.alive = false }
        stageHits = pack
        stageTime = duration
        stageCarry = battle.carry()
        pendingGear?.let { regear(it.gear) }
        pendingGear = null
        begin(stage + 1)
        return
    }
    stats.add(pack, duration)
    // The fight's figures (3.51.0): only the hero's statistics, before a fall closes the journal.
    record { n -> RunEvent.Fight(n, FightFigures.of(pack, duration, boss = fightAgents.any { it === world.boss }, won = outcome == Outcome.WIN)) }
    val leader = fightStrongest ?: fightLeader()
    val down = descent?.takeIf { abyssFight }
    when (outcome) {
        Outcome.WIN -> {
            fightAgents.forEach { it.alive = false }
            if (down != null) {
                report = null
                phase = RunPhase.ABYSS
            }
            // An autorun goes on without the report: what the fight brought is in its tally already
            else if (autopilot != null) {
                clearSpoils()
                slain = null
                report = null
                phase = RunPhase.MAP
            } else {
                slain = leader
                report = FightReport(leader, Outcome.WIN, pack, duration)
                phase = RunPhase.LOOT
            }
        }

        Outcome.LOSS -> {
            report = FightReport(leader, Outcome.LOSS, pack, duration)
            recap = RunStats.recap(pack)
            life = 0.0
            autopilot = null
            phase = RunPhase.DEAD
            deaths++
            end = MapEnd.FELL
            // A fall in the Abyss burns its hoard, but for the atlas's share; then the zone's own price.
            down?.let { take(it, fallen = true) }
            // A fall in the Vaal zone is a death as any (3.71.0, server 1.68.0): the same price, and the whole run is over
            record { RunEvent.Fall(it, vaal) }?.let {
                fallEvent = it.n
                fall = deathLoss()
            }
            onFallen()
        }

        // Nothing already looted is lost, but there is no report for a fight cut short: the packs of the stages won stay dead,
        // the current one and those still waiting go back to their places.
        Outcome.RETREAT -> {
            // Walking out of a fight takes the run back into the player's hands
            autopilot = null
            fightAgents.filter { it.id >= 0 && it.alive }.forEach(world::retreatFrom)
            report = null
            clearSpoils()
            slain = null
            if (down != null) {
                phase = RunPhase.ABYSS
                take(down, fallen = true)
            } else {
                phase = RunPhase.MAP
            }
        }
    }
    endFight()
    abyssFight = false
    if (phase != RunPhase.DEAD) pendingGear?.let { regear(it.gear) }
    pendingGear = null
    // The Abyss chains its fights (3.32.0): the next wave, or the next depth once the player descends, is the next stage.
    if (outcome == Outcome.WIN && down != null) {
        down.carry = battle.carry()
        if (down.fights.isNotEmpty()) nextFight(down) else down.cleared++
    }
}

/** What the death costs by the rules: a share of the level's experience, never the level — on what the answers granted so far. */
internal fun ExpeditionRun.deathLoss(): Double {
    val classes = index.classes
    val gained = heroExperience + granted.experience
    val level = classes.levelOf(gained).coerceAtLeast(heroLevel)
    return LootRoller(index).deathLoss(rules.death, zone.level, gained, classes.threshold(level) ?: 0.0, classes.nextThreshold(level))
}
