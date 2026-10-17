package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.core.i18n.ui
import kotlin.math.abs
import kotlin.math.roundToInt

/** Точность величины в секунду: целое (здоровье в секунду), десятая у малых ([fineNumber]), сотые (атак в секунду - [preciseNumber]). */
enum class RatePrecision {
    WHOLE,
    FINE,
    PRECISE,
    ;

    /** Число так, как его напечатают: по нему решается, что оно округлилось в ноль. */
    fun round(value: Double): Double = when (this) {
        WHOLE -> Math.round(value).toDouble()
        FINE -> if (abs(value) >= FINE_WHOLE) Math.round(value).toDouble() else Math.round(value * TENTHS) / TENTHS
        PRECISE -> Math.round(value * HUNDREDTHS) / HUNDREDTHS
    }

    /** Модуль числа общим форматтером этой точности. */
    fun format(value: Double): String = when (this) {
        WHOLE -> Math.round(value).toString()
        FINE -> fineNumber(value)
        PRECISE -> preciseNumber(value)
    }

    private companion object {
        /** С этого числа [fineNumber] печатает целое. */
        const val FINE_WHOLE = 10.0
        const val TENTHS = 10.0
        const val HUNDREDTHS = 100.0
    }
}

/**
 * Величина в секунду - один формат игры: «12/с» (ключ `unit.per_second`) над общим форматтером чисел. Со знаком - «−12/с», «+8/с»;
 * округлилось в ноль - покой «±0/с» ([still]).
 */
object Rate {
    fun perSecond(value: Double, signed: Boolean = false, precision: RatePrecision = RatePrecision.FINE): String = of(figure(value, signed, precision))

    /** Число без единицы: для узкой плитки, где единица - строкой ниже ([unit]). */
    fun figure(value: Double, signed: Boolean = false, precision: RatePrecision = RatePrecision.FINE): String = when {
        !signed -> precision.format(value)
        still(value, precision) -> STILL
        else -> signedNumber(precision.round(value), precision::format)
    }

    /** Готовое число [figure] (с процентом или коротким числом) с единицей в секунду: «5%/с», «12.5k/с». */
    fun of(figure: String): String = ui("unit.per_second", figure)

    /** Единица отдельно, без числа. */
    val unit: String get() = of("")

    /** Округлилось в ноль при точности [precision] - покой: ни знака, ни цвета убыли или прибыли. */
    fun still(value: Double, precision: RatePrecision = RatePrecision.WHOLE): Boolean = precision.round(value) == 0.0

    private const val STILL = "±0"
}

/** Тон итога здоровья в секунду: убыль, прибыль или покой - то же округление, что у числа ([Rate.still]). */
enum class FlowTone {
    LOSS,
    GAIN,
    STILL,
    ;

    companion object {
        fun of(net: Double): FlowTone = when {
            Rate.still(net) -> STILL
            net < 0 -> LOSS
            else -> GAIN
        }
    }
}

/** Итог здоровья в секунду со знаком и единицей: «−12/с», «+8/с», «±0/с». */
fun flowRate(net: Double): String = Rate.perSecond(net, signed = true, precision = RatePrecision.WHOLE)

/** Итог здоровья в секунду числом без единицы - для плитки, где «/с» строкой ниже. */
fun flowFigure(net: Double): String = Rate.figure(net, signed = true, precision = RatePrecision.WHOLE)

/**
 * Из чего сложился итог: «реген +8 · отдых вне боя +8 · деген 2% · отложенный урон −14 · недуги −34» (отдых и недуги - на карте,
 * сумма частей сходится с итогом); без источников - пусто.
 */
fun flowParts(flow: LifeFlow): String = listOfNotNull(
    ui("fight.flow.regen", (flow.regen - flow.rest).roundToInt()).takeIf { flow.regen - flow.rest >= 1 },
    ui("fight.flow.rest", flow.rest.roundToInt()).takeIf { flow.rest >= 1 },
    ui("fight.flow.degen", fineNumber(flow.degenShare)).takeIf { flow.degen > 0 },
    ui("fight.flow.delayed", flow.delayed.roundToInt()).takeIf { flow.delayed > 0 },
    ui("fight.flow.ailments", flow.ailments.roundToInt()).takeIf { flow.ailments >= .5 },
    ui("fight.flow.shielded", flow.shielded.roundToInt()).takeIf { flow.shielded >= .5 },
).joinToString(" · ")

/** Итог здоровья на полосе жизни: «+27/с», под ранящим недугом - «−13/с · кровотечение». */
fun flowLabel(flow: LifeFlow): String = flow.stalledBy?.let { ui("fight.flow.rate_cause", flowRate(flow.net), ui("fight.flow.stalled.${it.name}")) }
    ?: flowRate(flow.net)
