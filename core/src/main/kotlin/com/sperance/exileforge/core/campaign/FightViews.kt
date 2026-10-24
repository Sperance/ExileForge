package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.SlotHolder
import com.sperance.exileforge.core.campaign.combat.chargeViews
import com.sperance.exileforge.core.campaign.combat.effects
import com.sperance.exileforge.core.campaign.combat.fateGlows
import com.sperance.exileforge.core.campaign.combat.heroLadder
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.intimidateView
import com.sperance.exileforge.core.campaign.combat.passiveViews
import com.sperance.exileforge.core.campaign.combat.skillViews
import com.sperance.exileforge.core.campaign.combat.strikes
import com.sperance.exileforge.core.campaign.combat.wanted
import com.sperance.exileforge.core.campaign.run.AilmentView
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.BuildupView
import com.sperance.exileforge.core.campaign.run.CastState
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
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.roll.RolledMonster
import kotlin.math.roundToInt

/**
 * The fight as the overlay prints it: the [monsters] of the fight as cards in the battle's order, the hero's pools and states,
 * what just landed, and the blows so far, newest first. Shared by the map's runs and the trials.
 *
 * Недуг без урона показан силой, как её читает бой ([Battle.Fighter.held], приём цели и потолок правила).
 */
internal fun Battle.hud(
    monsters: List<RolledMonster>,
    leader: RolledMonster,
    speed: Int,
    started: Boolean,
    paused: Boolean,
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
            invulnerableLeft = if (f.alive) (f.invulnerableUntil - time).coerceAtLeast(0.0) else 0.0, invulnerableTotal = f.invulnerableTotal,
            inert = f.inert,
        )
    }
    return FightHud(
        leader = leader, foes = foes,
        heroLife = shownLife(h.life, h.alive, h.body.maxLife), heroShield = h.shield.roundToInt(),
        hits = hits, speed = speed,
        outcome = outcome,
        heroSwing = swing(h), heroAilments = ailments(h), heroHeld = h.held, heroBuildup = buildup(h),
        lunge = lunge()?.let { (event, progress) -> LungeView(event.actor, event.action, event.kind, event.landed, progress.toFloat(), event.foe) },
        events = newestFirst(),
        started = started, paused = paused,
        target = target()?.index, focus = focus,
        heroIntimidate = intimidateView(),
        heroMana = h.mana.roundToInt(), heroMaxMana = manaCap().roundToInt(), heroReserved = manaReserved().roundToInt(),
        skills = skillViews(), flasks = flaskViews(), heroEffects = effects(h), heroCharges = chargeViews(), heroFate = fateGlows(),
        heroBarrier = h.barrier.roundToInt(),
        heroMaxLife = shownMax(h.body.maxLife), heroMaxShield = h.body.maxShield.roundToInt(),
        level = level,
        round = round, rounds = rounds,
        heroBody = h.body,
        heroLadder = heroLadder(),
        boss = bossHud(),
        rage = RageView(enrage, rageRule.damage * enrage, rageRule.next(rageClock)).takeIf { outcome == null },
        heroCast = heroCast(),
        strikes = strikes(STRIKE_LUNGES * rules.lunge),
        heroFlow = lifeFlow,
        heroPassives = passiveViews(),
        heroPowers = powers.views(),
        time = time,
        beat = rules.lunge,
    )
}

/** Сцена удара на экране длится столько выпадов ([CombatRules.lunge]): выпад, вспышка или полёт снаряда, тряска цели. */
internal const val STRIKE_LUNGES = 3

/**
 * Удары последней секунды ([ExpeditionRun.HIT_LIFETIME]) - числа над карточками (с конца журнала, он идёт по времени,
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
        if (event.damage > 0 || event.healed > 0 || event.kind == HitKind.EVADED || event.kind == HitKind.BLOCKED || event.kind == HitKind.INVULNERABLE || event.action == Action.ATTACK) {
            hits += FloatingHit(
                index, event.target, event.action, event.kind, event.damage.roundToInt(), time - event.time, event.healed.roundToInt(),
                event.type, event.inflicted, event.stunned, event.foe, event.ailment,
            )
        }
    }
    return hits
}

/** Сцена применения умения длится столько выпадов удара ([CombatRules.lunge]): 0,5-0,9 с «Жил энергии». */
internal const val CAST_LUNGES = 4

