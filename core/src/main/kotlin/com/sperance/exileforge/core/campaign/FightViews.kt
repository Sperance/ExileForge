package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ally
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.SlotHolder
import com.sperance.exileforge.core.campaign.combat.chargeViews
import com.sperance.exileforge.core.campaign.combat.effects
import com.sperance.exileforge.core.campaign.combat.fateGlows
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.passiveViews
import com.sperance.exileforge.core.campaign.combat.skillViews
import com.sperance.exileforge.core.campaign.combat.strikes
import com.sperance.exileforge.core.campaign.run.AilmentView
import com.sperance.exileforge.core.campaign.run.AllyView
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.BuildupView
import com.sperance.exileforge.core.campaign.run.CastView
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FloatingHit
import com.sperance.exileforge.core.campaign.run.FoeView
import com.sperance.exileforge.core.campaign.run.HeroCastView
import com.sperance.exileforge.core.campaign.run.LungeView
import com.sperance.exileforge.core.campaign.run.RageView
import com.sperance.exileforge.core.campaign.run.SlotView
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.PetRole
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.sheet.PetFighter
import kotlin.math.roundToInt

/**
 * The combat pet as a fighter: its sheet at its level, what its role does; a new one each fight stands up whole. Since 3.33.0
 * (server 1.32.0) the hero's sheet reaches it — its levels, damage, life, speed, armour and resistances — made again only
 * when those change. Shared by the map's runs and the trials (3.49.0). Без питомца (4.6.3, Предначертание `PET_FIRST`) в бой
 * встаёт дух дара уровня героя - лист обычного питомца этого уровня без строк; настоящий питомец его заменяет.
 */
internal class PetAllies(private val index: ContentIndex, private val rules: CombatRules) {
    /** Из чего собран последний боец: питомец, или дух и уровень героя, и что героя до них доходит. */
    private data class Made(val pet: Pet?, val spirit: String?, val level: Int, val boons: Map<String, Double>)

    private var made: Pair<Made, Ally?>? = null
    private val menagerie by lazy { Menagerie(index) }

    /**
     * [pet] as a fighter under the [hero]'s sheet; since 3.70.0 the pet is the caller's, so one put to work mid-run joins the next
     * fight. Без питомца - дух Предначертания [spirit] (вид `pets.json`, null - нет) уровня героя.
     */
    fun of(hero: Combatant, pet: Pet?, spirit: String?): Ally? {
        val summoned = spirit.takeIf { pet == null }
        val key = Made(pet, summoned, if (summoned != null) hero.level else 0, PetBoons.of(hero.stats))
        made?.takeIf { it.first == key }?.let { return it.second }
        // Боец питомца или духа - правилом (4.9.0, [PetFighter]): тот же, что бьёт в Мощи героя
        val ally = PetFighter.of(index, pet, summoned, hero.level, hero.stats)?.let(::ally)
        made = key to ally
        return ally
    }

    private fun ally(fighter: PetFighter): Ally = Ally(
        fighter.species,
        Combatant(fighter.stats, fighter.level, rules),
        fighter.role == PetRole.TANK,
        if (fighter.role == PetRole.SUPPORT) menagerie.supportHeal(fighter.level) else 0.0,
        index.pets.drawFire,
    )
}

/**
 * The fight as the overlay prints it: the [monsters] of the fight as cards in the battle's order, the hero's pools and states,
 * what just landed, and the blows so far, newest first. Shared by the map's runs and the trials (3.49.0).
 */
