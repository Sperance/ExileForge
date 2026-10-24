package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.display.clock
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.roll.WorkFigure
import com.sperance.exileforge.rules.roll.WorkLine
import com.sperance.exileforge.rules.roll.WorkSource
import kotlin.math.abs

/** Вид поля листа работы: по нему экран берёт значок и цвет, [help] - ключ статьи справки ремёсел. */
enum class LedgerKind(val help: String) {
    CYCLE("cycle"),
    HOUR("hour"),
    WASTE("waste"),

    /** Работа без пустых циклов: «впустую» не бывает. */
    SURE("sure"),
    YIELD("yield"),
    DOUBLE("bonuses"),
    TRIPLE("bonuses"),
    SAVE("bonuses"),
    FREE("bonuses"),
    EXPERIENCE("experience"),
    FIND("finds"),

    /** Находка камня в жиле: камень по весам, целый или осколок. */
    GEM("finds"),
}

/** Ступень разбора: база работы, вклад источника, пустое место инструмента или итог. */
enum class StepKind { BASE, SOURCE, NO_TOOL, TOTAL }

/**
 * Ступень лесенки разбора: [label] - кто, [note] - сдвиг со знаком (у пустого инструмента - подсказка), [value] - мера после этой ступени.
 * Полоса - доли от наибольшего числа лесенки: до [low] - то, что было, от [low] до [high] - сдвиг источника; у базы и итога [low] = 0.
 * [tool] - вклад инструмента.
 */
data class LedgerStep(val kind: StepKind, val label: String, val note: String, val value: String, val low: Float, val high: Float, val tool: Boolean = false)

/**
 * Поле листа работы строкой: [caption] и [note] слева, [value] справа, [tool] - метка «инструмент» (его вклад ненулевой), [steps] - лесенка
 * разбора по источникам. [key] различает строки при раскрытии, [item] - код находки у [LedgerKind.FIND].
 */
data class LedgerRow(val key: String, val kind: LedgerKind, val caption: String, val note: String, val value: String, val tool: Boolean, val steps: List<LedgerStep>, val item: String = "")

/**
 * Поля листа работы из разбора правил ([ProfessionView.breakdown]): цикл, в час, исход (впустую или гарантия, выход ×1–N, удвоение, утроение,
 * возврат и бесплатный цикл у работы с расходом), опыт, каждая находка и находка камня. Мера без базы и без вкладов не показывается.
 */
object JobLedger {
    fun of(rules: CraftsRules, profession: ProfessionView, job: JobView): List<LedgerRow> {
        val breakdown = profession.breakdown(rules, job)
        val ladder = Ladder(profession)
        val spends = job.inputs.isNotEmpty()
        val nothing = breakdown[WorkFigure.NOTHING]
        val sure = nothing.isEmpty
        val cycle = breakdown[WorkFigure.CYCLE]
        val units = breakdown[WorkFigure.YIELD]
        val shown = listOfNotNull(
            ladder.row(LedgerKind.CYCLE, cycle, Scale.TIME, ui("crafts.help.cycle"), ui("crafts.ledger.base_note", clock(cycle.base.toLong())), clock(cycle.total.toLong())),
            breakdown[WorkFigure.HOUR].let { ladder.row(LedgerKind.HOUR, it, Scale.COUNT, ui("crafts.help.hour"), "", ui("crafts.about", fineNumber(it.total))) },
            if (sure) {
                ladder.row(LedgerKind.SURE, nothing, Scale.PERCENT, ui("crafts.help.sure"), "", "✓")
            } else {
                ladder.row(LedgerKind.WASTE, nothing, Scale.CHANCE, ui("crafts.help.waste"), "", percent(nothing.total))
            },
            ladder.row(
                LedgerKind.YIELD,
                units,
                Scale.UNITS,
                ui("crafts.ledger.yield"),
                if (units.total > 1) ui("crafts.ledger.extra_note", percent((units.total - 1) * 100)) else "",
                JobRates.yieldRange(profession.bonus),
            ),
            ladder.chance(LedgerKind.DOUBLE, breakdown[WorkFigure.DOUBLE], ui("crafts.ledger.double"), ""),
            ladder.chance(LedgerKind.TRIPLE, breakdown[WorkFigure.TRIPLE], ui("crafts.ledger.triple"), ui("crafts.ledger.triple_note")),
            if (spends) ladder.chance(LedgerKind.SAVE, breakdown[WorkFigure.SAVE], ui("crafts.ledger.save"), "") else null,
            if (spends) ladder.chance(LedgerKind.FREE, breakdown[WorkFigure.FREE], ui("crafts.ledger.free"), "") else null,
            breakdown[WorkFigure.EXPERIENCE].let { ladder.row(LedgerKind.EXPERIENCE, it, Scale.COUNT, ui("crafts.help.experience"), "", "+" + number(it.total)) },
        )
        val finds = breakdown.finds.mapIndexed { i, find ->
            ladder.row(LedgerKind.FIND, find.line, Scale.CHANCE, itemTitle(find.extra.item), ui("crafts.ledger.find"), percent(find.line.total), "${LedgerKind.FIND.name}$i", find.extra.item)
        }
        val gem = breakdown.gem?.let { ladder.row(LedgerKind.GEM, it, Scale.CHANCE, ui("crafts.ledger.gem"), ui("crafts.ledger.gem_note", job.gemAmount), percent(it.total)) }
        return shown + finds + listOfNotNull(gem)
    }

