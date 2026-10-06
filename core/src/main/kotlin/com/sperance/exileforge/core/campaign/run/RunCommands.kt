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
import com.sperance.exileforge.core.campaign.draught
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

// ==================== Commands and the server's answers ====================
internal fun ExpeditionRun.settle(answer: RunCommand.Settled) {
    answered = maxOf(answered, answer.applied)
    refused += answer.rejected
    // Отклонённый выбор объекта карты (3.90.0) - объект снова прежний: золото не списано, сделки нет
    answer.rejected.forEach(::refuseFeature)
    // Добыча начатых боёв (3.88.0) - раньше наград: убийства, что уже случились, показывают её сразу.
    answer.pending.forEach { (n, loot) ->
        val key = engaged[n] ?: return@forEach
        pendingLoot[key] = loot
        killsOf.filterValues { it.first == key }.keys.sorted().forEach(::preview)
    }
    answer.rewards.forEach { (n, gained) ->
        if (n !in mine || n in earned) return@forEach
        earned[n] = gained
        granted += gained
        // Показанная по ENGAGE добыча - ровно эта (сервер выдаёт её без нового броска): в отчёт боя она не ложится дважды.
        if (n in previewed) return@forEach
        if (n in fightEvents) reward = (reward ?: Reward.NONE) + gained
        if (n in autoEvents) autoReward = (autoReward ?: Reward.NONE) + gained
    }
    if (fallEvent != null) answer.lost?.let { fall = it }
    answer.crystals.forEach { (n, changed) -> if (n in vaalings) vaaled[n] = changed }
    answer.ranks.forEach { (n, place) -> if (n in fightEvents) rank = place }
    resolve()
}

/**
 * What only the server's answer tells: the Vaal zone behind the opened portal, and what a Vaal orb did to a crystal —
 * told by the event's answer, or, from an older server, matched on the campaign it brought.
 */
internal fun ExpeditionRun.resolve() {
    val state = campaign
    gateEvent?.takeIf { vaalZone == null }?.let { n ->
        val rolled = state?.vaalZone?.takeIf { it.mapCode == zone.code }
        if (rolled != null) {
            vaalZone = rolled
            run = Run(index, run.zone, run.seed, run.context.copy(vaal = rolled))
            if (phase == RunPhase.GATE) gate = rolled
        } else if (n in refused) {
            gateEvent = null
            corruptionOpened = true
            world.closePortal()
            closeGate()
        }
    }
    val standing = state?.crystals?.get(zone.code.value)?.crystals.orEmpty()
    val waiting = vaalings.entries.iterator()
    while (waiting.hasNext()) {
        val (n, orb) = waiting.next()
        if (n >= answered) continue
        val before = orb.spot.crystal
        fun changed(c: Crystal) = c.vaal && c.guardian == before.guardian && c.essences.size == before.essences.size
        val after = if (n in refused) {
            before
        } else {
            vaaled[n] ?: standing.getOrNull(orb.place)?.takeIf(::changed) ?: standing.firstOrNull(::changed) ?: continue
        }
        waiting.remove()
        vaaled.remove(n)
        if (after === before) continue
        orb.spot.crystal = after
        if (crystal === orb.spot) crystalOutcome = vaalOutcome(before, after)
    }
}

/** What the orb did, read off the crystal before and after it. */
internal fun ExpeditionRun.vaalOutcome(before: Crystal, after: Crystal): String = when {
    after.essences.count { index.essence(it)?.special == true } > before.essences.count { index.essence(it)?.special == true } -> EssenceBook.VAAL_SPECIAL
    after.essences != before.essences -> EssenceBook.VAAL_UPGRADE
    else -> EssenceBook.VAAL_STRONGER
}