internal fun Battle.hud(
    monsters: List<RolledMonster>,
    leader: RolledMonster,
    speed: Int,
    started: Boolean,
    paused: Boolean,
    heroTaunt: Boolean,
    level: Int,
    round: Int = 1,
    rounds: Int = 1,
): FightHud {
    val battle = this
    val h = heroFighter
    val hits = recentHits()
    fun ailments(f: Battle.Fighter) = f.ailments.groupBy { it.ailment }.map { (ailment, active) ->
        val until = active.maxOf { it.until }
        AilmentView(
            ailment,
            ((until - battle.time) / active.first().duration).toFloat().coerceIn(0f, 1f),
            active.size,
            (until - battle.time).coerceAtLeast(0.0),
            // Недуг без урона - сила, как её читает бой ([Battle.Fighter.held], 4.11.0: приём цели и потолок правила)
            if (ailment.hurts) active.sumOf { it.magnitude } else f.held(ailment),
        )
    }
    fun buildup(f: Battle.Fighter) = rules.buildup?.let {
        val lead = f.leading()
        BuildupView(
            lead?.first,
            (lead?.second ?: 0.0).toFloat().coerceIn(0f, 1f),
            f.buildup.map { it.toFloat().coerceIn(0f, 1f) },
            f.stunnedUntil > battle.time,
            f.shatter,
            f.electrocutedUntil > battle.time,
        )
    }
    val foes = foeFighters.map { f ->
        FoeView(
            f.index, monsters.getOrNull(f.index) ?: checkNotNull(foes[f.index].origin), shownLife(f.life, f.alive, f.body.maxLife), shownMax(f.body.maxLife), f.shield.roundToInt(), f.body.maxShield.roundToInt(),
            swing(f), ailments(f), f.held, f.alive, reachable(f.index), f.body.taunt, effects(f),
            f.mana.roundToInt(), f.body.maxMana.roundToInt(), place = place(f.index), buildup = buildup(f),
            barrier = f.barrier.takeIf { f.barrierUntil > time }?.roundToInt() ?: 0, waiting = waits(f.index),
        )
    }
    return FightHud(
        ally = allyFighter?.let { f -> AllyView(ally!!.code, shownLife(f.life, f.alive, f.body.maxLife), shownMax(f.body.maxLife), f.alive) },
        leader = leader, foes = foes,
        heroLife = shownLife(h.life, h.alive, h.body.maxLife), heroShield = h.shield.roundToInt(),
        hits = hits, speed = speed,
        outcome = outcome,
        heroSwing = swing(h), heroAilments = ailments(h), heroHeld = h.held, heroBuildup = buildup(h),
        lunge = lunge()?.let { (event, progress) -> LungeView(event.actor, event.action, event.kind, event.landed, progress.toFloat(), event.foe, event.pet != null) },
        events = newestFirst(),
        started = started, paused = paused,
        target = target()?.index, focus = focus,
        heroTaunt = heroTaunt,
        heroMana = h.mana.roundToInt(), heroMaxMana = manaCap().roundToInt(), heroReserved = manaReserved().roundToInt(),
        skills = skillViews(), flasks = flaskViews(), heroEffects = effects(h), heroCharges = chargeViews(), heroFate = fateGlows(),
        heroBarrier = h.barrier.roundToInt(),
        level = level,
        round = round, rounds = rounds,
        heroBody = h.body,
        boss = bossHud(),
        rage = RageView(enrage, rageRule.damage * enrage, rageRule.next(rageClock)).takeIf { outcome == null },
        heroCast = heroCast(),
        strikes = strikes(STRIKE_LUNGES * rules.lunge),
        heroFlow = lifeFlow,
        heroPassives = passiveViews(),
        foesAimHero = foesAimHero(),
        time = time,
        beat = rules.lunge,
    )
}

/** Сцена удара на экране (4.9.3) длится столько выпадов ([CombatRules.lunge]): выпад, вспышка или полёт снаряда, тряска цели. */
internal const val STRIKE_LUNGES = 3

/**
 * Замах врага скорее всего по герою (4.9.3): цель врага тянется костью в миг удара ([Battle.foeTarget]), экран её не знает -
 * без стоящего питомца, или с питомцем, что не танк и берёт меньше половины ударов, бьют героя.
 */
private fun Battle.foesAimHero(): Boolean {
    val pet = ally?.takeIf { allyFighter?.alive == true } ?: return true
    return !pet.tank && pet.drawFire < .5
}

/**
 * Удары последней секунды ([ExpeditionRun.HIT_LIFETIME]) - числа над карточками (4.9.0: с конца журнала, он идёт по времени,
 * а не фильтром всего боя на каждый кадр), по порядку журнала.
 */
private fun Battle.recentHits(): List<FloatingHit> {
    val log = events
    var from = log.size
    while (from > 0 && time - log[from - 1].time < ExpeditionRun.HIT_LIFETIME) from--
    if (from == log.size) return emptyList()
    val hits = ArrayList<FloatingHit>(log.size - from)
    for (index in from until log.size) {
        val event = log[index]
        if (event.time > time) continue
        if (event.damage > 0 || event.healed > 0 || event.kind == HitKind.EVADED || event.kind == HitKind.BLOCKED || event.action == Action.ATTACK) {
            hits += FloatingHit(
                index, event.target, event.action, event.kind, event.damage.roundToInt(), time - event.time, event.healed.roundToInt(),
                event.type, event.inflicted, event.stunned, event.foe, event.pet != null,
            )
        }
    }
    return hits
}

/** Сцена применения умения (4.4.0) длится столько выпадов удара ([CombatRules.lunge]): 0,5-0,9 с макета «Жилы энергии». */
internal const val CAST_LUNGES = 4

