package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.i18n.ui
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [replayed]: the server answered a repeated `Idempotency-Key` with its stored answer (server 1.28.0); [etag] - отпечаток
 * ответа для `If-None-Match` (3.94.1).
 */
internal data class HttpPayload(val status: Int, val body: String, val replayed: Boolean = false, val etag: String? = null)

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
                    val source = it.body.source()
                    val limit = 2L * 1024 * 1024
                    source.request(limit + 1)
                    if (source.buffer.size > limit) throw ApiFailure(it.code, null, ui("api.too_large"))
                    HttpPayload(it.code, source.readUtf8(), it.header(REPLAY_HEADER) == "true", it.header("ETag"))
                }
                if (!continuation.isCancelled) continuation.resume(payload)
            } catch (e: Exception) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }
        }
    })
}

/** The header of an answer replayed for a repeated key: its hero snapshot is the first run's, not the hero now. */
internal const val REPLAY_HEADER = "Idempotent-Replay"
