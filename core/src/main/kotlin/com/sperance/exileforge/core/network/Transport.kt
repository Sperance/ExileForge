package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.ui
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import kotlinx.serialization.json.*
import okhttp3.*

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
 * telling [onUnauthorized]. What a request *means* lives in the feature clients built on top of it;
 * `request` is internal, so the app reaches the server only through those.
 */
class Transport(
    server: String,
    private val journal: RequestJournal,
    private val client: OkHttpClient,
    private val onUnauthorized: () -> Unit,
) {
    private val base = normalizeServer(server).toHttpUrlOrNull()!!
    internal var token: String? = null

    /**
     * The fingerprints of the hero parts held for a character, or `null` for a character nobody is
     * looking at. A command on a character that has them asks the server for its snapshot.
     */
    internal var heroParts: (String) -> String? = { null }

    /** A command's snapshot of the hero, or `null` when the answer came without one. */
    internal var onHero: (String, HeroSnapshot?) -> Unit = { _, _ -> }

    /**
     * The whole collection.
     *
     * This is how every list is read. The server's `/paged` route still hands `page` straight to the
     * repository as the offset instead of `page * size`, so every page but the first is off by all
     * but one record — 0.9.1 swapped the arguments of `findLimited`, not the arithmetic above it.
     * Reading the collection is safe here because these collections are small and server-seeded.
     */
    internal suspend fun all(path: String): List<JsonObject> =
        request("GET", path, authenticated = true).jsonArray.map { it.jsonObject }

    /**
     * A plain JSON file from the server, outside the API envelope.
     *
     * Only the locale files are served this way. It still goes through the journal, because a
     * missing dictionary is exactly the kind of thing that has to be visible when text turns into
     * raw keys on screen.
     */
    internal suspend inline fun <reified T> fetch(path: String): T = WireJson.decodeFromString(fetchText(path))

    /**
     * The same file as text, so a dictionary can be stored verbatim and parsed again offline.
     * [json] = false is for a portrait's SVG, which is checked by its own parser, not as JSON.
     * [validate] = false skips the syntax check for a document its caller parses whole anyway,
     * so a large file is not read twice.
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
        // Several server commands are POSTs carrying their arguments in the query string; OkHttp
        // still demands a body for those methods, so an empty one stands in for "no payload".
        val payload = body?.toString()?.toRequestBody(JsonMedia)
            ?: if (method in setOf("POST", "PUT", "PATCH")) "".toRequestBody(JsonMedia) else null
        // Every command on a character the client shows asks for the hero back (server 0.48.0).
        val heroOf = query["characterId"]?.takeIf { method == "POST" && authenticated }
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
            val payload = client.newCall(request).awaitPayload()
            status = payload.status
            if (status == 401 && authenticated) { token = null; onUnauthorized() }
            if (status == 304) { success = true; return JsonNull }
            val raw = payload.body
            responseText = raw.take(12_000)
            val envelope = try { withContext(Dispatchers.Default) { WireJson.parseToJsonElement(raw).jsonObject } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { throw ApiFailure(status, null, if (status == 401) ui("api.session_expired") else if (status == 403) ui("editor.no_rights") else ui("api.bad_json", status)) }
            if (status !in 200..299 || (envelope["success"] as? JsonPrimitive)?.booleanOrNull != true) {
                val error = envelope["error"] as? JsonObject
                throw ApiFailure(status, error?.text("errorCode"),
                    error?.text("message")?.takeIf { it.isNotBlank() } ?: ui("api.rejected", status),
                    // The arguments that filled the server's sentence, so the client can fill its own.
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

/**
 * The query of a route about one hero: every such route names it by `characterId`, checked here once.
 * A `null` value leaves its parameter out, which is how an optional argument stays off the wire.
 */
internal fun heroQuery(characterId: String, vararg more: Pair<String, String?>): Map<String, String> {
    requireId(characterId)
    return buildMap {
        put("characterId", characterId)
        more.forEach { (name, value) -> if (value != null) put(name, value) }
    }
}
