package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.session.ServerConnection

/** Последние стеки падений клиента на устройстве, новые первыми: только стек, без версии и потока. */
fun interface CrashTraces {
    suspend fun recent(): List<String>
}

/**
 * Что прикладывается к отчёту игрока: к ошибке - последние падения клиента и снимок героя; журнал сбоев сервера - по правилам
 * контента, без прочитанного контента отчёт уходит без него.
 */
class BugEvidence(private val slice: GameSlice, private val connection: ServerConnection, private val crashes: CrashTraces) {
    /** Отчёт [report] с приложенным на этот миг. */
    suspend fun attach(report: BugReportRequest): BugReportRequest {
        val rules = slice.ui.value.index?.rules?.feedback
        val evidence = if (report.kind == FeedbackKind.BUG) {
            report.copy(traces = crashes.recent(), heroSheet = slice.ui.value.hero?.reportSheet().orEmpty())
        } else {
            report
        }
        return connection.current.value?.bugReport(evidence, rules?.failures ?: 0) ?: evidence
    }
}
