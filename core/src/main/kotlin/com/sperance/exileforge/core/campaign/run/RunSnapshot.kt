package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.ZoneShare
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
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.campaign.hud
import com.sperance.exileforge.core.campaign.shownLife
import com.sperance.exileforge.core.campaign.shownMax
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.ReservePool
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

// ==================== What the screen reads ====================
/** Здоровье, что тает на карте (недуги, деген), округляется вниз: число не обещает того, чего уже нет. */
internal fun ExpeditionRun.snapshot(): RunHud {
    val battle = fight
    val flow = if (battle == null && phase == RunPhase.MAP) roadFlow() else null
    val figures = stats.summary(kills)
    val awaiting = mine.count(::awaits)
    return RunHud(
        phase = phase, mapCode = zone.code,
        heroLife = shownLife(battle?.heroLife ?: life, (battle?.heroLife ?: life) > 0, hero.maxLife, falling = battle == null && flow?.let { it.net < 0 } == true),
        heroMaxLife = shownMax(hero.maxLife),
        heroShield = (battle?.heroFighter?.shield ?: shield).roundToInt(), heroMaxShield = (battle?.heroFighter?.body?.maxShield ?: hero.maxShield).roundToInt(),
        alive = world.alive, total = world.total, sealed = world.sealed,
        fight = battle?.takeIf { fightAgent != null }?.let(::fightHud),
        reward = reward, rank = rank, levelUp = levelNow().takeIf { report?.outcome == Outcome.WIN && it > levelShown }?.let { LevelUp(levelShown, it) },
        rewardAwaiting = fightEvents.count(::awaits), slain = slain, report = report, lastFight = lastFight,
        fall = fall, fallFrom = rules.death.fromLevel.takeIf { levelNow() < it },
        gold = granted.gold, experience = granted.experience, kills = kills, awaiting = awaiting,
        chestsLeft = world.chests.count { !it.opened },
        drops = dropsShown,
        bag = bagShown,
        fountainsLeft = world.fountains.count { !it.used },
        fountain = fountain?.let { FountainView(it.id, it.heal) },
        gate = gate, vaal = vaal,
        heroMana = (battle?.heroMana ?: mana).roundToInt(), heroMaxMana = (battle?.manaCap() ?: manaCap()).roundToInt(),
        heroReserved = (battle?.manaReserved() ?: kit.reserve(ReservePool.MANA, hero).held).roundToInt(),
        heroLifeReserved = (battle?.lifeReserved() ?: kit.reserve(ReservePool.LIFE, hero).held).roundToInt(),
        flasks = battle?.flaskViews() ?: mapFlasks(),
        crystal = crystal?.let { CrystalView(it.id, it.crystal.essences, it.crystal.guardian, it.crystal.stronger, it.crystal.vaal, crystalOutcome, vaaling(it)) },
        crystalsLeft = world.standingCrystals.size,
        abyss = abyssView(), cracksLeft = world.standingCracks.size,
        bossDown = bossDown, swift = swift, guardianRest = guardianBack?.let { ((it + ExpeditionRun.GUARDIAN_GRACE_MS - clock()) / 1000).coerceAtLeast(0L).toInt() },
        summon = summonView(),
        questTally = questTallyShown,
        pending = journal.pending.size, applied = journal.applied, rejected = journal.rejected.size,
        summary = figures, recap = recap,
        tally = MapTally(end, shownSeconds(), kills, bosses, deaths, granted, figures, awaiting),
        feature = offer?.let { spot -> FeatureView(spot.feature, spot.taken.toList(), world.guarded(spot.id)) },
        hazards = hazards.map { it.view },
        heroAilments = if (battle == null) carriedAilments() else emptyList(),
        heroEffects = if (battle == null) carriedEffects() else emptyList(),
        heroCharges = if (battle == null) carriedCharges() else emptyList(),
        lifeFlow = flow,
        action = objectAction(),
        challenge = challengeView(),
        blight = world.features.firstNotNullOfOrNull { it as? BlightSpot }?.view(world),
    )
}

