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
import com.sperance.exileforge.rules.content.WearRequest
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.FeatureUse
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import kotlin.math.hypot
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
 * физический - кровотечение, холод - охлаждение, молния - шок). [strength] - у ранящих процент максимума здоровья героя в секунду
 * до сопротивления, у холода и шока - сила недуга в процентах; [seconds] - сколько длится.
 */
data class HazardAilment(val ailment: Ailment, val type: DamageType, val strength: Double, val seconds: Double) {
    companion object {
        /** Состояние ловушки [trap] урона [type] силой [share]; null - ловушка без недуга или вовсе не ранит. */
        fun of(trap: TrapKind, type: DamageType, share: Double): HazardAilment? {
            val strength = Ailment.of(type).let { if (it.hurts) trap.dot else trap.hold } * share
            return HazardAilment(Ailment.of(type), type, strength, trap.seconds).takeIf { strength > 0 && trap.seconds > 0 }
        }
    }
}

/**
 * Что ловушка сделает с героем сейчас - подсказка по касанию и сам удар: вид [kind], тип урона [type], сколько снимет удар до щита
 * [damage] - [lifeShare] процентов максимума здоровья (защита от ловушек и потолок удара правил уже легли), и недуг [ailment];
 * [every] - раз в сколько секунд бьёт.
 */
data class TrapThreat(val kind: TrapKind, val type: DamageType, val damage: Double, val lifeShare: Double, val ailment: HazardAilment?, val every: Double)

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
        is FeatureAction.Strike -> strike(action.spot, action.kind)
        is FeatureAction.Rouse -> action.spot.rouse(this, action.group)
    }
}

/**
 * Выбор [choice] объекта [spot] - в журнал (объект помнит его сразу, даже если журнал уже закрыт); событие или null.
 *
 * Строки выбора (сделка, скорость за комнату, сила за узел) ложатся на героя и бои сразу.
 */
