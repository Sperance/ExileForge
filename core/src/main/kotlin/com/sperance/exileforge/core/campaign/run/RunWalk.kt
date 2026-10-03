package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.MapEnd
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
import com.sperance.exileforge.rules.content.LoneWolfRule
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
import com.sperance.exileforge.rules.run.RunEventKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ==================== The map between fights ====================
internal fun ExpeditionRun.walk(dt: Double) {
    recover(dt)
    wound(dt)
    val (x, y) = ExpeditionWorld.screenToWorld(stickX, stickY)
    when (val event = world.step(dt, x, y)) {
        is WorldEvent.Encounter -> engage(event.agent)

        WorldEvent.Exit -> exit()

        is WorldEvent.Opened -> chestEvent = rewarding(record(RunEventKind.CHEST, index = event.chest.id))?.n

        is WorldEvent.AtFountain -> fountain = event.fountain

        WorldEvent.Portal -> openGate()

        is WorldEvent.Crystal -> {
            phase = RunPhase.CRYSTAL
            crystal = event.spot
            crystalOutcome = null
        }

        is WorldEvent.Abyss -> {
            phase = RunPhase.ABYSS
            rift = event.spot
            descent = null
        }

        null -> Unit
    }
}

/** A fountain drunk dry: its share of life and mana back, and every flask full. */
internal fun ExpeditionRun.drink(spring: Fountain) {
    spring.used = true
    fountain = null
    life = (life + hero.maxLife * spring.heal / 100).coerceAtMost(hero.maxLife)
    mana = (mana + manaCap() * spring.heal / 100).coerceAtMost(manaCap())
    charges = kit.flasks.map { it?.maxCharges ?: 0.0 }
}

/** The way out: the Vaal zone's exit leads back to the map; the zone's own records the leaving, the boss passed. */
internal fun ExpeditionRun.exit() {
    if (!vaal) record(RunEventKind.LEAVE)
    end = MapEnd.CLEARED
    phase = RunPhase.CLEARED
    onCleared()
}

/**
 * The autorun's beat (3.2.0): mana and draughts run as on the road, and after a short rest the next step
 * is taken — a fight begins at once, a chest opens, a guardian stands up; a crack or the portal stops the
 * run for the player's word, and the way out ends it.
 */
internal fun ExpeditionRun.drive(pilot: AutoPilot, dt: Double) {
    recover(dt)
    pilot.rest -= dt * speed
    if (pilot.rest > 0) return
    pilot.rest = pace.autoBeat
    while (phase == RunPhase.MAP) {
        when (val step = pilot.next()) {
            null -> {
                autopilot = null
                return
            }

            is AutoStep.Wave -> Unit

            is AutoStep.Fight -> if (step.agent.alive && step.agent.standing.isNotEmpty()) {
                engage(step.agent)
                started = true
                return
            }

            is AutoStep.OpenChest -> if (!step.chest.opened) {
                step.chest.opened = true
                rewarding(record(RunEventKind.CHEST, index = step.chest.id))
                return
            }

            is AutoStep.Guardian -> if (!step.spot.freed) {
                crystal = step.spot
                release()
                started = true
                return
            }

            is AutoStep.Rift -> if (!step.spot.opened) {
                phase = RunPhase.ABYSS
                rift = step.spot
                descent = null
            }

            AutoStep.Portal -> if (world.portal != null) openGate()

            AutoStep.Boss -> world.boss?.takeIf { it.alive }?.let {
                engage(it)
                started = true
                return
            }

            AutoStep.Exit -> exit()
        }
    }
}

/** Mana back on the road and the draughts still running, over [dt] seconds off the fight. */
internal fun ExpeditionRun.recover(dt: Double) {
    mana = (mana + hero.manaRegen(rules.mana) * dt).coerceAtMost(manaCap())
    if (flaskLeft.any { it > 0 }) {
        val before = flaskLeft.map { it > 0 }
        flaskLeft.forEachIndexed { i, left ->
            val rate = rates.getOrNull(i) ?: return@forEachIndexed
            val slice = min(dt, left)
            if (slice <= 0 || !rate.flows) return@forEachIndexed
            life = (life + rate.life * slice).coerceAtMost(hero.maxLife)
            mana = (mana + rate.mana * slice).coerceAtMost(manaCap())
        }
        flaskLeft = flaskLeft.map { (it - dt).coerceAtLeast(0.0) }
        if (flaskLeft.map { it > 0 } != before) rebody()
    }
}

/**
 * The portal opens its gate: the first time, the opening is recorded and the server rolls the Vaal zone behind
 * it (1.30.0) — the gate waits for it, and the run takes its context when the answer brings it.
 */
internal fun ExpeditionRun.openGate() {
    if (vaalZone == null) {
        if (corruptionOpened) {
            world.closePortal()
            return
        }
        if (gateEvent == null) gateEvent = record(RunEventKind.VAAL_OPEN)?.n
        if (gateEvent == null) {
            world.closePortal()
            return
        }
    }
    gate = vaalZone
    phase = RunPhase.GATE
}
