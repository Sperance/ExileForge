package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FeatureStat
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.FeatureUse
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent

// ==================== Объекты карты (3.89.1, сервер 1.81.3) ====================

/** Сбор узла [spot]: прошло [elapsed] секунд из его [MapFeature.Node.seconds]. */
internal class Channel(val spot: NodeSpot, var elapsed: Double = 0.0)

/** Горение или яд ловушки на герое: [perSecond] здоровья в секунду ещё [left] секунд. */
internal class Burn(val perSecond: Double, var left: Double)

/** Удар ловушки, как его пишет полоса карты: вид ловушки и сколько здоровья он снял. */
data class HazardView(val trap: String, val damage: Int)

/** Правила ловушек контента. */
private val ExpeditionRun.traps get() = index.campaign.features?.traps

/** Шаг героя сделал что-то с объектом карты: лист к выбору или сработавший объект. */
internal fun ExpeditionRun.feature(action: FeatureAction) {
    when (action) {
        is FeatureAction.Offer -> offer = action.spot
        is FeatureAction.Trigger -> action.spot.resolve(this, action.choice)
    }
}

/** Выбор [choice] объекта [spot] - в журнал (объект помнит его сразу, даже если журнал уже закрыт); событие или null. */
internal fun ExpeditionRun.recordFeature(spot: FeatureSpot, choice: Int): RunEvent? {
    spot.take(choice)
    // Строки выбора (сделка, редкость за ловушку, скорость за комнату, сила за узел) - на героя и бои сразу
    rebargain()
    return record { RunEvent.Feature(it, spot.id, choice) }?.also { featureEvents[it.n] = spot to choice }
}

/** Событие с добычей объекта (сундук комнаты, сбор узла): её покажет та же панель, что у сундука, когда придёт ответ. */
internal fun ExpeditionRun.showLoot(event: RunEvent?) {
    rewarding(event)?.let { chestEvent = it.n }
}

/** Игрок выбрал на листе объекта [choice]: пару алтаря или вещь торговца. Исчерпанный объект закрывает лист. */
internal fun ExpeditionRun.choose(choice: Int) {
    val spot = offer?.takeIf { phase == RunPhase.MAP && it.feature.accepts(choice, it.taken) } ?: return
    spot.resolve(this, choice)
    if (spot.spent || spot is AltarSpot) offer = null
}

/** «Собрать» на листе узла: сбор идёт [MapFeature.Node.seconds], забег стоит. Инструмент проверяет лист и сервер. */
internal fun ExpeditionRun.startGather() {
    val spot = (offer as? NodeSpot)?.takeIf { phase == RunPhase.MAP && !it.spent } ?: return
    channel = Channel(spot)
}

/** Сбор идёт: по его концу - событие узла, лист закрыт. */
internal fun ExpeditionRun.gather(channel: Channel, dt: Double) {
    channel.elapsed += dt
    if (channel.elapsed < channel.spot.node.seconds) return
    this.channel = null
    offer = null
    channel.spot.resolve(this, MapFeature.TRIGGER)
}

/** Лист объекта закрыт без выбора; алтарь так не закрывается - сделку надо заключить. */
internal fun ExpeditionRun.leaveOffer(): Boolean {
    val spot = offer ?: return false
    if (spot is AltarSpot && !spot.spent) return true
    offer = null
    channel = null
    return true
}

/** Сервер отклонил событие [n]: объект, чей это был выбор, снова прежний. */
internal fun ExpeditionRun.refuseFeature(n: Int) {
    val (spot, choice) = featureEvents.remove(n) ?: return
    spot.refused(this, choice)
    rebargain()
}

/**
 * Строки объектов карты (сделки алтарей, редкость за ловушки, скорость за комнаты, сила за узлы) пересчитаны по журналу
 * объектов мира: герой несёт их до конца карты, стаи подкрепления встают у своих алтарей. Строки боя монстров ложатся при
 * каждом бое ([pactFoe]).
 */
internal fun ExpeditionRun.rebargain() {
    if (vaal) return
    bargain(run.features.bonuses(world.features.flatMap { spot -> spot.taken.map { FeatureUse(spot.id, it) } }))
}

/** Сделки [pacts] на этом забеге: лист героя, окно карты и стаи подкрепления. */
internal fun ExpeditionRun.bargain(pacts: Map<String, Double>) {
    if (pacts == this.pacts) return
    this.pacts = pacts
    mapEffects = MapEffects.sum(baseEffects, pacts)
    regear(build.gear)
    if (!vaal) reinforce()
}

