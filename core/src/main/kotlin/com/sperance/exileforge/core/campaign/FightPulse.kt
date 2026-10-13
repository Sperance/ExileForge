package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.SkillView
import com.sperance.exileforge.core.campaign.combat.StrikeView
import com.sperance.exileforge.core.campaign.run.AilmentView
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FloatingHit
import com.sperance.exileforge.core.campaign.run.HeroCastView
import com.sperance.exileforge.core.campaign.run.LungeView

/**
 * Пульс боя на кадр: часы боя и замахи бойцов - то, что экран рисует каждый кадр. Снимок экрана ([FightHud]) выходит реже кадра
 * ([HudCadence]); сцены ударов, выпадов, применения, числа урона и доли недугов и перезарядок кадр двигает от секунд снимка
 * ([FightHud.time]) к часам пульса ([time]) - они линейны во времени боя ([FightHud.ahead]). Пишет хозяин боя раз на шаг, читает
 * экран в рисовании; без выделений, только главный поток.
 */
class FightPulse {
    /** Секунды боя этого кадра; без боя - те, на которых он кончился. */
    var time = 0.0
        private set

    /** Доля замаха героя. */
    var heroSwing = 0f
        private set

    private var swings = FloatArray(SWINGS)

    /** Доля замаха врага [index] (номер в паке боя); вне пака - 0. */
    fun foeSwing(index: Int): Float = if (index in swings.indices) swings[index] else 0f

    /** Кадр боя [battle]: его часы и замахи; без боя пульс стоит. */
    fun capture(battle: Battle?) {
        battle ?: return
        time = battle.time
        heroSwing = battle.swing(battle.heroFighter)
        val foes = battle.foeFighters
        if (foes.size > swings.size) swings = FloatArray(foes.size)
        for (i in foes.indices) swings[i] = battle.swing(foes[i])
    }

    private companion object {
        const val SWINGS = 16
    }
}

/**
 * Секунды боя, на которые кадр [now] ушёл от снимка: не меньше нуля (новый бой начинается с нуля) и не больше [AHEAD_CAP] - снимок
 * выходит не реже двадцати раз в секунду, а с ускорением боя его секунды бегут быстрее.
 */
fun FightHud.ahead(now: Double): Double = (now - time).coerceIn(0.0, AHEAD_CAP)

/** Доля выпада к кадру [now]: от 0 до 1, за единицей выпад кончился. */
fun LungeView.progressAt(hud: FightHud, now: Double): Float = if (hud.beat > 0) progress + (hud.ahead(now) / (2 * hud.beat)).toFloat() else progress

/** Доля сцены удара к кадру [now]: за единицей сцена кончилась. */
fun StrikeView.progressAt(hud: FightHud, now: Double): Float = if (hud.beat > 0) progress + (hud.ahead(now) / (STRIKE_LUNGES * hud.beat)).toFloat() else progress

/** Доля сцены применения умения к кадру [now], не больше единицы. */
fun HeroCastView.progressAt(hud: FightHud, now: Double): Float =
    (if (hud.beat > 0) progress + (hud.ahead(now) / (CAST_LUNGES * hud.beat)).toFloat() else progress).coerceAtMost(1f)

/** Возраст числа урона к кадру [now], секунды. */
fun FloatingHit.ageAt(hud: FightHud, now: Double): Double = age + hud.ahead(now)

/** Доля времени недуга, что осталась к кадру [now]. */
fun AilmentView.leftAt(hud: FightHud, now: Double): Float =
    if (seconds > 0) (left * (1 - hud.ahead(now) / seconds)).toFloat().coerceIn(0f, 1f) else left

/** Готовность умения к кадру [now]: перезарядка идёт и между снимками. */
fun SkillView.readyAt(hud: FightHud, now: Double): Float =
    if (ready < 1f && seconds > 0) (ready + hud.ahead(now) * (1 - ready) / seconds).toFloat().coerceIn(0f, 1f) else ready

/** Предел сдвига кадра от снимка, секунды боя. */
private const val AHEAD_CAP = .5

/**
 * Когда выпускать снимок экрана забега или испытания: сразу - по команде, со сменой фазы или боя, с новой строкой журнала боя, когда
 * строка журнала дошла до своего мига (удар лёг, выпад начался) или бой кончился; иначе - не чаще [EVERY] секунд. Снимок - только
 * для экрана: частота его выпуска на бой и заход не влияет.
 */
class HudCadence {
    private var since = Double.MAX_VALUE
    private var key = Long.MIN_VALUE

    /**
     * Пора ли выпустить снимок после шага [dt]; [forced] - была команда, [state] - ключ того, что хозяин меняет скачком (фаза,
     * печать выхода, предложенный объект, убийства), [battle] - бой.
     */
    fun due(dt: Double, forced: Boolean, state: Long, battle: Battle?): Boolean {
        since += dt
        val now = keyOf(state, battle)
        if (!forced && now == key && since < EVERY) return false
        key = now
        since = 0.0
        return true
    }

    private fun keyOf(state: Long, battle: Battle?): Long {
        var key = state
        battle ?: return key
        val log = battle.events
        // Строки журнала, что уже легли, и те, чей выпад уже идёт (журнал пишет удар наперёд)
        var landed = log.size
        while (landed > 0 && log[landed - 1].time > battle.time) landed--
        var lunging = log.size
        while (lunging > 0 && log[lunging - 1].time > battle.time + battle.rules.lunge) lunging--
        key = key * PRIME + System.identityHashCode(battle)
        key = key * PRIME + log.size
        key = key * PRIME + landed
        key = key * PRIME + lunging
        key = key * PRIME + (battle.outcome?.ordinal ?: -1)
        return key
    }

    companion object {
        /** Снимок не реже двадцати раз в секунду. */
        private const val EVERY = .05
        private const val PRIME = 1_000_003L

        /** Ключ скачков хозяина из чисел [parts]. */
        fun key(vararg parts: Long): Long = parts.fold(0L) { key, part -> key * PRIME + part }
    }
}
