package com.sperance.exileforge.presentation.app

import android.content.Context
import com.sperance.exileforge.core.feedback.BugOutbox
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Доставка отчётов об ошибке (4.9.0) - «очередь на диске» [BugOutbox]: отчёт со сжатым снимком сразу ложится файлом и уходит
 * повторами под своим ключом, пока сервер не ответит. Не зависит от занятости [com.sperance.exileforge.core.session.CommandRunner]
 * и переживает краш и перезапуск: оставшееся в очереди уходит, как только есть связь. Принят - тост «отправлено», отклонён
 * окончательно - тост с причиной; нет связи - один тост «уйдёт, когда будет связь» и повтор с отступом. Один на приложение.
 */
class BugDelivery(context: Context, private val connection: ServerConnection, private val notices: Notices, private val scope: CoroutineScope) {
    private val outbox = BugOutbox(File(context.filesDir, OUTBOX_DIR))
    private val disk = Mutex()
    private var draining: Job? = null

    init {
        // Оставшееся с прошлого запуска уходит, как только встанет связь с сервером
        scope.launch {
            connection.current.filterNotNull().first()
            kick()
        }
    }

    /**
     * Отчёт, что собирает [build] (сжатие снимка, подпись - [com.sperance.exileforge.core.network.GameApi.bugReport]), - в очередь
     * и в путь; [onQueued] - отчёт на диске. Идёт в области приложения: закрытый экран сборку не обрывает.
     */
    fun file(build: suspend () -> BugReportRequest, onQueued: suspend () -> Unit = {}) {
        scope.launch {
            val report = build()
            disk.withLock { withContext(Dispatchers.IO) { outbox.put(report) } }
            onQueued()
            kick()
        }
    }

    /** Связь вернулась - ждущие отчёты уходят сразу, без досиживания отступа. */
    fun kick() {
        if (!connection.ready) return
        draining?.cancel()
        draining = scope.launch { drain() }
    }

    private suspend fun drain() {
        var attempt = 0
        var told = false
        while (true) {
            val next = disk.withLock { withContext(Dispatchers.IO) { outbox.pending() } }.firstOrNull() ?: return
            val delivered = try {
                connection.api.sendBug(next)
                notices.toast(ui("bug.sent"))
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiFailure) {
                if (CommandQueue.transient(e.status)) {
                    false
                } else {
                    notices.toast(ui("bug.refused", e.message.orEmpty()), NoticeKind.FAILED)
                    true
                }
            } catch (_: Exception) {
                // Сеть: тот же ключ позже
                false
            }
            if (delivered) {
                disk.withLock { withContext(Dispatchers.IO) { outbox.remove(next.key) } }
                attempt = 0
                continue
            }
            if (!told) {
                told = true
                notices.toast(ui("bug.queued"))
            }
            delay(RETRY_FIRST shl attempt.coerceAtMost(RETRY_DOUBLINGS))
            attempt++
        }
    }

    private companion object {
        /** Папка очереди в личных файлах приложения. */
        const val OUTBOX_DIR = "bug_outbox"

        /** Первый отступ повтора, мс, и сколько раз он удваивается (2 с → 2 мин). */
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 6
    }
}
