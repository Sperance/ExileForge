package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val JsonMedia = "application/json; charset=utf-8".toMediaType()

fun normalizeServer(value: String): String {
    val url = value.trim().toHttpUrlOrNull() ?: error(ui("api.url_scheme"))
    require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { ui("api.url_parts") }
    return url.toString().trimEnd('/') + "/"
}

internal fun requirePage(page: Int) = require(page >= 0) { ui("api.negative_page") }

/**
 * The wire, and nothing else: one request in, the envelope's `data` out, every exchange journaled.
 *
 * It holds the session's token because every request has to carry it, and drops it on a 401 before
 * telling [onUnauthorized]. What a request *means* lives in the feature clients built on top of it.
 */
class Transport(
    server: String,
    private val journal: RequestJournal,
    private val client: OkHttpClient,
    private val onUnauthorized: () -> Unit,
) {
    private val base = normalizeServer(server).toHttpUrlOrNull()!!
    internal var token: String? = null

    /** The fingerprints of the hero parts held for a hero, or `null` for one nobody is looking at: a command on it asks for its snapshot. */
    internal var heroParts: (String) -> String? = { null }

    /** A command's snapshot of the hero, or `null` when the answer came without one. */
    internal var onHero: (String, HeroSnapshot?) -> Unit = { _, _ -> }

    /** The whole of a small, server-seeded collection. */
    internal suspend fun all(path: String): List<JsonObject> = request("GET", path, authenticated = true).jsonArray.map { it.jsonObject }

    /** A plain JSON file from the server, outside the API envelope. */
    internal suspend inline fun <reified T> fetch(path: String): T = WireJson.decodeFromString(fetchText(path))

    /**
     * The same file as text, so a document can be stored verbatim and parsed again offline. [json] = false
     * is for a portrait's SVG; [validate] = false skips the syntax check for a document its caller parses whole anyway.
     */
    internal suspend fun fetchText(path: String, json: Boolean = true, authenticated: Boolean = false, validate: Boolean = json): String {
        val url = base.newBuilder().addPathSegments(path).build()
        val credential = token.takeIf { authenticated }
        if (authenticated) require(credential != null) { ui("api.sign_in_tab") }
        val start = System.nanoTime()
        var status: Int? = null
        var responseText = ""
        var success = false
        try {
            val payload = client.newCall(Request.Builder().url(url).header("Accept", if (json) "application/json" else "image/svg+xml")
                .apply { credential?.let { header("Authorization", "Bearer $it") } }.get().build()).awaitPayload()
            status = payload.status
            responseText = payload.body.take(2_000)
            if (status !in 200..299) throw ApiFailure(status, null, ui("api.file_not_served", status))
            if (validate) try { withContext(Dispatchers.Default) { WireJson.parseToJsonElement(payload.body) } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { throw ApiFailure(status, null, ui("api.malformed_json_at", path)) }
            success = true
            return payload.body
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (e is ApiFailure) status = e.status
            if (responseText.isBlank()) responseText = e.message.orEmpty()
            throw e
        } finally {
            journal.add(RequestLog("GET", url.encodedPath, status, (System.nanoTime() - start) / 1_000_000, "", responseText.take(400), success))
        }
    }

    /**
     * @param bearer a token to send instead of the session's own — only `GameApi.revoke` passes one, since
     * it speaks for a session that has already been dropped here
     */
    internal suspend fun request(method: String, path: String, query: Map<String, String> = emptyMap(), body: JsonElement? = null,
                                authenticated: Boolean = false, sensitive: Boolean = false, bearer: String? = null,
                                headers: Map<String, String> = emptyMap()): JsonElement {
        val credential = bearer ?: token.takeIf { authenticated }
        if (authenticated) require(credential != null) { ui("api.sign_in_tab") }
        val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        val bodyText = body?.toString().orEmpty()
        // Several commands are POSTs carrying their arguments in the query string; OkHttp still demands a body for those methods.
        val payload = body?.toString()?.toRequestBody(JsonMedia)
            ?: if (method in setOf("POST", "PUT", "PATCH")) "".toRequestBody(JsonMedia) else null
        // Every command on a hero the client shows asks for the hero back: the header names what it already holds.
        val heroOf = query["heroId"]?.takeIf { method == "POST" && authenticated }
        val parts = heroOf?.let(heroParts)
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .apply { credential?.let { header("Authorization", "Bearer $it") } }
            .apply { parts?.let { header(HeroParts.HEADER, it) } }
            .apply { headers.forEach { (name, value) -> header(name, value) } }.method(method, payload).build()
        val start = System.nanoTime()
        var status: Int? = null
        var responseText = ""
        var success = false
        try {
            val answer = client.newCall(request).awaitPayload()
            status = answer.status
            if (status == 401 && authenticated) { token = null; onUnauthorized() }
            if (status == 304) { success = true; return JsonNull }
            val raw = answer.body
            responseText = raw.take(12_000)
            val envelope = try { withContext(Dispatchers.Default) { WireJson.parseToJsonElement(raw).jsonObject } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { throw ApiFailure(status, null, if (status == 401) ui("api.session_expired") else if (status == 403) ui("api.no_rights") else ui("api.bad_json", status)) }
            if (status !in 200..299 || (envelope["success"] as? JsonPrimitive)?.booleanOrNull != true) {
                val error = envelope["error"] as? JsonObject
                throw ApiFailure(status, error?.text("errorCode"),
                    error?.text("message")?.takeIf { it.isNotBlank() } ?: ui("api.rejected", status),
                    (error?.get("messageArgs") as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull })
            }
            success = true
            // The command has landed: a snapshot that cannot be read only means the hero is read again.
            if (heroOf != null && parts != null) runCatching { onHero(heroOf, envelope["hero"]?.takeIf { it is JsonObject }
                ?.let { runCatching { WireJson.decodeFromJsonElement(HeroSnapshot.serializer(), it) }.getOrNull() }) }
            return envelope["data"] ?: JsonNull
        } catch (e: CancellationException) {
            responseText = ui("api.cancelled"); throw e
        } catch (e: Exception) {
            if (e is ApiFailure) status = e.status
            if (responseText.isBlank()) responseText = e.message.orEmpty()
            throw e
        } finally {
            journal.add(RequestLog(method, url.encodedPath + (url.encodedQuery?.let { "?${if (sensitive) ui("api.hidden") else it}" } ?: ""), status,
                (System.nanoTime() - start) / 1_000_000, if (sensitive) ui("api.hidden") else bodyText.take(12_000), if (sensitive) ui("api.hidden") else responseText, success))
        }
    }
}

/** A signed-in read whose envelope `data` decodes to [T]. */
internal suspend inline fun <reified T> Transport.get(path: String, query: Map<String, String> = emptyMap()): T =
    WireJson.decodeFromJsonElement(request("GET", path, query, authenticated = true))

/** A signed-in command whose envelope `data` decodes to [T]. */
internal suspend inline fun <reified T> Transport.post(path: String, query: Map<String, String> = emptyMap(), body: JsonElement? = null): T =
    WireJson.decodeFromJsonElement(request("POST", path, query, body, authenticated = true))

/** The query of a route about one hero: every such route names it by `heroId`. A `null` value leaves its parameter out. */
internal fun heroQuery(heroId: String, vararg more: Pair<String, String?>): Map<String, String> {
    requireId(heroId)
    return buildMap {
        put("heroId", heroId)
        more.forEach { (name, value) -> if (value != null) put(name, value) }
    }
}
