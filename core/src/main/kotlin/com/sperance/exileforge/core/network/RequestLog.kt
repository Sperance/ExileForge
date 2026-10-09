package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.command.RequestFailure
import java.time.Instant

/**
 * Один обмен с сервером в журнале. [code] (4.3.1) - код отказа сервера, если был; [queued] - команда не ушла, а встала в
 * очередь; [at] - когда запрос завершился (мс).
 */
data class RequestLog(
    val method: String,
    val path: String,
    val status: Int?,
    val elapsedMs: Long,
    val request: String,
    val response: String,
    val ok: Boolean,
    val code: String? = null,
    val queued: Boolean = false,
    val at: Long = System.currentTimeMillis(),
) {
    /** Неудача для отчёта об ошибке (4.3.1): без запроса и тела; статус 0 - ответа не было. Удача и очередь - null. */
    fun failure(): RequestFailure? = if (ok || queued) null else RequestFailure(method, path.substringBefore('?'), status ?: 0, code.orEmpty(), Instant.ofEpochMilli(at).toString())
}
