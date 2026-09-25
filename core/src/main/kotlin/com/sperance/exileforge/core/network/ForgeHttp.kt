package com.sperance.exileforge.core.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * The one HTTP client of the process.
 *
 * Every [GameApi] — a new one per server the player points at — shares it, so they share its
 * connection pool, its dispatcher threads and the TLS sessions already negotiated. It is built on
 * first use rather than at start-up. Commands are never retried on a dropped connection (a repeat
 * could pay twice) and redirects are not followed, so a token never travels to a host it was not
 * issued for. OkHttp asks for gzip on its own and unpacks it transparently.
 */
object ForgeHttp {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false)
            .build()
    }
}
