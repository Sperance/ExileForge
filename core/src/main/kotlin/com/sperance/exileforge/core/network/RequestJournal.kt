package com.sperance.exileforge.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*
import okhttp3.*

class RequestJournal {
    private val mutable = MutableStateFlow<List<RequestLog>>(emptyList())
    val entries = mutable.asStateFlow()
    fun add(entry: RequestLog) { mutable.update { (listOf(entry) + it).take(60) } }
    fun clear() { mutable.value = emptyList() }
}
