package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.campaign.FlowMeasure
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Число строки сводки характеристики [stat] - то, что строка печатает: процент защиты у брони и уклонения ([defence],
 * [StatLimits.defence]), действующее под пределом ([StatLimits.of]), иначе цифра сводки. [value] - это число, [gathered] - набранное до
 * предела (без предела - [value]), [cap] - предел, null - его нет. Одна точка строки сводки и разбора её сдвига: вклады считаются в
 * той же единице, что строка.
 */
data class RowFigure(val stat: String, val value: Double, val gathered: Double, val cap: Double?, val defence: Boolean) {
    /** Бой читает не набранное: оно выше предела или ниже дна. */
    val clamped: Boolean get() = cap != null && value != gathered

    /** Точность числа строки: сотые у характеристик в дроби ([preciseStats]), иначе как [fineNumber]. */
    private val precise: Boolean get() = !defence && stat in preciseStats

    /** Число [x] в единице строки так, как его печатают: телескоп вкладов сходится с итогом по этим числам. */
    fun shown(x: Double): Double = when {
        precise -> (x * HUNDREDTHS).roundToLong() / HUNDREDTHS
        abs(x) >= WHOLE -> x.roundToLong().toDouble()
        else -> (x * TENTHS).roundToLong() / TENTHS
    }

    /** Число строки текстом, как его печатает сводка; [sheet] - характеристики листа (диапазон урона вида удара). */
    fun text(index: ContentIndex?, sheet: Map<String, Double>): String = if (defence) ui("stat.defence_percent", number(value)) else rowValue(stat, value, index, sheet)

    /** Сдвиг [delta] числа строки со знаком и единицей строки; [delta] - разность показанных чисел ([shown]), печатается без потерь. */
    fun deltaText(delta: Double, index: ContentIndex?): String {
        val signed = signedNumber(delta, ::exactNumber)
        val flow = if (defence) null else FlowMeasure.of(stat)
        return when {
            flow != null -> ui(flow.key, signed)
            defence || statPercent(stat, index) || index?.stats?.get(stat)?.multiplier == true -> "$signed%"
            else -> signed
        }
    }

    /** Разность показанных чисел: целое без дробной части, иначе столько знаков, сколько у строки. */
    private fun exactNumber(x: Double): String = when {
        abs(x - x.roundToLong()) < EPSILON -> x.roundToLong().toString()
        precise -> String.format(Locale.ROOT, "%.2f", x).trimEnd('0').removeSuffix(".")
        else -> String.format(Locale.ROOT, "%.1f", x).removeSuffix(".0")
    }

    companion object {
        /** Число строки [stat] сводки [figures] героя уровня [level] (null - процента защиты нет) над контентом [index]. */
        fun of(stat: String, figures: StatFigures, level: Int?, index: ContentIndex): RowFigure {
            level?.let { StatLimits.defence(stat, figures.stats, it, index) }?.let { reading -> return RowFigure(stat, reading.effective, reading.effective, null, defence = true) }
            val own = figures.values[stat] ?: 0.0
            val limit = StatLimits.of(stat, figures.stats, index.campaign) ?: return RowFigure(stat, own, own, null, defence = false)
            return RowFigure(stat, limit.effective, limit.gathered, limit.cap, defence = false)
        }

        private const val WHOLE = 10.0
        private const val TENTHS = 10.0
        private const val HUNDREDTHS = 100.0
        private const val EPSILON = 1e-6
    }
}