    /**
     * Единица меры: как печатаются её числа и сдвиги. [shift] получает сдвиг источника и меру, которую множит его бонус ([Ladder]):
     * её читает лишь шанс, что множится бонусом героя.
     */
    private enum class Scale(val value: (Double) -> String, val shift: (Double, Double) -> String) {
        TIME({ clock(it.toLong()) }, { shift, _ -> signed(shift) { ui("crafts.ledger.seconds", fineNumber(it / 1000)) } }),
        COUNT({ fineNumber(it) }, { shift, _ -> signed(shift, ::fineNumber) }),
        PERCENT({ percent(it) }, { shift, _ -> signed(shift, ::percent) }),

        /** Средний выход цикла: «×1.1», сдвиг - лишняя добыча в процентах. */
        UNITS({ ui("crafts.ledger.times", number(it)) }, { shift, _ -> signed(shift) { percent(it * 100) } }),

        /**
         * Шанс, что бонус героя множит, а не складывает (впустую - удача, находка - уровень и бонус находок): сдвиг - доля от меры, которую
         * множит бонус, и что она даёт в пунктах: «−8,3% от 35% = −2,9». Мера без числа - сдвиг пунктами.
         */
        CHANCE({ percent(it) }, { shift, from -> if (from == 0.0) signed(shift, ::percent) else ui("crafts.ledger.relative", signed(shift / from * 100, ::percent), percent(from), signed(shift, ::number)) }),
    }

    private fun percent(value: Double): String = ui("crafts.percent", number(value))

    /** Число со знаком: «+3%», «−7 с». */
    private fun signed(value: Double, text: (Double) -> String): String = (if (value < 0) "−" else "+") + text(abs(value))

    /** Лесенки разбора профессии [profession]: подписи источников и пустое место инструмента, если слот пуст. */
    private class Ladder(private val profession: ProfessionView) {
        private val toolless = profession.tool.isNotBlank() && profession.equipped == null

        /** Шанс, которого у работы может не быть: без базы и вкладов строки нет. */
        fun chance(kind: LedgerKind, line: WorkLine, caption: String, note: String): LedgerRow? = if (line.isEmpty) null else row(kind, line, Scale.PERCENT, caption, note, percent(line.total))

        fun row(kind: LedgerKind, line: WorkLine, unit: Scale, caption: String, note: String, value: String, key: String = kind.name, item: String = ""): LedgerRow = LedgerRow(key, kind, caption, note, value, line.shifts.any { it.source == WorkSource.TOOL }, steps(line, unit), item)

        /**
         * Лесенка меры [line]: база, вклад каждого источника и итог. Бонус уровня множит базу, бонусы прочих источников - меру после
         * уровня (у шанса впустую уровень вклада не даёт - это та же база): от неё [Scale.CHANCE] считает долю сдвига.
         */
        private fun steps(line: WorkLine, unit: Scale): List<LedgerStep> {
            val runs = line.shifts.runningFold(line.base) { run, shift -> run + shift.shift }
            val top = (runs + line.total).maxOf { abs(it) }.takeIf { it > 0 } ?: 1.0
            fun share(value: Double) = (abs(value) / top).toFloat()
            val bySource = line.shifts.zip(runs.zipWithNext()).associateBy { it.first.source }
            val leveled = line.base + (bySource[WorkSource.LEVEL]?.first?.shift ?: 0.0)
            val middle = WorkSource.entries.mapNotNull { source ->
                val step = bySource[source]
                when {
                    step != null -> {
                        val (shift, run) = step
                        val (before, after) = run
                        LedgerStep(StepKind.SOURCE, title(source), unit.shift(shift.shift, if (source == WorkSource.LEVEL) line.base else leveled), unit.value(after), share(minOf(before, after)), share(maxOf(before, after)), source == WorkSource.TOOL)
                    }

                    source == WorkSource.TOOL && toolless -> LedgerStep(StepKind.NO_TOOL, ui("crafts.ledger.no_tool"), ui("crafts.ledger.no_tool_hint"), "—", 0f, 0f)

                    else -> null
                }
            }
            return listOf(LedgerStep(StepKind.BASE, ui("crafts.ledger.base"), "", unit.value(line.base), 0f, share(line.base))) + middle +
                LedgerStep(StepKind.TOTAL, ui("crafts.ledger.total"), "", unit.value(line.total), 0f, share(line.total))
        }

        private fun title(source: WorkSource): String = when (source) {
            WorkSource.LEVEL -> ui("crafts.ledger.level", profession.level)
            WorkSource.TOOL -> ui("crafts.ledger.tool")
            WorkSource.SHEET -> ui("crafts.ledger.sheet")
            WorkSource.GUILD -> ui("crafts.ledger.guild")
            WorkSource.FATE -> ui("crafts.ledger.fate")
        }
    }
}
