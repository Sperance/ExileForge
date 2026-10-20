package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.FactorKey
import com.sperance.exileforge.core.campaign.FactorTrace
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.PowerMoment
import com.sperance.exileforge.core.campaign.StatLines
import com.sperance.exileforge.core.campaign.TickTrace
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.TICK
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.BuffLayer
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.Condition
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.PowerEvent
import com.sperance.exileforge.rules.content.RecoveryPool
import com.sperance.exileforge.rules.content.SkillEvent
import com.sperance.exileforge.rules.sheet.DamageGrowth
import kotlin.math.max
import kotlin.math.min
import com.sperance.exileforge.rules.sheet.Recovery as RecoveryShare

/**
 * Глоток идёт и в оглушении, умение ждёт, пока герой может двигаться (засада Разлома - и то и другое позже). Применение прерывают оглушение
 * и заморозка, его конец - срабатывание; спровоцированный умений не применяет, босс - с медленной перезарядкой, выжигание маны - тоже
 * медленная перезарядка. Первым ходит тот, чей срок раньше, в одном срезе могут несколько; рывок силы этого замаха не стирается интервалом.
 * Без предела времени и без отступления: бой идёт, пока не падёт одна из сторон.
 */
internal fun Battle.step(dt: Double) {
    time += dt
    lookLife()
    everyone.forEach {
        regenerate(it, dt)
        degenerate(it, dt)
        burn(it, dt)
    }
    lifeFlowLine()
    marksTick()
    expire()
    fateTicks(dt)
    if (finished()) return
    rage()
    riftTick()
    powers.tick()
    if (finished()) return
    val hero = heroFighter
    if (hero.alive && joined()) {
        if (!autoDrunk) {
            autoDrunk = true
            if (hero.body.flasksAuto) {
                kit.flasks.forEachIndexed { i, flask ->
                    if (flask != null && draughtOf(i) == null && waste(flask).isEmpty()) {
                        flaskOpened[i] = true
                        drink(i, flask, free = true)
                    }
                }
            }
        }
        useFlasks()
        castTick(hero)
        if (!hero.held && outcome == null) useSkills()
    }
    taps.clear()
    drinks.clear()
    foeFighters.forEach { foe ->
        if (!foe.alive || outcome != null) return@forEach
        castTick(foe)
        val silenced = provokedSkills(foe, dt)
        burnedSkills(foe, dt)
        if (!foe.held && !silenced) monsterCast(foe)
    }
    if (finished()) return
    val order = dueOrder()
    for (k in order.indices) {
        val me = order[k]
        if (!me.alive || me.held || me.casting != null || me.nextAttack > time) continue
        val target = if (me.side == Side.HERO) target() else heroFighter
        me.rushed = false
        if (target != null) strike(me, target, swingOf(me))
        me.nextAttack = if (me.rushed) time else time + me.attackInterval * me.slow() * cadence(me)
        me.rushed = false
        if (finished()) return
    }
    watch()
}

/**
 * Очередь хода шага: [Battle.everyone], устойчиво упорядоченные по `nextAttack` - тот же порядок, что `sortedBy`,
 * в буфере боя вставками, без новых списков на шаг. Порядок равных - исходный (герой, враги): буфер каждый шаг
 * заполняется заново, а не досортировывается от прошлого шага.
 */
private fun Battle.dueOrder(): List<Fighter> {
    val order = turnOrder
    order.clear()
    for (i in everyone.indices) order.add(everyone[i])
    for (i in 1 until order.size) {
        val fighter = order[i]
        var j = i - 1
        while (j >= 0 && order[j].nextAttack.compareTo(fighter.nextAttack) > 0) {
            order[j + 1] = order[j]
            j--
        }
        order[j + 1] = fighter
    }
    return order
}

/**
 * Шкалы тают, когда удар не наполнял их дольше задержки правила. Доля восстановления ([Fighter.share]) - «меньше регенерации», запрет листа
 * и изранение, иссушение - мане. Поток здоровья - одна функция правил со сводкой, картой и Мощью: пополнение - регенерация листа и из маны
 * под долей и недугами, потери и деген - в [degenerate]. Регенерация не переливается (Переполнение копит только избыток восстановления); у
 * возврата здоровья монстра своей полки журнала нет.
 */
