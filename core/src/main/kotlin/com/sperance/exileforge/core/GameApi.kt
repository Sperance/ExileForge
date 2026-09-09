package com.sperance.exileforge.core

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ApiFailure(val status: Int?, val code: String?, message: String) : IOException(message)
data class RequestLog(val method: String, val path: String, val status: Int?, val elapsedMs: Long, val request: String, val response: String, val ok: Boolean)
class RequestJournal {
    private val mutable = MutableStateFlow<List<RequestLog>>(emptyList())
    val entries = mutable.asStateFlow()
    fun add(entry: RequestLog) { mutable.update { (listOf(entry) + it).take(60) } }
    fun clear() { mutable.value = emptyList() }
}
data class ItemPage(val items: List<JsonObject>, val page: Int, val totalPages: Int, val totalItems: Long)
interface ItemRepository {
    suspend fun page(catalog: Catalog, page: Int): ItemPage
    suspend fun get(catalog: Catalog, id: String): JsonObject?
    suspend fun create(catalog: Catalog, document: JsonObject): JsonObject
    suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject
    suspend fun delete(catalog: Catalog, id: String)
}
fun normalizeServer(value: String): String {
    val url = value.trim().toHttpUrlOrNull() ?: error("Введите URL с http:// или https://")
    require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "URL не должен содержать пароль, query или fragment" }
    return url.toString().trimEnd('/') + "/"
}
class GameApi(
    server: String,
    private val journal: RequestJournal = RequestJournal(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build()
) : ItemRepository {
    private val base = normalizeServer(server).toHttpUrlOrNull()!!
    private fun route(catalog: Catalog) = "api/v1/${catalog.path}"
    override suspend fun page(catalog: Catalog, page: Int): ItemPage {
        require(page >= 0)
        val body = request("GET", "${route(catalog)}/paged", mapOf("page" to "$page", "size" to "20")).jsonObject
        return ItemPage(body.getValue("items").jsonArray.map { it.jsonObject }, body.getValue("page").jsonPrimitive.int,
            body.getValue("totalPages").jsonPrimitive.int, body.getValue("totalItems").jsonPrimitive.long)
    }
    override suspend fun get(catalog: Catalog, id: String): JsonObject? {
        requireId(id)
        return request("GET", route(catalog), mapOf("id" to id)).let { if (it == JsonNull) null else it.jsonObject }
    }
    override suspend fun create(catalog: Catalog, document: JsonObject): JsonObject {
        validate(document, catalog)
        return request("POST", route(catalog), body = JsonArray(listOf(document))).jsonArray.single().jsonObject
    }
    override suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject {
        requireId(id)
        require(changes.isNotEmpty()) { "Нет изменений" }
        require(changes.keys.none { it in protectedFields }) { "Нельзя изменять служебные поля" }
        return request("PUT", route(catalog), mapOf("id" to id), changes).let {
            if (it == JsonNull) throw ApiFailure(200, null, "Сервер не вернул изменённый предмет")
            it.jsonObject
        }
    }
    override suspend fun delete(catalog: Catalog, id: String) { requireId(id); request("DELETE", route(catalog), mapOf("id" to id)) }
    suspend fun health(): JsonElement = request("GET", "system/health")
    suspend fun count(catalog: Catalog): JsonElement = request("GET", "${route(catalog)}/count")
    private suspend fun request(method: String, path: String, query: Map<String, String> = emptyMap(), body: JsonElement? = null): JsonElement {
        val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (k,v) -> addQueryParameter(k,v) } }.build()
        val bodyText = body?.toString().orEmpty()
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .method(method, body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val start = System.nanoTime()
        var status: Int? = null
        var responseText = ""
        var success = false
        try {
            val payload = client.newCall(request).awaitPayload()
            status = payload.status
            val raw = payload.body
            responseText = raw.take(12_000)
            val envelope = try { withContext(Dispatchers.Default) { WireJson.parseToJsonElement(raw).jsonObject } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { throw ApiFailure(status, null, "HTTP $status: сервер вернул не JSON ApiMongoResponse") }
            if (status !in 200..299 || (envelope["success"] as? JsonPrimitive)?.booleanOrNull != true) {
                val error = envelope["error"] as? JsonObject
                throw ApiFailure(status, error?.text("errorCode"), error?.text("message")?.takeIf { it.isNotBlank() } ?: "HTTP $status: операция отклонена")
            }
            success = true
            return envelope["data"] ?: JsonNull
        } catch (e: CancellationException) {
            responseText = "Запрос отменён. Результат записи следует проверить на сервере."; throw e
        } catch (e: Exception) {
            if (e is ApiFailure) status = e.status
            if (responseText.isBlank()) responseText = e.message.orEmpty()
            throw e
        } finally {
            journal.add(RequestLog(method, url.encodedPath + (url.encodedQuery?.let { "?$it" } ?: ""), status,
                (System.nanoTime() - start) / 1_000_000, bodyText.take(12_000), responseText, success))
        }
    }
}
private data class HttpPayload(val status: Int, val body: String)
/** Consume and close the body on OkHttp's worker, keeping cancellation wired through the full read. */
private suspend fun Call.awaitPayload(): HttpPayload = suspendCancellableCoroutine { continuation ->
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
