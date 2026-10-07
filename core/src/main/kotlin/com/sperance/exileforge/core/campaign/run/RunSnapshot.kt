package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.ZoneShare
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
import com.sperance.exileforge.core.campaign.hud
import com.sperance.exileforge.core.campaign.shownLife
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

// ==================== What the screen reads ====================
internal fun ExpeditionRun.snapshot(): RunHud {
    val battle = fight
    val figures = stats.summary(kills)
    val awaiting = mine.count(::awaits)
    return RunHud(
        phase = phase, mapCode = zone.code,
        heroLife = shownLife(battle?.heroLife ?: life, (battle?.heroLife ?: life) > 0), heroMaxLife = hero.maxLife.roundToInt(),
        heroShield = (battle?.heroFighter?.shield ?: hero.maxShield).roundToInt(), heroMaxShield = hero.maxShield.roundToInt(),
        alive = world.alive, total = world.total, sealed = world.sealed,
        fight = battle?.takeIf { fightAgent != null }?.let(::fightHud),
        reward = reward, rank = rank, levelUp = levelNow().takeIf { report?.outcome == Outcome.WIN && it > levelShown }?.let { LevelUp(levelShown, it) },
        rewardAwaiting = fightEvents.count(::awaits), slain = slain, report = report,
        fall = fall,
        gold = granted.gold, experience = granted.experience, kills = kills, awaiting = awaiting,
        chestsLeft = world.chests.count { !it.opened },
        chest = chestEvent?.let { earned[it] ?: Reward.NONE }, chestAwaiting = chestEvent?.let(::awaits) == true,
        fountainsLeft = world.fountains.count { !it.used },
        fountain = fountain?.let { FountainView(it.id, it.heal) },
        gate = gate, vaal = vaal,
        heroMana = (battle?.heroMana ?: mana).roundToInt(), heroMaxMana = (battle?.manaCap() ?: manaCap()).roundToInt(),
        heroReserved = (battle?.manaReserved() ?: (hero.maxMana - manaCap())).roundToInt(),
        flasks = battle?.flaskViews() ?: mapFlasks(),
        crystal = crystal?.let { CrystalView(it.id, it.crystal.essences, it.crystal.guardian, it.crystal.stronger, it.crystal.vaal, crystalOutcome, vaaling(it)) },
        crystalsLeft = world.standingCrystals.size,
        abyss = abyssView(), cracksLeft = world.standingCracks.size,
        bossDown = bossDown,
        auto = autopilot?.let { AutoHud(it.wave, it.waves, it.chests) }, autoReward = autoReward, autoAwaiting = autoEvents.count(::awaits), questTally = questTally.toMap(),
        pending = journal.pending.size, applied = journal.applied, rejected = journal.rejected.size,
        summary = figures, recap = recap,
        tally = MapTally(end, seconds, kills, bosses, deaths, granted, figures, awaiting),
        feature = offer?.let { spot -> FeatureView(spot.feature, spot.taken.toList(), channel?.let { (it.elapsed / it.spot.node.seconds).toFloat().coerceIn(0f, 1f) }) },
        hazard = hazard,
        opening = world.features.maxOfOrNull { it.progress }?.takeIf { it > 0 }?.toFloat(),
        challenge = challengeView(),
    )
}

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
internal fun ExpeditionRun.awaits(n: Int): Boolean = n >= answered && n !in previewed

internal fun ExpeditionRun.mapFlasks(): List<FlaskView?> = kit.flasks.mapIndexed { i, flask ->
    flask?.let {
        val left = flaskLeft.getOrNull(i) ?: 0.0
        FlaskView(
            i,
            it.code,
            it.kind,
            (charges.getOrNull(i) ?: 0.0).toInt(),
            it.maxCharges.toInt(),
            kotlin.math.ceil(it.perUse(hero) - 1e-9).toInt(),
            if (left > 0) (left / it.duration(hero)).toFloat().coerceIn(0f, 1f) else 0f,
            it.condition,
        )
    }
}

/** The strongest of the stage: its portrait and its name head the pack fought now. */
internal fun ExpeditionRun.fightLeader(): RolledMonster = members.maxBy { it.monster.rarity.ordinal }.monster

internal fun ExpeditionRun.fightHud(battle: Battle): FightHud = battle.hud(
    members.map { it.monster }, fightLeader(), speed, started, paused, hero.taunt,
    level = fightLevel, stage = stage, stages = fightStages.size, interlude = interlude,
)
