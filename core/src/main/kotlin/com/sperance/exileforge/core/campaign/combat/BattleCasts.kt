package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import kotlin.math.max

// ==================== Применение умений (4.12.x, SKILL-01) ====================

/**
 * Применение умения бойцом: [key] - слот героя или код умения монстра, [code] - умение, с секунды [from] до [until]; [finish] -
 * что умение делает, когда применено (мана, перезарядка, действие).
 */
internal class Casting(val key: String, val code: String, val from: Double, val until: Double, val finish: () -> Unit) {
    /** Доля пройденного применения к секунде [time]. */
    fun progress(time: Double): Double = if (until > from) ((time - from) / (until - from)).coerceIn(0.0, 1.0) else 1.0
}

/**
 * Боец [me] начинает применять умение [code] (слот [key]) на [seconds] секунд: удар и прочие умения ждут - замах встаёт на это время.
 * Мгновенное (ноль секунд) срабатывает сразу.
 */
internal fun Battle.beginCast(me: Fighter, key: String, code: String, seconds: Double, finish: () -> Unit) {
    if (seconds <= 0) {
        finish()
        return
    }
    me.casting = Casting(key, code, time, time + seconds, finish)
    me.nextAttack = max(me.nextAttack, time) + seconds
    note(me, NoteKind.CAST, code, seconds)
}

/**
 * Ход применения бойца [me]: оглушение и заморозка прерывают его - умение не срабатывает, маны не тратит, перезарядка не
 * начинается, замах идёт дальше; заново его не начать раньше паузы `combat.cast.interruptPause` (0.14.0, ASANA-N01: без вечного
 * круга «начал - прерван»), журнал пишет прерывание с паузой. Применённое срабатывает.
 */
internal fun Battle.castTick(me: Fighter) {
    val cast = me.casting ?: return
    if (!me.alive) {
        me.casting = null
        return
    }
    if (me.held) {
        me.casting = null
        me.nextAttack -= (cast.until - time).coerceAtLeast(0.0)
        val pause = rules.cast.interruptPause
        me.readyAt[cast.key] = max(me.readyAt[cast.key] ?: 0.0, time + pause)
        note(me, NoteKind.CAST_BROKEN, cast.code, pause)
        return
    }
    if (time + 1e-9 < cast.until) return
    me.casting = null
    cast.finish()
}

/**
 * Выжигание маны по монстру (0.14.0, ASANA-N01): маны у умений монстров нет - удар с выжиганием [burn] процентов замедляет перезарядку
 * умений цели (`combat.cast`: сильнейшее замедление, срок - от последнего удара).
 */
internal fun Battle.burnSkills(target: Fighter, burn: Double) {
    val rule = rules.cast
    target.burnFactor = max(if (target.burnUntil > time) target.burnFactor else 1.0, rule.burnFactor(burn))
    target.burnUntil = time + rule.burnSeconds
}

/** Перезарядки монстра [foe] под выжиганием за шаг [dt]: идут медленнее во столько раз, сколько велит выжигание. */
internal fun Battle.burnedSkills(foe: Fighter, dt: Double) {
    if (foe.burnUntil <= time || foe.burnFactor <= 1.0) return
    val held = dt * (1 - 1 / foe.burnFactor)
    foe.readyAt.replaceAll { _, at -> at + held }
}
