package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import com.sperance.exileforge.core.campaign.FightPulse
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FoeView

/**
 * Кадр боя для рисования: часы арены и пульс боя ([FightPulse]). Всё здесь читается только в рисовании (`drawBehind`, `Canvas`,
 * `graphicsLayer`): чтение подписывает рисунок на кадр, а композицию кадр не трогает - она идёт со снимком боя ([FightHud]),
 * что выходит реже кадра. Замахи - из пульса; сцены ударов, выпадов и применения, числа урона кадр двигает от секунд снимка к
 * часам пульса ([now]). Без пульса (превью) - как в снимке.
 */
@Stable
internal class FightFrame(private val clock: State<Float>, private val pulse: FightPulse?) {
    /** Секунды арены: тряска, пульсы, дыхание портретов. */
    val time: Float get() = clock.value

    /** Часы боя этого кадра. */
    fun now(fight: FightHud): Double {
        clock.value
        return pulse?.time ?: fight.time
    }

    /** Доля замаха героя в этом кадре. */
    fun heroSwing(fight: FightHud): Float {
        clock.value
        return pulse?.heroSwing ?: fight.heroSwing
    }

    /** Доля замаха врага [foe] в этом кадре. */
    fun foeSwing(foe: FoeView): Float {
        clock.value
        return pulse?.foeSwing(foe.index) ?: foe.swing
    }
}
