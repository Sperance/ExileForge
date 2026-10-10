package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.character.FlaskMeasure
import com.sperance.exileforge.core.character.FlaskRow
import com.sperance.exileforge.core.character.FlaskVerdict
import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.character.StatPriority
import com.sperance.exileforge.rules.text.ModifierText
import kotlin.math.abs

/**
 * Строка сравнения «было → стало»: имя [title], числа [before] и [after], разница [diff] со знаком в единицах самой строки
 * (у процентной - «+10%»), сдвиг [better] - к лучшему, к худшему или без цвета (null). [rank] и [weight] - важность строки:
 * группа [StatPriority] и относительная величина изменения.
 */
data class ComparisonLine(
    val title: String,
    val before: String,
    val after: String,
    val diff: String,
    val better: Boolean?,
    val rank: Int,
    val weight: Double,
)

/**
 * Сравнение листа «было → стало» (макет «Изменения характеристик», вариант B): строки по важности - группа [StatPriority],
 * затем величина изменения. Больше [LEADING] строк - свёрнутое показывает [leading], остальное ([hidden]) - по кнопке.
 */
class Comparison(lines: List<ComparisonLine>) {
    /** Все строки, важнейшие первыми. */
    val lines: List<ComparisonLine> = lines.sortedWith(compareBy<ComparisonLine> { it.rank }.thenByDescending { it.weight })

    /** Сворачивается ли сравнение: строк больше, чем видно свёрнутым. */
    val foldable: Boolean get() = lines.size > LEADING

    /** Строки, видные в свёрнутом сравнении. */
    val leading: List<ComparisonLine> get() = lines.take(LEADING)

    /** Строки под кнопкой «Показать ещё». */
    val hidden: List<ComparisonLine> get() = lines.drop(LEADING)

    companion object {
        /** Сколько строк видно в свёрнутом сравнении. */
        const val LEADING = 4

        /** Сравнение листа героя: каждая сдвинутая характеристика [delta]; рост - к лучшему. */
        fun of(delta: List<StatDelta>): Comparison = Comparison(
            delta.map {
                ComparisonLine(
                    statTitle(it.stat),
                    statValue(it.stat, it.before),
                    statValue(it.stat, it.after),
                    signed(it.change) { size -> statValue(it.stat, size) },
                    it.change > 0,
                    StatPriority.rank(it.stat),
                    share(it.before, it.after),
                )
            },
        )

        /** Сравнение фляги с флягой в гнезде ([FlaskVerdict]): меры её листа и строки, что она кладёт на героя. */
        fun of(verdict: FlaskVerdict): Comparison = Comparison(
            verdict.rows.map { row ->
                ComparisonLine(
                    flaskRowTitle(row),
                    flaskRowValue(row, row.before),
                    flaskRowValue(row, row.after),
                    signed(row.after - row.before) { size -> flaskRowValue(row, size) },
                    row.shift.takeIf { it != Shift.EVEN }?.let { it == Shift.UP },
                    rank(row),
                    abs(row.gain),
                )
            },
        )

        /** Важность строки фляги: строка на героя - по своей характеристике, мера листа фляги - после важных групп. */
        private fun rank(row: FlaskRow): Int = if (row.measure == FlaskMeasure.LINE) StatPriority.rank(row.stat) else StatPriority.entries.size

        /** Относительная величина изменения: новое число - целиком. */
        private fun share(before: Double, after: Double): Double = when {
            before != 0.0 -> abs(after - before) / abs(before)
            after != 0.0 -> 1.0
            else -> 0.0
        }

        /** Разница [change] со знаком: [format] печатает её величину в единицах строки. */
        private fun signed(change: Double, format: (Double) -> String): String = (if (change < 0) ModifierText.MINUS else "+") + format(abs(change))
    }
}
