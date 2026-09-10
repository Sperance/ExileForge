package com.sperance.exileforge.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import kotlin.test.*

class PoeApiTest {
    private val id = "0123456789abcdef01234567"
    private fun MockWebServer.ok(data: String) = enqueue(MockResponse().setBody("""{"success":true,"data":$data}"""))
    @Test fun `catalog is paged searched and exact revisions have query parameters`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = GameApi(server.url("/game/").toString())
            server.ok("""{"items":[],"page":2,"size":50,"total":40100}""")
            assertEquals(40100, api.definitions("life & mana", 2).getValue("total").jsonPrimitive.int)
            val query = server.takeRequest().requestUrl!!
            assertEquals("/game/api/v1/poe/modifier-definitions", query.encodedPath)
            assertEquals("life & mana", query.queryParameter("q")); assertEquals("2", query.queryParameter("page"))
            server.ok("""{"id":"custom/life","revision":2}""")
            assertEquals("2", api.definition("custom/life", 2).text("revision"))
            val exact = server.takeRequest().requestUrl!!
            assertEquals("custom/life", exact.queryParameter("id")); assertEquals("2", exact.queryParameter("revision"))
        }
    }
    @Test fun `token and password are redacted and bearer sent only to authenticated routes`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val journal = RequestJournal(); val api = GameApi(server.url("/").toString(), journal)
            server.ok("""{"token":"private-token","expiresIn":3600}""")
            api.login("user", "private-password")
            assertNull(server.takeRequest().getHeader("Authorization"))
            assertFalse(journal.entries.value.toString().contains("private-"))
            server.ok("""{"version":3,"equipment":[]}""")
            api.inventory(id)
            assertEquals("Bearer private-token", server.takeRequest().getHeader("Authorization"))
            server.ok("[]"); api.currencies()
            assertNull(server.takeRequest().getHeader("Authorization"))
            api.logout()
            assertFailsWith<IllegalArgumentException> { api.inventory(id) }
            assertEquals(3, server.requestCount)
        }
    }
    @Test fun `craft preserves request identity version and instance through retry`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = GameApi(server.url("/").toString())
            server.ok("""{"token":"token"}"""); api.login("user", "password"); server.takeRequest()
            val body = buildJsonObject { put("requestId", "same-request-123"); put("expectedVersion", 9007199254740993L); put("equipmentUuid", id); put("currency", "DIVINE") }
            server.enqueue(MockResponse().setResponseCode(503).setBody(""))
            assertFailsWith<ApiFailure> { api.mutateInventory(id, "craft", body) }
            val first = server.takeRequest()
            server.ok("""{"requestId":"same-request-123","characterVersion":9007199254740994,"equipment":{"uuid":"$id"}}""")
            api.mutateInventory(id, "craft", body)
            val second = server.takeRequest()
            assertEquals("/api/v1/poe/characters/$id/craft", second.path)
            assertEquals(first.body.readUtf8(), second.body.readUtf8())
        }
    }
    @Test fun `drop sends no locally generated item and publication uses revision precondition`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = GameApi(server.url("/").toString())
            server.ok("""{"token":"token"}"""); api.login("user", "password"); server.takeRequest()
            val drop = buildJsonObject { put("requestId", "drop-request-1"); put("expectedVersion", 12L) }
            server.ok("{}"); api.mutateInventory(id, "drop", drop)
            val sent = server.takeRequest()
            assertEquals("/api/v1/poe/characters/$id/drop", sent.path)
            assertEquals(drop, WireJson.parseToJsonElement(sent.body.readUtf8()))
            server.ok("{}"); api.publishDefinition(starterDefinition(), 2)
            val publication = server.takeRequest()
            assertEquals("/api/v1/poe/modifier-definitions", publication.path)
            assertEquals(2, WireJson.parseToJsonElement(publication.body.readUtf8()).jsonObject.getValue("expectedRevision").jsonPrimitive.int)
        }
    }
    @Test fun `empty auth responses retain HTTP status`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = GameApi(server.url("/").toString())
            server.ok("""{"token":"token"}"""); api.login("user", "password")
            server.enqueue(MockResponse().setResponseCode(401))
            assertEquals(401, assertFailsWith<ApiFailure> { api.inventory(id) }.status)
        }
    }
    @Test fun `equipment rejects inline definitions and malformed references before network`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val api = GameApi(server.url("/").toString())
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, id, buildJsonObject { put("modifierDefinitions", JsonArray(listOf(starterDefinition()))) }) }
            assertFailsWith<IllegalArgumentException> { api.update(Catalog.EQUIPMENT, id, buildJsonObject { put("modifierDefinitionRefs", JsonNull) }) }
            assertEquals(0, server.requestCount)
        }
    }
    @Test fun `selected modifiers attach immutable references and preserve revision`() {
        val definition = JsonObject(starterDefinition() + ("revision" to JsonPrimitive(7)))
        val mod = modifierFromDefinition(definition)
        assertEquals("7", mod.text("definitionRevision"))
        val document = attachSelectedDefinitions(template(Catalog.EQUIPMENT), JsonArray(listOf(mod)), listOf(definition))
        assertFalse("modifierDefinitions" in document)
        assertEquals("7", document.getValue("modifierDefinitionRefs").jsonArray.single().jsonObject.text("revision"))
        validateReferenceWrite(document)
        assertNotEquals(definitionKey(definition), definitionKey(starterDefinition()))
    }
}
