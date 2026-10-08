package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.FightFigures
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.RunStats
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
import com.sperance.exileforge.rules.roll.Dice
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
 * A fight with [agent]'s pack still standing, all at once, at [level] — the zone's, or a depth's of the Abyss. Пачка карты
 * не больше `fight.maxFoes` (4.2.0): соседние стаи в бой не втягиваются, каждая - свой бой.
 */
internal fun ExpeditionRun.engage(agent: MonsterAgent, level: Int = zone.level, abyssal: Boolean = false) {
    fightPet = pet()
    fightAgent = agent
    abyssFight = abyssal
    fightLevel = level
    if (!abyssal) announce(agent)
    members = agent.standing.map { FightMember(agent, it) }
    reported = 0
    fightStream = (fights++).toLong()
    fight = battle()
    // «В бой» игрока или автозабега начинает его.
    started = false
    paused = false
    phase = RunPhase.FIGHT
}

/**
 * Бой начат (3.88.0, server 1.80.0): каждому паку - событие ENGAGE, сервер катит добычу его членов сразу и отвечает ею, так
 * что убитый член показывает свою добычу, не дожидаясь ответа на убийство. Страж кристалла и Бездна - не паки жетонов, босс
 * Ваал-зоны - порча, а не убийство босса: им ENGAGE нет.
 */
internal fun ExpeditionRun.announce(pack: MonsterAgent) {
    val key = when {
        pack.crystal != null || pack.id < 0 -> return
        pack === world.boss -> if (vaal) return else FightKey.BOSS
        else -> FightKey(pack.id, vaal, boss = false)
    }
    if (key in engaged.values) return
    record { RunEvent.Engage(it, i = key.pack, vaal = key.vaal, boss = key.boss) }?.let { engaged[it.n] = key }
}

/** Убийство [n] члена [member] боя [key]: добыча, если сервер её уже ответил на ENGAGE, видна сразу. */
internal fun ExpeditionRun.killed(n: Int, key: FightKey, member: Int) {
    killsOf[n] = key to member
    preview(n)
}

/** Показывает добычу убийства [n] из ответа на ENGAGE, один раз; ответ на само убийство её уже не добавит. */
internal fun ExpeditionRun.preview(n: Int) {
    if (n in earned || n in previewed) return
    val (key, member) = killsOf[n] ?: return
    val loot = pendingLoot[key]?.get(member) ?: return
    previewed += n
    if (n in fightEvents) reward = (reward ?: Reward.NONE) + loot
    if (n in autoEvents) autoReward = (autoReward ?: Reward.NONE) + loot
}

/** The fight's battle on the hero's pools now, at the fight's level, on its own dice. */
internal fun ExpeditionRun.battle(): Battle {
    val phases = PhaseFoes(this.index, rules)
    val foes = members.map { member ->
        // Сделки алтаря (3.90.0) ложатся на каждый бой: строки карты, сила босса, лишние строки монстров
        val monster = pactFoe(member)
        // Its own level on a map (3.73.0), the fight's otherwise.
        phases.foe(monster, monster.level.takeIf { it > 0 } ?: fightLevel)
    }
    return Battle(
        hero,
        // Свита фаз босса (3.92.0) - в конце стаи, на своём потоке: бой без фаз катится как прежде
        phases.withRetinue(foes, Dice(Streams.mix(seed, ExpeditionRun.RETINUE_STREAM, fightStream))),
        rules, index.rules.fight, life, Random(Streams.mix(seed, ExpeditionRun.FIGHT_STREAM, fightStream)), stance, kit = kit, model = build, pools = pools,
        percent = build.gear.percent, ally = ally(),
    )
}

/** The fight is over, whichever way: nothing of it is held any longer. */
internal fun ExpeditionRun.endFight() {
    fight = null
    fightAgent = null
    members = emptyList()
}

/** «Освободить»: the crystal's guardian stands up — the zone's monster, rare, with the lines of the essences it guards — and the fight begins. */
internal fun ExpeditionRun.release() {
    val spot = crystal ?: return
    // Строки карты без сделок алтаря (3.90.0): сделки ложатся в самом бою ([pactFoe])
    val extra = MapEffects.guardianBuffs(spot.crystal.stronger, index.essences.crystals.stronger, baseEffects)
    val guardian = spawns.crystalGuardian(zone, spot.crystal, spot.id, MapEffects.buffs(baseEffects), extra) ?: return
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
    when (event) {
        is RunEvent.Boss -> killed(event.n, FightKey.BOSS, 0)
        is RunEvent.Kill -> killed(event.n, FightKey(event.i, event.vaal, boss = false), event.m)
        else -> Unit
    }
}

internal fun ExpeditionRun.play(dt: Double) {
    val battle = fight ?: return
    val agent = fightAgent ?: return
    if (!started || paused || waitingLink) return
    battle.advance(dt * speed)
    // Every foe is a kill of its own, recorded the moment it falls, the fight still going.
    while (reported < battle.fallen.size) {
        val member = members[battle.fallen[reported++]]
        member.agent.fallen += member.index
        fell(member.agent, member.index)
    }
    val outcome = battle.outcome ?: return
    if (battle.time < battle.duration + pace.aftermath) return
    val out = battle.pools()
    life = out.life
    shield = hero.maxShield
    mana = out.mana
    charges = out.charges
    flaskLeft = out.flaskLeft
    rates = out.rates
    rebody()
    // Свита фаз (3.92.0) - в журнале боя вслед за стаей
    val retinue = battle.foes.withIndex().drop(members.size).mapNotNull { (index, foe) -> foe.origin?.let { PackHit(it, battle.events.filter { e -> e.foe == index }, battle.duration) } }
    val pack = members.mapIndexed { index, member -> PackHit(member.monster, battle.events.filter { it.foe == index }, battle.duration) } + retinue
    val duration = battle.duration
    stats.add(pack, duration)
    // The fight's figures (3.51.0): only the hero's statistics, before a fall closes the journal.
    record { n -> RunEvent.Fight(n, FightFigures.of(pack, duration, boss = agent === world.boss, won = outcome == Outcome.WIN)) }
    val leader = fightLeader()
    val down = descent?.takeIf { abyssFight }
    when (outcome) {
        Outcome.WIN -> {
            agent.alive = false
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
    }
    endFight()
    abyssFight = false
    // The Abyss chains its fights (3.32.0): the next fight of the wave, or the next depth once the player descends.
    if (outcome == Outcome.WIN && down != null) {
        if (down.fights.isNotEmpty()) nextFight(down) else down.cleared++
    }
}

/** The hero's level on the experience the answers granted so far (3.81.0); never below the level they set out with. */
internal fun ExpeditionRun.levelNow(): Int = index.classes.levelOf(heroExperience + granted.experience).coerceAtLeast(heroLevel)

/** What the death costs by the rules: a share of the level's experience, never the level — on what the answers granted so far. */
internal fun ExpeditionRun.deathLoss(): Double {
    val classes = index.classes
    val gained = heroExperience + granted.experience
    val level = classes.levelOf(gained).coerceAtLeast(heroLevel)
    return LootRoller(index).deathLoss(rules.death, zone.level, gained, classes.threshold(level) ?: 0.0, classes.nextThreshold(level))
}
