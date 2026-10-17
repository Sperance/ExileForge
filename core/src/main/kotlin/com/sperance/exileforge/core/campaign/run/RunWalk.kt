package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.EnergyShield
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
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.RecoveryPool
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
import com.sperance.exileforge.rules.sheet.Recovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ==================== The map between fights ====================
/**
 * Недуги из боя и ловушек тикают на дороге и убивают. Страж вручную - сперва экран-вызов; запечатанный босс не встаёт. Крышка сундука
 * открывается сразу, герой идёт дальше: добыча вылетит с ответом сервера.
 */
internal fun ExpeditionRun.walk(dt: Double) {
    recover(dt)
    ailing(dt)
    if (phase != RunPhase.MAP) return
    openArena()
    val (x, y) = IsoGrid.pace(stickX, stickY)
    when (val event = world.step(dt, x, y)) {
        is WorldEvent.Encounter -> if (event.agent !== world.boss) {
            engage(event.agent)
        } else if (!sealed) {
            challenge(event.agent)
        }

        WorldEvent.Exit -> exit()

        is WorldEvent.Opened -> rewarding(record { RunEvent.Chest(it, event.chest.id) })?.let { opened ->
            event.chest.event = opened.n
            dropped(opened.n)
        }

        is WorldEvent.AtFountain -> fountain = event.fountain

        WorldEvent.Portal -> openGate()

        is WorldEvent.Feature -> feature(event.action)

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
    charges = kit.flasks.map { it?.sheet?.maxCharges ?: 0.0 }
}

/** The way out: the Vaal zone's exit leads back to the map; the zone's own records the leaving, the boss passed. */
internal fun ExpeditionRun.exit() {
    if (!vaal) record(RunEvent::Leave)
    end = MapEnd.CLEARED
    phase = RunPhase.CLEARED
    onCleared()
}

/**
 * Шаг автопрохода: мана и флаконы идут как в пути, и после короткой паузы берётся следующий шаг - бой начинается сразу,
 * сундук открывается, выход заканчивает заход. Перед волной и стражем - отдых (В-15): на скорости автопрохода, пока
 * здоровья меньше `autoRest.life`% и не дольше `autoRest.seconds`.
 *
 * Нет связи - шаг не берётся, пока сервер не ответит; недуги тикают и в автопробеге.
 */
internal fun ExpeditionRun.drive(pilot: AutoPilot, dt: Double) {
    if (waitingLink) return
    val clock = if (pilot.resting != null) dt * speed else dt
    recover(clock)
    ailing(clock)
    if (phase != RunPhase.MAP) return
    pilot.resting?.let { rested ->
        val rule = pace.autoRest
        if (rested + clock < rule.seconds && life < hero.maxLife * rule.life / 100) {
            pilot.resting = rested + clock
            return
        }
        pilot.resting = null
    }
    pilot.rest -= dt * speed
    if (pilot.rest > 0) return
    pilot.rest = pace.autoBeat
    while (phase == RunPhase.MAP) {
        if (pilot.restBefore(guarded = world.boss?.let { it.alive && !sealed } == true)) {
            rest(pilot)
            return
        }
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
                pilot.chests++
                rewarding(record { RunEvent.Chest(it, step.chest.id) })
                return
            }

            AutoStep.Boss -> world.boss?.takeIf { it.alive && !sealed }?.let {
                engage(it)
                started = true
                return
            }

            AutoStep.Exit -> exit()

            AutoStep.Leave -> {
                autopilot = null
                leave()
                return
            }
        }
    }
}

/**
 * Отдых автопрохода (В-15): фляги здоровья, чьё лечение целиком ляжет в недостающее здоровье, - сразу, как выпил бы
 * игрок; затем герой стоит, пока [drive] не решит, что хватит.
 */
private fun ExpeditionRun.rest(pilot: AutoPilot) {
    kit.flasks.forEachIndexed { slot, flask ->
        val draught = flask?.sheet?.draught(hero, life, manaCap()) ?: return@forEachIndexed
        val heal = draught.life + draught.lifeRate * draught.duration
        if (heal > 0 && heal <= hero.maxLife - life) drinkOnMap(slot)
    }
    pilot.resting = 0.0
}

/**
 * Вне боя за [dt] секунд: здоровье по чистой скорости карты ([RoadLife], регенерация и основа против дегена; под ранящим
 * недугом - ни того, ни другого), мана (поток маны [Recovery.manaFlow] - под долей восстановления листа; недуги на
 * герое режут и её), энергощит
 * и идущие флаконы.
 */
internal fun ExpeditionRun.recover(dt: Double) {
    life = RoadLife.of(hero, pace, curb(RecoveryPool.LIFE), curb(RecoveryPool.MANA), stalling()).after(life, hero.maxLife, dt)
    mana = (mana + Recovery.manaFlow(hero, rules.mana, curb(RecoveryPool.MANA)).total * dt).coerceAtMost(manaCap())
    shield = EnergyShield.recovered(hero, rules, shield, seconds - hitAt, dt)
    if (flaskLeft.any { it > 0 }) {
        val before = flaskLeft.map { it > 0 }
        flaskLeft.forEachIndexed { i, left ->
            val rate = rates.getOrNull(i) ?: return@forEachIndexed
            val slice = min(dt, left)
            if (slice <= 0 || !rate.flows) return@forEachIndexed
            val before = life
            life = (life + rate.life * slice).coerceAtMost(hero.maxLife)
            mana = (mana + rate.mana * slice).coerceAtMost(manaCap())
            healedInto(life - before, rate.toMana, rate.toShield)
        }
        flaskLeft = flaskLeft.map { (it - dt).coerceAtLeast(0.0) }
        if (flaskLeft.map { it > 0 } != before) rebody()
    }
}

/**
 * The portal opens its gate: the first time, the opening is recorded and the server rolls the Vaal zone behind
 * it — the gate waits for it, and the run takes its context when the answer brings it.
 */
internal fun ExpeditionRun.openGate() {
    if (vaalZone == null) {
        if (corruptionOpened) {
            world.closePortal()
            return
        }
        if (gateEvent == null) gateEvent = record(RunEvent::VaalOpen)?.n
        if (gateEvent == null) {
            world.closePortal()
            return
        }
    }
    gate = vaalZone
    phase = RunPhase.GATE
}

/** Доли вылеченного глотком здоровья [healed] в ману и энергощит вне боя - как в бою, от вылеченного. */
internal fun ExpeditionRun.healedInto(healed: Double, toMana: Double, toShield: Double) {
    if (healed <= 0) return
    mana = (mana + healed * toMana).coerceAtMost(manaCap())
    shield = (shield + healed * toShield).coerceAtMost(hero.maxShield)
}
