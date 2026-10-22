package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.command.CrashTrace
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.session.ServerConnection

/** Последние падения клиента на устройстве, новые первыми: стек и версия сборки, в которой оно случилось. */
fun interface CrashTraces {
    suspend fun recent(): List<CrashTrace>
}

/**
 * Что прикладывается к отчёту игрока: к ошибке - последние падения клиента, снимок героя и, из забега, сжатый разбор его последнего
 * боя ([fights], [FightDigest]); журнал сбоев сервера - по правилам контента, без прочитанного контента отчёт уходит без него.
 */
class BugEvidence(private val slice: GameSlice, private val connection: ServerConnection, private val crashes: CrashTraces, private val fights: LastFight) {
    /** Отчёт [report] с приложенным на этот миг. */
    suspend fun attach(report: BugReportRequest): BugReportRequest {
        val index = slice.ui.value.index
        val rules = index?.rules?.feedback
        val evidence = if (report.kind == FeedbackKind.BUG) {
            report.copy(crashes = crashes.recent(), heroSheet = slice.ui.value.hero?.reportSheet(index).orEmpty(), fight = fights.report()?.let(FightDigest::of).orEmpty())
        } else {
            report
        }
        return connection.current.value?.bugReport(evidence, rules?.failures ?: 0) ?: evidence
    }
}
