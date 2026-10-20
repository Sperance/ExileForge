package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.campaign.FightFigures
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.PhaseFoes
import com.sperance.exileforge.core.campaign.RunStats
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
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
import com.sperance.exileforge.core.campaign.combat.carry
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunEventKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ==================== Fights ====================
/**
 * A fight with [agent]'s pack still standing, at [level] — the zone's, or a depth's of the Abyss. На карте
 * бой собирает обычные стаи вокруг ([gathered]) и идёт раундами ([FightRounds]) не больше `fight.maxFoes` врагов:
 * герой переходит из раунда в раунд как есть, гибель в любом - гибель в бою. Каждой стае - своё ENGAGE.
 *
 * «В бой» игрока начинает бой.
 */
internal fun ExpeditionRun.engage(agent: MonsterAgent, level: Int = run.levelOf(zone), abyssal: Boolean = false) {
    fightAgent = agent
    fightAgents = if (abyssal) listOf(agent) else world.gathered(agent)
    abyssFight = abyssal
    fightLevel = level
    if (!abyssal) fightAgents.forEach(::announce)
    members = FightRounds.of(fightAgents, index.rules.fight.maxFoes) { it.standing.size }.flatMapIndexed { round, packs ->
        packs.flatMap { pack -> pack.standing.map { FightMember(pack, it, round) } }
    }
    reported = 0
    fightStream = (fights++).toLong()
    fight = battle()
    started = false
    paused = false
    phase = RunPhase.FIGHT
}

/**
 * Бой начат: каждому паку - событие ENGAGE. Сервер добычи по нему не катит и не показывает - она
 * приходит ответом на убийство; ENGAGE лишь отмечает бой начатым, и выход посреди него - поражение. Страж кристалла и Бездна -
 * не паки жетонов, босс Ваал-зоны - порча, а не убийство босса: им ENGAGE нет.
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

/**
 * The fight's battle on the hero's pools now, at the fight's level, on its own dice. Предначертание - замороженное захода
 * (сила, судьбоносные строки и счёт Разгона на входе, как их видит сервер).
 *
 * Сделки алтаря ложатся на каждый бой (строки карты, сила босса, лишние строки монстров); уровень боя - свой уровень на карте. Свита фаз
 * босса - в конце стаи, на своём потоке.
 */
internal fun ExpeditionRun.battle(): Battle {
    val phases = PhaseFoes(this.index, rules)
    val foes = members.map { member ->
        val monster = pactFoe(member)
        phases.foe(monster, monster.level.takeIf { it > 0 } ?: fightLevel).copy(round = member.round)
    }
    return Battle(
        hero,
        phases.withRetinue(foes, Dice(Streams.mix(seed, ExpeditionRun.RETINUE_STREAM, fightStream)), run::bound),
        rules, index.rules.fight, life, Random(Streams.mix(seed, ExpeditionRun.FIGHT_STREAM, fightStream)), stance, kit = kit.copy(fate = FateKit.of(run.fate)), model = build, pools = pools,
        percent = build.gear.percent, cleared = wonLast, fateRun = fateRun,
    )
}

/** The fight is over, whichever way: nothing of it is held any longer. */
internal fun ExpeditionRun.endFight() {
    fight = null
    fightAgent = null
    fightAgents = emptyList()
    members = emptyList()
}

/**
 * «Освободить»: the crystal's guardian stands up — the zone's monster, rare, with the lines of the essences it guards — and the fight begins.
 *
 * Строки карты - без сделок алтаря: сделки ложатся в самом бою ([pactFoe]).
 */
internal fun ExpeditionRun.release() {
    val spot = crystal ?: return
    val extra = MapEffects.guardianBuffs(spot.crystal.stronger, index.essences.crystals.stronger, baseEffects)
    val guardian = spawns.crystalGuardian(spot.crystal, spot.id, MapEffects.buffs(baseEffects), extra) ?: return
    crystal = null
    crystalOutcome = null
    engage(MonsterAgent(-1 - spot.id, listOf(guardian), spot.cell.x + 0.5, spot.cell.y + 0.5, crystal = spot.id, joinsBrawl = false))
}

/** Виды событий, что пишет падение врага в бою ([fell]): их награда и очки Атласа - в отчёте боя. */
val FOUGHT_EVENTS: Set<RunEventKind> = setOf(RunEventKind.KILL, RunEventKind.BOSS, RunEventKind.CORRUPT, RunEventKind.CRYSTAL)

