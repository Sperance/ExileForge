package com.sperance.exileforge.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Обрыв до ответа: чтение и команда со своим ключом повторяются один раз, команда без ключа - никогда. */
class SafeRetryTest {
    private val client = OkHttpClient.Builder().retryOnConnectionFailure(false).addInterceptor(SafeRetry).build()

    /** Сервер рвёт первое соединение и отвечает на второе; [send] шлёт запрос по его адресу. */
    private fun dropThenAnswer(send: (MockWebServer) -> Request): Int = MockWebServer().use { server ->
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setBody("{}"))
        client.newCall(send(server)).execute().use { it.code }
    }

    private fun post(server: MockWebServer, key: String?) = Request.Builder().url(server.url("/cmd"))
        .apply { key?.let { header(IDEMPOTENCY_HEADER, it) } }
        .post("{}".toRequestBody("application/json".toMediaType())).build()

    @Test
    fun readIsAskedOnceMore() {
        assertEquals(200, dropThenAnswer { Request.Builder().url(it.url("/read")).build() })
    }

    @Test
    fun keyedCommandIsAskedOnceMore() {
        assertEquals(200, dropThenAnswer { post(it, "k-1") })
    }

    @Test
    fun commandWithoutKeyIsNeverRepeated() {
        assertFailsWith<IOException> { dropThenAnswer { post(it, null) } }
    }
}