private fun Battle.regenerate(me: Fighter, dt: Double) {
    if (!me.alive) return
    rules.buildup?.let { rule -> if (time - me.builtAt >= rule.decayDelay) for (i in me.buildup.indices) me.buildup[i] = max(0.0, me.buildup[i] - rule.decayPerSecond * dt) }
    val manaCurb = me.curb(RecoveryPool.MANA)
    val life = RecoveryShare.lifeFlow(me.body, rules, me.curb(RecoveryPool.LIFE), manaCurb)
    val regenerated = lifeBack(me, recovery(me, life.gain * dt))
    if (me === heroFighter) lifeMeter.regen(regenerated)
    if (!shieldless(me)) me.shield = EnergyShield.recovered(me.body, rules, me.shield, time - me.lastHit, dt)
    if (!manaless(me)) manaBack(me, RecoveryShare.manaFlow(me.body, rules.mana, manaCurb).total * dt, spill = false)
    me.recouping.forEach { recoup -> lifeBack(me, recoup.life * min(dt, recoup.until - (time - dt)).coerceAtLeast(0.0)) }
    if (me === heroFighter) {
        recoveries.forEach { draught ->
            val slice = min(dt, draught.until - (time - dt)).coerceAtLeast(0.0)
            val gain = draught.life * slice
            val restored = lifeBack(me, gain, overheal = draught.slot >= 0)
            healedInto(me, restored, draught.toMana, draught.toShield)
            draught.restored += restored
            draught.wasted += gain - restored
            manaBack(me, draught.mana * slice)
            if (draught.stopsAtFull && me.life >= me.body.maxLife) stopDraught(draught)
        }
    }
}

/**
 * Итог здоровья героя раз в секунду: одна строка «регенерация минус деген и отложенный урон»: деген не
 * молчит; секунда, где ничего не текло, строки не пишет. Итог секунды виден экрану ([Battle.lifeFlow]).
 */
private fun Battle.lifeFlowLine() {
    val hero = heroFighter
    val flow = lifeMeter.due(time, hero.body.lifeDegenShare) ?: return
    lifeFlow = flow
    if (flow.flowing) note(hero, NoteKind.LIFE_FLOW, "", flow.net, flow)
}

/**
 * A pure life draught ends at full life: its buff goes with it, so the belt may drink it again when life falls. Журнал пишет
 * «прервано: здоровье полное» с оставшимися секундами, полка держит плитку прерванного глотка [Halt.SHOWN] секунд ([Battle.halts]).
 */
private fun Battle.stopDraught(draught: Recovery) {
    val code = kit.flasks.getOrNull(draught.slot)?.sheet?.code
    if (code != null) {
        note(heroFighter, NoteKind.RECOVER_HALT, code, (draught.until - time).coerceAtLeast(0.0))
        halts.removeAll { it.slot == draught.slot || time - it.at >= Halt.SHOWN }
        halts += Halt(draught.slot, code, time)
    }
    draught.until = time
    draughtOf(draught.slot)?.let { running -> heroFighter.effects[heroFighter.effects.indexOf(running)] = running.copy(until = time) }
}

/** The recovery shelf's line for a draught or a recoup that ran out: what it gave back, and what spilled over a full bar. */
private fun Battle.logRecovery(recovery: Recovery) {
    val code = kit.flasks.getOrNull(recovery.slot)?.sheet?.code
    if (recovery.restored >= 1) note(heroFighter, if (code != null) NoteKind.RECOVER_FLASK else NoteKind.RECOVER_RECOUP, code.orEmpty(), recovery.restored)
    if (recovery.wasted >= 1) note(heroFighter, NoteKind.RECOVER_WASTE, code.orEmpty(), recovery.wasted)
}

/** What ran out this slice goes: buffs, curses, draughts, a barrier, and the body is made again without them. */
private fun Battle.expire() {
    everyone.forEach { fighter ->
        if (fighter.barrier > 0 && fighter.barrierUntil <= time) fighter.barrier = 0.0
        if (fighter.effects.removeAll { it.until <= time }) remake(fighter)
        fighter.recouping.removeAll { it.until <= time }
    }
    recoveries.removeAll { recovery -> (recovery.until <= time).also { if (it) logRecovery(recovery) } }
    if (heroCharges.expire(time)) remake(heroFighter)
}