/** Стаи подкрепления: по жетону на каждую из сделок каждого заключённого алтаря, рядом с ним, по порядку алтарей. */
private fun ExpeditionRun.reinforce() {
    val buffs = MapEffects.buffs(baseEffects)
    var token = 0
    world.features.filterIsInstance<AltarSpot>().filter { it.spent }.sortedBy { it.id }.forEach { altar ->
        val own = run.features.bonuses(altar.taken.map { FeatureUse(altar.id, it) })[MapStat.REINFORCEMENTS.code]?.toInt() ?: 0
        repeat(own) {
            world.summon(Run.REINFORCEMENT + token, spawns.summoned(Run.REINFORCEMENT + token, buffs), altar.cell)
            token++
        }
    }
}

/** Стражи сокровищ (строка карты «Стражи сокровищ»): редкая стая у каждой комнаты и узла, жетон `Run.GUARD + номер объекта`. */
internal fun ExpeditionRun.guard() {
    val buffs = MapEffects.buffs(baseEffects)
    world.features.filter { it.id in run.guards }.forEach { spot -> world.summon(Run.GUARD + spot.id, spawns.summoned(Run.GUARD + spot.id, buffs), spot.cell) }
}

/**
 * Член боя со сделками алтаря: строки карты сделок - поверх его статов, босс - ещё и «сила босса», волшебный и редкий - лишние
 * строки на своём потоке. Без сделок - как стоит.
 */
internal fun ExpeditionRun.pactFoe(member: FightMember): RolledMonster {
    // Ловушки ранят монстров (3.89.1, сила уникалки): на карте с ловушками стая встаёт в бой с меньшим здоровьем
    val strike = run.context.feature(FeatureStat.TRAP_STRIKE).takeIf { it > 0 && world.features.any { spot -> spot is TrapSpot } }
    if (pacts.isEmpty() && strike == null) return member.monster
    val buffs = MapEffects.buffs(pacts) + (if (member.agent === world.boss) MapEffects.bossBuffs(pacts) else emptyList()) +
        listOfNotNull(strike?.let { MonsterEffect(CoreStat.HEALTH.code, Op.MORE, -it.coerceAtMost(traps?.maxStrike ?: 0.0)) })
    val extra = (pacts[MapStat.MONSTER_MODS.code] ?: 0.0).toInt()
    val dice = run.streams.of("pactMods", member.agent.id * Run.PACK_SLOTS + member.index)
    return spawns.buffed(MonsterRoller(index).empowered(member.monster, run.pool, zone.level, extra, dice), buffs)
}

/**
 * Ловушка сработала: удар - доля максимума здоровья её стихией, броня режет физический, сопротивление - стихийный; горение и
 * яд тикают дальше. Ловушка может убить.
 */
internal fun ExpeditionRun.spring(feature: MapFeature.Trap) {
    val trap = feature.trap
    val type = DamageType.element(trap.element) ?: DamageType.PHYSICAL
    // Сила ловушек карты и защита героя от её стихии (3.89.1): 100% защиты - ловушка не ранит
    val ward = FeatureStat.ward(trap.element)?.let { run.context.feature(it) } ?: 0.0
    val share = feature.power * (1 - ward / 100).coerceAtLeast(0.0)
    val raw = hero.maxLife * trap.hit / 100 * share
    val dealt = if (type == DamageType.PHYSICAL) raw * (1 - hero.physicalMitigation(raw, rules.armour.factor)) else raw * (1 - hero.resist(type))
    if (trap.dot > 0 && share > 0) burns += Burn(hero.maxLife * trap.dot / 100 * share * (1 - hero.resist(type)) * hero.damageTaken(type), trap.seconds)
    hazard = HazardView(trap.code, (dealt * hero.damageTaken(type)).toInt())
    hazardLeft = traps?.shown ?: 0.0
    hurt(dealt * hero.damageTaken(type))
}

/** Горение и яд ловушек на [dt] секунд дороги. */
internal fun ExpeditionRun.burn(dt: Double) {
    if (burns.isEmpty()) return
    val damage = burns.sumOf { it.perSecond * minOf(dt, it.left) }
    burns.forEach { it.left -= dt }
    burns.removeAll { it.left <= 0 }
    hurt(damage)
}

/** Урон вне боя: здоровье падает, ноль - гибель на карте. */
private fun ExpeditionRun.hurt(damage: Double) {
    if (damage <= 0 || phase != RunPhase.MAP) return
    life = (life - damage).coerceAtLeast(0.0)
    if (life <= 0) fallOnMap()
}

/** Гибель на карте вне боя (ловушкой): та же смерть, что в бою, - цена, событие гибели и итог карты. */
internal fun ExpeditionRun.fallOnMap() {
    autopilot = null
    offer = null
    channel = null
    burns.clear()
    phase = RunPhase.DEAD
    deaths++
    end = MapEnd.FELL
    record { RunEvent.Fall(it, vaal) }?.let {
        fallEvent = it.n
        fall = deathLoss()
    }
    onFallen()
}
