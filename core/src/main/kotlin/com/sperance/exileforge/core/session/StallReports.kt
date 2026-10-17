package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.network.RequestJournal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Зависания раннера - тихим баг-репортом на текущий сервер ([ServerConnection]): кто держал полосу и откуда его звали.
 * Один ключ ([Stall.key]) - не чаще раза в [periodMs], чтобы повторяющееся зависание не засыпало сервер отчётами.
 * Связи ещё нет или отчёт не ушёл - он теряется: это подсказка разработчику, не данные игрока. Работа, в которой запрос остался без
 * ответа ([RequestJournal.droppedSince] от [Stall.since]: обрыв, тайм-аут, нет сети), - не зависание, а связь: отчёта нет.
 */
class StallReports(
    private val connection: ServerConnection,
    private val scope: CoroutineScope,
    private val journal: RequestJournal,
    private val periodMs: Long = PERIOD_MS,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) : StallSink {
    private val sentAt = ConcurrentHashMap<String, Long>()

    override fun stalled(stall: Stall) {
        if (journal.droppedSince(stall.since) || !due(stall.key)) return
        val api = connection.current.value ?: return
        scope.launch {
            try {
                api.reportBug(request(stall))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }

    /** Пора ли снова сообщать о [key]; если да - отметка ставится сразу. */
    internal fun due(key: String): Boolean {
        val at = now()
        var due = false
        sentAt.compute(key) { _, last -> if (last == null || at - last >= periodMs) at.also { due = true } else last }
        return due
    }

    /** Причина - первые строки журнала раннера (кто держал полосу и [ORIGIN_FRAMES] кадров, откуда) - прямо в тексте отчёта. */
    private fun request(stall: Stall) = BugReportRequest(
        text = (listOf("runner stall: ${stall.key}") + stall.details.lines().filter { it.isNotBlank() }.take(REASON_LINES)).joinToString("\n"),
        screen = SCREEN,
        context = mapOf("key" to stall.key),
    )

    private companion object {
        const val SCREEN = "runner_stall"
        const val PERIOD_MS = 10 * 60 * 1000L

        /** Строка занятости, строка «откуда» и кадры места вызова. */
        const val REASON_LINES = 2 + ORIGIN_FRAMES
    }
}
