package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.creationFields
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.contract.validateModifierPool
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleLanguage
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.*
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierTier
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
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
    internal suspend fun fetch(path: String): JsonElement = WireJson.parseToJsonElement(fetchText(path))

    /** The same file as text, so a dictionary can be stored verbatim and parsed again offline. */
    internal suspend fun fetchText(path: String): String {
        val url = base.newBuilder().addPathSegments(path).build()
        val start = System.nanoTime()
        var status: Int? = null
        var responseText = ""
        var success = false
        try {
            val payload = client.newCall(Request.Builder().url(url).header("Accept", "application/json").get().build()).awaitPayload()
            status = payload.status
            responseText = payload.body.take(2_000)
            if (status !in 200..299) throw ApiFailure(status, null, ui("api.file_not_served", status))
            try { withContext(Dispatchers.Default) { WireJson.parseToJsonElement(payload.body) } }
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
                                authenticated: Boolean = false, sensitive: Boolean = false, bearer: String? = null): JsonElement {
        val credential = bearer ?: token.takeIf { authenticated }
        if (authenticated) require(credential != null) { ui("api.sign_in_tab") }
        val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        val bodyText = body?.toString().orEmpty()
        // Several server commands are POSTs carrying their arguments in the query string; OkHttp
        // still demands a body for those methods, so an empty one stands in for "no payload".
        val payload = body?.toString()?.toRequestBody(JsonMedia)
            ?: if (method in setOf("POST", "PUT", "PATCH")) "".toRequestBody(JsonMedia) else null
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .apply { credential?.let { header("Authorization", "Bearer $it") } }.method(method, payload).build()
        val start = System.nanoTime()
        var status: Int? = null
        var responseText = ""
        var success = false
        try {
            val payload = client.newCall(request).awaitPayload()
            status = payload.status
            if (status == 401 && authenticated) { token = null; onUnauthorized() }
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