/**
 * Секунды захода для снимка: пока заход идёт - целыми, иначе снимок менялся бы каждый кадр и экран похода
 * перекомпоновывался бы без нужды; их показывают только итоги, а там часы уже стоят и секунды точные.
 */
private fun ExpeditionRun.shownSeconds(): Double = if (phase == RunPhase.DEAD || phase == RunPhase.CLEARED || phase == RunPhase.LEFT) seconds else kotlin.math.floor(seconds)

internal fun ExpeditionRun.abyssView(): AbyssView? {
    val spot = rift ?: return null
    val rule = abyssRule ?: return null
    val down = descent
    val claim = down?.claim
    return AbyssView(
        down?.depth ?: spot.depth,
        down?.cleared ?: 0,
        down != null,
        waves.depths(rule, zone),
        claim?.let { earned[it] ?: Reward.NONE },
        down?.fallen == true,
        claim?.let(::awaits) == true,
    )
}

/**
 * A Vaal zone's share taken into the map's: what the server granted for its events is the map's loot now, and
 * the answers still to come for the rest land here, as this run's own do.
 */
internal fun ExpeditionRun.adopt(zone: ZoneShare) {
    zone.events.forEach { (n, gained) ->
        if (!mine.add(n) || n in earned) return@forEach
        bag[n] = LootFrom(LootKind.VAAL) to clock()
        gained?.let {
            earned[n] = it
            granted += it
        }
    }
    seconds += zone.seconds
    kills += zone.kills
    bosses += zone.bosses
    deaths += zone.deaths
    stats.add(zone.figures)
}

/** Event [n] is not answered yet. */
internal fun ExpeditionRun.awaits(n: Int): Boolean = n >= answered

/** Сундук [chest] открыт в этом заходе, а ответа на него ещё нет: он приоткрыт и ждёт. */
fun ExpeditionRun.waiting(chest: Chest): Boolean = chest.event?.let(::awaits) == true

internal fun ExpeditionRun.mapFlasks(): List<FlaskView?> = kit.flasks.mapIndexed { i, flask ->
    flask?.let {
        val left = flaskLeft.getOrNull(i) ?: 0.0
        FlaskView(
            i,
            it.sheet.code,
            it.sheet.kind,
            (charges.getOrNull(i) ?: 0.0).toInt(),
            it.sheet.maxCharges.toInt(),
            kotlin.math.ceil(it.sheet.perUse(hero::get) - 1e-9).toInt(),
            if (left > 0) (left / it.sheet.duration(hero::get)).toFloat().coerceIn(0f, 1f) else 0f,
            it.condition,
        )
    }
}

/** The strongest of the fight: its portrait and its name head the pack fought now. */
internal fun ExpeditionRun.fightLeader(): RolledMonster = members.maxBy { it.monster.rarity.ordinal }.monster

internal fun ExpeditionRun.fightHud(battle: Battle): FightHud = battle.hud(members.map { it.monster }, fightLeader(), speed, started, paused, level = fightLevel, round = battle.round + 1, rounds = battle.rounds)

/**
 * Забег для отчёта об ошибке: ключи контекста, что сервер выносит в публичный Issue - зона, её уровень, фаза и семя
 * карты (по нему карта строится заново).
 */
fun ExpeditionRun.reportContext(): Map<String, String> = mapOf(
    "run.zone" to zone.code.value + if (vaal) " (Vaal)" else "",
    "run.level" to zone.level.toString(),
    "run.phase" to phase.name,
    "run.seed" to seed.toString(),
)

/** Призыв стража для полосы захода: только у поверженного стража карты, не в зоне Ваал; в финале региона - в заходе по карте ([Zone.summonable]). */
private fun ExpeditionRun.summonView(): SummonView? {
    if (vaal || !bossDown || !zone.summonable(run.context.active != null)) return null
    val perRun = index.campaign.services.summonsPerRun
    return SummonView(summonPrice(), summoned >= perRun, perRun)
}