internal fun ExpeditionRun.recordFeature(spot: FeatureSpot, choice: Int, wear: WearRequest? = null): RunEvent? {
    spot.take(choice)
    rebargain()
    return record { RunEvent.Feature(it, spot.id, choice, wear) }?.also { featureEvents[it.n] = spot to choice }
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
 * «Купить и надеть» вещь [choice] бродячего торговца на место [wear]: только на карте и между боями ([gearLock]) - выбор с надеванием в
 * журнал ([RunEvent.Feature.wear]); отказ сервера вернёт товар, как у простой покупки.
 */
internal fun ExpeditionRun.chooseWorn(choice: Int, wear: WearRequest) {
    val spot = (offer as? MerchantSpot)?.takeIf { phase == RunPhase.MAP && gearLock == null && it.feature.accepts(choice, it.state(world)) } ?: return
    recordFeature(spot, choice, wear)
    if (spot.spent) offer = null
}

/**
 * «Собрать» на листе узла: лист закрыт, сбор идёт [MapFeature.Node.seconds] при тикающем мире - монстры ходят, здоровье и недуги
 * героя идут своим ходом. Инструмент проверяет лист и сервер; узел под стражами сокровищ ждёт их гибели.
 */
internal fun ExpeditionRun.startGather() {
    val spot = (offer as? NodeSpot)?.takeIf { phase == RunPhase.MAP && !it.spent && !world.guarded(it.id) } ?: return
    offer = null
    channel = Channel(spot)
}

/** Сбор прерван началом боя, вызовом стража или шагом героя: доля пропала с нулём, узел ждёт нового «Собрать»; игроку - тост. */
internal fun ExpeditionRun.breakGather() {
    channel ?: return
    channel = null
    callbacks.gatherBroken()
}

/** Сбор идёт тем же шагом, что мир ([walk]): по его концу - событие узла. */
internal fun ExpeditionRun.gather(channel: Channel, dt: Double) {
    channel.elapsed += dt
    if (channel.elapsed < channel.spot.node.seconds) return
    this.channel = null
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
 * Строки объектов карты (сделки алтарей, скорость за комнаты, сила за узлы) пересчитаны по журналу
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
 * Удар ловушки [feature] видом [kind] по герою сейчас, не нанесённый: доля максимума здоровья её стихией с силой ловушек карты
 * ([MapFeature.Trap.power]: тир и сила механики), не больше потолка удара правил (`traps.maxHit`) - одним срабатыванием не убивает;
 * броня, сопротивления и прочие защиты листа его не режут, энергощит принимает его первым, как любой урон ([hurt]). Режет только защита
 * героя от ловушек - общая (`TRAP_WARD`) и стихии (`TRAP_WARD_<стихия>`), 100% - ловушка не ранит. Состояние - из типа урона; недуг
 * ловушки сопротивление режет на тике ([carried]), а не на наложении.
 */
internal fun ExpeditionRun.threat(feature: MapFeature.Trap, kind: TrapKind = feature.trap): TrapThreat {
    val type = DamageType.element(kind.element) ?: DamageType.PHYSICAL
    val ward = (FeatureStat.ward(kind.element)?.let { run.context.feature(it) } ?: 0.0) + run.context.feature(FeatureStat.TRAP_WARD)
    val share = feature.power * (1 - ward / 100).coerceAtLeast(0.0)
    val lifeShare = minOf(kind.hit * share, traps?.maxHit ?: 100.0)
    return TrapThreat(kind, type, hero.maxLife * lifeShare / 100, lifeShare, HazardAilment.of(kind, type, share), kind.every)
}

/** Что ловушка [spot] сделает с героем сейчас - подсказка по касанию: её вид этого мига (у пульсирующей зоны - стихия цикла). */
fun ExpeditionRun.trapThreat(spot: TrapSpot): TrapThreat = threat(spot.trap, spot.kindAt(world.clock))

/**
 * Ловушка [feature] ударила видом [kind] ([threat]): горение, яд, кровотечение, охлаждение и шок ложатся недугом на героя
 * ([HeroCarry], та же система, что недуги из боя) - тикают на карте и входят в следующий бой; жернов отбрасывает героя от себя
 * поперёк жёлоба ([spot]). Ловушка может добить раненого.
 */
internal fun ExpeditionRun.spring(feature: MapFeature.Trap, kind: TrapKind = feature.trap, spot: TrapSpot? = null) {
    val threat = threat(feature, kind)
    threat.ailment?.let { afflict(carried(it)) }
    val wound = hurt(threat.damage, threat.type)
    showHazard(HazardView(nextHazard++, threat.type, wound.life.roundToInt(), wound.shield.roundToInt(), threat.ailment))
    if (kind.knock > 0 && spot != null && phase == RunPhase.MAP) knockBack(spot, kind.knock)
}

/** Удар ловушки [spot] видом [kind] на карте. */
internal fun ExpeditionRun.strike(spot: TrapSpot, kind: TrapKind) = spring(spot.trap, kind, spot)

/** Недуг ловушки на герое: ранящий - урон в секунду от максимума здоровья под сопротивлением (кроме основы до защит), прочие - своей силой. */
private fun ExpeditionRun.carried(ailment: HazardAilment): CarriedAilment {
    val magnitude = if (ailment.ailment.hurts) hero.maxLife * ailment.strength / 100 * (if (law.raw(ailment.ailment)) 1.0 else 1 - hero.resist(ailment.type)) else ailment.strength
    return CarriedAilment(ailment.ailment, ailment.seconds, magnitude, ailment.seconds, chaos = ailment.type == DamageType.CHAOS)
}

/** Отброс от тела ловушки [spot] на [cells] клеток: от середины тела к герою; герой на теле - вбок от оси ловушки. */
private fun ExpeditionRun.knockBack(spot: TrapSpot, cells: Double) {
    val (bx, by) = spot.body(world.clock) ?: (spot.layout.centreX to spot.layout.centreY)
    var dx = world.heroX - bx
    var dy = world.heroY - by
    val length = hypot(dx, dy)
    if (length < KNOCK_EPSILON) {
        dx = -spot.layout.axis.dy.toDouble()
        dy = spot.layout.axis.dx.toDouble()
    } else {
        dx /= length
        dy /= length
    }
    world.knock(dx * cells, dy * cells)
}

/** Ближе этого к середине тела ловушки отброс идёт вбок от её оси, клеток. */
private const val KNOCK_EPSILON = .05

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
    callbacks.fallen()
}
