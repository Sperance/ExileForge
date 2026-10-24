package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.campaign.combat.FightRung
import com.sperance.exileforge.core.campaign.combat.HeroLadder
import com.sperance.exileforge.core.display.RowFigure
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.StatSource
import kotlin.math.sign

/** Откуда вклад в сдвинутую строку листа похода. */
sealed interface ShareOrigin {
    /** Источник эффектов захода ([Shift.source]): строка карты, Атлас, узел гильдии, зелье. */
    data class Run(val source: StatSource) : ShareOrigin

    /** Эффекты захода одной долей - их источники не делятся в единице строки; [map] - заход по карте. */
    data class RunWhole(val map: Boolean) : ShareOrigin

    /** Ступень тела боя ([HeroLadder]). */
    data class Fight(val rung: FightRung) : ShareOrigin

    /** Набранное сверх предела [cap]: бой его не читает. */
    data class OverCap(val cap: Double) : ShareOrigin
}

/** Вклад [origin] в число строки: [delta] - сдвиг показанного числа строки ([RowFigure.shown]), не ноль. */
data class RowShare(val origin: ShareOrigin, val delta: Double)

/**
 * Разбор сдвинутой строки: число строки героя без похода [base] ([mine] - его лист) и вклады по порядку [shares]; показанное число
 * [base] и сумма вкладов - ровно показанное число строки.
 */
data class RowShift(val base: RowFigure, val mine: StatFigures, val shares: List<RowShare>)

/** Тело боя по ступеням ([HeroLadder]) цифрами сводки: до строк боя ([start]) и после каждой ступени ([steps]). */
class FightFigures(val start: StatFigures, val steps: List<Pair<FightRung, StatFigures>>)

/** Лестница тела боя [ladder] цифрами сводки героя - тем же листом боя, что сводка; без него - как есть. Тяжёлая: вне главного потока. */
fun HeroView.fightFigures(ladder: HeroLadder): FightFigures {
    val read = { stats: Map<String, Double> -> combat?.figures(stats) ?: StatFigures(stats) }
    return FightFigures(read(ladder.start().stats), ladder.bodies().map { (rung, body) -> rung to read(body.stats) })
}

/**
 * Ступени листа похода для разбора сдвинутых строк телескопом: лист героя без похода [mine], после эффектов захода [run] (null - их нет)
 * с источниками изменённых цифр [runSources] ([onMap] - заход по карте), после каждой ступени тела боя [fight] и лист, что печатает
 * строка, - [now]. Вклад ступени - сдвиг числа строки от прежней ступени, в единице строки ([RowFigure]): процент защиты, набранное до
 * предела; что предел срезал - последним ([ShareOrigin.OverCap]). Последняя ступень - всегда [now]: сумма сходится с числом строки.
 */
class RunSteps(
    private val mine: StatFigures,
    private val run: StatFigures?,
    private val runSources: Map<String, List<Shift>>,
    private val onMap: Boolean,
    private val fight: List<Pair<FightRung, StatFigures>>,
    private val now: StatFigures,
) {
    /** Разбор строки [stat] героя уровня [level] над контентом [index]: вклады с нулевым показанным сдвигом не входят. */
    fun of(stat: String, level: Int?, index: ContentIndex): RowShift {
        val base = RowFigure.of(stat, mine, level, index)
        val finish = RowFigure.of(stat, now, level, index)
        val steps = steps()
        val points = steps.mapIndexed { i, (origin, figures) -> origin to if (i == steps.lastIndex) finish else RowFigure.of(stat, figures, level, index) }
        val exact = buildList {
            var before = base.gathered
            points.forEach { (origin, figure) ->
                val delta = figure.gathered - before
                if (origin == null) addAll(runShares(stat, delta)) else add(RowShare(origin, delta))
                before = figure.gathered
            }
            finish.cap?.let { add(RowShare(ShareOrigin.OverCap(it), (finish.value - finish.gathered) - (base.value - base.gathered))) }
        }
        return RowShift(base, mine, telescoped(base.value, exact, finish))
    }

    /** Ступени по порядку: эффекты захода (origin null - его делит [runShares]), затем ступени боя; без ступеней - заход до [now]. */
    private fun steps(): List<Pair<ShareOrigin?, StatFigures>> {
        val all = listOfNotNull(run?.let { null to it }) + fight.map { (rung, figures) -> ShareOrigin.Fight(rung) to figures }
        return all.ifEmpty { listOf(null to now) }
    }

    /**
     * Сдвиг [delta] эффектами захода строки [stat] по их источникам - долями их сдвигов листа; источники с разным знаком или без них -
     * одной долей захода.
     */
    private fun runShares(stat: String, delta: Double): List<RowShare> {
        val sources = runSources[stat].orEmpty().filter { it.delta != 0.0 }
        val total = sources.sumOf { it.delta }
        if (sources.isEmpty() || total == 0.0 || sources.any { it.delta.sign != delta.sign }) return listOf(RowShare(ShareOrigin.RunWhole(onMap), delta))
        return sources.map { RowShare(ShareOrigin.Run(it.source), delta * it.delta / total) }
    }

    /** Вклады [exact] от числа [start] показанными числами строки [finish]: сдвиг вклада - разность показанных сумм до и после него. */
    private fun telescoped(start: Double, exact: List<RowShare>, finish: RowFigure): List<RowShare> {
        var sum = start
        return exact.mapNotNull { share ->
            val before = finish.shown(sum)
            sum += share.delta
            (finish.shown(sum) - before).takeIf { it != 0.0 }?.let { share.copy(delta = it) }
        }
    }
}
