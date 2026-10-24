package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FeedbackSort
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.rules.feedback.FeedbackQueue

/**
 * Отбор окна своих отчётов игрока: очередь [queue] (null - все очереди), поиск [query] ([ReportSearch]) и порядок [sort] - тот же смысл,
 * что у очереди модерации, но на месте: свои отчёты приходят целиком и уже свежими первыми. Очередь отчёта - [OwnReport.queue], одно
 * правило с сервером. Падения клиента в окне не стоят.
 */
data class OwnReportShelf(val queue: FeedbackQueue? = null, val query: String = "", val sort: FeedbackSort = FeedbackSort.NEWEST) {
    /** Свои отчёты [mine] под поиском, без отбора очереди: из них считается счёт чипов. */
    fun searched(mine: List<OwnReport>): List<OwnReport> {
        val search = ReportSearch(query)
        return mine.filter { it.kind != FeedbackKind.CRASH && search.matches(it.text, it.thread, it.issue) }
    }

    /** Счёт чипов по отчётам под поиском [searched]: каждая очередь и все (ключ null). */
    fun counts(searched: List<OwnReport>): Map<FeedbackQueue?, Int> = searched.groupingBy<OwnReport, FeedbackQueue?> { it.queue }.eachCount() + (null to searched.size)

    /** Строки окна из отчётов под поиском [searched]: очередь отбора и порядок; по номеру Issue - без Issue в конце, равные - свежие первыми. */
    fun rows(searched: List<OwnReport>): List<OwnReport> {
        val picked = queue?.let { q -> searched.filter { it.queue == q } } ?: searched
        return when (sort) {
            FeedbackSort.NEWEST -> picked
            FeedbackSort.ISSUE -> picked.sortedByDescending { it.issue?.number ?: NO_ISSUE }
        }
    }

    private companion object {
        /** Номер Issue отчёта без Issue в порядке по номеру: ниже любого. */
        const val NO_ISSUE = -1
    }
}