/**
 * Edges the hero's passives answer — low life, a broken shield — and the low-life lines of either side.
 *
 * Бонусы низкого здоровья пассивных - ниже своей доли правил. Каждое пришедшее и ушедшее условие - заметка журнала.
 */
internal fun Battle.watch() {
    val hero = heroFighter
    if (hero.alive) {
        val below = hero.life < hero.body.maxLife * rules.lowLife
        if (below && !belowLow) {
            trigger(SkillEvent.LOW_LIFE)
            powers.fire(PowerEvent.LOW_LIFE)
        }
        belowLow = below
        if (hero.body.maxShield > 0) {
            val up = hero.shield > 0.5
            if (!up && shieldUp) {
                trigger(SkillEvent.SHIELD_BROKEN)
                powers.fire(PowerEvent.SHIELD_BROKEN)
            }
            shieldUp = up
        }
        val low = hero.life < hero.body.maxLife * rules.passiveLowLife
        if (low != hero.low && model.lowLife.isNotEmpty()) {
            hero.low = low
            remake(hero)
        }
        if (powers.restand()) remake(hero)
        if (conditioned) {
            heroConditions().let { now ->
                if (now != conditions) {
                    val was = conditions
                    conditions = now
                    remake(hero)
                    (now - was).forEach { note(hero, NoteKind.CONDITION_ON, it.name) }
                    (was - now).forEach { note(hero, NoteKind.CONDITION_OFF, it.name) }
                }
            }
        }
    }
    traitsStand()
    foeFighters.forEach { foe -> if (foe.alive) phase(foe) }
    if (slotHolders.isNotEmpty()) totemTick()
    foeFighters.forEach { foe ->
        val low = foe.alive && foe.life < foe.body.maxLife * rules.passiveLowLife
        if (low != foe.low && foe.model.body(emptyList())[CoreStat.LOW_LIFE_SPEED.code] > 0) {
            foe.low = low
            remake(foe)
        }
    }
}

/** [fighter]'s body made again from its sheet with what lies on it — and the hero's under the auras of the foes still standing. */
internal fun Battle.remake(fighter: Fighter) {
    if (fighter === heroFighter) {
        fighter.rebody(heroBody(emptyList()))
    } else {
        fighter.rebody(fighter.model.body(foeLines(fighter)))
    }
}

/** Что лежит на враге [fighter] сейчас: строки эффектов и прибавка скорости при низком здоровье. */
internal fun Battle.foeLines(fighter: Fighter): List<StatLine> {
    val speed = fighter.model.body(emptyList())[CoreStat.LOW_LIFE_SPEED.code]
    return fighter.actingEffects.flatMap { it.lines } + if (fighter.low && speed > 0) listOf(StatLine(CoreStat.ATTACK_SPEED.code, Op.INCREASED, speed)) else emptyList()
}

/** The hero's body for one blow: what lies on them and [extra], a skill's own lines. */
internal fun Battle.heroBody(extra: List<StatLine>): Combatant = model.body(heroLines() + extra).under(auras())

/** What lies on the hero now: low life's lines, the standing powers', the charges', the conditions' and the effects'. */
private fun Battle.heroLines(): List<StatLine> = (if (heroFighter.low) model.lowLife else emptyList()) +
    powers.standing + heroCharges.lines() + chargeLines() + model.conditional(conditions) + heroFighter.actingEffects.flatMap { it.holding(conditions) }

/**
 * The increases of [type] a hero's blow is grown by now, in percent: the sheet's own, the damage in
 * general among them, and those of the lines on the hero and [extra] — the conditions', the charges', a flask's.
 */
internal fun Battle.heroIncrease(type: DamageType, extra: List<StatLine> = emptyList()): Double = heroGrowth(extra).increased(type.attack)

