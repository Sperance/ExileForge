package com.sperance.exileforge.core.session

import com.sperance.exileforge.core.model.command.BugReportRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Зависания раннера - тихим баг-репортом на текущий сервер ([ServerConnection]): кто держал полосу и откуда его звали.
 * Один ключ ([Stall.key]) - не чаще раза в [periodMs], чтобы повторяющееся зависание не засыпало сервер отчётами.
 * Связи ещё нет или отчёт не ушёл - он теряется: это подсказка разработчику, не данные игрока.
 */
class StallReports(
    private val connection: ServerConnection,
    private val scope: CoroutineScope,
    private val periodMs: Long = PERIOD_MS,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) : StallSink {
    private val sentAt = ConcurrentHashMap<String, Long>()

    override fun stalled(stall: Stall) {
        if (!due(stall.key)) return
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

    private fun request(stall: Stall) = BugReportRequest(
        text = "runner stall: ${stall.key}",
        screen = SCREEN,
        context = mapOf("key" to stall.key),
        // Сервер берёт до 20 строк по 400 знаков.
        requests = stall.details.chunked(ENTRY).take(ENTRIES),
    )

    private companion object {
        const val SCREEN = "runner_stall"
        const val PERIOD_MS = 10 * 60 * 1000L
        const val ENTRY = 400
        const val ENTRIES = 20
    }
}