/** A foe of the fight fell: the event by what it was; its reward comes with the server's answer. */
internal fun ExpeditionRun.fell(agent: MonsterAgent, member: Int) {
    kills++
    if (agent === world.boss) {
        bosses++
        guardianSlain = true
    }
    if (abyssFight) return
    val spot = agent.crystal?.let { id -> world.crystals.firstOrNull { it.id == id } }
    val event = when {
        spot != null -> record { RunEvent.Crystal(it, world.standingCrystals.indexOf(spot)) }.also { spot.freed = true }
        agent === world.boss -> (if (vaal) record(RunEvent::Corrupt) else record(RunEvent::Boss))?.also { bossDown = true }
        else -> record { RunEvent.Kill(it, agent.id, member, vaal) }
    }
    rewarding(event, fought = true)
    when (event) {
        is RunEvent.Boss -> {
            fateRun.bossSlain()
            bossSlain()
        }

        is RunEvent.Kill -> fateRun.slain(event.i * Run.PACK_SLOTS + event.m, event.vaal)

        else -> return
    }
    tallyWins()
}

/**
 * Выигранные бои захода (Разгон [FateLever.WIN_STREAK]) - тем же правилом, что считает сервер ([Run.fightsWon]); без
 * рычага паки не перебираются.
 */
internal fun ExpeditionRun.tallyWins() {
    if (run.fate.has(FateLever.WIN_STREAK)) fateRun.recount(run)
}

/**
 * Каждый враг - своё убийство, записанное, когда он пал, пока бой идёт; стая раунда, павшая целиком, уходит с карты сразу. Щит, недуги,
 * баффы, проклятия и заряды уходят на карту остатком ([HeroCarry]), перезарядка щита - от последнего удара боя. Свита фаз - в журнале вслед
 * за стаей. Цифры боя идут в статистику героя до того, как гибель закроет журнал. Гибель в Бездне
 * сжигает копилку (кроме доли атласа), в Ваал-зоне - обычная смерть, весь заход кончен. Бездна сцепляет бои: следующий бой волны или
 * следующая глубина, когда игрок спустится.
 */
internal fun ExpeditionRun.play(dt: Double) {
    val battle = fight ?: return
    val agent = fightAgent ?: return
    if (!started || paused) return
    battle.advance(dt * speed)
    while (reported < battle.fallen.size) {
        val member = members[battle.fallen[reported++]]
        member.agent.fallen += member.index
        if (member.agent.standing.isEmpty()) member.agent.alive = false
        fell(member.agent, member.index)
    }
    val outcome = battle.outcome ?: return
    if (battle.time < battle.duration + pace.aftermath) return
    val out = battle.pools()
    life = out.life
    val carried = battle.carry()
    shield = carried.shield ?: hero.maxShield
    carry = carried.copy(shield = null)
    hitAt = seconds - (battle.time - battle.heroFighter.lastHit).coerceAtLeast(0.0)
    mana = out.mana
    charges = out.charges
    flaskLeft = out.flaskLeft
    rates = out.rates
    rebody()
    val retinue = battle.foes.withIndex().drop(members.size).mapNotNull { (index, foe) -> foe.origin?.let { PackHit(it, battle.events.filter { e -> e.foe == index }, battle.duration) } }
    val pack = members.mapIndexed { index, member -> PackHit(member.monster, battle.events.filter { it.foe == index }, battle.duration) } + retinue
    val duration = battle.duration
    stats.add(pack, duration)
    record { n -> RunEvent.Fight(n, FightFigures.of(pack, duration, boss = agent === world.boss, won = outcome == Outcome.WIN)) }
    val leader = fightLeader()
    lastFight = FightReport(leader, outcome, pack, duration)
    wonLast = outcome == Outcome.WIN
    val down = descent?.takeIf { abyssFight }
    when (outcome) {
        Outcome.WIN -> {
            fightAgents.forEach { it.alive = false }
            if (down != null) {
                report = null
                phase = RunPhase.ABYSS
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
            phase = RunPhase.DEAD
            deaths++
            end = MapEnd.FELL
            down?.let { take(it, fallen = true) }
            record { RunEvent.Fall(it, vaal) }?.let {
                fallEvent = it.n
                fall = deathLoss()
            }
            onFallen()
        }
    }
    endFight()
    abyssFight = false
    if (phase != RunPhase.DEAD) pendingGear?.let { wear(it) }
    pendingGear = null
    if (outcome == Outcome.WIN && down != null) {
        if (down.fights.isNotEmpty()) nextFight(down) else down.cleared++
    }
}

/** The hero's level on the experience the answers granted so far; never below the level they set out with. */
internal fun ExpeditionRun.levelNow(): Int = index.classes.levelOf(heroExperience + granted.experience).coerceAtLeast(heroLevel)

/**
 * What the death costs by the rules: a share of the level's experience, never the level — on what the answers granted so far.
 * С уровня героя `combat.death.fromLevel` (прежде - уровня зоны), ниже - даром.
 */
internal fun ExpeditionRun.deathLoss(): Double = LootRoller(index).deathLoss(levelNow(), heroExperience + granted.experience)
