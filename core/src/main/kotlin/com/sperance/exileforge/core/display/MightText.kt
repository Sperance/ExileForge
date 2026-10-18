package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.character.MightShift
import com.sperance.exileforge.core.character.MightStanding
import com.sperance.exileforge.core.i18n.ui
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Мощь целиком: «12 480» - печать листа героя, карточка врага, «было → стало» вещи. */
fun mightText(value: Double): String = groupedNumber(value.roundToLong())

/** Мощь строкой списка: «⚜ 12.5k» - карточка игрока, состав гильдии, стая. */
fun mightLine(value: Double): String = ui("might.short", compactNumber(value))

/**
 * Короткое число для тесных строк: до [THOUSAND_SHORT] - целиком, тысячи - «12.5k», миллионы - «1.2M»; одна цифра после точки,
 * без «.0».
 */
fun compactNumber(value: Double): String = when {
    abs(value) >= MILLION -> tenths(value / MILLION) + "M"
    abs(value) >= THOUSAND_SHORT -> tenths(value / THOUSAND) + "k"
    else -> value.roundToLong().toString()
}

/** Как враг стоит против героя словами: «сильнее на 27%», «слабее на 60%», «сопоставим». */
fun mightStandingText(foe: Double, hero: Double): String {
    val percent = if (hero > 0) ((foe / hero - 1) * 100).roundToInt() else 0
    return when (MightStanding.of(foe, hero)) {
        MightStanding.STRONGER -> ui("might.stronger", percent)
        MightStanding.WEAKER -> ui("might.weaker", -percent)
        MightStanding.MATCHED -> ui("might.matched")
    }
}

/** Разница Мощи вещи процентом со знаком: «+5.1%». */
fun mightChange(shift: MightShift): String = factorChange(1 + shift.change)

private fun tenths(value: Double): String = String.format(java.util.Locale.ROOT, "%.1f", value).removeSuffix(".0")

private const val THOUSAND = 1_000.0
private const val THOUSAND_SHORT = 10_000.0
private const val MILLION = 1_000_000.0