/**
 * Рост урона героя сейчас: увеличения и «больше» каждого вида листа со строками на герое и [extra]. Удар оружием несёт
 * «больше» в числе листа; чары и урон во времени растут на него отдельно.
 */
internal fun Battle.heroGrowth(extra: List<StatLine> = emptyList()): DamageGrowth = model.growth(heroLines() + extra)

/** How much a hero's [blow] grows by [facing] percent increased beside its own increases, over all its damage. */
internal fun Battle.versus(blow: Blow, facing: Double): Double {
    val total = blow.damage.values.sum()
    if (facing == 0.0 || total <= 0) return 1.0
    return blow.damage.entries.sumOf { (type, value) -> regrow(value, blow.increase?.get(type) ?: heroIncrease(type), facing) } / total
}

/** The hero's damage for the charges held: so many percent increased for each of a kind, by the sheet — beside the other increases. */
internal fun Battle.chargeLines(): List<StatLine> = ChargeKind.REAL.mapNotNull { kind ->
    val per = heroFighter.body.perCharge(kind)
    val count = heroCharges.count(kind)
    if (per == 0.0 || count == 0) null else StatLine(StatLines.DAMAGE, Op.INCREASED, per * count)
}

/**
 * What holds for the hero now: the states their conditional lines wait for.
 *
 * Вечный бафф держит своё условие весь бой: строки героя «пока под баффом» - целиком.
 */
internal fun Battle.heroConditions(): Set<Condition> {
    val hero = heroFighter
    val body = hero.body
    fun recent(at: Double) = time - at <= rules.recentSeconds
    return buildSet {
        if (hero.life < body.maxLife * rules.lowLife) add(Condition.LOW_LIFE)
        if (hero.life >= body.maxLife - 0.5) add(Condition.FULL_LIFE)
        if (body.maxShield > 0 && hero.shield >= body.maxShield - 0.5) add(Condition.FULL_SHIELD)
        if (recent(killedAt)) add(Condition.RECENT_KILL)
        if (recent(hero.lastHit)) add(Condition.RECENT_HIT_TAKEN)
        if (recent(blockedAt)) add(Condition.RECENT_BLOCK)
        if (recent(critAt)) add(Condition.RECENT_CRIT)
        BuffKind.entries.forEach { kind -> kind.condition?.let { if (hero.buffed(kind)) add(it) } }
        if (heroCharges.count(ChargeKind.FRENZY) > 0) add(Condition.FRENZY_CHARGE)
        if (heroCharges.count(ChargeKind.POWER) > 0) add(Condition.POWER_CHARGE)
        if (heroCharges.count(ChargeKind.ENDURANCE) > 0) add(Condition.ENDURANCE_CHARGE)
        if (hero.effects.any { it.kind == EffectKind.FLASK && it.until > time }) add(Condition.FLASK_ACTIVE)
    }
}

/** How the hero's lines waiting for a target's state see [target]. */
internal fun Battle.states(target: Fighter): Set<Condition> {
    if (target.side != Side.MONSTER) return emptySet()
    val rarity = foes[target.index].rarity
    return buildSet {
        if (rarity >= MonsterRarity.RARE) add(Condition.VS_RARE)
        if (rarity == MonsterRarity.UNIQUE) add(Condition.VS_UNIQUE)
        if (foes[target.index].tainted) add(Condition.VS_BLIGHTED)
        if (target.life >= target.body.maxLife - 0.5) add(Condition.VS_FULL_LIFE)
    }
}

/** [kind] laid on [fighter] for the rule's time, longer by its buff duration. */
private fun Battle.gainBuff(fighter: Fighter, kind: BuffKind) {
    val rule = rules.buffs[kind]
    val stack = rule.stack(kind, BuffLayer.TIMED, hero = fighter.side == Side.HERO)
    val duration = rule.duration * fighter.body.buffDuration
    lay(fighter, TimedEffect(EffectKind.BUFF, kind.source, rule.lines(stack.share), time + duration, duration, buff = stack))
    note(fighter, NoteKind.BUFF, kind.name, duration)
}

/** The hero's chance at [kind]: drawn only when there is one, so a sheet without it plays the same seed as before. */
internal fun Battle.chanceBuff(kind: BuffKind) {
    val chance = heroFighter.body.buffChance(kind)
    if (chance > 0 && random.nextDouble() < chance) gainBuff(heroFighter, kind)
}

