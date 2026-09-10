package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.contract.validateReferenceWrite
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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
    private var token: String? = null
    suspend fun login(login: String, password: String) {
        token = null
        val result = request("POST", "api/v1/poe/token", body = buildJsonObject { put("login", login); put("password", password) }, sensitive = true).jsonObject
        token = result.text("token").also { require(it.isNotBlank()) }
    }
    fun logout() { token = null }
    suspend fun referencePage(source: EntitySource, page: Int): ItemPage {
        require(page >= 0)
        val body = request("GET", "api/v1/${source.path}/paged", mapOf("page" to "$page", "size" to "50")).jsonObject
        return ItemPage(body.getValue("items").jsonArray.map { it.jsonObject }, body.getValue("page").jsonPrimitive.int,
            body.getValue("totalPages").jsonPrimitive.int, body.getValue("totalItems").jsonPrimitive.long)
    }
    suspend fun definitions(query: String, page: Int): JsonObject = request("GET", "api/v1/poe/modifier-definitions", mapOf("q" to query, "page" to "$page", "size" to "50")).jsonObject
    suspend fun definition(id: String, revision: Int): JsonObject = request("GET", "api/v1/poe/modifier-definition", mapOf("id" to id, "revision" to "$revision")).jsonObject
    suspend fun publishDefinition(definition: JsonObject, expectedRevision: Int): JsonObject = request("POST", "api/v1/poe/modifier-definitions", body = buildJsonObject {
        put("definition", definition); put("expectedRevision", expectedRevision)
    }, authenticated = true).jsonObject
    suspend fun inventory(id: String): JsonObject { requireId(id); return request("GET", "api/v1/poe/characters/$id/inventory", authenticated = true).jsonObject }
    suspend fun currencies(): List<JsonObject> = request("GET", "api/v1/poe/currencies").jsonArray.map { it.jsonObject }
    suspend fun mutateInventory(id: String, operation: String, payload: JsonObject): JsonObject {
        requireId(id); require(operation in setOf("drop", "craft"))
        return request("POST", "api/v1/poe/characters/$id/$operation", body = payload, authenticated = true).jsonObject
    }
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
        if(catalog == Catalog.EQUIPMENT) validateReferenceWrite(document)
        return request("POST", route(catalog), body = JsonArray(listOf(document))).jsonArray.single().jsonObject
    }
    override suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject {
        requireId(id)
        if(catalog == Catalog.EQUIPMENT) validateReferenceWrite(changes)
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
    private suspend fun request(method: String, path: String, query: Map<String, String> = emptyMap(), body: JsonElement? = null, authenticated: Boolean = false, sensitive: Boolean = false): JsonElement {
        if (authenticated) require(!token.isNullOrBlank()) { "Войдите во вкладке «Сервер»" }
        val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (k,v) -> addQueryParameter(k,v) } }.build()
        val bodyText = body?.toString().orEmpty()
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .apply { if (authenticated) header("Authorization", "Bearer $token") }
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
                catch (_: Exception) { throw ApiFailure(status, null, if(status == 401) "Сессия истекла. Войдите снова" else if(status == 403) "Недостаточно прав" else "HTTP $status: пустой или некорректный JSON ответ сервера") }
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
                (System.nanoTime() - start) / 1_000_000, if(sensitive) "[скрыто]" else bodyText.take(12_000), if(sensitive) "[скрыто]" else responseText, success))
        }
    }
}
