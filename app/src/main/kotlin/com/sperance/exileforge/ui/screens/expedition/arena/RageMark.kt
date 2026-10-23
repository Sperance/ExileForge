package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.ui.text.TextStyle
import com.sperance.exileforge.core.campaign.run.RageView
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import kotlin.math.ceil

/** Цифры одной ширины: строка ярости не прыгает, пока тикают секунды. */
internal val TabularDigits = TextStyle(fontFeatureSettings = "tnum")

/** Ярость боя одной строкой: до первой ступени - отсчёт до неё, потом - ступень, прибавка урона врагов и отсчёт до следующей. */
internal fun rageText(rage: RageView): String {
    val next = rage.next
    return when {
        rage.stage <= 0 -> ui("fight.rage_in", ceil(next ?: 0.0).toInt())
        next == null -> ui("fight.rage_full", rage.stage, number(rage.damage))
        else -> ui("fight.rage", rage.stage, number(rage.damage), ceil(next).toInt())
    }
}
