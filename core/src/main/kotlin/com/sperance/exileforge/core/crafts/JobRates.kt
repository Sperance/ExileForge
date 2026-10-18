package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.rules.content.CraftsRules
import com.sperance.exileforge.rules.content.JobKind
import com.sperance.exileforge.rules.roll.Work
import com.sperance.exileforge.rules.roll.WorkBonus
import com.sperance.exileforge.rules.roll.WorkFigure
import kotlin.math.ceil

/** Выход в час одной фишкой: стопка [code] (работа делает не стопку - null, фишка несёт иконку работы) и сколько её в среднем за час. */
class HourYield(val code: String?, val amount: Double)

/**
 * Числа работы для экрана - из разбора правил ([ProfessionView.breakdown]), того же, что лист работы ([JobLedger]): строка списка,
 * плашка идущей работы и лист показывают одно число «в час». Только показ: исход решают кости сервера.
 */
object JobRates {
    /** Средний выход работы [job] в час - итог меры [WorkFigure.HOUR]. */
    fun perHour(rules: CraftsRules, profession: ProfessionView, job: JobView): Double = profession.breakdown(rules, job)[WorkFigure.HOUR].total

    /**
     * Что работа [job] приносит в час в среднем: первым - что она делает ([perHour]), за ним находки - удачные циклы в час на шанс
     * находки; нулевые не пишутся.
     */
    fun hourYields(rules: CraftsRules, profession: ProfessionView, job: JobView): List<HourYield> {
        val breakdown = profession.breakdown(rules, job)
        val landed = Work.landed(breakdown[WorkFigure.CYCLE].total.toLong(), breakdown[WorkFigure.NOTHING].total)
        val made = HourYield(job.output.takeIf { it.isNotBlank() && job.kind in STACK_WORKS }, breakdown[WorkFigure.HOUR].total)
        val finds = breakdown.finds.map { HourYield(it.extra.item, landed * it.line.total / 100) }
        return (listOf(made) + finds).filter { it.amount > 0 }
    }

    /** Сколько выхода приносит удачный цикл с бонусом [bonus]: «×1», с лишней добычей - «×1–N» ([Work.maxUnits]). */
    fun yieldRange(bonus: WorkBonus): String = Work.maxUnits(bonus).let { top -> if (top > 1) ui("crafts.yield_range", top) else ui("crafts.yield_one") }

    /**
     * Сколько мс работы [job] с циклом [cycleMillis] до следующего уровня профессии: опыт удачного цикла сервера ([JobView.experience],
     * уже с бонусом опыта) на долю удачных циклов, целыми циклами. null - на последнем уровне или работа опыта не даёт.
     */
    fun toLevel(profession: ProfessionView, job: JobView, cycleMillis: Long): Long? {
        val next = profession.next ?: return null
        val perCycle = job.experience * Work.landedShare(job.nothing)
        if (perCycle <= 0 || cycleMillis <= 0) return null
        return ceil((next - profession.experience).coerceAtLeast(0.0) / perCycle).toLong() * cycleMillis
    }

    /** Работы, чей выход - стопка сумки: её фишка открывает карточку материала. */
    private val STACK_WORKS = setOf(JobKind.ITEM, JobKind.CONDENSE, JobKind.REFINE)
}