internal fun ExpeditionRun.handle(command: RunCommand) {
    when (command) {
        RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2

        RunCommand.StopAuto -> autopilot = null

        // Вне боя с карты можно уйти всегда (3.88.9): бой идёт до конца, а карта - нет.
        RunCommand.Leave -> if (phase == RunPhase.MAP || phase == RunPhase.DEAD || phase == RunPhase.CLEARED) {
            if (phase == RunPhase.MAP) end = MapEnd.LEFT
            phase = RunPhase.LEFT
        }

        RunCommand.Begin -> if (fight != null) {
            started = true
            paused = false
            interlude = null
        }

        RunCommand.Pause -> if (fight != null && started && fight?.outcome == null) paused = !paused

        is RunCommand.Focus -> fight?.focus(command.index)

        is RunCommand.Hold -> holds = (holds + if (command.on) 1 else -1).coerceAtLeast(0)

        RunCommand.Continue -> when (phase) {
            RunPhase.LOOT -> {
                phase = RunPhase.MAP
                clearSpoils()
                slain = null
                report = null
                // The Vaal zone closes with its guardian (server 1.76.0): its packs left standing are no longer the server's to pay.
                if (vaal && bossDown) exit()
            }

            RunPhase.DEAD, RunPhase.CLEARED -> phase = RunPhase.LEFT

            else -> Unit
        }

        RunCommand.DismissChest -> chestEvent = null

        RunCommand.LevelSeen -> levelShown = levelNow()

        RunCommand.StepBack -> if (phase == RunPhase.GATE) closeGate()

        is RunCommand.ShutGate -> {
            // Refused while its zone is still on the way, the portal is as good as opened: the server rolled the zone, and it is left.
            if (!command.entered && (vaalZone != null || gateEvent != null)) {
                record(RunEvent::VaalLeave)
                vaalZone = null
                gateEvent = null
                corruptionOpened = true
            }
            world.closePortal()
            closeGate()
        }

        is RunCommand.Returned -> {
            command.zone?.let(::adopt)
            life = command.life.coerceIn(0.0, hero.maxLife)
            command.pools?.let {
                mana = it.mana.coerceIn(0.0, manaCap())
                charges = it.charges.ifEmpty { charges }
                flaskLeft = it.flaskLeft.ifEmpty { flaskLeft }
                rates = it.rates.ifEmpty { rates }
                rebody()
            }
            // The zone is closed either way: its guardian fell, or the hero did.
            vaalZone = null
            gateEvent = null
            corruptionOpened = true
            phase = RunPhase.MAP
        }

        is RunCommand.Cast -> fight?.useSkill(command.slot)

        // In the pause between stages a draught is drunk as on the road, and the stage's battle takes the pools it leaves.
        is RunCommand.Drink -> if (phase == RunPhase.FIGHT && interlude != null && !started) {
            drinkOnMap(command.slot)
            fight = battle()
        } else if (phase == RunPhase.FIGHT) {
            fight?.useFlask(command.slot)
        } else if (phase == RunPhase.MAP || phase == RunPhase.CRYSTAL || phase == RunPhase.ABYSS) {
            drinkOnMap(command.slot)
        }

        // The guardian waits for the orb's outcome: it stands up as the crystal the server holds.
        RunCommand.Release -> if (phase == RunPhase.CRYSTAL && crystal?.let(::vaaling) != true) release()

        RunCommand.VaalCrystal -> crystal?.takeIf { phase == RunPhase.CRYSTAL && !it.crystal.vaal && !vaaling(it) && vaalOrbs() >= 1 }?.let { spot ->
            val place = world.standingCrystals.indexOf(spot)
            record { RunEvent.CrystalVaal(it, place) }?.let {
                vaalings[it.n] = Vaaling(spot, place)
                crystalOutcome = null
            }
        }

        is RunCommand.OfferFountain -> if (phase == RunPhase.MAP && autopilot == null) world.fountainInSight(command.id)?.let { fountain = it }

        RunCommand.TakeFountain -> fountain?.takeIf { phase == RunPhase.MAP && !it.used }?.let(::drink)

        RunCommand.StepOff -> when {
            fountain != null -> fountain = null

            // Лист объекта карты (3.90.0): алтарь без сделки не отпускает
            leaveOffer() -> Unit

            phase == RunPhase.CRYSTAL -> closeCrystal()

            // A crack is left unopened, or once its hoard is in — never mid-descent with the hoard at stake.
            phase == RunPhase.ABYSS && (descent == null || descent?.claim != null) -> closeRift()
        }

        is RunCommand.Choose -> choose(command.choice)

        RunCommand.Gather -> startGather()

        RunCommand.Descend -> if (phase == RunPhase.ABYSS) descend()

        RunCommand.TakeHoard -> descent?.takeIf { phase == RunPhase.ABYSS && it.cleared > 0 && it.claim == null }?.let { take(it, fallen = false) }

        is RunCommand.Settled -> settle(command)

        is RunCommand.Campaign -> {
            campaign = command.state
            resolve()
        }
    }
}

/** A Vaal orb on [spot] whose outcome the server has not told yet. */
internal fun ExpeditionRun.vaaling(spot: CrystalSpot): Boolean = vaalings.values.any { it.spot === spot }

internal fun ExpeditionRun.closeRift() {
    rift = null
    descent = null
    if (phase == RunPhase.ABYSS) phase = RunPhase.MAP
}

internal fun ExpeditionRun.closeGate() {
    gate = null
    if (phase == RunPhase.GATE) phase = RunPhase.MAP
}

internal fun ExpeditionRun.closeCrystal() {
    crystal = null
    crystalOutcome = null
    if (phase == RunPhase.CRYSTAL) phase = RunPhase.MAP
}

/** A draught on the map: what it gives back comes at once, and what it lays on runs as the hero walks. */
internal fun ExpeditionRun.drinkOnMap(slot: Int) {
    val flask = kit.flasks.getOrNull(slot) ?: return
    if ((flaskLeft.getOrNull(slot) ?: 0.0) > 0 || (charges.getOrNull(slot) ?: 0.0) + 1e-9 < flask.perUse(hero)) return
    val draught = flask.draught(hero, life, manaCap())
    charges = charges.toMutableList().also { it[slot] = if (flask.usesAll) 0.0 else (it[slot] - flask.perUse(hero)).coerceAtLeast(0.0) }
    flaskLeft = flaskLeft.toMutableList().also { it[slot] = draught.duration }
    rates = rates.toMutableList().also { it[slot] = DraughtRate(draught.lifeRate, draught.manaRate) }
    rebody()
    life = (life + draught.life).coerceAtMost(hero.maxLife)
    mana = (mana + draught.mana).coerceAtMost(manaCap())
}