/**
 * The buffs [fighter] wears for the whole fight: вечный слой ([BuffLayer.STANDING]) на силе правила
 * ([com.sperance.exileforge.rules.content.BuffRule.share]) - у героя долей `combat.buffs.<вид>.always`, у монстра целиком.
 */
internal fun Battle.wear(fighter: Fighter) = BuffKind.entries.filter { fighter.body.wears(it) }.forEach { kind ->
    val rule = rules.buffs[kind]
    val stack = rule.stack(kind, BuffLayer.STANDING, hero = fighter.side == Side.HERO)
    lay(fighter, TimedEffect(EffectKind.BUFF, kind.standing, rule.lines(stack.share), FOREVER, FOREVER, buff = stack))
}

/** What of a hit on [target] its spell suppression or deflection lets through; drawn only for a target with a chance. */
internal fun Battle.eased(target: Fighter, blow: Blow): Double {
    val rule = rules.defence
    val chance = if (blow.spell) target.body.suppression else target.body.deflection
    if (chance <= 0 || draw() >= chance) return 1.0
    return rule.eased(blow.spell)
}

/**
 * Degeneration eats life: a share of the maximum a second, past the shield, and it can kill.
 *
 * Щит тает, отложенная доля ударов по герою приходит в срок. Отрицательная регенерация листа - потеря здоровья в секунду, как деген
 * ([RecoveryShare.lifeLoss]).
 */
private fun Battle.degenerate(me: Fighter, dt: Double) {
    if (!me.alive || me.invulnerable) return
    if (me.body.shieldDegenShare > 0) me.shield = max(0.0, me.shield - me.body.maxShield * me.body.shieldDegenShare * dt)
    val hero = me === heroFighter
    if (hero && delayed.isNotEmpty()) {
        val before = me.life
        delayed.forEach { me.life = max(0.0, me.life - it.rate * min(dt, it.until - (time - dt)).coerceAtLeast(0.0)) }
        delayed.removeAll { it.until <= time }
        lifeMeter.delayed(before - me.life)
    }
    val share = me.body.lifeDegenShare
    val loss = RecoveryShare.lifeLoss(me.body)
    if (share > 0 || loss > 0) {
        val before = me.life
        me.life = max(0.0, me.life - (max(me.body.maxLife * share, if (share > 0) rules.minDot / TICK else 0.0) + loss) * dt)
        if (hero) lifeMeter.degen(before - me.life)
    }
    if (!me.alive) fell(me)
}

/**
 * Ailments run their course: damage over time is applied every slice and logged once a second. Тикают только
 * действующие стаки ([Fighter.acting]: предел яда), основа до защит ([AilmentRule.raw]) встречает сопротивление и «получаемый урон»
 * цели на каждом тике ([Fighter.dotTaken]), Разложение растит тик своего вида; строка журнала - на недуг и умение, что его наложило.
 *
 * Убийство - за недугом, что нанёс больше всех в этом срезе. Всё, что наносит урон, наносит не меньше [CombatRules.minDot] за тик
 * ([dotRate] - одна формула с картой), и оборванный тик тоже. Печати Стража и «Одна стихия» Владыки - и здесь; «Каменная кожа» держит
 * секунду недугов по герою под потолком удара. Тик называет свой недуг, его силу и что её растит.
 */
