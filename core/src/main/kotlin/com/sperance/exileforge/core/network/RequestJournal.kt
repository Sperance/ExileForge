package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.command.RequestFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RequestJournal {
    private val mutable = MutableStateFlow<List<RequestLog>>(emptyList())
    val entries = mutable.asStateFlow()
    fun add(entry: RequestLog) {
        mutable.update { (listOf(entry) + it).take(60) }
    }
    /** Последние [limit] неудач (4.3.1), новые первыми - хвост для отчёта об ошибке. */
    fun failures(limit: Int): List<RequestFailure> = entries.value.asSequence().mapNotNull(RequestLog::failure).take(limit).toList()

    fun clear() {
        mutable.value = emptyList()
    }
}
