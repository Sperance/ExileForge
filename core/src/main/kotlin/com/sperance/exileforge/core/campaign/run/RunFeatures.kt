package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.CarriedAilment
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.EnergyShield
import com.sperance.exileforge.core.campaign.combat.HeroCarry
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FeatureStat
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.TrapKind
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.FeatureUse
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import kotlin.math.roundToInt

/** Сбор узла [spot]: прошло [elapsed] секунд из его [MapFeature.Node.seconds]. */
internal class Channel(val spot: NodeSpot, var elapsed: Double = 0.0) {
    /** Сбор как действие у объекта: узел и пройденная доля его времени. */
    val action: ObjectAction get() = ObjectAction(spot.feature, if (spot.node.seconds <= 0) 1f else (elapsed / spot.node.seconds).toFloat().coerceIn(0f, 1f))
}

/** Действие героя у объекта сейчас: идущий сбор, иначе объект, у которого герой стоит; null - ничего. */
internal fun ExpeditionRun.objectAction(): ObjectAction? = channel?.action ?: world.features.firstNotNullOfOrNull { it.action(world) }

/**
 * Тик недуга на герое вне боя: урон идёт каждый кадр, а на экран - тиками по [TICK_SECONDS]: сколько набралось со щита и
 * здоровья.
 */
internal class AilmentTick {
    var wound = Wound()
    var clock = 0.0
}

/** Сколько урона карты принял щит и сколько - здоровье. */
internal data class Wound(val shield: Double = 0.0, val life: Double = 0.0) {
    operator fun plus(other: Wound) = Wound(shield + other.shield, life + other.life)
    val total: Double get() = shield + life
}

/**
 * Урон карты над героем: удар ловушки или тик недуга на герое - стихией [type], [shield] со щита и [life] со здоровья. Удар
 * ещё и называет своё состояние [ailment]; тик - недуг, что тикнул ([of]: и принесённый из боя, и от ловушки - одна система).
 * [id] свой у каждого: два одинаковых подряд - два числа, а не одно.
 */
data class HazardView(
    val id: Int,
    val type: DamageType,
    val life: Int,
    val shield: Int,
    val ailment: HazardAilment? = null,
    val of: Ailment? = null,
) {
    /** Тик недуга, а не удар ловушки. */
    val tick: Boolean get() = of != null

    /** Всё, что снял удар: щит и здоровье вместе. */
    val total: Int get() = life + shield
}

/**
 * Состояние, что ловушка оставляет на герое: выводится из типа её урона ([Ailment.of]: огонь - горение, хаос - яд,
 * физический - кровотечение), если оно тикает уроном; [lifeShare] - процент максимума здоровья героя в секунду до сопротивления,
 * [seconds] - сколько длится.
 */
data class HazardAilment(val ailment: Ailment, val type: DamageType, val lifeShare: Double, val seconds: Double) {
    companion object {
        /** Состояние ловушки [trap] урона [type] силой [share]; null - ловушка без тика или вовсе не ранит. */
        fun of(trap: TrapKind, type: DamageType, share: Double): HazardAilment? = Ailment.of(type).takeIf { it.hurts && trap.dot > 0 && share > 0 }?.let { HazardAilment(it, type, trap.dot * share, trap.seconds) }
    }
}

/** Число урона карты на экране и сколько секунд ему ещё висеть. */
internal class ShownHazard(val view: HazardView, var left: Double)

/** Как часто тик эффекта ловушки встаёт числом над героем, секунд. */
internal const val TICK_SECONDS = 1.0

/** Правила ловушек контента. */
private val ExpeditionRun.traps get() = index.campaign.features?.traps

/**
 * Шаг героя сделал что-то с объектом карты: лист к выбору или сработавший объект.
 */
internal fun ExpeditionRun.feature(action: FeatureAction) {
    when (action) {
        is FeatureAction.Offer -> offer = action.spot
        is FeatureAction.Trigger -> action.spot.resolve(this, action.choice)
        is FeatureAction.Spring -> spring(action.spot.feature as MapFeature.Trap)
        is FeatureAction.Rouse -> action.spot.rouse(this, action.group)
    }
}

/**
 * Выбор [choice] объекта [spot] - в журнал (объект помнит его сразу, даже если журнал уже закрыт); событие или null.
 *
 * Строки выбора (сделка, редкость за ловушку, скорость за комнату, сила за узел) ложатся на героя и бои сразу.
 */
internal fun ExpeditionRun.recordFeature(spot: FeatureSpot, choice: Int): RunEvent? {
    spot.take(choice)
    rebargain()
    return record { RunEvent.Feature(it, spot.id, choice) }?.also { featureEvents[it.n] = spot to choice }
}

/** Событие с добычей объекта (сундук комнаты, сбор узла): её добыча вылетит в сумку, как у сундука, когда придёт ответ. */
internal fun ExpeditionRun.showLoot(event: RunEvent?) {
    rewarding(event)?.let { dropped(it.n) }
}

/** Игрок выбрал на листе объекта [choice]: пару алтаря или вещь торговца. Исчерпанный объект закрывает лист. */
internal fun ExpeditionRun.choose(choice: Int) {
    val spot = offer?.takeIf { phase == RunPhase.MAP && it.feature.accepts(choice, it.state(world)) } ?: return
    spot.resolve(this, choice)
    if (spot.spent || spot is AltarSpot) offer = null
}