/**
 * Последнее применение умения героя, пока его сцена идёт ([CAST_LUNGES] выпадов): умение из набора героя, все строки
 * того же мига - по кому оно легло. Умение на себя - без целей.
 *
 * Журнал читается с конца, пока строки моложе сцены: он идёт по времени.
 */
private fun Battle.heroCast(): HeroCastView? {
    val window = CAST_LUNGES * rules.lunge
    var last = log.size - 1
    while (last >= 0) {
        val it = log[last]
        if (time - it.time >= window) {
            last = -1
            break
        }
        if (it.actor == Side.HERO && it.action == Action.SKILL && it.skill != null) break
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

/**
 * Босс боя: первый враг редкости босса или с фазами - его пороги и умение на подходе.
 *
 * На подходе первым - умение, что босс применяет сейчас; затем готовое, что держит оглушение, заморозка или устрашение;
 * затем ближайшее по отсчёту; готовое без повода - последним.
 */
private fun Battle.bossHud(): BossHud? {
    val i = guardian ?: return null
    val foe = foes[i]
    val fighter = foeFighters[i]
    val casting = fighter.casting?.let { c -> CastView(c.code, (c.until - time).coerceAtLeast(0.0), c.until - c.from, CastState.CASTING) }
    val timed = (foe.skills + learned[i].orEmpty()).mapNotNull { skill -> fighter.readyAt[skill.code]?.let { skill to it } }
    val coming = timed.filter { it.second > time }.minByOrNull { it.second }?.let { (skill, ready) ->
        CastView(skill.code, ready - time, skill.cooldown / fighter.body.recovery, CastState.COUNTDOWN)
    }
    val ready = timed.firstOrNull { it.second <= time }?.first?.let { skill ->
        CastView(skill.code, 0.0, skill.cooldown / fighter.body.recovery, readiness(fighter, skill))
    }
    val cast = (casting ?: ready?.takeIf { it.state != CastState.WAITING } ?: coming ?: ready)?.takeIf { fighter.alive }
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

/** Почему готовое умение [skill] бойца [me] не применяется: те же условия, что у хода умений монстра ([monsterCast]). */
private fun Battle.readiness(me: Battle.Fighter, skill: MonsterSkill): CastState = when {
    me.ailments.any { it.ailment == Ailment.FROZEN && it.until > time } -> CastState.FROZEN
    me.held -> CastState.STUNNED
    me.intimidated && !me.chief -> CastState.SILENCED
    !wanted(me, skill) -> CastState.WAITING
    else -> CastState.COUNTDOWN
}

/** Полоса стража Разлома: печати, Законы, украденное - поверх полосы босса. */
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
 * Здоровье на экране: у живого - вверх до целого и не меньше 1, у павшего - 0. «0» у живого противника читалось
 * как смерть, хотя у него оставалась доля единицы. Не выше показанного максимума [max] ([shownMax]) - полный запас
 * с дробным максимумом читался «776 / 775»; журнал боя максимума не знает и его не ставит. [falling] - здоровье тает (на карте
 * под недугом или дегеном): вниз до целого, число не обещает того, чего уже нет.
 */
fun shownLife(life: Double, alive: Boolean, max: Double = Double.POSITIVE_INFINITY, falling: Boolean = false): Int = if (alive) (if (falling) kotlin.math.floor(life) else kotlin.math.ceil(life)).toInt().coerceAtMost(shownMax(max)).coerceAtLeast(1) else 0

/** Максимум запаса на экране: одно округление для всех полос, с ним сверяется [shownLife]. */
fun shownMax(max: Double): Int = max.roundToInt()
