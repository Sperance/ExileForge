package com.sperance.exileforge.core.network


data class RequestLog(val method: String, val path: String, val status: Int?, val elapsedMs: Long, val request: String, val response: String, val ok: Boolean)
