package com.sperance.exileforge.core.network

import kotlinx.serialization.json.*
import okhttp3.*

data class RequestLog(val method: String, val path: String, val status: Int?, val elapsedMs: Long, val request: String, val response: String, val ok: Boolean)
