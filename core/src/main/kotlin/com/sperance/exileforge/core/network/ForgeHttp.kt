package com.sperance.exileforge.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Единственный HTTP-клиент процесса.
 *
 * Его делят все [GameApi] - по одному на сервер, на который смотрит игрок, - а с ним пул соединений, потоки диспетчера и уже
 * согласованные сессии TLS. Строится при первом обращении, не на старте. OkHttp сам не повторяет запрос на оборванном соединении
 * и не идёт по редиректам - токен не уходит на хост, которому не выдан; gzip просит и распаковывает сам. Безопасный запрос (чтение
 * или команда со своим ключом), что потерял соединение до ответа, повторяется один раз ([SafeRetry]); соединению - [CONNECT_S] с:
 * после фона первый запрос не высиживает десять секунд на сокете, умершем, пока приложения не было.
 */
object ForgeHttp {
    private const val CONNECT_S = 5L

    /** Предел всего вызова, с повтором [SafeRetry]: дольше запрос не живёт, и сторож команды экрана меряет от него. */
    const val CALL_MS = 30_000L

    private val built = lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_S, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(CALL_MS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(SafeRetry)
            .followRedirects(false).followSslRedirects(false)
            .build()
    }
    internal val client: OkHttpClient by built

    /** Сеть процесса для приложения: `:app` держит её, не зная OkHttp. */
    val link: HttpLink by lazy { HttpLink(client) }

    /** Возврат из фона или с блокировки: сокеты пула могут быть мертвы на том конце - сбросить их до первого запроса. */
    fun dropIdleConnections() {
        if (built.isInitialized()) client.connectionPool.evictAll()
    }
}

/**
 * Сетевой клиент ядра: своя обёртка вокруг клиента OkHttp - `:app` передаёт её [GameApi] и
 * [com.sperance.exileforge.core.update.Updates], а OkHttp остаётся деталью ядра (`implementation`).
 */
class HttpLink internal constructor(internal val client: OkHttpClient) {
    /** Сбросить простаивающие соединения пула: после смены сети они мертвы на том конце. */
    fun dropIdleConnections() = client.connectionPool.evictAll()
}

/**
 * Политика повтора: безопасный запрос, оборванный до ответа (`IOException` из сети), повторяется ровно один раз. Безопасен
 * запрос, чей повтор не меняет итога на сервере: чтение (GET) и запрос с [IDEMPOTENCY_HEADER] - сервер узнаёт ключ и отдаёт
 * прежний ответ или `IDEM_001`, пока первый ещё выполняется, а не исполняет его второй раз. Запрос без ключа и отменённый вызов
 * не повторяются; мёртвый сокет пула после фона - не причина показывать «сервер недоступен».
 */
internal object SafeRetry : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!safe(request)) return chain.proceed(request)
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            chain.proceed(request)
        }
    }

    /** Повтор не меняет итога: чтение или запрос со своим ключом идемпотентности, и тело его можно послать ещё раз. */
    fun safe(request: Request): Boolean = (request.method == "GET" || request.header(IDEMPOTENCY_HEADER) != null) && request.body?.isOneShot() != true
}
