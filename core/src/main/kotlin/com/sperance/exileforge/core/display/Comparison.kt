package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.character.FlaskMeasure
import com.sperance.exileforge.core.character.FlaskRow
import com.sperance.exileforge.core.character.FlaskVerdict
import com.sperance.exileforge.core.character.GearVerdict.Shift
import com.sperance.exileforge.core.character.MightShift
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.character.StatPolarity
import com.sperance.exileforge.core.character.StatPriority
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HitRange
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.sheet.HitRanges
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
) {
    /** Строка что-то меняет: «было» и «стало» печатаются по-разному; строка без перемены нигде не пишется. */
    val moves: Boolean get() = before != after
}

/**
 * Сравнение листа «было → стало»: строки по важности - группа [StatPriority], затем величина изменения; [lossesFirst]
 * (фляги) - потери идут первыми: что пропадёт с заменой, видно сразу, как сравнение раскроют. Свёрнутое сравнение - один
 * заголовок со счётом [better] и [worse] и сдвигом Мощи ([speech] - он же для диктора).
 */
class Comparison(lines: List<ComparisonLine>, private val lossesFirst: Boolean = false) {
    /** Строки, что что-то меняют ([ComparisonLine.moves]), важнейшие первыми. */
    val lines: List<ComparisonLine> = lines.filter(ComparisonLine::moves).sortedWith(compareBy<ComparisonLine> { if (lossesFirst && it.better == false) 0 else 1 }.thenBy { it.rank }.thenByDescending { it.weight })

    /** Сколько строк к лучшему. */
    val better: Int = this.lines.count { it.better == true }

    /** Сколько строк к худшему; нейтральные не считаются ни туда, ни сюда. */
    val worse: Int = this.lines.count { it.better == false }

    /**
     * Описание заголовка для диктора: имя [title], Мощь «было → стало» с разницей, если [might] стоит показа, счёт без нулевых
     * частей и действие касания - «Раскрыть» или «Свернуть» по [open].
     */
    fun speech(title: String, might: MightShift?, open: Boolean): String {
        val parts = listOfNotNull(
            title.lowercase().replaceFirstChar(Char::titlecase),
            might?.takeIf { it.shown }?.let { "${ui("might.title")} ${mightText(it.before)} → ${mightText(it.after)}, ${mightChange(it)}" },
            ui("compare.better", better).takeIf { better > 0 },
            ui("compare.worse", worse).takeIf { worse > 0 },
        )
        return parts.joinToString(", ") + ". " + ui(if (open) "common.collapse" else "common.expand")
    }

    companion object {
        /**
         * Сравнение листа героя: каждая сдвинутая характеристика [delta]; к лучшему ли сдвиг - полярность характеристики
         * ([StatPolarity], меньше получаемого урона - лучше, нейтральная - без цвета). С контентом [index] - только
         * строки сводки (часть строки и сила уникальной - внутри своей строки), множитель - «100% → 60%», разница - «−40%».
         * С листом героя [sheet] уровня [level] уклонение и броня - процентом защиты, разница - в п.п.; урон вида удара - диапазоном
         * «мин–макс», разница - сдвигом каждого конца, и сдвиг одной ширины диапазона (часть строки) пишет строку урона.
         *
         * Флаг не прибавляется: разница - его новая отметка. Строка, чьё «было» и «стало» печатаются одинаково, не пишется.
         * Характеристика из [off] (строка `SET` в ноль) - «отключится»: узел закроет её; закрыть - убавить (у «меньше лучше» -
         * к лучшему). Строка сводки, чью часть закрывает [off], - «отключится», только когда сама уходит в ноль. Что и сейчас
         * печатается нулём, не отключается: строки «0 → отключится» нет.
         */
        fun of(delta: List<StatDelta>, index: ContentIndex? = null, off: Set<String> = emptySet(), sheet: Map<String, Double>? = null, level: Int = 0): Comparison {
            val after = sheet?.let { it + delta.associate { change -> change.stat to change.after } }
            val row: (String) -> Boolean = { stat -> index == null || index.stats.section(stat) != null && index.stats[stat]?.partOf == null }
            val partly = index?.let { off.mapNotNullTo(HashSet()) { stat -> it.stats[stat]?.partOf } }.orEmpty()
            val ranged = if (index != null && sheet != null) widened(delta, sheet) else emptyList()
            return Comparison(
                (delta + ranged).filter { row(it.stat) }.map {
                    if (index != null && sheet != null && after != null) {
                        defenceLine(it, index, sheet, after, level)?.let { line -> return@map line }
                        rangeLine(it, index, sheet, after)?.let { line -> return@map line }
                    }
                    val multiplier = index?.stats?.get(it.stat)?.multiplier == true
                    val shown = statValue(it.stat, it.after, index)
                    val was = statValue(it.stat, it.before, index)
                    val zero = statValue(it.stat, 0.0, index)
                    ComparisonLine(
                        statTitle(it.stat),
                        was,
                        if (was != zero && (it.stat in off || it.stat in partly && shown == zero)) ui("compare.off") else shown,
                        when {
                            multiplier -> factorChange((100 + it.after) / (100 + it.before))
                            statFlag(it.stat, index) -> shown
                            else -> signed(it.change) { size -> statValue(it.stat, size, index) }
                        },
                        StatPolarity.better(index, it.stat, it.change),
                        StatPriority.rank(it.stat),
                        share(it.before, it.after),
                    )
                },
            )
        }

        /** Уклонение и броня процентом защиты: «38% → 42%», «+4 п.п.»; прочее - null. */
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

        /** Урон вида удара диапазоном: «12–20 → 15–26», разница - сдвиг концов «+3 / +6» (одинаковый - одним числом); не вид удара - null. */
        private fun rangeLine(change: StatDelta, index: ContentIndex, sheet: Map<String, Double>, after: Map<String, Double>): ComparisonLine? {
            val range = HitRange.of(change.stat) ?: return null
            val was = HitRanges.range(sheet, range, index.stats)
            val will = HitRanges.range(after, range, index.stats)
            val low = will.min - was.min
            val high = will.max - was.max
            return ComparisonLine(
                statTitle(change.stat),
                rangeNumber(change.stat, was),
                rangeNumber(change.stat, will),
                if (low == high) signed(low) { number(it) } else ui("compare.range", signed(low) { number(it) }, signed(high) { number(it) }),
                StatPolarity.better(index, change.stat, change.change),
                StatPriority.rank(change.stat),
                share(was.first + was.second, will.first + will.second),
            )
        }

        /** Виды удара, у которых сдвинулась лишь ширина или конец диапазона ([HitRange]), а среднее - нет: строкой урона без перемены среднего. */
        private fun widened(delta: List<StatDelta>, sheet: Map<String, Double>): List<StatDelta> {
            val moved = delta.mapTo(HashSet()) { it.stat }
            return HitRange.entries.filter { it.hit !in moved && (it.spread in moved || it.min in moved || it.max in moved) }.map { StatDelta(it.hit, sheet[it.hit] ?: 0.0, sheet[it.hit] ?: 0.0) }
        }

        /** Сдвиг защиты меньше сотой п.п. - шум округления, без цвета. */
        private const val POINT_NOISE = 0.01

        /** Характеристики, что строки [lines] заменяют нулём: «Нет регенерации здоровья» - сравнение пишет «отключится». */
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
