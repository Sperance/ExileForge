package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.run.RageView
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Ember
import com.sperance.exileforge.ui.theme.Muted
import kotlin.math.ceil

/** Цифры одной ширины: строка ярости не прыгает, пока тикают секунды. */
internal val TabularDigits = TextStyle(fontFeatureSettings = "tnum")

/** Ярость боя (4.3.0) одной строкой: до первой ступени - отсчёт до неё, потом - ступень, прибавка урона врагов и отсчёт до следующей. */
internal fun rageText(rage: RageView): String {
    val next = rage.next
    return when {
        rage.stage <= 0 -> ui("fight.rage_in", ceil(next ?: 0.0).toInt())
        next == null -> ui("fight.rage_full", rage.stage, number(rage.damage))
        else -> ui("fight.rage", rage.stage, number(rage.damage), ceil(next).toInt())
    }
}

/** Компактный знак ярости (4.3.0): в шапке стаи и в полосе стража; пока ступени нет - приглушённый. */
@Composable internal fun RageMark(rage: RageView, modifier: Modifier = Modifier) {
    Text(rageText(rage), modifier, color = if (rage.stage > 0) Ember else Muted, fontSize = 9.sp, style = TabularDigits)
}
