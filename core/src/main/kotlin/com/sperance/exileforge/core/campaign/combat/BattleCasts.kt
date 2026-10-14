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
 * начинается, замах идёт дальше; применённое срабатывает.
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
        note(me, NoteKind.CAST_BROKEN, cast.code)
        return
    }
    if (time + 1e-9 < cast.until) return
    me.casting = null
    cast.finish()
}