private fun Battle.burn(me: Fighter, dt: Double) {
    if (me.ailments.isEmpty()) return
    val wasAlive = me.alive
    var heaviest: ActiveAilment? = null
    var heaviestSlice = 0.0
    me.ailments.mapTo(LinkedHashSet()) { it.ailment }.filter { it.hurts }.forEach { kind ->
        val taken = dotTaken(me, kind)
        me.acting(kind).forEach { active ->
            val span = min(dt, active.until - (time - dt)).coerceAtLeast(0.0)
            val dealt = dotRate(active.ailment, active.magnitude, me.body, me.weakness(), rules.minDot, taken.of(active)) * span
            val rift = sealed(me) * lawTaken(me, tickType(active))
            val unruled = if (rift == 1.0) dealt else dealt * rift
            if (unruled <= 0 || !me.alive || me.invulnerable) return@forEach
            val type = tickType(active)
            if (type == DamageType.CHAOS && me.body.chaosImmune) return@forEach
            val slice = fateDot(me, foeFighters.getOrNull(active.foe)?.takeIf { active.source == Side.MONSTER }, unruled)
            if (slice <= 0) return@forEach
            wound(me, slice, type)
            me.ticking.merge(Ticking(active.ailment, active.skill), slice, Double::plus)
            if (slice > heaviestSlice) {
                heaviest = active
                heaviestSlice = slice
            }
        }
    }
    val expired = me.ailments.filter { it.until <= time }
    val bySpell = heaviest?.let { it.spell && it.source == Side.HERO } == true
    me.ailments.removeAll(expired)
    val due = me.ticking.keys.mapTo(LinkedHashSet()) { it.ailment }.associateWith { ailment ->
        val since = me.tickedAt[ailment] ?: time.also { me.tickedAt[ailment] = it }
        time - since >= TICK - 1e-9 || expired.any { it.ailment == ailment } || !me.alive
    }
    me.ticking.keys.toList().forEach { key ->
        val ailment = key.ailment
        if (due[ailment] != true) return@forEach
        var amount = me.ticking.remove(key) ?: 0.0
        me.tickedAt[ailment] = time
        if (amount <= 0) return@forEach
        val own = (me.ailments + expired).filter { it.ailment == ailment && it.skill == key.skill }
        val active = own.firstOrNull()
        val type = active?.let { tickType(it) } ?: ruleOf[ailment]?.second
        if (amount < rules.minDot && me.alive && !me.invulnerable) {
            wound(me, rules.minDot - amount, type)
            amount = rules.minDot
        }
        val source = active?.source ?: me.side.other
        val acting = me.acting(ailment).filter { it.skill == key.skill && it.until > time - TICK }.ifEmpty { own.filter { it.until > time - TICK } }
        val base = acting.sumOf { it.magnitude }
        val taken = dotTaken(me, ailment)
        val seals = sealed(me)
        val law = lawTaken(me, type)
        val defence = active?.let { taken.defence(it) } ?: 1.0
        val exposure = active?.let { taken.exposure(it) } ?: 1.0
        val factors = listOfNotNull(
            FactorTrace(FactorKey.BASE, base, listOfNotNull(ailment.damage, CoreStat.DOT_DAMAGE.code, CoreStat.FASTER_AILMENTS.code, CoreStat.AILMENT_DURATION.code, "STOCK_${ailment.word}_DURATION", "STOCK_${ailment.word}_LIMIT")),
            FactorTrace(FactorKey.DEFENCE, defence, target = listOfNotNull(type?.resist)).takeIf { defence != 1.0 },
            FactorTrace(FactorKey.SHOCK, me.weakness(), target = listOf(CoreStat.SHOCK_TAKEN.code)),
            FactorTrace(FactorKey.TAKEN, me.body.dotTaken * me.body.ailmentTaken(ailment), target = listOf(CoreStat.DOT_TAKEN.code, CoreStat.BLEED_TAKEN.code)),
            FactorTrace(FactorKey.DECAY, exposure).takeIf { exposure != 1.0 },
            FactorTrace(FactorKey.SEALS, seals).takeIf { seals != 1.0 },
            FactorTrace(FactorKey.LAW, law).takeIf { law != 1.0 },
            FactorTrace(FactorKey.TOTAL, amount),
        )
        record(
            source, Action.TICK, HitKind.HIT, amount, type, 0.0, false, emptyList(), ailment,
            if (me.side == Side.MONSTER) me.index else active?.foe ?: 0,
            key.skill,
            trace = TickTrace(
                ailment, base * defence * exposure * me.weakness() * me.body.dotTaken * me.body.ailmentTaken(ailment),
                (own.maxOfOrNull { it.until } ?: time) - time, own.maxOfOrNull { it.duration } ?: 0.0, own.size, shot(me), factors, origin,
                (if (source == Side.HERO) heroFighter else foeFighters.getOrNull(active?.foe ?: -1))?.let { shot(it) },
                exposed = amount * (1 - 1 / exposure),
            ),
        )
        if (source == Side.HERO) sealStruck(me, crit = false)
    }
    if (wasAlive && !me.alive) fell(me, bySpell)
}

