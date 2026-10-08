package com.sperance.exileforge.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The one HTTP client of the process.
 *
 * Every [GameApi] — a new one per server the player points at — shares it, so they share its
 * connection pool, its dispatcher threads and the TLS sessions already negotiated. It is built on
 * first use rather than at start-up. Commands are never retried on a dropped connection (a repeat
 * could pay twice) and redirects are not followed, so a token never travels to a host it was not
 * issued for. OkHttp asks for gzip on its own and unpacks it transparently. Since 3.95.2 a read (GET) that lost its
 * connection is asked once more ([ReadRetry]) and a connection gets [CONNECT_S] seconds: after the app returns from the
 * background the first request no longer sits out ten seconds on a socket that went dead while it was away.
 */
object ForgeHttp {
    private const val CONNECT_S = 5L

    private val built = lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_S, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(ReadRetry)
            .followRedirects(false).followSslRedirects(false)
            .build()
    }
    val client: OkHttpClient by built

    /** Back from the background or a locked screen: the pooled sockets may be dead on the far side, and a command is never retried on one. */
    fun dropIdleConnections() {
        if (built.isInitialized()) client.connectionPool.evictAll()
    }
}

/**
 * Чтение (GET) на оборванном соединении повторяется один раз (3.95.2): оно ничего не меняет на сервере, а мёртвый сокет пула
 * после фона - не причина показывать «сервер недоступен». Команды не повторяются никогда: повтор мог бы заплатить дважды.
 */
private object ReadRetry : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            chain.proceed(request)
        }
    }
}
