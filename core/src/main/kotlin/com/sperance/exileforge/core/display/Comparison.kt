package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.character.FlaskMeasure
import com.sperance.exileforge.core.character.FlaskRow
import com.sperance.exileforge.core.character.FlaskVerdict
import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.character.StatPolarity
import com.sperance.exileforge.core.character.StatPriority
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Op
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
 * затем величина изменения. Больше [LEADING] строк - свёрнутое показывает [leading], остальное ([hidden]) - по кнопке. [lossesFirst]
 * (4.12.x, ASANA-05, фляги) - потери идут первыми и не сворачиваются: что пропадёт с заменой, видно сразу.
 */
class Comparison(lines: List<ComparisonLine>, private val lossesFirst: Boolean = false) {
    /** Все строки, важнейшие первыми. */
    val lines: List<ComparisonLine> = lines.sortedWith(compareBy<ComparisonLine> { if (lossesFirst && it.better == false) 0 else 1 }.thenBy { it.rank }.thenByDescending { it.weight })

    /** Сколько строк видно свёрнутым: не меньше [LEADING] и все потери, если они идут первыми. */
    private val shown: Int get() = maxOf(LEADING, if (lossesFirst) lines.count { it.better == false } else 0)

    /** Сворачивается ли сравнение: строк больше, чем видно свёрнутым. */
    val foldable: Boolean get() = lines.size > shown

    /** Строки, видные в свёрнутом сравнении. */
    val leading: List<ComparisonLine> get() = lines.take(shown)

    /** Строки под кнопкой «Показать ещё». */
    val hidden: List<ComparisonLine> get() = lines.drop(shown)

    companion object {
        /** Сколько строк видно в свёрнутом сравнении. */
        const val LEADING = 4

        /**
         * Сравнение листа героя: каждая сдвинутая характеристика [delta]; к лучшему ли сдвиг - полярность характеристики
         * ([StatPolarity], 0.15.0, #46: меньше получаемого урона - лучше, нейтральная - без цвета). С контентом [index] (4.6.5) - только
         * строки сводки (часть строки и сила уникальной - внутри своей строки), множитель - «100% → 60%», разница - «−40%».
         * С листом героя [sheet] уровня [level] (0.14.0, ASANA-N07) уклонение и броня - процентом защиты, разница - в п.п.
         */
        fun of(delta: List<StatDelta>, index: ContentIndex? = null, off: Set<String> = emptySet(), sheet: Map<String, Double>? = null, level: Int = 0): Comparison {
            val after = sheet?.let { it + delta.associate { change -> change.stat to change.after } }
            return Comparison(
                delta.filter { index == null || index.stats.section(it.stat) != null && index.stats[it.stat]?.partOf == null }.map {
                    if (index != null && sheet != null && after != null) defenceLine(it, index, sheet, after, level)?.let { line -> return@map line }
                    val multiplier = index?.stats?.get(it.stat)?.multiplier == true
                    ComparisonLine(
                        statTitle(it.stat),
                        statValue(it.stat, it.before, index),
                        if (it.stat in off) ui("compare.off") else statValue(it.stat, it.after, index),
                        when {
                            multiplier -> factorChange((100 + it.after) / (100 + it.before))

                            // Флаг (4.8.0) не прибавляется: разница - его новая отметка
                            statFlag(it.stat, index) -> statValue(it.stat, it.after, index)

                            else -> signed(it.change) { size -> statValue(it.stat, size, index) }
                        },
                        StatPolarity.better(index, it.stat, it.change),
                        StatPriority.rank(it.stat),
                        share(it.before, it.after),
                    )
                } + (off - delta.map { it.stat }.toSet()).map { stat ->
                    // Отключение (4.12.x, ASANA-02) видно и тогда, когда сейчас и так ноль: узел всё равно закроет характеристику. Закрыть -
                    // убавить: к худшему, у характеристики «меньше лучше» - к лучшему (#46)
                    ComparisonLine(statTitle(stat), statValue(stat, 0.0, index), ui("compare.off"), "", StatPolarity.better(index, stat, -1.0), StatPriority.rank(stat), 1.0)
                },
            )
        }

        /** Уклонение и броня процентом защиты (0.14.0, ASANA-N07): «38% → 42%», «+4 п.п.»; прочее - null. */
        private fun defenceLine(change: StatDelta, index: ContentIndex, sheet: Map<String, Double>, after: Map<String, Double>, level: Int): ComparisonLine? {
            val was = StatLimits.defence(change.stat, sheet, level, index)?.effective ?: return null
            val will = StatLimits.defence(change.stat, after, level, index)?.effective ?: return null
            return ComparisonLine(
                ui(if (change.stat == StatLimits.EVASION) "stat.defence_title.evasion" else "stat.defence_title.armour"),
                ui("stat.defence_percent", number(was)),
                ui("stat.defence_percent", number(will)),
                signed(will - was) { size -> ui("compare.points", number(size)) },
                if (abs(will - was) < POINT_NOISE) null else will > was,
                StatPriority.rank(change.stat),
                share(was, will),
            )
        }

        /** Сдвиг защиты меньше сотой п.п. - шум округления, без цвета. */
        private const val POINT_NOISE = 0.01

        /** Характеристики, что строки [lines] заменяют нулём (4.12.x, ASANA-02): «Нет регенерации здоровья» - сравнение пишет «отключится». */
        fun switchedOff(index: ContentIndex, lines: List<Line>): Set<String> = lines.flatMapTo(HashSet()) { line ->
            val def = index.modifier(line.code) ?: return@flatMapTo emptyList()
            def.effects.indices.filter { def.effects[it].op == Op.SET && line.values.getOrNull(def.slot(it)) == 0.0 }.map { def.effects[it].stat }
        }

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
            lossesFirst = true,
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