/** Вид урона тика [active]: хаос, если горит хаосом, иначе - вид его правила. */
private fun Battle.tickType(active: ActiveAilment): DamageType? = if (active.chaos) DamageType.CHAOS else ruleOf[active.ailment]?.second

/**
 * Что режет и растит тик недуга [ailment] на бойце [me]: основа до защит ([AilmentRule.raw]) - сопротивление вида
 * тика (без пробивания) и «получаемый урон» вида; любой тик - уязвимость своего вида (Разложение). Снижение сопротивления позже
 * усиливает и висящие стаки - тик читает лист цели в свой миг.
 */
private fun Battle.dotTaken(me: Fighter, ailment: Ailment): DotTaken = DotTaken(me, ruleOf[ailment]?.first?.raw == true) { tickType(it) }

/** Защиты и уязвимость цели [fighter] к тику: [raw] - основа до защит, [typeOf] - вид урона тика. */
internal class DotTaken(private val fighter: Fighter, private val raw: Boolean, private val typeOf: (ActiveAilment) -> DamageType?) {
    /** Доля тика [active], что проходит сопротивление и «получаемый урон» вида: у основы после защит - целиком. */
    fun defence(active: ActiveAilment): Double {
        val type = typeOf(active) ?: return 1.0
        return if (raw) (1 - fighter.body.resist(type)) * fighter.body.damageTaken(type) else 1.0
    }

    /** Уязвимость к виду тика [active] (Разложение). */
    fun exposure(active: ActiveAilment): Double = typeOf(active)?.let(fighter::exposure) ?: 1.0

    /** Множитель тика [active]: защиты и уязвимость. */
    fun of(active: ActiveAilment): Double = defence(active) * exposure(active)
}

/** Урон [amount] вида [type] по [me] мимо удара (тик, подрыв метки): сперва барьер, затем щит ([EnergyShield]), затем здоровье. */
internal fun Battle.wound(me: Fighter, amount: Double, type: DamageType?) {
    var rest = amount
    if (me.barrier > 0) {
        val soaked = min(me.barrier, rest)
        me.barrier -= soaked
        rest -= soaked
    }
    val absorbed = if (shieldless(me)) 0.0 else EnergyShield.absorbed(me.shield, rest, type)
    me.shield -= absorbed
    me.life = max(0.0, me.life - (rest - absorbed))
}

/** Accuracy against evasion, as in PoE, under the target's ceiling. */
internal fun Battle.evasion(me: Fighter, target: Fighter): Double = rules.accuracy.evaded(me.body.accuracy(rules.accuracy), target.body.evasion).coerceAtMost(target.body.evasionCap)

/** Сила может ответить на гибель героя и поднять его. Подкрепления встают, едва на поле освободилось место, раунд - едва пал последний. */
private fun Battle.finished(): Boolean {
    if (outcome != null) return true
    if (!heroFighter.alive) powers.fire(PowerEvent.DEATH)
    if (heroFighter.alive) muster()
    when {
        !heroFighter.alive -> end(Outcome.LOSS)
        foeFighters.none { it.alive } -> end(Outcome.WIN)
        else -> return false
    }
    return true
}

/**
 * Герой сдаётся: бой кончается поражением, как гибель, - с её ценой в заходе. Выход из боя, что сам не кончается.
 */
fun Battle.surrender() {
    if (outcome != null) return
    heroFighter.life = 0.0
    end(Outcome.LOSS)
}

private fun Battle.end(how: Outcome) {
    lookLife()
    outcome = how
    duration = time
    fateEnds()
}

/** Низшая доля здоровья героя за бой - для Испытания чемпиона. */
private fun Battle.lookLife() {
    val max = heroFighter.body.maxLife
    if (max > 0) trial.lowestLife = min(trial.lowestLife, (heroFighter.life / max).coerceIn(0.0, 1.0))
}
