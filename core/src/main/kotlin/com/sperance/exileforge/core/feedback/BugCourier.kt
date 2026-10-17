package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Что курьер отчётов говорит игроку: владелец решает, каким тостом. */
sealed interface BugNews {
    /** За проход ушло [count] отчётов - один тост на проход, а не на отчёт. */
    data class Sent(val count: Int) : BugNews

    /** Сервер отклонил отчёт окончательно, [reason] - его словами. */
    data class Refused(val reason: String) : BugNews

    /** Связи нет или сервер занят: очередь ждёт повтора - говорится раз, пока очередь не опустеет. */
    data object Queued : BugNews
}

/**
 * Курьер очереди отчётов: один долгоживущий работник над [outbox] - запрос в полёте никогда не отменяется. Проход
 * шлёт очередь от старых к новым, пока она не опустеет или сервер не ответит «не сейчас» ([CommandQueue.transient], сеть): тогда
 * курьер ждёт отступа, удваивая его до [RETRY_FIRST] × 2^[RETRY_DOUBLINGS]. Новый отчёт и возврат связи ([wake]) будят его раньше
 * и сбрасывают отступ - но только между запросами. Принятый или отклонённый отчёт сперва уходит из очереди, затем о нём говорится
 * ([news]); ушедшие за проход - одной вестью [BugNews.Sent].
 */
class BugCourier(
    private val outbox: BugOutbox,
    /** Отправка отчёта серверу: исключение [ApiFailure] - его ответ, прочие - сеть. */
    private val send: suspend (BugReportRequest) -> Unit,
    private val news: (BugNews) -> Unit,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val disk = Mutex()
    private val wakeUp = Channel<Unit>(Channel.CONFLATED)

    /** Отчёт [report] - в очередь на диске (с ключом, [BugOutbox.put]); будит курьера. */
    suspend fun put(report: BugReportRequest): BugReportRequest = onDisk { outbox.put(report) }.also { wake() }

    /** Разбудить курьера: новый отчёт или вернулась связь - повтор сразу, без досиживания отступа. */
    fun wake() {
        wakeUp.trySend(Unit)
    }

    /** Работа курьера - пока жива область, в которой его запустили. */
    suspend fun run(): Nothing {
        var attempt = 0
        var told = false
        while (true) {
            val pass = pass()
            if (pass.sent > 0) news(BugNews.Sent(pass.sent))
            if (!pass.stuck) {
                told = false
                attempt = 0
                wakeUp.receive()
                continue
            }
            if (!told) {
                told = true
                news(BugNews.Queued)
            }
            val woken = withTimeoutOrNull(RETRY_FIRST shl attempt.coerceAtMost(RETRY_DOUBLINGS)) { wakeUp.receive() } != null
            attempt = if (woken) 0 else attempt + 1
        }
    }

    /**
     * Один проход по очереди: сколько ушло и встал ли он на ответе «не сейчас».
     *
     * Сеть - тот же ключ позже; ответ сервера получен - отчёт уходит из очереди прежде любой вести, и отмена этого не прервёт.
     */
    private suspend fun pass(): Pass {
        var sent = 0
        while (true) {
            val next = onDisk { outbox.pending() }.firstOrNull() ?: return Pass(sent, stuck = false)
            val refused = try {
                send(next)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiFailure) {
                if (CommandQueue.transient(e.status)) return Pass(sent, stuck = true)
                e.message.orEmpty()
            } catch (_: Exception) {
                return Pass(sent, stuck = true)
            }
            withContext(NonCancellable) { onDisk { outbox.remove(next.key) } }
            if (refused == null) sent++ else news(BugNews.Refused(refused))
        }
    }

    private suspend fun <T> onDisk(block: () -> T): T = disk.withLock { withContext(io) { block() } }

    private data class Pass(val sent: Int, val stuck: Boolean)

    private companion object {
        /** Первый отступ повтора, мс, и сколько раз он удваивается (2 с → 2 мин). */
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 6
    }
}
