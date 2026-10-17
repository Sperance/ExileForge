package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.command.RequestFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Последние обмены с сервером, новые первыми: хвост для отчёта об ошибке и свидетель обрывов связи. */
class RequestJournal {
    private val mutable = MutableStateFlow<List<RequestLog>>(emptyList())
    val entries = mutable.asStateFlow()
    fun add(entry: RequestLog) {
        mutable.update { (listOf(entry) + it).take(60) }
    }

    /** Последние [limit] неудач, новые первыми - хвост для отчёта об ошибке. */
    fun failures(limit: Int): List<RequestFailure> = entries.value.asSequence().mapNotNull(RequestLog::failure).take(limit).toList()

    /** С момента [at] (мс) был запрос без ответа: обрыв, тайм-аут или нет сети. Команда, вставшая в очередь, не считается. */
    fun droppedSince(at: Long): Boolean = entries.value.any { it.status == null && !it.queued && it.at >= at }

    fun clear() {
        mutable.value = emptyList()
    }
}
