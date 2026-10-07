package com.sperance.exileforge.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RequestJournal {
    private val mutable = MutableStateFlow<List<RequestLog>>(emptyList())
    val entries = mutable.asStateFlow()
    fun add(entry: RequestLog) {
        mutable.update { (listOf(entry) + it).take(60) }
    }
    fun clear() {
        mutable.value = emptyList()
    }

    /**
     * Служебная часть баг-репорта (3.91.1): последние [requests] запросов строкой каждый - метод, путь, код и время - и
     * последние [errors] отказов с началом ответа сервера. Тела запросов не уходят: в них бывают пароли и токены.
     */
    fun service(requests: Int = REQUESTS, errors: Int = ERRORS): String = buildString {
        val all = entries.value
        appendLine("--- requests")
        all.take(requests).forEach { appendLine(line(it)) }
        appendLine("--- errors")
        all.filterNot { it.ok }.take(errors).forEach { appendLine(line(it) + " " + it.response.take(RESPONSE).replace('\n', ' ')) }
    }

    private fun line(log: RequestLog) = "${log.method} ${log.path} ${log.status ?: "—"} ${log.elapsedMs}ms${if (log.ok) "" else " FAIL"}"

    private companion object {
        const val REQUESTS = 30
        const val ERRORS = 10
        const val RESPONSE = 300
    }
}