/**
 * Последнее применение умения героя (4.4.0), пока его сцена идёт ([CAST_LUNGES] выпадов): умение из набора героя, все строки
 * того же мига - по кому оно легло. Умение на себя - без целей.
 */
private fun Battle.heroCast(): HeroCastView? {
    val window = CAST_LUNGES * rules.lunge
    // С конца журнала, пока строки моложе сцены (4.9.0): журнал идёт по времени, весь бой обходить незачем
    var last = log.size - 1
    while (last >= 0) {
        val it = log[last]
        if (time - it.time >= window) {
            last = -1
            break
        }
        if (it.actor == Side.HERO && it.action == Action.SKILL && it.skill != null && it.pet == null) break
        last--
    }
    if (last < 0 || log[last].time > time) return null
    val event = log[last]
    val used = (kit.actives.filterNotNull() + kit.passives).firstOrNull { it.skill.code == event.skill } ?: return null
    var first = last
    while (first > 0 && log[first - 1].time == event.time && log[first - 1].skill == event.skill) first--
    val moment = log.subList(first, last + 1).filter { it.actor == Side.HERO && it.skill == event.skill }
    val self = moment.all { it.onSelf }
    return HeroCastView(
        serial = first,
        code = used.skill.code,
        type = used.skill.type,
        element = used.skill.element,
        tier = used.tier,
        foes = if (self) emptyList() else moment.filterNot { it.onSelf }.map { it.foe }.distinct(),
        self = self,
        echo = event.echo,
        progress = ((time - event.time) / window).toFloat().coerceIn(0f, 1f),
    )
}

/** Босс боя (3.92.0): первый враг редкости босса или с фазами - его пороги и умение на подходе. */
private fun Battle.bossHud(): BossHud? {
    val i = guardian ?: return null
    val foe = foes[i]
    val fighter = foeFighters[i]
    val cast = (foe.skills + learned[i].orEmpty()).mapNotNull { skill ->
        val ready = fighter.readyAt[skill.code] ?: return@mapNotNull null
        CastView(skill.code, (ready - time).coerceAtLeast(0.0), skill.cooldown / fighter.body.recovery(skill.spell))
    }.minByOrNull { it.left }?.takeIf { fighter.alive }
    val slots = slotHolders.map { holder ->
        when (holder) {
            null -> null
            is SlotHolder.Minion -> SlotView.Minion(holder.index)
            is SlotHolder.Totem -> holder.totem.let { t -> SlotView.Totem(t.serial, t.totem.totem.code, t.totem.totem.kind, (t.until - time).coerceAtLeast(0.0), t.until - t.raised, t.totem.totem.element) }
        }
    }
    val hud = BossHud(i, foe.phase, foe.phases.map { it.step.at }, foe.phases.indices.map { (i to it) in phased }, cast, slots, tainted = foe.tainted)
    return riftHud(hud)
}

/** Полоса стража Разлома (3.96.0): печати, Законы, украденное - поверх полосы босса. */
private fun Battle.riftHud(hud: BossHud): BossHud {
    val fight = riftFight ?: return hud
    return hud.copy(
        rift = fight.guard,
        seals = fight.seals,
        sealsMax = fight.sealsMax,
        sealHits = fight.sealHits,
        sealEvery = fight.rules.warden?.hits ?: 0,
        law = fight.laws,
        lawNext = fight.next,
        lawIn = if (fight.rules.lord != null) (fight.lawAt - time).coerceAtLeast(0.0) else 0.0,
        lawElement = fight.element?.takeIf { RiftLaw.ONE_ELEMENT in fight.laws }?.name,
        lawNextElement = fight.nextElement?.name,
        stolen = fight.stolen.toList(),
        devoured = fight.devoured,
    )
}

/**
 * Здоровье на экране (3.88.8): у живого - вверх до целого и не меньше 1, у павшего - 0. «0» у живого противника читалось
 * как смерть, хотя у него оставалась доля единицы. 4.10.x: не выше показанного максимума [max] ([shownMax]) - полный запас
 * с дробным максимумом читался «776 / 775»; журнал боя максимума не знает и его не ставит. [falling] - здоровье тает (на карте
 * под недугом или дегеном): вниз до целого, число не обещает того, чего уже нет.
 */
fun shownLife(life: Double, alive: Boolean, max: Double = Double.POSITIVE_INFINITY, falling: Boolean = false): Int = if (alive) (if (falling) kotlin.math.floor(life) else kotlin.math.ceil(life)).toInt().coerceAtMost(shownMax(max)).coerceAtLeast(1) else 0

/** Максимум запаса на экране (4.10.x): одно округление для всех полос, с ним сверяется [shownLife]. */
fun shownMax(max: Double): Int = max.roundToInt()