/**
 * «Собрать» на листе узла: сбор идёт [MapFeature.Node.seconds], забег стоит. Инструмент проверяет лист и сервер; узел под стражами
 * сокровищ ждёт их гибели.
 */
internal fun ExpeditionRun.startGather() {
    val spot = (offer as? NodeSpot)?.takeIf { phase == RunPhase.MAP && !it.spent && !world.guarded(it.id) } ?: return
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

/** Лист объекта закрыт без выбора; мимо алтаря тоже можно пройти - он ждёт нетронутым. */
internal fun ExpeditionRun.leaveOffer(): Boolean {
    offer ?: return false
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

/**
 * Стаи подкрепления: по жетону `Run.REINFORCEMENT + k` на каждую стаю сделок каждого заключённого
 * алтаря, рядом с ним, по порядку алтарей. Встающий жетон встаёт один раз.
 */
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
 * строки на своём потоке (`MAP_MONSTER_MODS`). Без сделок - как стоит.
 *
 * Ловушки ранят монстров (сила уникалки): на карте с ловушками стая встаёт в бой с меньшим здоровьем.
 */
internal fun ExpeditionRun.pactFoe(member: FightMember): RolledMonster {
    val strike = run.context.feature(FeatureStat.TRAP_STRIKE).takeIf { it > 0 && world.features.any { spot -> spot is TrapSpot } }
    if (pacts.isEmpty() && strike == null) return member.monster
    val buffs = MapEffects.buffs(pacts) + (if (member.agent === world.boss) MapEffects.bossBuffs(pacts) else emptyList()) +
        listOfNotNull(strike?.let { MonsterEffect(CoreStat.HEALTH.code, Op.MORE, -it.coerceAtMost(traps?.maxStrike ?: 0.0)) })
    val extra = (pacts[MapStat.MONSTER_MODS.code] ?: 0.0).toInt()
    if (extra <= 0) return spawns.buffed(member.monster, buffs)
    val dice = run.streams.of("pactMods", member.agent.id * Run.PACK_SLOTS + member.index)
    return spawns.buffed(MonsterRoller(index).empowered(member.monster, run.pool, run.levelOf(zone), extra, dice), buffs)
}

/**
 * Ловушка сработала: удар - доля максимума здоровья её стихией, броня режет физический, сопротивление - стихийный; горение, яд и
 * кровотечение ложатся недугом на героя ([HeroCarry], та же система, что недуги из боя) - тикают на карте и входят в следующий
 * бой. Ловушка может убить.
 *
 * Сила ловушек карты и защита героя от её стихии: 100% защиты - ловушка не ранит. Состояние ловушки - из типа её урона; основа до защит
 * (яд) - сопротивление режет тик, а не наложение.
 */
internal fun ExpeditionRun.spring(feature: MapFeature.Trap) {
    val trap = feature.trap
    val type = DamageType.element(trap.element) ?: DamageType.PHYSICAL
    val ward = FeatureStat.ward(trap.element)?.let { run.context.feature(it) } ?: 0.0
    val share = feature.power * (1 - ward / 100).coerceAtLeast(0.0)
    val raw = hero.maxLife * trap.hit / 100 * share
    val dealt = if (type == DamageType.PHYSICAL) raw * (1 - hero.physicalMitigation(raw, rules.armour.factor)) else raw * (1 - hero.resist(type))
    val ailment = HazardAilment.of(trap, type, share)
    ailment?.let { afflict(CarriedAilment(it.ailment, it.seconds, hero.maxLife * it.lifeShare / 100 * (if (law.raw(it.ailment)) 1.0 else 1 - hero.resist(type)), it.seconds, chaos = type == DamageType.CHAOS)) }
    val wound = hurt(dealt * hero.damageTaken(type), type)
    showHazard(HazardView(nextHazard++, type, wound.life.roundToInt(), wound.shield.roundToInt(), ailment))
}

/** Число урона карты над героем на [TrapRules.shown] секунд. */
internal fun ExpeditionRun.showHazard(view: HazardView) {
    hazards += ShownHazard(view, if (view.tick) TICK_SECONDS * 1.5 else traps?.shown ?: 0.0)
}

/** Урон вне боя вида [type] (через щит, как в бою, - [EnergyShield]): ноль здоровья - гибель на карте. Сколько принял щит и здоровье. */
internal fun ExpeditionRun.hurt(damage: Double, type: DamageType?): Wound = wound(damage, EnergyShield.absorbed(shield, damage, type))

/** Урон [damage] вне боя, из которого щит принимает [absorbed], остальное - здоровье: ноль здоровья - гибель на карте. Сколько принял щит и здоровье. */
internal fun ExpeditionRun.wound(damage: Double, absorbed: Double): Wound {
    if (damage <= 0 || phase != RunPhase.MAP) return Wound()
    shield -= absorbed
    hitAt = seconds
    val taken = minOf(life, damage - absorbed)
    life -= taken
    if (life <= 0) fallOnMap()
    return Wound(absorbed, taken)
}

/** Гибель на карте вне боя (ловушкой): та же смерть, что в бою, - цена, событие гибели и итог карты. */
internal fun ExpeditionRun.fallOnMap() {
    offer = null
    channel = null
    carry = HeroCarry.NONE
    ticks.clear()
    phase = RunPhase.DEAD
    deaths++
    end = MapEnd.FELL
    record { RunEvent.Fall(it, vaal) }?.let {
        fallEvent = it.n
        fall = deathLoss()
    }
    onFallen()
}
