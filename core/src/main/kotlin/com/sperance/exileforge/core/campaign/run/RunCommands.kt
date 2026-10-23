package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.surrender
import com.sperance.exileforge.core.campaign.usesAll
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent

// ==================== Commands and the server's answers ====================
/**
 * Отклонённый выбор объекта карты возвращает объект как был: золото не списано, сделки нет. Добыча - только из ответа на само убийство:
 * выданные копии, уникалки уже с номером экземпляра.
 */
internal fun ExpeditionRun.settle(answer: RunCommand.Settled) {
    answered = maxOf(answered, answer.applied)
    refused += answer.rejected
    answer.rejected.forEach(::refuseFeature)
    answer.rewards.forEach { (n, gained) ->
        if (n !in mine || n in earned) return@forEach
        earned[n] = gained
        granted += gained
        if (n in fightEvents) reward = (reward ?: Reward.NONE) + gained
    }
    if (fallEvent != null) answer.lost?.let { fall = it }
    answer.crystals.forEach { (n, changed) -> if (n in vaalings) vaaled[n] = changed }
    answer.ranks.forEach { (n, place) -> if (n in fightEvents) rank = place }
    answer.atlas.forEach { (n, keys) -> if (n in fightEvents) fightAtlas = (fightAtlas + keys).distinct() }
    showDrops()
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

/**
 * Смена набора: решение игрока ([RunCommand.PutOn]) - только между боями ([ExpeditionRun.gearLock]) и поверх набора, что поход носит:
 * событие журнала [RunEvent.Regear] с местом, что выбрал клиент, и набор сразу ([ExpeditionRun.wear]); ответ - в [RunCommand.PutOn.done].
 * Без решения - набор, каким его держит сервер: посреди боя он ждёт конца боя.
 */
internal fun ExpeditionRun.changeGear(command: RunCommand.Regear) {
    val put = command.putOn
    when {
        put != null -> {
            val taken = gearLock == null && put.over == build.gear.worn && record { n -> RunEvent.Regear(n, put.item, put.slot) } != null
            if (taken) wear(command.gear)
            put.done(taken)
        }

        phase == RunPhase.FIGHT -> pendingGear = command.gear

        else -> wear(command.gear)
    }
}

/** What the orb did, read off the crystal before and after it. */
internal fun ExpeditionRun.vaalOutcome(before: Crystal, after: Crystal): String = when {
    after.essences.count { index.essence(it)?.special == true } > before.essences.count { index.essence(it)?.special == true } -> EssenceBook.VAAL_SPECIAL
    after.essences != before.essences -> EssenceBook.VAAL_UPGRADE
    else -> EssenceBook.VAAL_STRONGER
}

/**
 * Строка башни - команда арены испытания, у похода её нет. Тестировщик идёт к стражу тоже через экран-вызов, печать его не держит.
 * Ваал-зона закрывается со своим стражем (или с гибелью героя): её оставшиеся стаи сервер уже не оплачивает, несомое зоны уходит на
 * основную карту. Портал, отклонённый, пока зона в пути, всё равно открыт: сервер её бросил. Страж кристалла ждёт исхода сферы и встаёт тем
 * кристаллом, что держит сервер. Лист объекта закрывается без выбора - мимо алтаря можно пройти. Трещина остаётся неоткрытой или кончается
 * с копилкой, но не посреди спуска.
 */
internal fun ExpeditionRun.handle(command: RunCommand) {
    when (command) {
        RunCommand.Speed -> speed = index.rules.fight.next(speed)

        is RunCommand.SwiftAllowed -> if (swiftAllowed != command.on) {
            swiftAllowed = command.on
            if (swift) repace()
        }

        is RunCommand.PickLine -> Unit

        RunCommand.Leave -> leave()

        RunCommand.Begin -> if (fight != null) {
            started = true
            paused = false
        }

        RunCommand.Accept -> accept()

        RunCommand.ToBoss -> if (phase == RunPhase.MAP) world.boss?.takeIf { it.alive && it.standing.isNotEmpty() }?.let { challenge(it) }

        RunCommand.Pause -> if (fight != null && started && fight?.outcome == null) paused = !paused

        RunCommand.Surrender -> fight?.takeIf { it.outcome == null }?.let {
            it.surrender()
            paused = false
        }

        is RunCommand.Focus -> fight?.focus(command.index)

        is RunCommand.Hold -> holds = (holds + if (command.on) 1 else -1).coerceAtLeast(0)

        RunCommand.Continue -> when (phase) {
            RunPhase.LOOT -> {
                phase = RunPhase.MAP
                clearSpoils()
                slain = null
                report = null
            }

            RunPhase.DEAD, RunPhase.CLEARED -> {
                report = null
                phase = RunPhase.LEFT
            }

            RunPhase.ABYSS -> waveReport = null

            else -> Unit
        }

        RunCommand.LevelSeen -> levelShown = levelNow()

        RunCommand.StepBack -> if (phase == RunPhase.GATE) closeGate()

        is RunCommand.ShutGate -> {
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
                shield = it.carry.shield ?: shield
                carry = it.carry.copy(shield = null)
                ticks.clear()
                charges = it.charges.ifEmpty { charges }
                flaskLeft = it.flaskLeft.ifEmpty { flaskLeft }
                rates = it.rates.ifEmpty { rates }
                rebody()
            }
            vaalZone = null
            gateEvent = null
            corruptionOpened = true
            phase = RunPhase.MAP
        }

        is RunCommand.Regear -> changeGear(command)

        is RunCommand.Cast -> fight?.useSkill(command.slot)

        is RunCommand.Drink -> if (phase == RunPhase.FIGHT) {
            fight?.useFlask(command.slot)
        } else if (phase == RunPhase.MAP || phase == RunPhase.CRYSTAL || phase == RunPhase.ABYSS) {
            drinkOnMap(command.slot)
        }

        RunCommand.Release -> if (phase == RunPhase.CRYSTAL && crystal?.let(::vaaling) != true) release()

        RunCommand.VaalCrystal -> crystal?.takeIf { phase == RunPhase.CRYSTAL && !it.crystal.vaal && !vaaling(it) && vaalOrbs() >= 1 }?.let { spot ->
            val place = world.standingCrystals.indexOf(spot)
            record { RunEvent.CrystalVaal(it, place) }?.let {
                vaalings[it.n] = Vaaling(spot, place)
                crystalOutcome = null
            }
        }

        is RunCommand.OfferFountain -> if (phase == RunPhase.MAP) world.fountainInSight(command.id)?.let { fountain = it }

        RunCommand.TakeFountain -> fountain?.takeIf { phase == RunPhase.MAP && !it.used }?.let(::drink)

        RunCommand.TakeExit -> if (phase == RunPhase.MAP && world.atExit && !world.exitArmed) {
            exitOffered = false
            exit()
        }

        RunCommand.StepOff -> when {
            fountain != null -> fountain = null
            exitOffered -> exitOffered = false
            leaveOffer() -> Unit
            phase == RunPhase.CRYSTAL -> closeCrystal()
            phase == RunPhase.ABYSS && (descent == null || descent?.claim != null) -> closeRift()
        }

        is RunCommand.Choose -> choose(command.choice)

        is RunCommand.ChooseWorn -> chooseWorn(command.choice, command.wear)

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
    waveReport = null
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

/**
 * A draught on the map: what it gives back comes at once, and what it lays on runs as the hero walks.
 *
 * Глоток щита - и на карте, как в бою; доли лечения в ману и щит - от вылеченного. Глоток впустую ([com.sperance.exileforge.core.campaign.Draught.wasted]) не пьётся.
 */
internal fun ExpeditionRun.drinkOnMap(slot: Int) {
    val flask = kit.flasks.getOrNull(slot)?.sheet ?: return
    if ((flaskLeft.getOrNull(slot) ?: 0.0) > 0 || (charges.getOrNull(slot) ?: 0.0) + 1e-9 < flask.perUse(hero::get)) return
    val bars = bars()
    val draught = draughts.of(slot, flask, hero, bars.life, bars.maxMana)
    if (draught.wasted(bars)) return
    charges = charges.toMutableList().also { it[slot] = if (flask.usesAll) 0.0 else (it[slot] - flask.perUse(hero::get)).coerceAtLeast(0.0) }
    flaskLeft = flaskLeft.toMutableList().also { it[slot] = draught.duration }
    rates = rates.toMutableList().also { it[slot] = DraughtRate(draught.lifeRate, draught.manaRate, draught.toMana, draught.toShield, draught.lifeOnly) }
    rebody()
    val before = life
    life = (life + draught.life).coerceAtMost(hero.maxLife)
    mana = (mana + draught.mana).coerceAtMost(manaCap())
    shield = (shield + draught.shield).coerceAtMost(hero.maxShield)
    healedInto(life - before, draught.toMana, draught.toShield)
}

/**
 * Уход с карты: вне боя - всегда; бой идёт до конца, а карта - нет. С карты - итогом по состоянию захода ([departed]), с итога -
 * просто закрыть его.
 */
internal fun ExpeditionRun.leave() {
    if (phase != RunPhase.MAP && phase != RunPhase.DEAD && phase != RunPhase.CLEARED) return
    if (phase == RunPhase.MAP) end = departed()
    phase = RunPhase.LEFT
}

/** Итог живого ухода с карты - выходом или порталом одинаково: страж пал в этом заходе - пройдена, иначе - покинута. */
internal fun ExpeditionRun.departed(): MapEnd = if (guardianSlain) MapEnd.CLEARED else MapEnd.LEFT
