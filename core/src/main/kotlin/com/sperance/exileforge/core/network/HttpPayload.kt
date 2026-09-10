package com.sperance.exileforge.core.network

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*

internal data class HttpPayload(val status: Int, val body: String)
/** Consume and close the body on OkHttp's worker, keeping cancellation wired through the full read. */
internal suspend fun Call.awaitPayload(): HttpPayload = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            try {
                val payload = response.use {
                    val source = it.body?.source() ?: throw ApiFailure(it.code, null, "Пустой ответ сервера")
                    val limit = 2L * 1024 * 1024
                    source.request(limit + 1)
                    if (source.buffer.size > limit) throw ApiFailure(it.code, null, "Ответ слишком большой")
                    HttpPayload(it.code, source.readUtf8())
                }
                if (!continuation.isCancelled) continuation.resume(payload)
            } catch (e: Exception) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }
        }
    })
}
